package com.example.roomiesplit.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
                ? tx.get("description").getAsString() : "无描述";
        if (holder.textDesc != null) holder.textDesc.setText(desc);

        // Amount
        double amount = tx.has("amount") ? tx.get("amount").getAsDouble() : 0;
        if (holder.textAmount != null) holder.textAmount.setText(String.format("¥%.2f", amount));

        String dateStr = tx.has("transactionDate") && !tx.get("transactionDate").isJsonNull()
                        ? tx.get("transactionDate").getAsString() : "";
        if(dateStr.contains("T")) dateStr = dateStr.replace("T", " ");
        if (holder.textCategory != null) holder.textCategory.setText(dateStr); 

        // Payer
        long payerId = tx.has("payerId") ? tx.get("payerId").getAsLong() : -1;
        String payerName = (userMap != null && userMap.containsKey(payerId)) ? userMap.get(payerId) : "用户 " + payerId;
        if (holder.textPayer != null) holder.textPayer.setText("付款人: " + payerName);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textDesc;
        TextView textCategory;
        TextView textAmount;
        TextView textPayer;

        ViewHolder(View itemView) {
            super(itemView);
            textDesc = itemView.findViewById(R.id.text_desc);
            textCategory = itemView.findViewById(R.id.text_category);
            textAmount = itemView.findViewById(R.id.text_amount);
            textPayer = itemView.findViewById(R.id.text_payer);
        }
    }
}
