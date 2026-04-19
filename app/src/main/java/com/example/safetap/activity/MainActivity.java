package com.example.safetap.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.example.safetap.R;
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

public class MainActivity extends ComponentActivity implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "MainActivity";
    private static final String PATH = "/hello";
    private static final String GET_CONTACTS_PATH = "/get_contacts";
    private static final String CONTACTS_DATA_PATH = "/contacts_data";
    private static final String SEND_SOS_PATH = "/send_sos";
    private static final String PREFS_NAME = "SafeTapPrefs";
    private static final String CONTACTS_KEY = "contacts_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnGps).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GpsActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.button2).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnSos).setOnClickListener(v -> {
            Toast.makeText(this, "Long press to send SOS", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSos).setOnLongClickListener(v -> {
            sendSosViaPhone();
            return true;
        });
        
        findViewById(R.id.button4).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HeartRateActivity.class);
            startActivity(intent);
        });

        requestContactsFromPhone();
    }

    private void requestContactsFromPhone() {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), GET_CONTACTS_PATH, new byte[0]);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error requesting contacts from phone", e);
            }
        }).start();
    }

    private void sendSosViaPhone() {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                if (nodes.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No phone connected", Toast.LENGTH_SHORT).show());
                    return;
                }
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), SEND_SOS_PATH, new byte[0]);
                }
                runOnUiThread(() -> Toast.makeText(this, "SOS request sent to phone", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Error sending SOS request to phone", e);
                runOnUiThread(() -> Toast.makeText(this, "Error sending SOS request", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void saveContacts(String json) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(CONTACTS_KEY, json).apply();
    }

    private List<Contact> loadContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CONTACTS_KEY, null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contactList = gson.fromJson(json, type);

        if (contactList == null) {
            contactList = new ArrayList<>();
        }
        return contactList;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (PATH.equals(messageEvent.getPath())) {
            String message = new String(messageEvent.getData());

            runOnUiThread(() ->
                    // show the message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    );
        } else if (CONTACTS_DATA_PATH.equals(messageEvent.getPath())) {
            String json = new String(messageEvent.getData());
            saveContacts(json);
        }
    }
}
