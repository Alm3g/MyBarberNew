package com.example.mybarber.home;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.mybarber.AddPostActivity;
import com.example.mybarber.Login;
import com.example.mybarber.MainActivity;
import com.example.mybarber.PostAdapter;
import com.example.mybarber.R;
import com.example.mybarber.model.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements View.OnClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FloatingActionButton fabAddPost;
    private boolean isBarber = false;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerViewPosts = view.findViewById(R.id.recyclerViewPosts);
        fabAddPost = view.findViewById(R.id.fabAddPost);

        // Set up RecyclerView
        postList = new ArrayList<>();
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        postAdapter = new PostAdapter(getContext(), postList);
        recyclerViewPosts.setAdapter(postAdapter);

        // Check if user is barber and configure UI accordingly
        checkIfUserIsBarber();

        // Set click listener for add post button
        fabAddPost.setOnClickListener(this);

        // Load posts
        loadPosts();

        return view;
    }

    private void checkIfUserIsBarber() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Boolean isBarberValue = task.getResult().getBoolean("IsBarber");
                            isBarber = isBarberValue != null && isBarberValue;

                            // Show/hide add post button based on user type
                            fabAddPost.setVisibility(isBarber ? View.VISIBLE : View.GONE);
                        }
                    });
        }
    }

    private void loadPosts() {
        // Query Firestore for posts, sorted by timestamp
        db.collection("posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        postList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            if (post != null) {
                                post.setPostId(document.getId());
                                postList.add(post);
                            }
                        }
                        postAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Error loading posts", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fabAddPost) {
            // Navigate to add post activity
            Intent intent = new Intent(getActivity(), AddPostActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh posts when returning to this fragment
        loadPosts();
    }
}