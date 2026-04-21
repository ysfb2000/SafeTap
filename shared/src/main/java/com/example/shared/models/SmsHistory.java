package com.example.shared.models;

public class SmsHistory {
    private String recipient;
    private String message;
    private long timestamp;

    public SmsHistory(String recipient, String message, long timestamp) {
        this.recipient = recipient;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}