package com.example.selahbookingsystem.ui.customer;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.base.BaseActivity;

public class CustomerExploreActivity extends BaseActivity {

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_explore;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_explore);
    }
}