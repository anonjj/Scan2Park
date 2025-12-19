package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parkeasy.databinding.ActivityBookingSummaryBinding;
import com.example.parkeasy.model.Booking;
import com.example.parkeasy.api.EmailService;

import java.util.Date;

public class BookingSummaryActivity extends AppCompatActivity {

    private ActivityBookingSummaryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Get Booking Data from Intent
        String bookingId = getIntent().getStringExtra("BOOKING_ID");
        String slotName = getIntent().getStringExtra("SLOT_NAME");
        String location = getIntent().getStringExtra("LOCATION_NAME");
        double totalCost = getIntent().getDoubleExtra("TOTAL_COST", 0.0);
        binding.tvReceiptLocation.setText(location);
        binding.tvReceiptSlot.setText(slotName);
        binding.tvReceiptAmount.setText("₹" + (int)totalCost);

        // 2. Get User Data
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userName = getIntent().getStringExtra("USER_NAME");
        if (userName == null) userName = "Driver";

        // 3. Update UI
        binding.tvReceiptLocation.setText(location);
        binding.tvReceiptSlot.setText(slotName);
        binding.tvReceiptAmount.setText("₹" + (int)totalCost);

        // 4. SEND EMAIL 📧
        if (userEmail != null && !userEmail.isEmpty()) {

            // A. Construct the Booking Object locally so the EmailService can read it
            Booking receiptBooking = new Booking();
            receiptBooking.setBookingId(bookingId);
            receiptBooking.setSlotName(slotName);
            receiptBooking.setLocationName(location);
            receiptBooking.setTotalCost(totalCost); // ✅ Set Price INSIDE the object
            receiptBooking.setStartTime(new Date()); // Current time as booking time
            receiptBooking.setDurationHours(2); // Default or passed via intent
            receiptBooking.setVehicleNumber("MH-04-AB-1234"); // Optional

            // B. Call Service: (Email, BookingObject, UserName)
            EmailService.sendBookingReceipt(userEmail, receiptBooking, userName);

            Log.d("BookingSummary", "Email request sent to " + userEmail);
        } else {
            Log.e("BookingSummary", "Email not sent: User Email is NULL");
        }

        // 5. Home Button
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}