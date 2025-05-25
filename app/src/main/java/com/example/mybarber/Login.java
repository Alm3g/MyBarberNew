package com.example.mybarber;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class Login extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth mAuth;

    EditText emailInput, passwordInput;
    Button loginbtn;
    TextView signuptext, forgotPasswordText;
    private int failedPasswordAttempts = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.loginemail);
        passwordInput = findViewById(R.id.loginpassword);
        loginbtn = findViewById(R.id.loginbtn);
        signuptext = findViewById(R.id.Signuptext);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        signuptext.setOnClickListener(this);
        loginbtn.setOnClickListener(this);
        forgotPasswordText.setOnClickListener(this);
        forgotPasswordText.setVisibility(View.GONE);
    }

    private boolean validateInputs(String email, String password) {
        // Reset previous errors
        emailInput.setError(null);
        passwordInput.setError(null);

        boolean isValid = true;

        // Validate email
        if (email.trim().isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            emailInput.setError("Please enter a valid email address");
            emailInput.requestFocus();
            isValid = false;
        }

        // Validate password
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            if (isValid) passwordInput.requestFocus(); // Only focus if email is valid
            isValid = false;
        } else if (password.length() < 6) {
            passwordInput.setError("Password must be at least 6 characters");
            if (isValid) passwordInput.requestFocus();
            isValid = false;
        }

        return isValid;
    }

    private void performLogin(String email, String password) {
        // Disable login button and show progress
        loginbtn.setEnabled(false);
        loginbtn.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email.trim(), password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Re-enable login button
                        loginbtn.setEnabled(true);
                        loginbtn.setText("login");

                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), bottomnav.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Handle specific Firebase Auth exceptions
                            String errorMessage = "Authentication failed.";

                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                errorMessage = "No account found with this email address.";
                                emailInput.setError("Account not found");
                                emailInput.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Invalid password. Please try again.";
                                passwordInput.setError("Incorrect password");
                                passwordInput.requestFocus();

                                // Increment failed attempts and show forgot password after first failure
                                failedPasswordAttempts++;
                                if (failedPasswordAttempts >= 1) {
                                    forgotPasswordText.setVisibility(View.VISIBLE);
                                }
                            } else if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }

                            Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void sendPasswordResetEmail(String email) {
        if (email.trim().isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            Toast.makeText(this, "Please enter a valid email address first", Toast.LENGTH_SHORT).show();
            emailInput.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email.trim())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(Login.this, "Password reset email sent to " + email.trim(), Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = "Failed to send reset email.";
                            if (task.getException() != null) {
                                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                    errorMessage = "No account found with this email address.";
                                } else {
                                    errorMessage = task.getException().getMessage();
                                }
                            }
                            Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view == loginbtn) {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            // Validate inputs before attempting login
            if (validateInputs(email, password)) {
                performLogin(email, password);
            }
        }

        if (view == signuptext) {
            Intent intent = new Intent(getApplicationContext(), SignUp.class);
            startActivity(intent);
            finish();
        }

        if (view == forgotPasswordText) {
            String email = emailInput.getText().toString();
            sendPasswordResetEmail(email);
        }
    }
}