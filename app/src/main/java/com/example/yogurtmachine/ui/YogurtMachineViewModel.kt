package com.example.yogurtmachine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.yogurtmachine.data.YogurtMachineState
import com.example.yogurtmachine.network.MqttService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class YogurtMachineViewModel : ViewModel() {
    private val mqttService = MqttService()
    
    private val _uiState = MutableStateFlow(YogurtMachineState())
    val uiState: StateFlow<YogurtMachineState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            mqttService.machineState.collect { state ->
                _uiState.value = state
            }
        }
        connectToMqtt()
    }

    private fun connectToMqtt() {
        mqttService.connect()
    }

    fun startProcess(targetTemp: Float, timeHours: Int) {
        mqttService.startProcess(targetTemp, timeHours)
    }

    fun stopProcess() {
        mqttService.stopProcess()
    }

    override fun onCleared() {
        super.onCleared()
        mqttService.disconnect()
    }
} 