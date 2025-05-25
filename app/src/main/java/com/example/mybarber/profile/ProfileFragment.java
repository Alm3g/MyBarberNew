package com.example.mybarber.profile;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.mybarber.R;
import com.example.mybarber.settings.SettingsFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment implements View.OnClickListener {

    TextView username, emailText, roleText, ordersCount;
    ImageView userProfileImage;
    LinearLayout ordersLayout;
    ImageButton settings;
    Button editProfileBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isBarber = false;

    // Image handling
    private static final int GALLERY_REQUEST_CODE = 2001;
    private static final int CAMERA_REQUEST_CODE = 2002;
    private static final int PERMISSION_REQUEST_CODE = 2003;
    private String profileImageBase64 = null;
    private AlertDialog currentEditDialog = null;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        username = view.findViewById(R.id.username);
        emailText = view.findViewById(R.id.email);
        roleText = view.findViewById(R.id.role);
        ordersLayout = view.findViewById(R.id.ordersLayout);
        settings = view.findViewById(R.id.settings);
        editProfileBtn = view.findViewById(R.id.editProfileBtn);
        ordersCount = view.findViewById(R.id.xorders);
        userProfileImage = view.findViewById(R.id.userprofile);

        // Set click listeners
        settings.setOnClickListener(this);
        editProfileBtn.setOnClickListener(this);

        // Load user data
        loadUserData();

        return view;
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String displayName = documentSnapshot.getString("displayName");
                            String email = documentSnapshot.getString("email");
                            Boolean isBarberValue = documentSnapshot.getBoolean("isBarber");
                            String profileImageData = documentSnapshot.getString("profileImage");

                            // Update UI
                            if (displayName != null) username.setText(displayName);
                            if (email != null) emailText.setText(email);

                            isBarber = isBarberValue != null && isBarberValue;
                            roleText.setText(isBarber ? "Barber" : "Customer");

                            // Show/hide orders section based on user type
                            ordersLayout.setVisibility(isBarber ? View.VISIBLE : View.GONE);

                            // Load profile image
                            loadProfileImage(profileImageData);

                            // If barber, load actual order count
                            if (isBarber) {
                                ordersCount.setText("5");
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadProfileImage(String base64Image) {
        if (base64Image != null && !base64Image.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                userProfileImage.setImageBitmap(bitmap);
                profileImageBase64 = base64Image;
            } catch (Exception e) {
                userProfileImage.setImageResource(R.drawable.defualtprofile);
            }
        } else {
            userProfileImage.setImageResource(R.drawable.defualtprofile);
        }
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        currentEditDialog = builder.create();
        currentEditDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Initialize dialog views
        ImageView editProfileImage = dialogView.findViewById(R.id.editProfileImage);
        TextInputEditText editDisplayName = dialogView.findViewById(R.id.editDisplayName);
        TextInputEditText editEmail = dialogView.findViewById(R.id.editEmail);
        Button cancelBtn = dialogView.findViewById(R.id.cancelBtn);
        Button saveBtn = dialogView.findViewById(R.id.saveBtn);

        // Pre-fill current data
        editDisplayName.setText(username.getText().toString());
        editEmail.setText(editEmail.getText().toString());

        // Load current profile image
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                editProfileImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                editProfileImage.setImageResource(R.drawable.defualtprofile);
            }
        } else {
            editProfileImage.setImageResource(R.drawable.defualtprofile);
        }

        // Set click listeners
        editProfileImage.setOnClickListener(v -> showImagePickerDialog());
        cancelBtn.setOnClickListener(v -> currentEditDialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String newDisplayName = editDisplayName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();

            if (newDisplayName.isEmpty()) {
                editDisplayName.setError("Display name is required");
                return;
            }

            if (newEmail.isEmpty()) {
                editEmail.setError("Email is required");
                return;
            }

            updateUserProfile(newDisplayName, newEmail, isBarber, currentEditDialog);
        });

        currentEditDialog.show();
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Update Profile Picture")
                .setItems(new String[]{"Take Photo", "Choose from Gallery", "Remove Photo"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkPermissionsAndOpenCamera();
                            break;
                        case 1:
                            checkPermissionsAndOpenGallery();
                            break;
                        case 2:
                            removeProfileImage();
                            break;
                    }
                })
                .show();
    }

    private void checkPermissionsAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            openCamera();
        }
    }

    private void checkPermissionsAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(getContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    private void removeProfileImage() {
        profileImageBase64 = null;
        if (currentEditDialog != null) {
            ImageView editProfileImage = currentEditDialog.findViewById(R.id.editProfileImage);
            editProfileImage.setImageResource(R.drawable.defualtprofile);
        }
        Toast.makeText(getContext(), "Profile image will be removed when you save", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK && data != null) {
            Bitmap bitmap = null;

            try {
                if (requestCode == CAMERA_REQUEST_CODE) {
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == GALLERY_REQUEST_CODE) {
                    Uri selectedImageUri = data.getData();
                    InputStream inputStream = getActivity().getContentResolver().openInputStream(selectedImageUri);
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }

                if (bitmap != null) {
                    bitmap = resizeBitmap(bitmap, 300, 300);
                    profileImageBase64 = bitmapToBase64(bitmap);

                    // Update the image in the dialog
                    if (currentEditDialog != null) {
                        ImageView editProfileImage = currentEditDialog.findViewById(R.id.editProfileImage);
                        editProfileImage.setImageBitmap(bitmap);
                    }

                    Toast.makeText(getContext(), "Image updated! Save to apply changes.", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                Toast.makeText(getContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                showImagePickerDialog();
            } else {
                Toast.makeText(getContext(), "Permissions required for image selection", Toast.LENGTH_SHORT).show();
            }
        }
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void updateUserProfile(String newDisplayName, String newEmail, boolean newIsBarber, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Update Firestore document
        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", newDisplayName);
        updates.put("email", newEmail);
        updates.put("isBarber", newIsBarber);

        // Update profile image (or remove it if null)
        if (profileImageBase64 != null) {
            updates.put("profileImage", profileImageBase64);
        } else {
            updates.put("profileImage", null);
        }

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update Firebase Auth profile
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(newDisplayName)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Update email if it changed
                                    if (!newEmail.equals(user.getEmail())) {
                                        user.updateEmail(newEmail)
                                                .addOnCompleteListener(emailTask -> {
                                                    if (emailTask.isSuccessful()) {
                                                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                                        loadUserData(); // Refresh UI
                                                        dialog.dismiss();
                                                    } else {
                                                        Toast.makeText(getContext(), "Failed to update email: " + emailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    } else {
                                        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                        loadUserData(); // Refresh UI
                                        dialog.dismiss();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.settings) {
            SettingsFragment settingsFragment = new SettingsFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, settingsFragment)
                    .addToBackStack(null)
                    .commit();
        } else if (view.getId() == R.id.editProfileBtn) {
            showEditProfileDialog();
        }
    }
}