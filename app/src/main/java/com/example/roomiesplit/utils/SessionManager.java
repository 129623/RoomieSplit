package com.example.roomiesplit.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "RoomieSplitSession";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void createLoginSession(String token, Long userId, String username) {
        editor.putString(KEY_TOKEN, token);
        editor.putLong(KEY_USER_ID, userId);
        editor.putString(KEY_USERNAME, username);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.contains(KEY_TOKEN);
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public Long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "User");
    }

    private static final String KEY_CURRENT_LEDGER_ID = "current_ledger_id";

    public void saveCurrentLedgerId(Long ledgerId) {
        editor.putLong(KEY_CURRENT_LEDGER_ID, ledgerId);
        editor.apply();
    }

    public Long getCurrentLedgerId() {
        long id = sharedPreferences.getLong(KEY_CURRENT_LEDGER_ID, -1);
        return id == -1 ? null : id;
    }

    public void logout() {
        editor.clear();
        editor.apply();
    }
}
