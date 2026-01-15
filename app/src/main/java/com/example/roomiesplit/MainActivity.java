package com.example.roomiesplit;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            bottomNavigationView = findViewById(R.id.bottom_nav_view);

            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();
                if (id == R.id.navigation_dashboard ||
                        id == R.id.navigation_calendar ||
                        id == R.id.navigation_debt ||
                        id == R.id.navigation_messages ||
                        id == R.id.navigation_profile) {
                    bottomNavigationView.setVisibility(View.VISIBLE);
                } else {
                    bottomNavigationView.setVisibility(View.GONE);
                }
            });
        }

        // Notification Logic
        checkNotificationPermission();
        startNotificationPolling();
    }

    private android.os.Handler pollingHandler = new android.os.Handler();
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL = 2000; // 2 seconds
    private com.example.roomiesplit.utils.NotificationHelper notificationHelper;
    private long lastNotifiedId = 0;

    // Permission Request
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private void checkNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure polling is running
        if (pollingRunnable == null)
            startNotificationPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNotificationPolling();
    }

    private void startNotificationPolling() {
        if (pollingRunnable != null)
            return; // Already running

        notificationHelper = new com.example.roomiesplit.utils.NotificationHelper(this);

        // Initialize lastNotifiedId from Prefs to avoid blasting old notifs on restart
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        lastNotifiedId = prefs.getLong("last_notified_id", 0);

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollNotifications();
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void stopNotificationPolling() {
        if (pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingRunnable = null;
        }
    }

    private void pollNotifications() {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(this);
        Long userId = session.getUserId();
        if (userId == null || userId == -1)
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
                                long maxId = lastNotifiedId;
                                boolean hasNew = false;
                                int unreadCount = 0;

                                for (int i = 0; i < data.size(); i++) {
                                    com.google.gson.JsonObject notif = data.get(i).getAsJsonObject();
                                    long id = notif.get("id").getAsLong();
                                    boolean isRead = notif.has("isRead") && notif.get("isRead").getAsBoolean();

                                    if (!isRead) {
                                        unreadCount++; // Count unread
                                    }

                                    if (!isRead && id > lastNotifiedId) {
                                        // Found a new unread notification
                                        String title = notif.has("title") ? notif.get("title").getAsString()
                                                : "New Message";
                                        String message = notif.has("message") ? notif.get("message").getAsString() : "";

                                        notificationHelper.sendNotification((int) id, title, message);

                                        if (id > maxId)
                                            maxId = id;
                                        hasNew = true;
                                    } else if (!isRead && id > maxId) {
                                        // Just tracking max ID of unread stuff even if handled before?
                                        // Actually we only care about if ID > lastNotifiedId.
                                        // If we restart app, lastNotifiedId is loaded.
                                        if (id > maxId)
                                            maxId = id;
                                    }
                                }

                                // Update Badge on UI Thread
                                final int finalUnread = unreadCount;
                                runOnUiThread(() -> {
                                    if (bottomNavigationView != null) {
                                        com.google.android.material.badge.BadgeDrawable badge = bottomNavigationView
                                                .getOrCreateBadge(R.id.navigation_messages);
                                        if (finalUnread > 0) {
                                            badge.setVisible(true);
                                            // badge.setNumber(finalUnread); // Remove this to show only dot as
                                            // requested
                                        } else {
                                            badge.setVisible(false);
                                        }
                                    }
                                });

                                if (maxId > lastNotifiedId) {
                                    lastNotifiedId = maxId;
                                    android.content.SharedPreferences prefs = getSharedPreferences("app_prefs",
                                            MODE_PRIVATE);
                                    prefs.edit().putLong("last_notified_id", lastNotifiedId).apply();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                        // Silent fail
                    }
                });
    }
}
