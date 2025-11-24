package com.example.selahbookingsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @IdRes
    protected abstract int getBottomNavMenuItemId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

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

        // Highlight the correct item for this activity
        int itemId = getBottomNavMenuItemId();
        if (itemId != 0) {
            bottomNav.setSelectedItemId(itemId);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                if (!(this instanceof CustomerHomeActivity)) {
                    startActivity(new Intent(this, CustomerHomeActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }
                return true;
            }

            if (id == R.id.nav_bookings) {
                if (!(this instanceof CustomerBookingActivity)) {
                    startActivity(new Intent(this, CustomerBookingActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                }
                return true;
            }

            if (id == R.id.nav_profile) {
                if (!(this instanceof CustomerProfileActivity)) {
                    startActivity(new Intent(this, CustomerProfileActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                }
                return true;
            }

            if (id == R.id.nav_explore) {
                if (!(this instanceof CustomerExploreActivity)) {
                    startActivity(new Intent(this, CustomerExploreActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                }
                return true;
            }

            if (id == R.id.nav_messages) {
                if (!(this instanceof CustomerMessagesActivity)) {
                    startActivity(new Intent(this, CustomerMessagesActivity.class));
                    overridePendingTransition(0,0);
                    finish();
                }
                return true;
            }

            return false;
        });
    }
}