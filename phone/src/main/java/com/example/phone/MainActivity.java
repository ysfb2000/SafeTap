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
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.example.phone.databinding.ActivityMainBinding;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding binding;
    private ContactsAdapter adapter;
    private List<Contact> contactList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a binding object
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Use WearableLinearLayoutManager for WearableRecyclerView
        binding.rvContacts.setLayoutManager(new WearableLinearLayoutManager(this));
        binding.rvContacts.setEdgeItemsCenteringEnabled(true);

        // Load and display contacts
        loadAndDisplayContacts();

        // Initialize the listener
        initListener();

        // Check for SMS permission
        checkSmsPermission();
    }

    // Initialize the listener
    private void initListener() {
        binding.btnHello.setOnClickListener(this);
        binding.btnAddContact.setOnClickListener(this);
    }

    // Check for SMS permission
    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    Permission.PERMISSION_REQUEST_SEND_SMS);
        }
    }

    // Handle permission result
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

    // Handle lifecycle onResume events
    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayContacts();
    }

    // load and display contacts
    private void loadAndDisplayContacts() {
        contactList = loadContacts();
        adapter = new ContactsAdapter(contactList);

        // Handle long click on a contact
        adapter.setOnContactLongClickListener(position -> {
            showDeleteConfirmationDialog(position);
        });
        
        binding.rvContacts.setAdapter(adapter);
    }

    // Show a confirmation dialog before deleting a contact
    private void showDeleteConfirmationDialog(int position) {
        Contact contact = contactList.get(position);

        // Show a confirmation dialog before deleting a contact
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

    // Load contacts from shared preferences
    private List<Contact> loadContacts() {
        // Load contacts from shared preferences
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

    // Save contacts to shared preferences
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

    // Send contacts to the watch
    private void sendContactsToWatch() {
        List<Contact> contacts = loadContacts();
        String json = new Gson().toJson(contacts);
        sendMessageToWatch(CONTACTS_DATA_PATH, json);
    }

    // Send a message to the watch
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


    // Handle button clicks
    @Override
    public void onClick(View view) {
        int id = view.getId();

        // Handle button clicks
        if (id == R.id.btnHello) {
            sendMessageToWatch(MessageChannels.HELLO_PATH, "Hello from phone");
        }

        // Handle button clicks
        if (id == R.id.btnAddContact) {
            Intent intent = new Intent(MainActivity.this, AddContactActivity.class);
            startActivity(intent);
        }

    }
}
