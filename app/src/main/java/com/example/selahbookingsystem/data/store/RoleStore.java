package com.example.selahbookingsystem.data.store;

import android.content.Context;
import android.content.SharedPreferences;

public class RoleStore {
    private static final String PREF = "selah_roles";
    private static final String KEY_ROLE_PREFIX = "role_";           // role_{email}
    private static final String KEY_PHONE_PREFIX = "phone_";         // phone_{email}
    private static final String KEY_EIRCODE_PREFIX = "eircode_";     // eircode_{email}
    private static final String KEY_NAME_PREFIX = "name_";

    public enum Role { CUSTOMER, PROVIDER, UNKNOWN }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // CUSTOMER
    public static void saveCustomer(Context ctx, String email, String phone, String name) {
        prefs(ctx).edit()
                .putString(KEY_ROLE_PREFIX + email, Role.CUSTOMER.name())
                .putString(KEY_PHONE_PREFIX + email, phone)
                .putString(KEY_NAME_PREFIX + email, name)
                .remove(KEY_EIRCODE_PREFIX + email)
                .apply();
    }

    // PROVIDER
    public static void saveProvider(Context ctx, String email, String phone, String eircode) {
        prefs(ctx).edit()
                .putString(KEY_ROLE_PREFIX + email, Role.PROVIDER.name())
                .putString(KEY_PHONE_PREFIX + email, phone)
                .putString(KEY_EIRCODE_PREFIX + email, eircode)
                .apply();
    }


}