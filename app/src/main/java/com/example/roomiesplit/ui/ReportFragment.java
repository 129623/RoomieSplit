package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.roomiesplit.R;
import com.google.android.material.datepicker.MaterialDatePicker;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportFragment extends Fragment {

    private TextView startDateText;
    private TextView endDateText;
    private long startDate;
    private long endDate;

    {
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
        c.set(java.util.Calendar.HOUR_OF_DAY, 0);
        c.set(java.util.Calendar.MINUTE, 0);
        c.set(java.util.Calendar.SECOND, 0);
        c.set(java.util.Calendar.MILLISECOND, 0);
        startDate = c.getTimeInMillis();

        c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        endDate = c.getTimeInMillis();
    }

    private java.util.Map<Long, String> userMap = new java.util.HashMap<>();
    private TransactionsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        startDateText = view.findViewById(R.id.text_start_date);
        endDateText = view.findViewById(R.id.text_end_date);
        
        // Setup RecyclerView
        androidx.recyclerview.widget.RecyclerView recyclerView = view.findViewById(R.id.recycler_report_transactions);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        adapter = new TransactionsAdapter();
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.btn_back_report)
                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        view.findViewById(R.id.btn_select_range).setOnClickListener(v -> showDatePicker());

        view.findViewById(R.id.btn_export_excel).setOnClickListener(v -> exportToCSV());

        // Fetch members first, then data triggers update
        fetchMemberData();

        return view;
    }

    private void fetchMemberData() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(getContext());
        Long ledgerId = session.getCurrentLedgerId();
        Long userId = session.getUserId();

        if (ledgerId == null) return;

        com.example.roomiesplit.network.RetrofitClient.getApiService().getLedgerDetail(userId, ledgerId)
            .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                @Override
                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                        retrofit2.Response<com.google.gson.JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        com.google.gson.JsonObject body = response.body();
                        if (body.has("data")) {
                            com.google.gson.JsonObject data = body.getAsJsonObject("data");
                             // Ledger detail usually contains "members" list. 
                             // Based on schema, we have ledger_member. 
                             // Let's assume API returns "members" array in data.
                             // If not, we rely on best effort or "User ID" fallback.
                             if (data.has("members")) {
                                 com.google.gson.JsonArray members = data.getAsJsonArray("members");
                                 for(int i=0; i<members.size(); i++) {
                                     com.google.gson.JsonObject m = members.get(i).getAsJsonObject();
                                     long uId = -1;
                                     if (m.has("userId") && !m.get("userId").isJsonNull()) {
                                         uId = m.get("userId").getAsLong();
                                     }
                                     // "displayName" or "email"
                                     String name = "用户 " + uId;
                                     if(m.has("displayName") && !m.get("displayName").isJsonNull()) {
                                         String dn = m.get("displayName").getAsString();
                                         if (!dn.isEmpty()) name = dn;
                                     } else if (m.has("email") && !m.get("email").isJsonNull()) {
                                         name = m.get("email").getAsString();
                                     }
                                     if (uId != -1) userMap.put(uId, name);
                                 }
                                 adapter.setUserMap(userMap);
                             }
                        }
                    }
                    updateDateDisplay(); // Triggers fetchReportData
                }

                @Override
                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                    updateDateDisplay();
                }
            });
    }

    private void showDatePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("选择时间段")
                .setSelection(new androidx.core.util.Pair<>(startDate, endDate))
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            startDate = selection.first;
            endDate = selection.second;
            updateDateDisplay();
        });

        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        startDateText.setText(sdf.format(new Date(startDate)));
        endDateText.setText(sdf.format(new Date(endDate)));

        fetchReportData();
    }

    private void fetchReportData() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null) {
            updateReportUI(0, 0, new java.util.ArrayList<>());
            return;
        }

        com.example.roomiesplit.network.RetrofitClient.getApiService().getTransactions(ledgerId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject body = response.body();
                            if (body.has("data") && body.get("data").isJsonArray()) {
                                com.google.gson.JsonArray data = body.getAsJsonArray("data");

                                double totalSpend = 0;
                                int count = 0;
                                java.util.List<com.google.gson.JsonObject> filteredList = new java.util.ArrayList<>();

                                SimpleDateFormat parser1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault());
                                SimpleDateFormat parser2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                                for (int i = 0; i < data.size(); i++) {
                                    com.google.gson.JsonObject tx = data.get(i).getAsJsonObject();
                                    String dateStr = tx.has("transactionDate") && !tx.get("transactionDate").isJsonNull()
                                            ? tx.get("transactionDate").getAsString()
                                            : (tx.has("date") && !tx.get("date").isJsonNull() ? tx.get("date").getAsString() : null);

                                    boolean include = false;
                                    double amount = tx.has("amount") ? tx.get("amount").getAsDouble() : 0;

                                    if (dateStr != null) {
                                        try {
                                            Date d = null;
                                            String cleanDate = dateStr.replace("T", " ");
                                            if (cleanDate.length() > 10) d = parser1.parse(cleanDate);
                                            else d = parser2.parse(cleanDate);

                                            if (d != null) {
                                                long t = d.getTime();
                                                if (t >= startDate && t < (endDate + 86400000L)) {
                                                    totalSpend += amount;
                                                    count++;
                                                    include = true;
                                                }
                                            }
                                        } catch (Exception e) {}
                                    }
                                    
                                    if(include) {
                                        filteredList.add(tx);
                                    }
                                }

                                updateReportUI(totalSpend, count, filteredList);
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        updateReportUI(0, 0, new java.util.ArrayList<>());
                        Toast.makeText(getContext(), "获取报表失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateReportUI(double totalSpend, int count, java.util.List<com.google.gson.JsonObject> transactions) {
        if (getView() == null)
            return;
        TextView spendText = getView().findViewById(R.id.text_report_total_spend);
        TextView countText = getView().findViewById(R.id.text_report_transaction_count);

        if (spendText != null) {
            spendText.setText(String.format("总支出: ¥ %.2f", totalSpend));
        }
        if (countText != null) {
            countText.setText(String.format("共 %d 笔交易", count));
        }
        
        if (adapter != null) {
            adapter.setTransactions(transactions);
        }
    }

    private void exportToCSV() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null) {
            Toast.makeText(getContext(), "未选择账本", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        intent.putExtra(Intent.EXTRA_TITLE, "消费报表_" + System.currentTimeMillis() + ".xlsx");

        createFileLauncher.launch(intent);
    }

    private final androidx.activity.result.ActivityResultLauncher<Intent> createFileLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    android.net.Uri uri = result.getData().getData();
                    if (uri != null) {
                        fetchAndWriteExcel(uri);
                    }
                }
            });

    private void fetchAndWriteExcel(android.net.Uri uri) {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(getContext());
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null) return;

        Toast.makeText(getContext(), "正在导出...", Toast.LENGTH_SHORT).show();

        com.example.roomiesplit.network.RetrofitClient.getApiService().getTransactions(ledgerId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                           retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            com.google.gson.JsonObject body = response.body();
                            if (body.has("data") && body.get("data").isJsonArray()) {
                                com.google.gson.JsonArray data = body.getAsJsonArray("data");
                                writeDataToExcel(uri, data);
                            } else {
                                Toast.makeText(getContext(), "导出失败: 无数据", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "导出失败: API错误", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        Toast.makeText(getContext(), "导出失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void writeDataToExcel(android.net.Uri uri, com.google.gson.JsonArray data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("消费报表");
            
            // Header
            Row headerRow = sheet.createRow(0);
            String[] columns = {"日期", "项目", "金额", "支付人", "参与人详情"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            SimpleDateFormat parser1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat parser2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (int i = 0; i < data.size(); i++) {
                com.google.gson.JsonObject tx = data.get(i).getAsJsonObject();
                
                // Date Filtering
                String dateStr = tx.has("transactionDate") && !tx.get("transactionDate").isJsonNull()
                        ? tx.get("transactionDate").getAsString()
                        : (tx.has("date") && !tx.get("date").isJsonNull() ? tx.get("date").getAsString() : null);

                boolean include = false;
                if (dateStr != null) {
                    try {
                        String cleanDate = dateStr.replace("T", " ");
                        Date d = cleanDate.length() > 10 ? parser1.parse(cleanDate) : parser2.parse(cleanDate);
                        if (d != null) {
                            long t = d.getTime();
                            if (t >= startDate && t < (endDate + 86400000L)) {
                                include = true;
                            }
                        }
                    } catch (Exception e) {}
                }

                if (include) {
                    Row row = sheet.createRow(rowNum++);
                    
                    // Date
                    row.createCell(0).setCellValue(dateStr != null ? dateStr.replace("T", " ") : "");
                    
                    // Item
                    String title = tx.has("description") && !tx.get("description").isJsonNull() 
                            ? tx.get("description").getAsString() : "无描述";
                    row.createCell(1).setCellValue(title);

                    // Amount
                    double amount = tx.has("amount") ? tx.get("amount").getAsDouble() : 0;
                    row.createCell(2).setCellValue(amount);

                    // Payer
                    long payerId = tx.has("payerId") ? tx.get("payerId").getAsLong() : -1;
                    String payerName = userMap.containsKey(payerId) ? userMap.get(payerId) : "用户 " + payerId;
                    row.createCell(3).setCellValue(payerName); 

                    // Participants Formatting
                    StringBuilder partsBuilder = new StringBuilder();
                    if (tx.has("participants") && tx.get("participants").isJsonArray()) {
                        com.google.gson.JsonArray parts = tx.getAsJsonArray("participants");
                        for(int j=0; j<parts.size(); j++) {
                            com.google.gson.JsonObject p = parts.get(j).getAsJsonObject();
                            long pId = p.has("userId") ? p.get("userId").getAsLong() : -1;
                            double pAmount = p.has("owingAmount") ? p.get("owingAmount").getAsDouble() : 0;
                            String pName = userMap.containsKey(pId) ? userMap.get(pId) : "用户 " + pId;
                            
                            if(partsBuilder.length() > 0) partsBuilder.append(", ");
                            partsBuilder.append(pName).append(": ").append(String.format("%.2f", pAmount));
                        }
                    }
                    row.createCell(4).setCellValue(partsBuilder.toString());
                }
            }

            try (java.io.OutputStream os = requireContext().getContentResolver().openOutputStream(uri)) {
                workbook.write(os);
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "导出成功!", Toast.LENGTH_LONG).show()
                );
            }

        } catch (IOException e) {
            e.printStackTrace();
            getActivity().runOnUiThread(() -> 
                Toast.makeText(getContext(), "导出错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }
}
