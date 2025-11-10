package com.example.selahbookingsystem;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenStore {

    private static final String PREF_NAME = "supabase_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    // Save token
    public static void save(Context context, String token) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    // Get token
    public static String get(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_ACCESS_TOKEN, null);
    }

    // Clear token (logout)
    public static void clear(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().remove(KEY_ACCESS_TOKEN).apply();
    }
}
