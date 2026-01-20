package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.roomiesplit.R;
import java.util.ArrayList;
import java.util.List;

public class DebtFragment extends Fragment {

    private DebtGraphView graphView;
    private Switch simplifySwitch;
    private TextView descText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debt, container, false);

        graphView = view.findViewById(R.id.debt_graph_view);
        simplifySwitch = view.findViewById(R.id.switch_simplify);
        descText = view.findViewById(R.id.text_graph_desc);

        setupMockData(view, true); // Default simplified

        simplifySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // When callback fires, getView() might be safe, but let's be robust
            View currentView = getView();
            if (currentView != null) {
                // Use cached data to update view immediately
                com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                        getContext());
                Long userId = session.getUserId();
                updateView(currentView, isChecked, userId);
            }
        });

        return view;
    }

    private List<DebtGraphView.Edge> originalEdges = new ArrayList<>();
    private List<DebtGraphView.Edge> simplifiedEdges = new ArrayList<>();
    private List<DebtGraphView.Node> loadedNodes = new ArrayList<>();

    private void setupMockData(View rootView, boolean showSimplified) {
        // Fetch real data
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();
        Long userId = session.getUserId();

        if (ledgerId == null)
            return;

        com.example.roomiesplit.network.RetrofitClient.getApiService().getDebtGraph(ledgerId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        try {
                            if (getContext() == null || !isAdded()) {
                                return;
                            }
                            if (response.isSuccessful() && response.body() != null) {
                                com.google.gson.JsonObject body = response.body();
                                // Check business code if exists
                                if (body.has("code") && body.get("code").getAsInt() != 200) {
                                    String msg = body.has("message") ? body.get("message").getAsString()
                                            : (body.has("msg") ? body.get("msg").getAsString() : "Unknown Error");
                                    android.widget.Toast
                                            .makeText(getContext(), "Error: " + msg, android.widget.Toast.LENGTH_SHORT)
                                            .show();
                                    return;
                                }

                                com.google.gson.JsonObject data = null;
                                if (body.has("data") && !body.get("data").isJsonNull()) {
                                    if (body.get("data").isJsonObject()) {
                                        data = body.getAsJsonObject("data");
                                    }
                                }

                                if (data != null) {
                                    loadedNodes.clear();
                                    originalEdges.clear();
                                    simplifiedEdges.clear();

                                    // Parse Nodes
                                    try {
                                        if (data.has("nodes") && !data.get("nodes").isJsonNull()) {
                                            com.google.gson.JsonArray nodesJson = data.getAsJsonArray("nodes");
                                            for (int i = 0; i < nodesJson.size(); i++) {
                                                if (nodesJson.get(i).isJsonObject()) {
                                                    com.google.gson.JsonObject n = nodesJson.get(i).getAsJsonObject();
                                                    String id = n.has("id") && !n.get("id").isJsonNull()
                                                            ? n.get("id").getAsString()
                                                            : "";
                                                    String name = n.has("name") && !n.get("name").isJsonNull()
                                                            ? n.get("name").getAsString()
                                                            : "无名氏";
                                                    if (!id.isEmpty()) {
                                                        loadedNodes.add(new DebtGraphView.Node(id, name));
                                                        if (n.has("avatar") && !n.get("avatar").isJsonNull()) {
                                                            avatarUrlMap.put(id, n.get("avatar").getAsString());
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    // Parse Original Edges
                                    try {
                                        if (data.has("originalEdges") && !data.get("originalEdges").isJsonNull()) {
                                            com.google.gson.JsonArray edgesJson = data.getAsJsonArray("originalEdges");
                                            for (int i = 0; i < edgesJson.size(); i++) {
                                                if (edgesJson.get(i).isJsonObject()) {
                                                    com.google.gson.JsonObject e = edgesJson.get(i).getAsJsonObject();
                                                    if (e.has("from") && e.has("to")) {
                                                        originalEdges.add(new DebtGraphView.Edge(
                                                                e.get("from").getAsString(),
                                                                e.get("to").getAsString(),
                                                                e.has("amount") && !e.get("amount").isJsonNull()
                                                                        ? e.get("amount").getAsString()
                                                                        : "0"));
                                                    }
                                                }
                                            }
                                        } else if (data.has("edges") && !data.get("edges").isJsonNull()) {
                                            // Fallback
                                            com.google.gson.JsonArray edgesJson = data.getAsJsonArray("edges");
                                            for (int i = 0; i < edgesJson.size(); i++) {
                                                if (edgesJson.get(i).isJsonObject()) {
                                                    com.google.gson.JsonObject e = edgesJson.get(i).getAsJsonObject();
                                                    if (e.has("from") && e.has("to")) {
                                                        originalEdges.add(new DebtGraphView.Edge(
                                                                e.get("from").getAsString(),
                                                                e.get("to").getAsString(),
                                                                e.has("amount") && !e.get("amount").isJsonNull()
                                                                        ? e.get("amount").getAsString()
                                                                        : "0"));
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    // Parse Simplified Edges
                                    try {
                                        if (data.has("simplifiedEdges") && !data.get("simplifiedEdges").isJsonNull()) {
                                            com.google.gson.JsonArray edgesJson = data
                                                    .getAsJsonArray("simplifiedEdges");
                                            for (int i = 0; i < edgesJson.size(); i++) {
                                                if (edgesJson.get(i).isJsonObject()) {
                                                    com.google.gson.JsonObject e = edgesJson.get(i).getAsJsonObject();
                                                    if (e.has("from") && e.has("to")) {
                                                        simplifiedEdges.add(new DebtGraphView.Edge(
                                                                e.get("from").getAsString(),
                                                                e.get("to").getAsString(),
                                                                e.has("amount") && !e.get("amount").isJsonNull()
                                                                        ? e.get("amount").getAsString()
                                                                        : "0"));
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                    updateView(rootView, showSimplified, userId);
                                } else {
                                    android.widget.Toast
                                            .makeText(getContext(), "无数据", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (getContext() != null)
                                android.widget.Toast.makeText(getContext(), "系统错误: " + e.getMessage(),
                                        android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        t.printStackTrace();
                        android.widget.Toast
                                .makeText(getContext(), "网络错误: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private java.util.Map<String, String> avatarUrlMap = new java.util.HashMap<>();

    private void updateView(View rootView, boolean showSimplified, Long userId) {
        List<DebtGraphView.Edge> edgesToShow = showSimplified ? simplifiedEdges : originalEdges;

        graphView.setData(loadedNodes, edgesToShow);
        descText.setText(showSimplified ? "简化试图" : "原始视图");

        // Load Avatars
        for (DebtGraphView.Node node : loadedNodes) {
            String url = avatarUrlMap.get(node.id);
            if (url != null && !url.isEmpty()) {
                com.bumptech.glide.Glide.with(this)
                        .asBitmap()
                        .load(url)
                        .circleCrop()
                        .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull android.graphics.Bitmap resource,
                                    @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                                if (graphView != null) {
                                    graphView.updateNodeAvatar(node.id, resource);
                                }
                            }

                            @Override
                            public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {
                            }
                        });
            }
        }

        // Always show simplified list
        List<DebtGraphView.Edge> listEdges = simplifiedEdges;
        updateSettlementList(rootView, listEdges, String.valueOf(userId));

        // Calculate Net Asset
        double netAsset = 0;
        String myIdStr = String.valueOf(userId);
        for (DebtGraphView.Edge e : edgesToShow) {
            double amt = 0;
            try {
                if (e.amount != null)
                    amt = Double.parseDouble(e.amount);
            } catch (NumberFormatException ex) {
            }

            if (e.fromId != null && e.fromId.equals(myIdStr)) {
                netAsset -= amt; // I owe someone
            } else if (e.toId != null && e.toId.equals(myIdStr)) {
                netAsset += amt; // Someone owes me
            }
        }

        TextView netAssetText = rootView.findViewById(R.id.text_net_asset);
        if (netAssetText != null) {
            if (Math.abs(netAsset) < 0.01) {
                netAssetText.setText("¥0.00");
                netAssetText.setTextColor(android.graphics.Color.GRAY);
            } else {
                String sign = netAsset > 0 ? "+" : "";
                netAssetText.setText(String.format("%s ¥%.2f", sign, netAsset));
                netAssetText.setTextColor(
                        netAsset > 0 ? androidx.core.content.ContextCompat.getColor(getContext(), R.color.primary_dark)
                                : android.graphics.Color.RED);
            }
        }
    }

    private void updateSettlementList(View rootView, List<DebtGraphView.Edge> listEdges, String myUserId) {
        android.widget.LinearLayout listContainer = rootView.findViewById(R.id.container_settlement_list);
        if (listContainer == null)
            return;

        listContainer.removeAllViews();

        boolean hasItems = false;

        // 1. Debts I owe (Payable)
        for (DebtGraphView.Edge edge : listEdges) {
            if (edge.fromId != null && edge.fromId.equals(myUserId)) {
                addSettlementItem(listContainer, edge, false);
                hasItems = true;
            }
        }

        // 2. Debts owed to me (Receivable)
        for (DebtGraphView.Edge edge : listEdges) {
            if (edge.toId != null && edge.toId.equals(myUserId)) {
                addSettlementItem(listContainer, edge, true);
                hasItems = true;
            }
        }

        // If no debts
        if (!hasItems) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("无待结算债务");
            emptyView.setPadding(0, 20, 0, 0);
            listContainer.addView(emptyView);
        }
    }

    private void addSettlementItem(android.widget.LinearLayout container, DebtGraphView.Edge edge,
            boolean isReceivable) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_debt_settlement, container, false);

        TextView textInfo = itemView.findViewById(R.id.text_settle_info);
        com.google.android.material.button.MaterialButton actionBtn = itemView.findViewById(R.id.btn_pay);
        android.widget.ImageView icon = itemView.findViewById(R.id.img_debt_direction);

        if (itemView == null || textInfo == null || actionBtn == null || icon == null)
            return;

        // Let's just stick to Text and Button changes which represent the requirement.

        if (isReceivable) {
            textInfo.setText(getCmdName(edge.fromId) + " 应付我 ¥" + edge.amount);
            textInfo.setTextColor(container.getResources().getColor(android.R.color.holo_green_dark, null));

            // Set Icon to Green styling
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE8F5E9)); // Light Green
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D32)); // Dark Green

            actionBtn.setText("提醒付款");
            actionBtn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // Green
            actionBtn.setOnClickListener(v -> {
                com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                        getContext());
                Long myId = session.getUserId();
                Long targetId = 0L;
                try {
                    targetId = Long.parseLong(edge.fromId);
                } catch (Exception e) {
                }

                com.google.gson.JsonObject body = new com.google.gson.JsonObject();
                body.addProperty("fromUserId", myId);
                body.addProperty("toUserId", targetId);
                body.addProperty("amount", edge.amount);

                com.example.roomiesplit.network.RetrofitClient.getApiService().remindPayment(body)
                        .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                            @Override
                            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                    retrofit2.Response<com.google.gson.JsonObject> response) {
                                if (response.isSuccessful()) {
                                    android.widget.Toast
                                            .makeText(getContext(), "已向 " + getCmdName(edge.fromId) + " 发送收款提醒",
                                                    android.widget.Toast.LENGTH_SHORT)
                                            .show();
                                } else {
                                    android.widget.Toast
                                            .makeText(getContext(), "发送失败", android.widget.Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                                android.widget.Toast.makeText(getContext(), "网络错误", android.widget.Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
            });
        } else {
            textInfo.setText("应付 " + getCmdName(edge.toId) + "  ¥" + edge.amount);
            textInfo.setTextColor(container.getResources().getColor(R.color.black, null));

            // Icon default Red styling (already in XML but good to be explicit if recycled)
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFEBEE)); // Light Red
            icon.setImageTintList(android.content.res.ColorStateList.valueOf(0xFFD32F2F)); // Red

            actionBtn.setText("立即结算");
            actionBtn.setOnClickListener(v -> showPaymentDialog(edge));
        }

        container.addView(itemView);
    }

    private String getCmdName(String id) {
        if (id == null)
            return "Unknown";
        if (id.equals("user_c"))
            return "Charlie";
        if (id.equals("user_b"))
            return "Bob";
        return id;
    }

    private void showPaymentDialog(DebtGraphView.Edge edge) {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("立即结算")
                .setMessage("选择支付方式向 " + getCmdName(edge.toId) + " 支付 ¥" + edge.amount)
                .setPositiveButton("微信支付 (模拟)", (dialog, which) -> {
                    callSmartSettle(edge, "WECHAT");
                })
                .setNeutralButton("线下支付 (需确认)", (dialog, which) -> {
                    callSmartSettle(edge, "OFFLINE");
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void callSmartSettle(DebtGraphView.Edge edge, String method) {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null)
            return;

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("ledgerId", ledgerId);
        body.addProperty("fromUserId", edge.fromId); // edge.fromId is the payer (Me, usually)
        body.addProperty("toUserId", edge.toId);
        body.addProperty("amount", edge.amount);
        body.addProperty("method", method);

        com.example.roomiesplit.network.RetrofitClient.getApiService().smartSettle(body)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            String msg = method.equals("WECHAT") ? "微信支付成功！已自动消除相关债务路径" : "已发送确认请求，待对方确认";
                            android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_LONG).show();
                            // Refresh data
                            setupMockData(getView(), simplifySwitch.isChecked());
                        } else {
                            android.widget.Toast.makeText(getContext(), "结算失败: " + response.code(),
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        android.widget.Toast
                                .makeText(getContext(), "网络错误: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }
}
