package com.example.safetap.activity;

import static com.example.shared.constants.MessageChannels.HEART_RATE_DATA_PATH;
import static com.example.shared.constants.Permission.PERMISSION_REQUEST_CODE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.safetap.R;
import com.example.safetap.databinding.ActivityHeartRateBinding;
import com.example.shared.constants.Tag;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class HeartRateActivity extends AppCompatActivity implements SensorEventListener {

    private TextView tvHeartRate;
    private ImageView ivHeartIcon;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;

    ActivityHeartRateBinding heartRateBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a binding object
        heartRateBinding = ActivityHeartRateBinding.inflate(getLayoutInflater());
        setContentView(heartRateBinding.getRoot());

        // Initialize the views
        tvHeartRate = heartRateBinding.tvHeartRate;
        ivHeartIcon = heartRateBinding.ivHeartIcon;

        // Initialize the sensor manager and heart rate sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        // Check if the heart rate sensor is available
        if (heartRateSensor == null) {
            tvHeartRate.setText("N/A");
            Toast.makeText(this, "Heart Rate sensor not available", Toast.LENGTH_SHORT).show();
        }

        // Request permissions
        checkPermissions();
    }

    // Check for permissions
    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BODY_SENSORS},
                    PERMISSION_REQUEST_CODE);
        }
    }

    // Handle lifecycle onResume events
    @Override
    protected void onResume() {
        super.onResume();
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    // Handle lifecycle onPause events
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    // Handle sensor events
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            float heartRate = event.values[0];
            if (heartRate > 0) {
                String hrValue = String.valueOf((int) heartRate);
                tvHeartRate.setText(hrValue);
                animateHeart();
            }
        }
    }


    // Animate the heart icon
    private void animateHeart() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f,
                1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(200);
        scaleAnimation.setRepeatCount(1);
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        ivHeartIcon.startAnimation(scaleAnimation);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permissions required for features", Toast.LENGTH_SHORT).show();
            }
        }
    }
}