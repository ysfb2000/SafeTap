package com.example.phone.activity;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.MessageChannels.CONTACTS_DATA_PATH;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phone.R;
import com.example.phone.databinding.ActivityAddContactBinding;
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
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class AddContactActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityAddContactBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a binding object
        binding = ActivityAddContactBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSave.setOnClickListener(v -> saveContact());
        binding.btnCancel.setOnClickListener(v -> finish());
    }

    // Initialize the listener
    private void initListener() {
        binding.btnSave.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);
    }

    // Save a contact
    private void saveContact() {
        String name = Objects.requireNonNull(binding.etName.getText()).toString().trim();
        String countryCode = Objects.requireNonNull(binding.etCountryCode.getText()).toString().trim();
        String phoneNumber = Objects.requireNonNull(binding.etPhone.getText()).toString().trim();

        // Empty field check
        if (name.isEmpty() || countryCode.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        // Country code validation (max 3 digits, numbers only)
        if (!countryCode.matches("\\d{1,3}")) {
            Toast.makeText(this, getString(R.string.invalid_country_code), Toast.LENGTH_SHORT).show();
            return;
        }

        // Phone number validation (7–15 digits, numbers only)
        if (!phoneNumber.matches("\\d{7,15}")) {
            Toast.makeText(this, getString(R.string.invalid_phone_number), Toast.LENGTH_SHORT).show();
            return;
        }

        // Save contact
        List<Contact> contactList = loadContacts();
        contactList.add(new Contact(name, countryCode, phoneNumber));
        saveContacts(contactList);

        Toast.makeText(this, getString(R.string.contact_saved), Toast.LENGTH_SHORT).show();
        finish();
    }

    // Load contacts from shared preferences
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

    // Save contacts to shared preferences
    private void saveContacts(List<Contact> contactList) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contactList);
        editor.putString(CONTACTS_KEY, json);
        editor.apply();

        // Sync with watch after saving
        sendContactsToWatch(json);
    }

    // Send contacts to the watch
    private void sendContactsToWatch(String json) {
        new Thread(() -> {
            try {
                List<Node> nodes = Tasks.await(Wearable.getNodeClient(this).getConnectedNodes());
                for (Node node : nodes) {
                    Tasks.await(Wearable.getMessageClient(this)
                            .sendMessage(node.getId(), CONTACTS_DATA_PATH, json.getBytes()));
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
        if (id == R.id.btnSave) {
            saveContact();
        }

        if (id == R.id.btnCancel) {
            finish();
        }


    }
}