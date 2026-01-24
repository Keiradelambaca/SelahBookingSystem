package com.example.selahbookingsystem.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.GoTrueService;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.data.model.SignUpReq;
import com.example.selahbookingsystem.data.model.SignUpResponse;
import com.example.selahbookingsystem.network.service.SupabaseRestService;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivityCustomer extends AppCompatActivity {

    private static final String EXTRA_FROM_SIGNUP = "from_signup";
    private static final String EXTRA_PREFILL_EMAIL = "prefill_email";

    private EditText fullNameEt;
    private EditText emailEt;
    private EditText phoneEt;
    private EditText passwordEt;
    private EditText dobEt;
    private Button signupBtn;
    private TextView validationText;
    private TextView backToLoginText;

    // Match Supabase enum: user_role = 'client' | 'provider'
    private String userRole = "client";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_customer);

        // Role from chooser (optional)
        String fromIntent = getIntent().getStringExtra(SignupActivity1.EXTRA_ROLE);
        if (fromIntent != null && !fromIntent.trim().isEmpty()) {
            userRole = fromIntent;
        }

        // ==== Bind views (IDs match your XML) ====
        fullNameEt       = findViewById(R.id.nameEditText);
        emailEt          = findViewById(R.id.emailEditText);
        phoneEt          = findViewById(R.id.editTextPhone);
        dobEt            = findViewById(R.id.editTextDob);
        passwordEt       = findViewById(R.id.passwordEditText);
        signupBtn        = findViewById(R.id.signupButton);
        validationText   = findViewById(R.id.validationText);
        backToLoginText  = findViewById(R.id.backToLoginText);

        // Date picker
        dobEt.setOnClickListener(v -> showDatePicker());

        signupBtn.setOnClickListener(v -> {
            validationText.setText("");
            doSignup();
        });

        backToLoginText.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = 2004;
        int month = 9; // October index
        int day = 31;

        DatePickerDialog dpd = new DatePickerDialog(
                this,
                (DatePicker view, int y, int m, int d) -> {
                    String mm = String.format("%02d", m + 1);
                    String dd = String.format("%02d", d);
                    dobEt.setText(y + "-" + mm + "-" + dd);
                },
                year, month, day
        );
        dpd.show();
    }

    private void doSignup() {
        // Read values
        String fullName = fullNameEt.getText().toString().trim();
        String email    = emailEt.getText().toString().trim();
        String phone    = phoneEt.getText().toString().trim();
        String dob      = dobEt.getText().toString().trim();
        String password = passwordEt.getText().toString().trim();

        // ==== Validation ====
        if (TextUtils.isEmpty(fullName) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) ||
                TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(dob)) {

            validationText.setText("Please fill in all fields");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            validationText.setText("Enter a valid email.");
            return;
        }

        if (phone.length() == 0) {
            validationText.setText("Enter a valid phone number.");
            return;
        }

        if (password.length() < 6 || !password.matches(".*\\d.*") || password.contains(" ")) {
            validationText.setText("Password must be 6+ chars, include a number, no spaces.");
            return;
        }

        validationText.setText("Creating account...");
        signupBtn.setEnabled(false);

        // ==== Supabase auth sign-up ====
        GoTrueService auth = ApiClient.get().create(GoTrueService.class);
        SignUpReq body = new SignUpReq(email, password);

        auth.signUp(body).enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(Call<SignUpResponse> call, Response<SignUpResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().id == null) {
                    signupBtn.setEnabled(true);
                    String msg = "Sign-up failed";
                    try {
                        if (resp.errorBody() != null) {
                            msg = resp.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    validationText.setText(msg);
                    return;
                }

                String userId = resp.body().id;
                insertCustomerProfile(userId, fullName, email, phone, dob);
            }

            @Override
            public void onFailure(Call<SignUpResponse> call, Throwable t) {
                signupBtn.setEnabled(true);
                validationText.setText("Network error: " + t.getMessage());
            }
        });
    }

    private void insertCustomerProfile(String userId,
                                       String fullName,
                                       String email,
                                       String phone,
                                       String dob) {

        validationText.setText("Saving profile...");

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);
        SupabaseRestService.ProfileDto body = new SupabaseRestService.ProfileDto();
        body.id        = userId;
        body.full_name = fullName;
        body.email     = email;
        body.phone     = phone;
        body.dob       = dob;
        body.role      = userRole != null ? userRole : "client";

        api.insertProfile(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                signupBtn.setEnabled(true);

                if (!response.isSuccessful()) {
                    validationText.setText("Error saving profile.");
                    return;
                }

                // Local RoleStore for login routing
                RoleStore.saveCustomer(SignupActivityCustomer.this,
                        email, phone, fullName);

                Toast.makeText(SignupActivityCustomer.this,
                        "Account created successfully. Please log in.",
                        Toast.LENGTH_LONG).show();

                Intent i = new Intent(SignupActivityCustomer.this, MainActivity.class);
                i.putExtra(EXTRA_FROM_SIGNUP, true);
                i.putExtra(EXTRA_PREFILL_EMAIL, email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                signupBtn.setEnabled(true);
                validationText.setText("Profile error: " + t.getMessage());
            }
        });
    }
}
