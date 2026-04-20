package com.example.phone;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.HEART_RATE_ALARM_ENABLED_KEY;
import static com.example.shared.constants.Common.LAST_HEART_RATE_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.MessageChannels.CONTACTS_DATA_PATH;

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
import com.example.shared.constants.MessageChannels;
import com.example.shared.constants.Permission;
import com.example.shared.constants.Tag;
import com.example.shared.models.Contact;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
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
            button.setOnClickListener(v -> sendMessageToWatch(MessageChannels.HELLO_PATH, "Hello from phone"));
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
                    Permission.PERMISSION_REQUEST_SEND_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permission.PERMISSION_REQUEST_SEND_SMS) {
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
    }

    @Override
    protected void onPause() {
        super.onPause();
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
                Log.e(Tag.MainActivity, "Error sending message", e);
            }
        }).start();
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
                Log.d(Tag.MainActivity, "SMS sent to: " + contact.getPhone());
            } catch (Exception e) {
                Log.e(Tag.MainActivity, "Failed to send SMS to " + contact.getPhone(), e);
            }
        }
        Toast.makeText(this, "Message sent to all contacts", Toast.LENGTH_LONG).show();
    }
}
