cmake_minimum_required(VERSION 3.22.1)
project(vaani_whisper)

include(FetchContent)

# Pinned to a specific tag rather than a moving branch so builds are
# reproducible. Verify this tag still exists before relying on it — this
# repo could not reach github.com from the sandbox it was written in
# (network policy only allowed dl.google.com), so this was never actually
# fetched/built there. See HANDOVER.md.
FetchContent_Declare(
    whisper_cpp
    GIT_REPOSITORY https://github.com/ggml-org/whisper.cpp.git
    GIT_TAG        v1.7.2
)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(whisper_cpp)

add_library(vaani_whisper SHARED jni_bridge.cpp)

find_library(log-lib log)

target_link_libraries(vaani_whisper
    whisper
    ${log-lib}
)
