package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.parkeasy.adapter.BookingHistoryAdapter;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityBookingHistoryBinding;
import com.example.parkeasy.model.Booking;
import java.util.ArrayList;
import java.util.Collections;
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
        // Pass 'this' as the listener since we implemented the interface
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

        binding.chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipActive)) currentFilter = "ACTIVE";
            else if (checkedIds.contains(R.id.chipCompleted)) currentFilter = "COMPLETED";
            else currentFilter = "ALL";
            applyFilters();
        });
    }

    // --- 🚀 INTERFACE IMPLEMENTATION ---

    @Override
    public void onItemClick(Booking booking) {
        // Open Receipt
        Intent intent = new Intent(this, BookingSummaryActivity.class);
        intent.putExtra("BOOKING_ID", booking.getBookingId());
        intent.putExtra("SLOT_NAME", booking.getSlotName());
        intent.putExtra("LOCATION_NAME", booking.getLocationName());
        intent.putExtra("TOTAL_COST", booking.getTotalCost());
        startActivity(intent);
    }

    @Override
    public void onCancelClick(Booking booking) {
        // ⚠️ Call Firebase Cancel Logic
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().cancelBooking(booking.getBookingId(), booking.getSlotId(), new FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Booking Cancelled!", Toast.LENGTH_SHORT).show();
                loadBookingsFromFirebase(); // Refresh List
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Cancel Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onExtendClick(Booking booking) {
        // ⚠️ Call Firebase Extend Logic (Adding 1 Hour, +₹40 Cost)
        binding.progressBar.setVisibility(View.VISIBLE);
        int extensionCost = 40;

        FirebaseManager.getInstance().extendSpecificBooking(booking.getBookingId(), booking.getSlotId(), extensionCost, new FirebaseManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String result) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Extended by 1 Hour!", Toast.LENGTH_SHORT).show();
                loadBookingsFromFirebase(); // Refresh List
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(BookingHistoryActivity.this, "Extend Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ... (loadBookingsFromFirebase & applyFilters remain same as previous response) ...
    private void loadBookingsFromFirebase() {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseManager.getInstance().getUserBookings(new FirebaseManager.FirestoreCallback<List<Booking>>() {
            @Override
            public void onSuccess(List<Booking> result) {
                binding.progressBar.setVisibility(View.GONE);
                allBookings.clear();
                allBookings.addAll(result);
                Collections.sort(allBookings, (b1, b2) -> b2.getStartTime().compareTo(b1.getStartTime()));
                applyFilters();
            }
            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilters() {
        filteredBookings.clear();
        for (Booking b : allBookings) {
            boolean matchesSearch = b.getBookingId().toLowerCase().contains(currentSearch) ||
                    b.getLocationName().toLowerCase().contains(currentSearch);
            boolean matchesType = currentFilter.equals("ALL") ||
                    (b.getStatus() != null && b.getStatus().equalsIgnoreCase(currentFilter));
            if (matchesSearch && matchesType) filteredBookings.add(b);
        }
        adapter.notifyDataSetChanged();
    }
}