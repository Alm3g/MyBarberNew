package com.example.mybarber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mybarber.model.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int MAX_IMAGE_SIZE = 1024; // Maximum width/height for resizing

    private ImageView imagePreview;
    private EditText editDescription;
    private Button buttonSelectImage, buttonPost;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri imageUri;
    private String base64Image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        imagePreview = findViewById(R.id.imagePreview);
        editDescription = findViewById(R.id.editDescription);
        buttonSelectImage = findViewById(R.id.buttonSelectImage);
        buttonPost = findViewById(R.id.buttonPost);
        progressBar = findViewById(R.id.progressBar);

        // Set click listeners
        buttonSelectImage.setOnClickListener(this);
        buttonPost.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.buttonSelectImage) {
            openImageChooser();
        } else if (id == R.id.buttonPost) {
            uploadPost();
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                // Load and display the selected image
                imagePreview.setImageURI(imageUri);
                imagePreview.setVisibility(View.VISIBLE);

                // Convert the image to base64
                base64Image = convertImageToBase64(imageUri);
            } catch (Exception e) {
                Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String convertImageToBase64(Uri imageUri) throws FileNotFoundException {
        InputStream imageStream = getContentResolver().openInputStream(imageUri);
        Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);

        // Resize the bitmap to reduce file size
        Bitmap resizedBitmap = resizeBitmap(originalBitmap);

        // Convert to base64
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratio = (float) width / height;

        if (width > height && width > MAX_IMAGE_SIZE) {
            width = MAX_IMAGE_SIZE;
            height = (int) (width / ratio);
        } else if (height > MAX_IMAGE_SIZE) {
            height = MAX_IMAGE_SIZE;
            width = (int) (height * ratio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private void uploadPost() {
        if (base64Image == null) {
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        String description = editDescription.getText().toString().trim();

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonPost.setEnabled(false);

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to post", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create post data map
        Map<String, Object> postData = new HashMap<>();
        postData.put("userId", currentUser.getUid());
        postData.put("userName", currentUser.getDisplayName());
        postData.put("imageData", base64Image); // Store image as base64 string
        postData.put("description", description);
        postData.put("timestamp", Timestamp.now());
        postData.put("likes", 0);

        // Save post to Firestore
        db.collection("posts")
                .add(postData)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddPostActivity.this, "Post uploaded successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    buttonPost.setEnabled(true);
                    Toast.makeText(AddPostActivity.this, "Error adding post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}