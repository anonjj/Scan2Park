package com.example.parkeasy.model;

import java.util.Date;

public class Booking {
    private String bookingId;
    private String userId;
    private String userFullName;
    private String userEmail;
    private String slotId;
    private String slotName;
    private String locationId;
    private String locationName;
    private String location;  // For backward compatibility
    private String vehicleNumber;
    private Date startTime;
    private Date endTime;
    private int durationHours;
    private double totalCost;
    private String status;

    // Required no-argument constructor for Firestore
    public Booking() {
    }

    // Full constructor
    public Booking(String bookingId, String userId, String userFullName, String userEmail,
                   String slotId, String slotName, String locationId, String locationName,
                   String vehicleNumber, Date startTime, Date endTime,
                   int durationHours, int totalCost, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.userFullName = userFullName;
        this.userEmail = userEmail;
        this.slotId = slotId;
        this.slotName = slotName;
        this.locationId = locationId;
        this.locationName = locationName;
        this.location = locationName; // Set location for backward compatibility
        this.vehicleNumber = vehicleNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationHours = durationHours;
        this.totalCost = totalCost;
        this.status = status;
    }

    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
        this.location = locationName; // Keep location in sync
    }

    // For backward compatibility
    public String getLocation() {
        return location != null ? location : locationName;
    }

    public void setLocation(String location) {
        this.location = location;
        this.locationName = location; // Keep locationName in sync
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Utility methods
    public boolean isActive() {
        Date now = new Date();
        return status != null &&
                (status.equals("ACTIVE") || status.equals("CONFIRMED")) &&
                startTime != null && startTime.before(now) &&
                endTime != null && endTime.after(now);
    }

    public boolean isCompleted() {
        Date now = new Date();
        return (status != null && status.equals("COMPLETED")) ||
                (endTime != null && endTime.before(now));
    }

    public boolean isCancelled() {
        return status != null && status.equals("CANCELLED");
    }

    public String getFormattedDuration() {
        if (durationHours < 1) {
            return "Less than 1 hour";
        } else if (durationHours == 1) {
            return "1 hour";
        } else {
            return durationHours + " hours";
        }
    }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId='" + bookingId + '\'' +
                ", slotName='" + slotName + '\'' +
                ", locationName='" + locationName + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}