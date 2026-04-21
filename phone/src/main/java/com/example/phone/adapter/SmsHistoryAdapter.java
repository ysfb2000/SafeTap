package com.example.phone.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phone.R;
import com.example.shared.models.SmsHistory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsHistoryAdapter extends RecyclerView.Adapter<SmsHistoryAdapter.SmsViewHolder> {

    private List<SmsHistory> historyList;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public SmsHistoryAdapter(List<SmsHistory> historyList) {
        this.historyList = historyList;
    }

    public void setHistoryList(List<SmsHistory> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public SmsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sms_history, parent, false);
        return new SmsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SmsViewHolder holder, int position) {
        SmsHistory history = historyList.get(position);
        
        // Show only the contact name
        String recipient = history.getRecipient();
        holder.tvRecipient.setText(recipient != null ? recipient : "Unknown");
        
        holder.tvMessage.setText(history.getMessage());
        holder.tvTimestamp.setText(dateFormat.format(new Date(history.getTimestamp())));
    }

    // Return the number of items in the list
    @Override
    public int getItemCount() {
        return historyList.size();
    }

    // ViewHolder class for the RecyclerView
    public static class SmsViewHolder extends RecyclerView.ViewHolder {
        public TextView tvRecipient, tvMessage, tvTimestamp;

        public SmsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecipient = itemView.findViewById(R.id.tvRecipient);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
