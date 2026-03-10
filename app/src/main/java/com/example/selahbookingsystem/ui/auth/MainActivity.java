package com.example.selahbookingsystem.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.ProfileRoleDto;
import com.example.selahbookingsystem.data.model.RefreshReq;
import com.example.selahbookingsystem.data.model.SignInReq;
import com.example.selahbookingsystem.data.session.Session;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.GoTrueService;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.customer.CustomerHomeActivity;
import com.example.selahbookingsystem.ui.provider.SPHomeActivity;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "AUTH_ROUTE";

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView validationText, signupText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton      = findViewById(R.id.loginButton);
        validationText   = findViewById(R.id.validationText);
        signupText       = findViewById(R.id.signupText);

        // Auto-login: if we have a stored session, skip login screen
        if (TokenStore.hasSession(this)) {
            autoRouteFromStoredSession();
            return;
        }

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

        loginButton.setOnClickListener(v -> attemptLogin());

        signupText.setOnClickListener(v -> startActivity(new Intent(this, SignupActivity1.class)));

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v2, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v2.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void attemptLogin() {
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

        setLoading(true, "Signing in...");

        new Thread(() -> {
            try {
                GoTrueService auth = ApiClient.get().create(GoTrueService.class);
                Response<Session> r = auth.signIn("password", new SignInReq(email, password)).execute();

                if (!r.isSuccessful() || r.body() == null || r.body().accessToken == null) {
                    String err = (r.errorBody() != null) ? r.errorBody().string() : "Login failed";
                    runOnUiThread(() -> {
                        validationText.setText("Login failed: " + err);
                        setLoading(false, null);
                    });
                    return;
                }

                Session s = r.body();
                String userId = (s.user != null ? s.user.id : null);

                // Save tokens + expiry + userId + last email for auto-login
                TokenStore.save(
                        MainActivity.this,
                        s.accessToken,
                        s.refreshToken,
                        s.expiresIn,
                        userId,
                        email
                );

                // Optional: keep SessionManager in memory too (only if you actually have it)
                // com.example.selahbookingsystem.data.session.SessionManager.setSession(s);

                // IMPORTANT: route based on DATABASE role (profiles.role), NOT RoleStore-by-email
                runOnUiThread(() -> routeToCorrectHomeFromDbRole(userId));

            } catch (Exception ex) {
                runOnUiThread(() -> {
                    validationText.setText("Login error: " + ex.getMessage());
                    setLoading(false, null);
                });
            }
        }).start();
    }

    /**
     * Auto-login:
     * - If access token valid -> route by DB role
     * - Else refresh token -> save new session -> route by DB role
     */
    private void autoRouteFromStoredSession() {

        // If access token is still valid, route immediately (BUT STILL BY DB ROLE)
        if (TokenStore.isAccessTokenValid(this)) {
            String userId = TokenStore.getUserId(this);
            routeToCorrectHomeFromDbRole(userId);
            return;
        }

        // Otherwise refresh token in background
        setLoading(true, "Restoring session...");

        new Thread(() -> {
            try {
                GoTrueService auth = ApiClient.get().create(GoTrueService.class);

                String refreshToken = TokenStore.getRefreshToken(this);
                Response<Session> rr = auth.refreshToken(new RefreshReq(refreshToken)).execute();

                if (!rr.isSuccessful() || rr.body() == null || rr.body().accessToken == null) {
                    runOnUiThread(() -> {
                        TokenStore.clear(this);
                        setLoading(false, null);
                        validationText.setText("");
                    });
                    return;
                }

                Session s = rr.body();
                String userId = (s.user != null ? s.user.id : null);
                String email = TokenStore.getLastEmail(this);

                TokenStore.save(
                        MainActivity.this,
                        s.accessToken,
                        s.refreshToken,
                        s.expiresIn,
                        userId,
                        email
                );

                // com.example.selahbookingsystem.data.session.SessionManager.setSession(s);

                runOnUiThread(() -> routeToCorrectHomeFromDbRole(userId));

            } catch (Exception e) {
                runOnUiThread(() -> {
                    TokenStore.clear(this);
                    setLoading(false, null);
                    validationText.setText("");
                });
            }
        }).start();
    }

    /**
     * ALWAYS route by the role stored in Supabase profiles table.
     * This prevents "provider logs in -> client UI" caused by stale RoleStore/SharedPrefs.
     */
    private void routeToCorrectHomeFromDbRole(String userId) {

        if (userId == null || userId.trim().isEmpty()) {
            TokenStore.clear(this);
            setLoading(false, null);
            validationText.setText("Session invalid. Please log in again.");
            return;
        }

        Log.d(TAG, "routeToCorrectHomeFromDbRole userId=" + userId);

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);

        // NOTE: you must have this endpoint in SupabaseRestService:
        // Call<List<ProfileRoleDto>> getUserRole(@Query("id") String idEq, @Query("select") String select);
        api.getUserRole("eq." + userId, "role").enqueue(new Callback<List<ProfileRoleDto>>() {
            @Override
            public void onResponse(Call<List<ProfileRoleDto>> call, Response<List<ProfileRoleDto>> response) {

                String debugRole = (response.body() != null && !response.body().isEmpty())
                        ? response.body().get(0).role
                        : "EMPTY";

                Log.d(TAG, "getUserRole code=" + response.code() + " role=" + debugRole);

                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    setLoading(false, null);
                    validationText.setText("No role found for this user.");
                    loginButton.setEnabled(true);
                    return;
                }

                String roleRaw = response.body().get(0).role;
                if (roleRaw == null) {
                    setLoading(false, null);
                    validationText.setText("Role missing on profile.");
                    loginButton.setEnabled(true);
                    return;
                }

                String role = roleRaw.trim().toLowerCase(Locale.ROOT);

                Intent next;
                if (role.equals("provider") || role.equals("service_provider")) {
                    next = new Intent(MainActivity.this, SPHomeActivity.class);
                } else if (role.equals("client")) {
                    next = new Intent(MainActivity.this, CustomerHomeActivity.class);
                } else {
                    setLoading(false, null);
                    Toast.makeText(MainActivity.this, "Unknown role: " + role, Toast.LENGTH_LONG).show();
                    loginButton.setEnabled(true);
                    return;
                }

                next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(next);
                finish();
            }

            @Override
            public void onFailure(Call<List<ProfileRoleDto>> call, Throwable t) {
                Log.e(TAG, "getUserRole FAILED", t);
                setLoading(false, null);
                validationText.setText("Failed to load user role: " + t.getMessage());
                loginButton.setEnabled(true);
            }
        });
    }

    private void setLoading(boolean loading, String message) {
        loginButton.setEnabled(!loading);
        if (message != null) validationText.setText(message);
    }
}
