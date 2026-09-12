package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;

public class DeviceModelManager {

    public static final int MODEL_GT_5 = 0;
    public static final int MODEL_GT_NEO_5 = 1;

    private static final String PREFS_NAME = "pulse_device_prefs";
    private static final String KEY_DEVICE_MODEL = "device_model";

    public static int getDeviceModel(Context context) {
        if (context == null) return MODEL_GT_5;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_DEVICE_MODEL, MODEL_GT_5);
    }

    public static void setDeviceModel(Context context, int model) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_DEVICE_MODEL, model).apply();
    }

    public static boolean isGtNeo5(Context context) {
        return getDeviceModel(context) == MODEL_GT_NEO_5;
    }

    public static boolean isGt5(Context context) {
        return getDeviceModel(context) == MODEL_GT_5;
    }
}