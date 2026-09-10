package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Random;

public class GlyphColorManager {
    public static final int COLOR_MODE_UNIFIED = 0;
    public static final int COLOR_MODE_PER_SEGMENT = 1;
    public static final int COLOR_MODE_RANDOM = 2;

    // Stock Realme GT 5 calibrated hardware colors
    public static final int DEFAULT_PURPLE = 0xFFA820FF; // #A820FF (Matches GT 5 physical hardware purple)
    public static final int DEFAULT_BLUE = 0xFF71BBFF; // #71BBFF (Stock Realme GT Blue)
    public static final int COLOR_CYAN = 0xFF74BBFF;    // #74BBFF (Blue)
    public static final int COLOR_VIOLET = 0xFFA820FF;  // #A820FF (Purple)
    public static final int COLOR_PINK = 0xFFFF8173;    // #FF8173 (Red 2)
    public static final int COLOR_ORANGE = 0xFFFFBE15;  // #FFBE15 (Orange)
    public static final int COLOR_YELLOW = 0xFFFFFC3C;  // #FFFC3C (Yellow)
    public static final int COLOR_GREEN = 0xFF00FF1E;   // #00FF1E (Green)
    public static final int COLOR_WHITE = 0xFFFDFFFB;   // #FDFFFB (White)

    public static final int[] PRESET_PALETTE = new int[]{
            DEFAULT_BLUE, COLOR_CYAN, COLOR_VIOLET, COLOR_PINK,
            COLOR_ORANGE, COLOR_YELLOW, COLOR_GREEN, COLOR_WHITE
    };

    private static final String PREFS_NAME = "pulse_glyph_colors";
    private static final String KEY_MODE = "color_mode";
    private static final String KEY_UNIFIED = "color_unified";
    private static final String KEY_SEG_A = "color_seg_a";
    private static final String KEY_SEG_B = "color_seg_b";
    private static final String KEY_SEG_C = "color_seg_c";
    private static final String KEY_SEG_D = "color_seg_d";

    private static final Random sRandom = new Random();

    public static int getColorMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_MODE, COLOR_MODE_UNIFIED);
    }

    public static void setColorMode(Context context, int mode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MODE, mode).apply();
    }

    public static int getUnifiedColor(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_UNIFIED, DEFAULT_PURPLE);
    }

    public static void setUnifiedColor(Context context, int color) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_UNIFIED, color).apply();
    }

    public static int getSegmentColor(Context context, int segmentBitmask) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int mode = sp.getInt(KEY_MODE, COLOR_MODE_UNIFIED);
        if (mode == COLOR_MODE_RANDOM) {
            return getRandomColor();
        }
        if (mode == COLOR_MODE_UNIFIED) {
            return sp.getInt(KEY_UNIFIED, DEFAULT_PURPLE);
        }

        // Per-segment mode
        switch (segmentBitmask) {
            case RealmeGlyphDriver.LED_A:
                return sp.getInt(KEY_SEG_A, DEFAULT_PURPLE);
            case RealmeGlyphDriver.LED_B:
                return sp.getInt(KEY_SEG_B, DEFAULT_PURPLE);
            case RealmeGlyphDriver.LED_C:
                return sp.getInt(KEY_SEG_C, DEFAULT_PURPLE);
            case RealmeGlyphDriver.LED_D:
                return sp.getInt(KEY_SEG_D, DEFAULT_PURPLE);
            default:
                return sp.getInt(KEY_UNIFIED, DEFAULT_PURPLE);
        }
    }

    public static void setSegmentColor(Context context, int segmentBitmask, int color) {
        int validColor = ColorWheelView.toNearestHardwareColor(color);
        SharedPreferences.Editor edit = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        switch (segmentBitmask) {
            case RealmeGlyphDriver.LED_A:
                edit.putInt(KEY_SEG_A, validColor);
                break;
            case RealmeGlyphDriver.LED_B:
                edit.putInt(KEY_SEG_B, validColor);
                break;
            case RealmeGlyphDriver.LED_C:
                edit.putInt(KEY_SEG_C, validColor);
                break;
            case RealmeGlyphDriver.LED_D:
                edit.putInt(KEY_SEG_D, validColor);
                break;
        }
        edit.apply();
    }

    public static int getRandomColor() {
        return PRESET_PALETTE[sRandom.nextInt(PRESET_PALETTE.length)];
    }
}
