cmake_minimum_required(VERSION 3.22.1)
project(nativevr)

add_library(nativevr SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(nativevr ${log-lib})