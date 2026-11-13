# Keep JNI-called methods and the whisper.cpp bridge intact.
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.vaani.keyboard.speech.** { *; }
