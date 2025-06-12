package com.example.yogurtmachine.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.yogurtmachine.R;
import com.example.yogurtmachine.data.MachineStatus;
import com.example.yogurtmachine.network.MqttService;

public class MainActivity extends AppCompatActivity {
    private YogurtMachineViewModel viewModel;
    private TextView currentTempValue;
    private TextView statusText;
    private EditText targetTempInput;
    private EditText timeInput;
    private Button startButton;
    private Button stopButton;
    private MqttService mqttService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(YogurtMachineViewModel.class);
        mqttService = viewModel.getMqttService();

        // Initialize views
        currentTempValue = findViewById(R.id.currentTempValue);
        statusText = findViewById(R.id.statusText);
        targetTempInput = findViewById(R.id.targetTempInput);
        timeInput = findViewById(R.id.timeInput);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        // Set up click listeners
        startButton.setOnClickListener(v -> {
            if (!mqttService.isConnected()) {
                Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                float targetTemp = Float.parseFloat(targetTempInput.getText().toString());
                int timeHours = Integer.parseInt(timeInput.getText().toString());
                
                if (targetTemp < 0 || targetTemp > 100) {
                    Toast.makeText(this, "Nhiệt độ không hợp lệ (0-100°C)", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (timeHours < 1 || timeHours > 24) {
                    Toast.makeText(this, "Thời gian không hợp lệ (1-24 giờ)", Toast.LENGTH_SHORT).show();
                    return;
                }

                viewModel.startProcess(targetTemp, timeHours);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Vui lòng nhập số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        stopButton.setOnClickListener(v -> {
            if (!mqttService.isConnected()) {
                Toast.makeText(this, "Đang kết nối...", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.stopProcess();
        });

        // Observe ViewModel state
        viewModel.getUiState().observe(this, state -> {
            if (state != null) {
                updateUI(state);
            }
        });
    }

    private void updateUI(YogurtMachineState state) {
        // Update temperature display
        currentTempValue.setText(String.format("%.1f°C", state.getCurrentTemperature()));

        // Update status text
        String statusMessage = "Trạng thái: ";
        switch (state.getStatus()) {
            case IDLE:
                statusMessage += "Chưa bắt đầu";
                break;
            case RUNNING:
                statusMessage += "Đang chạy";
                break;
            case COMPLETED:
                statusMessage += "Hoàn thành";
                break;
            case TEMPERATURE_TOO_HIGH:
                statusMessage += "Nhiệt độ quá cao";
                break;
            case TEMPERATURE_TOO_LOW:
                statusMessage += "Nhiệt độ quá thấp";
                break;
            case ERROR:
                statusMessage += "Lỗi kết nối";
                break;
        }
        statusText.setText(statusMessage);

        // Update button states
        boolean isRunning = state.isRunning();
        startButton.setEnabled(!isRunning && mqttService.isConnected());
        stopButton.setEnabled(isRunning && mqttService.isConnected());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.onCleared();
    }
} 