package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomiesplit.R;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Date;

public class CalendarFragment extends Fragment {

    private RecyclerView calendarGrid;
    private CalendarAdapter calendarAdapter;
    private LinearLayout weekViewContainer;
    private WeekBarChartView weekBarChart;
    private TextView dateHeader;
    private RecyclerView recyclerView;
    private TransactionsAdapter adapter;

    private Calendar currentDate = Calendar.getInstance();
    private List<JsonObject> allTransactions = new ArrayList<>();
    private Map<Long, String> userMap = new HashMap<>();

    // Parsers
    java.text.SimpleDateFormat parser1 = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    java.text.SimpleDateFormat parser2 = new java.text.SimpleDateFormat("yyyy-MM-dd");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        // Initialize Views
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggle_view_mode);
        calendarGrid = view.findViewById(R.id.calendar_grid);
        weekViewContainer = view.findViewById(R.id.week_view_container);
        weekBarChart = view.findViewById(R.id.week_bar_chart);
        dateHeader = view.findViewById(R.id.text_date_header);
        recyclerView = view.findViewById(R.id.recycler_daily_transactions);

        // Setup Month Grid
        calendarGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter();
        calendarAdapter.setListener(day -> {
            if (day.dayOfMonth != -1) {
                currentDate.set(Calendar.DAY_OF_MONTH, day.dayOfMonth);
                updateDataForCurrentView();
            }
        });
        calendarGrid.setAdapter(calendarAdapter);

        // Setup Toggle Logic
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_month_view) {
                    showMonthView();
                } else if (checkedId == R.id.btn_week_view) {
                    showWeekView();
                }
                updateDataForCurrentView();
            }
        });

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionsAdapter();
        recyclerView.setAdapter(adapter);

        // Init default view
        showMonthView();

        // Fetch Data
        fetchMemberData();
        fetchTransactions();

        return view;
    }

    private void showMonthView() {
        calendarGrid.setVisibility(View.VISIBLE);
        weekViewContainer.setVisibility(View.GONE);
    }

    private void showWeekView() {
        calendarGrid.setVisibility(View.GONE);
        weekViewContainer.setVisibility(View.VISIBLE);
    }

    private void fetchMemberData() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();
        Long userId = session.getUserId();

        if (ledgerId == null)
            return;

        com.example.roomiesplit.network.RetrofitClient.getApiService().getLedgerDetail(userId, ledgerId)
                .enqueue(new retrofit2.Callback<JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("data")) {
                                JsonObject data = body.getAsJsonObject("data");
                                if (data.has("members")) {
                                    com.google.gson.JsonArray members = data.getAsJsonArray("members");
                                    for (int i = 0; i < members.size(); i++) {
                                        JsonObject m = members.get(i).getAsJsonObject();
                                        long uId = -1;
                                        if (m.has("userId") && !m.get("userId").isJsonNull()) {
                                            uId = m.get("userId").getAsLong();
                                        }
                                        String name = "用户 " + uId;
                                        if (m.has("displayName") && !m.get("displayName").isJsonNull()) {
                                            String dn = m.get("displayName").getAsString();
                                            if (!dn.isEmpty())
                                                name = dn;
                                        }
                                        if (uId != -1)
                                            userMap.put(uId, name);
                                    }
                                    adapter.setUserMap(userMap);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                    }
                });
    }

    private void fetchTransactions() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null) {
            allTransactions.clear();
            updateDataForCurrentView();
            return;
        }

        com.example.roomiesplit.network.RetrofitClient.getApiService().getTransactions(ledgerId)
                .enqueue(new retrofit2.Callback<JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonArray data = response.body().getAsJsonArray("data");
                            if (data != null) {
                                allTransactions.clear();
                                for (int i = 0; i < data.size(); i++) {
                                    allTransactions.add(data.get(i).getAsJsonObject());
                                }
                                updateDataForCurrentView();
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateDataForCurrentView() {
        boolean isMonthView = calendarGrid.getVisibility() == View.VISIBLE;
        int currentYear = currentDate.get(Calendar.YEAR);
        int currentMonth = currentDate.get(Calendar.MONTH);
        int currentDay = currentDate.get(Calendar.DAY_OF_MONTH);

        List<JsonObject> filtered = new ArrayList<>();

        // Group data by day for entire loaded history (to populate calendar)
        Map<String, Double> dailyTotals = new HashMap<>(); // "yyyy-MM-dd" -> Amount
        for (JsonObject t : allTransactions) {
            long ts = getTimestamp(t);
            if (ts > 0) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(ts);
                String key = String.format("%04d-%02d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH));
                double amount = t.has("amount") ? t.get("amount").getAsDouble() : 0;
                dailyTotals.put(key, dailyTotals.getOrDefault(key, 0.0) + amount);
            }
        }

        if (isMonthView) {
            // 1. Prepare Calendar Data
            List<CalendarAdapter.CalendarDay> days = new ArrayList<>();
            Calendar c = (Calendar) currentDate.clone();
            c.set(Calendar.DAY_OF_MONTH, 1);
            int daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK); // 1 = Sunday

            // Empty padding slots
            for (int i = 1; i < dayOfWeek; i++) {
                days.add(new CalendarAdapter.CalendarDay(currentYear, currentMonth, -1));
            }

            // Days
            for (int i = 1; i <= daysInMonth; i++) {
                CalendarAdapter.CalendarDay dayObj = new CalendarAdapter.CalendarDay(currentYear, currentMonth, i);
                String key = String.format("%04d-%02d-%02d", currentYear, currentMonth, i);
                if (dailyTotals.containsKey(key)) {
                    dayObj.totalAmount = dailyTotals.get(key);
                }
                days.add(dayObj);
            }
            calendarAdapter.setDays(days);

            // 2. Prepare Details List for SELECTED Day
            String headerText = (currentMonth + 1) + "月" + currentDay + "日 账单明细";
            dateHeader.setText(headerText);

            for (JsonObject t : allTransactions) {
                long timestamp = getTimestamp(t);
                if (isSameDay(timestamp, currentYear, currentMonth, currentDay)) {
                    filtered.add(t);
                }
            }
        } else {
            // Week View Logic
            Calendar cal = (Calendar) currentDate.clone();
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
            long startOfWeek = cal.getTimeInMillis();

            String startStr = (cal.get(Calendar.MONTH) + 1) + "月" + cal.get(Calendar.DAY_OF_MONTH) + "日";

            // Collect Weekly data for Chart
            List<Double> chartData = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            String[] weekDays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

            for (int i = 0; i < 7; i++) {
                String key = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH));
                chartData.add(dailyTotals.getOrDefault(key, 0.0));
                labels.add(weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]);
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            long endOfWeek = cal.getTimeInMillis();
            String endStr = (cal.get(Calendar.MONTH) + 1) + "月" + cal.get(Calendar.DAY_OF_MONTH) + "日";

            dateHeader.setText(startStr + " - " + endStr + " 本周概览");

            weekBarChart.setData(chartData, labels);

            // Filter list for the week
            for (JsonObject t : allTransactions) {
                long timestamp = getTimestamp(t);
                if (timestamp >= startOfWeek && timestamp < endOfWeek) {
                    filtered.add(t);
                }
            }
        }

        adapter.setTransactions(filtered);
    }

    private long getTimestamp(JsonObject t) {
        String dateStr = t.has("transactionDate") && !t.get("transactionDate").isJsonNull()
                ? t.get("transactionDate").getAsString()
                : (t.has("date") && !t.get("date").isJsonNull() ? t.get("date").getAsString() : null);

        if (dateStr != null) {
            try {
                String cleanDate = dateStr.replace("T", " ");
                if (cleanDate.length() > 10) {
                    return parser1.parse(cleanDate).getTime();
                } else {
                    return parser2.parse(cleanDate).getTime();
                }
            } catch (Exception e) {
            }
        }
        return 0;
    }

    private boolean isSameDay(long timestamp, int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
                && c.get(Calendar.DAY_OF_MONTH) == day;
    }
}
