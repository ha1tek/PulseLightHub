package com.antigravity.pulselight;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

public class PulseLightManager {
    private static final String TAG = "PulseLightManager";
    public static final String KEY_MUSIC_APPS = "breathing_light_music_apps";
    public static final String SETTINGS_PKG = "com.android.settings";

    public static final String STOCK_MUSIC_APPS =
            "com.netease.cloudmusic:false,com.tencent.qqmusic:false,com.kugou.android:false," +
            "cn.kuwo.player:false,com.heytap.music:true,com.ximalaya.ting.android:false," +
            "bubei.tingshu:false,com.dragon.read:false,com.kugou.android.ringtone:false," +
            "com.xs.fm:false,com.shinyv.cnr:false,com.tencent.radio:false," +
            "com.ting.mp3.android:false,com.yibasan.soundrecorder:false," +
            "fm.qingting.qtradio:false,com.wondertek.migusm:false";

    public static boolean hasPermission(Context context) {
        return context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") 
                == PackageManager.PERMISSION_GRANTED;
    }

    public static Map<String, Boolean> getMusicAppsMap(Context context) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        try {
            String raw = Settings.Global.getString(context.getContentResolver(), KEY_MUSIC_APPS);
            if (raw == null || raw.trim().isEmpty()) {
                raw = STOCK_MUSIC_APPS;
            }
            String[] entries = raw.split(",");
            for (String entry : entries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;
                String[] parts = entry.split(":");
                if (parts.length >= 2) {
                    map.put(parts[0].trim(), "true".equalsIgnoreCase(parts[1].trim()));
                } else if (parts.length == 1) {
                    map.put(parts[0].trim(), true);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to read Settings.Global", e);
        }
        return map;
    }

    public static boolean isAppEnabled(Context context, String packageName) {
        Map<String, Boolean> map = getMusicAppsMap(context);
        return Boolean.TRUE.equals(map.get(packageName));
    }

    public static boolean setAppEnabled(Context context, String packageName, boolean enabled) {
        try {
            Map<String, Boolean> map = getMusicAppsMap(context);
            map.put(packageName, enabled);
            boolean ok = saveMap(context, map);
            Log.d(TAG, "setAppEnabled: " + packageName + " = " + enabled + " (saved=" + ok + ")");
            return ok;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set app enabled state", e);
            return false;
        }
    }

    public static boolean resetToStock(Context context) {
        try {
            boolean success = Settings.Global.putString(context.getContentResolver(), KEY_MUSIC_APPS, STOCK_MUSIC_APPS);
            killSettingsProcess(context);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset to stock", e);
            return false;
        }
    }

    public static int getActiveCount(Context context) {
        Map<String, Boolean> map = getMusicAppsMap(context);
        int count = 0;
        for (Boolean val : map.values()) {
            if (Boolean.TRUE.equals(val)) {
                count++;
            }
        }
        return count;
    }

    private static boolean saveMap(Context context, Map<String, Boolean> map) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Boolean> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append(entry.getKey()).append(":").append(entry.getValue() ? "true" : "false");
            first = false;
        }
        String serialized = sb.toString();
        Log.d(TAG, "Saving to Settings.Global: " + serialized);
        boolean success = Settings.Global.putString(context.getContentResolver(), KEY_MUSIC_APPS, serialized);
        Log.d(TAG, "Settings.Global write result: " + success);
        killSettingsProcess(context);
        return success;
    }

    public static void killSettingsProcess(Context context) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                am.killBackgroundProcesses(SETTINGS_PKG);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not kill background process for settings", e);
        }
        try {
            Runtime.getRuntime().exec("am force-stop " + SETTINGS_PKG);
        } catch (Throwable ignored) {}
    }
}
