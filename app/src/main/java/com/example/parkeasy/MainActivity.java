package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityLoginBinding;
import com.example.parkeasy.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onStart() {
        super.onStart();
        // AUTO-LOGIN: If user is already signed in, skip to Dashboard
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            goToDashboard();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1. Handle "Sign In" Button
        binding.btnLogin.setOnClickListener(v -> loginUser());

        // 2. Handle "Sign Up" Link
        binding.tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        // Get inputs from the UI
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Basic Validation
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError("Email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etPassword.setError("Password is required");
            return;
        }

        // Show Loading State
        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("Verifying...");

        // Call Firebase Engine
        FirebaseManager.getInstance().loginUser(email, password, new FirebaseManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User result) {
                binding.btnLogin.setEnabled(true);
                binding.btnLogin.setText("Sign In");
                goToDashboard();
            }

            @Override
            public void onFailure(Exception e) {
                binding.btnLogin.setEnabled(true);
                binding.btnLogin.setText("Sign In");
                Toast.makeText(MainActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToDashboard() {
        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
        // Clear back stack so user can't press "Back" to return to Login
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
