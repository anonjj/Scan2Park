package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityOwnerDashboardBinding;
import com.example.parkeasy.model.ParkingLocation;
import com.example.parkeasy.util.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class OwnerDashboardActivity extends AppCompatActivity {

    private ActivityOwnerDashboardBinding binding;
    private String ownerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ownerId = FirebaseAuth.getInstance().getUid();
        if (ownerId == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, OwnerLoginActivity.class));
            finish();
            return;
        }

        binding.btnPublishLocation.setOnClickListener(v -> publishLocation());
        binding.btnOwnerLogout.setOnClickListener(v -> logoutOwner());

        loadOwnerLocations();
    }

    private void publishLocation() {
        String name = binding.etLocationName.getText().toString().trim();
        String address = binding.etLocationAddress.getText().toString().trim();
        String rateStr = binding.etRatePerHour.getText().toString().trim();
        String slotsStr = binding.etTotalSlots.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etLocationName.setError("Location name required");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            binding.etLocationAddress.setError("Address required");
            return;
        }
        if (TextUtils.isEmpty(rateStr)) {
            binding.etRatePerHour.setError("Rate required");
            return;
        }
        if (TextUtils.isEmpty(slotsStr)) {
            binding.etTotalSlots.setError("Total slots required");
            return;
        }

        int rate;
        int totalSlots;
        try {
            rate = Integer.parseInt(rateStr);
            totalSlots = Integer.parseInt(slotsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rate <= 0 || totalSlots <= 0) {
            Toast.makeText(this, "Rate and slots must be greater than zero", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        setPublishing(true);
        setProgressVisible(true);
        FirebaseManager.getInstance().addParkingLocationForOwner(ownerId, name, address, rate, totalSlots,
                new FirebaseManager.FirestoreCallback<String>() {
                    @Override
                    public void onSuccess(String locationId) {
                        setPublishing(false);
                        setProgressVisible(false);
                        clearForm();
                        Toast.makeText(OwnerDashboardActivity.this, "Location published!", Toast.LENGTH_SHORT).show();
                        loadOwnerLocations();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        setPublishing(false);
                        setProgressVisible(false);
                        Toast.makeText(OwnerDashboardActivity.this, "Failed to publish: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logoutOwner() {
        FirebaseManager.getInstance().logout();
        Intent intent = new Intent(this, OwnerLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void clearForm() {
        binding.etLocationName.setText("");
        binding.etLocationAddress.setText("");
        binding.etRatePerHour.setText("");
        binding.etTotalSlots.setText("");
    }

    private void setProgressVisible(boolean isVisible) {
        binding.progressOwner.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void setPublishing(boolean isPublishing) {
        binding.btnPublishLocation.setEnabled(!isPublishing);
        binding.btnPublishLocation.setText(isPublishing ? "Publishing..." : "Publish Location");
    }

    private void loadOwnerLocations() {
        if (!NetworkUtils.isOnline(this)) {
            binding.tvOwnerLocationsEmpty.setText("No internet connection");
            binding.tvOwnerLocationsEmpty.setVisibility(View.VISIBLE);
            binding.layoutOwnerLocations.removeAllViews();
            return;
        }

        setProgressVisible(true);
        FirebaseManager.getInstance().fetchOwnerLocations(ownerId, new FirebaseManager.FirestoreCallback<List<ParkingLocation>>() {
            @Override
            public void onSuccess(List<ParkingLocation> result) {
                setProgressVisible(false);
                renderOwnerLocations(result);
            }

            @Override
            public void onFailure(Exception e) {
                setProgressVisible(false);
                binding.tvOwnerLocationsEmpty.setText("Unable to load locations");
                binding.tvOwnerLocationsEmpty.setVisibility(View.VISIBLE);
                binding.layoutOwnerLocations.removeAllViews();
            }
        });
    }

    private void renderOwnerLocations(List<ParkingLocation> locations) {
        binding.layoutOwnerLocations.removeAllViews();

        if (locations == null || locations.isEmpty()) {
            binding.tvOwnerLocationsEmpty.setText("No locations published yet");
            binding.tvOwnerLocationsEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.tvOwnerLocationsEmpty.setVisibility(View.GONE);
        for (ParkingLocation location : locations) {
            View itemView = getLayoutInflater().inflate(R.layout.item_owner_location, binding.layoutOwnerLocations, false);

            android.widget.TextView tvName = itemView.findViewById(R.id.tvOwnerLocName);
            android.widget.TextView tvAddress = itemView.findViewById(R.id.tvOwnerLocAddress);
            android.widget.TextView tvMeta = itemView.findViewById(R.id.tvOwnerLocMeta);
            android.widget.TextView tvStatus = itemView.findViewById(R.id.tvOwnerLocStatus);
            com.google.android.material.button.MaterialButton btnEdit = itemView.findViewById(R.id.btnOwnerEdit);
            com.google.android.material.button.MaterialButton btnToggle = itemView.findViewById(R.id.btnOwnerToggle);

            tvName.setText(location.getName());
            tvAddress.setText(location.getAddress());
            tvMeta.setText("₹" + location.getRatePerHour() + "/hr • " + location.getTotalSlots() + " slots");

            boolean isActive = location.isActive();
            tvStatus.setText(isActive ? "ACTIVE" : "DISABLED");
            int statusColor = getResources().getColor(isActive ? R.color.brand_secondary : R.color.error_red);
            tvStatus.setTextColor(statusColor);
            btnToggle.setText(isActive ? "Disable" : "Enable");

            btnEdit.setOnClickListener(v -> showEditDialog(location));
            btnToggle.setOnClickListener(v -> toggleLocation(location));

            binding.layoutOwnerLocations.addView(itemView);
        }
    }

    private void toggleLocation(ParkingLocation location) {
        boolean nextState = !location.isActive();
        setProgressVisible(true);
        FirebaseManager.getInstance().updateLocationActive(location.getLocationId(), nextState,
                new FirebaseManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        setProgressVisible(false);
                        loadOwnerLocations();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        setProgressVisible(false);
                        Toast.makeText(OwnerDashboardActivity.this, "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditDialog(ParkingLocation location) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_location, null);
        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etEditLocationName);
        com.google.android.material.textfield.TextInputEditText etAddress = dialogView.findViewById(R.id.etEditLocationAddress);
        com.google.android.material.textfield.TextInputEditText etRate = dialogView.findViewById(R.id.etEditRate);
        com.google.android.material.textfield.TextInputEditText etSlots = dialogView.findViewById(R.id.etEditSlots);

        etName.setText(location.getName());
        etAddress.setText(location.getAddress());
        etRate.setText(String.valueOf(location.getRatePerHour()));
        etSlots.setText(String.valueOf(location.getTotalSlots()));

        new AlertDialog.Builder(this)
                .setTitle("Edit Location")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
                    String rateStr = etRate.getText() != null ? etRate.getText().toString().trim() : "";
                    String slotsStr = etSlots.getText() != null ? etSlots.getText().toString().trim() : "";

                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(address)
                            || TextUtils.isEmpty(rateStr) || TextUtils.isEmpty(slotsStr)) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int rate;
                    int slots;
                    try {
                        rate = Integer.parseInt(rateStr);
                        slots = Integer.parseInt(slotsStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter valid numbers", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (rate <= 0 || slots <= 0) {
                        Toast.makeText(this, "Rate and slots must be greater than zero", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    setProgressVisible(true);
                    FirebaseManager.getInstance().updateOwnerLocation(
                            location.getLocationId(),
                            name,
                            address,
                            rate,
                            slots,
                            location.getTotalSlots(),
                            new FirebaseManager.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    setProgressVisible(false);
                                    loadOwnerLocations();
                                    Toast.makeText(OwnerDashboardActivity.this, "Location updated", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    setProgressVisible(false);
                                    Toast.makeText(OwnerDashboardActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
