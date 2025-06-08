package com.example.mybarber;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "BOOKING_REMINDERS";

    @Override
    public void onReceive(Context context, Intent intent) {
        String bookingId = intent.getStringExtra("bookingId");
        String barberName = intent.getStringExtra("barberName");
        String date = intent.getStringExtra("date");
        String time = intent.getStringExtra("time");

        if (bookingId != null && barberName != null && date != null && time != null) {
            showNotification(context, bookingId, barberName, date, time);
        }
    }

    private void showNotification(Context context, String bookingId, String barberName, String date, String time) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Format time for display
        String displayTime = formatTimeForDisplay(time);
        String displayDate = formatDateForDisplay(date);

        // Create intent to open app when notification is tapped
        Intent appIntent = new Intent(context, BarberProfileActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                bookingId.hashCode(),
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Upcoming Appointment Reminder")
                .setContentText("Your appointment with " + barberName + " is in 1 hour")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your appointment with " + barberName + " is scheduled for " +
                                displayTime + " on " + displayDate + ". Don't forget!"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 1000})
                .setDefaults(NotificationCompat.DEFAULT_SOUND);

        // Show notification
        if (notificationManager != null) {
            notificationManager.notify(bookingId.hashCode(), builder.build());
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

    private String formatDateForDisplay(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault());
            Date dateObj = inputFormat.parse(date);
            return outputFormat.format(dateObj);
        } catch (Exception e) {
            return date;
        }
    }
}