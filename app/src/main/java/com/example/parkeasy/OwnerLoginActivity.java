package com.example.parkeasy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.parkeasy.data.FirebaseManager;
import com.example.parkeasy.databinding.ActivityOwnerLoginBinding;
import com.example.parkeasy.model.Owner;
import com.example.parkeasy.util.NetworkUtils;

public class OwnerLoginActivity extends AppCompatActivity {

    private ActivityOwnerLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOwnerLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnOwnerLogin.setOnClickListener(v -> loginOwner());
        binding.tvOwnerSignUp.setOnClickListener(v ->
                startActivity(new Intent(this, OwnerSignUpActivity.class)));
    }

    private void loginOwner() {
        String email = binding.etOwnerEmail.getText().toString().trim();
        String password = binding.etOwnerPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnOwnerLogin.setEnabled(false);
        binding.btnOwnerLogin.setText("Signing in...");

        FirebaseManager.getInstance().loginOwner(email, password, new FirebaseManager.FirestoreCallback<Owner>() {
            @Override
            public void onSuccess(Owner result) {
                Intent intent = new Intent(OwnerLoginActivity.this, OwnerDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                binding.btnOwnerLogin.setEnabled(true);
                binding.btnOwnerLogin.setText("Sign In");
                Toast.makeText(OwnerLoginActivity.this, "Owner login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
