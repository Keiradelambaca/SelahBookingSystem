package com.example.selahbookingsystem;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.selahbookingsystem.ApiClient;
import com.example.selahbookingsystem.GoTrueService;
import com.example.selahbookingsystem.SignUpReq;
import com.example.selahbookingsystem.SignUpRes;



public class SignupActivityServiceProvider1 extends AppCompatActivity {
    private static final String EXTRA_FROM_SIGNUP = "from_signup";
    private static final String EXTRA_PREFILL_EMAIL = "prefill_email";
    private TextView validationText, backToLoginText;
    private EditText emailEditText, editTextPhone, passwordEditText, passwordConfirmEditText, eircodeEditText;
    private Button signupButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_sp1);

        // Bind views
        validationText = findViewById(R.id.validationText);
        validationText          = findViewById(R.id.validationText);
        backToLoginText         = findViewById(R.id.backToLoginText);
        emailEditText           = findViewById(R.id.emailEditText);
        editTextPhone           = findViewById(R.id.editTextPhone);
        passwordEditText        = findViewById(R.id.passwordEditText);
        passwordConfirmEditText = findViewById(R.id.passwordConfirmEditText);
        eircodeEditText         = findViewById(R.id.eircodeEditText);
        signupButton            = findViewById(R.id.signupButton);

        // Actions
        signupButton.setOnClickListener(v -> attemptSignup());
        backToLoginText.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        // Edge-to-edge insets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }
    private void attemptSignup() {
        // Clear previous UI errors
        validationText.setText("");
        clearErrors();
        // Read & trim inputs
        String email    = safeText(emailEditText);
        String phone    = safeText(editTextPhone);
        String password      = safeText(passwordEditText);
        String confirm  = safeText(passwordConfirmEditText);
        String eircode  = safeText(eircodeEditText).toUpperCase(); // Eircodes are uppercase
        boolean ok = true;
        StringBuilder summary = new StringBuilder();
        // Email
        if (!isValidEmail(email)) {
            setError(emailEditText, "Enter a valid email address");
            append(summary, "Invalid email.");
            ok = false;
        }
        // Phone (general; tighten if you want only Irish mobile)
        if (!isValidPhone(phone)) {
            setError(editTextPhone, "Enter a valid phone number");
            append(summary, "Invalid phone number.");
            ok = false;
        }
        // Password strength
        if (!isValidPassword(password)) {
            setError(passwordEditText, "Min 6 chars, include a number, no spaces");
            append(summary, "Weak password.");
            ok = false;
        }
        // Confirm password
        if (!password.equals(confirm)) {
            setError(passwordConfirmEditText, "Passwords do not match");
            append(summary, "Passwords don't match.");
            ok = false;
        }
        // Eircode format (basic)
        if (!isValidEircode(eircode)) {
            setError(eircodeEditText, "Enter a valid Eircode");
            append(summary, "Invalid Eircode.");
            ok = false;
        }
        if (!ok) {
            validationText.setText(summary.toString());
            return;
        }

        // Supabase sign-up (async, navigate only on success)
        GoTrueService auth = ApiClient.get().create(GoTrueService.class);
        SignUpReq body = new SignUpReq(email, password);

        auth.signUp(body).enqueue(new retrofit2.Callback<SignUpRes>() {
            @Override
            public void onResponse(retrofit2.Call<SignUpRes> call,
                                   retrofit2.Response<SignUpRes> resp) {
                if (resp.isSuccessful() && resp.body() != null && resp.body().user != null) {
                    // Optional: save local metadata
                    RoleStore.saveProvider(SignupActivityServiceProvider1.this, email, phone, eircode);

                    // Return to Login with prefilled email
                    Intent i = new Intent(SignupActivityServiceProvider1.this, MainActivity.class);
                    i.putExtra(EXTRA_FROM_SIGNUP, true);
                    i.putExtra(EXTRA_PREFILL_EMAIL, email);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    finish();
                } else {
                    String msg = "Sign-up failed";
                    try { if (resp.errorBody() != null) msg = resp.errorBody().string(); } catch (Exception ignored) {}
                    validationText.setText(msg);
                }
            }

            @Override
            public void onFailure(retrofit2.Call<SignUpRes> call, Throwable t) {
                validationText.setText("Network error: " + t.getMessage());
            }
        });
    }

    // ----- Validation helpers -----
    private static boolean isValidEmail(String email) {
        return email.length() > 0 && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    private static boolean isValidPhone(String phone) {
        return phone.length() > 0 && Patterns.PHONE.matcher(phone).matches();
    }
    private static boolean isValidPassword(String pwd) {
        // At least 6 chars, at least one digit, no spaces
        return pwd.length() >= 6 && pwd.matches(".*\\d.*") && !pwd.contains(" ");
    }
    private static boolean isValidEircode(String code) {
        // Basic Eircode shape: Letter + 2 digits (routing key), optional space, 4 alphanumerics
        // Examples: D02 X285, A65F4E2
        // NOTE: This is a lenient regex; tighten with allowed-letter sets if needed.
        return code.matches("^[A-Za-z]\\d{2}\\s?[A-Za-z0-9]{4}$");
    }
    // ----- UI helpers -----
    private static String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private static void setError(EditText et, String msg) {
        et.setError(msg);
        et.requestFocus();
    }
    private static void append(StringBuilder sb, String msg) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(msg);
    }
    private void clearErrors() {
        emailEditText.setError(null);
        editTextPhone.setError(null);
        passwordEditText.setError(null);
        passwordConfirmEditText.setError(null);
        eircodeEditText.setError(null);
    }
}