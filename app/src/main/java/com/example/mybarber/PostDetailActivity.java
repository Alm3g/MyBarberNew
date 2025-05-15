package com.example.mybarber;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mybarber.R;
import com.example.mybarber.model.Comment;
import com.example.mybarber.model.Post;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostDetailActivity extends AppCompatActivity {

    private String postId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private ImageView imagePost, imageUserProfile;
    private TextView textUserName, textDescription, textTimestamp, textLikes;
    private RecyclerView recyclerViewComments;
    private EditText editComment;
    private Button buttonPostComment;

    private List<Comment> commentList;
    private CommentAdapter commentAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // Get post ID from intent
        postId = getIntent().getStringExtra("POST_ID");
        if (postId == null) {
            finish();
            return;
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        imagePost = findViewById(R.id.imagePost);
        imageUserProfile = findViewById(R.id.imageUserProfile);
        textUserName = findViewById(R.id.textUserName);
        textDescription = findViewById(R.id.textDescription);
        textTimestamp = findViewById(R.id.textTimestamp);
        textLikes = findViewById(R.id.textLikes);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        editComment = findViewById(R.id.editComment);
        buttonPostComment = findViewById(R.id.buttonPostComment);

        // Set up RecyclerView for comments
        commentList = new ArrayList<>();
        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(this, commentList);
        recyclerViewComments.setAdapter(commentAdapter);

        // Load post details
        loadPostDetails();

        // Load comments
        loadComments();

        // Set up comment posting
        buttonPostComment.setOnClickListener(v -> postComment());
    }

    private void loadPostDetails() {
        db.collection("posts").document(postId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Post post = documentSnapshot.toObject(Post.class);
                        if (post != null) {
                            // Set user name
                            textUserName.setText(post.getUserName());

                            if (post.getImageData() != null && !post.getImageData().isEmpty()) {
                                try {
                                    byte[] decodedBytes = Base64.decode(post.getImageData(), Base64.DEFAULT);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                    imagePost.setImageBitmap(bitmap);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            // Set description
                            if (post.getDescription() != null && !post.getDescription().isEmpty()) {
                                textDescription.setVisibility(View.VISIBLE);
                                textDescription.setText(post.getDescription());
                            } else {
                                textDescription.setVisibility(View.GONE);
                            }

                            // Format and set timestamp
                            if (post.getTimestamp() != null) {
                                Date date = post.getTimestamp().toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                                textTimestamp.setText(sdf.format(date));
                            }

                            // Set like count
                            textLikes.setText(String.valueOf(post.getLikes()));

                            // Load user profile image
                            db.collection("users").document(post.getUserId())
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        if (userSnapshot.exists()) {
                                            String profileImageUrl = userSnapshot.getString("profileImageUrl");
                                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                                Glide.with(this)
                                                        .load(profileImageUrl)
                                                        .circleCrop()
                                                        .into(imageUserProfile);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void loadComments() {
        db.collection("posts").document(postId).collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        commentList.clear();
                        for (DocumentSnapshot document : value) {
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null) {
                                comment.setId(document.getId());
                                commentList.add(comment);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();

                        // Scroll to bottom to show latest comments
                        if (commentList.size() > 0) {
                            recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
                        }
                    }
                });
    }

    private void postComment() {
        String commentText = editComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to comment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create comment data
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("userId", currentUser.getUid());
        commentData.put("userName", currentUser.getDisplayName());
        commentData.put("text", commentText);
        commentData.put("timestamp", Timestamp.now());

        // Add to Firestore
        db.collection("posts").document(postId).collection("comments")
                .add(commentData)
                .addOnSuccessListener(documentReference -> {
                    editComment.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error posting comment", Toast.LENGTH_SHORT).show();
                });
    }

    // Comment adapter class would typically be defined as a separate file
    // but for simplicity, we'll define it as inner class
    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

        private Context context;
        private List<Comment> comments;

        public CommentAdapter(Context context, List<Comment> comments) {
            this.context = context;
            this.comments = comments;
        }

        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            Comment comment = comments.get(position);

            holder.textUserName.setText(comment.getUserName());
            holder.textComment.setText(comment.getText());

            if (comment.getTimestamp() != null) {
                Date date = comment.getTimestamp().toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
                holder.textTimestamp.setText(sdf.format(date));
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        static class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView textUserName, textComment, textTimestamp;

            public CommentViewHolder(@NonNull View itemView) {
                super(itemView);
                textUserName = itemView.findViewById(R.id.textUserName);
                textComment = itemView.findViewById(R.id.textComment);
                textTimestamp = itemView.findViewById(R.id.textTimestamp);
            }
        }
    }
}