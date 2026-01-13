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
        fetchMessages();
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

                                    if (!isRead) {
                                        boolean isActionable = "INVITE".equalsIgnoreCase(type) && actionUrl != null
                                                && !actionUrl.isEmpty();
                                        list.add(new MessageItem(id, title, content, time, isActionable, actionUrl));
                                    }
                                }

                                RecyclerView recyclerView = getView().findViewById(R.id.recycler_messages);
                                if (recyclerView != null) {
                                    recyclerView.setAdapter(new MessagesAdapter(list));
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "获取消息失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    static class MessageItem {
        Long id;
        String title;
        String message;
        String time;
        boolean isActionable;
        String actionToken;

        public MessageItem(Long id, String title, String message, String time, boolean isActionable,
                String actionToken) {
            this.id = id;
            this.title = title;
            this.message = message;
            this.time = time;
            this.isActionable = isActionable;
            this.actionToken = actionToken;
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
                holder.btnAccept.setOnClickListener(v -> handleAction(v, item, true, holder));
                holder.btnReject.setOnClickListener(v -> handleAction(v, item, false, holder));
            } else {
                holder.actionsContainer.setVisibility(View.GONE);
            }
        }

        private String formatTime(String isoTime) {
            if (isoTime == null || isoTime.isEmpty())
                return "";

            try {
                java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(
                        isoTime.length() > 19 ? isoTime.substring(0, 19) : isoTime);
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                long seconds = java.time.Duration.between(dateTime, now).getSeconds();

                if (seconds < 60)
                    return "刚刚";
                else if (seconds < 3600)
                    return (seconds / 60) + "分钟前";
                else if (seconds < 86400)
                    return (seconds / 3600) + "小时前";
                else
                    return (seconds / 86400) + "天前";
            } catch (Exception e) {
                return isoTime;
            }
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
                            data.remove(currentPos);
                            notifyItemRemoved(currentPos);
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
