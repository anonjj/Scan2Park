package com.example.parkeasy.model;

import java.io.Serializable;

public class ParkingLocation implements Serializable {
    private String locationId;
    private String name;
    private String address;
    private int ratePerHour;
    private int totalSlots;
    private String ownerId;
    private boolean active = true;

    // Empty Constructor (Required for Firestore)
    public ParkingLocation() {}

    // Getters and Setters
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    // --- THIS WAS THE PROBLEM AREA ---
    public int getRatePerHour() { return ratePerHour; }
    public void setRatePerHour(int ratePerHour) { this.ratePerHour = ratePerHour; }
    // ---------------------------------

    public int getTotalSlots() { return totalSlots; }
    public void setTotalSlots(int totalSlots) { this.totalSlots = totalSlots; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
