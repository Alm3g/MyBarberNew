package com.example.mybarber;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mybarber.home.HomeFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;

public class SignUp extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    EditText displayName, Email, Password, ConfirmPassword, locationEditText;
    RadioGroup UserTypeGroup;
    RadioButton BarberButton, CustomerButton;
    ImageView profileImageView;
    LinearLayout locationContainer;
    Boolean IsBarber = null;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Button SignUp;
    TextView loginTextView;
    private FirebaseAuth mAuth;

    // Image handling
    private static final int GALLERY_REQUEST_CODE = 1001;
    private static final int CAMERA_REQUEST_CODE = 1002;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1003;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1004;
    private String profileImageBase64 = null;

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (BarberButton.isChecked()) {
            IsBarber = true;
            showLocationField();
        }
        if (CustomerButton.isChecked()) {
            IsBarber = false;
            hideLocationField();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        loginTextView = findViewById(R.id.logintext);
        loginTextView.setOnClickListener(this);
        displayName = findViewById(R.id.displayname);
        Email = findViewById(R.id.email);
        Password = findViewById(R.id.password);
        ConfirmPassword = findViewById(R.id.confirmpassword);
        locationEditText = findViewById(R.id.locationEditText);
        locationContainer = findViewById(R.id.locationContainer);
        UserTypeGroup = findViewById(R.id.userTypeGroup);
        BarberButton = findViewById(R.id.barberButton);
        CustomerButton = findViewById(R.id.customerButton);
        profileImageView = findViewById(R.id.profileImage);

        BarberButton.setOnCheckedChangeListener(this);
        CustomerButton.setOnCheckedChangeListener(this);
        SignUp = findViewById(R.id.signupbtn);
        SignUp.setOnClickListener(this);
        profileImageView.setOnClickListener(this);
        mAuth = FirebaseAuth.getInstance();

        // Initially hide location field
        hideLocationField();
    }

    private void showLocationField() {
        locationContainer.setVisibility(View.VISIBLE);
    }

    private void hideLocationField() {
        locationContainer.setVisibility(View.GONE);
        locationEditText.setText("");
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openCamera();
                            break;
                        case 1:
                            openGallery();
                            break;
                    }
                })
                .show();
    }

    private void checkPermissionsAndPickImage() {
        // For Android 13+ (API 33+), we need READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        String storagePermission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            storagePermission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            storagePermission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermissionGranted = ContextCompat.checkSelfPermission(this, storagePermission) == PackageManager.PERMISSION_GRANTED;

        if (!cameraPermissionGranted || !storagePermissionGranted) {
            // Request missing permissions
            java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();

            if (!cameraPermissionGranted) {
                permissionsToRequest.add(Manifest.permission.CAMERA);
            }
            if (!storagePermissionGranted) {
                permissionsToRequest.add(storagePermission);
            }

            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            showImagePickerDialog();
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);

    }

    private void openGallery() {
        String permission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Storage permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;

            try {
                if (requestCode == CAMERA_REQUEST_CODE) {
                    // Camera result
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST_CODE) {
                    // Gallery result
                    Uri selectedImageUri = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }

                if (bitmap != null) {
                    bitmap = resizeBitmap(bitmap, 300, 300);
                    profileImageView.setImageBitmap(bitmap);
                    profileImageBase64 = bitmapToBase64(bitmap);
                }

            } catch (Exception e) {
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;

            // Check if all requested permissions were granted
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;

                    // Check if user permanently denied the permission
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])) {
                        // User permanently denied permission, show dialog to go to settings
                        showPermissionDeniedDialog(permissions[i]);
                        return;
                    } else {
                        // User denied permission but can be asked again
                        Toast.makeText(this, "Permission " + permissions[i] + " is required", Toast.LENGTH_LONG).show();
                    }
                }
            }

            if (allPermissionsGranted) {
                showImagePickerDialog();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required for image selection", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showPermissionDeniedDialog(String permission) {
        String message;
        if (permission.equals(Manifest.permission.CAMERA)) {
            message = "Camera permission is required to take photos. Please enable it in app settings.";
        } else {
            message = "Storage permission is required to select images. Please enable it in app settings.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream); // 70% quality
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private boolean validateInputs(String username, String email, String password, String confirmPassword) {
        // Reset previous errors
        displayName.setError(null);
        Email.setError(null);
        Password.setError(null);
        ConfirmPassword.setError(null);
        locationEditText.setError(null);

        boolean isValid = true;

        // Validate display name
        if (username.trim().isEmpty()) {
            displayName.setError("Display name is required");
            isValid = false;
        } else if (username.trim().length() < 3) {
            displayName.setError("Display name must be at least 3 characters");
            isValid = false;
        } else if (username.trim().length() > 20) {
            displayName.setError("Display name must be less than 20 characters");
            isValid = false;
        }

        // Validate email
        if (email.trim().isEmpty()) {
            Email.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Email.setError("Please enter a valid email address");
            isValid = false;
        }

        // Validate password
        if (password.isEmpty()) {
            Password.setError("Password is required");
            isValid = false;
        } else if (password.length() < 6) {
            Password.setError("Password must be at least 6 characters");
            isValid = false;
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            ConfirmPassword.setError("Please confirm your password");
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            ConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        // Validate user type selection
        if (IsBarber == null) {
            Toast.makeText(this, "Please select user type (Barber or Customer)", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Validate location if user is a barber
        if (IsBarber != null && IsBarber) {
            String location = locationEditText.getText().toString().trim();
            if (location.isEmpty()) {
                locationEditText.setError("Location is required for barbers");
                Toast.makeText(this, "Please enter your location", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (location.length() < 3) {
                locationEditText.setError("Location must be at least 3 characters");
                isValid = false;
            } else if (location.length() > 100) {
                locationEditText.setError("Location must be less than 100 characters");
                isValid = false;
            }
        }

        return isValid;
    }

    private void checkUsernameUniqueness(String username, String email, String password) {
        SignUp.setEnabled(false);
        SignUp.setText("Checking...");

        db.collection("users")
                .whereEqualTo("displayName", username.trim())
                .get()
                .addOnCompleteListener(task -> {
                    SignUp.setEnabled(true);
                    SignUp.setText("sign up");

                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            displayName.setError("This username is already taken");
                            Toast.makeText(SignUp.this, "Username already exists. Please choose a different one.", Toast.LENGTH_SHORT).show();
                        } else {
                            createUserAccount(username, email, password);
                        }
                    } else {
                        Toast.makeText(SignUp.this, "Error checking username availability. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createUserAccount(String username, String email, String password) {
        SignUp.setEnabled(false);
        SignUp.setText("Creating Account...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        SignUp.setEnabled(true);
                        SignUp.setText("sign up");

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                try {
                                    String uid = user.getUid();

                                    HashMap<String, Object> userData = new HashMap<>();
                                    userData.put("uid", uid);
                                    userData.put("email", user.getEmail());
                                    userData.put("displayName", username.trim());
                                    userData.put("isBarber", IsBarber);
                                    userData.put("createdAt", System.currentTimeMillis());

                                    // Add profile image if selected
                                    if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                        userData.put("profileImage", profileImageBase64);
                                    }

                                    // Add location if user is a barber
                                    if (IsBarber != null && IsBarber) {
                                        String location = locationEditText.getText().toString().trim();
                                        userData.put("location", location);

                                        // Initialize barber-specific fields
                                        userData.put("rating", 0.0);
                                        userData.put("ratingCount", 0);
                                        userData.put("bio", ""); // Empty bio initially
                                    }

                                    db.collection("users").document(uid)
                                            .set(userData)
                                            .addOnSuccessListener(aVoid -> {
                                                updateUserProfile(user, username);
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(SignUp.this, "Error saving user data: " + e.getMessage(),
                                                        Toast.LENGTH_SHORT).show();
                                                user.delete();
                                            });

                                } catch (Exception e) {
                                    System.out.println("Error: " + e.getMessage());
                                    Toast.makeText(SignUp.this, "Error creating user profile", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            String errorMessage = "Authentication failed.";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(SignUp.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateUserProfile(FirebaseUser user, String username) {
        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(username.trim())
                .build();

        user.updateProfile(profileUpdate).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(SignUp.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getApplicationContext(), bottomnav.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(SignUp.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        if (view == SignUp) {
            String username = displayName.getText().toString();
            String email = Email.getText().toString();
            String password = Password.getText().toString();
            String confirmPassword = ConfirmPassword.getText().toString();

            if (validateInputs(username, email, password, confirmPassword)) {
                checkUsernameUniqueness(username, email, password);
            }
        }

        if (view == loginTextView) {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

        if (view == profileImageView) {
            checkPermissionsAndPickImage();
        }
    }
}