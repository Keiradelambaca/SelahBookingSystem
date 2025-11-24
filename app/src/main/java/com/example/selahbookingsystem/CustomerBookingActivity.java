package com.example.selahbookingsystem;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

public class CustomerBookingActivity extends BaseActivity{

        @Override
        protected int getBottomNavMenuItemId() {
        return R.id.nav_bookings;
    }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_bookings);
    }
}
