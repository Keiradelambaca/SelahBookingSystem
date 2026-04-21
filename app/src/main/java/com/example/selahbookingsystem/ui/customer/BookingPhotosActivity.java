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
import com.example.selahbookingsystem.network.storage.SupabaseStorageUploader;
import com.example.selahbookingsystem.ui.base.BaseActivity;

public class BookingPhotosActivity extends BaseActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";

    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";

    private Uri currentUri = null;
    private Uri inspoUri = null;

    private ImageView imgCurrent, imgInspo;
    private Button btnContinue;

    @Override
    protected int getBottomNavMenuItemId() {
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
        imgInspo = findViewById(R.id.imgInspo);

        Button btnPickCurrent = findViewById(R.id.btnPickCurrent);
        Button btnPickInspo = findViewById(R.id.btnPickInspo);
        TextView btnNoInspo = findViewById(R.id.btnNoInspo);
        btnContinue = findViewById(R.id.btnContinue);

        btnPickCurrent.setOnClickListener(v -> pickCurrentLauncher.launch("image/*"));
        btnPickInspo.setOnClickListener(v -> pickInspoLauncher.launch("image/*"));

        btnNoInspo.setOnClickListener(v ->
                Toast.makeText(this, "Explore flow next (Phase 1.5)", Toast.LENGTH_SHORT).show()
        );

        btnContinue.setOnClickListener(v -> {
            if (providerId == null || providerId.trim().isEmpty()) {
                Toast.makeText(this, "Missing provider. Go back and select again.", Toast.LENGTH_LONG).show();
                return;
            }

            if (currentUri == null) {
                Toast.makeText(this, "Please upload a photo of your current nails.", Toast.LENGTH_LONG).show();
                return;
            }

            uploadPhotosAndContinue(providerId, providerName);
        });
    }

    private void uploadPhotosAndContinue(String providerId, @Nullable String providerName) {
        setContinueEnabled(false);
        Toast.makeText(this, "Uploading photos...", Toast.LENGTH_SHORT).show();

        SupabaseStorageUploader.uploadImage(
                this,
                currentUri,
                "current",
                new SupabaseStorageUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String currentUrl) {
                        if (inspoUri != null) {
                            uploadInspoAndOpenNext(providerId, providerName, currentUrl);
                        } else {
                            runOnUiThread(() -> openNextScreen(providerId, providerName, currentUrl, null));
                        }
                    }

                    @Override
                    public void onError(String message, @Nullable Throwable throwable) {
                        runOnUiThread(() -> {
                            setContinueEnabled(true);
                            Toast.makeText(
                                    BookingPhotosActivity.this,
                                    "Failed to upload current photo.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void uploadInspoAndOpenNext(String providerId, @Nullable String providerName, String currentUrl) {
        SupabaseStorageUploader.uploadImage(
                this,
                inspoUri,
                "inspo",
                new SupabaseStorageUploader.UploadCallback() {
                    @Override
                    public void onSuccess(String inspoUrl) {
                        runOnUiThread(() -> openNextScreen(providerId, providerName, currentUrl, inspoUrl));
                    }

                    @Override
                    public void onError(String message, @Nullable Throwable throwable) {
                        runOnUiThread(() -> {
                            setContinueEnabled(true);

                            String friendlyMessage = "Failed to upload inspo photo.";

                            if (throwable != null && throwable.getMessage() != null) {
                                String errorText = throwable.getMessage().toLowerCase();

                                if (errorText.contains("heif") || errorText.contains("heic") || errorText.contains("decode")) {
                                    friendlyMessage = "That inspiration image format is not supported on this emulator. Please choose a JPG or PNG image.";
                                }
                            }

                            Toast.makeText(
                                    BookingPhotosActivity.this,
                                    friendlyMessage,
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                }
        );
    }

    private void openNextScreen(String providerId, @Nullable String providerName, String currentUrl, @Nullable String inspoUrl) {
        setContinueEnabled(true);

        Intent next = new Intent(this, BookingBubblesActivity.class);
        next.putExtra(BookingBubblesActivity.EXTRA_PROVIDER_ID, providerId);
        next.putExtra(BookingBubblesActivity.EXTRA_PROVIDER_NAME, providerName);

        // These are now Supabase Storage URLs
        next.putExtra(BookingBubblesActivity.EXTRA_CURRENT_URI, currentUrl);

        if (inspoUrl != null) {
            next.putExtra(BookingBubblesActivity.EXTRA_INSPO_URI, inspoUrl);
        }

        startActivity(next);
    }

    private void setContinueEnabled(boolean enabled) {
        btnContinue.setEnabled(enabled);
        btnContinue.setAlpha(enabled ? 1f : 0.6f);
    }
}
