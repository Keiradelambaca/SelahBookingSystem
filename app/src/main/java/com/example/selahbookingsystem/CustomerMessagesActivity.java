package com.example.selahbookingsystem;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;

public class CustomerMessagesActivity extends BaseActivity{

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_messages;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_messages);
    }
}