package com.example.roomiesplit.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.roomiesplit.R;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusSelectionDialogFragment extends DialogFragment {

    private Long ledgerId;
    private OnStatusSelectedListener listener;

    public interface OnStatusSelectedListener {
        void onStatusSelected();
    }

    public void setListener(OnStatusSelectedListener listener) {
        this.listener = listener;
    }

    public static StatusSelectionDialogFragment newInstance(Long ledgerId) {
        StatusSelectionDialogFragment fragment = new StatusSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putLong("ledgerId", ledgerId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getArguments() != null) {
            ledgerId = getArguments().getLong("ledgerId");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_status_selection, null);

        setupStatusOption(view.findViewById(R.id.status_available), "AVAILABLE");
        setupStatusOption(view.findViewById(R.id.status_busy), "BUSY");
        setupStatusOption(view.findViewById(R.id.status_away), "AWAY");
        setupStatusOption(view.findViewById(R.id.status_sleeping), "ASLEEP");

        builder.setView(view)
                .setTitle("设置我的状态")
                .setNegativeButton("取消", (dialog, id) -> StatusSelectionDialogFragment.this.getDialog().cancel());
        return builder.create();
    }

    private void setupStatusOption(View view, String status) {
        view.setOnClickListener(v -> updateStatus(status));
    }

    private void updateStatus(String status) {
        com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                getContext());
        Long userId = session.getUserId();

        JsonObject request = new JsonObject();
        request.addProperty("status", status);

        com.example.roomiesplit.network.RetrofitClient.getApiService().updateMemberStatus(userId, ledgerId, request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            if (listener != null)
                                listener.onStatusSelected();
                            dismiss();
                        } else {
                            android.widget.Toast.makeText(getContext(), "更新失败", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        android.widget.Toast.makeText(getContext(), "网络错误", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
