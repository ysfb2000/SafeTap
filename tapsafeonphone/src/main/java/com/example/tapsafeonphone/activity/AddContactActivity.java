package com.example.tapsafeonphone.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tapsafeonphone.R;

public class AddContactActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etPhone;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        etName = findViewById(R.id.etName);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String rawPhone = etPhone.getText().toString().trim();

            if (!name.isEmpty() && !rawPhone.isEmpty()) {
                // Normalize phone number: Keep leading '+' and digits only
                String normalizedPhone = normalizePhoneNumber(rawPhone);
                
                Intent resultIntent = new Intent();
                resultIntent.putExtra("name", name);
                resultIntent.putExtra("phone", normalizedPhone);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private String normalizePhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        if (phone.startsWith("+")) {
            sb.append("+");
        }
        
        for (char c : phone.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
