package com.example.yogurtmachine.data;

public class YogurtMachineState {
    private float currentTemperature;
    private float targetTemperature;
    private int fermentationTime;
    private boolean isRunning;
    private MachineStatus status;

    public YogurtMachineState() {
        this.currentTemperature = 0f;
        this.targetTemperature = 42f;
        this.fermentationTime = 8;
        this.isRunning = false;
        this.status = MachineStatus.IDLE;
    }

    public YogurtMachineState(float currentTemperature, float targetTemperature, 
                            int fermentationTime, boolean isRunning, MachineStatus status) {
        this.currentTemperature = currentTemperature;
        this.targetTemperature = targetTemperature;
        this.fermentationTime = fermentationTime;
        this.isRunning = isRunning;
        this.status = status;
    }

    // Getters and Setters
    public float getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(float currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public float getTargetTemperature() {
        return targetTemperature;
    }

    public void setTargetTemperature(float targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    public int getFermentationTime() {
        return fermentationTime;
    }

    public void setFermentationTime(int fermentationTime) {
        this.fermentationTime = fermentationTime;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public MachineStatus getStatus() {
        return status;
    }

    public void setStatus(MachineStatus status) {
        this.status = status;
    }
} 