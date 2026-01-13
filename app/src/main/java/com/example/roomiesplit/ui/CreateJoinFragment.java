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

public class CreateJoinFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                View view = inflater.inflate(R.layout.fragment_create_join, container, false);

                // Setup Currency Spinner
                android.widget.Spinner spinner = view.findViewById(R.id.spinner_currency);
                android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(getContext(),
                                android.R.layout.simple_spinner_dropdown_item,
                                new String[] { "CNY (¥)", "USD ($)", "EUR (€)" });
                spinner.setAdapter(adapter);

                view.findViewById(R.id.btn_back_create)
                                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

                view.findViewById(R.id.btn_skip_setup).setOnClickListener(v -> {
                        Navigation.findNavController(view).navigate(R.id.action_create_join_to_dashboard);
                });

                view.findViewById(R.id.btn_create_dorm).setOnClickListener(v -> {
                        android.widget.EditText nameInput = view.findViewById(R.id.edit_dorm_name);
                        String name = nameInput.getText().toString().trim();
                        if (name.isEmpty()) {
                                nameInput.setError("请输入寝室名称");
                                return;
                        }
                        String currency = spinner.getSelectedItem().toString().split(" ")[0]; // "CNY", "USD"

                        // Call Create Ledger API
                        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
                        json.addProperty("name", name);
                        json.addProperty("defaultCurrency", currency);
                        json.addProperty("description", "Created via Android App");

                        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                                        getContext());
                        Long userId = session.getUserId();

                        com.example.roomiesplit.network.RetrofitClient.getApiService().createLedger(userId, json)
                                        .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                                                @Override
                                                public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                                                retrofit2.Response<com.google.gson.JsonObject> response) {
                                                        if (response.isSuccessful() && response.body() != null) {
                                                                com.google.gson.JsonObject body = response.body();
                                                                if (body.get("code").getAsInt() == 200) {
                                                                        // Automatically select this ledger? Maybe just
                                                                        // navigate to Dashboard
                                                                        android.widget.Toast.makeText(getContext(),
                                                                                        "创建成功",
                                                                                        android.widget.Toast.LENGTH_SHORT)
                                                                                        .show();
                                                                        Navigation.findNavController(view).navigate(
                                                                                        R.id.action_create_join_to_dashboard);
                                                                } else {
                                                                        android.widget.Toast.makeText(getContext(),
                                                                                        body.get("message")
                                                                                                        .getAsString(),
                                                                                        android.widget.Toast.LENGTH_SHORT)
                                                                                        .show();
                                                                }
                                                        } else {
                                                                android.widget.Toast.makeText(getContext(),
                                                                                "创建失败: " + response.code(),
                                                                                android.widget.Toast.LENGTH_SHORT)
                                                                                .show();
                                                        }
                                                }

                                                @Override
                                                public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call,
                                                                Throwable t) {
                                                        android.widget.Toast.makeText(getContext(), "网络错误",
                                                                        android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                        });
                });

                return view;
        }
}
