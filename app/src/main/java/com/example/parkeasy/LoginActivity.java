package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.parkeasy.data.FirebaseManager;

import com.example.parkeasy.databinding.ActivityLoginBinding;
import com.example.parkeasy.model.User;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvGoToSignUp.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));
    }

    private void loginUser() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseManager.getInstance().loginUser(email, password, new FirebaseManager.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User result) {
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LoginActivity.this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}