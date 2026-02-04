package com.example.selahbookingsystem.ui.provider;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SPLocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_EIRCODE = "EXTRA_EIRCODE";
    public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
    public static final String EXTRA_LAT = "EXTRA_LAT";
    public static final String EXTRA_LNG = "EXTRA_LNG";

    private GoogleMap map;
    private Geocoder geocoder;

    private EditText etSearch;
    private TextView tvSelected;

    private LatLng selectedLatLng = null;
    private String selectedAddress = null;
    private String selectedEircode = null;

    // Irish Eircode regex (covers routing key + unique identifier)
    private static final Pattern EIRCODE_PATTERN =
            Pattern.compile("(?i)\\b(?:D6W|[AC-FHKNPRTV-Y]\\d{2})\\s?[0-9AC-FHKNPRTV-Y]{4}\\b");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp_location_picker);

        geocoder = new Geocoder(this, Locale.getDefault());

        etSearch = findViewById(R.id.etSearch);
        tvSelected = findViewById(R.id.tvSelected);

        Button btnSearch = findViewById(R.id.btnSearch);
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnConfirm = findViewById(R.id.btnConfirm);

        // Create the map fragment programmatically into mapContainer
        SupportMapFragment mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapContainer, mapFragment)
                .commit();

        mapFragment.getMapAsync(gMap -> {
            map = gMap;

            // Default camera: Dublin-ish
            LatLng dublin = new LatLng(53.3498, -6.2603);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(dublin, 11f));

            // Long press to select a location
            map.setOnMapLongClickListener(latLng -> {
                setSelection(latLng);
                reverseGeocode(latLng);
            });
        });

        btnSearch.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) {
                Toast.makeText(this, "Enter an eircode or address", Toast.LENGTH_SHORT).show();
                return;
            }
            forwardGeocode(q);
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng == null) {
                Toast.makeText(this, "Long-press the map to choose a location", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent data = new Intent();
            data.putExtra(EXTRA_LAT, selectedLatLng.latitude);
            data.putExtra(EXTRA_LNG, selectedLatLng.longitude);
            data.putExtra(EXTRA_ADDRESS, selectedAddress != null ? selectedAddress : "");
            data.putExtra(EXTRA_EIRCODE, selectedEircode != null ? selectedEircode : "");

            setResult(RESULT_OK, data);
            finish();
        });
    }

    private void setSelection(LatLng latLng) {
        selectedLatLng = latLng;
        selectedAddress = null;
        selectedEircode = null;

        if (map != null) {
            map.clear();
            map.addMarker(new MarkerOptions().position(latLng).title("Selected"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        }

        tvSelected.setText("Selected: " + latLng.latitude + ", " + latLng.longitude);
    }

    private void forwardGeocode(String query) {
        try {
            List<Address> results = geocoder.getFromLocationName(query, 1);
            if (results == null || results.isEmpty()) {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();
                return;
            }

            Address a = results.get(0);
            LatLng latLng = new LatLng(a.getLatitude(), a.getLongitude());
            setSelection(latLng);

            // Build address + attempt to extract eircode
            String addressLine = bestAddressLine(a);
            selectedAddress = addressLine;
            selectedEircode = extractEircode(a, addressLine);

            tvSelected.setText("Selected: " + safe(addressLine)
                    + (selectedEircode != null ? "\nEircode: " + selectedEircode : ""));

        } catch (IOException e) {
            Toast.makeText(this, "Geocoding error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void reverseGeocode(LatLng latLng) {
        try {
            List<Address> results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (results == null || results.isEmpty()) {
                selectedAddress = latLng.latitude + ", " + latLng.longitude;
                tvSelected.setText("Selected: " + selectedAddress);
                return;
            }

            Address a = results.get(0);
            String addressLine = bestAddressLine(a);

            selectedAddress = addressLine;
            selectedEircode = extractEircode(a, addressLine);

            tvSelected.setText("Selected: " + safe(addressLine)
                    + (selectedEircode != null ? "\nEircode: " + selectedEircode : ""));

        } catch (IOException e) {
            Toast.makeText(this, "Reverse geocode error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String bestAddressLine(Address a) {
        // Sometimes getAddressLine(0) is best; fall back to composed string
        String line0 = a.getMaxAddressLineIndex() >= 0 ? a.getAddressLine(0) : null;
        if (line0 != null && !line0.trim().isEmpty()) return line0.trim();

        StringBuilder sb = new StringBuilder();
        if (a.getFeatureName() != null) sb.append(a.getFeatureName()).append(", ");
        if (a.getLocality() != null) sb.append(a.getLocality()).append(", ");
        if (a.getAdminArea() != null) sb.append(a.getAdminArea()).append(", ");
        if (a.getCountryName() != null) sb.append(a.getCountryName());

        return sb.toString().replaceAll(", $", "").trim();
    }

    private String extractEircode(Address a, String addressLine) {
        // 1) Android sometimes puts Eircode in postalCode
        String postal = a.getPostalCode();
        if (postal != null && !postal.trim().isEmpty()) {
            Matcher m = EIRCODE_PATTERN.matcher(postal.trim());
            if (m.find()) return normalizeEircode(m.group());
        }

        // 2) Try parsing from the address line
        if (addressLine != null) {
            Matcher m = EIRCODE_PATTERN.matcher(addressLine);
            if (m.find()) return normalizeEircode(m.group());
        }

        return null;
    }

    private String normalizeEircode(String raw) {
        // Make it "A65 F4E2" format when possible
        String cleaned = raw.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
        if (cleaned.length() == 7) {
            return cleaned.substring(0, 3) + " " + cleaned.substring(3);
        }
        return raw.toUpperCase(Locale.ROOT).trim();
    }

    private String safe(String v) {
        return (v == null || v.trim().isEmpty()) ? "(not set)" : v.trim();
    }
}

