package com.example.selahbookingsystem.data.store;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenStore {

    private static final String PREF_NAME = "supabase_prefs";

    private static final String KEY_ACCESS_TOKEN  = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT_MS = "expires_at_ms";
    private static final String KEY_LAST_EMAIL    = "last_email";
    private static final String KEY_USER_ID       = "user_id";

    public static void save(Context context,
                            String accessToken,
                            String refreshToken,
                            Integer expiresInSeconds,
                            String userId,
                            String email) {

        long expiresAtMs = 0L;
        if (expiresInSeconds != null) {
            // a small safety buffer so we refresh slightly before it expires
            expiresAtMs = System.currentTimeMillis() + (expiresInSeconds * 1000L) - 30_000L;
        }

        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_EXPIRES_AT_MS, expiresAtMs)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_LAST_EMAIL, email)
                .apply();
    }

    public static String getAccessToken(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCESS_TOKEN, null);
    }

    public static String getRefreshToken(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_REFRESH_TOKEN, null);
    }

    public static long getExpiresAtMs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_EXPIRES_AT_MS, 0L);
    }

    public static String getLastEmail(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_EMAIL, null);
    }

    public static String getUserId(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, null);
    }

    public static boolean hasSession(Context context) {
        return getRefreshToken(context) != null; // refresh token is the real "remember me"
    }

    public static boolean isAccessTokenValid(Context context) {
        String access = getAccessToken(context);
        if (access == null) return false;
        long expiresAt = getExpiresAtMs(context);
        return expiresAt == 0L || System.currentTimeMillis() < expiresAt;
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}

