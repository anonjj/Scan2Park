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

        // Show loading state
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.recyclerLocations.setVisibility(View.GONE);

        // 2. Fetch Data & Use the Adapter
        loadLocations();
    }

    private void loadLocations() {
        FirebaseManager.getInstance().fetchParkingLocations(new FirebaseManager.FirestoreCallback<>() {
            @Override
            public void onSuccess(List<ParkingLocation> locations) {
                if (isFinishing() || isDestroyed()) return;
                binding.progressBar.setVisibility(View.GONE);

                if (locations.isEmpty()) {
                    Toast.makeText(LocationSelectionActivity.this, "No locations found", Toast.LENGTH_SHORT).show();
                    return;
                }

                binding.recyclerLocations.setVisibility(View.VISIBLE);

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
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(LocationSelectionActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}