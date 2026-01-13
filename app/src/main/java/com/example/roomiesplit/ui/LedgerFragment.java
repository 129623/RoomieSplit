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

public class LedgerFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                        @Nullable Bundle savedInstanceState) {
                View view = inflater.inflate(R.layout.fragment_ledger, container, false);

                view.findViewById(R.id.btn_back_ledger)
                                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

                view.findViewById(R.id.card_current_ledger)
                                .setOnClickListener(v -> Navigation.findNavController(view).popBackStack());

                view.findViewById(R.id.btn_create_ledger).setOnClickListener(
                                v -> Navigation.findNavController(view).navigate(R.id.action_ledger_to_create_join));

                return view;
        }
}
