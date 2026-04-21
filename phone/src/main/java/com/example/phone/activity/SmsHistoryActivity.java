package com.example.phone.activity;

import static com.example.shared.constants.Common.PREFS_NAME;
import static com.example.shared.constants.Common.SMS_HISTORY_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phone.R;
import com.example.phone.adapter.SmsHistoryAdapter;
import com.example.phone.databinding.ActivitySmsHistoryBinding;
import com.example.shared.models.SmsHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmsHistoryActivity extends AppCompatActivity {

    private ActivitySmsHistoryBinding binding;
    private SmsHistoryAdapter adapter;
    private List<SmsHistory> historyList;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySmsHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup Back Button
        binding.btnBack.setOnClickListener(v -> finish());

        loadHistory();

        adapter = new SmsHistoryAdapter(historyList);
        binding.rvSmsHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSmsHistory.setAdapter(adapter);

        // Add Swipe to Delete
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
                        .setTitle(getString(R.string.delete_history_title))
                        .setMessage(getString(R.string.delete_history_message))
                        .setPositiveButton(getString(R.string.delete_button), (dialog, which) -> {

                            historyList.remove(position);
                            saveHistory();
                            adapter.notifyItemRemoved(position);
                            updateUI();

                        })
                        .setNegativeButton(getString(R.string.cancel_button), (dialog, which) -> {

                            adapter.notifyItemChanged(position);

                        })
                        .setCancelable(false)
                        .show();
            }
        }).attachToRecyclerView(binding.rvSmsHistory);

        // Listen for history changes (e.g. when an SMS is sent from the background service)
        listener = (sharedPreferences, key) -> {
            if (SMS_HISTORY_KEY.equals(key)) {
                runOnUiThread(() -> {
                    loadHistory();
                    adapter.setHistoryList(historyList);
                    adapter.notifyDataSetChanged();
                    updateUI();
                });
            }
        };
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(listener);

        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private void loadHistory() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(SMS_HISTORY_KEY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<SmsHistory>>() {}.getType();
        historyList = gson.fromJson(json, type);

        if (historyList == null) {
            historyList = new ArrayList<>();
        }

        // Show newest first
        Collections.sort(historyList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
    }

    private void saveHistory() {
        // Unregister listener temporarily to avoid infinite loop or redundant updates
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(historyList);
        editor.putString(SMS_HISTORY_KEY, json);
        editor.apply();
        
        // Re-register listener
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void updateUI() {
        if (historyList.isEmpty()) {
            binding.tvEmptyHistory.setVisibility(View.VISIBLE);
            binding.rvSmsHistory.setVisibility(View.GONE);
        } else {
            binding.tvEmptyHistory.setVisibility(View.GONE);
            binding.rvSmsHistory.setVisibility(View.VISIBLE);
        }
    }
}
