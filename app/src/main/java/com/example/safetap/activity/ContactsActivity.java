package com.example.safetap.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.example.safetap.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        recyclerView = findViewById(R.id.contactsRecyclerView);
        
        loadContactsFromPrefs();
        
        adapter = new ContactsAdapter(contactList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);
        recyclerView.setLayoutManager(new WearableLinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        requestContactsFromPhone();
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

    private void requestContactsFromPhone() {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Wearable.getMessageClient(this).sendMessage(node.getId(), MessageChannels.GET_CONTACTS_PATH, new byte[0]);
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(Tag.WatchContactsActivity, "Error requesting contacts", e);
            }
        }).start();
    }

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

    private void saveContactsToPrefs(String json) {
        SharedPreferences sharedPreferences = getSharedPreferences(Common.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Common.CONTACTS_KEY, json);
        editor.apply();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (MessageChannels.CONTACTS_DATA_PATH.equals(messageEvent.getPath())) {
            String json = new String(messageEvent.getData());
            saveContactsToPrefs(json);
            
            Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
            List<Contact> newContacts = new Gson().fromJson(json, type);
            
            runOnUiThread(() -> {
                contactList.clear();
                contactList.addAll(newContacts);
                adapter.notifyDataSetChanged();
            });
        }
    }
}
