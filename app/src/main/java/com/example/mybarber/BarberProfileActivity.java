package com.example.mybarber;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mybarber.model.Post;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class BarberProfileActivity extends AppCompatActivity {

    private String barberId;
    private FirebaseFirestore db;

    private ImageView imageProfile;
    private TextView textName, textBio, textPostCount, textRatingCount;
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
            // Navigate to booking screen
            // This would be implemented in a future update
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

                        // Set profile image if exists
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(imageProfile);
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
                });
    }
}