package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.selahbookingsystem.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AddOnBottomSheet extends BottomSheetDialogFragment {

    public interface OnAddOnSelectedListener {
        void onAddOnSelected(String category,
                             String value,
                             @Nullable String selectedCode);
    }

    private static final String ARG_PROVIDER_ID = "arg_provider_id";

    private Spinner spCategory, spValue;

    public static AddOnBottomSheet newInstance(@Nullable String providerId) {
        AddOnBottomSheet sheet = new AddOnBottomSheet();
        Bundle b = new Bundle();
        b.putString(ARG_PROVIDER_ID, providerId);
        sheet.setArguments(b);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottomsheet_addon, container, false);

        spCategory = v.findViewById(R.id.spCategory);
        spValue = v.findViewById(R.id.spValue);
        Button btnApply = v.findViewById(R.id.btnApply);

        List<String> categories = new ArrayList<>();
        categories.add("Base Service");
        categories.add("Add-on");
        categories.add("Length");
        categories.add("Shape");
        categories.add("Design Level");

        spCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
        ));

        updateValuesForCategory("Base Service");

        spCategory.setOnItemSelectedListener(new SimpleItemSelectedListener(pos -> {
            String cat = categories.get(pos);
            updateValuesForCategory(cat);
        }));

        btnApply.setOnClickListener(view -> {
            String category = (String) spCategory.getSelectedItem();
            String value = (String) spValue.getSelectedItem();

            if (value == null || value.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedCode = codeFor(category, value);
            notifyParent(category, value, selectedCode);
            dismiss();
        });

        return v;
    }

    private void updateValuesForCategory(String cat) {
        List<String> values = new ArrayList<>();

        switch (cat) {
            case "Base Service":
                values.add("BIAB Overlay");
                values.add("BIAB Infill");
                values.add("Gel Polish");
                values.add("Gel Extensions");
                values.add("Acrylic Extensions");
                values.add("Acrylic Infill");
                break;

            case "Add-on":
                values.add("Removal");
                values.add("French Tip");
                values.add("Chrome");
                values.add("Ombre");
                values.add("Gems / Charms");
                values.add("Hand Drawn Art");
                values.add("3D Art");
                values.add("Glitter");
                values.add("Extra Length");
                break;

            case "Length":
                values.add("Short");
                values.add("Medium");
                values.add("Long");
                values.add("XL");
                break;

            case "Shape":
                values.add("Round");
                values.add("Square");
                values.add("Tapered Square");
                values.add("Almond");
                values.add("Tapered Almond");
                values.add("Coffin");
                values.add("Stiletto");

                break;

            case "Design Level":
                values.add("Low");
                values.add("Medium");
                values.add("High");
                break;
        }

        spValue.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                values
        ));
    }

    private String codeFor(String category, String value) {
        if (category == null || value == null) return "";

        switch (category) {
            case "Base Service":
                switch (value) {
                    case "BIAB Overlay": return "biab_overlay";
                    case "BIAB Infill": return "biab_infill";
                    case "Gel Polish": return "gel_polish";
                    case "Gel Extensions": return "gel_extensions";
                    case "Acrylic Extensions": return "acrylic_extensions";
                    case "Acrylic Infill": return "acrylic_infill";
                }
                break;

            case "Add-on":
                switch (value) {
                    case "Removal": return "removal";
                    case "French Tip": return "french_tip";
                    case "Chrome": return "chrome";
                    case "Ombre": return "ombre";
                    case "Gems / Charms": return "gems_charms";
                    case "Hand Drawn Art": return "hand_drawn_art";
                    case "3D Art": return "three_d_art";
                    case "Glitter": return "glitter";
                    case "Extra Length": return "extra_length";
                }
                break;

            case "Length":
                return value.toLowerCase().replace(" ", "_");

            case "Shape":
                return value.toLowerCase().replace(" ", "_");

            case "Design Level":
                return value.toLowerCase().replace(" ", "_");
        }

        return value.toLowerCase().replace(" ", "_");
    }

    private void notifyParent(String category,
                              String value,
                              @Nullable String selectedCode) {
        if (getActivity() instanceof OnAddOnSelectedListener) {
            ((OnAddOnSelectedListener) getActivity()).onAddOnSelected(category, value, selectedCode);
        }
    }
}