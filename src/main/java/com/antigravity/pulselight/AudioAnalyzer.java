package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;

public class AudioAnalyzer {

    // Trigger Modes
    public static final int MODE_KICK_ONLY = 0;           // Nothing Phone Pure style
    public static final int MODE_KICK_AND_SNARE = 1;      // Kick = Top/Bottom, Snare = Sides
    public static final int MODE_FREQUENCY_SPLIT_4WAY = 2;// Sub=C, Kick=D, Snare=B, Hi-Hat=A
    public static final int MODE_ENERGY_LEVELS = 3;       // Staged energy (1 to 4 LEDs)

    // Presets
    public static final int PRESET_NOTHING_PURE = 0;
    public static final int PRESET_PHONK_808 = 1;
    public static final int PRESET_ROCK_DRUMS = 2;
    public static final int PRESET_EDM_CLUB = 3;
    public static final int PRESET_CUSTOM = 4;

    private static final String PREFS_NAME = "pulse_audio_engine";
    private static final String KEY_PRESET = "preset_index";
    private static final String KEY_TRIGGER_MODE = "trigger_mode";
    private static final String KEY_SENSITIVITY = "sensitivity_multiplier"; // 0.8 to 2.5
    private static final String KEY_DECAY_MS = "decay_ms";                  // 40 to 300 ms
    private static final String KEY_ENGINE_ENABLED = "engine_enabled";

    // Frequency bands (assuming ~48kHz sampling rate and 512 or 1024 bin FFT)
    // 0: Sub-bass (20 - 80 Hz)
    // 1: Kick punch (80 - 180 Hz)
    // 2: Snare / Clap (250 - 900 Hz)
    // 3: Hi-hat / Treble (4000 - 12000 Hz)
    private static final int BANDS_COUNT = 4;

    private final float[] mCurrentBands = new float[BANDS_COUNT];
    private final float[] mPreviousBands = new float[BANDS_COUNT];
    private final float[] mBandFlux = new float[BANDS_COUNT];
    private final float[] mMovingAvg = new float[BANDS_COUNT];

    // Band output intensities (0.0 to 1.0)
    private final float[] mBandIntensities = new float[BANDS_COUNT];
    private long[] mLastBeatTime = new long[BANDS_COUNT];

    // Active configuration
    private int mTriggerMode = MODE_KICK_ONLY;
    private float mSensitivity = 1.35f; // Threshold multiplier (lower = more sensitive)
    private int mDecayMs = 75;          // ms for flash to extinguish
    private int mPreset = PRESET_NOTHING_PURE;

    // Minimum silence floor to ignore background noise
    private static final float NOISE_FLOOR = 3.0f;
    private static final long MIN_BEAT_INTERVAL_MS = 65; // Debounce between successive beat attacks

    public static class AnalysisResult {
        public int activeLedMask = 0;
        public float intensity = 0f;
        public boolean isBeat = false;
        public float[] bandLevels = new float[4]; // For UI visualizer bars
    }

    private final AnalysisResult mResult = new AnalysisResult();

    public AudioAnalyzer(Context context) {
        loadSettings(context);
    }

    public void loadSettings(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mPreset = sp.getInt(KEY_PRESET, PRESET_NOTHING_PURE);
        mTriggerMode = sp.getInt(KEY_TRIGGER_MODE, MODE_KICK_ONLY);
        mSensitivity = sp.getFloat(KEY_SENSITIVITY, 1.35f);
        mDecayMs = sp.getInt(KEY_DECAY_MS, 75);
    }

    public void saveSettings(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putInt(KEY_PRESET, mPreset)
                .putInt(KEY_TRIGGER_MODE, mTriggerMode)
                .putFloat(KEY_SENSITIVITY, mSensitivity)
                .putInt(KEY_DECAY_MS, mDecayMs)
                .apply();
    }

