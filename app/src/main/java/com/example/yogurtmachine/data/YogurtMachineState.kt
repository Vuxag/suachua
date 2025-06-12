package com.example.yogurtmachine.data

data class YogurtMachineState(
    val currentTemperature: Float = 0f,
    val targetTemperature: Float = 42f,
    val fermentationTime: Int = 8, // in hours
    val isRunning: Boolean = false,
    val status: MachineStatus = MachineStatus.IDLE
)

enum class MachineStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    TEMPERATURE_TOO_HIGH,
    TEMPERATURE_TOO_LOW,
    ERROR
} 