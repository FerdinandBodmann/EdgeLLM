# Synapse native bridge (integration-level-0 scaffold).
#
# This CMake project builds `libsynapse_native.so`, the JNI layer for the
# optional llama.cpp backend. It is only configured when the Gradle build is
# invoked with `-PenableNative=true`; the default build (and CI, which has no
# NDK) never touches this file.
#
# To make this a *real* backend, drop the llama.cpp sources (or add them as a
# git submodule), then:
#   1. `add_subdirectory(llama.cpp)` (or FetchContent it), and
#   2. `target_link_libraries(synapse_native llama ${log-lib})`.
# The Kotlin call sites in `LlamaBridge.kt` stay unchanged.

cmake_minimum_required(VERSION 3.22.1)

project(synapse_native LANGUAGES CXX)

add_library(
    synapse_native
    SHARED
    synapse_native.cpp
)

target_compile_features(synapse_native PRIVATE cxx_std_17)

find_library(
    log-lib
    log
)

target_link_libraries(
    synapse_native
    ${log-lib}
)
