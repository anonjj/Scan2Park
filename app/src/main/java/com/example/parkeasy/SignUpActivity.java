package com.example.parkeasy;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parkeasy.data.FirebaseManager;

import com.example.parkeasy.databinding.ActivitySignUpBinding;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSignUp.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {
        // 1. Get Inputs
        String name = binding.etFullName.getText().toString().trim();
        String mobile = binding.etMobile.getText().toString().trim();
        String email = binding.etEmailSign.getText().toString().trim();
        String password = binding.etPasswordSign.getText().toString().trim();

        // 2. Validation (Don't let them send empty data)
        if (TextUtils.isEmpty(name)) {
            binding.etFullName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(mobile)) {
            binding.etMobile.setError("Mobile is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmailSign.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            binding.etPasswordSign.setError("Password must be at least 6 chars");
            return;
        }

        // 3. UI Feedback (Show user something is happening)
        binding.btnSignUp.setEnabled(false);
        binding.btnSignUp.setText("CREATING ID...");

        // 4. Call Firebase Engine
        FirebaseManager.getInstance().createUser(email, password, name, mobile, new FirebaseManager.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // Success!
                Toast.makeText(SignUpActivity.this, "Account Created! Please Login.", Toast.LENGTH_LONG).show();
                finish(); // Close this screen and go back to Login
            }

            @Override
            public void onFailure(Exception e) {
                // Failure
                binding.btnSignUp.setEnabled(true);
                binding.btnSignUp.setText("Create Account");
                Toast.makeText(SignUpActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}