package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roomiesplit.R;
import com.example.roomiesplit.network.RetrofitClient;
import com.example.roomiesplit.utils.SessionManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DecisionFragment extends Fragment {

    private ViewFlipper viewFlipper;
    private RecyclerView recyclerPolls;
    private PollAdapter adapter;
    private List<JsonObject> pollList = new ArrayList<>();

    // Create UI
    private LinearLayout optionsContainer;
    private List<EditText> optionInputs = new ArrayList<>();
    private EditText editPollTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_decision, container, false);

        viewFlipper = view.findViewById(R.id.view_flipper);

        // --- List View Setup ---
        recyclerPolls = view.findViewById(R.id.recycler_polls);
        recyclerPolls.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PollAdapter();
        recyclerPolls.setAdapter(adapter);

        view.findViewById(R.id.btn_back_list)
                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.fab_create_poll).setOnClickListener(v -> showCreateMode());

        // --- Create View Setup ---
        editPollTitle = view.findViewById(R.id.edit_poll_title);
        optionsContainer = view.findViewById(R.id.container_options);

        // Init default options
        optionInputs.clear();
        optionInputs.add(view.findViewById(R.id.edit_option_1));
        optionInputs.add(view.findViewById(R.id.edit_option_2));

        view.findViewById(R.id.btn_cancel_create).setOnClickListener(v -> showListMode());
        view.findViewById(R.id.btn_add_option).setOnClickListener(v -> addOptionInput());
        view.findViewById(R.id.btn_initiate_vote).setOnClickListener(v -> initiateVote(false));
        view.findViewById(R.id.btn_random_pick).setOnClickListener(v -> initiateVote(true));

        loadPolls();

        return view;
    }

    private void showListMode() {
        viewFlipper.setDisplayedChild(0);
        loadPolls(); // Refresh
    }

    private void showCreateMode() {
        viewFlipper.setDisplayedChild(1);
        editPollTitle.setText("");
        // Reset options if needed, or keep last state
    }

    private void addOptionInput() {
        EditText newOption = new EditText(getContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (50 * getResources().getDisplayMetrics().density));
        params.setMargins(0, 0, 0, (int) (12 * getResources().getDisplayMetrics().density));
        newOption.setLayoutParams(params);
        newOption.setHint("选项 " + (optionInputs.size() + 1));
        newOption.setBackgroundColor(0xFFF8FAFC);
        newOption.setPadding((int) (16 * getResources().getDisplayMetrics().density), 0, 0, 0);

        optionsContainer.addView(newOption);
        optionInputs.add(newOption);
    }

    private void loadPolls() {
        SessionManager session = new SessionManager(getContext());
        Long ledgerId = session.getCurrentLedgerId();
        if (ledgerId == null)
            return;

        RetrofitClient.getApiService().getPolls(ledgerId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.get("code").getAsInt() == 200) {
                        JsonArray data = body.getAsJsonArray("data");
                        pollList.clear();
                        for (int i = 0; i < data.size(); i++) {
                            pollList.add(data.get(i).getAsJsonObject());
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initiateVote(boolean isRandomMode) {
        String title = editPollTitle.getText().toString().trim();
        if (title.isEmpty())
            title = "今天吃什么？";

        List<String> options = new ArrayList<>();
        for (EditText input : optionInputs) {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) {
                options.add(text);
            }
        }

        if (options.size() < 2) {
            Toast.makeText(getContext(), "请至少输入两个选项", Toast.LENGTH_SHORT).show();
            return;
        }

        SessionManager session = new SessionManager(getContext());
        Long userId = session.getUserId();
        Long ledgerId = session.getCurrentLedgerId();

        JsonObject request = new JsonObject();
        request.addProperty("title", title);
        request.addProperty("mode", isRandomMode ? "RANDOM" : "SINGLE");

        JsonArray optionsArray = new JsonArray();
        for (String opt : options) {
            optionsArray.add(opt);
        }
        request.add("options", optionsArray);

        RetrofitClient.getApiService().createPoll(userId, ledgerId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.get("code").getAsInt() == 200) {
                        Toast.makeText(getContext(), isRandomMode ? "随机决策生成中..." : "投票已发起", Toast.LENGTH_SHORT).show();
                        showListMode();
                    } else {
                        Toast.makeText(getContext(), "失败: " + body.get("message").getAsString(), Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- Adapter ---
    class PollAdapter extends RecyclerView.Adapter<PollAdapter.PollViewHolder> {

        @NonNull
        @Override
        public PollViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poll, parent, false);
            return new PollViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PollViewHolder holder, int position) {
            JsonObject poll = pollList.get(position);

            String title = poll.has("title") && !poll.get("title").isJsonNull() ? poll.get("title").getAsString()
                    : "无标题";
            holder.title.setText(title);

            String status = poll.has("status") && !poll.get("status").isJsonNull() ? poll.get("status").getAsString()
                    : "ACTIVE";
            holder.status.setText("ACTIVE".equals(status) ? "进行中" : "已结束");
            holder.status.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    "ACTIVE".equals(status) ? 0xFF4CAF50 : 0xFF9E9E9E));

            String dateStr = poll.has("createdAt") && !poll.get("createdAt").isJsonNull()
                    ? poll.get("createdAt").getAsString()
                    : "";
            try {
                if (dateStr.length() > 16)
                    holder.date.setText(dateStr.replace("T", " ").substring(0, 16));
                else
                    holder.date.setText(dateStr);
            } catch (Exception e) {
                holder.date.setText(dateStr);
            }

            // Click to View/Vote
            long pollId = poll.has("id") ? poll.get("id").getAsLong() : -1;
            holder.itemView.setOnClickListener(v -> {
                if (pollId != -1)
                    showVoteDialog(pollId);
            });

            holder.result.setText("点击查看详情 >");
        }

        @Override
        public int getItemCount() {
            return pollList.size();
        }

        class PollViewHolder extends RecyclerView.ViewHolder {
            TextView title, status, date, result;

            PollViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_poll_title);
                status = itemView.findViewById(R.id.chip_status);
                date = itemView.findViewById(R.id.text_poll_date);
                result = itemView.findViewById(R.id.text_poll_winner);
            }
        }
    }

    private void showVoteDialog(Long pollId) {
        RetrofitClient.getApiService().getPollResult(pollId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded() || getContext() == null)
                    return;

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body(); // Ensure we get body first
                    if (body.has("data") && !body.get("data").isJsonNull()) {
                        JsonObject data = body.getAsJsonObject("data");
                        openDialog(data);
                    } else {
                        Toast.makeText(getContext(), "数据为空", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDialog(JsonObject data) {
        if (getContext() == null)
            return;

        JsonObject poll = data.getAsJsonObject("poll");
        JsonArray details = data.getAsJsonArray("details");
        String winner = data.has("winner") && !data.get("winner").isJsonNull() ? data.get("winner").getAsString()
                : "Pending";

        Long goldPayerId = -1L;
        if (data.has("goldPayerId") && !data.get("goldPayerId").isJsonNull()) {
            goldPayerId = data.get("goldPayerId").getAsLong();
        }

        SessionManager session = new SessionManager(getContext());
        Long myId = session.getUserId();
        boolean imGold = myId.equals(goldPayerId);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext());
        String pollTitle = poll.has("title") ? poll.get("title").getAsString() : "投票";
        builder.setTitle(pollTitle);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        // Status Text
        TextView statusText = new TextView(getContext());
        statusText.setText("当前结果: " + winner + (imGold ? " (您是金主，权重x1.5!)" : ""));
        statusText.setTextSize(16);
        statusText.setPadding(0, 0, 0, 20);
        if (imGold)
            statusText.setTextColor(0xFFFFC107); // Gold color
        layout.addView(statusText);

        RadioGroup group = new RadioGroup(getContext());

        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                JsonObject d = details.get(i).getAsJsonObject();
                String opt = d.has("option") ? d.get("option").getAsString() : "选项";
                double score = d.has("score") && !d.get("score").isJsonNull() ? d.get("score").getAsDouble() : 0.0;
                int count = d.has("count") && !d.get("count").isJsonNull() ? d.get("count").getAsInt() : 0;

                RadioButton rb = new RadioButton(getContext());
                rb.setText(String.format("%s (%.1f分 / %d票)", opt, score, count));
                rb.setId(i);
                group.addView(rb);
            }
        }

        layout.addView(group);
        builder.setView(layout);

        // Buttons
        String status = poll.has("status") ? poll.get("status").getAsString() : "ACTIVE";

        if ("ACTIVE".equals(status)) {
            Long pollId = poll.get("id").getAsLong();
            builder.setPositiveButton("投票", (dialog, which) -> {
                int selectedId = group.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(getContext(), "请选择一项", Toast.LENGTH_SHORT).show();
                    return;
                }
                submitVote(pollId, selectedId);
            });
        }
        builder.setNegativeButton("关闭", null);
        builder.show();
    }

    private void submitVote(Long pollId, int optionIndex) {
        if (getContext() == null)
            return;
        SessionManager session = new SessionManager(getContext());
        JsonObject req = new JsonObject();
        req.addProperty("optionIndex", optionIndex);

        RetrofitClient.getApiService().vote(session.getUserId(), pollId, req).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!isAdded() || getContext() == null)
                    return;

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    if (body.get("code").getAsInt() == 200) {
                        Toast.makeText(getContext(), "投票成功！", Toast.LENGTH_SHORT).show();
                        // Refresh poll list to show updated status (COMPLETED if all voted)
                        loadPolls();
                        // Dismiss any dialog and return to list view
                    } else {
                        Toast.makeText(getContext(), body.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "网络错误", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
