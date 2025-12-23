package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkManager; // ✅ Import WorkManager

import com.example.parkeasy.adapter.BookingHistoryAdapter;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityBookingHistoryBinding;
import com.example.parkeasy.model.Booking;

import java.util.ArrayList;
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
        adapter = new BookingHistoryAdapter(this, filteredBookings, this);
        binding.recyclerHistory.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> finish());

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                currentSearch = s.toString().toLowerCase();
                applyFilters();
            }
        });

        // Set default selection
        binding.chipAll.setChecked(true);

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipActive)) {
                currentFilter = "ACTIVE";
            } else if (checkedIds.contains(R.id.chipCompleted)) {
                currentFilter = "COMPLETED";
            } else if (checkedIds.contains(R.id.chipCancelled)) {
                currentFilter = "CANCELLED";
            } else {
                currentFilter = "ALL";
            }
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

        if (booking.getStartTime() != null) {
            intent.putExtra("START_TIME", booking.getStartTime().getTime());
        }
        if (booking.getEndTime() != null) {
            intent.putExtra("END_TIME", booking.getEndTime().getTime());
        }
        // Pass duration for calculations
        intent.putExtra("DURATION", booking.getDurationHours());

        startActivity(intent);
    }

    @Override
    public void onCancelClick(Booking booking) {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 1. Call Firebase Manager
        FirebaseManager.getInstance().cancelBooking(booking.getBookingId(), booking.getSlotId(), new FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);

                // 2. 🛑 KILL NOTIFICATIONS (Clean Logic)
                // We try to cancel by Specific ID (Best Practice) AND Generic Tags (Safety Net)
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag(booking.getBookingId());
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag("parking_reminder");
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag("parking_overtime");

                Toast.makeText(BookingHistoryActivity.this, "Booking Cancelled & Alarms Stopped", Toast.LENGTH_SHORT).show();
                loadBookingsFromFirebase(); // Refresh List
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
        int extensionCost = 40;

        FirebaseManager.getInstance().extendSpecificBooking(booking.getBookingId(), booking.getSlotId(), extensionCost, new FirebaseManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);

                // 🛑 STOP OLD ALARMS (Because we extended time, the old "15 min left" warning is invalid)
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag(booking.getBookingId());
                WorkManager.getInstance(BookingHistoryActivity.this).cancelAllWorkByTag("parking_reminder");

                Toast.makeText(BookingHistoryActivity.this, "Extended by 1 Hour! (Old Alarms Reset)", Toast.LENGTH_SHORT).show();
                loadBookingsFromFirebase();
            }

            @Override
            public void onFailure(Exception e) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Extend Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBookingsFromFirebase() {
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
            }
        });
    }

    private void applyFilters() {
        filteredBookings.clear();
        Date now = new Date();

        for (Booking b : allBookings) {
            // Search filter
            boolean matchesSearch = b.getBookingId().toLowerCase().contains(currentSearch) ||
                    (b.getLocationName() != null && b.getLocationName().toLowerCase().contains(currentSearch));

            // Status filter logic
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

            if (matchesSearch && matchesStatus) {
                filteredBookings.add(b);
            }
        }

        // Update UI empty state
        if (filteredBookings.isEmpty()) {
            binding.layoutNoBookings.setVisibility(View.VISIBLE);
            binding.recyclerHistory.setVisibility(View.GONE);
        } else {
            binding.layoutNoBookings.setVisibility(View.GONE);
            binding.recyclerHistory.setVisibility(View.VISIBLE);
        }

        adapter.notifyDataSetChanged();
    }
}