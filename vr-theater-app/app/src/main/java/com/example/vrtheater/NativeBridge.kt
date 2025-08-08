package com.example.vrtheater

object NativeBridge {
    init { System.loadLibrary("nativevr") }
    external fun hello(): String
}