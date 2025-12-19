package com.example.parkeasy;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.parkeasy.adapter.TransactionAdapter;
import com.example.parkeasy.databinding.ActivityWalletBinding;
import com.example.parkeasy.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class WalletActivity extends AppCompatActivity {

    private ActivityWalletBinding binding;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWalletBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userId = FirebaseAuth.getInstance().getUid();

        binding.btnBack.setOnClickListener(v -> finish());
        binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));

        loadWalletData();
        setupAddMoney();
    }

    private void loadWalletData() {
        // 1. Get Balance
        com.example.parkeasy.data.FirebaseManager.getInstance().getUserData(userId, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    binding.tvWalletBalance.setText("₹" + user.getWalletBalance() + ".00");
                }
            }
            @Override public void onFailure(Exception e) {}
        });

        // 2. Get History
        com.example.parkeasy.data.FirebaseManager.getInstance().fetchTransactions(userId, new com.example.parkeasy.data.FirebaseManager.FirestoreCallback<java.util.List<com.example.parkeasy.model.Transaction>>() {
            @Override
            public void onSuccess(java.util.List<com.example.parkeasy.model.Transaction> result) {
                TransactionAdapter adapter = new TransactionAdapter(result);
                binding.recyclerTransactions.setAdapter(adapter);
            }
            @Override public void onFailure(Exception e) {}
        });
    }

    private void setupAddMoney() {
        binding.btnAddMoney.setOnClickListener(v -> {
            String amountStr = binding.etAmount.getText().toString();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            // Open the Fake Payment Gateway
            showPaymentGateway(amount);
        });
    }

    private void showPaymentGateway(double amount) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);

        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_payment, null);
        bottomSheetDialog.setContentView(sheetView);
        ((View) sheetView.getParent()).setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // 1. Find Views
        TextView tvAmount = sheetView.findViewById(R.id.tvPayAmount);
        View btnUpi = sheetView.findViewById(R.id.btnUpi);
        View btnCard = sheetView.findViewById(R.id.btnCard);
        android.widget.RadioButton radioUpi = sheetView.findViewById(R.id.radioUpi);
        android.widget.RadioButton radioCard = sheetView.findViewById(R.id.radioCard);
        View btnConfirm = sheetView.findViewById(R.id.btnConfirmPayment);

        // 2. Set Initial Data
        tvAmount.setText("₹" + amount);

        // 3. Selection Logic (The missing part!)
        btnUpi.setOnClickListener(v -> {
            radioUpi.setChecked(true);
            radioCard.setChecked(false);
            btnUpi.setAlpha(1.0f);
            btnCard.setAlpha(0.5f);
            radioUpi.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00FF88"))); // Green
            radioCard.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7A8BA0"))); // Gray
        });

        btnCard.setOnClickListener(v -> {
            radioCard.setChecked(true);
            radioUpi.setChecked(false);
            btnCard.setAlpha(1.0f);
            btnUpi.setAlpha(0.5f);
            radioCard.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00FF88"))); // Green
            radioUpi.setButtonTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#7A8BA0"))); // Gray
        });

        // 4. Confirm Payment
        btnConfirm.setOnClickListener(v2 -> {
            bottomSheetDialog.dismiss();
            processTopUp(amount);
        });

        bottomSheetDialog.show();
    }

    private void processTopUp(double amount) {
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
                Toast.makeText(WalletActivity.this, "Transaction Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}