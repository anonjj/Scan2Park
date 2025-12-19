package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parkeasy.databinding.ActivityBookingSummaryBinding;

public class BookingSummaryActivity extends AppCompatActivity {

    private ActivityBookingSummaryBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBookingSummaryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Get Booking Data from Intent
        String slotName = getIntent().getStringExtra("SLOT_NAME");
        String location = getIntent().getStringExtra("LOCATION_NAME");
        double totalCost = getIntent().getDoubleExtra("TOTAL_COST", 0.0);

        // 2. Update UI (Receipt Details)
        binding.tvReceiptLocation.setText(location);
        binding.tvReceiptSlot.setText(slotName);
        binding.tvReceiptAmount.setText("₹" + (int)totalCost);

        // 3. Home Button Logic
        binding.btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            // Clear the back stack so they can't go back to the receipt
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}