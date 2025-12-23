package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.parkeasy.databinding.ActivityBookingSummaryBinding; // Ensure this matches your XML name
import com.example.parkeasy.model.Booking;
import com.example.parkeasy.service.NotificationWorker;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class BookingSummaryActivity extends AppCompatActivity {

    private ActivityBookingSummaryBinding binding;
    private String bookingId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bookingId = getIntent().getStringExtra("BOOKING_ID");

        if (bookingId != null && !bookingId.isEmpty()) {
            loadBookingFromFirestore();
        } else {
            loadBookingFromIntent();
        }

        // Home Button Logic
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // ❌ DELETED: setupShareButton definition was here. It must be outside.
    }

    // ✅ FIXED: Method defined here (Class Level)
    private void setupShareButton(String location, String slot, String time, String bId) {
        binding.btnShare.setOnClickListener(v -> {
            // Safe check for null ID
            String safeId = (bId != null) ? bId : "PENDING";

            // Create the message text
            String shareMessage = "🚗 *Parking Booking Confirmed!*\n\n" +
                    "📍 Location: " + location + "\n" +
                    "🅿️ Slot: " + slot + "\n" +
                    "🕒 Time: " + time + "\n" +
                    "🆔 Booking ID: " + safeId + "\n\n" +
                    "Navigate via: http://maps.google.com/?q=" + location.replace(" ", "+") + "\n" +
                    "- Shared via Scan2Pay App";

            // Create the Share Intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Parking Receipt");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

            // Launch the System Share Sheet
            startActivity(Intent.createChooser(shareIntent, "Share Receipt via"));
        });
    }

    private void loadBookingFromIntent() {
        String slotName = getIntent().getStringExtra("SLOT_NAME");
        String location = getIntent().getStringExtra("LOCATION_NAME");
        double totalCost = getIntent().getDoubleExtra("TOTAL_COST", 0.0);
        long startTimeMillis = getIntent().getLongExtra("START_TIME", System.currentTimeMillis());
        int durationHours = getIntent().getIntExtra("DURATION", 1);

        displayDetails(location, slotName, totalCost, startTimeMillis, durationHours);
        scheduleReminders(startTimeMillis, durationHours);
    }

    private void loadBookingFromFirestore() {
        if (binding.progressBar != null) binding.progressBar.setVisibility(View.VISIBLE);
        if (binding.contentLayout != null) binding.contentLayout.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("bookings")
                .document(bookingId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Booking booking = documentSnapshot.toObject(Booking.class);
                        if (booking != null) {
                            displayDetails(
                                    booking.getLocationName(),
                                    booking.getSlotName(),
                                    booking.getTotalCost(),
                                    booking.getStartTime().getTime(),
                                    booking.getDurationHours()
                            );

                            // Schedule reminders using the booking data
                            scheduleReminders(booking.getStartTime().getTime(), booking.getDurationHours());
                        }
                    }
                    if (binding.progressBar != null) binding.progressBar.setVisibility(View.GONE);
                    if (binding.contentLayout != null) binding.contentLayout.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading booking", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayDetails(String location, String slot, double cost, long start, int duration) {
        long end = start + ((long) duration * 3600000L);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

        // Construct the Time Range String (e.g., "14:00 - 16:00")
        String timeRange = timeFormat.format(new Date(start)) + " - " + timeFormat.format(new Date(end));

        binding.tvReceiptLocation.setText(location);
        binding.tvReceiptSlot.setText(slot);
        binding.tvReceiptAmount.setText("₹" + (int) cost);
        binding.tvReceiptDateTime.setText(timeRange);

        // ✅ FIXED: Call the setup function here!
        // This ensures the button gets the correct data to share.
        setupShareButton(location, slot, timeRange, bookingId);
    }

    private void scheduleReminders(long startTimeMillis, int durationHours) {
        long endTimeMillis = startTimeMillis + (durationHours * 3600000L);
        long currentTime = System.currentTimeMillis();

        // --- REMINDER 1: 15 Minutes Before ---
        long warningTime = endTimeMillis - (15 * 60 * 1000);
        long delayWarning = warningTime - currentTime;

        if (delayWarning > 0) {
            Data data = new Data.Builder()
                    .putString("TITLE", "⏳ Time is running out!")
                    .putString("MESSAGE", "You have 15 minutes left on your parking slot.")
                    .build();

            OneTimeWorkRequest warningRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delayWarning, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .addTag("parking_reminder")
                    .build();

            WorkManager.getInstance(this).enqueue(warningRequest);
        }

        // --- REMINDER 2: Overtime Alert (At Exact End Time) ---
        long delayOvertime = endTimeMillis - currentTime;

        if (delayOvertime > 0) {
            Data overtimeData = new Data.Builder()
                    .putString("TITLE", "⚠️ OVERTIME ZONE ENTERED")
                    .putString("MESSAGE", "Your parking has expired! Extend now to avoid fines.")
                    .build();

            OneTimeWorkRequest overtimeRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                    .setInitialDelay(delayOvertime, TimeUnit.MILLISECONDS)
                    .setInputData(overtimeData)
                    .addTag("parking_overtime")
                    .build();

            WorkManager.getInstance(this).enqueue(overtimeRequest);
        }
    }
    private void stopNotifications() {
        // This looks for any pending job with these tags and deletes them instantly
        WorkManager.getInstance(this).cancelAllWorkByTag("parking_reminder");
        WorkManager.getInstance(this).cancelAllWorkByTag("parking_overtime");
    }
}