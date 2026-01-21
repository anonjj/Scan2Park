package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.example.parkeasy.databinding.ActivityProfileBinding;
import com.example.parkeasy.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.parkeasy.util.VehicleManagerDialog;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Setup Bottom Nav (Highlight Profile)
        binding.bottomNavigation.setSelectedItemId(R.id.nav_profile);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0); // No animation
                return true;
            } else if (id == R.id.nav_parking) {
                startActivity(new Intent(this, LocationSelectionActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingHistoryActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

        // 2. Load Data
        loadProfileData();
        calculateStats();

        // 3. Setup Buttons
        setupActions();

    }

    private void loadProfileData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Fetch User Info
        com.example.parkeasy.data.FirebaseManager.getInstance().getUserData(uid, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    binding.tvProfileName.setText(user.getFullName() != null ? user.getFullName() : "Driver");
                    binding.tvProfileEmail.setText(user.getEmail());
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.tvProfileName.setText("Error Loading");
            }
        });
    }

    private void calculateStats() {
        String uid = FirebaseAuth.getInstance().getUid();

        FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("userId", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    int totalBookings = snapshots.size();
                    int totalSpent = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Long cost = doc.getLong("totalCost");
                        if (cost != null) {
                            totalSpent += cost.intValue();
                        }
                    }

                    // Update UI
                    binding.tvStatBookings.setText(String.valueOf(totalBookings));
                    binding.tvStatSpent.setText("₹" + totalSpent);
                });
    }

    private void setupActions() {
        // My Vehicles
        binding.btnMyVehicles.setOnClickListener(v -> showVehicleDialog());

        // Notifications
        binding.btnNotifications.setOnClickListener(v -> showNotificationSettings());

        // Payment Methods
        binding.btnPaymentMethods.setOnClickListener(v -> {
            startActivity(new Intent(this, WalletActivity.class));
        });

        // Booking History
        binding.btnHistoryLink.setOnClickListener(v -> {
            startActivity(new Intent(this, BookingHistoryActivity.class));
        });

        // Edit Profile
        binding.btnEditProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Edit Feature locked in prototype.", Toast.LENGTH_SHORT).show();
        });

        // LOGOUT
        binding.btnLogout.setOnClickListener(v -> {
            com.example.parkeasy.data.FirebaseManager.getInstance().logout();

            // Clear stack and go to Login
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showVehicleDialog() {
        openVehicleDialog();
    }

    private void openVehicleDialog() {
        VehicleManagerDialog.show(this, null, "");
    }

    private void showNotificationSettings() {
        // Options for the user
        String[] options = {"Parking Timer Alerts", "Overtime Warnings", "Promotional Emails"};
        boolean[] checkedItems = {true, true, false}; // Default values (Fake it for the demo)

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Notification Preferences")
                .setMultiChoiceItems(options, checkedItems, (dialog, which, isChecked) -> {
                    if (which == 0 && !isChecked) {
                         WorkManager.getInstance(this).cancelAllWorkByTag("parking_reminder");
                         Toast.makeText(this, "Timer Alerts Muted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Done", null)
                .show();
    }
}
