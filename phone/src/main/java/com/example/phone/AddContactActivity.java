package com.example.phone;

import static com.example.shared.constants.Common.CONTACTS_KEY;
import static com.example.shared.constants.Common.PREFS_NAME;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.phone.databinding.ActivityAddContactBinding;
import com.example.shared.models.Contact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        // Check if any field is empty
        if (name.isEmpty() || countryCode.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the contact
        List<Contact> contactList = loadContacts();
        contactList.add(new Contact(name, countryCode, phoneNumber));
        saveContacts(contactList);

        Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
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