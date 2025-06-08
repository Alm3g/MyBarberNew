package com.example.mybarber.model;

public class TimeSlot {
    private String time;
    private boolean available;

    public TimeSlot(String time, boolean available) {
        this.time = time;
        this.available = available;
    }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
