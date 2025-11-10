package com.example.selahbookingsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

public class WelcomeCustomerActivity extends AppCompatActivity {

    private AppointmentsAdapter apptAdapter;

    private final ActivityResultLauncher<Intent> pickProviderLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String providerId = result.getData().getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_ID);
                    String providerName = result.getData().getStringExtra(SelectProviderActivity.EXTRA_SELECTED_PROVIDER_NAME);
                    Toast.makeText(this, "Selected: " + providerName, Toast.LENGTH_LONG).show();

                    // TODO (next step): open a "Pick date/time" screen or create a simple booking.
                    // For now we just show a Toast. We can POST to /rest/v1/bookings when ready.
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_customer);

        TextView emailTv = findViewById(R.id.emailText);
        TextView phoneTv = findViewById(R.id.phoneText);
        Button   bookBtn = findViewById(R.id.bookButton);

        String email = getIntent().getStringExtra("email");
        if (email == null) email = "";
        String phone = RoleStore.getPhone(this, email);

        emailTv.setText("Email: " + email);
        phoneTv.setText("Phone: " + (phone.isEmpty() ? "-" : phone));

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