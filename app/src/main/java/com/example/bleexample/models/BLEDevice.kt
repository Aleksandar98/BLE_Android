package com.example.bleexample.models


data class BLEDevice(
    var deviceName: String,
    var deviceMacAddress: String,
    var deviceSignal: Int
)
