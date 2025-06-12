package com.example.yogurtmachine.network;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.example.yogurtmachine.data.YogurtMachineState;
import com.example.yogurtmachine.data.MachineStatus;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqttService {
    private static final String TAG = "MqttService";
    private MqttClient mqttClient;
    private final MutableLiveData<YogurtMachineState> machineState = new MutableLiveData<>(new YogurtMachineState());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 5000;

    private static final String BROKER = "tcp://your-mqtt-broker:1883";
    private static final String CLIENT_ID = "android_yogurt_app";
    private static final String TOPIC_TEMPERATURE = "yogurt/temperature";
    private static final String TOPIC_STATUS = "yogurt/status";
    private static final String TOPIC_COMMAND = "yogurt/command";

    public LiveData<YogurtMachineState> getMachineState() {
        return machineState;
    }

    public void connect() {
        if (isConnecting.get() || isConnected.get()) {
            Log.d(TAG, "Already connecting or connected");
            return;
        }

        isConnecting.set(true);
        executor.execute(() -> {
            try {
                Log.d(TAG, "Connecting to MQTT broker...");
                if (mqttClient != null && mqttClient.isConnected()) {
                    Log.d(TAG, "Already connected");
                    return;
                }

                mqttClient = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setConnectionTimeout(30);
                options.setKeepAliveInterval(60);
                options.setAutomaticReconnect(true);
                options.setMaxReconnectDelay(5000);
                
                // Add authentication if needed
                // options.setUserName("your_username");
                // options.setPassword("your_password".toCharArray());

                mqttClient.connect(options);
                isConnected.set(true);
                reconnectAttempts = 0;
                Log.d(TAG, "Connected successfully");
                
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
                Log.e(TAG, "Connection failed: " + e.getMessage());
                handleConnectionError();
            } finally {
                isConnecting.set(false);
            }
        });
    }

    private void handleConnectionError() {
        isConnected.set(false);
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            Log.d(TAG, "Attempting to reconnect... Attempt " + reconnectAttempts);
            mainHandler.postDelayed(this::connect, RECONNECT_DELAY_MS);
        } else {
            Log.e(TAG, "Max reconnection attempts reached");
            mainHandler.post(() -> {
                YogurtMachineState state = machineState.getValue();
                if (state != null) {
                    state.setStatus(MachineStatus.ERROR);
                    machineState.setValue(state);
                }
            });
        }
    }

    private void subscribeToTopics() {
        try {
            Log.d(TAG, "Subscribing to topics...");
            mqttClient.subscribe(TOPIC_TEMPERATURE, (topic, message) -> {
                executor.execute(() -> {
                    float temperature = 0f;
                    try {
                        temperature = Float.parseFloat(new String(message.getPayload()));
                        Log.d(TAG, "Received temperature: " + temperature);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid temperature format: " + e.getMessage());
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
                        Log.d(TAG, "Received status: " + status);
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Invalid status format: " + e.getMessage());
                        status = MachineStatus.ERROR;
                    }
                    updateState(state -> {
                        state.setStatus(status);
                        return state;
                    });
                });
            });
            Log.d(TAG, "Subscribed to topics successfully");
        } catch (MqttException e) {
            Log.e(TAG, "Subscription failed: " + e.getMessage());
            handleConnectionError();
        }
    }

    public void startProcess(float targetTemp, int timeHours) {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot start process: Not connected");
            return;
        }
        executor.execute(() -> {
            String command = String.format("START,%.1f,%d", targetTemp, timeHours);
            Log.d(TAG, "Sending start command: " + command);
            publishCommand(command);
        });
    }

    public void stopProcess() {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot stop process: Not connected");
            return;
        }
        executor.execute(() -> {
            Log.d(TAG, "Sending stop command");
            publishCommand("STOP");
        });
    }

    private void publishCommand(String command) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish(TOPIC_COMMAND, new MqttMessage(command.getBytes()));
                Log.d(TAG, "Command published successfully: " + command);
            } else {
                Log.w(TAG, "Cannot publish command: Not connected");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Failed to publish command: " + e.getMessage());
            handleConnectionError();
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
                    Log.d(TAG, "Disconnecting...");
                    mqttClient.disconnect();
                    mqttClient = null;
                }
                isConnected.set(false);
                Log.d(TAG, "Disconnected successfully");
            } catch (MqttException e) {
                Log.e(TAG, "Disconnect failed: " + e.getMessage());
            }
        });
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void shutdown() {
        Log.d(TAG, "Shutting down MQTT service...");
        disconnect();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface StateUpdater {
        YogurtMachineState update(YogurtMachineState state);
    }
} 