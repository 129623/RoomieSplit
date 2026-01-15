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

public class WelcomeFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {

                // Auto Login Check
                com.example.roomiesplit.utils.SessionManager session = new com.example.roomiesplit.utils.SessionManager(
                                getContext());
                if (session.isLoggedIn()) {
                        // Navigate directly to Dashboard
                        // Use post slightly after view creation or just do it here if possible.
                        // Since we are inside onCreateView, returning the view first and then
                        // navigating is safer to avoid illegal state.
                }

                View view = inflater.inflate(R.layout.fragment_welcome, container, false);

                if (session.isLoggedIn()) {
                        view.post(() -> {
                                if (view != null) {
                                        try {
                                                Navigation.findNavController(view)
                                                                .navigate(R.id.action_welcome_to_dashboard);
                                        } catch (Exception e) {
                                                e.printStackTrace();
                                        }
                                }
                        });
                }

                view.findViewById(R.id.btn_login)
                                .setOnClickListener(v -> Navigation.findNavController(view)
                                                .navigate(R.id.action_welcome_to_login));

                view.findViewById(R.id.btn_register)
                                .setOnClickListener(v -> Navigation.findNavController(view)
                                                .navigate(R.id.action_welcome_to_register));

                view.findViewById(R.id.btn_guest)
                                .setOnClickListener(v -> Navigation.findNavController(view)
                                                .navigate(R.id.action_welcome_to_dashboard));
                return view;
        }
}
