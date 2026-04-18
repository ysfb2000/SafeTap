package com.example.tapsafeonphone.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tapsafeonphone.R;
import com.example.tapsafeonphone.models.Contact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SafeTapPrefs";
    private static final String CONTACTS_KEY = "contacts_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnGps).setOnClickListener(v -> {
            // Intent intent = new Intent(MainActivity.this, GpsActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "GPS Activity not implemented on phone yet", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.button2).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ContactsActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnSos).setOnClickListener(v -> {
            Toast.makeText(this, "Long press to send SOS", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btnSos).setOnLongClickListener(v -> {
            sendSosSmsToAllContacts();
            return true;
        });
        
        findViewById(R.id.button4).setOnClickListener(v -> {
            // Intent intent = new Intent(MainActivity.this, HeartRateActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Heart Rate Activity not implemented on phone yet", Toast.LENGTH_SHORT).show();
        });
    }

    private void sendSosSmsToAllContacts() {
        List<Contact> contactList = loadContacts();
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No emergency contacts found", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder phoneNumbers = new StringBuilder();
        for (int i = 0; i < contactList.size(); i++) {
            phoneNumbers.append(contactList.get(i).getPhone());
            if (i < contactList.size() - 1) {
                phoneNumbers.append(";");
            }
        }

        String message = "Emergency! I need help! This is an SOS message from SafeTap.";
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + phoneNumbers.toString()));
        intent.putExtra("sms_body", message);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open SMS app", Toast.LENGTH_SHORT).show();
        }
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
}
