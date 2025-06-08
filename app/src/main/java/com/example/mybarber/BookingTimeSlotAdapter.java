package com.example.mybarber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mybarber.model.TimeSlot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BookingTimeSlotAdapter extends RecyclerView.Adapter<BookingTimeSlotAdapter.TimeSlotViewHolder> {

    private List<TimeSlot> timeSlots;
    private OnTimeSlotClickListener listener;
    private String selectedTime;

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(TimeSlot timeSlot);
    }

    public BookingTimeSlotAdapter(List<TimeSlot> timeSlots, OnTimeSlotClickListener listener) {
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    public void setSelectedTime(String selectedTime) {
        this.selectedTime = selectedTime;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        TimeSlot timeSlot = timeSlots.get(position);

        // Format time for display
        String displayTime = formatTimeForDisplay(timeSlot.getTime());
        holder.timeText.setText(displayTime);

        // Set appearance based on availability and selection
        if (!timeSlot.isAvailable()) {
            // Unavailable slot
            holder.timeText.setBackgroundResource(R.drawable.time_slot_unavailable);
            holder.timeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red));
            holder.itemView.setClickable(false);
        } else if (timeSlot.getTime().equals(selectedTime)) {
            // Selected slot
            holder.timeText.setBackgroundResource(R.drawable.time_slot_selected);
            holder.timeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.itemView.setClickable(true);
        } else {
            // Available slot
            holder.timeText.setBackgroundResource(R.drawable.time_slot_available);
            holder.timeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green));
            holder.itemView.setClickable(true);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSlotClick(timeSlot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
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

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;

        TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}