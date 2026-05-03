cmake_minimum_required(VERSION 3.28)
project(Blackjack VERSION 1.0.0)
include(ProcessorCount)

ProcessorCount(N)
if(NOT N EQUAL 0)
	set(CMAKE_BUILD_PARALLEL_LEVEL ${N})
	message(STATUS "Building with ${N} Jobs")
endif()

find_package(PkgConfig REQUIRED)
pkg_check_modules(GTKMM REQUIRED gtkmm-4.0)
find_package(OpenSSL REQUIRED)

set(EXE_NAME Blackjack)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

add_executable(${EXE_NAME} src/blackjack.cpp)
add_subdirectory(src)
add_subdirectory(includes)
include_directories(${GTKMM_INCLUDE_DIRS})

target_compile_options(${EXE_NAME} PRIVATE -Wall -Wextra ${GTK4_CFLAGS})
target_include_directories(${EXE_NAME} PRIVATE ${GTKMM_INCLUDE_DIRS})
target_link_libraries(${EXE_NAME} PRIVATE ${GTKMM_LIBRARIES} OpenSSL::SSL OpenSSL::Crypto)

target_compile_options(${EXE_NAME} PRIVATE
  $<$<CONFIG:Debug>:-DDEBUG>
)

set_target_properties(${EXE_NAME} PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY_DEBUG "${CMAKE_BINARY_DIR}/../bin/debug"
    RUNTIME_OUTPUT_DIRECTORY_RELEASE "${CMAKE_BINARY_DIR}/../bin/release"
)

option(BUILD_TESTS "Build unit tests" OFF)

if(BUILD_TESTS)
    enable_testing()
    
    find_package(GTest REQUIRED)
    set(TEST_NAME BlackjackTsts)
    
    add_executable(${TEST_NAME}
        tests/test_deck.cpp
    )
    
    target_include_directories(${TEST_NAME} PRIVATE 
        ${CMAKE_SOURCE_DIR}/includes
        ${GTKMM_INCLUDE_DIRS}
    )
    
    target_link_libraries(${TEST_NAME} PRIVATE
        gtest
        gtest_main
        pthread
        OpenSSL::SSL
        OpenSSL::Crypto
    )
    include(GoogleTest) 
    gtest_discover_tests(${TEST_NAME})

    set_target_properties(${TEST_NAME} PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/../bin/test"
    )
endif()
