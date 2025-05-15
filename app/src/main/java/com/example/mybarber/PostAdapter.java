package com.example.mybarber;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mybarber.model.Post;
import com.example.mybarber.BarberProfileActivity;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> posts;

    public PostAdapter(Context context, List<Post> posts) {
        this.context = context;
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        // Set user name
        holder.textUserName.setText(post.getUserName());

        // Load post image using base64 data
        if (post.getImageData() != null && !post.getImageData().isEmpty()) {
            Bitmap bitmap = convertBase64ToBitmap(post.getImageData());
            if (bitmap != null) {
                holder.imagePost.setImageBitmap(bitmap);
            }
        }

        // Set description
        if (post.getDescription() != null && !post.getDescription().isEmpty()) {
            holder.textDescription.setVisibility(View.VISIBLE);
            holder.textDescription.setText(post.getDescription());
        } else {
            holder.textDescription.setVisibility(View.GONE);
        }

        // Format and set timestamp
        if (post.getTimestamp() != null) {
            Date date = post.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            holder.textTimestamp.setText(sdf.format(date));
        }

        // Set like count
        holder.textLikes.setText(String.valueOf(post.getLikes()));

        // Set up click listeners
        holder.textUserName.setOnClickListener(v -> {
            // Navigate to barber profile
            Intent intent = new Intent(context, BarberProfileActivity.class);
            intent.putExtra("BARBER_ID", post.getUserId());
            context.startActivity(intent);
        });

        holder.imageLike.setOnClickListener(v -> {
            // Handle like action
            // This would need to update Firestore in a real implementation
        });

        holder.imageComment.setOnClickListener(v -> {
            // Navigate to comments screen
            Intent intent = new Intent(context, PostDetailActivity.class);
            intent.putExtra("POST_ID", post.getPostId());
            context.startActivity(intent);
        });
    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageUserProfile;
        TextView textUserName;
        ImageView imagePost;
        TextView textDescription;
        TextView textTimestamp;
        ImageView imageLike;
        TextView textLikes;
        ImageView imageComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageUserProfile = itemView.findViewById(R.id.imageUserProfile);
            textUserName = itemView.findViewById(R.id.textUserName);
            imagePost = itemView.findViewById(R.id.imagePost);
            textDescription = itemView.findViewById(R.id.textDescription);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            imageLike = itemView.findViewById(R.id.imageLike);
            textLikes = itemView.findViewById(R.id.textLikes);
            imageComment = itemView.findViewById(R.id.imageComment);
        }
    }
}