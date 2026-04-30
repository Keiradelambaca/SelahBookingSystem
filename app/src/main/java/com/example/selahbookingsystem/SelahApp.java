package com.example.selahbookingsystem;

import android.app.Application;

import com.example.selahbookingsystem.network.api.ApiClient;

public class SelahApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
