package com.example.phone.activity;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.Common.SMS_HISTORY_KEY;
import static com.example.shared.constants.MessageChannels.CONTACTS_DATA_PATH;
import static com.example.shared.constants.MessageChannels.GET_CONTACTS_PATH;
import static com.example.shared.constants.MessageChannels.SEND_LOCATION_SMS_PATH;
import static com.example.shared.constants.MessageChannels.SEND_SOS_PATH;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.example.phone.R;
import com.example.shared.constants.Tag;
import com.example.shared.models.Contact;
import com.example.shared.models.SmsHistory;
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


    @Override
    public void onCreate(){
        super.onCreate();
        // Toast.makeText(this, "Service created!", Toast.LENGTH_SHORT).show();
    }

    // Handle lifecycle onResume events
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        // Handle message events
        if (GET_CONTACTS_PATH.equals(messageEvent.getPath())) {
            sendContactsToWatch();
        } else if (SEND_SOS_PATH.equals(messageEvent.getPath())) {
            sendSosToAllContacts(new String(messageEvent.getData()));
        } else if (SEND_LOCATION_SMS_PATH.equals(messageEvent.getPath())) {
            sendLocationToAllContacts(new String(messageEvent.getData()));
        }
    }

    // Send location to all contacts
    private void sendLocationToAllContacts(String message) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(CONTACTS_KEY, "[]");
        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();

        List<Contact> contactList = gson.fromJson(json, type);

        if (contactList == null || contactList.isEmpty()) {
            Log.w(Tag.DataLayerListenerService,  getString(R.string.log_no_contacts_location));
            return;
        }

        // Send location to all contacts
        SmsManager smsManager = SmsManager.getDefault();
        for (Contact contact : contactList) {
            try {
                smsManager.sendTextMessage(contact.getFullPhone(), null, message, null, null);
                Log.d(Tag.DataLayerListenerService, getString(R.string.log_location_sent, contact.getFullPhone()));
                saveSmsToHistory(contact.getName(), message);
            } catch (Exception e) {
                Log.e(Tag.DataLayerListenerService, getString(R.string.log_location_failed, contact.getFullPhone()), e);
            }
        }

        // Show a toast message
        Toast.makeText(this, getString(R.string.location_sent_all), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, getString(R.string.sms_prefix) + message, Toast.LENGTH_LONG).show();
    }

    private void saveSmsToHistory(String recipient, String message) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(SMS_HISTORY_KEY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<SmsHistory>>() {}.getType();
        List<SmsHistory> historyList = gson.fromJson(json, type);
        if (historyList == null) {
            historyList = new ArrayList<>();
        }
        historyList.add(new SmsHistory(recipient, message, System.currentTimeMillis()));
        sharedPreferences.edit().putString(SMS_HISTORY_KEY, gson.toJson(historyList)).apply();
    }

    // Send contacts to the watch
    private void sendContactsToWatch() {
        // Retrieve contacts from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(CONTACTS_KEY, "[]");

        // Send the contacts to the watch
        new Thread(() -> {
            try {
                // get a list of connected nodes
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());

                // iterate through the nodes and send a message to each one
                for (Node node : nodes) {
                    Tasks.await(Wearable.getMessageClient(this).sendMessage(node.getId(), CONTACTS_DATA_PATH, json.getBytes()));
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(Tag.DataLayerListenerService, getString(R.string.log_error_sending_contacts), e);
            }
        }).start();
    }

    // Send SOS to all contacts
    private void sendSosToAllContacts(String message) {
        // Retrieve contacts from shared preferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(CONTACTS_KEY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contactList = gson.fromJson(json, type);

        if (contactList == null || contactList.isEmpty()) {
            Log.w(Tag.DataLayerListenerService, getString(R.string.log_no_contacts_sos));
            return;
        }

        // Send SOS to all contacts
        SmsManager smsManager = SmsManager.getDefault();
        for (Contact contact : contactList) {
            try {
                smsManager.sendTextMessage(contact.getFullPhone(), null, message, null, null);
                Log.d(Tag.DataLayerListenerService, getString(R.string.log_sos_sent, contact.getFullPhone()));
                saveSmsToHistory(contact.getName(), message);
            } catch (Exception e) {
                Log.e(Tag.DataLayerListenerService, getString(R.string.log_sos_failed, contact.getFullPhone()), e);
            }
        }

        // Show a toast message
        Toast.makeText(this, getString(R.string.sos_sent_all), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, getString(R.string.sms_prefix) + message, Toast.LENGTH_LONG).show();
    }
}
