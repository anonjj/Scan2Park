package com.example.parkeasy;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.os.Handler;
import android.os.Looper;
import com.example.parkeasy.adapter.TransactionAdapter;
import com.example.parkeasy.databinding.ActivityWalletBinding;
import com.example.parkeasy.model.User;
import com.example.parkeasy.util.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Random;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private String userId;
    private int pendingLoads = 0;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.btnBack.setOnClickListener(v -> finish());
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerTransactions.setHasFixedSize(true);
        binding.recyclerTransactions.setItemViewCacheSize(16);
        transactionAdapter = new TransactionAdapter(new java.util.ArrayList<>());
        binding.recyclerTransactions.setAdapter(transactionAdapter);

        loadWalletData();
        setupAddMoney();
    }

    private void loadWalletData() {
        if (!NetworkUtils.isOnline(this)) {
            setLoading(false);
            showEmptyTransactions("No internet connection");
            Toast.makeText(this, "Please check your connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        pendingLoads = 2;
        setLoading(true);
        // 1. Get Balance
        com.example.parkeasy.data.FirebaseManager.getInstance().getUserData(userId, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    binding.tvWalletBalance.setText("₹" + user.getWalletBalance() + ".00");
                }
                onWalletLoadFinished();
            }
            @Override public void onFailure(Exception e) {
                onWalletLoadFinished();
                Toast.makeText(WalletActivity.this, "Failed to load balance.", Toast.LENGTH_SHORT).show();
            }
        });

        // 2. Get History
        com.example.parkeasy.data.FirebaseManager.getInstance().fetchTransactions(userId, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<java.util.List<com.example.parkeasy.model.Transaction>>() {
            @Override
            public void onSuccess(java.util.List<com.example.parkeasy.model.Transaction> result) {
                transactionAdapter.updateData(result);
                if (result == null || result.isEmpty()) {
                    showEmptyTransactions("No transactions yet");
                } else {
                    binding.recyclerTransactions.setVisibility(View.VISIBLE);
                    binding.tvEmptyTransactions.setVisibility(View.GONE);
                }
                onWalletLoadFinished();
            }
            @Override public void onFailure(Exception e) {
                showEmptyTransactions("Unable to load transactions");
                onWalletLoadFinished();
                Toast.makeText(WalletActivity.this, "Failed to load transactions.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAddMoney() {
        binding.btnAddMoney.setOnClickListener(v -> {
            String amountStr = binding.etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            // Open the Fake Payment Gateway
            showPaymentGateway(amount);
        });
    }

    private void showPaymentGateway(double amount) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_razorpay, null);
        bottomSheetDialog.setContentView(sheetView);
        View parent = (View) sheetView.getParent();
        if (parent != null) {
            parent.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        }

        // 1. Find Views
        TextView tvAmount = sheetView.findViewById(R.id.tvPayAmount);
        View btnUpi = sheetView.findViewById(R.id.layoutOptionUpi);
        View btnCard = sheetView.findViewById(R.id.layoutOptionCard);
        android.widget.RadioButton radioUpi = sheetView.findViewById(R.id.radioUpi);
        android.widget.RadioButton radioCard = sheetView.findViewById(R.id.radioCard);
        View btnConfirm = sheetView.findViewById(R.id.btnConfirmPayment);
        View processingLayout = sheetView.findViewById(R.id.layoutProcessing);
        TextView tvPaymentStatus = sheetView.findViewById(R.id.tvPaymentStatus);

        // 2. Set Initial Data
        tvAmount.setText("₹" + (int) amount);

        // 3. Selection Logic (The missing part!)
        btnUpi.setOnClickListener(v -> {
            setPaymentOptionSelected(true, btnUpi, btnCard, radioUpi, radioCard);
        });

        btnCard.setOnClickListener(v -> {
            setPaymentOptionSelected(false, btnUpi, btnCard, radioUpi, radioCard);
        });

        setPaymentOptionSelected(true, btnUpi, btnCard, radioUpi, radioCard);

        // 4. Confirm Payment
        btnConfirm.setOnClickListener(v2 -> {
            if (!radioUpi.isChecked() && !radioCard.isChecked()) {
                Toast.makeText(this, "Select a payment method", Toast.LENGTH_SHORT).show();
                return;
            }
            btnConfirm.setEnabled(false);
            processingLayout.setVisibility(View.VISIBLE);
            if (tvPaymentStatus != null) {
                tvPaymentStatus.setVisibility(View.GONE);
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean isSuccess = new Random().nextInt(100) < 85;
                if (isSuccess) {
                    Toast.makeText(WalletActivity.this, "Payment successful. Adding to wallet...", Toast.LENGTH_SHORT).show();
                    bottomSheetDialog.dismiss();
                    processTopUp(amount);
                    return;
                }

                processingLayout.setVisibility(View.GONE);
                btnConfirm.setEnabled(true);
                if (tvPaymentStatus != null) {
                    tvPaymentStatus.setText("Payment failed. Please try again.");
                    tvPaymentStatus.setVisibility(View.VISIBLE);
                }
            }, 1200);
        });

        bottomSheetDialog.show();
    }

    private void setPaymentOptionSelected(
            boolean isUpi,
            View btnUpi,
            View btnCard,
            android.widget.RadioButton radioUpi,
            android.widget.RadioButton radioCard
    ) {
        radioUpi.setChecked(isUpi);
        radioCard.setChecked(!isUpi);
        btnUpi.setAlpha(isUpi ? 1.0f : 0.5f);
        btnCard.setAlpha(isUpi ? 0.5f : 1.0f);
        android.content.res.ColorStateList activeTint =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00FF88"));
        android.content.res.ColorStateList inactiveTint =
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7A8BA0"));
        radioUpi.setButtonTintList(isUpi ? activeTint : inactiveTint);
        radioCard.setButtonTintList(isUpi ? inactiveTint : activeTint);
    }

    private void processTopUp(double amount) {
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show Loading (Reuse the activity button to show state)
        binding.btnAddMoney.setEnabled(false);
        binding.btnAddMoney.setText("PROCESSING...");

        com.example.parkeasy.data.FirebaseManager.getInstance().addMoneyToWallet(userId, amount, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Success!
                Toast.makeText(WalletActivity.this, "Payment Successful!", Toast.LENGTH_LONG).show();
                binding.etAmount.setText("");
                binding.btnAddMoney.setEnabled(true);
                binding.btnAddMoney.setText("ADD");
                loadWalletData(); // Refresh Balance
            }

            @Override
            public void onFailure(Exception e) {
                binding.btnAddMoney.setEnabled(true);
                binding.btnAddMoney.setText("ADD");
                Toast.makeText(WalletActivity.this, "Transaction failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onWalletLoadFinished() {
        pendingLoads = Math.max(0, pendingLoads - 1);
        if (pendingLoads == 0) {
            setLoading(false);
        }
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerTransactions.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            binding.tvEmptyTransactions.setVisibility(View.GONE);
        }
    }

    private void showEmptyTransactions(String message) {
        binding.recyclerTransactions.setVisibility(View.GONE);
        binding.tvEmptyTransactions.setText(message);
        binding.tvEmptyTransactions.setVisibility(View.VISIBLE);
    }
}
