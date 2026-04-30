package com.example.selahbookingsystem.ui.base;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.provider.SPClientsActivity;
import com.example.selahbookingsystem.ui.provider.SPHomeActivity;
import com.example.selahbookingsystem.ui.provider.SPMessagesActivity;
import com.example.selahbookingsystem.ui.provider.SPProfileActivity;
import com.example.selahbookingsystem.ui.provider.SPSchedulingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class SPBaseActivity extends AppCompatActivity {

    protected abstract @LayoutRes int getLayoutResourceId();

    protected abstract @IdRes int getSelectedNavItemId();

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_base_provider);

        FrameLayout container = findViewById(R.id.providerContentContainer);
        LayoutInflater.from(this).inflate(getLayoutResourceId(), container, true);

        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.providerBottomNav);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(getSelectedNavItemId());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == getSelectedNavItemId()) return true;

            Intent intent = null;

            if (id == R.id.nav_sp_profile) {
                intent = new Intent(this, SPProfileActivity.class);
            } else if (id == R.id.nav_sp_scheduling) {
                intent = new Intent(this, SPSchedulingActivity.class);
            } else if (id == R.id.nav_sp_home) {
                intent = new Intent(this, SPHomeActivity.class);
            } else if (id == R.id.nav_sp_messages) {
                intent = new Intent(this, SPMessagesActivity.class);
            } else if (id == R.id.nav_sp_clients) {
                intent = new Intent(this, SPClientsActivity.class);
            }

            if (intent != null) {
                String email = getIntent().getStringExtra("email");
                if (email != null) {
                    intent.putExtra("email", email);
                }

                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }
}