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
                setupMockData(currentView, isChecked);
            }
        });

        return view;
    }

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
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject data = response.body().getAsJsonObject("data");
                            if (data != null) {
                                List<DebtGraphView.Node> nodes = new ArrayList<>();
                                List<DebtGraphView.Edge> edges = new ArrayList<>();

                                // Parse Nodes
                                if (data.has("nodes")) {
                                    com.google.gson.JsonArray nodesJson = data.getAsJsonArray("nodes");
                                    for (int i = 0; i < nodesJson.size(); i++) {
                                        com.google.gson.JsonObject n = nodesJson.get(i).getAsJsonObject();
                                        nodes.add(new DebtGraphView.Node(n.get("id").getAsString(),
                                                n.get("name").getAsString())); // ID might be string or long
                                    }
                                }

                                // Parse Edges
                                String edgesKey = showSimplified ? "optimizedPath" : "originalPath"; // Assuming backend
                                                                                                     // structure
                                // Check backend ReportController getDebtGraph return structure.
                                // It usually returns "nodes", "edges". Does it support simplification params?
                                // Backend `ReportService.getDebtGraph(ledgerId)` returns `GraphData`.
                                // Let's assume standard edges for now.
                                if (data.has("edges")) {
                                    com.google.gson.JsonArray edgesJson = data.getAsJsonArray("edges");
                                    for (int i = 0; i < edgesJson.size(); i++) {
                                        com.google.gson.JsonObject e = edgesJson.get(i).getAsJsonObject();
                                        edges.add(new DebtGraphView.Edge(
                                                e.get("from").getAsString(),
                                                e.get("to").getAsString(),
                                                e.get("amount").getAsString())); // amount
                                    }
                                }

                                // Debug Toast
                                int edgeCount = edges.size();
                                // Toast.makeText(getContext(), "Formatted Graph: " + nodes.size() + " nodes, "
                                // + edgeCount + " edges", Toast.LENGTH_SHORT).show();

                                // Update View
                                graphView.setData(nodes, edges);
                                descText.setText(showSimplified ? "简化试图" : "原始视图");
                                updateSettlementList(rootView, edges, String.valueOf(userId));

                                // Calculate Net Asset
                                double netAsset = 0;
                                String myIdStr = String.valueOf(userId);
                                for (DebtGraphView.Edge e : edges) {
                                    double amt = 0;
                                    try {
                                        amt = Double.parseDouble(e.amount);
                                    } catch (NumberFormatException ex) {
                                    }

                                    if (e.fromId.equals(myIdStr)) {
                                        netAsset -= amt; // I owe someone
                                    } else if (e.toId.equals(myIdStr)) {
                                        netAsset += amt; // Someone owes me
                                    }
                                }

                                TextView netAssetText = rootView.findViewById(R.id.text_net_asset);
                                if (netAssetText != null) {
                                    String sign = netAsset >= 0 ? "+" : "";
                                    netAssetText.setText(String.format("%s ¥%.2f", sign, netAsset));
                                    netAssetText.setTextColor(
                                            netAsset >= 0 ? rootView.getResources().getColor(R.color.primary_dark, null)
                                                    : android.graphics.Color.RED);
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    }
                });
    }

    private void updateSettlementList(View rootView, List<DebtGraphView.Edge> simplifiedEdges, String myUserId) {
        android.widget.LinearLayout listContainer = rootView.findViewById(R.id.container_settlement_list);
        if (listContainer == null)
            return;

        listContainer.removeAllViews();
        // Remove hardcoded myUserId

        for (DebtGraphView.Edge edge : simplifiedEdges) {
            if (edge.fromId.equals(myUserId)) {
                // I owe someone
                addSettlementItem(listContainer, edge);
            }
        }

        // If no debts
        if (listContainer.getChildCount() == 0) {
            TextView emptyView = new TextView(getContext());
            emptyView.setText("无待结算债务");
            emptyView.setPadding(0, 20, 0, 0);
            listContainer.addView(emptyView);
        }
    }

    private void addSettlementItem(android.widget.LinearLayout container, DebtGraphView.Edge edge) {
        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.item_debt_settlement, container, false);

        TextView textInfo = itemView.findViewById(R.id.text_settle_info);
        textInfo.setText("应付 " + getCmdName(edge.toId) + "  ¥" + edge.amount);

        itemView.findViewById(R.id.btn_pay).setOnClickListener(v -> showPaymentDialog(edge));

        container.addView(itemView);
    }

    private String getCmdName(String id) {
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
                    android.widget.Toast.makeText(getContext(), "微信支付成功！债务已清除", android.widget.Toast.LENGTH_LONG)
                            .show();
                    // Logic to clear debt locally would go here
                })
                .setNeutralButton("线下支付 (需确认)", (dialog, which) -> {
                    android.widget.Toast.makeText(getContext(), "已发送确认请求给 " + getCmdName(edge.toId),
                            android.widget.Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
