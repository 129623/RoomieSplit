package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.roomiesplit.R;
import java.util.ArrayList;
import java.util.List;

public class KarmaFragment extends Fragment {

    private KarmaRouletteView rouletteView;
    private android.widget.Spinner workCategorySpinner;
    private EditText workDescInput;
    private EditText taskNameInput;
    private TextView myStatusText;
    private TextView goldStatusText;

    // Defined Tasks
    private static class KarmaTask {
        String name;
        int points;

        KarmaTask(String name, int points) {
            this.name = name;
            this.points = points;
        }

        @Override
        public String toString() {
            return name + " (+" + points + " 人品)";
        }
    }

    private List<KarmaTask> availableTasks = new ArrayList<>();

    // Mock Data Models
    private static class Member {
        String id;
        String name;
        float baseWeight = 100f;
        boolean isGold;
        int karmaPoints = 0; // Accumulate points

        Member(String id, String name, boolean isGold) {
            this.id = id;
            this.name = name;
            this.isGold = isGold;
        }

        float getFinalWeight() {
            float weight = baseWeight;
            if (isGold)
                weight *= 0.7f; // -30% for Gold

            weight -= karmaPoints; // Linear deduction

            return Math.max(weight, 10f); // Minimum weight 10
        }
    }

    private List<Member> members = new ArrayList<>();
    private String currentUserId = "user_001"; // Me

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_karma, container, false);

        rouletteView = view.findViewById(R.id.karma_roulette_view);
        workCategorySpinner = view.findViewById(R.id.spinner_work_category);
        workDescInput = view.findViewById(R.id.input_work_description);
        taskNameInput = view.findViewById(R.id.input_task_name);
        myStatusText = view.findViewById(R.id.text_my_status);
        goldStatusText = view.findViewById(R.id.text_gold_status);

        view.findViewById(R.id.btn_back_karma)
                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.btn_submit_work).setOnClickListener(v -> submitGoodKarma());
        view.findViewById(R.id.btn_spin_roulette).setOnClickListener(v -> spinRoulette());

        initTasks();
        initSpinner();
        initMockData();
        updateRoulette();

        return view;
    }

    private void initTasks() {
        availableTasks.clear();
        availableTasks.add(new KarmaTask("倒垃圾", 5));
        availableTasks.add(new KarmaTask("买东西/取快递", 10));
        availableTasks.add(new KarmaTask("带饭", 15));
        availableTasks.add(new KarmaTask("刷厕所", 20));
    }

    private void initSpinner() {
        android.widget.ArrayAdapter<KarmaTask> adapter = new android.widget.ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_dropdown_item, availableTasks);
        workCategorySpinner.setAdapter(adapter);
    }

    private void initMockData() {
        // Fetch from API instead of mock
        fetchKarmaStats();
    }

    private void fetchKarmaStats() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();
        currentUserId = String.valueOf(session.getUserId());

        if (ledgerId == null)
            return;

        com.example.roomiesplit.network.RetrofitClient.getApiService().getKarmaStats(ledgerId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject body = response.body();
                            if (body.get("code").getAsInt() == 200) {
                                members.clear();
                                com.google.gson.JsonArray data = body.getAsJsonArray("data");
                                for (com.google.gson.JsonElement el : data) {
                                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                                    String id = String.valueOf(obj.get("userId").getAsLong());
                                    String name = obj.get("displayName").getAsString();
                                    // boolean isGold = ... (not in simple API yet, ignore for now)
                                    Member m = new Member(id, name, false);
                                    m.karmaPoints = obj.get("accumulatedPoints").getAsInt();
                                    // Override weight calculation with server provided if available, or just set
                                    // points
                                    // For now, let's trust server weight if we want, or keep local calc?
                                    // let's use local calc for UI consistency or use server 'finalWeight'
                                    members.add(m);
                                }
                                updateRoulette();
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "获取人品数据失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateRoulette() {
        List<KarmaRouletteView.RouletteEntry> entries = new ArrayList<>();
        float totalWeight = 0;

        for (Member m : members) {
            float w = m.getFinalWeight();
            entries.add(new KarmaRouletteView.RouletteEntry(m.name, w));
            totalWeight += w;
        }

        rouletteView.setEntries(entries);

        // Update My Status
        for (Member m : members) {
            if (m.id.equals(currentUserId)) {
                float myWeight = m.getFinalWeight();
                float prob = (totalWeight > 0) ? (myWeight / totalWeight) * 100f : 0;

                myStatusText.setText(String.format("我当前概率: %.1f%% (人品: %d)", prob, m.karmaPoints));

                if (m.isGold) {
                    goldStatusText.setVisibility(View.VISIBLE);
                    goldStatusText.setText("👑 金主特权 (-30%)");
                } else if (m.karmaPoints > 0) {
                    goldStatusText.setVisibility(View.VISIBLE);
                    goldStatusText.setText("😇 人品积累中 (-" + m.karmaPoints + ")");
                    goldStatusText.setBackgroundColor(0x334CAF50);
                    goldStatusText.setTextColor(0xFF4CAF50);
                } else {
                    goldStatusText.setVisibility(View.GONE);
                }
                break;
            }
        }
    }

    private void submitGoodKarma() {
        KarmaTask selectedTask = (KarmaTask) workCategorySpinner.getSelectedItem();
        if (selectedTask == null)
            return;

        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long userId = session.getUserId();
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null)
            return;

        com.google.gson.JsonObject request = new com.google.gson.JsonObject();
        request.addProperty("category", selectedTask.name);
        request.addProperty("points", selectedTask.points);
        request.addProperty("description", workDescInput.getText().toString());

        com.example.roomiesplit.network.RetrofitClient.getApiService().recordKarmaWork(userId, ledgerId, request)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "打卡成功！获得 +" + selectedTask.points + " 人品", Toast.LENGTH_SHORT)
                                    .show();
                            workDescInput.setText("");
                            fetchKarmaStats(); // Refresh
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "打卡失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void spinRoulette() {
        String task = taskNameInput.getText().toString().trim();
        if (task.isEmpty()) {
            Toast.makeText(getContext(), "请输入待分配任务", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long userId = session.getUserId();
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null)
            return;

        com.google.gson.JsonObject request = new com.google.gson.JsonObject();
        request.addProperty("task", task);

        com.example.roomiesplit.network.RetrofitClient.getApiService().karmaDraw(userId, ledgerId, request)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject body = response.body();
                            if (body.get("code").getAsInt() == 200) {
                                com.google.gson.JsonObject data = body.getAsJsonObject("data");
                                String winnerName = data.get("winnerName").getAsString();
                                String message = "恭喜 " + winnerName + " 喜提 " + task + " 大奖！";
                                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "抽奖失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
