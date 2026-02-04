package com.example.selahbookingsystem.ui.provider;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.auth.MainActivity;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPProfileActivity extends SPBaseActivity {

    private ShapeableImageView profileImage, bannerImage;
    private TextView titleText, uidText, dobText, emailText, phoneText, passwordText, locationPreviewText;
    private EditText editName, editEircode;
    private Button changeBannerButton, pickOnMapButton, saveLocationButton, changeButton, logoutButton;
    private SupabaseRestService api;
    private String currentUserId;
    private ActivityResultLauncher<String> pickProfileImageLauncher;
    private ActivityResultLauncher<Uri> takeProfilePhotoLauncher;
    private Uri profileCameraUri;
    private ActivityResultLauncher<String> pickBannerLauncher;
    private ActivityResultLauncher<Intent> mapPickerLauncher;
    private String pickedAddress = null;
    private Double pickedLat = null;
    private Double pickedLng = null;
    private Double savedLat = null;
    private Double savedLng = null;
    private com.google.android.gms.maps.GoogleMap previewMap;


    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_sp_profile;
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_sp_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        api = ApiClient.get().create(SupabaseRestService.class);
        currentUserId = resolveCurrentUserId();

        initViews();
        initMapPreviewIfNeeded();
        setupPickers();
        setupListeners();

        if (currentUserId == null) {
            Toast.makeText(this, "No logged-in user ID found", Toast.LENGTH_LONG).show();
        } else {
            loadProviderProfile();
        }
    }


    // Resolve user id
    private String resolveCurrentUserId() {

        // from Intent extra
        String fromIntent = getIntent().getStringExtra("EXTRA_USER_ID");
        if (fromIntent != null && !fromIntent.isEmpty()) return fromIntent;

        // from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("selah_auth", Context.MODE_PRIVATE);
        return prefs.getString("auth_user_id", null);
    }


    // Init Views
    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        bannerImage = findViewById(R.id.bannerImage);

        titleText = findViewById(R.id.titleText);
        uidText = findViewById(R.id.UidText);
        dobText = findViewById(R.id.DOBText);
        emailText = findViewById(R.id.EmailText);
        phoneText = findViewById(R.id.PhoneNumberText);
        passwordText = findViewById(R.id.PasswordText);

        editName = findViewById(R.id.EditName);
        changeButton = findViewById(R.id.changeButton);

        changeBannerButton = findViewById(R.id.changeBannerButton);

        editEircode = findViewById(R.id.editEircode);
        pickOnMapButton = findViewById(R.id.pickOnMapButton);
        saveLocationButton = findViewById(R.id.saveLocationButton);
        locationPreviewText = findViewById(R.id.locationPreviewText);

        logoutButton = findViewById(R.id.logoutButton);
    }


    // Pickers (gallery/camera/map)
    private void setupPickers() {

        // Profile: gallery
        pickProfileImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        profileImage.setImageURI(uri);
                        Toast.makeText(this, "Profile picture set (upload later)", Toast.LENGTH_SHORT).show();
                        // TODO: upload to Supabase Storage and save profile_image_url
                    }
                }
        );

        // Profile: camera
        takeProfilePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && profileCameraUri != null) {
                        profileImage.setImageURI(profileCameraUri);
                        Toast.makeText(this, "Profile photo captured (upload later)", Toast.LENGTH_SHORT).show();
                        // TODO: upload to Supabase Storage and save profile_image_url
                    }
                }
        );

        // Banner: gallery
        pickBannerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        bannerImage.setImageURI(uri);
                        Toast.makeText(this, "Banner set (upload later)", Toast.LENGTH_SHORT).show();
                        // TODO: upload to Supabase Storage and save banner_url
                    }
                }
        );

        // Map picker: returns eircode/address/lat/lng
        mapPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        String eircode = data.getStringExtra(SPLocationPickerActivity.EXTRA_EIRCODE);
                        String address = data.getStringExtra(SPLocationPickerActivity.EXTRA_ADDRESS);

                        double lat = data.getDoubleExtra(SPLocationPickerActivity.EXTRA_LAT, 0);
                        double lng = data.getDoubleExtra(SPLocationPickerActivity.EXTRA_LNG, 0);

                        if (eircode != null && !eircode.isEmpty()) {
                            editEircode.setText(eircode);
                        }

                        if (address != null && !address.isEmpty()) {
                            locationPreviewText.setText("Current: " + address);
                        } else if (lat != 0 && lng != 0) {
                            locationPreviewText.setText("Current: " + lat + ", " + lng);
                        }

                        pickedAddress = address;
                        pickedLat = (lat != 0) ? lat : null;
                        pickedLng = (lng != 0) ? lng : null;
                        if (pickedLat != null && pickedLng != null) {
                            updateMapPreview(pickedLat, pickedLng);
                        }

                    }
                }
        );
    }


    // Listeners
    private void setupListeners() {

        profileImage.setOnClickListener(v -> showProfileImageDialog());

        changeButton.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateName(newName);
        });

        changeBannerButton.setOnClickListener(v -> pickBannerLauncher.launch("image/*"));

        pickOnMapButton.setOnClickListener(v -> {
            Intent i = new Intent(this, SPLocationPickerActivity.class);
            mapPickerLauncher.launch(i);
        });

        saveLocationButton.setOnClickListener(v -> {
            String eircode = editEircode.getText().toString().trim();
            if (eircode.isEmpty()) {
                Toast.makeText(this, "Enter an eircode first.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateLocation(eircode);
        });

        findViewById(R.id.mapPreviewCard).setOnClickListener(v -> {
            Intent i = new Intent(this, SPLocationPickerActivity.class);
            mapPickerLauncher.launch(i);
        });


        logoutButton.setOnClickListener(v -> {
            // Clear tokens/session
            TokenStore.clear(SPProfileActivity.this);
            com.example.selahbookingsystem.data.session.SessionManager.clear();

            // Clear auth prefs
            getSharedPreferences("selah_auth", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            // Back to login
            Intent intent = new Intent(SPProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }


    private void showProfileImageDialog() {
        String[] options = {"Choose from gallery", "Take a photo"};
        new AlertDialog.Builder(this)
                .setTitle("Set as Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickProfileImageLauncher.launch("image/*");
                    } else {
                        profileCameraUri = createImageUri("Selah_SP_Profile_");
                        if (profileCameraUri != null) {
                            takeProfilePhotoLauncher.launch(profileCameraUri);
                        } else {
                            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }


    private Uri createImageUri(String prefix) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, prefix + timeStamp + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }


    private void loadProviderProfile() {
        if (currentUserId == null) return;

        String idFilter = "eq." + currentUserId;
        String select = "id,full_name,email,phone,dob,role,created_at,eircode,address,banner_url";

        api.getProfile(idFilter, select).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                   Response<List<SupabaseRestService.ProfileDto>> response) {

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(SPProfileActivity.this, "Could not load profile", Toast.LENGTH_SHORT).show();
                    return;
                }

                bindToUi(response.body().get(0));
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                Toast.makeText(SPProfileActivity.this, "Error loading profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindToUi(SupabaseRestService.ProfileDto p) {
        String name = p.full_name != null ? p.full_name : "Your";
        titleText.setText(name + "'s Profile");
        editName.setText(p.full_name);

        uidText.setText("User ID: " + safe(p.id));
        dobText.setText("Date of Birth: " + safe(p.dob));
        emailText.setText("Email: " + safe(p.email));
        phoneText.setText("Phone Number: " + safe(p.phone));
        passwordText.setText("Password: ••••••••");

        savedLat = p.lat;
        savedLng = p.lng;

        if (savedLat != null && savedLng != null) {
            updateMapPreview(savedLat, savedLng);
        }

        if (p.eircode != null && !p.eircode.isEmpty()) {
            editEircode.setText(p.eircode);
        }

        String preview = (p.address != null && !p.address.isEmpty())
                ? p.address
                : (p.eircode != null && !p.eircode.isEmpty() ? p.eircode : "(not set)");

        locationPreviewText.setText("Current: " + preview);

        // Later: load profile image + banner from URLs using Glide
    }

    private String safe(String v) {
        return (v == null || v.isEmpty()) ? "(not set)" : v;
    }

    private void updateName(String newName) {
        if (currentUserId == null) return;

        String idFilter = "eq." + currentUserId;
        SupabaseRestService.ProfileUpdateBody body = new SupabaseRestService.ProfileUpdateBody(newName);

        api.updateProfileName(idFilter, body).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                   Response<List<SupabaseRestService.ProfileDto>> response) {

                if (!response.isSuccessful()) {
                    Toast.makeText(SPProfileActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(SPProfileActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
                loadProviderProfile();
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                Toast.makeText(SPProfileActivity.this, "Error updating name: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateLocation(String eircode) {
        if (currentUserId == null) return;

        String idFilter = "eq." + currentUserId;

        // Save eircode + address
        SupabaseRestService.ProviderLocationUpdateBody body =
                new SupabaseRestService.ProviderLocationUpdateBody(eircode, pickedAddress, pickedLat, pickedLng);

        api.updateProviderLocation(idFilter, body).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.ProfileDto>> call,
                                   Response<List<SupabaseRestService.ProfileDto>> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(SPProfileActivity.this, "Failed to save location", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(SPProfileActivity.this, "Location saved", Toast.LENGTH_SHORT).show();
                loadProviderProfile();
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.ProfileDto>> call, Throwable t) {
                Toast.makeText(SPProfileActivity.this, "Error saving location: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initMapPreviewIfNeeded() {
        androidx.fragment.app.Fragment existing = getSupportFragmentManager().findFragmentById(R.id.mapPreviewContainer);
        if (existing != null) return;

        com.google.android.gms.maps.SupportMapFragment mapFragment =
                com.google.android.gms.maps.SupportMapFragment.newInstance();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mapPreviewContainer, mapFragment)
                .commit();

        mapFragment.getMapAsync(gMap -> {
            previewMap = gMap;

            // non-interactive frame
            previewMap.getUiSettings().setAllGesturesEnabled(false);
            previewMap.getUiSettings().setMapToolbarEnabled(false);
            previewMap.getUiSettings().setCompassEnabled(false);

            // render saved coords
            if (savedLat != null && savedLng != null) {
                updateMapPreview(savedLat, savedLng);
            }
        });
    }

    private void updateMapPreview(double lat, double lng) {
        if (previewMap == null) return;

        com.google.android.gms.maps.model.LatLng pos =
                new com.google.android.gms.maps.model.LatLng(lat, lng);

        previewMap.clear();
        previewMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions().position(pos));
        previewMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(pos, 14f));
    }

}