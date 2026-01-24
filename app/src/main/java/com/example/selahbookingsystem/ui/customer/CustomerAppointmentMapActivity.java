package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class CustomerAppointmentMapActivity extends AppCompatActivity {

    public static final String EXTRA_LAT = "extra_lat";
    public static final String EXTRA_LNG = "extra_lng";
    public static final String EXTRA_TITLE = "extra_title";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_appointment_map);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        double lat = getIntent().getDoubleExtra(EXTRA_LAT, 53.3498); // Dublin default
        double lng = getIntent().getDoubleExtra(EXTRA_LNG, -6.2603);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        if (title == null) title = "Appointment location";

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            String finalTitle = title;
            mapFragment.getMapAsync(googleMap -> {
                GoogleMap m = googleMap;
                LatLng pos = new LatLng(lat, lng);
                m.addMarker(new MarkerOptions().position(pos).title(finalTitle));
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
            });
        }
    }
}

