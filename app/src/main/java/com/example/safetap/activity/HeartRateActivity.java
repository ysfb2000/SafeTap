package com.example.safetap.activity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import com.example.safetap.R;
import com.example.shared.models.Contact;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HeartRateActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "HeartRateActivity";
    private TextView tvHeartRate;
    private ImageView ivHeartIcon;
    private SwitchCompat switchSms;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String PREFS_NAME = "SafeTapPrefs";
    private static final String CONTACTS_KEY = "contacts_list";
    private static final String HEART_RATE_DATA_PATH = "/heart_rate_data";
    private static final String HEART_RATE_ALARM_ENABLED_PATH = "/heart_rate_alarm_enabled";
    
    private List<Contact> contactList;
    private boolean isSmsEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate);

        tvHeartRate = findViewById(R.id.tvHeartRate);
        ivHeartIcon = findViewById(R.id.ivHeartIcon);
        switchSms = findViewById(R.id.switchSms);
        
        findViewById(R.id.btnBackHeart).setOnClickListener(v -> finish());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        if (heartRateSensor == null) {
            tvHeartRate.setText("N/A");
            Toast.makeText(this, "Heart Rate sensor not available", Toast.LENGTH_SHORT).show();
        }

        switchSms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isSmsEnabled = isChecked;
            if (isChecked) {
                checkPermissions();
            }
            sendHeartRateAlarmStatusToPhone(isChecked);
        });

        loadContacts();
        checkPermissions();
    }

    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.SEND_SMS}, 
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (heartRateSensor != null) {
            sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            float heartRate = event.values[0];
            if (heartRate > 0) {
                String hrValue = String.valueOf((int) heartRate);
                tvHeartRate.setText(hrValue);
                animateHeart();
                sendHeartRateToPhone(hrValue);
            }
        }
    }

    private void sendHeartRateToPhone(String heartRate) {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), HEART_RATE_DATA_PATH, heartRate.getBytes());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending heart rate to phone", e);
            }
        }).start();
    }

    private void sendHeartRateAlarmStatusToPhone(boolean enabled) {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                String status = enabled ? "true" : "false";
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), HEART_RATE_ALARM_ENABLED_PATH, status.getBytes());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending heart rate alarm status to phone", e);
            }
        }).start();
    }

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

    private void loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CONTACTS_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
            contactList = gson.fromJson(json, type);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                switchSms.setChecked(false);
                Toast.makeText(this, "Permissions required for features", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
