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
import com.example.parkeasy.util.NetworkUtils;
import com.example.parkeasy.util.VehicleManagerDialog;
import com.example.parkeasy.util.VehiclePrefs;
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
    private int durationHours = 1;
    private String selectedVehicle = "";
    private static final int PREBOOK_MINUTES = 15;
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
        setBookingDisabled("Select a Slot");
        updateDurationUI();
        setupVehiclePicker();
        binding.tvManageVehicles.setOnClickListener(v -> openVehicleDialog());
        binding.switchPrebook.setOnCheckedChangeListener((buttonView, isChecked) -> updatePricingUI());

        setupGrid();
        fetchSlots();

        binding.btnDurationMinus.setOnClickListener(v -> {
            if (durationHours > 1) {
                durationHours--;
                updateDurationUI();
            }
        });

        binding.btnDurationPlus.setOnClickListener(v -> {
            durationHours++;
            updateDurationUI();
        });

        binding.btnBook.setOnClickListener(v -> {
            if (selectedSlot != null) {
                confirmBooking();
            }
        });
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupGrid() {
        binding.recyclerSlots.setLayoutManager(new GridLayoutManager(this, 3));
        binding.recyclerSlots.setHasFixedSize(true);
        binding.recyclerSlots.setItemViewCacheSize(18);

        // Initialize the new adapter (no need to pass a list)
        adapter = new SlotAdapter(slot -> {
            selectedSlot = slot;
            adapter.setSelectedSlot(slot); // Use the new method

            updatePricingUI();
        });
        binding.recyclerSlots.setAdapter(adapter);
    }

    private void fetchSlots() {
        if (!NetworkUtils.isOnline(this)) {
            showEmptyState("No internet connection");
            Toast.makeText(this, "Please check your connection.", Toast.LENGTH_SHORT).show();
            setBookingDisabled("Offline");
            return;
        }

        List<Slot> cached = FirebaseManager.getInstance().getCachedSlots(locationId);
        if (cached != null && !cached.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerSlots.setVisibility(View.VISIBLE);
            binding.tvEmptySlots.setVisibility(View.GONE);
            if (adapter != null) {
                adapter.submitList(cached);
            }
        } else {
            setLoading(true);
        }

        // 🚀 REAL-TIME LISTENER (Replaces .get())
        slotsListener = FirebaseFirestore.getInstance()
                .collection("slots")
                .whereEqualTo("locationId", locationId)
                .addSnapshotListener((snapshots, e) -> {
                    if (isFinishing() || isDestroyed()) return;

                    // 1. Handle Errors
                    if (e != null) {
                        showEmptyState("Unable to load slots");
                        Toast.makeText(this, "Failed to load slots.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Clear Old Data
                    slotList.clear();
                    setLoading(false);

                    if (snapshots != null && !snapshots.isEmpty()) {
                        long now = System.currentTimeMillis();

                        for (DocumentSnapshot doc : snapshots) {
                            Slot slot = doc.toObject(Slot.class);
                            if (slot == null || !slot.isActive()) {
                                continue;
                            }

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
                            adapter.submitList(slotList); // Use submitList instead of setSlots
                        }
                        FirebaseManager.getInstance().updateSlotCache(locationId, slotList);
                        binding.recyclerSlots.setVisibility(View.VISIBLE);
                        binding.tvEmptySlots.setVisibility(View.GONE);
                    } else {
                        showEmptyState("No slots available");
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

        String userId = currentUser.getUid();
        double totalPrice = ratePerHour * durationHours;

        Slot slot = selectedSlot;
        String vehicleNumber = getSelectedVehicle();

        long startTimeMillis = getStartTimeMillis();
        FirebaseManager.getInstance().bookSlot(slot, userId, locationName, vehicleNumber, startTimeMillis, durationHours, totalPrice,
                new FirebaseManager.FirestoreCallback<String>() {
            @Override
            public void onSuccess(String bookingId) {
                // 1. Create the Booking Object locally to pass to Email (or fetch it)
                Booking receiptBooking = new Booking();
                receiptBooking.setBookingId(bookingId);
                receiptBooking.setLocationName(locationName); // Passed from intent
                receiptBooking.setSlotName(slot.getName());
                receiptBooking.setStartTime(new Date(startTimeMillis));
                receiptBooking.setDurationHours(durationHours);
                receiptBooking.setTotalCost(totalPrice);
                receiptBooking.setVehicleNumber(vehicleNumber);

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
                intent.putExtra("VEHICLE_NUMBER", vehicleNumber);

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
                Toast.makeText(SlotSelectionActivity.this, "Booking failed. Please try again.", Toast.LENGTH_SHORT).show();
                setBookingDisabled("Select a Slot");
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

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerSlots.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.tvEmptySlots.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        binding.recyclerSlots.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.tvEmptySlots.setText(message);
        binding.tvEmptySlots.setVisibility(View.VISIBLE);
    }

    private void setBookingDisabled(String label) {
        binding.btnBook.setEnabled(false);
        binding.btnBook.setText(label);
        binding.btnBook.setAlpha(0.5f);
    }

    private void updateDurationUI() {
        String durationText = durationHours == 1 ? "1 hour" : durationHours + " hours";
        binding.tvDurationValue.setText(durationText);
        binding.btnDurationMinus.setEnabled(durationHours > 1);
        binding.btnDurationMinus.setAlpha(durationHours > 1 ? 1.0f : 0.5f);
        updatePricingUI();
    }

    private void updatePricingUI() {
        if (selectedSlot == null) {
            binding.tvSelectionInfo.setText("No slot selected");
            binding.tvTotalPrice.setText("₹0");
            setBookingDisabled("Select a Slot");
            return;
        }

        if (selectedSlot.isOccupied()) {
            binding.tvSelectionInfo.setText("Slot " + selectedSlot.getName() + " is occupied");
            binding.tvTotalPrice.setText("₹0");
            setBookingDisabled("Occupied");
            return;
        }

        int totalPrice = ratePerHour * durationHours;
        String vehicleLabel = getVehicleLabel();
        String startLabel = binding.switchPrebook.isChecked() ? "Starts in 15 min" : "Starts now";
        binding.tvSelectionInfo.setText("Selected " + selectedSlot.getName() + " • " + vehicleLabel + " • " + startLabel);
        binding.tvTotalPrice.setText("₹" + totalPrice);
        binding.btnBook.setEnabled(true);
        binding.btnBook.setAlpha(1.0f);
        binding.btnBook.setText("Book " + selectedSlot.getName() + " - ₹" + totalPrice);
    }

    private String getVehicleLabel() {
        String vehicle = getSelectedVehicle();
        if (vehicle.isEmpty() || "NOT_SET".equals(vehicle)) {
            return "Vehicle not set";
        }
        return "Vehicle " + vehicle;
    }

    private void setupVehiclePicker() {
        List<String> vehicles = new ArrayList<>(VehiclePrefs.getVehicles(this));
        String primary = VehiclePrefs.getPrimaryVehicle(this);
        if (vehicles.isEmpty()) {
            vehicles.add("Not set");
        }
        if (!primary.isEmpty() && vehicles.contains(primary)) {
            selectedVehicle = primary;
        } else {
            selectedVehicle = vehicles.get(0);
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, vehicles);
        binding.spinnerVehicle.setAdapter(adapter);

        int selectedIndex = vehicles.indexOf(selectedVehicle);
        if (selectedIndex >= 0) {
            binding.spinnerVehicle.setSelection(selectedIndex);
        }

        binding.spinnerVehicle.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedVehicle = vehicles.get(position);
                updatePricingUI();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op
            }
        });
        updatePricingUI();
    }

    private void openVehicleDialog() {
        VehicleManagerDialog.show(this, this::setupVehiclePicker, "");
    }

    private String getSelectedVehicle() {
        if (selectedVehicle == null || selectedVehicle.isEmpty()) {
            String primary = VehiclePrefs.getPrimaryVehicle(this);
            return primary.isEmpty() ? "NOT_SET" : primary;
        }
        if ("Not set".equals(selectedVehicle)) {
            return "NOT_SET";
        }
        return selectedVehicle;
    }

    private long getStartTimeMillis() {
        long now = System.currentTimeMillis();
        if (binding.switchPrebook.isChecked()) {
            return now + (PREBOOK_MINUTES * 60_000L);
        }
        return now;
    }
}
