package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class GlyphColorManager {
    public static final int COLOR_MODE_UNIFIED = 0;
    public static final int COLOR_MODE_RANDOM = 2;

    // Stock Realme GT 5 calibrated hardware colors
    public static final int DEFAULT_PURPLE = 0xFFA820FF; // #A820FF (Matches GT 5 physical hardware purple)
    public static final int DEFAULT_BLUE = 0xFF71BBFF;   // #71BBFF (Stock Realme GT Blue)
    public static final int COLOR_CYAN = 0xFF74BBFF;     // #74BBFF (Blue)
    public static final int COLOR_VIOLET = 0xFFA820FF;   // #A820FF (Purple)
    public static final int COLOR_PINK = 0xFFFF8173;     // #FF8173 (Red 2)
    public static final int COLOR_ORANGE = 0xFFFFBE15;   // #FFBE15 (Orange)
    public static final int COLOR_YELLOW = 0xFFFFFC3C;   // #FFFC3C (Yellow)
    public static final int COLOR_GREEN = 0xFF00FF1E;    // #00FF1E (Green)
    public static final int COLOR_WHITE = 0xFFFDFFFB;    // #FDFFFB (White)

    public static final int[] PRESET_PALETTE = new int[]{
            DEFAULT_BLUE, COLOR_CYAN, COLOR_VIOLET, COLOR_PINK,
            COLOR_ORANGE, COLOR_YELLOW, COLOR_GREEN, COLOR_WHITE
    };

    // 11 vivid hardware-calibrated colors that directly match Qualcomm Lights HAL registers on Realme GT 5
    public static final int[] RAINBOW_COLORS = new int[]{
            0xFFFF3B30, // Красный (Red) -> HAL 0x8C790000
            0xFFFF9500, // Оранжевый (Orange) -> HAL 0x8BFFBE14
            0xFFFFEA00, // Желтый (Yellow) -> HAL 0x8BFFFC3B
            0xFF00E676, // Зеленый (Green) -> HAL 0x8D007400
            0xFF00FF1B, // Изумрудный (Emerald) -> HAL 0x8B00FF1B
            0xFF71BBFF, // Голубой (Cyan) -> HAL 0x8B00FF19
            0xFF2979FF, // Синий (Blue) -> HAL 0x8B74BBFF
            0xFFA820FF, // Фиолетовый (Purple) -> HAL 0x8C71BBFF
            0xFFFFA7FF, // Неоновый Розовый (Neon Pink) -> HAL 0x8BFFA7FF
            0xFFFDFFFB, // Белый (White) -> HAL 0x8BFDFFFB
            0xFFFF2D7A  // Розовый (Pink) -> HAL 0x8BFFFFF0
    };

    private static final String PREFS_NAME = "pulse_glyph_colors";
    private static final String KEY_MODE = "color_mode";
    private static final String KEY_UNIFIED = "color_unified";

    private static final Random sRandom = new Random();
    private static int sLastRainbowIndex = -1;

    public static int getColorMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_MODE, COLOR_MODE_UNIFIED);
    }

    public static void setColorMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MODE, mode).apply();
    }

    public static int getUnifiedColor(Context context) {
        if (context == null) return DEFAULT_PURPLE;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sp.contains(KEY_UNIFIED)) {
            return sp.getInt(KEY_UNIFIED, DEFAULT_PURPLE);
        }
        int alwaysOn = PulseLightManager.getAlwaysOnColor(context);
        if (alwaysOn != 0) {
            return alwaysOn;
        }
        return DEFAULT_PURPLE;
    }

    public static void setUnifiedColor(Context context, int color) {
        int clean = 0xFF000000 | (color & 0x00FFFFFF);
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_UNIFIED, clean).apply();
    }

    public static int getSegmentColor(Context context, int segmentBitmask) {
        return getUnifiedColor(context);
    }

    public static boolean isRainbowMode(Context context) {
        return getColorMode(context) == COLOR_MODE_RANDOM;
    }

    public static void setRainbowMode(Context context, boolean enabled) {
        setColorMode(context, enabled ? COLOR_MODE_RANDOM : COLOR_MODE_UNIFIED);
    }

    /**
     * Returns a new hardware-calibrated color for the rainbow effect.
     * Guaranteed to pick a different color than the previous one on every invocation.
     */
    public static synchronized int getNextRainbowColor() {
        int nextIndex;
        if (sLastRainbowIndex < 0) {
            nextIndex = sRandom.nextInt(RAINBOW_COLORS.length);
        } else {
            // Guaranteed different index from the last one
            int offset = 1 + sRandom.nextInt(RAINBOW_COLORS.length - 1);
            nextIndex = (sLastRainbowIndex + offset) % RAINBOW_COLORS.length;
        }
        sLastRainbowIndex = nextIndex;
        return RAINBOW_COLORS[nextIndex];
    }

    public static final int[] NEO_5_COLORS = new int[]{
            0xFFFDFFFB, // Белый
            0xFFFFA7FF, // Неоновый Розовый
            0xFFFF3B30, // Красный
            0xFFFF9500, // Оранжевый
            0xFFFFEA00, // Желтый
            0xFF00E676, // Зеленый
            0xFF00FF1B, // Изумрудный
            0xFF71BBFF, // Голубой
            0xFF2979FF, // Синий
            0xFFA820FF  // Фиолетовый
    };
    private static int sLastNeo5Index = -1;

    public static synchronized int getRandomNeo5Color() {
        int nextIndex;
        if (sLastNeo5Index < 0) {
            nextIndex = sRandom.nextInt(NEO_5_COLORS.length);
        } else {
            int offset = 1 + sRandom.nextInt(NEO_5_COLORS.length - 1);
            nextIndex = (sLastNeo5Index + offset) % NEO_5_COLORS.length;
        }
        sLastNeo5Index = nextIndex;
        return NEO_5_COLORS[nextIndex];
    }

    public static synchronized int getNextNeo5Color() {
        return getRandomNeo5Color();
    }

    public static int getRandomColor() {
        return getNextRainbowColor();
    }
}
