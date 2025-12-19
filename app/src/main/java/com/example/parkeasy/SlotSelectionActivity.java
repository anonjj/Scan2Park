package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.parkeasy.adapter.SlotAdapter;
import com.example.parkeasy.api.EmailService;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivitySlotSelectionBinding;
import com.example.parkeasy.model.Booking;
import com.example.parkeasy.model.Slot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import android.util.Log;

public class SlotSelectionActivity extends AppCompatActivity {

    private ActivitySlotSelectionBinding binding;
    private SlotAdapter adapter;
    private Slot selectedSlot;
    private String locationId;
    private String locationName;
    private int ratePerHour;
    private List<Slot> slotList = new ArrayList<>(); // Initialize the list here
    private ListenerRegistration slotsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySlotSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
// --- LOGGING POINT 1: Check received data ---
        locationId = getIntent().getStringExtra("LOCATION_ID");
        locationName = getIntent().getStringExtra("LOCATION_NAME");
        ratePerHour = getIntent().getIntExtra("RATE", 40);
        Log.d("SlotSelection", "Activity started for locationName: " + locationName + ", locationId: " + locationId);

        binding.tvLocationTitle.setText(locationName);
        binding.btnBook.setEnabled(false);
        binding.btnBook.setText("Select a Slot");

        setupGrid();
        fetchSlots();

        binding.btnBook.setOnClickListener(v -> {
            if (selectedSlot != null) {
                confirmBooking();
            }
        });
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupGrid() {
        binding.recyclerSlots.setLayoutManager(new GridLayoutManager(this, 3));

        // Initialize the new adapter (no need to pass a list)
        adapter = new SlotAdapter(slot -> {
            selectedSlot = slot;
            adapter.setSelectedSlot(slot); // Use the new method

            binding.btnBook.setEnabled(!slot.isOccupied());
            if (slot.isOccupied()) {
                binding.btnBook.setText("Occupied");
            } else {
                binding.btnBook.setText("Book " + slot.getName() + " - ₹" + ratePerHour + "/hr");
                binding.btnBook.setAlpha(1.0f);
            }
        });
        binding.recyclerSlots.setAdapter(adapter);
    }

    private void fetchSlots() {
        binding.progressBar.setVisibility(View.VISIBLE);

        // 🚀 REAL-TIME LISTENER (Replaces .get())
        slotsListener = FirebaseFirestore.getInstance()
                .collection("slots")
                .whereEqualTo("locationId", locationId)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;

                    // 1. Handle Errors
                    if (e != null) {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Listen failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Clear Old Data
                    slotList.clear();
                    binding.progressBar.setVisibility(View.GONE);

                    if (snapshots != null && !snapshots.isEmpty()) {
                        long now = System.currentTimeMillis();

                        for (DocumentSnapshot doc : snapshots) {
                            Slot slot = doc.toObject(Slot.class);

                            // 🧹 LIVE CLEANUP: If expiry passed, treat as FREE locally
                            // (The Janitor fixes the DB, but this fixes the UI instantly)
                            if (slot.isOccupied() && slot.getExpiryTime() > 0 && slot.getExpiryTime() < now) {
                                slot.setOccupied(false);
                            }

                            slotList.add(slot);
                        }

                        // 3. Sort Slots (A1, A2, A3...)
                        Collections.sort(slotList, (s1, s2) -> extractInt(s1.getName()) - extractInt(s2.getName()));

                        if (adapter != null) {
                            adapter.setSlots(slotList); // Give the data to the adapter!
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Toast.makeText(this, "No slots found for this location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helper to sort "A1", "A10", "A2" correctly
    private int extractInt(String name) {
        String num = name.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    private void confirmBooking() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to book.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnBook.setEnabled(false);
        binding.btnBook.setText("Confirming...");

        // Assuming a default booking duration of 1 hour for now
        int durationHours = 1;

        String userId = currentUser.getUid();
        double totalPrice = ratePerHour * durationHours;

        Slot slot = selectedSlot;
        FirebaseManager.getInstance().bookSlot(slot, userId, durationHours, totalPrice, new FirebaseManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String bookingId) {
                // 1. Create the Booking Object locally to pass to Email (or fetch it)
                Booking receiptBooking = new Booking();
                receiptBooking.setBookingId(bookingId);
                receiptBooking.setLocationName(locationName); // Passed from intent
                receiptBooking.setSlotName(slot.getName());
                receiptBooking.setStartTime(new Date());
                receiptBooking.setDurationHours(durationHours);
                receiptBooking.setTotalCost(totalPrice);
                receiptBooking.setVehicleNumber("MH-04-AB-1234"); // Replace with selected vehicle logic if you have it

                // 2. Get User Email & Send Receipt
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    String userName = user.getDisplayName() != null ? user.getDisplayName() : "Driver";

                    // 🚀 FIRE THE DYNAMIC EMAIL
                    EmailService.sendBookingReceipt(user.getEmail(), receiptBooking, userName);
                }

                // 3. Navigate to Success Screen
                Intent intent = new Intent(SlotSelectionActivity.this, BookingSummaryActivity.class);

                intent.putExtra("BOOKING_ID", bookingId);
                intent.putExtra("SLOT_NAME", slot.getName());       // Pass Slot Name
                intent.putExtra("LOCATION_NAME", locationName);     // Pass Location
                intent.putExtra("TOTAL_COST", totalPrice);          // 💰 Pass the Price!

                // Pass User Data (So the Summary screen can send the email if you moved logic there)
                if (user != null) {
                    intent.putExtra("USER_EMAIL", user.getEmail());
                    intent.putExtra("USER_NAME", user.getDisplayName());
                }

                startActivity(intent);
                finish();
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SlotSelectionActivity.this, "Booking Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 🛑 Stop listening when user leaves the screen
        if (slotsListener != null) {
            slotsListener.remove();
        }

    }
}
