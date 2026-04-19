package com.example.phone;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;

import com.example.shared.models.Contact;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerService";
    private static final String GET_CONTACTS_PATH = "/get_contacts";
    private static final String CONTACTS_DATA_PATH = "/contacts_data";
    private static final String SEND_SOS_PATH = "/send_sos";
    private static final String PREFS_NAME = "SafeTapPrefs";
    private static final String CONTACTS_KEY = "contacts_list";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (GET_CONTACTS_PATH.equals(messageEvent.getPath())) {
            sendContactsToWatch();
        } else if (SEND_SOS_PATH.equals(messageEvent.getPath())) {
            sendSosToAllContacts();
        }
    }

    private void sendContactsToWatch() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(CONTACTS_KEY, "[]");

        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Tasks.await(Wearable.getMessageClient(this)
                            .sendMessage(node.getId(), CONTACTS_DATA_PATH, json.getBytes()));
                }
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error sending contacts to watch", e);
            }
        }).start();
    }

    private void sendSosToAllContacts() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(CONTACTS_KEY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contactList = gson.fromJson(json, type);

        if (contactList == null || contactList.isEmpty()) {
            Log.w(TAG, "No contacts found to send SOS");
            return;
        }

        String message = "Emergency! I need help! This is an SOS message from SafeTap.";
        SmsManager smsManager = SmsManager.getDefault();
        for (Contact contact : contactList) {
            try {
                smsManager.sendTextMessage(contact.getPhone(), null, message, null, null);
                Log.d(TAG, "SOS sent to: " + contact.getPhone());
            } catch (Exception e) {
                Log.e(TAG, "Failed to send SOS to " + contact.getPhone(), e);
            }
        }
    }
}
