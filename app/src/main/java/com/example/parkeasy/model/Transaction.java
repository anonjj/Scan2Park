package com.example.parkeasy.model;

import java.util.Date;

public class Transaction {
    private String transactionId;
    private String userId;
    private double amount;
    private String type; // "CREDIT" (Added money) or "DEBIT" (Paid for parking)
    private String description;
    private Date timestamp;

    public Transaction() {} // Required for Firestore

    public Transaction(String transactionId, String userId, double amount, String type, String description, Date timestamp) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.description = description;
        this.timestamp = timestamp;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public Date getTimestamp() { return timestamp; }
}