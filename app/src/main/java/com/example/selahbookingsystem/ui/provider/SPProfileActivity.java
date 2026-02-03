package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;

public class SPProfileActivity extends SPBaseActivity {

    @Override protected int getLayoutResourceId() { return R.layout.activity_sp_profile; }
    @Override protected int getSelectedNavItemId() { return R.id.nav_sp_profile; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
