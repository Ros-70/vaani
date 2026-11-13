// JNI bridge between com.vaani.keyboard.speech.WhisperEngine and
// whisper.cpp's public C API (whisper.h). Written against that API from
// documentation/memory — this sandbox had no NDK and could not reach
// github.com to vendor whisper.cpp itself, so this file has never actually
// been compiled here. CI (full internet access + a real NDK) is the first
// real build; verify there before shipping. See HANDOVER.md.

#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include <mutex>

#include "whisper.h"

#define LOG_TAG "vaani_whisper_jni"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

// Holds one loaded model plus the accumulation buffer used for the simple
// "re-decode the growing buffer" streaming approach (see WhisperEngine.kt's
// doc comment / HANDOVER.md for why this is a known MVP-level simplification
// rather than true incremental streaming).
struct VaaniWhisperContext {
    whisper_context *ctx = nullptr;
    std::vector<float> streamBuffer;
    std::mutex mutex;
};

std::vector<float> pcm16ToFloat(const int16_t *pcm, int count) {
    std::vector<float> out(count);
    for (int i = 0; i < count; ++i) {
        out[i] = static_cast<float>(pcm[i]) / 32768.0f;
    }
    return out;
}

std::string runFull(VaaniWhisperContext *vctx, const std::vector<float> &samples) {
    if (vctx->ctx == nullptr || samples.empty()) return "";

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_special = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.single_segment = false;
    params.n_threads = 4;
    // Hindi/Hinglish support per the build brief's model list — whisper.cpp's
    // "auto" language detection generally handles this; left as "auto"
    // rather than hardcoding "en" so Hindi dictation works without a
    // separate code path.
    params.language = "auto";
    params.translate = false;

    if (whisper_full(vctx->ctx, params, samples.data(), static_cast<int>(samples.size())) != 0) {
        LOGE("whisper_full failed");
        return "";
    }

    std::string result;
    const int segments = whisper_full_n_segments(vctx->ctx);
    for (int i = 0; i < segments; ++i) {
        const char *text = whisper_full_get_segment_text(vctx->ctx, i);
        if (text != nullptr) {
            if (!result.empty()) result += " ";
            result += text;
        }
    }
    return result;
}

} // namespace

extern "C" JNIEXPORT jlong JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeLoad(JNIEnv *env, jobject /*thiz*/, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    struct whisper_context_params cparams = whisper_context_default_params();
    whisper_context *ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("whisper_init_from_file failed");
        return 0;
    }

    auto *vctx = new VaaniWhisperContext();
    vctx->ctx = ctx;
    return reinterpret_cast<jlong>(vctx);
}

extern "C" JNIEXPORT void JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeUnload(JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    auto *vctx = reinterpret_cast<VaaniWhisperContext *>(handle);
    if (vctx == nullptr) return;
    if (vctx->ctx != nullptr) whisper_free(vctx->ctx);
    delete vctx;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeTranscribeFull(
    JNIEnv *env, jobject /*thiz*/, jlong handle, jshortArray pcm) {
    auto *vctx = reinterpret_cast<VaaniWhisperContext *>(handle);
    if (vctx == nullptr) return env->NewStringUTF("");

    jsize len = env->GetArrayLength(pcm);
    jshort *data = env->GetShortArrayElements(pcm, nullptr);
    auto samples = pcm16ToFloat(reinterpret_cast<const int16_t *>(data), len);
    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);

    std::string result;
    {
        std::lock_guard<std::mutex> lock(vctx->mutex);
        result = runFull(vctx, samples);
    }
    return env->NewStringUTF(result.c_str());
}

// Streaming path: appends this chunk to the growing buffer and re-decodes
// it. Simple and correct, not yet true incremental streaming — see the
// doc comment on runFull / HANDOVER.md.
extern "C" JNIEXPORT jstring JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeTranscribeChunk(
    JNIEnv *env, jobject /*thiz*/, jlong handle, jshortArray pcm) {
    auto *vctx = reinterpret_cast<VaaniWhisperContext *>(handle);
    if (vctx == nullptr) return env->NewStringUTF("");

    jsize len = env->GetArrayLength(pcm);
    jshort *data = env->GetShortArrayElements(pcm, nullptr);
    auto chunkSamples = pcm16ToFloat(reinterpret_cast<const int16_t *>(data), len);
    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);

    std::string result;
    {
        std::lock_guard<std::mutex> lock(vctx->mutex);
        vctx->streamBuffer.insert(vctx->streamBuffer.end(), chunkSamples.begin(), chunkSamples.end());
        result = runFull(vctx, vctx->streamBuffer);
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeFinalizeStream(JNIEnv *env, jobject /*thiz*/, jlong handle) {
    auto *vctx = reinterpret_cast<VaaniWhisperContext *>(handle);
    if (vctx == nullptr) return env->NewStringUTF("");

    std::string result;
    {
        std::lock_guard<std::mutex> lock(vctx->mutex);
        result = runFull(vctx, vctx->streamBuffer);
        vctx->streamBuffer.clear();
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_vaani_keyboard_speech_WhisperEngine_nativeResetStream(JNIEnv * /*env*/, jobject /*thiz*/, jlong handle) {
    auto *vctx = reinterpret_cast<VaaniWhisperContext *>(handle);
    if (vctx == nullptr) return;
    std::lock_guard<std::mutex> lock(vctx->mutex);
    vctx->streamBuffer.clear();
}
