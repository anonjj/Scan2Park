package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager;

import com.example.parkeasy.adapter.BookingHistoryAdapter;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityBookingHistoryBinding;
import com.example.parkeasy.model.Booking;
import com.example.parkeasy.util.NetworkUtils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BookingHistoryActivity extends AppCompatActivity implements BookingHistoryAdapter.OnBookingActionListener {

    private ActivityBookingHistoryBinding binding;
    private BookingHistoryAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> filteredBookings = new ArrayList<>();
    private String currentSearch = "";
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        loadBookingsFromFirebase();
    }

    private void setupUI() {
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setHasFixedSize(true);
        binding.recyclerHistory.setItemViewCacheSize(20);
        adapter = new BookingHistoryAdapter(this, new ArrayList<>(), this);
        binding.recyclerHistory.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s.toString().toLowerCase();
                applyFilters();
            }
        });

        binding.chipAll.setChecked(true);

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipActive)) currentFilter = "ACTIVE";
            else if (checkedIds.contains(R.id.chipCompleted)) currentFilter = "COMPLETED";
            else if (checkedIds.contains(R.id.chipCancelled)) currentFilter = "CANCELLED";
            else currentFilter = "ALL";
            applyFilters();
        });
    }

    @Override
    public void onItemClick(Booking booking) {
        Intent intent = new Intent(this, BookingSummaryActivity.class);
        intent.putExtra("BOOKING_ID", booking.getBookingId());
        intent.putExtra("SLOT_NAME", booking.getSlotName());
        intent.putExtra("LOCATION_NAME", booking.getLocationName());
        intent.putExtra("TOTAL_COST", booking.getTotalCost());

        if (booking.getStartTime() != null) intent.putExtra("START_TIME", booking.getStartTime().getTime());
        if (booking.getEndTime() != null) intent.putExtra("END_TIME", booking.getEndTime().getTime());
        intent.putExtra("DURATION", booking.getDurationHours());

        startActivity(intent);
    }

    @Override
    public void onCancelClick(Booking booking) {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().cancelBooking(booking.getBookingId(), booking.getSlotId(), new FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);

                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag(booking.getBookingId());
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag("parking_reminder");
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag("parking_overtime");

                Toast.makeText(BookingHistoryActivity.this, "Booking Cancelled!", Toast.LENGTH_SHORT).show();
                loadBookingsFromFirebase();
            }

            @Override
            public void onFailure(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Cancel Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onExtendClick(Booking booking) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 1. Calculate the exact duration based on Start and End times
        long durationMillis = booking.getEndTime().getTime() - booking.getStartTime().getTime();
        double hours = durationMillis / (1000.0 * 60 * 60); // Convert milliseconds to hours

        // Safety: If booking is fresh (0 hours), treat as 1 hour to avoid crash
        if (hours < 1) hours = 1;

        // 2. CALCULATE RATE: (Total Cost / Hours)
        int calculatedRate = (int) (booking.getTotalCost() / hours);

        if (calculatedRate <= 0) {
            calculatedRate = 40;
        }

        // 4. Proceed with the calculated rate (e.g., 100)
        performExtension(booking, calculatedRate);
    }

    private void performExtension(Booking booking, int extensionAmount) {
        // 2. Calculate New End Time
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(booking.getEndTime() != null ? booking.getEndTime() : new Date());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date newEndTime = calendar.getTime();

        // 3. Get User ID & Check Balance
        String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userRef = FirebaseFirestore.getInstance().collection("users").document(userId);

        userRef.get().addOnSuccessListener(userDoc -> {
            if (userDoc.exists()) {
                Double currentBalance = userDoc.getDouble("walletBalance");

                if (currentBalance != null && currentBalance >= extensionAmount) {
                    // Deduct Money & Extend Booking
                    userRef.update("walletBalance", FieldValue.increment(-extensionAmount))
                            .addOnSuccessListener(aVoid -> {
                                updateBookingExtension(booking, newEndTime, extensionAmount);
                            });
                } else {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Insufficient Balance for ₹" + extensionAmount + " extension.", Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(e -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Error checking balance.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onExitClick(Booking booking) {
        binding.progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("bookings")
                .document(booking.getBookingId())
                .update("status", "COMPLETED")
                .addOnSuccessListener(aVoid -> {
                    freeUpParkingSlot(booking.getSlotId());
                    WorkManager.getInstance(this).cancelAllWorkByTag(booking.getBookingId());
                    WorkManager.getInstance(this).cancelAllWorkByTag("parking_reminder");
                    Toast.makeText(this, "Session Ended. Spot is now free!", Toast.LENGTH_SHORT).show();
                    loadBookingsFromFirebase();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error ending session", Toast.LENGTH_SHORT).show();
                });
    }

    // ---------------- HELPER METHODS ----------------

    private void updateBookingExtension(Booking booking, Date newEndTime, int extensionCost) {
        FirebaseFirestore.getInstance().collection("bookings")
                .document(booking.getBookingId())
                .update(
                        "endTime", newEndTime,
                        "status", "EXTENDED",
                        "totalCost", FieldValue.increment(extensionCost)
                )
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    WorkManager.getInstance(this).cancelAllWorkByTag(booking.getBookingId());
                    WorkManager.getInstance(this).cancelAllWorkByTag("parking_reminder");
                    Toast.makeText(this, "Extended! ₹" + extensionCost + " deducted from wallet.", Toast.LENGTH_LONG).show();
                    loadBookingsFromFirebase();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Extension Error", Toast.LENGTH_SHORT).show();
                });
    }

    private void freeUpParkingSlot(String slotId) {
        if (slotId == null || slotId.isEmpty()) return;
        FirebaseFirestore.getInstance().collection("slots")
                .document(slotId)
                .update(
                    "occupied", false,
                    "expiryTime", 0
                )
                .addOnSuccessListener(aVoid -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    binding.progressBar.setVisibility(View.GONE);
                });
    }

    private void loadBookingsFromFirebase() {
        if (!NetworkUtils.isOnline(this)) {
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.GONE);
            binding.layoutNoBookings.setVisibility(View.VISIBLE);
            binding.tvNoBookings.setText("No internet connection");
            Toast.makeText(this, "Please check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().getUserBookings(new FirebaseManager.FirestoreCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> result) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                allBookings.clear();
                allBookings.addAll(result);
                Collections.sort(allBookings, (b1, b2) -> {
                    if (b1.getStartTime() == null || b2.getStartTime() == null) return 0;
                    return b2.getStartTime().compareTo(b1.getStartTime());
                });
                applyFilters();
            }
            @Override
            public void onFailure(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.recyclerHistory.setVisibility(View.GONE);
                binding.layoutNoBookings.setVisibility(View.VISIBLE);
                binding.tvNoBookings.setText("Unable to load bookings");
                Toast.makeText(BookingHistoryActivity.this, "Failed to load bookings.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredBookings.clear();
        Date now = new Date();
        for (Booking b : allBookings) {
            boolean matchesSearch = b.getBookingId().toLowerCase().contains(currentSearch) ||
                    (b.getLocationName() != null && b.getLocationName().toLowerCase().contains(currentSearch));
            boolean matchesStatus = false;
            String status = b.getStatus() != null ? b.getStatus().toUpperCase() : "";
            boolean isCancelled = status.equals("CANCELLED");
            if (currentFilter.equals("ALL")) {
                matchesStatus = true;
            } else if (currentFilter.equals("ACTIVE")) {
                boolean isTimeActive = b.getEndTime() != null && b.getEndTime().after(now);
                boolean isStatusActive = status.equals("ACTIVE") || status.equals("EXTENDED") || status.equals("CONFIRMED");
                matchesStatus = !isCancelled && isStatusActive && isTimeActive;
            } else if (currentFilter.equals("COMPLETED")) {
                boolean isTimePassed = b.getEndTime() != null && b.getEndTime().before(now);
                boolean isStatusCompleted = status.equals("COMPLETED");
                matchesStatus = !isCancelled && (isStatusCompleted || isTimePassed);
            } else if (currentFilter.equals("CANCELLED")) {
                matchesStatus = isCancelled;
            }
            if (matchesSearch && matchesStatus) filteredBookings.add(b);
        }
        if (filteredBookings.isEmpty()) {
            binding.layoutNoBookings.setVisibility(View.VISIBLE);
            binding.tvNoBookings.setText("No bookings found");
            binding.recyclerHistory.setVisibility(View.GONE);
        } else {
            binding.layoutNoBookings.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.VISIBLE);
        }
        adapter.submitList(new ArrayList<>(filteredBookings));
    }
}
