package com.autoskip.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Small settings store shared by the service and the setup screen. */
final class Prefs {

    static final String KEY_ENABLED = "enabled";
    static final String KEY_SKIPS = "skips";

    private Prefs() {}

    static SharedPreferences get(Context context) {
        return context.getSharedPreferences("autoskip", Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context context) {
        return get(context).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    static int skips(Context context) {
        return get(context).getInt(KEY_SKIPS, 0);
    }

    static void addSkip(Context context) {
        SharedPreferences prefs = get(context);
        prefs.edit().putInt(KEY_SKIPS, prefs.getInt(KEY_SKIPS, 0) + 1).apply();
    }
}
