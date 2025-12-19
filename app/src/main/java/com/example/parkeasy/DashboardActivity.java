package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;

/**
 * The main screen after a user logs in.
 * Serves as the central navigation hub for the app's core features.
 * Displays user-specific info like name and wallet balance.
 */
public class DashboardActivity extends AppCompatActivity {

    private ActivityDashboardBinding binding;
    // Singleton instance for data operations
    private final FirebaseManager firebaseManager = FirebaseManager.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //com.example.parkeasy.data.ParkingSeeder.seedData();

        // Set a dynamic greeting based on the time of day (Morning/Evening).
        binding.tvGreeting.setText(getGreeting());

        // Wire up all the buttons and navigation elements.
        setupClickListeners();

        // Fetch user-specific data to populate the UI (Wallet, Name).
        loadUserData();
    }

    private void setupClickListeners() {
        // --- Core Feature Navigation ---

        // 1. Book Parking (Main Action)
        binding.btnBookParking.setOnClickListener(v ->
                startActivity(new Intent(this, LocationSelectionActivity.class))
        );

        // 2. Booking History
        binding.btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, BookingHistoryActivity.class))
        );

        // 3. Wallet Access (Clicking the Balance Card OR the Add Button)
        binding.cardWallet.setOnClickListener(v ->
                startActivity(new Intent(this, WalletActivity.class))
        );
        binding.btnAddMoney.setOnClickListener(v ->
                startActivity(new Intent(this, WalletActivity.class))
        );

        // --- Header & Profile Actions ---

        // Quick logout action attached to the welcome message (Hidden feature)
        binding.tvWelcome.setOnClickListener(v -> {
            firebaseManager.logout();
            Intent intent = new Intent(this, MainActivity.class);
            // Clear the activity stack so back button doesn't return here
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Profile Avatar Click
        binding.ivAvatar.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class))
        );

        // --- Bottom Navigation Handler ---
        // Ensure "Home" is visually selected
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true; // Already here
            } else if (id == R.id.nav_parking) {
                startActivity(new Intent(this, LocationSelectionActivity.class));
                return true;
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, BookingHistoryActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    /**
     * Fetches the current user's data from FirebaseManager and updates the UI.
     * Handles the case where the user might be a guest (not logged in).
     */
    private void loadUserData() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Sanity check: If no user is logged in, show a default guest state.
        if (currentUser == null) {
            binding.tvWelcome.setText("Welcome Back,\nGuest");
            binding.tvWalletBalance.setText("₹0.00");
            return;
        }

        // Fetch the user's profile payload (Name & Wallet Balance).
        firebaseManager.getUserData(
                currentUser.getUid(),
                new FirebaseManager.FirestoreCallback<com.example.parkeasy.model.User>() {
                    @Override
                    public void onSuccess(com.example.parkeasy.model.User user) {
                        if (user != null) {
                            // Personalize the welcome message.
                            String name = (user.getFullName() != null && !user.getFullName().isEmpty()) ? user.getFullName() : "Driver";
                            binding.tvWelcome.setText("Welcome Back,\n" + name);

                            // Update wallet display dynamically.
                            binding.tvWalletBalance.setText("₹" + user.getWalletBalance() + ".00");
                        } else {
                            // Fallback if user record is missing in Firestore.
                            binding.tvWelcome.setText("Welcome Back,\nDriver");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Fail gracefully on network error.
                        binding.tvWelcome.setText("Welcome Back,\nDriver");
                    }
                }
        );
    }

    /**
     * Helper: Provides a time-appropriate greeting message.
     */
    private String getGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return "Good Morning";
        } else if (hour < 17) {
            return "Good Afternoon";
        } else {
            return "Good Evening";
        }
    }

    /**
     * Refresh user data every time the screen is displayed.
     * Ensures wallet balance is up-to-date after booking/top-up.
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        com.example.parkeasy.data.ParkingJanitor.freeExpiredSlots();
    }
}