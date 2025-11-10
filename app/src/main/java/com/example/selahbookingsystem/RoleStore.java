package com.example.selahbookingsystem;

import android.content.Context;
import android.content.SharedPreferences;

public class RoleStore {
    private static final String PREF = "selah_roles";
    private static final String KEY_ROLE_PREFIX = "role_";           // role_{email}
    private static final String KEY_PHONE_PREFIX = "phone_";         // phone_{email}
    private static final String KEY_EIRCODE_PREFIX = "eircode_";     // eircode_{email}

    public enum Role { CUSTOMER, PROVIDER, UNKNOWN }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveCustomer(Context ctx, String email, String phone) {
        prefs(ctx).edit()
                .putString(KEY_ROLE_PREFIX + email, Role.CUSTOMER.name())
                .putString(KEY_PHONE_PREFIX + email, phone)
                .remove(KEY_EIRCODE_PREFIX + email)
                .apply();
    }

    public static void saveProvider(Context ctx, String email, String phone, String eircode) {
        prefs(ctx).edit()
                .putString(KEY_ROLE_PREFIX + email, Role.PROVIDER.name())
                .putString(KEY_PHONE_PREFIX + email, phone)
                .putString(KEY_EIRCODE_PREFIX + email, eircode)
                .apply();
    }

    public static Role getRole(Context ctx, String email) {
        String v = prefs(ctx).getString(KEY_ROLE_PREFIX + email, null);
        if (v == null) return Role.UNKNOWN;
        try { return Role.valueOf(v); } catch (Exception e) { return Role.UNKNOWN; }
    }

    public static String getPhone(Context ctx, String email) {
        return prefs(ctx).getString(KEY_PHONE_PREFIX + email, "");
    }

    public static String getEircode(Context ctx, String email) {
        return prefs(ctx).getString(KEY_EIRCODE_PREFIX + email, "");
    }
}