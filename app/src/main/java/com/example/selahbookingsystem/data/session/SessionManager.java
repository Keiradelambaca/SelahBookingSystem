package com.example.selahbookingsystem.data.session;

import androidx.annotation.Nullable;

public final class SessionManager {
    private static Session current;

    private SessionManager(){}

    public static void setSession(@Nullable Session s) { current = s; }
    @Nullable public static Session getSession() { return current; }

    @Nullable public static String getAccessToken() {
        return current != null ? current.accessToken : null;
    }

    @Nullable public static String getUserId() {
        return (current != null && current.user != null) ? current.user.id : null;
    }

    public static void clear() { current = null; }
}