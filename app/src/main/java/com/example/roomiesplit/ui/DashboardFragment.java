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

public class DashboardFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

                view.findViewById(R.id.fab_add_bill).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_global_addBillFragment));

                view.findViewById(R.id.text_room_name).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_global_ledgerFragment));

                view.findViewById(R.id.btn_invite_roommate).setOnClickListener(v -> {
                        new InviteRoommateDialogFragment().show(getParentFragmentManager(), "InviteDialog");
                });

                view.findViewById(R.id.card_decision_entry).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_dashboard_to_decision));

                view.findViewById(R.id.card_karma_entry).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_dashboard_to_karma));

                view.findViewById(R.id.btn_open_report).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_dashboard_to_report));

                return view;
        }

        private android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        private Runnable statusPoller = new Runnable() {
                @Override
                public void run() {
                        if (getView() != null) {
                                com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                                                getContext());
                                Long ledgerId = session.getCurrentLedgerId();
                                Long userId = session.getUserId();
                                if (ledgerId != null && ledgerId != -1 && userId != -1) {
                                        loadDashboardStats(userId, ledgerId, getView());
                                }
                                handler.postDelayed(this, 3000); // Poll every 3 seconds
                        }
                }
        };

        @Override
        public void onResume() {
                super.onResume();
                if (getView() != null) {
                        loadMyLedgers(getView());
                }
                handler.post(statusPoller);
        }

        @Override
        public void onPause() {
                super.onPause();
                handler.removeCallbacks(statusPoller);
        }

        private com.google.gson.JsonArray myLedgers; // Store loaded ledgers

        private void loadMyLedgers(View view) {
                com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                                getContext());
                Long userId = session.getUserId();

                if (userId == -1)
                        return;

                com.example.roomiesplit.network.RetrofitClient.getApiService().getMyLedgers(userId)
                                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                        com.google.gson.JsonObject body = response.body();
                                                        if (body.get("code").getAsInt() == 200) {
                                                                com.google.gson.JsonArray data = body
                                                                                .getAsJsonArray("data");
                                                                myLedgers = data; // Save for switching

                                                                if (data != null && data.size() > 0) {
                                                                        // Logic: Try to find saved ID, otherwise use
                                                                        // the LAST one (newest)
                                                                        Long savedIdObj = session.getCurrentLedgerId();
                                                                        long savedId = (savedIdObj != null) ? savedIdObj
                                                                                        : -1;
                                                                        com.google.gson.JsonObject targetLedger = null;

                                                                        // 1. Try saved
                                                                        if (savedId != -1) {
                                                                                for (int i = 0; i < data.size(); i++) {
                                                                                        com.google.gson.JsonObject l = data
                                                                                                        .get(i)
                                                                                                        .getAsJsonObject();
                                                                                        if (l.get("id").getAsLong() == savedId) {
                                                                                                targetLedger = l;
                                                                                                break;
                                                                                        }
                                                                                }
                                                                        }

                                                                        // 2. Fallback to last one (newest ledger
                                                                        // usually)
                                                                        if (targetLedger == null) {
                                                                                targetLedger = data.get(data.size() - 1)
                                                                                                .getAsJsonObject();
                                                                        }

                                                                        switchLedger(targetLedger, view, userId,
                                                                                        session);

                                                                        // Setup click listener for switching
                                                                        view.findViewById(R.id.text_room_name)
                                                                                        .setOnClickListener(
                                                                                                        v -> Navigation.findNavController(
                                                                                                                        view)
                                                                                                                        .navigate(R.id.action_global_ledgerFragment));

                                                                } else {
                                                                        android.widget.TextView roomName = view
                                                                                        .findViewById(R.id.text_room_name);
                                                                        roomName.setText("点击创建账本");
                                                                        session.saveCurrentLedgerId(-1L);
                                                                        view.findViewById(R.id.text_room_name)
                                                                                        .setOnClickListener(
                                                                                                        v -> Navigation.findNavController(
                                                                                                                        view)
                                                                                                                        .navigate(R.id.action_global_ledgerFragment));
                                                                }
                                                        } else {
                                                                android.widget.Toast.makeText(getContext(),
                                                                                "加载账本失败: " + body.get("message")
                                                                                                .getAsString(),
                                                                                android.widget.Toast.LENGTH_SHORT)
                                                                                .show();
                                                        }
                                                } else {
                                                        android.widget.Toast.makeText(getContext(), "加载账本失败: 服务器错误",
                                                                        android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                                android.widget.TextView roomName = view
                                                                .findViewById(R.id.text_room_name);
                                                roomName.setText("网络连接失败");
                                        }
                                });
        }

        private void showSwitchLedgerDialog(View view, Long userId,
                        com.example.roomiesplit.utils.SessionManager session) {
                if (myLedgers == null || myLedgers.size() == 0)
                        return;

                String[] names = new String[myLedgers.size()];
                for (int i = 0; i < myLedgers.size(); i++) {
                        names[i] = myLedgers.get(i).getAsJsonObject().get("name").getAsString();
                }

                new android.app.AlertDialog.Builder(getContext())
                                .setTitle("切换账本")
                                .setItems(names, (dialog, which) -> {
                                        com.google.gson.JsonObject selected = myLedgers.get(which).getAsJsonObject();
                                        switchLedger(selected, view, userId, session);
                                })
                                .setNegativeButton("创建新账本", (d, w) -> {
                                        Navigation.findNavController(view).navigate(R.id.action_global_ledgerFragment);
                                })
                                .show();
        }

        private void switchLedger(com.google.gson.JsonObject ledger, View view, Long userId,
                        com.example.roomiesplit.utils.SessionManager session) {
                String name = ledger.get("name").getAsString();
                Long ledgerId = ledger.get("id").getAsLong();

                android.widget.TextView roomName = view.findViewById(R.id.text_room_name);
                roomName.setText(name);

                session.saveCurrentLedgerId(ledgerId);
                loadDashboardStats(userId, ledgerId, view);
        }

        private void loadDashboardStats(Long userId, Long ledgerId, View view) {
                com.example.roomiesplit.network.RetrofitClient.getApiService().getTransactions(ledgerId)
                                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                        com.google.gson.JsonArray data = response.body()
                                                                        .getAsJsonArray("data");
                                                        if (data != null) {
                                                                double myPaid = 0;
                                                                double myConsumption = 0;

                                                                for (int i = 0; i < data.size(); i++) {
                                                                        com.google.gson.JsonObject tx = data.get(i)
                                                                                        .getAsJsonObject();
                                                                        long payerId = tx.get("payerId").getAsLong();
                                                                        double amount = tx.get("amount").getAsDouble();

                                                                        if (payerId == userId) {
                                                                                myPaid += amount;
                                                                        }

                                                                        if (tx.has("participants")) {
                                                                                com.google.gson.JsonArray participants = tx
                                                                                                .getAsJsonArray("participants");
                                                                                for (int j = 0; j < participants
                                                                                                .size(); j++) {
                                                                                        com.google.gson.JsonObject p = participants
                                                                                                        .get(j)
                                                                                                        .getAsJsonObject();
                                                                                        long pUserId = p.has(
                                                                                                        "userId") ? p.get("userId").getAsLong() : -1;
                                                                                        if (pUserId == userId) {
                                                                                                if (p.has("owingAmount")) {
                                                                                                        myConsumption += p
                                                                                                                        .get("owingAmount")
                                                                                                                        .getAsDouble();
                                                                                                }
                                                                                        }
                                                                                }
                                                                        }
                                                                }
                                                                updateStatsUI(view, myPaid, myConsumption);
                                                        }
                                                }
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                        }
                                });

                com.example.roomiesplit.network.RetrofitClient.getApiService().getLedgerDetail(userId, ledgerId)
                                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                        com.google.gson.JsonObject body = response.body();
                                                        if (body.has("data")) {
                                                                com.google.gson.JsonObject data = body
                                                                                .getAsJsonObject("data");
                                                                if (data.has("members")) {
                                                                        com.google.gson.JsonArray members = data
                                                                                        .getAsJsonArray("members");
                                                                        updateRoommatesUI(view, members, userId,
                                                                                        ledgerId);
                                                                }
                                                        }
                                                }
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                        }
                                });
        }

        private void updateStatsUI(View view, double myPaid, double myConsumption) {
                android.widget.TextView consTv = view.findViewById(R.id.text_my_spend_amount);
                if (consTv != null) {
                        consTv.setText(String.format("¥%.2f", myConsumption));
                }
                android.widget.TextView totalTv = view.findViewById(R.id.text_total_spend);
                if (totalTv != null) {
                        totalTv.setText(String.format("我垫付: ¥%.2f", myPaid));
                }
        }

        private void updateRoommatesUI(View view, com.google.gson.JsonArray members, Long myUserId, Long ledgerId) {
                android.widget.LinearLayout container = view.findViewById(R.id.container_roommates);

                // Find existing Invite Button (could be direct or wrapped)
                View inviteBtn = container.findViewById(R.id.btn_invite_roommate);
                View inviteViewToAdd = null;

                if (inviteBtn != null) {
                        // Check if it's direct child or wrapped
                        if (inviteBtn.getParent() == container) {
                                inviteViewToAdd = inviteBtn;
                        } else if (inviteBtn.getParent() != null && inviteBtn.getParent().getParent() == container) {
                                inviteViewToAdd = (View) inviteBtn.getParent();
                        }
                }

                if (inviteViewToAdd != null) {
                        container.removeView(inviteViewToAdd);
                } else {
                        // Recreate if not found
                        View newBtn = android.view.LayoutInflater.from(getContext())
                                        .inflate(R.layout.item_invite_button, container, false);

                        newBtn.setOnClickListener(v -> {
                                new InviteRoommateDialogFragment().show(getParentFragmentManager(), "InviteDialog");
                        });
                        newBtn.setTag(Long.MAX_VALUE); // Prevent removal
                        inviteViewToAdd = newBtn;
                }

                // Identify current members to handle removals
                java.util.Set<Long> currentMemberIds = new java.util.HashSet<>();
                if (members != null) {
                        for (int i = 0; i < members.size(); i++) {
                                com.google.gson.JsonObject m = members.get(i).getAsJsonObject();
                                if (m.has("userId"))
                                        currentMemberIds.add(m.get("userId").getAsLong());
                        }
                }

                // Remove obsolete member views (Keeping members and the invite button)
                for (int i = container.getChildCount() - 1; i >= 0; i--) {
                        View child = container.getChildAt(i);
                        Object tag = child.getTag();
                        // Remove if it's not a Long, or if it is a Long (Member ID) but not in current
                        // list
                        // Exception: Long.MAX_VALUE is the Invite Button
                        if (!(tag instanceof Long)) {
                                container.removeViewAt(i);
                        } else {
                                Long idTag = (Long) tag;
                                if (!idTag.equals(Long.MAX_VALUE) && !currentMemberIds.contains(idTag)) {
                                        container.removeViewAt(i);
                                }
                        }
                }

                if (members != null) {
                        for (int i = 0; i < members.size(); i++) {
                                com.google.gson.JsonObject m = members.get(i).getAsJsonObject();
                                if (!m.has("userId") || !m.has("displayName"))
                                        continue;

                                Long uId = m.get("userId").getAsLong();
                                String name = m.get("displayName").getAsString();
                                String status = m.has("memberStatus") && !m.get("memberStatus").isJsonNull()
                                                ? m.get("memberStatus").getAsString()
                                                : "AVAILABLE";

                                int color = getStatusColor(status);

                                // Find existing view
                                View item = container.findViewWithTag(uId);
                                boolean isNew = (item == null);

                                if (isNew) {
                                        item = android.view.LayoutInflater.from(getContext())
                                                        .inflate(R.layout.item_roommate_status, container, false);
                                        item.setTag(uId);
                                        container.addView(item); // Simple append

                                        // Initialize Click Listener
                                        item.setOnClickListener(v -> {
                                                if (uId.equals(myUserId)) {
                                                        StatusSelectionDialogFragment dialog = StatusSelectionDialogFragment
                                                                        .newInstance(ledgerId);
                                                        dialog.setListener(() -> loadDashboardStats(myUserId, ledgerId,
                                                                        view));
                                                        dialog.show(getParentFragmentManager(), "StatusSelect");
                                                } else {
                                                        android.widget.Toast.makeText(getContext(),
                                                                        name + ": " + status,
                                                                        android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                        });
                                }

                                // Always update status indicator
                                View bg = item.findViewById(R.id.view_status_indicator);
                                if (bg != null)
                                        bg.setBackgroundColor(color);

                                String avatarUrl = null;
                                if (m.has("avatarUrl") && !m.get("avatarUrl").isJsonNull()) {
                                        avatarUrl = m.get("avatarUrl").getAsString();
                                }

                                // Only load avatar if new (to avoid flicker)
                                if (isNew) {
                                        loadUserAvatar(uId, item.findViewById(R.id.image_avatar),
                                                        item.findViewById(R.id.text_initials),
                                                        item.findViewById(R.id.view_avatar_bg), name, color, avatarUrl);
                                }
                        }
                }

                // Add Invite Button at the end if it's not already added (check parent)
                if (inviteViewToAdd != null && inviteViewToAdd.getParent() != container) {
                        container.addView(inviteViewToAdd);
                }
        }

        private int getStatusColor(String status) {
                if ("BUSY".equals(status))
                        return 0xFFF44336;
                if ("AWAY".equals(status))
                        return 0xFFFF9800;
                if ("ASLEEP".equals(status))
                        return 0xFF9E9E9E;
                return 0xFF4CAF50; // AVAILABLE
        }

        private void loadUserAvatar(Long userId, android.widget.ImageView avatarImg,
                        android.widget.TextView textInitials, View bgView,
                        String displayName, int statusColor, String preloadedAvatarUrl) {
                // First, show initial as fallback
                if (displayName != null && !displayName.isEmpty()) {
                        textInitials.setText(displayName.substring(0, 1).toUpperCase());
                }

                // Optimization: Use preloaded URL if available
                if (preloadedAvatarUrl != null) {
                        displayAvatar(preloadedAvatarUrl, avatarImg, textInitials, bgView);
                        return;
                }

                // Try to load user profile to get avatar
                com.example.roomiesplit.network.RetrofitClient.getApiService().getUserProfile(userId)
                                .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        retrofit2.Response<com.google.gson.JsonObject> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                        com.google.gson.JsonObject result = response.body();
                                                        if (result.get("code").getAsInt() == 200) {
                                                                com.google.gson.JsonObject userData = result
                                                                                .getAsJsonObject("data");
                                                                if (userData.has("avatarUrl") && !userData
                                                                                .get("avatarUrl").isJsonNull()) {
                                                                        String avatarUrl = userData.get("avatarUrl")
                                                                                        .getAsString();
                                                                        displayAvatar(avatarUrl, avatarImg,
                                                                                        textInitials, bgView);
                                                                }
                                                        }
                                                }
                                        }

                                        @Override
                                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                        Throwable t) {
                                                // Just keep showing the initial with default gray background
                                                bgView.setBackgroundColor(0xFF757575);
                                                avatarImg.setVisibility(View.GONE);
                                                textInitials.setVisibility(View.VISIBLE);
                                        }
                                });
        }

        private void displayAvatar(String avatarUrl, android.widget.ImageView avatarImg,
                        android.widget.TextView textInitials, View bgView) {
                if (avatarUrl.startsWith("http")) {
                        // Load image URL using Glide
                        com.bumptech.glide.Glide.with(DashboardFragment.this)
                                        .load(avatarUrl)
                                        .circleCrop()
                                        .dontAnimate() // Prevent crossfade flash
                                        .into(avatarImg);
                        avatarImg.setVisibility(View.VISIBLE);
                        textInitials.setVisibility(View.GONE);
                } else if (avatarUrl.startsWith("avatar_")) {
                        // Default colored avatar - keep showing initial
                        int avatarColor;
                        switch (avatarUrl) {
                                case "avatar_blue":
                                        avatarColor = 0xFF4285F4;
                                        break;
                                case "avatar_green":
                                        avatarColor = 0xFF34A853;
                                        break;
                                case "avatar_purple":
                                        avatarColor = 0xFF9C27B0;
                                        break;
                                case "avatar_orange":
                                        avatarColor = 0xFFFF9800;
                                        break;
                                case "avatar_pink":
                                        avatarColor = 0xFFE91E63;
                                        break;
                                default:
                                        avatarColor = 0xFF757575;
                        }
                        // Override background with avatar color
                        bgView.setBackgroundColor(avatarColor);
                        avatarImg.setVisibility(View.GONE);
                        textInitials.setVisibility(View.VISIBLE);
                }
        }

        public static class LedgerMember {
                public Long id;
                public String name;

                public LedgerMember(Long id, String name) {
                        this.id = id;
                        this.name = name;
                }
        }
}
