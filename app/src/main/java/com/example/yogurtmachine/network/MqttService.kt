package com.example.yogurtmachine.network

import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.yogurtmachine.data.YogurtMachineState
import com.example.yogurtmachine.data.MachineStatus

class MqttService {
    private var mqttClient: MqttClient? = null
    private val _machineState = MutableStateFlow(YogurtMachineState())
    val machineState: StateFlow<YogurtMachineState> = _machineState

    companion object {
        private const val BROKER = "tcp://your-mqtt-broker:1883"
        private const val CLIENT_ID = "android_yogurt_app"
        private const val TOPIC_TEMPERATURE = "yogurt/temperature"
        private const val TOPIC_STATUS = "yogurt/status"
        private const val TOPIC_COMMAND = "yogurt/command"
    }

    fun connect() {
        try {
            mqttClient = MqttClient(BROKER, CLIENT_ID, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                // Add authentication if needed
                // userName = "your_username"
                // password = "your_password".toCharArray()
            }

            mqttClient?.connect(options)
            subscribeToTopics()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopics() {
        mqttClient?.subscribe(TOPIC_TEMPERATURE) { _, message ->
            val temperature = message.payload.toString().toFloatOrNull() ?: 0f
            updateState { copy(currentTemperature = temperature) }
        }

        mqttClient?.subscribe(TOPIC_STATUS) { _, message ->
            val status = try {
                MachineStatus.valueOf(message.payload.toString())
            } catch (e: IllegalArgumentException) {
                MachineStatus.ERROR
            }
            updateState { copy(status = status) }
        }
    }

    fun startProcess(targetTemp: Float, timeHours: Int) {
        val command = "START,$targetTemp,$timeHours"
        publishCommand(command)
    }

    fun stopProcess() {
        publishCommand("STOP")
    }

    private fun publishCommand(command: String) {
        try {
            mqttClient?.publish(TOPIC_COMMAND, MqttMessage(command.toByteArray()))
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun updateState(update: YogurtMachineState.() -> YogurtMachineState) {
        _machineState.value = _machineState.value.update()
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient = null
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }
} 