cmake_minimum_required(VERSION 3.22.1)
project("parsec_bridge" C)

# Source files
add_library(parsec_bridge SHARED
    parsec_jni.c
    audio_android.c
)

# Include our compatibility header (or real SDK header if available)
target_include_directories(parsec_bridge PRIVATE
    ${CMAKE_SOURCE_DIR}
)

# Link Android libraries
find_library(log_lib log)
find_library(opensles_lib OpenSLES)
target_link_libraries(parsec_bridge
    ${log_lib}
    ${opensles_lib}
)

# Link Parsec SDK pre-built library
# The Parsec SDK .so must be placed in app/src/main/jniLibs/{abi}/
set(PARSEC_SDK_LIB "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libparsec.so")
if(EXISTS ${PARSEC_SDK_LIB})
    add_library(parsec_sdk SHARED IMPORTED)
    set_target_properties(parsec_sdk PROPERTIES
        IMPORTED_LOCATION ${PARSEC_SDK_LIB}
    )
    target_link_libraries(parsec_bridge parsec_sdk)
    message(STATUS "Found Parsec SDK library: ${PARSEC_SDK_LIB}")
else()
    message(WARNING "Parsec SDK library not found at ${PARSEC_SDK_LIB}. "
                    "Place libparsec.so in app/src/main/jniLibs/${ANDROID_ABI}/ "
                    "for full SDK functionality.")
endif()
