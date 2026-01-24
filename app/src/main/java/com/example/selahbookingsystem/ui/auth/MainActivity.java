package com.example.selahbookingsystem.ui.auth;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.selahbookingsystem.ui.customer.CustomerHomeActivity;
import com.example.selahbookingsystem.network.service.GoTrueService;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.data.session.Session;
import com.example.selahbookingsystem.data.model.SignInReq;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.ui.provider.WelcomeProviderActivity;

public class MainActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginButton;
    TextView validationText, signupText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        emailEditText   = findViewById(R.id.emailEditText);
        passwordEditText= findViewById(R.id.passwordEditText);
        loginButton     = findViewById(R.id.loginButton);
        validationText  = findViewById(R.id.validationText);
        signupText      = findViewById(R.id.signupText);

        // If coming back from sign-up: prefill and prompt to log in
        boolean fromSignup = getIntent().getBooleanExtra("from_signup", false);
        if (fromSignup) {
            String prefill = getIntent().getStringExtra("prefill_email");
            if (prefill != null) {
                emailEditText.setText(prefill);
                emailEditText.setSelection(prefill.length());
            }
            validationText.setText("Account created. Please log in with your new credentials.");
            passwordEditText.requestFocus();
        }

        loginButton.setOnClickListener(v -> {
            validationText.setText("");

            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            boolean validEmail = email.length() > 0 && Patterns.EMAIL_ADDRESS.matcher(email).matches();
            boolean validPassword = password.length() >= 6 && password.matches(".*\\d.*") && !password.contains(" ");

            // Validate first; do NOT hit network if invalid
            if (!validEmail && !validPassword) {
                validationText.setText("Invalid email and password.");
                return;
            }
            if (!validEmail) {
                validationText.setText("Invalid email.");
                return;
            }
            if (!validPassword) {
                validationText.setText("Invalid password.");
                return;
            }

            // Disable button while logging in
            loginButton.setEnabled(false);
            validationText.setText("Signing in...");

            new Thread(() -> {
                try {
                    GoTrueService auth = ApiClient.get().create(GoTrueService.class);
                    retrofit2.Response<Session> r =
                            auth.signIn("password", new SignInReq(email, password)).execute();

                    if (!r.isSuccessful() || r.body() == null || r.body().accessToken == null) {
                        String err = (r.errorBody() != null) ? r.errorBody().string() : "Login failed";
                        runOnUiThread(() -> {
                            validationText.setText("Login failed: " + err);
                            loginButton.setEnabled(true);
                        });
                        return;
                    }

                    // Save JWT for future requests
                    TokenStore.save(MainActivity.this, r.body().accessToken);

                    // --- Save the Supabase authenticated user ID ---
                    String supabaseUserId = r.body().user.id;

                    SharedPreferences prefs = getSharedPreferences("selah_auth", MODE_PRIVATE);
                    prefs.edit()
                            .putString("auth_user_id", supabaseUserId)
                            .apply();


                    // >>> Route to correct welcome screen based on saved role <<<
                    runOnUiThread(() -> {
                        String emailUsed = email; // whatever variable you validated against
                        RoleStore.Role role = RoleStore.getRole(this, emailUsed);

                        Intent next;
                        if (role == RoleStore.Role.PROVIDER) {
                            next = new Intent(this, WelcomeProviderActivity.class);
                        } else {
                            next = new Intent(this, CustomerHomeActivity.class);
                        }

                        next.putExtra("EXTRA_USER_ID", supabaseUserId);
                        next.putExtra("email", emailUsed);
                        next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(next);
                    });

                } catch (Exception ex) {
                    runOnUiThread(() -> {
                        validationText.setText("Login error: " + ex.getMessage());
                        loginButton.setEnabled(true);
                    });
                }
            }).start();
        });

        signupText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity1.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v2, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v2.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}