package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.adapter.AppointmentsAdapter;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.ui.base.BaseActivity;

import java.util.Arrays;

public class CustomerHomeActivity extends BaseActivity {

    // Tell BaseActivity which bottom-nav item should be selected on this screen
    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;   // middle icon = Home
    }

    private AppointmentsAdapter apptAdapter;

    private final ActivityResultLauncher<Intent> pickProviderLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            String providerId = result.getData()
                                    .getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_ID);
                            String providerName = result.getData()
                                    .getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_NAME);

                            Toast.makeText(this,
                                    "Selected: " + providerName,
                                    Toast.LENGTH_LONG
                            ).show();

                            // TODO: open "Pick date/time" screen or create a simple booking.
                            // Later we can POST to /rest/v1/bookings here.
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // This layout is your page content; BaseActivity wraps it in activity_base with the bottom nav
        setContentView(R.layout.activity_customer_home);

        TextView titleText = findViewById(R.id.titleText);
        Button   bookBtn = findViewById(R.id.bookButton);

        String email = getIntent().getStringExtra("email");
        if (email == null) email = "";
        String name = RoleStore.getName(this, email);

        titleText.setText("Hey " + (name != null ? name : "Customer") + ",");

        // Appointments list (placeholder for now)
        RecyclerView apptRv = findViewById(R.id.appointmentsRecycler);
        apptRv.setLayoutManager(new LinearLayoutManager(this));
        apptAdapter = new AppointmentsAdapter();
        apptRv.setAdapter(apptAdapter);

        // Placeholder data until we wire GET /rest/v1/bookings
        apptAdapter.submit(Arrays.asList(
                new AppointmentsAdapter.Item("Demo booking with Alice", "Today 14:00"),
                new AppointmentsAdapter.Item("Demo booking with Bob", "Fri 09:30")
        ));

        bookBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, SelectProviderActivity.class);
            pickProviderLauncher.launch(i);
        });
    }
}
