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

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        // Force height to make robust grid
        int height = parent.getHeight() / 6; // Approx 6 rows
        if (height < 100)
            height = 150; // Min height
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
        } else {
            holder.dayText.setText(String.valueOf(day.dayOfMonth));
            if (day.totalAmount > 0) {
                holder.amountText.setText(String.format("¥%.0f", day.totalAmount));
                holder.amountText.setVisibility(View.VISIBLE);
                holder.dotView.setVisibility(View.VISIBLE);
                // Heatmap logic: change dot color or alpha? For now just show it.
            } else {
                holder.amountText.setVisibility(View.INVISIBLE);
                holder.dotView.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onDateClick(day);
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
