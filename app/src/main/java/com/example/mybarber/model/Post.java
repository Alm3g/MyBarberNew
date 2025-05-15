package com.example.mybarber.model;

import com.google.firebase.Timestamp;

public class Post {
    private String postId;
    private String userId;
    private String userName;
    private String imageData; // Base64 encoded image data
    private String description;
    private Timestamp timestamp;
    private int likes;

    // Empty constructor needed for Firestore
    public Post() {
    }

    public Post(String userId, String userName, String imageData, String description, Timestamp timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.imageData = imageData;
        this.description = description;
        this.timestamp = timestamp;
        this.likes = 0;
    }

    // Getters and setters
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getImageData() {
        return imageData;
    }

    public void setImageData(String imageData) {
        this.imageData = imageData;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }
}