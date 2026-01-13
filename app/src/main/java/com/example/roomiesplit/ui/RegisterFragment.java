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

public class RegisterFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        TextInputEditText nameInput = view.findViewById(R.id.input_name);
        TextInputEditText emailInput = view.findViewById(R.id.input_email);
        TextInputEditText passwordInput = view.findViewById(R.id.input_password);

        view.findViewById(R.id.btn_submit_register).setOnClickListener(v -> {
            String name = nameInput.getText() != null ? nameInput.getText().toString() : "";
            String email = emailInput.getText() != null ? emailInput.getText().toString() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString() : "";

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "请填写完整信息", Toast.LENGTH_SHORT).show();
            } else {
                // Call Register API
                com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                json.addProperty("email", email);
                json.addProperty("password", password);
                json.addProperty("displayName", name);

                com.example.roomiesplit.network.RetrofitClient.getApiService().register(json)
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
                                        Navigation.findNavController(view)
                                                .navigate(R.id.action_register_to_create_join);
                                        Toast.makeText(getContext(), "注册成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(getContext(), body.get("message").getAsString(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "注册失败: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                                Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        return view;
    }
}
