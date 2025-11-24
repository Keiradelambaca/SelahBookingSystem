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

public class SignupActivityCustomer extends AppCompatActivity {

    TextView validationText, backToLoginText;
    EditText nameEditText, emailEditText, editTextPhone, passwordEditText;
    Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup_customer);

        validationText = findViewById(R.id.validationText);
        backToLoginText = findViewById(R.id.backToLoginText);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        editTextPhone = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);

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

        String name = safeText(nameEditText);
        String email = safeText(emailEditText);
        String phone = safeText(editTextPhone);
        String password = safeText(passwordEditText);


        boolean ok = true;
        StringBuilder summary = new StringBuilder();

        if(name.isEmpty()) {
            setError(nameEditText, "Enter your name");
            append(summary, "Name is required");
            ok = false;
        }
        if (!isValidEmail(email)) {
            setError(emailEditText, "Enter a valid email address");
            append(summary, "Invalid email.");
            ok = false;
        }
        if (!isValidPhone(phone)) {
            setError(editTextPhone, "Enter a valid phone number");
            append(summary, "Invalid phone number.");
            ok = false;
        }
        if (!isValidPassword(password)) {
            setError(passwordEditText, "Min 6 chars, include a number, no spaces");
            append(summary, "Weak password.");
            ok = false;
        }

        if (!ok) {
            validationText.setText(summary.toString());
            return;
        }

        // TODO: Call Supabase sign-up here if you want real accounts now.
        // For demo we'll just save role locally:
        RoleStore.saveCustomer(this, email, phone, name);

        // Return to Login with prefill + banner
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("from_signup", true);
        i.putExtra("prefill_email", email);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // Helpers
    private static String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private static void append(StringBuilder sb, String msg) {
        if (sb.length() > 0) sb.append(" ");
        sb.append(msg);
    }
    private static void setError(EditText et, String msg) {
        et.setError(msg);
        et.requestFocus();
    }
    private void clearErrors() {
        nameEditText.setError(null);
        emailEditText.setError(null);
        editTextPhone.setError(null);
        passwordEditText.setError(null);
    }
    private static boolean isValidEmail(String email) {
        return email.length()>0 && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    private static boolean isValidPhone(String phone) {
        return phone.length()>0 && Patterns.PHONE.matcher(phone).matches();
    }
    private static boolean isValidPassword(String password) {
        return password.length()>=6 && password.matches(".*\\d.*") && !password.contains(" ");
    }
}