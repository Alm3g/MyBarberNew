package com.example.mybarber;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BarberCalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView selectedDateText;
    private RecyclerView timeSlotRecyclerView;
    private Button addTimeSlotButton, saveScheduleButton;
    private LinearLayout timeSlotContainer;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String selectedDate;
    private List<TimeSlot> timeSlots;
    private TimeSlotAdapter timeSlotAdapter;

    // Predefined time slots for quick selection
    private String[] defaultTimeSlots = {
            "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
            "12:00", "12:30", "13:00", "13:30", "14:00", "14:30",
            "15:00", "15:30", "16:00", "16:30", "17:00", "17:30",
            "18:00", "18:30", "19:00", "19:30", "20:00"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barber_calendar);

        initializeViews();
        initializeFirebase();
        setupCalendar();
        setupTimeSlots();
        setupButtons();
    }

    private void initializeViews() {
        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        timeSlotRecyclerView = findViewById(R.id.timeSlotRecyclerView);
        addTimeSlotButton = findViewById(R.id.addTimeSlotButton);
        saveScheduleButton = findViewById(R.id.saveScheduleButton);
        timeSlotContainer = findViewById(R.id.timeSlotContainer);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void setupCalendar() {
        // Set minimum date to today
        calendarView.setMinDate(System.currentTimeMillis());

        // Set selected date to today initially
        Calendar today = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getTime());
        selectedDateText.setText("Selected Date: " + formatDateForDisplay(selectedDate));

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                // Month is 0-indexed, so add 1
                selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                selectedDateText.setText("Selected Date: " + formatDateForDisplay(selectedDate));
                loadExistingSchedule();
            }
        });

        // Load existing schedule for today
        loadExistingSchedule();
    }

    private void setupTimeSlots() {
        timeSlots = new ArrayList<>();
        timeSlotAdapter = new TimeSlotAdapter(timeSlots);
        timeSlotRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        timeSlotRecyclerView.setAdapter(timeSlotAdapter);
    }

    private void setupButtons() {
        addTimeSlotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeSlotSelectionDialog();
            }
        });

        saveScheduleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSchedule();
            }
        });
    }

    private void showTimeSlotSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Time Slots");

        // Create checkboxes for each default time slot
        boolean[] checkedItems = new boolean[defaultTimeSlots.length];
        String[] displayTimeSlots = new String[defaultTimeSlots.length];

        for (int i = 0; i < defaultTimeSlots.length; i++) {
            displayTimeSlots[i] = formatTimeForDisplay(defaultTimeSlots[i]);
        }

        builder.setMultiChoiceItems(displayTimeSlots, checkedItems,
                (dialog, which, isChecked) -> checkedItems[which] = isChecked);

        builder.setPositiveButton("Add Selected", (dialog, which) -> {
            for (int i = 0; i < checkedItems.length; i++) {
                if (checkedItems[i]) {
                    addTimeSlotIfNotExists(defaultTimeSlots[i]);
                }
            }
        });

        builder.setNeutralButton("Custom Time", (dialog, which) -> showCustomTimeDialog());
        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void showCustomTimeDialog() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                    addTimeSlotIfNotExists(time);
                }, hour, minute, true);

        timePickerDialog.show();
    }

    private void addTimeSlotIfNotExists(String time) {
        // Check if time slot already exists
        for (TimeSlot slot : timeSlots) {
            if (slot.getTime().equals(time)) {
                Toast.makeText(this, "Time slot already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        timeSlots.add(new TimeSlot(time, true)); // Available by default
        timeSlotAdapter.notifyDataSetChanged();
    }

    private void loadExistingSchedule() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("barber_schedules")
                .document(currentUser.getUid())
                .collection("dates")
                .document(selectedDate)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            List<Map<String, Object>> savedSlots = (List<Map<String, Object>>) document.get("timeSlots");

                            timeSlots.clear();
                            if (savedSlots != null) {
                                for (Map<String, Object> slotMap : savedSlots) {
                                    String time = (String) slotMap.get("time");
                                    Boolean available = (Boolean) slotMap.get("available");
                                    timeSlots.add(new TimeSlot(time, available != null ? available : true));
                                }
                            }
                            timeSlotAdapter.notifyDataSetChanged();
                        } else {
                            timeSlots.clear();
                            timeSlotAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void saveSchedule() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (timeSlots.isEmpty()) {
            Toast.makeText(this, "Please add at least one time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert time slots to map for Firestore
        List<Map<String, Object>> slotsData = new ArrayList<>();
        for (TimeSlot slot : timeSlots) {
            Map<String, Object> slotMap = new HashMap<>();
            slotMap.put("time", slot.getTime());
            slotMap.put("available", slot.isAvailable());
            slotsData.add(slotMap);
        }

        Map<String, Object> scheduleData = new HashMap<>();
        scheduleData.put("date", selectedDate);
        scheduleData.put("timeSlots", slotsData);
        scheduleData.put("barberId", currentUser.getUid());
        scheduleData.put("lastUpdated", System.currentTimeMillis());

        saveScheduleButton.setEnabled(false);
        saveScheduleButton.setText("Saving...");

        db.collection("barber_schedules")
                .document(currentUser.getUid())
                .collection("dates")
                .document(selectedDate)
                .set(scheduleData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BarberCalendarActivity.this, "Schedule saved successfully!", Toast.LENGTH_SHORT).show();
                    saveScheduleButton.setEnabled(true);
                    saveScheduleButton.setText("Save Schedule");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BarberCalendarActivity.this, "Error saving schedule: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    saveScheduleButton.setEnabled(true);
                    saveScheduleButton.setText("Save Schedule");
                });
    }

    private String formatDateForDisplay(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
            Date dateObj = inputFormat.parse(date);
            return outputFormat.format(dateObj);
        } catch (Exception e) {
            return date;
        }
    }

    private String formatTimeForDisplay(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date timeObj = inputFormat.parse(time);
            return outputFormat.format(timeObj);
        } catch (Exception e) {
            return time;
        }
    }

    // TimeSlot model class
    public static class TimeSlot {
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
}