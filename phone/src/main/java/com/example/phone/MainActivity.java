package com.example.phone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.example.shared.adapter.ContactsAdapter;
import com.example.shared.models.Contact;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "PhoneMainActivity";
    private static final String PATH = "/hello";
    private static final String GET_CONTACTS_PATH = "/get_contacts";
    private static final String CONTACTS_DATA_PATH = "/contacts_data";
    private static final String SEND_SOS_PATH = "/send_sos";
    private static final String SEND_LOCATION_SMS_PATH = "/send_location_sms";
    private static final String HEART_RATE_DATA_PATH = "/heart_rate_data";
    private static final String HEART_RATE_ALARM_ENABLED_PATH = "/heart_rate_alarm_enabled";
    
    private static final String PREFS_NAME = "SafeTapPrefs";
    private static final String CONTACTS_KEY = "contacts_list";
    private static final String HEART_RATE_ALARM_ENABLED_KEY = "heart_rate_alarm_enabled";
    private static final String LAST_HEART_RATE_KEY = "last_heart_rate";
    private static final int PERMISSION_REQUEST_SEND_SMS = 1;

    private WearableRecyclerView rvContacts;
    private ContactsAdapter adapter;
    private List<Contact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvContacts = findViewById(R.id.rvContacts);
        
        // Use WearableLinearLayoutManager for WearableRecyclerView
        rvContacts.setLayoutManager(new WearableLinearLayoutManager(this));
        rvContacts.setEdgeItemsCenteringEnabled(true);

        loadAndDisplayContacts();

        Button button = findViewById(R.id.button);
        if (button != null) {
            button.setOnClickListener(v -> sendMessageToWatch(PATH, "Hello from phone"));
        }

        Button btnAddContact = findViewById(R.id.btnAddContact);
        if (btnAddContact != null) {
            btnAddContact.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddContactActivity.class);
                startActivity(intent);
            });
        }

        checkSmsPermission();
    }

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST_SEND_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS Permission Denied. SOS will not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayContacts();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    private void loadAndDisplayContacts() {
        contactList = loadContacts();
        adapter = new ContactsAdapter(contactList);
        
        adapter.setOnContactLongClickListener(position -> {
            showDeleteConfirmationDialog(position);
        });
        
        rvContacts.setAdapter(adapter);
    }

    private void showDeleteConfirmationDialog(int position) {
        Contact contact = contactList.get(position);
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + contact.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactList.remove(position);
                    saveContacts(contactList);
                    adapter.notifyItemRemoved(position);
                    // Sync with watch after deletion
                    sendContactsToWatch();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private List<Contact> loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CONTACTS_KEY, null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> list = gson.fromJson(json, type);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    private void saveContacts(List<Contact> list) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(CONTACTS_KEY, json);
        editor.apply();
        // Sync with watch after saving
        sendContactsToWatch();
    }

    private void sendContactsToWatch() {
        List<Contact> contacts = loadContacts();
        String json = new Gson().toJson(contacts);
        sendMessageToWatch(CONTACTS_DATA_PATH, json);
    }

    private void sendMessageToWatch(String path, String message) {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Tasks.await(Wearable.getMessageClient(this)
                            .sendMessage(node.getId(), path, message.getBytes()));
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error sending message", e);
            }
        }).start();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (GET_CONTACTS_PATH.equals(messageEvent.getPath())) {
            sendContactsToWatch();
        } else if (SEND_SOS_PATH.equals(messageEvent.getPath())) {
            runOnUiThread(() -> {
                Toast.makeText(this, "SOS Request Received!", Toast.LENGTH_SHORT).show();
                sendSmsWithHeartRate("Emergency! I need help! This is an SOS message from SafeTap.");
            });
        } else if (SEND_LOCATION_SMS_PATH.equals(messageEvent.getPath())) {
            String message = new String(messageEvent.getData());
            runOnUiThread(() -> {
                Toast.makeText(this, "Location SMS Request Received!", Toast.LENGTH_SHORT).show();
                sendSmsWithHeartRate(message);
            });
        } else if (HEART_RATE_DATA_PATH.equals(messageEvent.getPath())) {
            String heartRate = new String(messageEvent.getData());
            saveHeartRate(heartRate);
        } else if (HEART_RATE_ALARM_ENABLED_PATH.equals(messageEvent.getPath())) {
            boolean enabled = new String(messageEvent.getData()).equals("true");
            saveHeartRateAlarmEnabled(enabled);
        }
    }

    private void saveHeartRate(String heartRate) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(LAST_HEART_RATE_KEY, heartRate).apply();
    }

    private void saveHeartRateAlarmEnabled(boolean enabled) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putBoolean(HEART_RATE_ALARM_ENABLED_KEY, enabled).apply();
    }

    private void sendSmsWithHeartRate(String message) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAlarmEnabled = sharedPreferences.getBoolean(HEART_RATE_ALARM_ENABLED_KEY, false);
        
        String finalMessage = message;
        if (isAlarmEnabled) {
            String lastHeartRate = sharedPreferences.getString(LAST_HEART_RATE_KEY, "N/A");
            finalMessage += "\nMy current heartbeat is: " + lastHeartRate + " bpm";
        }
        
        sendSmsToAllContacts(finalMessage);
    }

    private void sendSmsToAllContacts(String message) {
        List<Contact> contacts = loadContacts();
        if (contacts.isEmpty()) {
            Toast.makeText(this, "No contacts found to send message", Toast.LENGTH_SHORT).show();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        for (Contact contact : contacts) {
            try {
                smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);
                Log.d(TAG, "SMS sent to: " + contact.getPhone());
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SMS to " + contact.getPhone(), e);
            }
        }
        Toast.makeText(this, "Message sent to all contacts", Toast.LENGTH_LONG).show();
    }
}
