cmake_minimum_required(VERSION 3.22.1)
project(stockfish_bridge LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Path to Stockfish source (relative to this CMakeLists.txt)
set(SF_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../../../src")

# Stockfish core sources (excluding main.cpp — we provide our own entry via JNI)
set(SF_SOURCES
    ${SF_SRC}/benchmark.cpp
    ${SF_SRC}/bitboard.cpp
    ${SF_SRC}/engine.cpp
    ${SF_SRC}/evaluate.cpp
    ${SF_SRC}/memory.cpp
    ${SF_SRC}/misc.cpp
    ${SF_SRC}/movegen.cpp
    ${SF_SRC}/movepick.cpp
    ${SF_SRC}/position.cpp
    ${SF_SRC}/score.cpp
    ${SF_SRC}/search.cpp
    ${SF_SRC}/thread.cpp
    ${SF_SRC}/timeman.cpp
    ${SF_SRC}/tt.cpp
    ${SF_SRC}/tune.cpp
    ${SF_SRC}/uci.cpp
    ${SF_SRC}/ucioption.cpp
    ${SF_SRC}/nnue/network.cpp
    ${SF_SRC}/nnue/nnue_accumulator.cpp
    ${SF_SRC}/nnue/nnue_misc.cpp
    ${SF_SRC}/nnue/features/half_ka_v2_hm.cpp
    ${SF_SRC}/nnue/features/full_threats.cpp
    ${SF_SRC}/syzygy/tbprobe.cpp
)

# JNI bridge
set(BRIDGE_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/stockfish_bridge.cpp
)

add_library(stockfish_bridge SHARED ${SF_SOURCES} ${BRIDGE_SOURCES})

target_include_directories(stockfish_bridge PRIVATE
    ${SF_SRC}
    ${SF_SRC}/nnue
    ${SF_SRC}/syzygy
)

# Stockfish expects these defines for Android/NDK builds
target_compile_definitions(stockfish_bridge PRIVATE
    NNUE_EMBEDDING_OFF=0
    IS_64BIT
    USE_POPCNT
)

# arm64 NEON optimizations
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    target_compile_definitions(stockfish_bridge PRIVATE USE_NEON)
endif()

# Optimization flags
target_compile_options(stockfish_bridge PRIVATE
    -O3
    -DNDEBUG
    -flto
)

target_link_options(stockfish_bridge PRIVATE -flto)

target_link_libraries(stockfish_bridge PRIVATE
    android
    log
)
