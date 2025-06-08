package com.example.mybarber;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mybarber.model.Post;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BarberProfileActivity extends AppCompatActivity {

    private String barberId;
    private FirebaseFirestore db;

    private ImageView imageProfile;
    private TextView textName, textBio, textPostCount, textRatingCount, textLocation;
    private RatingBar ratingBar;
    private Button buttonBook;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_profile);

        // Get barber ID from intent
        barberId = getIntent().getStringExtra("BARBER_ID");
        if (barberId == null) {
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        imageProfile = findViewById(R.id.imageProfile);
        textName = findViewById(R.id.textName);
        textBio = findViewById(R.id.textBio);
        textLocation = findViewById(R.id.textLocation);
        textPostCount = findViewById(R.id.textPostCount);
        textRatingCount = findViewById(R.id.textRatingCount);
        ratingBar = findViewById(R.id.ratingBar);
        buttonBook = findViewById(R.id.buttonBook);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);

        // Set up RecyclerView
        postList = new ArrayList<>();
        recyclerViewPosts.setLayoutManager(new GridLayoutManager(this, 3));
        postAdapter = new PostAdapter(this, postList);
        recyclerViewPosts.setAdapter(postAdapter);

        // Load barber profile
        loadBarberProfile();

        // Load barber posts
        loadBarberPosts();

        // Set up booking button
        buttonBook.setOnClickListener(v -> {
            // Check if user is logged in
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, "Please login to book an appointment", Toast.LENGTH_SHORT).show();
                // Navigate to login activity
                Intent loginIntent = new Intent(this, Login.class);
                startActivity(loginIntent);
                return;
            }

            // Check if user is trying to book themselves (barber booking themselves)
            if (auth.getCurrentUser().getUid().equals(barberId)) {
                Toast.makeText(this, "You cannot book an appointment with yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            // Navigate to booking screen
            Intent bookingIntent = new Intent(this, BookingActivity.class);
            bookingIntent.putExtra("BARBER_ID", barberId);
            startActivity(bookingIntent);
        });
    }

    private void loadBarberProfile() {
        db.collection("users").document(barberId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Set barber name
                        String name = documentSnapshot.getString("displayName");
                        if (name != null) {
                            textName.setText(name);
                        }

                        // Handle profile image - check both URL and base64
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        String profileImageBase64 = documentSnapshot.getString("profileImage");

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            // Load from URL using Glide
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(imageProfile);
                        } else if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            // Load from base64
                            try {
                                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                // Apply circular crop to the bitmap
                                Glide.with(this)
                                        .load(decodedByte)
                                        .circleCrop()
                                        .into(imageProfile);
                            } catch (Exception e) {
                                e.printStackTrace();
                                // If base64 decoding fails, use default image
                                imageProfile.setImageResource(R.drawable.defualtprofile);
                            }
                        } else {
                            // No profile image, use default
                            imageProfile.setImageResource(R.drawable.defualtprofile);
                        }

                        // Set location if exists
                        String location = documentSnapshot.getString("location");
                        if (location != null && !location.isEmpty()) {
                            textLocation.setVisibility(View.VISIBLE);
                            textLocation.setText("📍 " + location);
                        } else {
                            textLocation.setVisibility(View.GONE);
                        }

                        // Set bio if exists
                        String bio = documentSnapshot.getString("bio");
                        if (bio != null && !bio.isEmpty()) {
                            textBio.setVisibility(View.VISIBLE);
                            textBio.setText(bio);
                        } else {
                            textBio.setVisibility(View.GONE);
                        }

                        // Set rating if exists
                        Double rating = documentSnapshot.getDouble("rating");
                        if (rating != null) {
                            ratingBar.setRating(rating.floatValue());
                        }

                        // Set rating count
                        Long ratingCount = documentSnapshot.getLong("ratingCount");
                        if (ratingCount != null) {
                            textRatingCount.setText("(" + ratingCount + ")");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading barber profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadBarberPosts() {
        db.collection("posts")
                .whereEqualTo("userId", barberId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    postList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Post post = document.toObject(Post.class);
                        if (post != null) {
                            post.setPostId(document.getId());
                            postList.add(post);
                        }
                    }
                    postAdapter.notifyDataSetChanged();
                    textPostCount.setText(String.valueOf(postList.size()));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading posts", Toast.LENGTH_SHORT).show();
                });
    }
}