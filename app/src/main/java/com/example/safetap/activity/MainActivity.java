package com.example.safetap.activity;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.MessageChannels.CONTACTS_DATA_PATH;
import static com.example.shared.constants.MessageChannels.GET_CONTACTS_PATH;
import static com.example.shared.constants.MessageChannels.HELLO_PATH;
import static com.example.shared.constants.MessageChannels.SEND_SOS_PATH;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.example.safetap.R;
import com.example.safetap.databinding.ActivityMainBinding;
import com.example.shared.constants.Tag;
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

public class MainActivity extends ComponentActivity implements MessageClient.OnMessageReceivedListener, View.OnClickListener {

    ActivityMainBinding mainBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a binding object
        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        requestContactsFromPhone();

        initListener();
    }

    // Initialize the listener
    private void initListener() {
        mainBinding.btnGps.setOnClickListener(this);
        mainBinding.btnContact.setOnClickListener(this);
        mainBinding.btnSos.setOnClickListener(this);
        mainBinding.btnHeartbeat.setOnClickListener(this);


        // Set up long click listener for the SOS button
        mainBinding.btnSos.setOnLongClickListener(v -> {
            sendSosViaPhone();
            return true;
        });
    }

    // Request contacts from the phone by message
    private void requestContactsFromPhone() {
        new Thread(() -> {
            try {
                // Get a list of connected nodes
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());

                // iterate through the nodes and send a message to each one
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), GET_CONTACTS_PATH, new byte[0]);
                }

            } catch (Exception e) {
                Log.e(Tag.MainActivity, "Error requesting contacts from phone", e);
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

                String message = "Emergency! I need help! This is an SOS message from SafeTap.";

                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), SEND_SOS_PATH, message.getBytes());
                }

                runOnUiThread(() -> Toast.makeText(this, "SOS request sent to phone", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e(Tag.MainActivity, "Error sending SOS request to phone", e);
                runOnUiThread(() -> Toast.makeText(this, "Error sending SOS request", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void saveContacts(String json) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(CONTACTS_KEY, json).apply();
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
        if (HELLO_PATH.equals(messageEvent.getPath())) {
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

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btnGps) {
            Intent intent = new Intent(MainActivity.this, GpsActivity.class);
            startActivity(intent);
        }

        if (id == R.id.btnContact) {
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        }

        if (id == R.id.btnSos) {
            Toast.makeText(this, "Long press to send SOS", Toast.LENGTH_SHORT).show();
        }

        if (id == R.id.btnHeartbeat) {
            Intent intent = new Intent(MainActivity.this, HeartRateActivity.class);
            startActivity(intent);
        }
    }


}
