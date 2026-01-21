package com.example.parkeasy.model;

import java.io.Serializable;

public class Owner implements Serializable {
    private String ownerId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String businessName;
    private String businessRegNumber;
    private String yearsInOperation;
    private String licenseId;
    private long createdAt;

    public Owner() {
    }

    public Owner(String ownerId, String fullName, String email, String phoneNumber,
                 String businessName, String businessRegNumber, String yearsInOperation,
                 String licenseId, long createdAt) {
        this.ownerId = ownerId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.businessName = businessName;
        this.businessRegNumber = businessRegNumber;
        this.yearsInOperation = yearsInOperation;
        this.licenseId = licenseId;
        this.createdAt = createdAt;
    }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getBusinessRegNumber() { return businessRegNumber; }
    public void setBusinessRegNumber(String businessRegNumber) { this.businessRegNumber = businessRegNumber; }

    public String getYearsInOperation() { return yearsInOperation; }
    public void setYearsInOperation(String yearsInOperation) { this.yearsInOperation = yearsInOperation; }

    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
