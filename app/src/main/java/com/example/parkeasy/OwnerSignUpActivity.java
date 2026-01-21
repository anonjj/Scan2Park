package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityOwnerSignUpBinding;
import com.example.parkeasy.model.Owner;
import com.example.parkeasy.util.NetworkUtils;

public class OwnerSignUpActivity extends AppCompatActivity {

    private ActivityOwnerSignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerSignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnOwnerSignUp.setOnClickListener(v -> createOwnerAccount());
        binding.tvOwnerBackToLogin.setOnClickListener(v -> finish());
    }

    private void createOwnerAccount() {
        String name = binding.etOwnerName.getText().toString().trim();
        String businessName = binding.etBusinessName.getText().toString().trim();
        String phone = binding.etOwnerPhone.getText().toString().trim();
        String email = binding.etOwnerEmail.getText().toString().trim();
        String password = binding.etOwnerPassword.getText().toString().trim();
        String businessReg = binding.etBusinessReg.getText().toString().trim();
        String yearsInOp = binding.etYearsOperation.getText().toString().trim();
        String licenseId = binding.etLicenseId.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            binding.etOwnerName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(businessName)) {
            binding.etBusinessName.setError("Business name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            binding.etOwnerPhone.setError("Phone number is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etOwnerEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etOwnerPassword.setError("Password must be at least 6 chars");
            return;
        }
        if (TextUtils.isEmpty(businessReg)) {
            binding.etBusinessReg.setError("Registration number required");
            return;
        }
        if (TextUtils.isEmpty(yearsInOp)) {
            binding.etYearsOperation.setError("Years in operation required");
            return;
        }
        if (TextUtils.isEmpty(licenseId)) {
            binding.etLicenseId.setError("License/permit ID required");
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnOwnerSignUp.setEnabled(false);
        binding.btnOwnerSignUp.setText("Creating...");

        Owner owner = new Owner();
        owner.setFullName(name);
        owner.setBusinessName(businessName);
        owner.setPhoneNumber(phone);
        owner.setBusinessRegNumber(businessReg);
        owner.setYearsInOperation(yearsInOp);
        owner.setLicenseId(licenseId);

        FirebaseManager.getInstance().createOwner(email, password, owner, new FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(OwnerSignUpActivity.this, "Owner account created!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(OwnerSignUpActivity.this, OwnerDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                binding.btnOwnerSignUp.setEnabled(true);
                binding.btnOwnerSignUp.setText("Create Owner Account");
                Toast.makeText(OwnerSignUpActivity.this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
