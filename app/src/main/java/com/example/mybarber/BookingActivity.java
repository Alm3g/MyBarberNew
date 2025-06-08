package com.example.mybarber;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mybarber.model.TimeSlot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "BOOKING_REMINDERS";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private String barberId;
    private String selectedDate;
    private String selectedTimeSlot;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // UI Components
    private ImageView barberImageView;
    private TextView barberNameText, selectedDateText, selectedTimeText;
    private CalendarView calendarView;
    private RecyclerView timeSlotRecyclerView;
    private EditText notesEditText;
    private Button confirmBookingButton, cancelBookingButton;

    private List<TimeSlot> availableTimeSlots;
    private BookingTimeSlotAdapter timeSlotAdapter;
    private String existingBookingId = null; // To track if user already has booking

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Get barber ID from intent
        barberId = getIntent().getStringExtra("BARBER_ID");
        if (barberId == null) {
            Toast.makeText(this, "Error: Barber not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeFirebase();
        initializeViews();
        createNotificationChannel();
        requestNotificationPermission();
        setupCalendar();
        setupTimeSlots();
        loadBarberInfo();
        setupButtons();
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        barberImageView = findViewById(R.id.barberImageView);
        barberNameText = findViewById(R.id.barberNameText);
        selectedDateText = findViewById(R.id.selectedDateText);
        selectedTimeText = findViewById(R.id.selectedTimeText);
        calendarView = findViewById(R.id.calendarView);
        timeSlotRecyclerView = findViewById(R.id.timeSlotRecyclerView);
        notesEditText = findViewById(R.id.notesEditText);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        cancelBookingButton = findViewById(R.id.cancelBookingButton);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Booking Reminders";
            String description = "Notifications for upcoming barber appointments";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You won't receive booking reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupCalendar() {
        // Set minimum date to today (allow booking for today if time is in future)
        calendarView.setMinDate(System.currentTimeMillis());

        // Set maximum date to 30 days from now
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_MONTH, 30);
        calendarView.setMaxDate(maxDate.getTimeInMillis());

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
                selectedTimeSlot = null;
                selectedTimeText.setText("Selected Time: Not selected");

                // Check if customer already has booking with this barber on this date
                checkExistingBookingForDate();
            }
        });

        // Check existing booking for initial date (today)
        checkExistingBookingForDate();
    }

    private void setupTimeSlots() {
        availableTimeSlots = new ArrayList<>();
        timeSlotAdapter = new BookingTimeSlotAdapter(availableTimeSlots, new BookingTimeSlotAdapter.OnTimeSlotClickListener() {
            @Override
            public void onTimeSlotClick(TimeSlot timeSlot) {
                if (timeSlot.isAvailable()) {
                    selectedTimeSlot = timeSlot.getTime();
                    selectedTimeText.setText("Selected Time: " + formatTimeForDisplay(selectedTimeSlot));
                    timeSlotAdapter.setSelectedTime(selectedTimeSlot);
                    updateConfirmButtonState();
                } else {
                    Toast.makeText(BookingActivity.this, "This time slot is not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        timeSlotRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        timeSlotRecyclerView.setAdapter(timeSlotAdapter);
    }

    private void setupButtons() {
        confirmBookingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmBooking();
            }
        });

        cancelBookingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelExistingBooking();
            }
        });
    }

    private void loadBarberInfo() {
        db.collection("users").document(barberId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("displayName");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        String profileImageBase64 = documentSnapshot.getString("profileImage");

                        if (name != null) {
                            barberNameText.setText(name);
                        }

                        // Load profile image
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .into(barberImageView);
                        } else if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            // Handle base64 image
                            try {
                                byte[] decodedString = Base64.decode(profileImageBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                barberImageView.setImageBitmap(decodedByte);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading barber info", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkExistingBookingForDate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Check if customer already has a booking with this barber on this date
        db.collection("bookings")
                .whereEqualTo("customerId", currentUser.getUid())
                .whereEqualTo("barberId", barberId)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Customer already has booking on this date
                        existingBookingId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        String existingTime = queryDocumentSnapshots.getDocuments().get(0).getString("time");
                        String existingNotes = queryDocumentSnapshots.getDocuments().get(0).getString("notes");

                        showExistingBookingUI(existingTime, existingNotes);
                    } else {
                        existingBookingId = null;
                        showNewBookingUI();
                        loadAvailableTimeSlots();
                    }
                })
                .addOnFailureListener(e -> {
                    existingBookingId = null;
                    showNewBookingUI();
                    loadAvailableTimeSlots();
                });
    }

    private void showExistingBookingUI(String existingTime, String existingNotes) {
        selectedTimeSlot = existingTime;
        selectedTimeText.setText("Current Booking: " + formatTimeForDisplay(existingTime));

        if (existingNotes != null && !existingNotes.isEmpty()) {
            notesEditText.setText(existingNotes);
        }

        // Hide time slots and show cancel option
        timeSlotRecyclerView.setVisibility(View.GONE);
        confirmBookingButton.setVisibility(View.GONE);
        cancelBookingButton.setVisibility(View.VISIBLE);

        Toast.makeText(this, "You already have a booking on this date", Toast.LENGTH_LONG).show();
    }

    private void showNewBookingUI() {
        selectedTimeSlot = null;
        selectedTimeText.setText("Selected Time: Not selected");
        notesEditText.setText("");

        timeSlotRecyclerView.setVisibility(View.VISIBLE);
        confirmBookingButton.setVisibility(View.VISIBLE);
        cancelBookingButton.setVisibility(View.GONE);

        updateConfirmButtonState();
    }

    private void cancelExistingBooking() {
        if (existingBookingId == null) return;

        cancelBookingButton.setEnabled(false);
        cancelBookingButton.setText("Canceling...");

        // Cancel the notification for this booking
        cancelNotificationForBooking(existingBookingId);

        db.collection("bookings")
                .document(existingBookingId)
                .update("status", "cancelled")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show();
                    existingBookingId = null;
                    showNewBookingUI();
                    loadAvailableTimeSlots();

                    cancelBookingButton.setEnabled(true);
                    cancelBookingButton.setText("Cancel Booking");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cancelling booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    cancelBookingButton.setEnabled(true);
                    cancelBookingButton.setText("Cancel Booking");
                });
    }

    private void loadAvailableTimeSlots() {
        db.collection("barber_schedules")
                .document(barberId)
                .collection("dates")
                .document(selectedDate)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        availableTimeSlots.clear();

                        if (task.isSuccessful() && task.getResult().exists()) {
                            DocumentSnapshot document = task.getResult();
                            List<Map<String, Object>> savedSlots = (List<Map<String, Object>>) document.get("timeSlots");

                            if (savedSlots != null) {
                                for (Map<String, Object> slotMap : savedSlots) {
                                    String time = (String) slotMap.get("time");
                                    Boolean available = (Boolean) slotMap.get("available");

                                    if (available != null && available) {
                                        // Check if time is in the future (for today) or any time (for future dates)
                                        if (isTimeSlotValid(time)) {
                                            // Check if this time slot is already booked
                                            checkIfTimeSlotIsBooked(time);
                                        }
                                    }
                                }
                            }
                        } else {
                            // No schedule available for this date
                            Toast.makeText(BookingActivity.this, "No available time slots for this date", Toast.LENGTH_SHORT).show();
                            timeSlotAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private boolean isTimeSlotValid(String timeSlot) {
        try {
            // Get current date and time
            Calendar now = Calendar.getInstance();
            String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now.getTime());

            // If selected date is not today, all time slots are valid
            if (!selectedDate.equals(todayDate)) {
                return true;
            }

            // If selected date is today, check if time slot is in the future
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date slotTime = timeFormat.parse(timeSlot);
            Date currentTime = timeFormat.parse(
                    String.format(Locale.getDefault(), "%02d:%02d",
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE))
            );

            // Add 1 hour buffer - can't book within next hour
            Calendar bufferTime = Calendar.getInstance();
            bufferTime.add(Calendar.HOUR_OF_DAY, 1);
            Date minimumBookingTime = timeFormat.parse(
                    String.format(Locale.getDefault(), "%02d:%02d",
                            bufferTime.get(Calendar.HOUR_OF_DAY),
                            bufferTime.get(Calendar.MINUTE))
            );

            return slotTime.after(minimumBookingTime);

        } catch (Exception e) {
            e.printStackTrace();
            // If there's an error parsing, assume it's valid for future dates
            return !isToday(selectedDate);
        }
    }

    private boolean isToday(String date) {
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return date.equals(todayDate);
    }

    private void checkIfTimeSlotIsBooked(String time) {
        db.collection("bookings")
                .whereEqualTo("barberId", barberId)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("time", time)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isBooked = !queryDocumentSnapshots.isEmpty();
                    availableTimeSlots.add(new TimeSlot(time, !isBooked));
                    timeSlotAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // If there's an error checking bookings, assume slot is available
                    availableTimeSlots.add(new TimeSlot(time, true));
                    timeSlotAdapter.notifyDataSetChanged();
                });
    }

    private void confirmBooking() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to book an appointment", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDate == null || selectedTimeSlot == null) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Double check that customer doesn't already have booking on this date
        if (existingBookingId != null) {
            Toast.makeText(this, "You already have a booking on this date", Toast.LENGTH_SHORT).show();
            return;
        }

        confirmBookingButton.setEnabled(false);
        confirmBookingButton.setText("Booking...");

        // Final check before creating booking
        db.collection("bookings")
                .whereEqualTo("customerId", currentUser.getUid())
                .whereEqualTo("barberId", barberId)
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("status", "confirmed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "You already have a booking on this date", Toast.LENGTH_SHORT).show();
                        confirmBookingButton.setEnabled(true);
                        confirmBookingButton.setText("Confirm Booking");
                        checkExistingBookingForDate(); // Refresh UI
                        return;
                    }

                    // Proceed with booking creation
                    createNewBooking(currentUser);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking existing bookings", Toast.LENGTH_SHORT).show();
                    confirmBookingButton.setEnabled(true);
                    confirmBookingButton.setText("Confirm Booking");
                });
    }

    private void createNewBooking(FirebaseUser currentUser) {
        // Create booking data
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("customerId", currentUser.getUid());
        bookingData.put("barberId", barberId);
        bookingData.put("date", selectedDate);
        bookingData.put("time", selectedTimeSlot);
        bookingData.put("notes", notesEditText.getText().toString().trim());
        bookingData.put("status", "confirmed");
        bookingData.put("createdAt", System.currentTimeMillis());

        // Add customer and barber info for easy access
        bookingData.put("customerName", currentUser.getDisplayName());
        bookingData.put("customerEmail", currentUser.getEmail());

        // Add booking to Firestore
        db.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener(documentReference -> {
                    String bookingId = documentReference.getId();

                    // Schedule notification for 1 hour before appointment
                    scheduleBookingNotification(bookingId, selectedDate, selectedTimeSlot,
                            barberNameText.getText().toString());

                    Toast.makeText(BookingActivity.this,
                            "Booking confirmed successfully! You'll receive a reminder 1 hour before your appointment.",
                            Toast.LENGTH_LONG).show();

                    // Return to previous screen
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookingActivity.this, "Error confirming booking: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    confirmBookingButton.setEnabled(true);
                    confirmBookingButton.setText("Confirm Booking");
                });
    }

    private void scheduleBookingNotification(String bookingId, String date, String time, String barberName) {
        try {
            // Parse booking date and time
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date bookingDateTime = dateTimeFormat.parse(date + " " + time);

            if (bookingDateTime == null) return;

            // Calculate notification time (1 hour before booking)
            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(bookingDateTime);
            notificationTime.add(Calendar.HOUR_OF_DAY, -1);

            // Only schedule if notification time is in the future
            if (notificationTime.getTimeInMillis() > System.currentTimeMillis()) {
                Intent notificationIntent = new Intent(this, BookingReminderReceiver.class);
                notificationIntent.putExtra("bookingId", bookingId);
                notificationIntent.putExtra("barberName", barberName);
                notificationIntent.putExtra("date", date);
                notificationIntent.putExtra("time", time);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        bookingId.hashCode(), // Use booking ID hash as unique request code
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationTime.getTimeInMillis(),
                            pendingIntent
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't fail the booking if notification scheduling fails
        }
    }

    private void cancelNotificationForBooking(String bookingId) {
        try {
            Intent notificationIntent = new Intent(this, BookingReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    bookingId.hashCode(),
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateConfirmButtonState() {
        boolean canBook = selectedDate != null && selectedTimeSlot != null;
        confirmBookingButton.setEnabled(canBook);

        if (canBook) {
            confirmBookingButton.setBackgroundTintList(getResources().getColorStateList(R.color.white));
        } else {
            confirmBookingButton.setBackgroundTintList(getResources().getColorStateList(R.color.grey));
        }
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


}