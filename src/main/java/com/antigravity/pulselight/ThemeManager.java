package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {

    private static final String PREFS_NAME = "pulse_theme_prefs";
    private static final String KEY_BG_COLOR = "theme_bg_color";
    private static final String KEY_ACCENT_COLOR = "theme_accent_color";

    public static class ColorOption {
        public final int color;
        public final String name;

        public ColorOption(int color, String name) {
            this.color = color;
            this.name = name;
        }
    }

    public static final ColorOption[] BG_OPTIONS = new ColorOption[]{
            new ColorOption(0xFF08090C, "Глубокий черный"),
            new ColorOption(0xFF12141A, "Темный графит"),
            new ColorOption(0xFF0F172A, "Темный сланец"),
            new ColorOption(0xFF18181B, "Обсидиан"),
            new ColorOption(0xFF0A1C16, "Темный изумруд"),
            new ColorOption(0xFF140E1C, "Космический")
    };

    public static final ColorOption[] ACCENT_OPTIONS = new ColorOption[]{
            new ColorOption(0xFFCCFF00, "Неоновый лайм"),
            new ColorOption(0xFF00F0FF, "Электрический циан"),
            new ColorOption(0xFF10B981, "Изумрудный"),
            new ColorOption(0xFFF59E0B, "Огненный янтарь"),
            new ColorOption(0xFFFF3B30, "Неоновый алый"),
            new ColorOption(0xFFA855F7, "Ультрафиолет"),
            new ColorOption(0xFFF8FAFC, "Арктический белый"),
            new ColorOption(0xFFFF2E93, "Неоновый розовый")
    };

    public interface OnThemeChangeListener {
        void onThemeChanged(int bgColor, int accentColor);
    }

    private static final List<OnThemeChangeListener> sListeners = new ArrayList<>();

    public static void addListener(OnThemeChangeListener listener) {
        if (listener != null && !sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static void removeListener(OnThemeChangeListener listener) {
        sListeners.remove(listener);
    }

    public static int getBackgroundColor(Context context) {
        if (context == null) return BG_OPTIONS[0].color;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_BG_COLOR, BG_OPTIONS[0].color);
    }

    public static void setBackgroundColor(Context context, int color) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BG_COLOR, color).apply();
        notifyListeners(color, getAccentColor(context));
    }

    public static int getAccentColor(Context context) {
        if (context == null) return ACCENT_OPTIONS[0].color;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_ACCENT_COLOR, ACCENT_OPTIONS[0].color);
    }

    public static void setAccentColor(Context context, int color) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_ACCENT_COLOR, color).apply();
        notifyListeners(getBackgroundColor(context), color);
    }

    public static int getCardBackgroundColor(int bgColor) {
        int r = Math.min(255, Color.red(bgColor) + 12);
        int g = Math.min(255, Color.green(bgColor) + 14);
        int b = Math.min(255, Color.blue(bgColor) + 20);
        return Color.rgb(r, g, b);
    }

    public static int getCardStrokeColor(int cardBgColor) {
        int r = Math.min(255, Color.red(cardBgColor) + 18);
        int g = Math.min(255, Color.green(cardBgColor) + 20);
        int b = Math.min(255, Color.blue(cardBgColor) + 26);
        return Color.rgb(r, g, b);
    }

    public static int getContrastTextColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.45 ? 0xFF0C101A : 0xFFFFFFFF;
    }

    public static Drawable createPillDrawable(int color, float radiusDp, Context context) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        float density = context != null ? context.getResources().getDisplayMetrics().density : 2.5f;
        gd.setCornerRadius(radiusDp * density);
        gd.setColor(color);
        return gd;
    }

    public static Drawable createCardDrawable(int cardBgColor, int strokeColor, float radiusDp, Context context) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        float density = context != null ? context.getResources().getDisplayMetrics().density : 2.5f;
        gd.setCornerRadius(radiusDp * density);
        gd.setColor(cardBgColor);
        return gd;
    }

    private static void notifyListeners(int bgColor, int accentColor) {
        for (OnThemeChangeListener l : sListeners) {
            try {
                l.onThemeChanged(bgColor, accentColor);
            } catch (Throwable ignored) {}
        }
    }
}
