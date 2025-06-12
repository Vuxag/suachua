package com.example.yogurtmachine.network;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.example.yogurtmachine.data.YogurtMachineState;
import com.example.yogurtmachine.data.MachineStatus;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttService {
    private MqttClient mqttClient;
    private final MutableLiveData<YogurtMachineState> machineState = new MutableLiveData<>(new YogurtMachineState());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isConnected = false;

    private static final String BROKER = "tcp://your-mqtt-broker:1883";
    private static final String CLIENT_ID = "android_yogurt_app";
    private static final String TOPIC_TEMPERATURE = "yogurt/temperature";
    private static final String TOPIC_STATUS = "yogurt/status";
    private static final String TOPIC_COMMAND = "yogurt/command";

    public LiveData<YogurtMachineState> getMachineState() {
        return machineState;
    }

    public void connect() {
        executor.execute(() -> {
            try {
                if (mqttClient != null && mqttClient.isConnected()) {
                    return;
                }

                mqttClient = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(30);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                
                // Add authentication if needed
                // options.setUserName("your_username");
                // options.setPassword("your_password".toCharArray());

                mqttClient.connect(options);
                isConnected = true;
                subscribeToTopics();
                
                // Update UI on main thread
                mainHandler.post(() -> {
                    YogurtMachineState state = machineState.getValue();
                    if (state != null) {
                        state.setStatus(MachineStatus.IDLE);
                        machineState.setValue(state);
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
                isConnected = false;
                mainHandler.post(() -> {
                    YogurtMachineState state = machineState.getValue();
                    if (state != null) {
                        state.setStatus(MachineStatus.ERROR);
                        machineState.setValue(state);
                    }
                });
            }
        });
    }

    private void subscribeToTopics() {
        try {
            mqttClient.subscribe(TOPIC_TEMPERATURE, (topic, message) -> {
                executor.execute(() -> {
                    float temperature = 0f;
                    try {
                        temperature = Float.parseFloat(new String(message.getPayload()));
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    updateState(state -> {
                        state.setCurrentTemperature(temperature);
                        return state;
                    });
                });
            });

            mqttClient.subscribe(TOPIC_STATUS, (topic, message) -> {
                executor.execute(() -> {
                    MachineStatus status;
                    try {
                        status = MachineStatus.valueOf(new String(message.getPayload()));
                    } catch (IllegalArgumentException e) {
                        status = MachineStatus.ERROR;
                    }
                    updateState(state -> {
                        state.setStatus(status);
                        return state;
                    });
                });
            });
        } catch (MqttException e) {
            e.printStackTrace();
            isConnected = false;
            mainHandler.post(() -> {
                YogurtMachineState state = machineState.getValue();
                if (state != null) {
                    state.setStatus(MachineStatus.ERROR);
                    machineState.setValue(state);
                }
            });
        }
    }

    public void startProcess(float targetTemp, int timeHours) {
        if (!isConnected) {
            return;
        }
        executor.execute(() -> {
            String command = String.format("START,%.1f,%d", targetTemp, timeHours);
            publishCommand(command);
        });
    }

    public void stopProcess() {
        if (!isConnected) {
            return;
        }
        executor.execute(() -> publishCommand("STOP"));
    }

    private void publishCommand(String command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(TOPIC_COMMAND, new MqttMessage(command.getBytes()));
            }
        } catch (MqttException e) {
            e.printStackTrace();
            isConnected = false;
            mainHandler.post(() -> {
                YogurtMachineState state = machineState.getValue();
                if (state != null) {
                    state.setStatus(MachineStatus.ERROR);
                    machineState.setValue(state);
                }
            });
        }
    }

    private void updateState(StateUpdater updater) {
        YogurtMachineState currentState = machineState.getValue();
        if (currentState != null) {
            YogurtMachineState newState = updater.update(currentState);
            mainHandler.post(() -> machineState.setValue(newState));
        }
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (mqttClient != null && mqttClient.isConnected()) {
                    mqttClient.disconnect();
                    mqttClient = null;
                }
                isConnected = false;
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean isConnected() {
        return isConnected;
    }

    @FunctionalInterface
    private interface StateUpdater {
        YogurtMachineState update(YogurtMachineState state);
    }
} 