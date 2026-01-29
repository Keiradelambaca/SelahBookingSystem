package com.example.selahbookingsystem.ui.customer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.model.ServiceItem;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddOnBottomSheet extends BottomSheetDialogFragment {

    public interface OnAddOnSelectedListener {
        void onAddOnSelected(String category,
                             String value,
                             @Nullable String designPrompt,
                             @Nullable String serviceId);
    }

    private Spinner spCategory, spValue;
    private EditText etDesignPrompt;

    // For services dropdown (name list) + mapping to UUID
    private final List<ServiceItem> services = new ArrayList<>();
    private final List<String> serviceNames = new ArrayList<>();

    public static AddOnBottomSheet newInstance(LinkedHashMap<String, String> current) {
        // You can pass current selections via args later if you want
        return new AddOnBottomSheet();
    }

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.bottomsheet_addon, container, false);

        spCategory = v.findViewById(R.id.spCategory);
        spValue = v.findViewById(R.id.spValue);
        etDesignPrompt = v.findViewById(R.id.etDesignPrompt);
        Button btnApply = v.findViewById(R.id.btnApply);

        // Categories
        List<String> categories = new ArrayList<>();
        categories.add("Service");
        categories.add("Length");
        categories.add("Shape");
        categories.add("Design");

        spCategory.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                categories
        ));

        // Default category
        updateValuesForCategory("Service");

        // When category changes
        spCategory.setOnItemSelectedListener(new SimpleItemSelectedListener(pos -> {
            String cat = categories.get(pos);
            updateValuesForCategory(cat);
        }));

        // Apply button
        btnApply.setOnClickListener(view -> {
            String category = (String) spCategory.getSelectedItem();

            if ("Design".equals(category)) {
                String prompt = etDesignPrompt.getText() != null ? etDesignPrompt.getText().toString().trim() : "";
                if (prompt.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter a design prompt", Toast.LENGTH_SHORT).show();
                    return;
                }
                notifyParent(category, "Custom", prompt, null);
                dismiss();
                return;
            }

            if ("Service".equals(category)) {
                int idx = spValue.getSelectedItemPosition();
                if (idx < 0 || idx >= services.size()) {
                    Toast.makeText(requireContext(), "Please select a service", Toast.LENGTH_SHORT).show();
                    return;
                }
                ServiceItem s = services.get(idx);
                if (s == null || s.id == null || s.id.trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Service is missing an id", Toast.LENGTH_SHORT).show();
                    return;
                }
                notifyParent("Service", s.name, null, s.id); // ✅ sends serviceId UUID
                dismiss();
                return;
            }

            // Length / Shape
            String value = (String) spValue.getSelectedItem();
            notifyParent(category, value, null, null);
            dismiss();
        });

        return v;
    }

    private void updateValuesForCategory(String cat) {
        etDesignPrompt.setVisibility("Design".equals(cat) ? View.VISIBLE : View.GONE);
        spValue.setVisibility("Design".equals(cat) ? View.GONE : View.VISIBLE);

        if ("Design".equals(cat)) {
            return;
        }

        // Service comes from Supabase
        if ("Service".equals(cat)) {
            loadServicesIntoSpinner();
            return;
        }

        // Otherwise hardcoded lists
        List<String> values = new ArrayList<>();

        switch (cat) {
            case "Length":
                values.add("Overlay");
                values.add("Short");
                values.add("Shmedium");
                values.add("Medium");
                values.add("Med-long");
                values.add("Long");
                values.add("XL");
                values.add("XXL");
                values.add("XXL+");
                break;

            case "Shape":
                values.add("Square");
                values.add("Tapered square");
                values.add("Duck shape");
                values.add("Coffin");
                values.add("Ballerina");
                values.add("Stiletto");
                values.add("Almond");
                values.add("Tapered almond");
                values.add("Round");
                values.add("Sharp almond");
                break;
        }

        spValue.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                values
        ));
    }

    private void loadServicesIntoSpinner() {
        // Clear old
        services.clear();
        serviceNames.clear();

        android.util.Log.d("SERVICES", "Loading services from Supabase...");

        // Temporary UI while loading
        serviceNames.add("Loading services...");
        spValue.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                serviceNames
        ));

        // ✅ Fetch from Supabase services table
        // Assumes columns: id, name
        ApiClient.services().listServices("id,name").enqueue(new Callback<List<ServiceItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<ServiceItem>> call,
                                   @NonNull Response<List<ServiceItem>> response) {
                if (!isAdded()) return;

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Failed to load services (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    showServicesErrorState();
                    return;
                }

                services.clear();
                services.addAll(response.body());

                serviceNames.clear();
                for (ServiceItem s : services) {
                    serviceNames.add(s.name == null ? "Service" : s.name);
                }

                if (serviceNames.isEmpty()) {
                    serviceNames.add("No services found");
                }

                spValue.setAdapter(new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        serviceNames
                ));

                android.util.Log.d("SERVICES", "Loaded services count=" + (response.body() == null ? 0 : response.body().size()));

            }

            @Override
            public void onFailure(@NonNull Call<List<ServiceItem>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error loading services", Toast.LENGTH_SHORT).show();
                showServicesErrorState();
            }
        });
    }

    private void showServicesErrorState() {
        services.clear();
        serviceNames.clear();
        serviceNames.add("Could not load services");
        spValue.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                serviceNames
        ));
    }

    private void notifyParent(String category,
                              String value,
                              @Nullable String designPrompt,
                              @Nullable String serviceId) {
        if (getActivity() instanceof OnAddOnSelectedListener) {
            ((OnAddOnSelectedListener) getActivity()).onAddOnSelected(category, value, designPrompt, serviceId);
        }
    }
}
