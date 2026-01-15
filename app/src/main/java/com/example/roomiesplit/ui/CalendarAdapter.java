package com.example.roomiesplit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomiesplit.R;
import java.util.ArrayList;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> days = new ArrayList<>();
    private OnDateClickListener listener;

    public interface OnDateClickListener {
        void onDateClick(CalendarDay day);
    }

    public void setListener(OnDateClickListener listener) {
        this.listener = listener;
    }

    public void setDays(List<CalendarDay> days) {
        this.days = days;
        notifyDataSetChanged();
    }

    private int selectedYear, selectedMonth, selectedDay;

    public void setSelectedDate(int y, int m, int d) {
        this.selectedYear = y;
        this.selectedMonth = m;
        this.selectedDay = d;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        int height = parent.getHeight() / 6;
        if (height < 100)
            height = 150;
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = days.get(position);
        if (day.dayOfMonth == -1) {
            holder.dayText.setText("");
            holder.amountText.setVisibility(View.INVISIBLE);
            holder.dotView.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.dayText.setBackground(null);
            holder.dayText.setElevation(0f);
            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        } else {
            holder.dayText.setText(String.valueOf(day.dayOfMonth));

            boolean isSelected = (day.year == selectedYear && day.month == selectedMonth
                    && day.dayOfMonth == selectedDay);

            // Heatmap Background Logic
            int color;
            double value = day.totalAmount / 10.0;

            // Re-using logic...
            if (value == 0)
                color = 0x00000000; // Transparent for 0
            else if (value < 1)
                color = 0xFFD1E9FC;
            else if (value < 2)
                color = 0xFFC8E6C9;
            else if (value < 3)
                color = 0xFFA5D6A7;
            else if (value < 5)
                color = 0xFFFFF59D;
            else if (value < 6)
                color = 0xFFFFE082;
            else if (value < 8)
                color = 0xFFFFB74D;
            else if (value < 10)
                color = 0xFFFF8A65;
            else if (value < 12)
                color = 0xFFFF7043;
            else if (value < 14)
                color = 0xFFEF5350;
            else
                color = 0xFFE95F5F;

            // If selected, ensure it has a visible background even if 0 amount
            if (isSelected && color == 0x00000000) {
                color = 0xFFE0E0E0; // Light Gray for selected empty day
            }

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(color);

            if (isSelected) {
                // Add border for selection
                drawable.setStroke(4, 0xFF424242); // Dark Grey Border
                holder.dayText.setElevation(12f); // Shadow !
                // Elevation requires a background to cast shadow. Logic holds.
            } else {
                holder.dayText.setElevation(0f);
            }

            if (day.totalAmount > 0 || isSelected) {
                holder.dayText.setBackground(drawable);
            } else {
                holder.dayText.setBackground(null);
                holder.dayText.setElevation(0f); // Double check
            }

            holder.itemView.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            if (day.totalAmount > 0) {
                holder.amountText.setText(String.format("¥%.0f", day.totalAmount));
                holder.amountText.setVisibility(View.VISIBLE);
                holder.dotView.setVisibility(View.GONE);
            } else {
                holder.amountText.setVisibility(View.INVISIBLE);
                holder.dotView.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onDateClick(day);
                // Also update internal selection immediately for responsiveness
                setSelectedDate(day.year, day.month, day.dayOfMonth);
            });
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dayText, amountText;
        View dotView;

        CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.text_day);
            amountText = itemView.findViewById(R.id.text_amount);
            dotView = itemView.findViewById(R.id.view_dot);
        }
    }

    public static class CalendarDay {
        public int year;
        public int month; // 0-11
        public int dayOfMonth; // -1 for padding
        public double totalAmount;

        public CalendarDay(int y, int m, int d) {
            this.year = y;
            this.month = m;
            this.dayOfMonth = d;
        }
    }
}
