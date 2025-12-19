package com.example.parkeasy.model;

import java.io.Serializable;

public class Slot implements Serializable {
    private String slotId;
    private String name;
    private String locationId;
    private boolean occupied; // Changed to match Firestore field standard usually
    private long expiryTime;

    // 1. Empty Constructor (Required for Firestore)
    public Slot() {
    }

    // 2. Full Constructor (Used by Seeder)
    public Slot(String slotId, String name, String locationId, boolean occupied, long expiryTime) {
        this.slotId = slotId;       // <--- THIS WAS LIKELY MISSING
        this.name = name;
        this.locationId = locationId;
        this.occupied = occupied;
        this.expiryTime = expiryTime;
    }

    // 3. Getters and Setters
    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    // NOTE: Getter for boolean is often isOccupied() or getOccupied()
    // Firestore maps "occupied" field to isOccupied() or getOccupied() automatically
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public long getExpiryTime() { return expiryTime; }
    public void setExpiryTime(long expiryTime) { this.expiryTime = expiryTime; }
}