package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.roomiesplit.R;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

public class AddBillFragment extends Fragment {

    private java.time.LocalDate selectedDate = java.time.LocalDate.now();
    private Long selectedPayerId = null;
    private String selectedPayerName = null;
    private java.util.List<com.example.roomiesplit.ui.DashboardFragment.LedgerMember> members = new java.util.ArrayList<>();
    private android.widget.TextView dateText;
    private android.widget.TextView payerText;

    private String selectedCategory = "餐饮"; // Default
    private String selectedSplitType = "EQUAL"; // EQUAL, PERCENT, EXACT

    // UI References for Split Type Tabs
    private TextView btnSplitEqual, btnSplitWeight, btnSplitExact;
    // Map for dynamic inputs
    private java.util.Map<Long, android.widget.EditText> participantInputViews = new java.util.HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_bill, container, false);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> Navigation.findNavController(view).popBackStack());
        view.findViewById(R.id.btn_save).setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

        // Date
        // Check Arguments
        if (getArguments() != null && getArguments().containsKey("date")) {
            String dateStr = getArguments().getString("date");
            try {
                selectedDate = java.time.LocalDate.parse(dateStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        dateText = view.findViewById(R.id.text_date);
        updateDateUI();
        view.findViewById(R.id.container_date).setOnClickListener(v -> showDatePicker());

        // Payer
        payerText = view.findViewById(R.id.text_payer);
        // Default to current user
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        selectedPayerId = session.getUserId();
        selectedPayerName = session.getUsername(); // This might be null/empty, but safe to set
        updatePayerUI();
        view.findViewById(R.id.container_payer).setOnClickListener(v -> showPayerSelectionDialog());

        // Smart Entry Listener
        view.findViewById(R.id.btn_smart_parse).setOnClickListener(v -> {
            EditText smartInput = view.findViewById(R.id.input_smart_entry);
            String raw = smartInput.getText().toString().trim();
            if (!raw.isEmpty()) {
                parseSmartEntry(raw, view);
                // Hide keyboard
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity()
                        .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        // Amount Input Listener for Real-time Splitting
        android.widget.EditText etAmount = view.findViewById(R.id.input_amount);
        etAmount.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateSplits();
            }
        });

        // --- Category Selection Logic ---
        setupCategorySelection(view);

        // --- Split Type Selection Logic ---
        setupSplitTypeSelection(view);

        // Load Members
        loadLedgerMembers();

        view.findViewById(R.id.btn_confirm_add).setOnClickListener(v -> {
            android.widget.EditText amountInput = view.findViewById(R.id.input_amount);
            android.widget.EditText descInput = view.findViewById(R.id.input_description);

            String amountStr = amountInput.getText().toString();
            String desc = descInput.getText().toString();

            if (amountStr.isEmpty()) {
                android.widget.Toast.makeText(getContext(), "请输入金额", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            double totalAmount = Double.parseDouble(amountStr);

            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("amount", totalAmount);
            json.addProperty("description", desc);
            json.addProperty("category", selectedCategory);
            json.addProperty("currency", "CNY");
            json.addProperty("splitType", selectedSplitType);
            // Backend expects 'date' field mapping to LocalDateTime
            // We append T00:00:00 to make it parsed as LocalDateTime by Jackson
            json.addProperty("date", selectedDate.toString() + "T00:00:00");

            com.google.gson.JsonArray participantsArray = new com.google.gson.JsonArray();

            if ("EQUAL".equals(selectedSplitType)) {
                long selectedCount = 0;
                for (Boolean b : selectedParticipants.values()) {
                    if (b)
                        selectedCount++;
                }
                double perPerson = selectedCount > 0 ? totalAmount / selectedCount : 0;
                for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                    if (selectedParticipants.get(m.id)) {
                        com.google.gson.JsonObject p = new com.google.gson.JsonObject();
                        p.addProperty("userId", m.id);
                        p.addProperty("amount", perPerson);
                        participantsArray.add(p);
                    }
                }
            } else if ("PERCENT".equals(selectedSplitType) || "WEIGHT".equals(selectedSplitType)) { // Using WEIGHT
                                                                                                    // logic for now
                double totalWeight = 0;
                // First pass calc total weight
                for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                    if (selectedParticipants.get(m.id)) {
                        android.widget.EditText input = participantInputViews.get(m.id);
                        double w = 1;
                        if (input != null && input.getText().length() > 0) {
                            try {
                                w = Double.parseDouble(input.getText().toString());
                            } catch (Exception e) {
                            }
                        }
                        if (w < 0)
                            w = 0;
                        totalWeight += w;
                    }
                }
                if (totalWeight == 0)
                    totalWeight = 1; // Avoid divide by zero

                for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                    if (selectedParticipants.get(m.id)) {
                        android.widget.EditText input = participantInputViews.get(m.id);
                        double w = 1;
                        if (input != null && input.getText().length() > 0) {
                            try {
                                w = Double.parseDouble(input.getText().toString());
                            } catch (Exception e) {
                            }
                        }
                        if (w < 0)
                            w = 0;

                        double amount = (w / totalWeight) * totalAmount;
                        com.google.gson.JsonObject p = new com.google.gson.JsonObject();
                        p.addProperty("userId", m.id);
                        p.addProperty("amount", amount);
                        // Save weight info if backend supported, but for now just amount
                        participantsArray.add(p);
                    }
                }
            } else if ("EXACT".equals(selectedSplitType)) {
                double sumExact = 0;
                for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                    if (selectedParticipants.get(m.id)) {
                        android.widget.EditText input = participantInputViews.get(m.id);
                        double amount = 0;
                        if (input != null && input.getText().length() > 0) {
                            try {
                                amount = Double.parseDouble(input.getText().toString());
                            } catch (Exception e) {
                            }
                        }
                        sumExact += amount;

                        com.google.gson.JsonObject p = new com.google.gson.JsonObject();
                        p.addProperty("userId", m.id);
                        p.addProperty("amount", amount);
                        participantsArray.add(p);
                    }
                }
                // Validation
                if (Math.abs(sumExact - totalAmount) > 0.05) {
                    android.widget.Toast.makeText(getContext(), "金额不匹配: 输入总和 " + sumExact + " != 总支出 " + totalAmount,
                            android.widget.Toast.LENGTH_LONG).show();
                    return; // Stop
                }
            }

            json.add("participants", participantsArray);

            // Payer
            if (selectedPayerId != null) {
                json.addProperty("payerId", selectedPayerId);
            } else {
                com.example.roomiesplit.utils.SessionManager payerSession = new com.example.roomiesplit.utils.SessionManager(
                        getContext());
                json.addProperty("payerId", payerSession.getUserId());
            }

            com.example.roomiesplit.utils.SessionManager sess = new com.example.roomiesplit.utils.SessionManager(
                    getContext());
            Long userId = sess.getUserId();
            Long ledgerId = sess.getCurrentLedgerId();

            if (ledgerId == null) {
                android.widget.Toast.makeText(getContext(), "未选择账本", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            createTransaction(userId, ledgerId, json, view);
        });

        return view;
    }

    // Category Buttons
    // Category Buttons
    private TextView btnCatFood, btnCatTransport, btnCatShopping, btnCatFun, btnCatOther;

    private void setupCategorySelection(View view) {
        btnCatFood = view.findViewById(R.id.btn_cat_food);
        btnCatTransport = view.findViewById(R.id.btn_cat_transport);
        btnCatShopping = view.findViewById(R.id.btn_cat_shopping);
        btnCatFun = view.findViewById(R.id.btn_cat_fun);
        btnCatOther = view.findViewById(R.id.btn_cat_other);

        View.OnClickListener l = v -> {
            if (v == btnCatFood)
                selectedCategory = "餐饮";
            else if (v == btnCatTransport)
                selectedCategory = "交通";
            else if (v == btnCatShopping)
                selectedCategory = "购物";
            else if (v == btnCatFun)
                selectedCategory = "娱乐";
            else if (v == btnCatOther)
                selectedCategory = "其他";
            updateCategoryUI();
        };

        if (btnCatFood != null)
            btnCatFood.setOnClickListener(l);
        if (btnCatTransport != null)
            btnCatTransport.setOnClickListener(l);
        if (btnCatShopping != null)
            btnCatShopping.setOnClickListener(l);
        if (btnCatFun != null)
            btnCatFun.setOnClickListener(l);
        if (btnCatOther != null)
            btnCatOther.setOnClickListener(l);

        updateCategoryUI();
    }

    private void updateCategoryUI() {
        if (btnCatFood == null)
            return;
        int activeColor = 0xFF4CAF50; // Green
        int inactiveColor = 0xFFF1F5F9; // Gray
        int activeText = 0xFFFFFFFF;
        int inactiveText = 0xFF757575;

        setCatStyle(btnCatFood, "餐饮".equals(selectedCategory), activeColor, inactiveColor, activeText, inactiveText);
        setCatStyle(btnCatTransport, "交通".equals(selectedCategory), activeColor, inactiveColor, activeText,
                inactiveText);
        setCatStyle(btnCatShopping, "购物".equals(selectedCategory), activeColor, inactiveColor, activeText,
                inactiveText);
        setCatStyle(btnCatFun, "娱乐".equals(selectedCategory), activeColor, inactiveColor, activeText, inactiveText);
        setCatStyle(btnCatOther, "其他".equals(selectedCategory), activeColor, inactiveColor, activeText, inactiveText);
    }

    private void setCatStyle(TextView v, boolean active, int ac, int ic, int at, int it) {
        if (v == null)
            return;
        v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active ? ac : ic));
        v.setTextColor(active ? at : it);
        v.setClickable(true); // 确保可点击
    }

    private void setupSplitTypeSelection(View view) {
        // Need IDs for split buttons. XML showed a LinearLayout with 3 TextViews.
        // Let's assign IDs in XML step first.
        // But I have to compile this file now. I will use findViewById assuming IDs
        // exist: btn_split_equal, btn_split_weight, btn_split_exact
        btnSplitEqual = view.findViewById(R.id.btn_split_equal);
        btnSplitWeight = view.findViewById(R.id.btn_split_weight);
        btnSplitExact = view.findViewById(R.id.btn_split_exact);

        View.OnClickListener l = v -> {
            if (v == btnSplitEqual)
                selectedSplitType = "EQUAL";
            else if (v == btnSplitWeight)
                selectedSplitType = "WEIGHT"; // or PERCENT
            else if (v == btnSplitExact)
                selectedSplitType = "EXACT";
            updateSplitTypeUI();
            populateParticipants(); // Re-render list with correct inputs
        };

        if (btnSplitEqual != null)
            btnSplitEqual.setOnClickListener(l);
        if (btnSplitWeight != null)
            btnSplitWeight.setOnClickListener(l);
        if (btnSplitExact != null)
            btnSplitExact.setOnClickListener(l);

        updateSplitTypeUI();
    }

    private void updateSplitTypeUI() {
        if (btnSplitEqual == null)
            return;

        int activeColor = 0xFFFFFFFF; // White
        int inactiveColor = 0xFFF1F5F9; // Light Gray
        int activeText = 0xFF000000;
        int inactiveText = 0xFF757575;

        btnSplitEqual.setBackgroundColor("EQUAL".equals(selectedSplitType) ? activeColor : inactiveColor);
        btnSplitEqual.setTextColor("EQUAL".equals(selectedSplitType) ? activeText : inactiveText);

        btnSplitWeight.setBackgroundColor("WEIGHT".equals(selectedSplitType) ? activeColor : inactiveColor);
        btnSplitWeight.setTextColor("WEIGHT".equals(selectedSplitType) ? activeText : inactiveText);

        btnSplitExact.setBackgroundColor("EXACT".equals(selectedSplitType) ? activeColor : inactiveColor);
        btnSplitExact.setTextColor("EXACT".equals(selectedSplitType) ? activeText : inactiveText);
    }

    private void updateDateUI() {
        if (dateText != null) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MM月dd日");
            dateText.setText(selectedDate.format(formatter));
        }
    }

    private void updatePayerUI() {
        if (payerText != null) {
            payerText.setText(selectedPayerName != null ? selectedPayerName : "选择垫付人");
        }
    }

    private void showDatePicker() {
        // Use user's current time to set default selection if needed, but for selection
        // output we need safe handling
        long today = com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds();

        // If we have a selectedDate, try to focus it.
        // Note: selectedDate is LocalDate (system default zone). We need to convert to
        // UTC millis for the picker.
        long selection = today;
        if (selectedDate != null) {
            selection = selectedDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
        }

        com.google.android.material.datepicker.MaterialDatePicker<Long> picker = com.google.android.material.datepicker.MaterialDatePicker.Builder
                .datePicker()
                .setSelection(selection)
                .build();

        picker.addOnPositiveButtonClickListener(s -> {
            // MaterialDatePicker returns UTC milliseconds for the start of the day.
            // When we convert this back to LocalDate, we MUST use UTC to retrieve the
            // correct calendar date
            // regardless of the user's local timezone.
            selectedDate = java.time.Instant.ofEpochMilli(s).atZone(java.time.ZoneId.of("UTC")).toLocalDate();
            updateDateUI();
        });
        picker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void showPayerSelectionDialog() {
        if (members.isEmpty()) {
            android.widget.Toast.makeText(getContext(), "加载成员中...", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[members.size()];
        for (int i = 0; i < members.size(); i++) {
            names[i] = members.get(i).name;
        }

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle("选择垫付人")
                .setItems(names, (dialog, which) -> {
                    com.example.roomiesplit.ui.DashboardFragment.LedgerMember m = members.get(which);
                    selectedPayerId = m.id;
                    selectedPayerName = m.name;
                    updatePayerUI();
                })
                .show();
    }

    // Map to track checked state and UI elements for updates
    private java.util.Map<Long, Boolean> selectedParticipants = new java.util.HashMap<>();
    private java.util.Map<Long, android.widget.TextView> participantsAmountViews = new java.util.HashMap<>();

    private void loadLedgerMembers() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long userId = session.getUserId();
        Long ledgerId = session.getCurrentLedgerId();

        if (ledgerId == null) {
            android.widget.Toast.makeText(getContext(), "未选择账本，无法加载成员", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.roomiesplit.network.RetrofitClient.getApiService().getLedgerDetail(userId, ledgerId)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        try {
                            if (response.isSuccessful() && response.body() != null) {
                                com.google.gson.JsonObject body = response.body();
                                if (body.has("code") && body.get("code").getAsInt() == 200) {
                                    com.google.gson.JsonObject data = body.getAsJsonObject("data");

                                    if (data != null) {
                                        members.clear();

                                        // Parse Members
                                        if (data.has("members")) {
                                            com.google.gson.JsonArray mems = data.getAsJsonArray("members");
                                            for (int i = 0; i < mems.size(); i++) {
                                                com.google.gson.JsonObject m = mems.get(i).getAsJsonObject();
                                                Long uId = m.get("userId").getAsLong();
                                                String dName = m.has("displayName")
                                                        && !m.get("displayName").isJsonNull()
                                                                ? m.get("displayName").getAsString()
                                                                : "User " + uId;
                                                members.add(
                                                        new com.example.roomiesplit.ui.DashboardFragment.LedgerMember(
                                                                uId,
                                                                dName));
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            android.widget.Toast.makeText(getContext(), "加载成员出错: " + e.getMessage(),
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }

                        // Fallback & UI Update
                        if (members.isEmpty()) {
                            // If empty, it might mean failure or really no members.
                            // Warn user if it seems like a failure (api ok but no data?)
                            // But usually fallback for "Myself"
                            String myName = session.getUsername();
                            members.add(new com.example.roomiesplit.ui.DashboardFragment.LedgerMember(userId,
                                    myName));
                            android.widget.Toast
                                    .makeText(getContext(), "未加载到成员，仅显示自己", android.widget.Toast.LENGTH_SHORT).show();
                        }

                        // Set default payer
                        boolean payerSet = false;
                        if (selectedPayerId != null) {
                            for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                                if (m.id.equals(selectedPayerId)) {
                                    selectedPayerName = m.name;
                                    payerSet = true;
                                    break;
                                }
                            }
                        }
                        if (!payerSet) {
                            selectedPayerId = userId;
                            selectedPayerName = session.getUsername();
                            // Check if I am in members, if so use that name
                            for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
                                if (m.id.equals(userId)) {
                                    selectedPayerName = m.name;
                                    break;
                                }
                            }
                        }
                        if (payerText != null)
                            payerText.setText(selectedPayerName);

                        // Populate Participants List
                        populateParticipants();
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        android.widget.Toast.makeText(getContext(), "加载成员失败: 网络错误", android.widget.Toast.LENGTH_SHORT)
                                .show();
                        // Fallback
                        String myName = session.getUsername();
                        members.add(new com.example.roomiesplit.ui.DashboardFragment.LedgerMember(userId, myName));
                        populateParticipants();
                        updatePayerUI();
                    }
                });
    }

    private void populateParticipants() {
        android.widget.LinearLayout container = getView().findViewById(R.id.container_participants);
        if (container == null)
            return;
        container.removeAllViews();
        // Remove old references
        // We only clear participantsAmountViews if we are fully rebuilding.
        // Actually for dynamic inputs we need to track them.
        participantInputViews.clear();
        participantsAmountViews.clear();

        boolean showInputs = "WEIGHT".equals(selectedSplitType) || "EXACT".equals(selectedSplitType);

        for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
            // Default selected if not in map
            if (!selectedParticipants.containsKey(m.id)) {
                selectedParticipants.put(m.id, true);
            }

            // Row
            android.widget.LinearLayout row = new android.widget.LinearLayout(getContext());
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setPadding(32, 24, 32, 24);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Checkbox
            android.widget.CheckBox checkBox = new android.widget.CheckBox(getContext());
            checkBox.setChecked(selectedParticipants.get(m.id));
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                selectedParticipants.put(m.id, isChecked);
                updateSplits();
            });
            row.addView(checkBox);

            // Name
            android.widget.TextView nameView = new android.widget.TextView(getContext());
            nameView.setText(m.name);
            nameView.setTextSize(16);
            nameView.setTextColor(android.graphics.Color.BLACK);
            nameView.setPadding(24, 0, 0, 0);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(0,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            nameView.setLayoutParams(params);
            row.addView(nameView);

            // Input or Result Text
            if (showInputs) {
                android.widget.EditText input = new android.widget.EditText(getContext());
                input.setHint(selectedSplitType.equals("WEIGHT") ? "1" : "0.00");
                input.setInputType(
                        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                input.setWidth(200);
                input.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                input.setText(selectedSplitType.equals("WEIGHT") ? "1" : "");

                // Add listener to recalculate
                input.addTextChangedListener(new android.text.TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    public void afterTextChanged(android.text.Editable s) {
                        // Debounce? No, just straightforward.
                    }
                });

                participantInputViews.put(m.id, input);
                row.addView(input);
            } else {
                android.widget.TextView amountView = new android.widget.TextView(getContext());
                amountView.setText("¥0.00");
                amountView.setTextColor(android.graphics.Color.GRAY);
                participantsAmountViews.put(m.id, amountView);
                row.addView(amountView);
            }
            container.addView(row);
        }

        updateSplits();
    }

    private void updateSplits() {
        // Only for EQUAL mode really needs visual update of "Result Text".
        // For others, user types in.
        if (!"EQUAL".equals(selectedSplitType))
            return;

        android.widget.EditText amountInput = getView().findViewById(R.id.input_amount);
        if (amountInput == null)
            return;

        double totalAmount = 0;
        try {
            String s = amountInput.getText().toString();
            if (!s.isEmpty())
                totalAmount = Double.parseDouble(s);
        } catch (NumberFormatException e) {
        }

        long selectedCount = 0;
        for (Boolean b : selectedParticipants.values()) {
            if (b)
                selectedCount++;
        }

        double perPerson = selectedCount > 0 ? totalAmount / selectedCount : 0;

        for (com.example.roomiesplit.ui.DashboardFragment.LedgerMember m : members) {
            android.widget.TextView tv = participantsAmountViews.get(m.id);
            if (tv != null) {
                if (selectedParticipants.get(m.id)) {
                    tv.setText(String.format("应付: ¥%.2f", perPerson));
                    tv.setVisibility(View.VISIBLE);
                } else {
                    tv.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    // --- Smart Entry Logic ---
    private void parseSmartEntry(String raw, View view) {
        String processing = raw.trim();
        String amountStr = "";
        String descStr = "";

        // 0. Extract Date (e.g. "昨晚", "今天", "10号")
        if (containsAny(processing, "昨")) {
            selectedDate = java.time.LocalDate.now().minusDays(1);
            updateDateUI();
            processing = processing.replace("昨晚", "").replace("昨天", "");
        } else if (containsAny(processing, "前天")) {
            selectedDate = java.time.LocalDate.now().minusDays(2);
            updateDateUI();
            processing = processing.replace("前天", "");
        } else {
            // Try to find "N号" or "N日"
            java.util.regex.Pattern datePattern = java.util.regex.Pattern.compile("(\\d+)[号日]");
            java.util.regex.Matcher dm = datePattern.matcher(processing);
            if (dm.find()) {
                try {
                    int day = Integer.parseInt(dm.group(1));
                    java.time.LocalDate today = java.time.LocalDate.now();
                    // If day > today's day, assume last month? Or just current month?
                    // Let's assume current month for simplicity, unless it's in future then last
                    // month.
                    java.time.LocalDate date = today.withDayOfMonth(day);
                    if (date.isAfter(today)) {
                        date = date.minusMonths(1);
                    }
                    selectedDate = date;
                    updateDateUI();
                    processing = processing.replace(dm.group(0), "");
                } catch (Exception e) {
                }
            }
        }

        // 1. Extract Amount - Improved to look for "元/块" prefix/suffix
        // Regex: Look for number followed by optional 元/块
        // Or "花了" followed by number

        java.util.regex.Pattern explicitMoney = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)\\s*[元块钱]");
        java.util.regex.Matcher mm = explicitMoney.matcher(processing);

        if (mm.find()) {
            amountStr = mm.group(1);
            processing = processing.replace(mm.group(0), " ");
        } else {
            // Fallback to "spent X"
            java.util.regex.Pattern spentMoney = java.util.regex.Pattern.compile("花了\\s*(\\d+(\\.\\d+)?)");
            java.util.regex.Matcher sm = spentMoney.matcher(processing);
            if (sm.find()) {
                amountStr = sm.group(1);
                processing = processing.replace(sm.group(0), " ");
            } else {
                // Fallback to max number heuristic
                java.util.regex.Pattern anyNum = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)");
                java.util.regex.Matcher am = anyNum.matcher(processing);
                double maxVal = -1;
                String bestMatch = "";
                while (am.find()) {
                    String valStr = am.group(1);
                    try {
                        double val = Double.parseDouble(valStr);
                        if (val > maxVal) {
                            maxVal = val;
                            bestMatch = valStr;
                        }
                    } catch (Exception e) {
                    }
                }
                if (maxVal != -1) {
                    amountStr = bestMatch;
                    processing = processing.replace(amountStr, " ");
                }
            }
        }

        // 2. Extract Split Mode
        if (containsAny(processing, "AA", "均分", "平分", "人均")) {
            selectedSplitType = "EQUAL";
            updateSplitTypeUI();
        }

        // 3. Extract Category
        String cat = "其他"; // Default to Other now if no match
        if (containsAny(processing, "吃", "饭", "餐", "面", "粉", "喝", "这顿", "饿", "酒", "串", "海底捞"))
            cat = "餐饮";
        else if (containsAny(processing, "车", "路", "油", "铁", "机", "票", "滴滴", "打车"))
            cat = "交通";
        else if (containsAny(processing, "买", "购", "市", "店", "菜", "果", "某宝", "京东", "耐克", "衣服"))
            cat = "购物";
        else if (containsAny(processing, "玩", "乐", "影", "KTV", "唱", "游", "戏", "票", "网咖", "剧本杀"))
            cat = "娱乐";

        selectedCategory = cat;
        updateCategoryUI();

        // 4. Clean Description
        // Remove common stopwords for billing
        String[] stopWords = { "花了", "花费", "共计", "总共", "块", "元", "钱", "使用了", "合计", "，", ",", "。" };
        for (String sw : stopWords) {
            processing = processing.replace(sw, " ");
        }
        descStr = processing.trim().replaceAll("\\s+", " "); // Collapse spaces

        // Set UI
        if (!amountStr.isEmpty()) {
            EditText amountInput = view.findViewById(R.id.input_amount);
            amountInput.setText(amountStr);
        }
        if (!descStr.isEmpty()) {
            EditText descInput = view.findViewById(R.id.input_description);
            descInput.setText(descStr);
        }

        android.widget.Toast
                .makeText(getContext(), String.format("已识别: ￥%s - %s [%s]", amountStr, cat, selectedSplitType),
                        android.widget.Toast.LENGTH_LONG)
                .show();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k))
                return true;
        }
        return false;
    }

    private void createTransaction(Long userId, Long ledgerId, com.google.gson.JsonObject transactionJson, View view) {
        android.util.Log.d("AddBillFragment", "Creating transaction: " + transactionJson.toString());

        com.example.roomiesplit.network.RetrofitClient.getApiService()
                .createTransaction(userId, ledgerId, transactionJson)
                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                            retrofit2.Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            android.util.Log.d("AddBillFragment", "Transaction created successfully");
                            android.widget.Toast.makeText(getContext(), "记账成功",
                                    android.widget.Toast.LENGTH_SHORT).show();
                            Navigation.findNavController(view).popBackStack();
                        } else {
                            String errorMsg = "Failed: " + response.code();
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    android.util.Log.e("AddBillFragment", "Error response: " + errorBody);
                                    errorMsg += " - " + errorBody;
                                }
                            } catch (Exception e) {
                                android.util.Log.e("AddBillFragment", "Error reading error body", e);
                            }
                            android.widget.Toast
                                    .makeText(getContext(), errorMsg,
                                            android.widget.Toast.LENGTH_LONG)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                            Throwable t) {
                        android.util.Log.e("AddBillFragment", "Network error", t);
                        android.widget.Toast.makeText(getContext(), "Network Error: " + t.getMessage(),
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
    }
}