    public static boolean isEngineEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENGINE_ENABLED, false);
    }

    public static void setEngineEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENGINE_ENABLED, enabled).apply();
    }

    public void applyPreset(int preset, Context context) {
        mPreset = preset;
        switch (preset) {
            case PRESET_NOTHING_PURE:
                mTriggerMode = MODE_KICK_ONLY;
                mSensitivity = 1.40f;
                mDecayMs = 70;
                break;
            case PRESET_PHONK_808:
                mTriggerMode = MODE_KICK_ONLY;
                mSensitivity = 1.15f; // Extra sensitive to sub-bass
                mDecayMs = 130;       // Long rolling 808 bass punch
                break;
            case PRESET_ROCK_DRUMS:
                mTriggerMode = MODE_KICK_AND_SNARE;
                mSensitivity = 1.30f;
                mDecayMs = 80;
                break;
            case PRESET_EDM_CLUB:
                mTriggerMode = MODE_FREQUENCY_SPLIT_4WAY;
                mSensitivity = 1.25f;
                mDecayMs = 90;
                break;
            case PRESET_CUSTOM:
            default:
                break;
        }
        saveSettings(context);
    }

    public int getPreset() { return mPreset; }
    public int getTriggerMode() { return mTriggerMode; }
    public void setTriggerMode(int mode) { mTriggerMode = mode; mPreset = PRESET_CUSTOM; }
    public float getSensitivity() { return mSensitivity; }
    public void setSensitivity(float s) { mSensitivity = s; mPreset = PRESET_CUSTOM; }
    public int getDecayMs() { return mDecayMs; }
    public void setDecayMs(int ms) { mDecayMs = ms; mPreset = PRESET_CUSTOM; }

    /**
     * Process raw FFT bytes from android.media.audiofx.Visualizer.
     * Guaranteed zero heap allocations per frame.
     */
    public AnalysisResult processFft(byte[] fft, int samplingRateHz) {
        if (fft == null || fft.length < 4) {
            mResult.activeLedMask = 0;
            mResult.intensity = 0f;
            mResult.isBeat = false;
            return mResult;
        }

        int n = fft.length;
        int halfN = n / 2;
        float binWidth = (float) samplingRateHz / (float) n;
        if (binWidth <= 0f) binWidth = 46.875f;

        // Reset band accumulators
        for (int i = 0; i < BANDS_COUNT; i++) {
            mCurrentBands[i] = 0f;
        }

        int subBassCount = 0;
        int kickCount = 0;
        int snareCount = 0;
        int trebleCount = 0;

        // Calculate magnitude for each bin and group into 4 bands
        for (int k = 1; k < halfN; k++) {
            byte r = fft[2 * k];
            byte im = fft[2 * k + 1];
            float mag = (float) Math.hypot(r, im);
            float freq = k * binWidth;

            if (freq >= 20f && freq < 80f) {
                mCurrentBands[0] += mag;
                subBassCount++;
            } else if (freq >= 80f && freq < 180f) {
                mCurrentBands[1] += mag;
                kickCount++;
            } else if (freq >= 220f && freq < 900f) {
                mCurrentBands[2] += mag;
                snareCount++;
            } else if (freq >= 3500f && freq < 12000f) {
                mCurrentBands[3] += mag;
                trebleCount++;
            }
        }

        if (subBassCount > 0) mCurrentBands[0] /= subBassCount;
        if (kickCount > 0) mCurrentBands[1] /= kickCount;
        if (snareCount > 0) mCurrentBands[2] /= snareCount;
        if (trebleCount > 0) mCurrentBands[3] /= trebleCount;

        // Copy normalized band levels for UI visualization
        for (int i = 0; i < BANDS_COUNT; i++) {
            mResult.bandLevels[i] = Math.min(1.0f, mCurrentBands[i] / 45.0f);
        }

        long now = System.currentTimeMillis();

        // 1. Compute Spectral Flux (Onset Detection) & Adaptive Moving Average
        boolean[] isBandHit = new boolean[BANDS_COUNT];
        float alpha = 0.88f; // Smoothing factor for moving average

        for (int i = 0; i < BANDS_COUNT; i++) {
            float diff = mCurrentBands[i] - mPreviousBands[i];
            mBandFlux[i] = Math.max(0f, diff); // Half-wave rectification
            mPreviousBands[i] = mCurrentBands[i];

            // Adaptive moving average of flux
            mMovingAvg[i] = alpha * mMovingAvg[i] + (1.0f - alpha) * mBandFlux[i];

            // Adaptive threshold
            float threshold = Math.max(NOISE_FLOOR, mMovingAvg[i] * mSensitivity);

            if (mBandFlux[i] > threshold && (now - mLastBeatTime[i] > MIN_BEAT_INTERVAL_MS)) {
                isBandHit[i] = true;
                mLastBeatTime[i] = now;
                mBandIntensities[i] = 1.0f; // Reset peak envelope
            } else {
                // Exponential decay envelope
                long elapsed = now - mLastBeatTime[i];
                if (elapsed >= mDecayMs) {
                    mBandIntensities[i] = 0f;
                } else {
                    mBandIntensities[i] = 1.0f - ((float) elapsed / mDecayMs);
                }
            }
        }

        // 2. Trigger mode dispatching
        int activeMask = 0;
        float maxIntensity = 0f;
        boolean beatTriggered = false;

        switch (mTriggerMode) {
            case MODE_KICK_ONLY: {
                // Combine Sub-bass & Kick punch
                boolean kickHit = isBandHit[0] || isBandHit[1];
                float kickInt = Math.max(mBandIntensities[0], mBandIntensities[1]);
                if (kickHit) beatTriggered = true;
                if (kickInt > 0.05f) {
                    activeMask = RealmeGlyphDriver.LED_ALL;
                    maxIntensity = kickInt;
                }
                break;
            }

            case MODE_KICK_AND_SNARE: {
                boolean kickHit = isBandHit[0] || isBandHit[1];
                boolean snareHit = isBandHit[2];
                if (kickHit || snareHit) beatTriggered = true;

                // Kick drives Top & Bottom (A & C)
                float kickInt = Math.max(mBandIntensities[0], mBandIntensities[1]);
                if (kickInt > 0.05f) {
                    activeMask |= (RealmeGlyphDriver.LED_A | RealmeGlyphDriver.LED_C);
                    maxIntensity = Math.max(maxIntensity, kickInt);
                }

                // Snare drives Sides (B & D)
                float snareInt = mBandIntensities[2];
                if (snareInt > 0.05f) {
                    activeMask |= (RealmeGlyphDriver.LED_B | RealmeGlyphDriver.LED_D);
                    maxIntensity = Math.max(maxIntensity, snareInt);
                }
                break;
            }

            case MODE_FREQUENCY_SPLIT_4WAY: {
                // Sub-Bass -> Bottom (C)
                if (mBandIntensities[0] > 0.05f) {
                    activeMask |= RealmeGlyphDriver.LED_C;
                    maxIntensity = Math.max(maxIntensity, mBandIntensities[0]);
                }
                // Kick -> Left (D)
                if (mBandIntensities[1] > 0.05f) {
                    activeMask |= RealmeGlyphDriver.LED_D;
                    maxIntensity = Math.max(maxIntensity, mBandIntensities[1]);
                }
                // Snare -> Right (B)
                if (mBandIntensities[2] > 0.05f) {
                    activeMask |= RealmeGlyphDriver.LED_B;
                    maxIntensity = Math.max(maxIntensity, mBandIntensities[2]);
                }
                // Treble / Hats -> Top (A)
                if (mBandIntensities[3] > 0.05f) {
                    activeMask |= RealmeGlyphDriver.LED_A;
                    maxIntensity = Math.max(maxIntensity, mBandIntensities[3]);
                }
                beatTriggered = isBandHit[0] || isBandHit[1] || isBandHit[2] || isBandHit[3];
                break;
            }

            case MODE_ENERGY_LEVELS: {
                float totalEnergy = (mCurrentBands[0] * 1.5f + mCurrentBands[1] * 1.2f + mCurrentBands[2] + mCurrentBands[3] * 0.5f);
                float normEnergy = Math.min(1.0f, totalEnergy / 60.0f);
                maxIntensity = normEnergy;

                if (normEnergy > 0.15f) activeMask |= RealmeGlyphDriver.LED_C; // Stage 1
                if (normEnergy > 0.40f) activeMask |= RealmeGlyphDriver.LED_D; // Stage 2
                if (normEnergy > 0.65f) activeMask |= RealmeGlyphDriver.LED_B; // Stage 3
                if (normEnergy > 0.85f) {
                    activeMask |= RealmeGlyphDriver.LED_A;                      // Stage 4 (Full peak)
                    beatTriggered = true;
                }
                break;
            }
        }

        mResult.activeLedMask = activeMask;
        mResult.intensity = maxIntensity;
        mResult.isBeat = beatTriggered;
        return mResult;
    }
}
