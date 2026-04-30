package com.example.selahbookingsystem.ui.customer;

import android.content.ContentValues;
import android.content.Intent;
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
import com.example.selahbookingsystem.data.session.SessionManager;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.auth.MainActivity;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerProfileActivity extends BaseActivity {

    private ShapeableImageView profileImage;
    private TextView titleText;
    private TextView uidText;
    private TextView dobText;
    private TextView emailText;
    private TextView phoneText;
    private TextView passwordText;
    private EditText editName;
    private Button changeButton;
    private Button logoutButton;

    private SupabaseRestService api;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;

    private Uri cameraImageUri;
    private String currentUserId;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_customer_profile;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        api = ApiClient.get().create(SupabaseRestService.class);
        currentUserId = resolveCurrentUserId();

        initViews();
        setupImagePickers();
        setupListeners();

        if (currentUserId == null) {
            Toast.makeText(this, "No logged-in user ID found", Toast.LENGTH_LONG).show();
        } else {
            loadUserProfileFromSupabase();
        }
    }

    private String resolveCurrentUserId() {
        String id = TokenStore.getUserId(this);
        return id == null || id.trim().isEmpty() ? null : id;
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        titleText = findViewById(R.id.titleText);
        uidText = findViewById(R.id.UidText);
        dobText = findViewById(R.id.DOBText);
        emailText = findViewById(R.id.EmailText);
        phoneText = findViewById(R.id.PhoneNumberText);
        passwordText = findViewById(R.id.PasswordText);
        editName = findViewById(R.id.EditName);
        changeButton = findViewById(R.id.changeButton);
        logoutButton = findViewById(R.id.logoutButton);
    }

    private void setupImagePickers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        setProfileImage(uri);
                    }
                }
        );

        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        setProfileImage(cameraImageUri);
                    }
                }
        );
    }

    private void setupListeners() {
        profileImage.setOnClickListener(v -> showImageSourceDialog());

        changeButton.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }

            updateUserNameInSupabase(newName);
        });

        logoutButton.setOnClickListener(v -> logoutUser());
    }

    private void showImageSourceDialog() {
        String[] options = {"Choose from gallery", "Take a photo"};

        new AlertDialog.Builder(this)
                .setTitle("Set Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageLauncher.launch("image/*");
                    } else {
                        cameraImageUri = createImageUri();

                        if (cameraImageUri != null) {
                            takePhotoLauncher.launch(cameraImageUri);
                        } else {
                            Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private Uri createImageUri() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "Selah_Profile_" + timeStamp + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        return getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );
    }

    private void setProfileImage(Uri uri) {
        profileImage.setImageURI(uri);
    }

    private void loadUserProfileFromSupabase() {
        String idFilter = "eq." + currentUserId;
        String select = "id,full_name,email,phone,dob,role,created_at";

        api.getProfile(idFilter, select).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(
                    Call<List<SupabaseRestService.ProfileDto>> call,
                    Response<List<SupabaseRestService.ProfileDto>> response
            ) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(CustomerProfileActivity.this, "Could not load profile", Toast.LENGTH_SHORT).show();
                    return;
                }

                bindUserToUI(response.body().get(0));
            }

            @Override
            public void onFailure(
                    Call<List<SupabaseRestService.ProfileDto>> call,
                    Throwable t
            ) {
                Toast.makeText(CustomerProfileActivity.this, "Error loading profile: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUserToUI(SupabaseRestService.ProfileDto profile) {
        String name = safeName(profile.full_name);

        titleText.setText(name + "'s Profile");
        editName.setText(safeEdit(profile.full_name));

        uidText.setText("User ID: " + safe(profile.id));
        emailText.setText("Email: " + safe(profile.email));
        dobText.setText("Date of Birth: " + safe(profile.dob));
        phoneText.setText("Phone Number: " + safe(profile.phone));
        passwordText.setText("Password: ••••••••");

        profileImage.setImageResource(R.drawable.profile_icon);
    }

    private void updateUserNameInSupabase(String newName) {
        if (currentUserId == null) {
            Toast.makeText(this, "No logged-in user ID found", Toast.LENGTH_SHORT).show();
            return;
        }

        String idFilter = "eq." + currentUserId;
        SupabaseRestService.ProfileUpdateBody body =
                new SupabaseRestService.ProfileUpdateBody(newName);

        api.updateProfileName(idFilter, body).enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
            @Override
            public void onResponse(
                    Call<List<SupabaseRestService.ProfileDto>> call,
                    Response<List<SupabaseRestService.ProfileDto>> response
            ) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CustomerProfileActivity.this, "Failed to update name", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<SupabaseRestService.ProfileDto> list = response.body();

                if (list != null && !list.isEmpty()) {
                    bindUserToUI(list.get(0));
                } else {
                    titleText.setText(newName + "'s Profile");
                    editName.setText(newName);
                }

                Toast.makeText(CustomerProfileActivity.this, "Name updated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(
                    Call<List<SupabaseRestService.ProfileDto>> call,
                    Throwable t
            ) {
                Toast.makeText(CustomerProfileActivity.this, "Error updating name: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logoutUser() {
        TokenStore.clear(this);
        SessionManager.clear();

        getSharedPreferences("selah_auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        Intent intent = new Intent(CustomerProfileActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "(not set)" : value;
    }

    private String safeEdit(String value) {
        return value == null ? "" : value;
    }

    private String safeName(String value) {
        return value == null || value.trim().isEmpty() ? "Your" : value;
    }
}