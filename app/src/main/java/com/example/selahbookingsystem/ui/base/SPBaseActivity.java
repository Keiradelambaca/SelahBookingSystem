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
import com.example.selahbookingsystem.ui.provider.SPMessagesActivity;
import com.example.selahbookingsystem.ui.provider.SPProfileActivity;
import com.example.selahbookingsystem.ui.provider.SPSchedulingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.selahbookingsystem.ui.provider.SPHomeActivity;

public abstract class SPBaseActivity extends AppCompatActivity {

    protected abstract @LayoutRes int getLayoutResourceId();
    protected abstract @IdRes int getSelectedNavItemId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1) Inflate the base shell
        setContentView(R.layout.activity_base_provider);

        // 2) Inflate the child screen into the container
        FrameLayout container = findViewById(R.id.providerContentContainer);
        LayoutInflater.from(this).inflate(getLayoutResourceId(), container, true);

        // 3) Setup bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.providerBottomNav);
        bottomNav.setSelectedItemId(getSelectedNavItemId());

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == getSelectedNavItemId()) return true;

            if (id == R.id.nav_sp_profile) {
                startProviderActivity(SPProfileActivity.class);
                return true;
            } else if (id == R.id.nav_sp_scheduling) {
                startProviderActivity(SPSchedulingActivity.class);
                return true;
            } else if (id == R.id.nav_sp_home) {
                startProviderActivity(SPHomeActivity.class);
                return true;
            } else if (id == R.id.nav_sp_messages) {
                startProviderActivity(SPMessagesActivity.class);
                return true;
            } else if (id == R.id.nav_sp_clients) {
                startProviderActivity(SPClientsActivity.class);
                return true;
            }

            return false;
        });
    }

    private void startProviderActivity(Class<?> cls) {
        Intent i = new Intent(this, cls);

        // Pass along the same "email" extra if you’re using it for display/data
        String email = getIntent().getStringExtra("email");
        if (email != null) i.putExtra("email", email);

        // Prevent stacking lots of activities
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);

        // Keeps nav feeling snappy
        overridePendingTransition(0, 0);
    }
}

