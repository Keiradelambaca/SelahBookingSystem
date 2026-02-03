package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.widget.TextView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.store.RoleStore;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;

public class SPHomeActivity extends SPBaseActivity {

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_welcome_provider;
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_sp_home;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
