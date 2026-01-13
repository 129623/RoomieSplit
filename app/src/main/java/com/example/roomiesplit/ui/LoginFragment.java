package com.example.roomiesplit.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.roomiesplit.R;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        TextInputEditText emailInput = view.findViewById(R.id.input_email);
        TextInputEditText passwordInput = view.findViewById(R.id.input_password);

        view.findViewById(R.id.btn_submit_login).setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "请输入邮箱和密码", Toast.LENGTH_SHORT).show();
            } else {
                // Call Login API
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("email", email);
                json.addProperty("password", password);

                com.example.roomiesplit.network.RetrofitClient.getApiService().login(json)
                        .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                            @Override
                            public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                    retrofit2.Response<com.google.gson.JsonObject> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    com.google.gson.JsonObject body = response.body();
                                    if (body.get("code").getAsInt() == 200) {
                                        com.google.gson.JsonObject data = body.getAsJsonObject("data");
                                        String token = data.get("token").getAsString();
                                        com.google.gson.JsonObject user = data.getAsJsonObject("user");
                                        Long userId = user.get("id").getAsLong();
                                        String username = user.get("username").getAsString();

                                        // Save Session
                                        new com.example.roomiesplit.utils.SessionManager(getContext())
                                                .createLoginSession(token, userId, username);

                                        // Navigate
                                        Navigation.findNavController(view).navigate(R.id.action_login_to_dashboard);
                                        Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), body.get("message").getAsString(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "登录失败: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        view.findViewById(R.id.text_forgot_password).setOnClickListener(v -> {
            Toast.makeText(getContext(), "重置密码功能开发中", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
