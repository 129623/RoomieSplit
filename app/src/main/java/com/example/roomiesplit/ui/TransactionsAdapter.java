package com.example.roomiesplit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.roomiesplit.R;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

    private List<JsonObject> transactions = new ArrayList<>();
    private Map<Long, String> userMap;

    public void setTransactions(List<JsonObject> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    public void setUserMap(Map<Long, String> userMap) {
        this.userMap = userMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JsonObject tx = transactions.get(position);

        // Description
        String desc = tx.has("description") && !tx.get("description").isJsonNull()
                ? tx.get("description").getAsString()
                : "无描述";
        if (holder.textDesc != null)
            holder.textDesc.setText(desc);

        // Amount
        double amount = tx.has("amount") ? tx.get("amount").getAsDouble() : 0;
        if (holder.textAmount != null)
            holder.textAmount.setText(String.format("¥%.2f", amount));

        // Category
        String category = tx.has("category") && !tx.get("category").isJsonNull()
                ? tx.get("category").getAsString()
                : "其他";
        if (holder.textCategory != null)
            holder.textCategory.setText(category);

        // Set category icon and background color
        if (holder.iconCategory != null) {
            int iconRes;
            int bgTint;

            switch (category) {
                case "餐饮":
                    iconRes = R.drawable.ic_category_food;
                    bgTint = 0xFFFFF3E0; // Light Orange
                    break;
                case "交通":
                    iconRes = R.drawable.ic_category_transport;
                    bgTint = 0xFFE3F2FD; // Light Blue
                    break;
                case "购物":
                    iconRes = R.drawable.ic_category_shopping;
                    bgTint = 0xFFE8F5E9; // Light Green
                    break;
                case "娱乐":
                    iconRes = R.drawable.ic_category_fun;
                    bgTint = 0xFFF3E5F5; // Light Purple
                    break;
                default:
                    iconRes = R.drawable.ic_category_other;
                    bgTint = 0xFFF5F5F5; // Light Gray
                    break;
            }
            holder.iconCategory.setImageResource(iconRes);
            holder.iconCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgTint));
        }

        // Payer
        long payerId = tx.has("payerId") ? tx.get("payerId").getAsLong() : -1;
        String payerName = (userMap != null && userMap.containsKey(payerId)) ? userMap.get(payerId) : "用户 " + payerId;
        if (holder.textPayer != null)
            holder.textPayer.setText("付款人: " + payerName);

        // Time
        String dateStr = tx.has("transactionDate") && !tx.get("transactionDate").isJsonNull()
                ? tx.get("transactionDate").getAsString()
                : (tx.has("date") && !tx.get("date").isJsonNull() ? tx.get("date").getAsString() : "");

        // Simple formatting
        if (dateStr != null && !dateStr.isEmpty()) {
            dateStr = dateStr.replace("T", " ");
            // If length is enough, take yyyy-MM-dd HH:mm
            if (dateStr.length() > 16)
                dateStr = dateStr.substring(0, 16);
        }

        if (holder.textTime != null)
            holder.textTime.setText(dateStr);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iconCategory;
        TextView textDesc;
        TextView textCategory;
        TextView textAmount;
        TextView textPayer;
        TextView textTime;

        ViewHolder(View itemView) {
            super(itemView);
            iconCategory = itemView.findViewById(R.id.icon_category);
            textDesc = itemView.findViewById(R.id.text_desc);
            textCategory = itemView.findViewById(R.id.text_category);
            textAmount = itemView.findViewById(R.id.text_amount);
            textPayer = itemView.findViewById(R.id.text_payer);
            textTime = itemView.findViewById(R.id.text_time);
        }
    }
}
