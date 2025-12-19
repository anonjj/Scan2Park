package com.example.parkeasy.model;

import com.google.firebase.firestore.Exclude; // 👈 Import this!
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

public class User implements Serializable {

    private String uid;

    // Map Firestore "name" to Java "fullName"
    @PropertyName("name")
    private String fullName;

    private String email;
    private long walletBalance;
    private String phoneNumber;

    public User() {}

    public User(String uid, String name, String email, long walletBalance, String phone) {
        this.uid = uid;
        this.fullName = name;
        this.email = email;
        this.walletBalance = walletBalance;
        this.phoneNumber = phone;
    }

    // --- Getters and Setters ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // ✅ PRIMARY GETTER (Used by Firestore)
    @PropertyName("name")
    public String getFullName() { return fullName; }

    @PropertyName("name")
    public void setFullName(String fullName) { this.fullName = fullName; }

    // 🙈 HIDDEN FROM FIRESTORE (Used by your App only)
    @Exclude
    public String getName() { return fullName; }

    @Exclude
    public void setName(String name) { this.fullName = name; }

    // --- Standard Fields ---
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public long getWalletBalance() { return walletBalance; }
    public void setWalletBalance(long walletBalance) { this.walletBalance = walletBalance; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}