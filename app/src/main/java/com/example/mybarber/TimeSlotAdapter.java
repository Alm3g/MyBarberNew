package com.example.mybarber;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<BarberCalendarActivity.TimeSlot> timeSlots;

    public TimeSlotAdapter(List<BarberCalendarActivity.TimeSlot> timeSlots) {
        this.timeSlots = timeSlots;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        BarberCalendarActivity.TimeSlot timeSlot = timeSlots.get(position);

        // Format time for display (12-hour format)
        String displayTime = formatTimeForDisplay(timeSlot.getTime());
        holder.timeText.setText(displayTime);

        // Set availability checkbox
        holder.availableCheckBox.setChecked(timeSlot.isAvailable());
        holder.availableCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            timeSlot.setAvailable(isChecked);
        });

        // Set delete button click listener
        holder.deleteButton.setOnClickListener(v -> {
            timeSlots.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, timeSlots.size());
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

    public static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        CheckBox availableCheckBox;
        ImageButton deleteButton;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
            availableCheckBox = itemView.findViewById(R.id.availableCheckBox);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
