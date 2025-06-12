package com.example.yogurtmachine.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.yogurtmachine.data.YogurtMachineState;
import com.example.yogurtmachine.network.MqttService;

public class YogurtMachineViewModel extends ViewModel {
    private final MqttService mqttService;
    private final MutableLiveData<YogurtMachineState> uiState;

    public YogurtMachineViewModel() {
        mqttService = new MqttService();
        uiState = new MutableLiveData<>(new YogurtMachineState());
        
        // Observe MQTT service state changes
        mqttService.getMachineState().observeForever(state -> {
            if (state != null) {
                uiState.setValue(state);
            }
        });
        
        connectToMqtt();
    }

    private void connectToMqtt() {
        mqttService.connect();
    }

    public LiveData<YogurtMachineState> getUiState() {
        return uiState;
    }

    public MqttService getMqttService() {
        return mqttService;
    }

    public void startProcess(float targetTemp, int timeHours) {
        mqttService.startProcess(targetTemp, timeHours);
    }

    public void stopProcess() {
        mqttService.stopProcess();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mqttService.disconnect();
    }
} 