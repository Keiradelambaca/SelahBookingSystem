package com.example.selahbookingsystem.ui.base;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.example.selahbookingsystem.ui.customer.CustomerAppointmentActivity;
import com.example.selahbookingsystem.ui.customer.CustomerExploreActivity;
import com.example.selahbookingsystem.ui.customer.CustomerHomeActivity;
import com.example.selahbookingsystem.ui.customer.CustomerMessagesActivity;
import com.example.selahbookingsystem.ui.customer.CustomerProfileActivity;
import com.example.selahbookingsystem.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @IdRes
    protected abstract int getBottomNavMenuItemId();

    @Override
    public void setContentView(int layoutResID) {
        View fullView = LayoutInflater.from(this).inflate(R.layout.activity_base, null);
        FrameLayout container = fullView.findViewById(R.id.baseContent);

        LayoutInflater.from(this).inflate(layoutResID, container, true);

        super.setContentView(fullView);

        setupBottomNav();
    }

    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottomNav);
        if (bottomNav == null) return;

        // highlight correct item WITHOUT firing navigation
        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(getBottomNavMenuItemId());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == getBottomNavMenuItemId()) return true;
            Intent intent = null;

            if (id == R.id.nav_home) intent = new Intent(this, CustomerHomeActivity.class);
            else if (id == R.id.nav_appointments) intent = new Intent(this, CustomerAppointmentActivity.class);
            else if (id == R.id.nav_explore) intent = new Intent(this, CustomerExploreActivity.class);
            else if (id == R.id.nav_messages) intent = new Intent(this, CustomerMessagesActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, CustomerProfileActivity.class);

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }
}