package com.example.selahbookingsystem.ui.provider;

import android.content.Intent;
import android.os.Bundle;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.example.selahbookingsystem.ui.provider.SPAvailabilityActivity;
import com.google.android.material.card.MaterialCardView;

public class SPSchedulingActivity extends SPBaseActivity {

    @Override protected int getLayoutResourceId() { return R.layout.activity_sp_scheduling; }
    @Override protected int getSelectedNavItemId() { return R.id.nav_sp_scheduling; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialCardView cardAvailability = findViewById(R.id.cardAvailability);
        MaterialCardView cardServices = findViewById(R.id.cardServices);
        MaterialCardView cardEmails = findViewById(R.id.cardEmails);
        MaterialCardView cardPayments = findViewById(R.id.cardPayments);
        MaterialCardView cardManualBooking = findViewById(R.id.cardManualBooking);

        cardAvailability.setOnClickListener(v ->
                startActivity(new Intent(this, SPAvailabilityActivity.class)));

        cardServices.setOnClickListener(v ->
                startActivity(new Intent(this, SPServicesActivity.class)));

        cardEmails.setOnClickListener(v ->
                startActivity(new Intent(this, SPAutoEmailsActivity.class)));

        cardPayments.setOnClickListener(v ->
                startActivity(new Intent(this, SPDepositsPaymentsActivity.class)));

        // TODO: wire these later
        cardManualBooking.setOnClickListener(v -> { });
    }
}
