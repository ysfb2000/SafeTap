package com.example.safetap.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.example.safetap.R;
import com.example.safetap.databinding.ActivityContactsBinding;
import com.example.shared.adapter.ContactsAdapter;
import com.example.shared.constants.Common;
import com.example.shared.constants.MessageChannels;
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
import java.util.concurrent.ExecutionException;

public class ContactsActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    private WearableRecyclerView recyclerView;
    private ContactsAdapter adapter;
    private List<Contact> contactList;

    ActivityContactsBinding contactsBinding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a binding object
        contactsBinding = ActivityContactsBinding.inflate(getLayoutInflater());
        setContentView(contactsBinding.getRoot());

        // Load contacts from phone
        requestContactsFromPhone();

        // Load contacts from shared preferences
        loadContactsFromPrefs();

        // Initialize the RecyclerView
        recyclerView = contactsBinding.contactsRecyclerView;

        // Set up the RecyclerView
        adapter = new ContactsAdapter(contactList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        //Update UI
        updateUI();
    }

    // Handle lifecycle onResume events
    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
    }

    // Handle lifecycle onPause events
    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
    }

    // Request contacts from the phone
    private void requestContactsFromPhone() {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), MessageChannels.GET_CONTACTS_PATH, new byte[0]);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Tag.WatchContactsActivity, getString(R.string.error_request_contacts), e);
            }
        }).start();
    }

    // Load contacts from shared preferences
    private void loadContactsFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences(Common.PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(Common.CONTACTS_KEY, null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        contactList = gson.fromJson(json, type);

        if (contactList == null) {
            contactList = new ArrayList<>();
        }
    }

    //Update UI for No Contacts Found
    private void updateUI() {
        if (contactList == null || contactList.isEmpty()) {
            contactsBinding.tvEmptyContacts.setVisibility(View.VISIBLE);
            contactsBinding.contactsRecyclerView.setVisibility(View.GONE);
        } else {
            contactsBinding.tvEmptyContacts.setVisibility(View.GONE);
            contactsBinding.contactsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    // Save contacts to shared preferences
    private void saveContactsToPrefs(String json) {
        SharedPreferences sharedPreferences = getSharedPreferences(Common.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Common.CONTACTS_KEY, json);
        editor.apply();
    }

    // Handle message events
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (MessageChannels.CONTACTS_DATA_PATH.equals(messageEvent.getPath())) {

            String json = new String(messageEvent.getData());
            saveContactsToPrefs(json);

            Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
            List<Contact> newContacts = new Gson().fromJson(json, type);

            if (newContacts == null) {
                newContacts = new ArrayList<>();
            }

            List<Contact> finalNewContacts = newContacts;

            runOnUiThread(() -> {
                contactList.clear();
                contactList.addAll(finalNewContacts);

                adapter.notifyDataSetChanged();

                updateUI(); // ✅ IMPORTANT FIX
            });
        }
    }
}
