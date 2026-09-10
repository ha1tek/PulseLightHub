package com.antigravity.pulselight;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.provider.Settings;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class PulseLightManager {
    private static final String TAG = "PulseLightManager";
    public static final String SETTINGS_PKG = "com.android.settings";

    // System Setting Keys
    public static final String KEY_MUSIC_APPS = "breathing_light_music_apps";
    public static final String KEY_MUSIC_COLOR = "breathing_light_color_music";
    public static final String KEY_MUSIC_FLICKER_COLOR = "breathing_light_flicker_color_music";
    public static final String KEY_MULTI_LED_MUSIC_STATUS = "breathing_light_multi_led_music_status";

    public static final String KEY_ALWAYS_ON_SWITCH = "customize_breath_light_always_on";
    public static final String KEY_ALWAYS_ON_COLOR = "customize_breath_light_always_on_color";

    public static final String KEY_GT_COLOR = "breathing_light_color_gt";
    public static final String KEY_GT_SWITCH = "customize_breath_light_gt";

    public static final String KEY_CALL_COLOR = "breathing_light_color_call";
    public static final String KEY_NOTIFICATION_COLOR = "breathing_light_color_notification";
    public static final String KEY_ALARM_COLOR = "breathing_light_color_alarm";

    public static final String KEY_CHARGING_REMINDER = "customize_breath_light_charging_reminder";
    public static final String KEY_MASTER_SWITCH = "customize_breath_light_master_switch";
    public static final String KEY_OPLUS_MASTER_SWITCH = "oplus_breath_light_master_switch";
    public static final String KEY_TIME_LIMIT = "customize_breath_light_time";
    public static final String KEY_EXCLUSIVE_PREVIEW_COLOR = "breath_light_exclusive_preview_color";

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

    // =========================================================================
    // Generic Settings.Global Accessors
    // =========================================================================

    public static String getGlobalString(Context context, String key, String defaultVal) {
        try {
            String val = Settings.Global.getString(context.getContentResolver(), key);
            return (val != null && !val.trim().isEmpty()) ? val : defaultVal;
        } catch (Exception e) {
            Log.w(TAG, "Failed reading " + key, e);
            return defaultVal;
        }
    }

    public static boolean setGlobalString(Context context, String key, String value) {
        try {
            boolean ok = Settings.Global.putString(context.getContentResolver(), key, value);
            Log.d(TAG, "setGlobalString [" + key + " = " + value + "] -> " + ok);
            return ok;
        } catch (Exception e) {
            Log.e(TAG, "Failed writing " + key, e);
            return false;
        }
    }

    public static int getGlobalInt(Context context, String key, int defaultVal) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), key, defaultVal);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean setGlobalInt(Context context, String key, int value) {
        try {
            boolean ok = Settings.Global.putInt(context.getContentResolver(), key, value);
            Log.d(TAG, "setGlobalInt [" + key + " = " + value + "] -> " + ok);
            return ok;
        } catch (Exception e) {
            Log.e(TAG, "Failed writing int " + key, e);
            return false;
        }
    }

    // =========================================================================
    // Color Helpers
    // =========================================================================

    public static String colorToHex(int color) {
        return String.format(Locale.US, "#%06X", (0xFFFFFF & color));
    }

    public static int hexToColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            hex = hex.trim();
            if (!hex.startsWith("#")) {
                hex = "#" + hex;
            }
            return Color.parseColor(hex);
        } catch (Exception e) {
            return fallback;
        }
    }

    public static int getMusicColor(Context context) {
        String hex = getGlobalString(context, KEY_MUSIC_COLOR, "#FFFFF0");
        return hexToColor(hex, 0xFFFFFFF0);
    }

    public static boolean setMusicColor(Context context, int color) {
        return setGlobalString(context, KEY_MUSIC_COLOR, colorToHex(color));
    }

    public static int getMusicFlickerColor(Context context) {
        String hex = getGlobalString(context, KEY_MUSIC_FLICKER_COLOR, "#FF8175");
        return hexToColor(hex, 0xFFFF8175);
    }

    public static boolean setMusicFlickerColor(Context context, int color) {
        return setGlobalString(context, KEY_MUSIC_FLICKER_COLOR, colorToHex(color));
    }

    public static boolean isAlwaysOnEnabled(Context context) {
        return getGlobalInt(context, KEY_ALWAYS_ON_SWITCH, 0) == 1;
    }

    public static boolean setAlwaysOnEnabled(Context context, boolean enabled) {
        return setGlobalInt(context, KEY_ALWAYS_ON_SWITCH, enabled ? 1 : 0);
    }

    public static int getAlwaysOnColor(Context context) {
        String hex = getGlobalString(context, KEY_ALWAYS_ON_COLOR, "#71BBFF");
        return hexToColor(hex, 0xFF71BBFF);
    }

    public static boolean setAlwaysOnColor(Context context, int color) {
        return setGlobalString(context, KEY_ALWAYS_ON_COLOR, colorToHex(color));
    }

    public static int getGtColor(Context context) {
        String hex = getGlobalString(context, KEY_GT_COLOR, "#71BBFF");
        return hexToColor(hex, 0xFF71BBFF);
    }

    public static boolean setGtColor(Context context, int color) {
        return setGlobalString(context, KEY_GT_COLOR, colorToHex(color));
    }

    public static int getNotificationColor(Context context) {
        String hex = getGlobalString(context, KEY_NOTIFICATION_COLOR, "#FDFFFB");
        return hexToColor(hex, 0xFFFDFFFB);
    }

    public static boolean setNotificationColor(Context context, int color) {
        return setGlobalString(context, KEY_NOTIFICATION_COLOR, colorToHex(color));
    }

    public static int getCallColor(Context context) {
        String hex = getGlobalString(context, KEY_CALL_COLOR, "#FDFFFB");
        return hexToColor(hex, 0xFFFDFFFB);
    }

    public static boolean setCallColor(Context context, int color) {
        return setGlobalString(context, KEY_CALL_COLOR, colorToHex(color));
    }

    public static boolean ensureAllDaySupport(Context context) {
        setGlobalInt(context, KEY_MASTER_SWITCH, 1);
        setGlobalInt(context, KEY_OPLUS_MASTER_SWITCH, 1);
        return setGlobalString(context, KEY_TIME_LIMIT, "00002359");
    }

    public static boolean is24hMode(Context context) {
        String time = getGlobalString(context, KEY_TIME_LIMIT, "");
        return "00002359".equals(time);
    }

    public static boolean set24hMode(Context context, boolean enable) {
        setGlobalInt(context, KEY_MASTER_SWITCH, 1);
        setGlobalInt(context, KEY_OPLUS_MASTER_SWITCH, 1);
        return setGlobalString(context, KEY_TIME_LIMIT, enable ? "00002359" : "08002300");
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
