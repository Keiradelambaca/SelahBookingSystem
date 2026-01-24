package com.example.selahbookingsystem.ui.auth;

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

import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.GoTrueService;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.data.model.SignUpReq;
import com.example.selahbookingsystem.data.model.SignUpResponse;
import com.example.selahbookingsystem.network.service.SupabaseRestService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivityServiceProvider1 extends AppCompatActivity {

    private static final String EXTRA_FROM_SIGNUP = "from_signup";
    private static final String EXTRA_PREFILL_EMAIL = "prefill_email";

    private TextView validationText, backToLoginText;
    private EditText emailEditText, editTextPhone, businessNameEditText,
            passwordEditText, eircodeEditText;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_sp1);

        // Bind views (updated to include businessName, no confirm password)
        validationText       = findViewById(R.id.validationText);
        backToLoginText      = findViewById(R.id.backToLoginText);
        emailEditText        = findViewById(R.id.emailEditText);
        editTextPhone        = findViewById(R.id.editTextPhone);
        businessNameEditText = findViewById(R.id.businessNameEditText);
        passwordEditText     = findViewById(R.id.passwordEditText);
        eircodeEditText      = findViewById(R.id.eircodeEditText);
        signupButton         = findViewById(R.id.signupButton);

        signupButton.setOnClickListener(v -> attemptSignup());

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

    private void attemptSignup() {
        validationText.setText("");
        clearErrors();

        String email        = safeText(emailEditText);
        String phone        = safeText(editTextPhone);
        String businessName = safeText(businessNameEditText);
        String password     = safeText(passwordEditText);
        String eircode      = safeText(eircodeEditText).toUpperCase();

        boolean ok = true;
        StringBuilder summary = new StringBuilder();

        // Email
        if (!isValidEmail(email)) {
            setError(emailEditText, "Enter a valid email address");
            append(summary, "Invalid email.");
            ok = false;
        }
        // Phone
        if (!isValidPhone(phone)) {
            setError(editTextPhone, "Enter a valid phone number");
            append(summary, "Invalid phone number.");
            ok = false;
        }
        // Business name
        if (businessName.isEmpty()) {
            setError(businessNameEditText, "Enter your business name");
            append(summary, "Business name required.");
            ok = false;
        }
        // Password strength
        if (!isValidPassword(password)) {
            setError(passwordEditText, "Min 6 chars, include a number, no spaces");
            append(summary, "Weak password.");
            ok = false;
        }
        // Eircode format
        if (!isValidEircode(eircode)) {
            setError(eircodeEditText, "Enter a valid Eircode");
            append(summary, "Invalid Eircode.");
            ok = false;
        }

        if (!ok) {
            validationText.setText(summary.toString());
            return;
        }

        validationText.setText("Creating provider account...");
        signupButton.setEnabled(false);

        GoTrueService auth = ApiClient.get().create(GoTrueService.class);
        SignUpReq body = new SignUpReq(email, password);

        auth.signUp(body).enqueue(new Callback<SignUpResponse>() {
            @Override
            public void onResponse(Call<SignUpResponse> call,
                                   Response<SignUpResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().id == null) {
                    signupButton.setEnabled(true);
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
                insertProviderProfile(userId, email, phone, eircode, businessName);
            }

            @Override
            public void onFailure(Call<SignUpResponse> call, Throwable t) {
                signupButton.setEnabled(true);
                validationText.setText("Network error: " + t.getMessage());
            }
        });
    }

    private void insertProviderProfile(String userId,
                                       String email,
                                       String phone,
                                       String eircode,
                                       String businessName) {

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);

        SupabaseRestService.ProfileDto body = new SupabaseRestService.ProfileDto();
        body.id            = userId;
        body.email         = email;
        body.phone         = phone;
        body.role          = "provider";
        body.business_name = businessName;
        // If your table also stores eircode, add:
        // body.eircode = eircode;

        api.insertProfile(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                signupButton.setEnabled(true);

                if (!response.isSuccessful()) {
                    validationText.setText("Error saving provider profile.");
                    return;
                }

                // Local RoleStore
                RoleStore.saveProvider(SignupActivityServiceProvider1.this,
                        email, phone, eircode);

                Intent i = new Intent(SignupActivityServiceProvider1.this, MainActivity.class);
                i.putExtra(EXTRA_FROM_SIGNUP, true);
                i.putExtra(EXTRA_PREFILL_EMAIL, email);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                signupButton.setEnabled(true);
                validationText.setText("Provider profile error: " + t.getMessage());
            }
        });
    }

    // Validation helpers
    private static boolean isValidEmail(String email) {
        return email.length() > 0 && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private static boolean isValidPhone(String phone) {
        return phone.length() > 0 && Patterns.PHONE.matcher(phone).matches();
    }

    private static boolean isValidPassword(String pwd) {
        return pwd.length() >= 6 && pwd.matches(".*\\d.*") && !pwd.contains(" ");
    }

    private static boolean isValidEircode(String code) {
        return code.matches("^[A-Za-z]\\d{2}\\s?[A-Za-z0-9]{4}$");
    }

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
        businessNameEditText.setError(null);
        passwordEditText.setError(null);
        eircodeEditText.setError(null);
    }
}
