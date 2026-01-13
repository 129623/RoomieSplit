package com.example.roomiesplit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.roomiesplit.R;
import com.example.roomiesplit.network.ApiService;
import com.example.roomiesplit.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.text.DecimalFormat;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PREF_NAME = "RoomieSplitPrefs";

    private CardView cardAvatar;
    private ImageView imageAvatar;
    private TextView textAvatarChar;
    private TextView textUsername;
    private TextView textAdvancedAmount;
    private TextView textPaidGrowth;
    private TextView textExpenseAmount;
    private MaterialButton btnLogout;
    private MaterialButton btnExit;

    private ApiService apiService;
    private Long currentUserId;
    private Long currentLedgerId;
    private String currentAvatarUrl;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Default avatar options (simple colored backgrounds with initials)
    private static final String[] DEFAULT_AVATARS = {
            "avatar_blue",
            "avatar_green",
            "avatar_purple",
            "avatar_orange",
            "avatar_pink"
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            String uriString = imageUri.toString();
                            Log.d(TAG, "Image selected: " + uriString);

                            // Optimistic update
                            Glide.with(ProfileFragment.this)
                                    .load(uriString)
                                    .circleCrop()
                                    .into(imageAvatar);
                            imageAvatar.setVisibility(View.VISIBLE);
                            textAvatarChar.setVisibility(View.GONE);

                            // Update server
                            updateAvatarOnServer(uriString);
                        }
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        initializeViews(view);
        setupApiService();
        loadUserData();
        setupClickListeners();

        return view;
    }

    private void initializeViews(View view) {
        cardAvatar = view.findViewById(R.id.card_avatar);
        imageAvatar = view.findViewById(R.id.image_avatar);
        textAvatarChar = view.findViewById(R.id.text_avatar_char);
        textUsername = view.findViewById(R.id.text_username);
        textAdvancedAmount = view.findViewById(R.id.text_advanced_amount);
        textPaidGrowth = view.findViewById(R.id.text_paid_growth);
        textExpenseAmount = view.findViewById(R.id.text_expense_amount);
        btnLogout = view.findViewById(R.id.btn_logout);
        btnExit = view.findViewById(R.id.btn_exit);
    }

    private void setupApiService() {
        apiService = RetrofitClient.getInstance().create(ApiService.class);

        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                requireContext());
        currentUserId = session.getUserId();

        Long lId = session.getCurrentLedgerId();
        currentLedgerId = lId != null ? lId : -1;

        if (currentUserId == -1) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // Avatar click - show selection dialog
        cardAvatar.setOnClickListener(v -> showAvatarSelectionDialog());

        // Logout button
        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("退出登录")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("确定", (dialog, which) -> performLogout())
                    .setNegativeButton("取消", null)
                    .show();
        });

        // Exit button
        btnExit.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("退出应用")
                    .setMessage("确定要退出应用吗？")
                    .setPositiveButton("确定", (dialog, which) -> requireActivity().finish())
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void loadUserData() {
        if (currentUserId == -1)
            return;

        // Load user profile
        apiService.getUserProfile(currentUserId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.get("code").getAsInt() == 200) {
                        JsonObject userData = result.getAsJsonObject("data");
                        updateUserProfile(userData);
                    }
                } else {
                    Log.e(TAG, "Failed to load user profile: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading user profile", t);
                Toast.makeText(requireContext(), "加载用户信息失败", Toast.LENGTH_SHORT).show();
            }
        });

        // Load statistics if ledger is selected
        if (currentLedgerId != -1) {
            loadStatistics();
        }
    }

    private void updateUserProfile(JsonObject userData) {
        String username = userData.has("username") ? userData.get("username").getAsString() : "未知用户";
        String displayName = userData.has("displayName") && !userData.get("displayName").isJsonNull()
                ? userData.get("displayName").getAsString()
                : username;
        currentAvatarUrl = userData.has("avatarUrl") && !userData.get("avatarUrl").isJsonNull()
                ? userData.get("avatarUrl").getAsString()
                : null;

        textUsername.setText(displayName);

        // Update avatar
        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            // Check if it's a URL or default avatar identifier
            // Check if it's a default avatar identifier
            if (currentAvatarUrl.startsWith("avatar_")) {
                // Default avatar - show colored background with initial
                displayDefaultAvatar(currentAvatarUrl, displayName);
            } else {
                // It's a URL or URI (http, content, file, etc.) - Try to load with Glide
                textAvatarChar.setVisibility(View.GONE);
                imageAvatar.setVisibility(View.VISIBLE);

                Glide.with(this)
                        .load(currentAvatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background) // Fallback image if load fails
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e,
                                    Object model,
                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    boolean isFirstResource) {
                                // If loading fails, revert to initial avatar
                                imageAvatar.post(() -> displayInitialAvatar(displayName));
                                return true; // Consume the event
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model,
                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target,
                                    com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(imageAvatar);
            }
        } else {
            // No avatar, show initial
            displayInitialAvatar(displayName);
        }
    }

    private void displayDefaultAvatar(String avatarType, String name) {
        imageAvatar.setVisibility(View.GONE);
        textAvatarChar.setVisibility(View.VISIBLE);

        // Set background color based on avatar type
        int color;
        switch (avatarType) {
            case "avatar_blue":
                color = Color.parseColor("#4285F4");
                break;
            case "avatar_green":
                color = Color.parseColor("#34A853");
                break;
            case "avatar_purple":
                color = Color.parseColor("#9C27B0");
                break;
            case "avatar_orange":
                color = Color.parseColor("#FF9800");
                break;
            case "avatar_pink":
                color = Color.parseColor("#E91E63");
                break;
            default:
                color = Color.parseColor("#607D8B");
        }

        cardAvatar.setCardBackgroundColor(color);
        textAvatarChar.setText(name.substring(0, 1).toUpperCase());
    }

    private void displayInitialAvatar(String name) {
        imageAvatar.setVisibility(View.GONE);
        textAvatarChar.setVisibility(View.VISIBLE);

        // Generate color based on name hash
        int hash = name.hashCode();
        int color = Color.HSVToColor(new float[] {
                Math.abs(hash % 360),
                0.6f,
                0.7f
        });

        cardAvatar.setCardBackgroundColor(color);
        textAvatarChar.setText(name.substring(0, 1).toUpperCase());
    }

    private void loadStatistics() {
        if (currentLedgerId == -1) {
            textPaidGrowth.setText("未选择账本");
            return;
        }

        // Fetch transactions to calculate statistics locally
        // because ledger detail member list might not contain real-time balance
        apiService.getTransactions(currentLedgerId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.get("code").getAsInt() == 200) {
                        JsonArray transactions = result.getAsJsonArray("data");
                        calculateAndDisplayStatistics(transactions);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading statistics", t);
                textPaidGrowth.setText("加载失败");
            }
        });
    }

    private void calculateAndDisplayStatistics(JsonArray transactions) {
        DecimalFormat df = new DecimalFormat("#,##0.00");

        double totalAdvanced = 0.0;
        double totalExpense = 0.0;
        boolean foundActivities = false;

        if (transactions != null) {
            for (int i = 0; i < transactions.size(); i++) {
                JsonObject tx = transactions.get(i).getAsJsonObject();
                long payerId = tx.has("payerId") ? tx.get("payerId").getAsLong() : -1;
                double amount = tx.has("amount") ? tx.get("amount").getAsDouble() : 0.0;

                // 1. Calculate Advanced (Money I paid)
                if (payerId == currentUserId) {
                    totalAdvanced += amount;
                    foundActivities = true;
                }

                // 2. Calculate Expense (My share)
                if (tx.has("participants")) {
                    JsonArray participants = tx.getAsJsonArray("participants");
                    for (int j = 0; j < participants.size(); j++) {
                        JsonObject p = participants.get(j).getAsJsonObject();
                        long pUserId = p.has("userId") ? p.get("userId").getAsLong() : -1;

                        if (pUserId == currentUserId) {
                            double share = p.has("owingAmount") ? p.get("owingAmount").getAsDouble() : 0.0;
                            totalExpense += share;
                            foundActivities = true;
                        }
                    }
                }
            }
        }

        textAdvancedAmount.setText("¥" + df.format(totalAdvanced));
        textExpenseAmount.setText("¥" + df.format(totalExpense));

        if (!foundActivities) {
            // Maybe user just joined and has no activities yet
            textPaidGrowth.setText("-- vs 上月");
        } else {
            textPaidGrowth.setText("-- vs 上月");
        }
    }

    private void showAvatarSelectionDialog() {
        String[] options = {
                "选择默认头像",
                "从相册选择",
                "取消"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("更换头像")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showDefaultAvatarSelection();
                            break;
                        case 1:
                            openImagePicker();
                            break;
                        case 2:
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void showDefaultAvatarSelection() {
        String[] avatarNames = { "蓝色", "绿色", "紫色", "橙色", "粉色" };

        new AlertDialog.Builder(requireContext())
                .setTitle("选择默认头像")
                .setItems(avatarNames, (dialog, which) -> {
                    String selectedAvatar = DEFAULT_AVATARS[which];

                    // Optimistic update
                    String displayName = textUsername.getText().toString();
                    displayDefaultAvatar(selectedAvatar, displayName);

                    updateAvatarOnServer(selectedAvatar);
                })
                .show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void updateAvatarOnServer(String avatarUrl) {
        if (currentUserId == -1)
            return;

        JsonObject request = new JsonObject();
        request.addProperty("avatarUrl", avatarUrl);

        Log.d(TAG, "Updating avatar on server: " + avatarUrl);

        apiService.updateUserProfile(currentUserId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                Log.d(TAG, "Update response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    Log.d(TAG, "Update result body: " + result.toString());

                    if (result.get("code").getAsInt() == 200) {
                        Toast.makeText(requireContext(), "头像保存成功", Toast.LENGTH_SHORT).show();
                        currentAvatarUrl = avatarUrl;
                        // UI already updated optimistically
                    } else {
                        String msg = result.has("msg") ? result.get("msg").getAsString() : "未知错误";
                        Toast.makeText(requireContext(), "保存失败: " + msg, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Update failed: " + msg);
                    }
                } else {
                    Toast.makeText(requireContext(), "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Server error: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error updating avatar", t);
                Toast.makeText(requireContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performLogout() {
        // Clear session
        new com.example.roomiesplit.utils.SessionManager(requireContext()).logout();

        // Navigate to login screen
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show();
        requireActivity().finish();
    }
}
