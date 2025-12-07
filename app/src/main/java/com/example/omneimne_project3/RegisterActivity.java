package com.example.omneimne_project3;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private EditText nameEdit, emailEdit, passwordEdit, confirmPasswordEdit;
    private Button registerBtn;
    private TextView loginLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEdit = findViewById(R.id.nameEdit);
        emailEdit = findViewById(R.id.emailEditRegister);
        passwordEdit = findViewById(R.id.passwordEditRegister);
        confirmPasswordEdit = findViewById(R.id.confirmPasswordEdit);
        registerBtn = findViewById(R.id.registerBtn);
        loginLink = findViewById(R.id.loginLink);
        progressBar = findViewById(R.id.progressBarRegister);

        registerBtn.setOnClickListener(v -> registerUser());

        loginLink.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = nameEdit.getText().toString().trim();
        String email = emailEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String confirmPassword = confirmPasswordEdit.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEdit.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEdit.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEdit.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            passwordEdit.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEdit.setError("Passwords do not match");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        registerBtn.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            createUserDocument(user.getUid(), name, email);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        registerBtn.setEnabled(true);
                        String errorMessage = "Registration failed";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        Toast.makeText(RegisterActivity.this, errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserDocument(String userId, String name, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("displayName", name);
        user.put("email", email);
        user.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RegisterActivity.this,
                            "Registration successful!", Toast.LENGTH_SHORT).show();
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    registerBtn.setEnabled(true);
                    Toast.makeText(RegisterActivity.this,
                            "Failed to create user profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}