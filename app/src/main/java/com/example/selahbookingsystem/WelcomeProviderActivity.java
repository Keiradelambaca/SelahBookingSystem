package com.example.selahbookingsystem;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeProviderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_provider);

        TextView emailTv = findViewById(R.id.emailText);
        TextView phoneTv = findViewById(R.id.phoneText);
        TextView eircodeTv = findViewById(R.id.eircodeText);

        String email = getIntent().getStringExtra("email");
        if (email == null) email = "";
        String phone = RoleStore.getPhone(this, email);
        String eircode = RoleStore.getEircode(this, email);

        emailTv.setText("Email: " + email);
        phoneTv.setText("Phone: " + (phone.isEmpty() ? "-" : phone));
        eircodeTv.setText("Eircode: " + (eircode.isEmpty() ? "-" : eircode));
    }
}