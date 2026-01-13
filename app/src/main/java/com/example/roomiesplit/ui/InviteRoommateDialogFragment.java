package com.example.roomiesplit.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.roomiesplit.R;

public class InviteRoommateDialogFragment extends DialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_invite_roommate, container, false);

        EditText emailInput = view.findViewById(R.id.edit_invite_email);

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());

        view.findViewById(R.id.btn_send_invite).setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                emailInput.setError("请输入账号");
                return;
            }

            // API Call
            com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                    getContext());
            Long userId = session.getUserId();
            Long ledgerId = session.getCurrentLedgerId();

            if (ledgerId == null) {
                Toast.makeText(getContext(), "未选择账本", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.gson.JsonObject request = new com.google.gson.JsonObject();
            request.addProperty("email", email);

            com.example.roomiesplit.network.RetrofitClient.getApiService().inviteMember(userId, ledgerId, request)
                    .enqueue(new retrofit2.Callback<com.google.gson.JsonObject>() {
                        @Override
                        public void onResponse(retrofit2.Call<com.google.gson.JsonObject> call,
                                retrofit2.Response<com.google.gson.JsonObject> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                com.google.gson.JsonObject body = response.body();
                                if (body.get("code").getAsInt() == 200) {
                                    Toast.makeText(getContext(), "邀请已发送", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                } else {
                                    Toast.makeText(getContext(), "邀请失败: " + body.get("message").getAsString(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getContext(), "网络请求失败", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<com.google.gson.JsonObject> call, Throwable t) {
                            Toast.makeText(getContext(), "网络错误: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Optional: for rounded
                                                                                           // corners if handled in xml
                                                                                           // root background
        }
    }
}
