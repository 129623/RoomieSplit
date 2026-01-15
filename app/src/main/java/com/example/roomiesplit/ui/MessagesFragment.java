package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomiesplit.R;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class MessagesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new MessagesAdapter(new ArrayList<MessageItem>()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load local first
        loadFromDb();
        fetchMessages();
    }

    private void loadFromDb() {
        com.example.roomiesplit.database.NotificationDbHelper dbHelper = new com.example.roomiesplit.database.NotificationDbHelper(
                getContext());
        List<MessageItem> localItems = dbHelper.getAllNotifications();
        RecyclerView recyclerView = getView() != null ? getView().findViewById(R.id.recycler_messages) : null;
        if (recyclerView != null) {
            recyclerView.setAdapter(new MessagesAdapter(localItems));
        }
    }

    private void fetchMessages() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long userId = session.getUserId();

        if (userId == -1)
            return;

        com.example.roomiesplit.network.RetrofitClient.getApiService().getMyNotifications(userId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject body = response.body();
                            if (body.has("data") && body.get("data").isJsonArray()) {
                                com.google.gson.JsonArray data = body.getAsJsonArray("data");
                                List<MessageItem> list = new ArrayList<>();

                                for (int i = 0; i < data.size(); i++) {
                                    com.google.gson.JsonObject notif = data.get(i).getAsJsonObject();
                                    Long id = notif.has("id") ? notif.get("id").getAsLong() : -1;
                                    String type = notif.has("type") ? notif.get("type").getAsString() : "INFO";
                                    String title = notif.has("title") ? notif.get("title").getAsString() : "";
                                    String content = notif.has("message") ? notif.get("message").getAsString() : "";
                                    String actionUrl = notif.has("actionUrl") && !notif.get("actionUrl").isJsonNull()
                                            ? notif.get("actionUrl").getAsString()
                                            : null;
                                    String time = notif.has("createdAt") ? notif.get("createdAt").getAsString() : "";
                                    boolean isRead = notif.has("isRead") && notif.get("isRead").getAsBoolean();

                                    // Show ALL messages, not just unread
                                    // if (!isRead) { // Removed filter
                                    boolean isActionable = "INVITE".equalsIgnoreCase(type) && actionUrl != null
                                            && !actionUrl.isEmpty();
                                    if ("PAYMENT_CONFIRMATION".equalsIgnoreCase(type)) {
                                        isActionable = true;
                                    }
                                    list.add(new MessageItem(id, title, content, time, isActionable, actionUrl,
                                            type));

                                    // Auto-mark as read ONLY if it is currently unread
                                    if (!isRead && id != -1) {
                                        com.example.roomiesplit.network.RetrofitClient.getApiService()
                                                .markNotificationRead(userId, id)
                                                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                                    @Override
                                                    public void onResponse(
                                                            retrofit2.Call<com.google.gson.JsonObject> call,
                                                            retrofit2.Response<com.google.gson.JsonObject> response) {
                                                    }

                                                    @Override
                                                    public void onFailure(
                                                            retrofit2.Call<com.google.gson.JsonObject> call,
                                                            Throwable t) {
                                                    }
                                                });
                                    }
                                    // }
                                }

                                // Save new messages to DB
                                com.example.roomiesplit.database.NotificationDbHelper dbHelper = new com.example.roomiesplit.database.NotificationDbHelper(
                                        getContext());
                                dbHelper.insertOrUpdate(list);

                                // Refresh View from DB (Merged Local + Remote)
                                loadFromDb();
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "获取消息失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class MessageItem {
        public Long id;
        public String title;
        public String message;
        public String time;
        public boolean isActionable;
        public String actionToken; // For Invite: token. For Payment: actionUrl
        public String type;

        public MessageItem(Long id, String title, String message, String time, boolean isActionable,
                String actionToken, String type) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.time = time;
            this.isActionable = isActionable;
            this.actionToken = actionToken;
            this.type = type;
        }
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
        private final List<MessageItem> data;

        public MessagesAdapter(List<MessageItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MessageItem item = data.get(position);
            holder.title.setText(item.title != null && !item.title.isEmpty() ? item.title : "通知");
            holder.message.setText(item.message != null && !item.message.isEmpty() ? item.message : "");
            holder.time.setText(formatTime(item.time));

            if (item.isActionable) {
                holder.actionsContainer.setVisibility(View.VISIBLE);

                if ("PAYMENT_CONFIRMATION".equalsIgnoreCase(item.type)) {
                    holder.btnAccept.setText("确认收款");
                    holder.btnReject.setVisibility(View.GONE);
                    holder.btnAccept.setOnClickListener(v -> handleConfirmPayment(v, item, holder));
                } else {
                    // INVITE
                    holder.btnAccept.setText("接受");
                    holder.btnReject.setVisibility(View.VISIBLE);
                    holder.btnReject.setText("拒绝");
                    holder.btnAccept.setOnClickListener(v -> handleAction(v, item, true, holder));
                    holder.btnReject.setOnClickListener(v -> handleAction(v, item, false, holder));
                }
            } else {
                holder.actionsContainer.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos);
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                        .setTitle("删除消息")
                        .setMessage("确定要删除这条消息吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            int currentPos = holder.getAdapterPosition();
                            if (currentPos != RecyclerView.NO_POSITION) {
                                // 1. Remove from Local DB
                                new com.example.roomiesplit.database.NotificationDbHelper(v.getContext())
                                        .deleteNotification(item.id);

                                // 2. Remove from UI
                                data.remove(currentPos);
                                notifyItemRemoved(currentPos);

                                // 3. Call API
                                if (item.id != -1) {
                                    com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                                            v.getContext());
                                    com.example.roomiesplit.network.RetrofitClient.getApiService()
                                            .deleteNotification(session.getUserId(), item.id)
                                            .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                                @Override
                                                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                }

                                                @Override
                                                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                                }
                                            });
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        private String formatTime(String isoTime) {
            if (isoTime == null || isoTime.isEmpty())
                return "";

            java.time.ZonedDateTime msgTime;
            try {
                // Attempt 1: Parse standard ISO (e.g. 2023-01-01T12:00:00Z)
                msgTime = java.time.ZonedDateTime.parse(isoTime);
            } catch (Exception e) {
                try {
                    // Attempt 2: Parse as LocalDateTime and assume UTC (backend default)
                    String cleanTime = isoTime.length() > 19 ? isoTime.substring(0, 19) : isoTime;
                    cleanTime = cleanTime.replace(" ", "T");
                    msgTime = java.time.LocalDateTime.parse(cleanTime)
                            .atZone(java.time.ZoneId.of("UTC"));
                } catch (Exception e2) {
                    return isoTime;
                }
            }

            java.time.ZonedDateTime now = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault());
            long seconds = java.time.Duration.between(msgTime, now).getSeconds();

            if (seconds < 0) {
                // Time skew (future msg), treat as just now or show date if huge?
                // If it is really future, 'Just now' matches perception of 'New'.
                // If it is -hours (e.g. timezone mismatch), "Just now" hides bug but better
                // than "-2 hours ago".
                return "刚刚";
            }

            if (seconds < 60)
                return "刚刚";
            else if (seconds < 3600)
                return (seconds / 60) + "分钟前";
            else if (seconds < 86400)
                return (seconds / 3600) + "小时前";
            else if (seconds < 259200) // Up to 3 days show "x days ago"
                return (seconds / 86400) + "天前";
            else
                return msgTime.format(java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm"));
        }

        private void handleConfirmPayment(View v, MessageItem item, final ViewHolder holder) {
            // Parse settlement ID from actionUrl: app://settlement/confirm?id=123
            // Or maybe just try to parsing it
            Long settlementId = -1L;
            if (item.actionToken != null && item.actionToken.contains("id=")) {
                try {
                    String idStr = item.actionToken.substring(item.actionToken.indexOf("id=") + 3);
                    settlementId = Long.parseLong(idStr);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            if (settlementId == -1) {
                Toast.makeText(v.getContext(), "无效的结算ID", Toast.LENGTH_SHORT).show();
                return;
            }

            com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                    v.getContext());
            Long userId = session.getUserId(); // Not strictly needed for the API but good for session check

            Toast.makeText(v.getContext(), "正在确认...", Toast.LENGTH_SHORT).show();

            com.example.roomiesplit.network.RetrofitClient.getApiService().confirmSettlement(settlementId)
                    .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                retrofit2.Response<com.google.gson.JsonObject> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(v.getContext(), "收款已确认", Toast.LENGTH_SHORT).show();

                                // Update item instead of removing
                                int currentPos = holder.getAdapterPosition();
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    item.isActionable = false;
                                    item.message = "已确认收款 ¥" + (item.message.contains("¥")
                                            ? item.message.replaceAll(".*¥([0-9\\.]+).*", "$1")
                                            : "");
                                    if (item.message.endsWith("."))
                                        item.message = "已确认收款";
                                    // Use simple text if parsing fails
                                    if (!item.message.startsWith("已确认"))
                                        item.message = "已确认收款";

                                    item.actionToken = "";
                                    notifyItemChanged(currentPos);

                                    // Update Local DB
                                    com.example.roomiesplit.database.NotificationDbHelper dbHelper = new com.example.roomiesplit.database.NotificationDbHelper(
                                            v.getContext());
                                    java.util.List<MessageItem> updateList = new java.util.ArrayList<>();
                                    updateList.add(item);
                                    dbHelper.insertOrUpdate(updateList);
                                }

                                // Mark notification read
                                if (item.id != -1) {
                                    com.example.roomiesplit.network.RetrofitClient.getApiService()
                                            .markNotificationRead(userId, item.id)
                                            .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                                @Override
                                                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                }

                                                @Override
                                                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                                }
                                            });
                                }
                            } else {
                                Toast.makeText(v.getContext(), "确认失败: " + response.message(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                            Toast.makeText(v.getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        private void handleAction(View v, MessageItem item, boolean accept, final ViewHolder holder) {
            com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                    v.getContext());
            Long userId = session.getUserId();

            Toast.makeText(v.getContext(), accept ? "正在加入..." : "正在拒绝...", Toast.LENGTH_SHORT).show();

            retrofit2.Call<com.google.gson.JsonObject> call;
            if (accept) {
                call = com.example.roomiesplit.network.RetrofitClient.getApiService().acceptInvitation(userId,
                        item.actionToken);
            } else {
                call = com.example.roomiesplit.network.RetrofitClient.getApiService().rejectInvitation(userId,
                        item.actionToken);
            }

            call.enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                        retrofit2.Response<com.google.gson.JsonObject> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(v.getContext(), accept ? "加入成功，请刷新账本列表" : "已拒绝", Toast.LENGTH_LONG).show();

                        if (item.id != -1) {
                            com.example.roomiesplit.network.RetrofitClient.getApiService()
                                    .markNotificationRead(userId, item.id)
                                    .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                retrofit2.Response<com.google.gson.JsonObject> response) {
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                Throwable t) {
                                        }
                                    });
                        }

                        int currentPos = holder.getAdapterPosition();
                        if (currentPos != RecyclerView.NO_POSITION) {
                            item.isActionable = false;
                            item.message = accept ? "您已成功加入账本" : "您已拒绝邀请";
                            item.actionToken = "";
                            notifyItemChanged(currentPos);

                            // Update Local DB
                            com.example.roomiesplit.database.NotificationDbHelper dbHelper = new com.example.roomiesplit.database.NotificationDbHelper(
                                    v.getContext());
                            java.util.List<MessageItem> updateList = new java.util.ArrayList<>();
                            updateList.add(item);
                            dbHelper.insertOrUpdate(updateList);
                        }
                    } else {
                        Toast.makeText(v.getContext(), "操作失败: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    Toast.makeText(v.getContext(), "网络错误", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, message, time;
            View actionsContainer;
            Button btnAccept, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.text_title);
                message = itemView.findViewById(R.id.text_message);
                time = itemView.findViewById(R.id.text_time);
                actionsContainer = itemView.findViewById(R.id.container_actions);
                btnAccept = itemView.findViewById(R.id.btn_accept);
                btnReject = itemView.findViewById(R.id.btn_reject);
            }
        }
    }
}
