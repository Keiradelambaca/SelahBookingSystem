package com.example.selahbookingsystem;

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
    private TextView titleText, uidText, dobText, emailText, phoneText, passwordText;
    private EditText editName;
    private Button changeButton, addCardButton, logoutButton;
    private SupabaseRestService api;

    // For image picking
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private Uri cameraImageUri;

    // Supabase auth user id (same as profiles.id)
    private String currentUserId;

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_profile);

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

    /**
     * Get the current Supabase auth user id that matches profiles.id
     * You can set this in SharedPreferences after login,
     * or pass it in the Intent as EXTRA_USER_ID.
     */
    private String resolveCurrentUserId() {
        // 1) From Intent extra
        String fromIntent = getIntent().getStringExtra("EXTRA_USER_ID");
        if (fromIntent != null && !fromIntent.isEmpty()) {
            return fromIntent;
        }

        // 2) From SharedPreferences
        SharedPreferences prefs = getSharedPreferences("selah_auth", Context.MODE_PRIVATE);
        return prefs.getString("auth_user_id", null);
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
        addCardButton = findViewById(R.id.addCardButton);
        logoutButton = findViewById(R.id.logoutButton);
        // cardsRecycler = findViewById(R.id.cardsRecycler); // later when you do Stripe
    }

    // Allow user to set a profile picture from gallery or camera
    private void setupImagePickers() {
        // Gallery picker
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        setProfileImage(uri);
                        // TODO: upload this uri to Supabase Storage and save URL
                    }
                }
        );

        // Camera capture
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        setProfileImage(cameraImageUri);
                        // TODO: upload this image to Supabase Storage and save URL
                    }
                }
        );
    }

    // Listeners for buttons / image
    private void setupListeners() {
        // Tap profile picture to change it
        profileImage.setOnClickListener(v -> showImageSourceDialog());

        // Change button that updates user’s name in Supabase
        changeButton.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Please enter your name.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateUserNameInSupabase(newName);
        });

        // Add a card button (placeholder until Stripe)
        addCardButton.setOnClickListener(v ->
                Toast.makeText(this,
                        "Card management with Stripe coming soon 💳",
                        Toast.LENGTH_SHORT).show()
        );

        // Logout Button
        logoutButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("selah_auth", MODE_PRIVATE);
            prefs.edit().clear().apply();

            Intent intent = new Intent(CustomerProfileActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Choose from gallery", "Take a photo"};
        new AlertDialog.Builder(this)
                .setTitle("Set as Profile Picture")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Gallery
                        pickImageLauncher.launch("image/*");
                    } else {
                        // Camera
                        cameraImageUri = createImageUri();
                        if (cameraImageUri != null) {
                            takePhotoLauncher.launch(cameraImageUri);
                        } else {
                            Toast.makeText(this,
                                    "Unable to open camera",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private Uri createImageUri() {
        String timeStamp = new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault()
        ).format(new Date());

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,
                "Selah_Profile_" + timeStamp + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        return getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
        );
    }

    private void setProfileImage(Uri uri) {
        // Simple version for now
        profileImage.setImageURI(uri);
        // Later: use Glide/Coil and upload to Supabase Storage.
    }

    // -----------------------------
    //  SUPABASE: LOAD + UPDATE USER
    // -----------------------------

    // Load user row from public.profiles and bind to UI
    private void loadUserProfileFromSupabase() {
        if (currentUserId == null) return;

        String idFilter = "eq." + currentUserId;
        String select = "id,full_name,email,phone,dob,role,created_at";

        api.getProfile(idFilter, select)
                .enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
                    @Override
                    public void onResponse(
                            Call<List<SupabaseRestService.ProfileDto>> call,
                            Response<List<SupabaseRestService.ProfileDto>> response
                    ) {
                        if (!response.isSuccessful()
                                || response.body() == null
                                || response.body().isEmpty()) {
                            Toast.makeText(CustomerProfileActivity.this,
                                    "Could not load profile",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        SupabaseRestService.ProfileDto dto = response.body().get(0);
                        bindUserToUI(dto);
                    }

                    @Override
                    public void onFailure(
                            Call<List<SupabaseRestService.ProfileDto>> call,
                            Throwable t
                    ) {
                        Toast.makeText(CustomerProfileActivity.this,
                                "Error loading profile: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Bind Supabase profile row to your card UI
    private void bindUserToUI(SupabaseRestService.ProfileDto profile) {
        String name = profile.full_name != null ? profile.full_name : "Your";
        titleText.setText(name + "'s Profile");
        editName.setText(profile.full_name);

        uidText.setText("User ID: " + safe(profile.id));
        emailText.setText("Email: " + safe(profile.email));

        dobText.setText("Date of Birth: " + safe(profile.dob));
        phoneText.setText("Phone Number: " + safe(profile.phone));

        // Just a fixed mask – never expose real password
        passwordText.setText("Password: ••••••••");

        // When you add profile_image_url later, load it here
        profileImage.setImageResource(R.drawable.profile_icon);
    }

    private String safe(String value) {
        return (value == null || value.isEmpty()) ? "(not set)" : value;
    }

    // Update just the full_name column in Supabase
    private void updateUserNameInSupabase(String newName) {
        if (currentUserId == null) return;

        String idFilter = "eq." + currentUserId;
        SupabaseRestService.ProfileUpdateBody body =
                new SupabaseRestService.ProfileUpdateBody(newName);

        api.updateProfileName(idFilter, body)
                .enqueue(new Callback<List<SupabaseRestService.ProfileDto>>() {
                    @Override
                    public void onResponse(
                            Call<List<SupabaseRestService.ProfileDto>> call,
                            Response<List<SupabaseRestService.ProfileDto>> response
                    ) {
                        if (!response.isSuccessful()) {
                            Toast.makeText(CustomerProfileActivity.this,
                                    "Failed to update name",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        List<SupabaseRestService.ProfileDto> list = response.body();
                        if (list != null && !list.isEmpty()) {
                            bindUserToUI(list.get(0));
                        } else {
                            // Fallback: just update title + field
                            titleText.setText(newName + "'s Profile");
                            editName.setText(newName);
                        }

                        Toast.makeText(CustomerProfileActivity.this,
                                "Name updated",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(
                            Call<List<SupabaseRestService.ProfileDto>> call,
                            Throwable t
                    ) {
                        Toast.makeText(CustomerProfileActivity.this,
                                "Error updating name: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}