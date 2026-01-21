package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.parkeasy.adapter.LocationAdapter;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityLocationSelectionBinding;
import com.example.parkeasy.model.ParkingLocation;
import com.example.parkeasy.util.NetworkUtils;
import java.util.List;

public class LocationSelectionActivity extends AppCompatActivity {

    private ActivityLocationSelectionBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocationSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Setup UI
        binding.btnBack.setOnClickListener(v -> finish());
        binding.recyclerLocations.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerLocations.setHasFixedSize(true);
        binding.recyclerLocations.setItemViewCacheSize(16);

        // Show loading state
        setLoading(true);

        // 2. Fetch Data & Use the Adapter
        loadLocations();
    }

    private void loadLocations() {
        if (!NetworkUtils.isOnline(this)) {
            showEmptyState("No internet connection");
            Toast.makeText(LocationSelectionActivity.this, "Please check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        FirebaseManager.getInstance().fetchParkingLocations(new FirebaseManager.FirestoreCallback<>() {
            @Override
            public void onSuccess(List<ParkingLocation> locations) {
                if (isFinishing() || isDestroyed()) return;

                if (locations.isEmpty()) {
                    showEmptyState("No locations available");
                    return;
                }

                setLoading(false);

                // 3. HERE IS WHERE WE USE THE ADAPTER 👇
                binding.recyclerLocations.setVisibility(View.VISIBLE);

                // ✅ NEW: Use LocationAdapter with the Click Interface
                LocationAdapter adapter = new LocationAdapter(locations, location -> {
                    // This code runs when a user clicks a location
                    Intent intent = new Intent(LocationSelectionActivity.this, SlotSelectionActivity.class);
                    intent.putExtra("LOCATION_ID", location.getLocationId());
                    intent.putExtra("LOCATION_NAME", location.getName());

                    // Pass the Rate (Make sure your model has getRatePerHour!)
                    intent.putExtra("RATE", location.getRatePerHour());

                    startActivity(intent);
                });

                binding.recyclerLocations.setAdapter(adapter);
            }

            @Override
            public void onFailure(Exception e) {
                showEmptyState("Unable to load locations");
                Toast.makeText(LocationSelectionActivity.this, "Failed to load locations.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerLocations.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        binding.tvEmptyLocations.setVisibility(View.GONE);
    }

    private void showEmptyState(String message) {
        binding.recyclerLocations.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        binding.tvEmptyLocations.setText(message);
        binding.tvEmptyLocations.setVisibility(View.VISIBLE);
    }
}
