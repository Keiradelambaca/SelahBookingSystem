package com.example.selahbookingsystem;

import static androidx.core.content.ContextCompat.startActivity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Customer signup screen:
 * 1. Sign up user in Supabase Auth (email + password)
 * 2. On success, insert a row into public.profiles using the returned user.id
 *    columns: id, email, full_name, phone, dob, role
 *
 * NOTE:
 *  - RLS on profiles must allow inserts for anonymous role (or for auth.uid()).
 *  - ApiClient is assumed to add apikey + Authorization: Bearer <anon-key>
 */
public class SignupActivityCustomer extends AppCompatActivity {

    private EditText fullNameEt;
    private EditText emailEt;
    private EditText phoneEt;
    private EditText passwordEt;
    private EditText confirmPasswordEt;
    private EditText dobEt;   // yyyy-MM-dd
    private Button signupBtn;
    private TextView validationText;

    // Retrofit services – you already have these via ApiClient
    private AuthService authService;
    private ProfilesService profilesService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_customer);

        // Bind views (update IDs if your XML uses different ones)
        fullNameEt        = findViewById(R.id.nameEditText);
        emailEt           = findViewById(R.id.emailEditText);
        phoneEt           = findViewById(R.id.editTextPhone);
        passwordEt        = findViewById(R.id.passwordEditText);
        dobEt             = findViewById(R.id.editTextDob);
        signupBtn         = findViewById(R.id.signupButton);
        validationText    = findViewById(R.id.validationText);

        // Init Retrofit services (ApiClient is your existing Retrofit builder)
        authService = ApiClient.get().create(AuthService.class);
        profilesService = ApiClient.get().create(ProfilesService.class);

        // Optional: date picker for DOB
        dobEt.setOnClickListener(v -> showDatePicker());

        signupBtn.setOnClickListener(v -> {
            validationText.setText("");
            doSignup();
        });
    }

    // -----------------------------
    // Date picker for DOB (optional)
    // -----------------------------
    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = 2004;
        int month = 9; // October is 9 (0-based)
        int day = 31;

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (DatePicker view, int y, int m, int d) -> {
                    // Format: yyyy-MM-dd
                    String mm = String.format("%02d", m + 1);
                    String dd = String.format("%02d", d);
                    dobEt.setText(y + "-" + mm + "-" + dd);
                },
                year, month, day
        );
        dpd.show();
    }

    // -----------------------------
    // 1. Validate, then call Supabase Auth
    // -----------------------------
    private void doSignup() {
        String fullName = fullNameEt.getText().toString().trim();
        String email    = emailEt.getText().toString().trim();
        String phone    = phoneEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();
        String dob      = dobEt.getText().toString().trim(); // "yyyy-MM-dd"

        if (TextUtils.isEmpty(fullName) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(dob)) {

            validationText.setText("Please fill in all fields");
            return;
        }

        validationText.setText("Creating account...");

        // Body for Supabase Auth signUp
        Map<String, Object> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        authService.signUp(body).enqueue(new Callback<SignUpRes>() {
            @Override
            public void onResponse(Call<SignUpRes> call, Response<SignUpRes> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    validationText.setText("Sign up failed: " + resp.code());
                    Log.e("SUPA", "signUp error body = " + resp.message());
                    return;
                }

                try {
                    // Convert the response body to JSON so we can pull out user.id
                    String raw = new Gson().toJson(resp.body());
                    JSONObject json = new JSONObject(raw);

                    JSONObject user = json.getJSONObject("user");
                    String userId = user.getString("id"); // UUID from Supabase Auth
                    Log.d("SUPA", "SignUp userId = " + userId);

                    // NOTE: signUp may NOT return an access_token if email confirmation is enabled
                    // For our current RLS policy we allow anon inserts, so we don't need it.
                    saveProfileToSupabase(userId, email, fullName, phone, dob);

                } catch (Exception e) {
                    Log.e("SUPA", "Failed to parse signUp response", e);
                    validationText.setText("Error reading server response");
                }
            }

            @Override
            public void onFailure(Call<SignUpRes> call, Throwable t) {
                Log.e("SUPA", "signUp failure", t);
                validationText.setText("Network error during sign up");
            }
        });
    }

    // -----------------------------
    // 2. Insert into public.profiles
    // -----------------------------
    private void saveProfileToSupabase(String userId,
                                       String email,
                                       String fullName,
                                       String phone,
                                       String dob) {

        validationText.setText("Saving profile...");

        Map<String, Object> body = new HashMap<>();
        body.put("id", userId);          // must match auth user id
        body.put("email", email);
        body.put("full_name", fullName);
        body.put("phone", phone);
        body.put("dob", dob);
        body.put("role", "client");      // fixed for this screen

        profilesService.insertProfile("resolution=merge-duplicates", body)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call,
                                           Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            Log.d("SUPA", "Profile saved OK");
                            Toast.makeText(SignupActivityCustomer.this,
                                    "Account created successfully",
                                    Toast.LENGTH_SHORT).show();

                            if (response.isSuccessful()) {

                                Toast.makeText(SignupActivityCustomer.this,
                                        "Signup successful! Please log in.",
                                        Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(SignupActivityCustomer.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }

                        } else {
                            Log.e("SUPA", "Profile insert error: " + response.code());
                            validationText.setText("Error saving profile");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("SUPA", "Profile insert failure", t);
                        validationText.setText("Network error saving profile");
                    }
                });
    }

    // -----------------------------
    // Retrofit service interfaces
    // (If you already have these in separate files, delete these inner ones)
    // -----------------------------

    public interface AuthService {
        // POST https://<project>.supabase.co/auth/v1/signup
        // ApiClient should already set baseUrl + apikey + Authorization: Bearer <anon-key>
        @retrofit2.http.POST("auth/v1/signup")
        Call<SignUpRes> signUp(@retrofit2.http.Body Map<String, Object> body);
    }

    public interface ProfilesService {
        // POST https://<project>.supabase.co/rest/v1/profiles
        // ApiClient should add apikey & Authorization headers.
        @retrofit2.http.Headers({
                "Content-Type: application/json"
        })
        @retrofit2.http.POST("rest/v1/profiles")
        Call<ResponseBody> insertProfile(
                @retrofit2.http.Header("Prefer") String prefer,
                @retrofit2.http.Body Map<String, Object> body
        );
    }

    // -----------------------------
    // Model class for signUp response
    // (shape matches what Supabase returns in your logs)
    // If you already have this, remove this inner class.
    // -----------------------------
    public static class SignUpRes {
        public User user;

        public static class User {
            public String id;
            public String email;
        }
    }
}