package com.example.mybarber.model;

import com.google.firebase.Timestamp;

public class Comment {
    private String id;
    private String userId;
    private String userName;
    private String text;
    private Timestamp timestamp;

    // Empty constructor for Firestore
    public Comment() {
    }

    public Comment(String userId, String userName, String text, Timestamp timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}