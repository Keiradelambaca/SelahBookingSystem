package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.base.BaseActivity;

public class BookingPhotosActivity extends BaseActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";

    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";

    private Uri currentUri = null;
    private Uri inspoUri = null;

    private ImageView imgCurrent, imgInspo;

    @Override
    protected int getBottomNavMenuItemId() {
        // booking flow usually shouldn’t highlight a bottom nav item
        // but BaseActivity requires one; choose home
        return R.id.nav_home;
    }

    private final ActivityResultLauncher<String> pickCurrentLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    currentUri = uri;
                    imgCurrent.setImageURI(uri);
                }
            });

    private final ActivityResultLauncher<String> pickInspoLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    inspoUri = uri;
                    imgInspo.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_photos);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);

        TextView providerText = findViewById(R.id.providerText);
        providerText.setText("Booking with: " + (providerName != null ? providerName : ""));

        imgCurrent = findViewById(R.id.imgCurrent);
        imgInspo   = findViewById(R.id.imgInspo);

        Button btnPickCurrent = findViewById(R.id.btnPickCurrent);
        Button btnPickInspo   = findViewById(R.id.btnPickInspo);
        TextView btnNoInspo   = findViewById(R.id.btnNoInspo);
        Button btnContinue    = findViewById(R.id.btnContinue);

        btnPickCurrent.setOnClickListener(v -> pickCurrentLauncher.launch("image/*"));
        btnPickInspo.setOnClickListener(v -> pickInspoLauncher.launch("image/*"));

        btnNoInspo.setOnClickListener(v -> {
            // Phase 1: just show a toast until Explore booking handoff is built
            Toast.makeText(this, "Explore flow next (Phase 1.5)", Toast.LENGTH_SHORT).show();

            // Later:
            // Intent i = new Intent(this, CustomerExploreActivity.class);
            // i.putExtra("booking_provider_id", providerId);
            // i.putExtra("booking_provider_name", providerName);
            // startActivity(i);
        });

        btnContinue.setOnClickListener(v -> {
            if (providerId == null) {
                Toast.makeText(this, "Missing provider. Go back and select again.", Toast.LENGTH_LONG).show();
                return;
            }
            if (currentUri == null) {
                Toast.makeText(this, "Please upload a photo of your current nails.", Toast.LENGTH_LONG).show();
                return;
            }

            // In Phase 1 we can allow inspo optional.
            Intent next = new Intent(this, BookingBubblesActivity.class);
            next.putExtra(BookingBubblesActivity.EXTRA_PROVIDER_ID, providerId);
            next.putExtra(BookingBubblesActivity.EXTRA_PROVIDER_NAME, providerName);
            next.putExtra(BookingBubblesActivity.EXTRA_CURRENT_URI, currentUri.toString());
            if (inspoUri != null) next.putExtra(BookingBubblesActivity.EXTRA_INSPO_URI, inspoUri.toString());
            startActivity(next);
        });
    }
}
