package com.example.phone.activity;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.MessageChannels.CONTACTS_DATA_PATH;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.example.phone.R;
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

        // Initialize swipe to delete
        initSwipeToDelete();

        // Initialize the listener
        initListener();

        // Check for SMS permission
        checkSmsPermission();
    }

    private void initSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (position == RecyclerView.NO_POSITION) return;

                new androidx.appcompat.app.AlertDialog.Builder(viewHolder.itemView.getContext())
                        .setTitle("Delete Contact")
                        .setMessage("Are you sure you want to delete this contact?")
                        .setPositiveButton("Delete", (dialog, which) -> {

                            contactList.remove(position);
                            saveContacts(contactList);
                            adapter.notifyItemRemoved(position);

                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {

                            adapter.notifyItemChanged(position); // restore item if cancelled

                        })
                        .setCancelable(false)
                        .show();
            }
        }).attachToRecyclerView(binding.rvContacts);
    }

    // Initialize the listener
    private void initListener() {
        binding.btnHello.setOnClickListener(this);
        binding.btnAddContact.setOnClickListener(this);
        binding.btnSmsHistory.setOnClickListener(this);
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
                Toast.makeText(this,  getString(R.string.sms_permission_granted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.sms_permission_denied), Toast.LENGTH_LONG).show();
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
        binding.rvContacts.setAdapter(adapter);
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
                Log.e(Tag.MainActivity, getString(R.string.error_sending_msg), e);
            }
        }).start();
    }


    // Handle button clicks
    @Override
    public void onClick(View view) {
        int id = view.getId();

        // Handle button clicks
        if (id == R.id.btnHello) {
            sendMessageToWatch(MessageChannels.HELLO_PATH, getString(R.string.hello_from_phone));
        }

        // Handle button clicks
        if (id == R.id.btnAddContact) {
            Intent intent = new Intent(MainActivity.this, AddContactActivity.class);
            startActivity(intent);
        }

        if (id == R.id.btnSmsHistory) {
            Intent intent = new Intent(MainActivity.this, SmsHistoryActivity.class);
            startActivity(intent);
        }

    }
}
