package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

public class AudioAnalyzer {

    // Trigger Modes
    public static final int MODE_KICK_ONLY = 0;           // Nothing Phone Pure style
    public static final int MODE_KICK_AND_SNARE = 1;      // Kick = Top/Bottom, Snare = Sides
    public static final int MODE_FREQUENCY_SPLIT_4WAY = 2;// Sub=C, Kick=D, Snare=B, Hi-Hat=A
    public static final int MODE_ENERGY_LEVELS = 3;       // Staged energy (1 to 4 LEDs)
    public static final int MODE_CUSTOM_PATTERN = 4;      // Uses selected 11-pattern trigger

    // Spectrum Modes
    public static final int SPECTRUM_MODE_NARROW = 0;     // 4 bands (SUB, KICK, SNARE, TREBLE)
    public static final int SPECTRUM_MODE_WIDE = 1;       // 10 bands (Sub -> Air)
    public static final int SPECTRUM_MODE_BOTH = 2;       // Both narrow & wide simultaneously

    // Studio Modes: Fast (4 bands + quick combinations) vs Deep (4 or 10 bands + individual per-band pattern configuration)
    public static final int STUDIO_MODE_FAST = 0;
    public static final int STUDIO_MODE_DEEP = 1;

    // Quick Trigger Presets (for Fast Mode)
    public static final int QUICK_PRESET_CLASSIC_SPLIT = 0;
    public static final int QUICK_PRESET_CIRCLE_4WAY = 1;
    public static final int QUICK_PRESET_CENTER_BASS = 2;
    public static final int QUICK_PRESET_KICK_BASS_ONLY = 3;
    public static final int QUICK_PRESET_STEREO_SIDES = 4;
    public static final int QUICK_PRESET_VERTICAL_PULSE = 5;
    public static final int QUICK_PRESET_DIAGONAL_CROSS = 6;
    public static final int QUICK_PRESET_CLUB_DRIVE = 7;
    public static final int QUICK_PRESET_FULL_AURA = 8;
    public static final int QUICK_PRESET_MINIMAL_BOTTOM = 9;
    public static final int QUICK_PRESET_TOP_VOCAL = 10;
    public static final int QUICK_PRESET_CLOCKWISE_WAVE = 11;

    // 11 Glyph Trigger Patterns + OFF
    public static final int PATTERN_OFF = -1;             // No LEDs
    public static final int PATTERN_TOP = 0;              // LED_A
    public static final int PATTERN_BOTTOM = 1;           // LED_C
    public static final int PATTERN_LEFT = 2;             // LED_D
    public static final int PATTERN_RIGHT = 3;            // LED_B
    public static final int PATTERN_TOP_BOTTOM = 4;       // LED_A | LED_C
    public static final int PATTERN_LEFT_RIGHT = 5;       // LED_D | LED_B
    public static final int PATTERN_TOP_LEFT = 6;         // LED_A | LED_D
    public static final int PATTERN_TOP_RIGHT = 7;        // LED_A | LED_B
    public static final int PATTERN_BOTTOM_LEFT = 8;      // LED_C | LED_D
    public static final int PATTERN_BOTTOM_RIGHT = 9;     // LED_C | LED_B
    public static final int PATTERN_ALL = 10;             // LED_ALL

    // Presets
    public static final int PRESET_NOTHING_PURE = 0;
    public static final int PRESET_PHONK_808 = 1;
    public static final int PRESET_ROCK_DRUMS = 2;
    public static final int PRESET_EDM_CLUB = 3;
    public static final int PRESET_CUSTOM = 4;

    // Audio Source
    public static final int SOURCE_AUTO = 0;
    public static final int SOURCE_MIC = 1;
    public static final int SOURCE_INTERNAL = 2;

    private static final String PREFS_NAME = "pulse_audio_engine";
    private static final String KEY_PRESET = "preset_index";
    private static final String KEY_TRIGGER_MODE = "trigger_mode";
    private static final String KEY_SENSITIVITY = "sensitivity_multiplier";
    private static final String KEY_DECAY_MS = "decay_ms";
    private static final String KEY_ENGINE_ENABLED = "engine_enabled";
    private static final String KEY_AUDIO_SOURCE = "audio_source";
    private static final String KEY_SPECTRUM_MODE = "spectrum_mode";
    private static final String KEY_STUDIO_ANALYSIS_MODE = "studio_analysis_mode";
    private static final String KEY_QUICK_TRIGGER_PRESET = "quick_trigger_preset";
    private static final String KEY_PATTERN_INDEX = "pattern_index";
    private static final String KEY_NARROW_PATTERN_PREFIX = "narrow_pattern_";
    private static final String KEY_WIDE_PATTERN_PREFIX = "wide_pattern_";
    private static final String KEY_ENABLE_ONSET = "enable_onset";
    private static final String KEY_ENABLE_LOUDNESS = "enable_loudness";
    private static final String KEY_LOUDNESS_GATE = "loudness_gate";
    private static final String KEY_ENABLE_CENTROID = "enable_centroid";
    private static final String KEY_CENTROID_MODE = "centroid_mode";
    private static final String KEY_NARROW_GAIN_PREFIX = "gain_narrow_";
    private static final String KEY_WIDE_GAIN_PREFIX = "gain_wide_";
    private static final String KEY_FFT_SIZE = "fft_size";
    private static final String KEY_USE_TUKEY = "use_tukey";
    private static final String KEY_ACTIVE_PRESET_ID = "active_preset_id";
    private static final String KEY_NARROW_THRESH_PREFIX = "thresh_narrow_";
    private static final String KEY_WIDE_THRESH_PREFIX = "thresh_wide_";
    private static final String KEY_DIAGRAM_INTERVAL_MS = "diagram_interval_ms";
    private static final String KEY_ENABLE_BAND_THRESHOLD = "enable_band_threshold";
    private static final String KEY_SPECTRUM_VISUAL_GAIN = "spectrum_visual_gain";
    private static final String KEY_NARROW_ENABLED_PREFIX = "narrow_enabled_";
    private static final String KEY_WIDE_ENABLED_PREFIX = "wide_enabled_";
    private static final String KEY_ENABLE_MIN_HOLD = "enable_min_hold";
    private static final String KEY_MIN_HOLD_MS = "min_hold_ms";
    private static final String KEY_ENABLE_MAX_HOLD = "enable_max_hold";
    private static final String KEY_MAX_HOLD_MS = "max_hold_ms";
    private static final String KEY_ENABLE_FINTERP = "enable_finterp";
    private static final String KEY_FINTERP_SPEED = "finterp_speed";
    private static final String KEY_ENABLE_VARIATION = "enable_variation";
    private static final String KEY_VARIATION_DEPTH = "variation_depth";
    private static final String KEY_ENABLE_LIMITER = "enable_limiter";

    public static final int NARROW_BANDS_COUNT = 4;
    public static final int WIDE_BANDS_COUNT = 12;

    private boolean mEnableBandThreshold = true;
    private float mSpectrumVisualGain = 1.40f; // Global spectrum visual sensitivity (0.5x to 3.0x)

    // Hold Time Constraints
    private boolean mEnableMinHoldTime = false;
    private int mMinHoldTimeMs = 40;
    private boolean mEnableMaxHoldTime = false;
    private int mMaxHoldTimeMs = 250;
    private long mLastBeatTime = 0;
    private boolean mIsBeatActive = false;
    private float mLastPeakIntensity = 0f;

    // Advanced Sound Filters from Audio Analysis Tools
    private boolean mEnableFInterp = false;
    private float mFInterpSpeed = 12.0f;
    private boolean mEnableRandomVariation = false;
    private float mRandomVariationDepth = 0.15f;
    private boolean mEnableLimiter = false;
    private float mSmoothedIntensity = 0f;
    private long mLastFrameTime = 0;
    private final java.util.Random mFilterRnd = new java.util.Random();

    // Auto-Calibration Configuration
    private int mCalibrationDurationMs = 10000;
    private boolean mCalibGains = true;
    private boolean mCalibThresholds = true;
    private boolean mCalibSensitivity = true;
    private boolean mCalibLoudnessGate = true;
    private boolean mCalibDecay = true;

    // Narrow bands (4): SUB (20-80), KICK (80-180), SNARE (220-900), TREBLE (3500-16000)
    private final float[] mNarrowBands = new float[NARROW_BANDS_COUNT];
    private final float[] mPrevNarrowBands = new float[NARROW_BANDS_COUNT];
    private final float[] mNarrowFlux = new float[NARROW_BANDS_COUNT];
    private final float[] mNarrowAvg = new float[NARROW_BANDS_COUNT];
    private final float[] mNarrowIntensities = new float[NARROW_BANDS_COUNT];
    private final long[] mLastNarrowBeatTime = new long[NARROW_BANDS_COUNT];
    private final float[] mNarrowGains = new float[NARROW_BANDS_COUNT];
    private final int[] mNarrowPatterns = new int[]{PATTERN_BOTTOM, PATTERN_TOP, PATTERN_LEFT_RIGHT, PATTERN_TOP};
    private final float[] mNarrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
    private final boolean[] mNarrowEnabled = new boolean[]{true, true, true, true};

    // Wide bands (12): 20 Hz to 20 kHz (30, 60, 120, 250, 500, 1k, 2k, 4k, 6k, 9k, 12k, 16k)
    private final float[] mWideBands = new float[WIDE_BANDS_COUNT];
    private final float[] mPrevWideBands = new float[WIDE_BANDS_COUNT];
    private final float[] mWideFlux = new float[WIDE_BANDS_COUNT];
    private final float[] mWideAvg = new float[WIDE_BANDS_COUNT];
    private final float[] mWideIntensities = new float[WIDE_BANDS_COUNT];
    private final long[] mLastWideBeatTime = new long[WIDE_BANDS_COUNT];
    private final float[] mWideGains = new float[WIDE_BANDS_COUNT];
    private final float[] mWideCurvePoints = new float[WIDE_BANDS_COUNT];
    private final float[] mWideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
    private final int[] mWidePatterns = new int[]{
            PATTERN_BOTTOM, PATTERN_TOP_BOTTOM, PATTERN_BOTTOM, PATTERN_LEFT_RIGHT,
            PATTERN_LEFT, PATTERN_RIGHT, PATTERN_LEFT_RIGHT, PATTERN_TOP_LEFT,
            PATTERN_TOP_RIGHT, PATTERN_TOP, PATTERN_TOP_BOTTOM, PATTERN_ALL
    };
    private final boolean[] mWideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};

    private int mDiagramIntervalMs = 1; // 1ms = 0.001 sec default (range: 1ms to 1000ms)

    private Context mContext;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public interface CalibrationCallback {
        void onCalibrationProgress(int secondsRemaining);
        void onCalibrationComplete();
    }

    private volatile boolean mIsCalibrating = false;
    private long mCalibrationStartTime = 0;
    private int mCalibLastReportedSec = -1;
    private CalibrationCallback mCalibrationCallback = null;
    private int mCalibFramesCount = 0;
    private final float[] mCalibNarrowMax = new float[NARROW_BANDS_COUNT];
    private final float[] mCalibNarrowFluxSum = new float[NARROW_BANDS_COUNT];
    private final float[] mCalibWideMax = new float[WIDE_BANDS_COUNT];
    private final float[] mCalibWideFluxSum = new float[WIDE_BANDS_COUNT];

    // Settings
    private int mTriggerMode = MODE_CUSTOM_PATTERN;
    private int mStudioAnalysisMode = STUDIO_MODE_FAST;
    private int mQuickTriggerPreset = 0;
    private float mSensitivity = 1.35f;
    private int mDecayMs = 75;
    private int mPreset = PRESET_CUSTOM;
    private int mAudioSource = SOURCE_INTERNAL;
    private int mSpectrumMode = SPECTRUM_MODE_NARROW;
    private int mCustomPattern = PATTERN_ALL;

    // Студийные фильтры детекции
    private boolean mEnableOnset = true;
    private boolean mEnableLoudnessGate = false;
    private float mLoudnessGateThreshold = 0.15f;
    private boolean mEnableCentroid = false;
    private int mCentroidMode = 0; // 0 = Bass focus, 1 = Treble focus

    private float mCurrentRms = 0f;
    private float mCurrentCentroid = 0f;

    // Radix-2 FFT Buffers (Supports up to 4096-sample high resolution)
    public static final int MAX_FFT_SIZE = 4096;
    private int mFftSize = 4096;
    private boolean mUseTukeyWindow = true;
    private String mActivePresetId = AudioPresetManager.PRESET_STUDIO_PRO_ID;

    private final float[] mPcmReal = new float[MAX_FFT_SIZE];
    private final float[] mPcmImag = new float[MAX_FFT_SIZE];
    private final float[] mHannWindow = new float[MAX_FFT_SIZE];
    private final float[] mTukeyWindow = new float[MAX_FFT_SIZE];

    private static final float[] NARROW_WEIGHTS = { 1.0f, 1.15f, 1.50f, 2.40f };
    private static final float[] WIDE_WEIGHTS = { 1.0f, 1.05f, 1.12f, 1.25f, 1.40f, 1.65f, 1.95f, 2.30f, 2.70f, 3.15f, 3.65f, 4.20f };
    private float mNarrowVisualCeiling = 0.25f;
    private float mWideVisualCeiling = 0.22f;

    private static final float NOISE_FLOOR = 0.025f;
    private static final long MIN_BEAT_INTERVAL_MS = 60;

    public static class AnalysisResult {
        public int activeLedMask = 0;
        public float intensity = 0f;
        public boolean isBeat = false;
        public float[] bandLevels = new float[NARROW_BANDS_COUNT];
        public float[] wideLevels = new float[WIDE_BANDS_COUNT];
        public float[] wideCurve = new float[WIDE_BANDS_COUNT];
        public boolean[] narrowEnabled = new boolean[NARROW_BANDS_COUNT];
        public boolean[] wideEnabled = new boolean[WIDE_BANDS_COUNT];
        public float rmsLoudness = 0f;
        public float spectralCentroid = 0f;
        public int spectrumMode = SPECTRUM_MODE_NARROW;
    }

    private final AnalysisResult mResult = new AnalysisResult();

    public AnalysisResult getEmptyResult() {
        mResult.activeLedMask = 0;
        mResult.intensity = 0f;
        mResult.isBeat = false;
        mResult.rmsLoudness = 0f;
        mResult.spectralCentroid = 0f;
        mResult.spectrumMode = mSpectrumMode;
        System.arraycopy(mNarrowEnabled, 0, mResult.narrowEnabled, 0, NARROW_BANDS_COUNT);
        System.arraycopy(mWideEnabled, 0, mResult.wideEnabled, 0, WIDE_BANDS_COUNT);
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mResult.bandLevels[i] = 0f;
            mNarrowBands[i] = 0f;
            mNarrowIntensities[i] = 0f;
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mResult.wideLevels[i] = 0f;
            mResult.wideCurve[i] = 0f;
            mWideBands[i] = 0f;
            mWideIntensities[i] = 0f;
        }
        return mResult;
    }

    public AudioAnalyzer(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }
        float alphaT = 0.5f;
        int edgeLen = (int) (alphaT * MAX_FFT_SIZE / 2.0f);

        for (int i = 0; i < MAX_FFT_SIZE; i++) {
            mHannWindow[i] = (float) (0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (MAX_FFT_SIZE - 1))));

            if (i < edgeLen) {
                mTukeyWindow[i] = (float) (0.5 * (1.0 + Math.cos(Math.PI * (2.0 * i / (alphaT * MAX_FFT_SIZE) - 1.0))));
            } else if (i <= MAX_FFT_SIZE - edgeLen) {
                mTukeyWindow[i] = 1.0f;
            } else {
                mTukeyWindow[i] = (float) (0.5 * (1.0 + Math.cos(Math.PI * (2.0 * i / (alphaT * MAX_FFT_SIZE) - 2.0 / alphaT + 1.0))));
            }
        }
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mNarrowGains[i] = 1.0f;
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mWideGains[i] = 1.0f;
        }
        loadSettings(context);
    }

    public void loadSettings(Context context) {
        if (context == null) return;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mPreset = sp.getInt(KEY_PRESET, PRESET_NOTHING_PURE);
        mTriggerMode = sp.getInt(KEY_TRIGGER_MODE, MODE_CUSTOM_PATTERN);
        mSensitivity = sp.getFloat(KEY_SENSITIVITY, 1.35f);
        mDecayMs = sp.getInt(KEY_DECAY_MS, 75);
        mAudioSource = sp.getInt(KEY_AUDIO_SOURCE, SOURCE_INTERNAL);
        mSpectrumMode = sp.getInt(KEY_SPECTRUM_MODE, SPECTRUM_MODE_NARROW);
        mStudioAnalysisMode = sp.getInt(KEY_STUDIO_ANALYSIS_MODE, STUDIO_MODE_FAST);
        mQuickTriggerPreset = sp.getInt(KEY_QUICK_TRIGGER_PRESET, 0);
        mCustomPattern = sp.getInt(KEY_PATTERN_INDEX, PATTERN_ALL);

        mEnableOnset = sp.getBoolean(KEY_ENABLE_ONSET, true);
        mEnableLoudnessGate = sp.getBoolean(KEY_ENABLE_LOUDNESS, false);
        mLoudnessGateThreshold = sp.getFloat(KEY_LOUDNESS_GATE, 0.15f);
        mEnableCentroid = sp.getBoolean(KEY_ENABLE_CENTROID, false);
        mCentroidMode = sp.getInt(KEY_CENTROID_MODE, 0);

        mEnableMinHoldTime = sp.getBoolean(KEY_ENABLE_MIN_HOLD, false);
        mMinHoldTimeMs = sp.getInt(KEY_MIN_HOLD_MS, 40);
        mEnableMaxHoldTime = sp.getBoolean(KEY_ENABLE_MAX_HOLD, false);
        mMaxHoldTimeMs = sp.getInt(KEY_MAX_HOLD_MS, 250);
        mEnableFInterp = sp.getBoolean(KEY_ENABLE_FINTERP, false);
        mFInterpSpeed = sp.getFloat(KEY_FINTERP_SPEED, 12.0f);
        mEnableRandomVariation = sp.getBoolean(KEY_ENABLE_VARIATION, false);
        mRandomVariationDepth = sp.getFloat(KEY_VARIATION_DEPTH, 0.15f);
        mEnableLimiter = sp.getBoolean(KEY_ENABLE_LIMITER, false);

        mFftSize = sp.getInt(KEY_FFT_SIZE, 4096);
        mUseTukeyWindow = sp.getBoolean(KEY_USE_TUKEY, true);
        mActivePresetId = sp.getString(KEY_ACTIVE_PRESET_ID, AudioPresetManager.PRESET_STUDIO_PRO_ID);
        mDiagramIntervalMs = sp.getInt(KEY_DIAGRAM_INTERVAL_MS, 10);
        mEnableBandThreshold = sp.getBoolean(KEY_ENABLE_BAND_THRESHOLD, true);
        mSpectrumVisualGain = sp.getFloat(KEY_SPECTRUM_VISUAL_GAIN, 1.40f);

        int[] defNarrowPatterns = {PATTERN_BOTTOM, PATTERN_TOP, PATTERN_LEFT_RIGHT, PATTERN_TOP};
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mNarrowGains[i] = sp.getFloat(KEY_NARROW_GAIN_PREFIX + i, 1.0f);
            mNarrowPatterns[i] = sp.getInt(KEY_NARROW_PATTERN_PREFIX + i, defNarrowPatterns[i]);
            mNarrowThresholds[i] = sp.getFloat(KEY_NARROW_THRESH_PREFIX + i, 0.15f);
            mNarrowEnabled[i] = sp.getBoolean(KEY_NARROW_ENABLED_PREFIX + i, true);
        }

        int[] defWidePatterns = {
                PATTERN_BOTTOM, PATTERN_TOP_BOTTOM, PATTERN_BOTTOM, PATTERN_LEFT_RIGHT,
                PATTERN_LEFT, PATTERN_RIGHT, PATTERN_LEFT_RIGHT, PATTERN_TOP_LEFT,
                PATTERN_TOP_RIGHT, PATTERN_TOP, PATTERN_TOP_BOTTOM, PATTERN_ALL
        };
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mWideGains[i] = sp.getFloat(KEY_WIDE_GAIN_PREFIX + i, 1.0f);
            mWidePatterns[i] = sp.getInt(KEY_WIDE_PATTERN_PREFIX + i, defWidePatterns[i]);
            mWideThresholds[i] = sp.getFloat(KEY_WIDE_THRESH_PREFIX + i, 0.15f);
            mWideEnabled[i] = sp.getBoolean(KEY_WIDE_ENABLED_PREFIX + i, true);
        }
    }

    public void resetToDefaults(Context context) {
        mPreset = PRESET_NOTHING_PURE;
        mTriggerMode = MODE_CUSTOM_PATTERN;
        mSensitivity = 1.35f;
        mDecayMs = 75;
        mAudioSource = SOURCE_INTERNAL;
        mSpectrumMode = SPECTRUM_MODE_NARROW;
        mStudioAnalysisMode = STUDIO_MODE_FAST;
        mQuickTriggerPreset = 0;
        mCustomPattern = PATTERN_ALL;

        mEnableOnset = true;
        mEnableLoudnessGate = false;
        mLoudnessGateThreshold = 0.15f;
        mEnableCentroid = false;
        mCentroidMode = 0;

        mEnableMinHoldTime = false;
        mMinHoldTimeMs = 40;
        mEnableMaxHoldTime = false;
        mMaxHoldTimeMs = 250;
        mEnableFInterp = false;
        mFInterpSpeed = 12.0f;
        mEnableRandomVariation = false;
        mRandomVariationDepth = 0.15f;
        mEnableLimiter = false;

        mFftSize = 4096;
        mUseTukeyWindow = true;
        mActivePresetId = AudioPresetManager.PRESET_STUDIO_PRO_ID;
        mDiagramIntervalMs = 10;
        mEnableBandThreshold = true;
        mSpectrumVisualGain = 1.40f;

        int[] defNarrowPatterns = {PATTERN_BOTTOM, PATTERN_TOP, PATTERN_LEFT_RIGHT, PATTERN_TOP};
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mNarrowGains[i] = 1.0f;
            mNarrowPatterns[i] = defNarrowPatterns[i];
            mNarrowThresholds[i] = 0.15f;
            mNarrowEnabled[i] = true;
        }

        int[] defWidePatterns = {
                PATTERN_BOTTOM, PATTERN_TOP_BOTTOM, PATTERN_BOTTOM, PATTERN_LEFT_RIGHT,
                PATTERN_LEFT, PATTERN_RIGHT, PATTERN_LEFT_RIGHT, PATTERN_TOP_LEFT,
                PATTERN_TOP_RIGHT, PATTERN_TOP, PATTERN_TOP_BOTTOM, PATTERN_ALL
        };
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mWideGains[i] = 1.0f;
            mWidePatterns[i] = defWidePatterns[i];
            mWideThresholds[i] = 0.12f;
            mWideEnabled[i] = true;
        }

        if (context != null) {
            saveSettings(context);
            AudioPresetManager.setActivePresetId(context, mActivePresetId);
        }
    }

    public void generateRandomConfig(Context context) {
        java.util.Random rnd = new java.util.Random();
        mPreset = PRESET_CUSTOM;
        mTriggerMode = MODE_CUSTOM_PATTERN;

        // Mode: 50% Fast, 50% Deep
        mStudioAnalysisMode = rnd.nextBoolean() ? STUDIO_MODE_FAST : STUDIO_MODE_DEEP;
        mSpectrumMode = rnd.nextBoolean() ? SPECTRUM_MODE_NARROW : SPECTRUM_MODE_WIDE;

        // Punchy, musical sensitivity (1.20 - 1.45) and responsive decay (65 - 100 ms)
        mSensitivity = (float) (Math.round((1.20f + rnd.nextFloat() * 0.25f) * 100.0) / 100.0);
        mDecayMs = 65 + rnd.nextInt(36);

        mEnableOnset = true;
        mEnableLoudnessGate = (rnd.nextInt(4) == 0); // 25% chance of loudness gate
        mLoudnessGateThreshold = (float) (Math.round((0.05f + rnd.nextFloat() * 0.05f) * 100.0) / 100.0);
        mEnableCentroid = false;
        mCentroidMode = 0;

        int[] fftSizes = {1024, 2048, 4096};
        mFftSize = fftSizes[rnd.nextInt(fftSizes.length)];
        mUseTukeyWindow = rnd.nextBoolean();

        mQuickTriggerPreset = rnd.nextInt(12);

        // Musical pattern palettes (Strictly playable, symmetrical, zero dead beats)
        int[] bassPatterns = {PATTERN_ALL, PATTERN_TOP_BOTTOM, PATTERN_BOTTOM};
        int[] midPatterns = {PATTERN_LEFT_RIGHT, PATTERN_TOP_BOTTOM, PATTERN_TOP_LEFT, PATTERN_BOTTOM_RIGHT, PATTERN_ALL};
        int[] highPatterns = {PATTERN_TOP, PATTERN_LEFT_RIGHT, PATTERN_TOP_RIGHT, PATTERN_TOP_LEFT, PATTERN_ALL};

        // Narrow bands (4): SUB, KICK, SNARE, TREBLE
        mNarrowGains[0] = (float) (Math.round((1.2f + rnd.nextFloat() * 0.7f) * 100.0) / 100.0);
        mNarrowGains[1] = (float) (Math.round((1.2f + rnd.nextFloat() * 0.7f) * 100.0) / 100.0);
        mNarrowGains[2] = (float) (Math.round((0.9f + rnd.nextFloat() * 0.7f) * 100.0) / 100.0);
        mNarrowGains[3] = (float) (Math.round((0.8f + rnd.nextFloat() * 0.7f) * 100.0) / 100.0);

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            // Safe, responsive threshold: 7% - 15% (never suffocates beats)
            mNarrowThresholds[i] = (float) (Math.round((0.07f + rnd.nextFloat() * 0.08f) * 100.0) / 100.0);
        }
        mNarrowPatterns[0] = bassPatterns[rnd.nextInt(bassPatterns.length)];
        mNarrowPatterns[1] = bassPatterns[rnd.nextInt(bassPatterns.length)];
        mNarrowPatterns[2] = midPatterns[rnd.nextInt(midPatterns.length)];
        mNarrowPatterns[3] = highPatterns[rnd.nextInt(highPatterns.length)];

        // Wide bands (12): 30Hz -> 16kHz
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            float baseGain = (i <= 2) ? (1.2f + rnd.nextFloat() * 0.7f) : (0.8f + rnd.nextFloat() * 0.7f);
            mWideGains[i] = (float) (Math.round(baseGain * 100.0) / 100.0);
            mWideThresholds[i] = (float) (Math.round((0.06f + rnd.nextFloat() * 0.08f) * 100.0) / 100.0);

            if (i <= 2) {
                mWidePatterns[i] = bassPatterns[rnd.nextInt(bassPatterns.length)];
            } else if (i <= 6) {
                mWidePatterns[i] = midPatterns[rnd.nextInt(midPatterns.length)];
            } else {
                mWidePatterns[i] = highPatterns[rnd.nextInt(highPatterns.length)];
            }
        }

        if (mStudioAnalysisMode == STUDIO_MODE_FAST) {
            applyQuickTriggerPreset(mQuickTriggerPreset);
        }

        if (context != null) {
            saveSettings(context);
        }
    }

    public void saveSettings(Context context) {
        if (context == null) return;
        SharedPreferences.Editor ed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        ed.putInt(KEY_PRESET, mPreset)
                .putInt(KEY_TRIGGER_MODE, mTriggerMode)
                .putFloat(KEY_SENSITIVITY, mSensitivity)
                .putInt(KEY_DECAY_MS, mDecayMs)
                .putInt(KEY_AUDIO_SOURCE, mAudioSource)
                .putInt(KEY_SPECTRUM_MODE, mSpectrumMode)
                .putInt(KEY_STUDIO_ANALYSIS_MODE, mStudioAnalysisMode)
                .putInt(KEY_QUICK_TRIGGER_PRESET, mQuickTriggerPreset)
                .putInt(KEY_PATTERN_INDEX, mCustomPattern)
                .putBoolean(KEY_ENABLE_ONSET, mEnableOnset)
                .putBoolean(KEY_ENABLE_LOUDNESS, mEnableLoudnessGate)
                .putFloat(KEY_LOUDNESS_GATE, mLoudnessGateThreshold)
                .putBoolean(KEY_ENABLE_CENTROID, mEnableCentroid)
                .putInt(KEY_CENTROID_MODE, mCentroidMode)
                .putBoolean(KEY_ENABLE_MIN_HOLD, mEnableMinHoldTime)
                .putInt(KEY_MIN_HOLD_MS, mMinHoldTimeMs)
                .putBoolean(KEY_ENABLE_MAX_HOLD, mEnableMaxHoldTime)
                .putInt(KEY_MAX_HOLD_MS, mMaxHoldTimeMs)
                .putBoolean(KEY_ENABLE_FINTERP, mEnableFInterp)
                .putFloat(KEY_FINTERP_SPEED, mFInterpSpeed)
                .putBoolean(KEY_ENABLE_VARIATION, mEnableRandomVariation)
                .putFloat(KEY_VARIATION_DEPTH, mRandomVariationDepth)
                .putBoolean(KEY_ENABLE_LIMITER, mEnableLimiter)
                .putInt(KEY_FFT_SIZE, mFftSize)
                .putBoolean(KEY_USE_TUKEY, mUseTukeyWindow)
                .putString(KEY_ACTIVE_PRESET_ID, mActivePresetId)
                .putInt(KEY_DIAGRAM_INTERVAL_MS, mDiagramIntervalMs)
                .putBoolean(KEY_ENABLE_BAND_THRESHOLD, mEnableBandThreshold)
                .putFloat(KEY_SPECTRUM_VISUAL_GAIN, mSpectrumVisualGain);

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            ed.putFloat(KEY_NARROW_GAIN_PREFIX + i, mNarrowGains[i]);
            ed.putInt(KEY_NARROW_PATTERN_PREFIX + i, mNarrowPatterns[i]);
            ed.putFloat(KEY_NARROW_THRESH_PREFIX + i, mNarrowThresholds[i]);
            ed.putBoolean(KEY_NARROW_ENABLED_PREFIX + i, mNarrowEnabled[i]);
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            ed.putFloat(KEY_WIDE_GAIN_PREFIX + i, mWideGains[i]);
            ed.putInt(KEY_WIDE_PATTERN_PREFIX + i, mWidePatterns[i]);
            ed.putFloat(KEY_WIDE_THRESH_PREFIX + i, mWideThresholds[i]);
            ed.putBoolean(KEY_WIDE_ENABLED_PREFIX + i, mWideEnabled[i]);
        }
        ed.apply();
    }

    public static boolean isEngineEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENGINE_ENABLED, false);
    }

    public static void setEngineEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENGINE_ENABLED, enabled).apply();
    }

    public static int getPatternLedMask(int patternIndex) {
        switch (patternIndex) {
            case PATTERN_TOP: return RealmeGlyphDriver.LED_A;
            case PATTERN_BOTTOM: return RealmeGlyphDriver.LED_C;
            case PATTERN_LEFT: return RealmeGlyphDriver.LED_D;
            case PATTERN_RIGHT: return RealmeGlyphDriver.LED_B;
            case PATTERN_TOP_BOTTOM: return RealmeGlyphDriver.LED_A | RealmeGlyphDriver.LED_C;
            case PATTERN_LEFT_RIGHT: return RealmeGlyphDriver.LED_D | RealmeGlyphDriver.LED_B;
            case PATTERN_TOP_LEFT: return RealmeGlyphDriver.LED_A | RealmeGlyphDriver.LED_D;
            case PATTERN_TOP_RIGHT: return RealmeGlyphDriver.LED_A | RealmeGlyphDriver.LED_B;
            case PATTERN_BOTTOM_LEFT: return RealmeGlyphDriver.LED_C | RealmeGlyphDriver.LED_D;
            case PATTERN_BOTTOM_RIGHT: return RealmeGlyphDriver.LED_C | RealmeGlyphDriver.LED_B;
            case PATTERN_ALL: return RealmeGlyphDriver.LED_ALL;
            case PATTERN_OFF:
            default: return 0;
        }
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
                mSensitivity = 1.15f;
                mDecayMs = 130;
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
    public int getAudioSource() { return mAudioSource; }
    public void setAudioSource(int source) { mAudioSource = source; }

    public int getSpectrumMode() { return mSpectrumMode; }
    public void setSpectrumMode(int mode) { mSpectrumMode = mode; }
    public int getCustomPattern() { return mCustomPattern; }
    public void setCustomPattern(int pattern) { mCustomPattern = pattern; mTriggerMode = MODE_CUSTOM_PATTERN; mPreset = PRESET_CUSTOM; }

    public int getStudioAnalysisMode() { return mStudioAnalysisMode; }
    public void setStudioAnalysisMode(int mode) { mStudioAnalysisMode = mode; }
    public int getQuickTriggerPreset() { return mQuickTriggerPreset; }

    public void applyQuickTriggerPreset(int presetIndex) {
        mQuickTriggerPreset = presetIndex;
        switch (presetIndex) {
            case QUICK_PRESET_CLASSIC_SPLIT:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_TOP;
                mNarrowPatterns[2] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[3] = PATTERN_TOP;
                break;
            case QUICK_PRESET_CIRCLE_4WAY:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_LEFT;
                mNarrowPatterns[2] = PATTERN_RIGHT;
                mNarrowPatterns[3] = PATTERN_TOP;
                break;
            case QUICK_PRESET_CENTER_BASS:
                mNarrowPatterns[0] = PATTERN_ALL;
                mNarrowPatterns[1] = PATTERN_TOP_BOTTOM;
                mNarrowPatterns[2] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[3] = PATTERN_TOP;
                break;
            case QUICK_PRESET_KICK_BASS_ONLY:
                mNarrowPatterns[0] = PATTERN_ALL;
                mNarrowPatterns[1] = PATTERN_ALL;
                mNarrowPatterns[2] = PATTERN_OFF;
                mNarrowPatterns[3] = PATTERN_OFF;
                break;
            case QUICK_PRESET_STEREO_SIDES:
                mNarrowPatterns[0] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[1] = PATTERN_TOP_BOTTOM;
                mNarrowPatterns[2] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[3] = PATTERN_TOP_BOTTOM;
                break;
            case QUICK_PRESET_VERTICAL_PULSE:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_TOP;
                mNarrowPatterns[2] = PATTERN_BOTTOM;
                mNarrowPatterns[3] = PATTERN_TOP;
                break;
            case QUICK_PRESET_DIAGONAL_CROSS:
                mNarrowPatterns[0] = PATTERN_BOTTOM_LEFT;
                mNarrowPatterns[1] = PATTERN_TOP_RIGHT;
                mNarrowPatterns[2] = PATTERN_TOP_LEFT;
                mNarrowPatterns[3] = PATTERN_BOTTOM_RIGHT;
                break;
            case QUICK_PRESET_CLUB_DRIVE:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[2] = PATTERN_TOP_BOTTOM;
                mNarrowPatterns[3] = PATTERN_ALL;
                break;
            case QUICK_PRESET_FULL_AURA:
                mNarrowPatterns[0] = PATTERN_ALL;
                mNarrowPatterns[1] = PATTERN_ALL;
                mNarrowPatterns[2] = PATTERN_ALL;
                mNarrowPatterns[3] = PATTERN_ALL;
                break;
            case QUICK_PRESET_MINIMAL_BOTTOM:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_BOTTOM;
                mNarrowPatterns[2] = PATTERN_OFF;
                mNarrowPatterns[3] = PATTERN_OFF;
                break;
            case QUICK_PRESET_TOP_VOCAL:
                mNarrowPatterns[0] = PATTERN_OFF;
                mNarrowPatterns[1] = PATTERN_BOTTOM;
                mNarrowPatterns[2] = PATTERN_LEFT_RIGHT;
                mNarrowPatterns[3] = PATTERN_TOP;
                break;
            case QUICK_PRESET_CLOCKWISE_WAVE:
                mNarrowPatterns[0] = PATTERN_BOTTOM;
                mNarrowPatterns[1] = PATTERN_RIGHT;
                mNarrowPatterns[2] = PATTERN_TOP;
                mNarrowPatterns[3] = PATTERN_LEFT;
                break;
        }
    }

    public int getNarrowPattern(int index) {
        return (index >= 0 && index < NARROW_BANDS_COUNT) ? mNarrowPatterns[index] : PATTERN_ALL;
    }
    public void setNarrowPattern(int index, int pattern) {
        if (index >= 0 && index < NARROW_BANDS_COUNT) mNarrowPatterns[index] = pattern;
    }

    public int getWidePattern(int index) {
        return (index >= 0 && index < WIDE_BANDS_COUNT) ? mWidePatterns[index] : PATTERN_ALL;
    }
    public void setWidePattern(int index, int pattern) {
        if (index >= 0 && index < WIDE_BANDS_COUNT) mWidePatterns[index] = pattern;
    }

    public float getNarrowGain(int index) {
        return (index >= 0 && index < NARROW_BANDS_COUNT) ? mNarrowGains[index] : 1.0f;
    }
    public void setNarrowGain(int index, float gain) {
        if (index >= 0 && index < NARROW_BANDS_COUNT) mNarrowGains[index] = Math.max(0.2f, Math.min(3.0f, gain));
    }

    public float getWideGain(int index) {
        return (index >= 0 && index < WIDE_BANDS_COUNT) ? mWideGains[index] : 1.0f;
    }
    public void setWideGain(int index, float gain) {
        if (index >= 0 && index < WIDE_BANDS_COUNT) mWideGains[index] = Math.max(0.2f, Math.min(3.0f, gain));
    }

    public float getNarrowThreshold(int index) {
        return (index >= 0 && index < NARROW_BANDS_COUNT) ? mNarrowThresholds[index] : 0.15f;
    }
    public void setNarrowThreshold(int index, float val) {
        if (index >= 0 && index < NARROW_BANDS_COUNT) mNarrowThresholds[index] = Math.max(0.0f, Math.min(0.80f, val));
    }

    public float getWideThreshold(int index) {
        return (index >= 0 && index < WIDE_BANDS_COUNT) ? mWideThresholds[index] : 0.15f;
    }
    public void setWideThreshold(int index, float val) {
        if (index >= 0 && index < WIDE_BANDS_COUNT) mWideThresholds[index] = Math.max(0.0f, Math.min(0.80f, val));
    }

    public float getSpectrumVisualGain() {
        return mSpectrumVisualGain;
    }
    public void setSpectrumVisualGain(float gain, Context context) {
        mSpectrumVisualGain = Math.max(0.3f, Math.min(4.0f, gain));
        if (context != null) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putFloat(KEY_SPECTRUM_VISUAL_GAIN, mSpectrumVisualGain).apply();
        }
    }

    public boolean isNarrowBandEnabled(int index) {
        return (index >= 0 && index < NARROW_BANDS_COUNT) ? mNarrowEnabled[index] : true;
    }
    public void setNarrowBandEnabled(int index, boolean enabled, Context context) {
        if (index >= 0 && index < NARROW_BANDS_COUNT) {
            mNarrowEnabled[index] = enabled;
            if (context != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_NARROW_ENABLED_PREFIX + index, enabled).apply();
            }
        }
    }

    public boolean isWideBandEnabled(int index) {
        return (index >= 0 && index < WIDE_BANDS_COUNT) ? mWideEnabled[index] : true;
    }
    public void setWideBandEnabled(int index, boolean enabled, Context context) {
        if (index >= 0 && index < WIDE_BANDS_COUNT) {
            mWideEnabled[index] = enabled;
            if (context != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        .edit().putBoolean(KEY_WIDE_ENABLED_PREFIX + index, enabled).apply();
            }
        }
    }

    public int getDiagramIntervalMs() {
        return mDiagramIntervalMs;
    }
    public void setDiagramIntervalMs(int ms) {
        mDiagramIntervalMs = Math.max(1, Math.min(1000, ms));
    }

    public void startAutoCalibration(CalibrationCallback callback) {
        startAutoCalibration(10000, true, true, true, true, true, callback);
    }

    public void startAutoCalibration(int durationMs, boolean calibGains, boolean calibThresholds,
                                     boolean calibSens, boolean calibLoudness, boolean calibDecay,
                                     CalibrationCallback callback) {
        mCalibrationDurationMs = Math.max(2000, Math.min(20000, durationMs));
        mCalibGains = calibGains;
        mCalibThresholds = calibThresholds;
        mCalibSensitivity = calibSens;
        mCalibLoudnessGate = calibLoudness;
        mCalibDecay = calibDecay;

        mCalibrationCallback = callback;
        mCalibrationStartTime = System.currentTimeMillis();
        mCalibLastReportedSec = (int) Math.ceil(mCalibrationDurationMs / 1000.0) + 1;
        mCalibFramesCount = 0;
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mCalibNarrowMax[i] = 0f;
            mCalibNarrowFluxSum[i] = 0f;
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mCalibWideMax[i] = 0f;
            mCalibWideFluxSum[i] = 0f;
        }
        mIsCalibrating = true;
    }

    public boolean isCalibrating() {
        return mIsCalibrating;
    }

    public void cancelCalibration() {
        mIsCalibrating = false;
        mCalibrationCallback = null;
    }

    private void finishAutoCalibration() {
        mIsCalibrating = false;
        final CalibrationCallback cb = mCalibrationCallback;
        mCalibrationCallback = null;

        int frames = Math.max(1, mCalibFramesCount);

        // =========================================================================
        // NARROW BANDS (4): SUB, KICK, SNARE, TREBLE
        // =========================================================================
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            if (mCalibGains) {
                if (mCalibNarrowMax[i] > 0.02f) {
                    float targetGain = 0.85f / mCalibNarrowMax[i];
                    mNarrowGains[i] = Math.max(0.70f, Math.min(2.50f, targetGain));
                } else {
                    mNarrowGains[i] = 1.0f;
                }
            }
            float avgFlux = mCalibNarrowFluxSum[i] / frames;
            if (mCalibThresholds && mEnableBandThreshold) {
                // Musical responsive threshold: 5% - 18% (never suffocates beats)
                mNarrowThresholds[i] = Math.max(0.05f, Math.min(0.18f, avgFlux * 1.15f));
            }
        }

        if (mCalibSensitivity) {
            float totalFlux = 0f;
            for (float f : mCalibNarrowFluxSum) totalFlux += f;
            float avg = totalFlux / (frames * NARROW_BANDS_COUNT);
            mSensitivity = Math.max(1.20f, Math.min(1.60f, 1.35f + (avg - 0.08f) * 1.2f));
        }

        if (mCalibLoudnessGate && mEnableLoudnessGate) {
            mLoudnessGateThreshold = Math.max(0.05f, Math.min(0.20f, mCurrentRms * 1.25f));
        }

        if (mCalibDecay) {
            mDecayMs = 75;
        }

        // Sub and Kick (Bands 0 & 1): Symmetrical foundation
        float subMax = mCalibNarrowMax[0];
        float kickMax = mCalibNarrowMax[1];
        if (subMax >= kickMax) {
            mNarrowPatterns[0] = PATTERN_ALL;        // Full aura on heavy sub
            mNarrowPatterns[1] = PATTERN_TOP_BOTTOM; // Vertical duet on kick
        } else {
            mNarrowPatterns[1] = PATTERN_ALL;        // Full aura on kick
            mNarrowPatterns[0] = PATTERN_TOP_BOTTOM; // Vertical duet on sub
        }
        if (subMax < 0.015f) mNarrowPatterns[0] = PATTERN_OFF;
        if (kickMax < 0.015f) mNarrowPatterns[1] = PATTERN_OFF;

        // Snare (Band 2): Horizontal symmetrical duet
        if (mCalibNarrowMax[2] >= 0.015f) {
            mNarrowPatterns[2] = PATTERN_LEFT_RIGHT;
        } else {
            mNarrowPatterns[2] = PATTERN_OFF;
        }

        // Treble (Band 3): Top / Aura accent
        if (mCalibNarrowMax[3] >= 0.015f) {
            mNarrowPatterns[3] = PATTERN_TOP;
        } else {
            mNarrowPatterns[3] = PATTERN_OFF;
        }

        // =========================================================================
        // WIDE BANDS (12): 30Hz to 16kHz
        // =========================================================================
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            if (mCalibGains) {
                if (mCalibWideMax[i] > 0.02f) {
                    float targetGain = 0.85f / mCalibWideMax[i];
                    mWideGains[i] = Math.max(0.70f, Math.min(2.50f, targetGain));
                } else {
                    mWideGains[i] = 1.0f;
                }
            }
            float avgFlux = mCalibWideFluxSum[i] / frames;
            if (mCalibThresholds && mEnableBandThreshold) {
                // Responsive threshold: 4% - 16%
                mWideThresholds[i] = Math.max(0.04f, Math.min(0.16f, avgFlux * 1.10f));
            }
        }

        // Symmetrical Musical Distribution across 12 bands:
        // Zone 1: Bass / Kick (Bands 0, 1, 2: 30Hz, 60Hz, 120Hz)
        int bestBass = 0;
        float maxBassFlux = 0f;
        for (int i = 0; i <= 2; i++) {
            float f = mCalibWideFluxSum[i] / frames;
            if (f > maxBassFlux) {
                maxBassFlux = f;
                bestBass = i;
            }
        }
        mWidePatterns[0] = PATTERN_BOTTOM;
        mWidePatterns[1] = PATTERN_TOP_BOTTOM;
        mWidePatterns[2] = PATTERN_BOTTOM;
        mWidePatterns[bestBass] = PATTERN_ALL; // Dominant bass gets full halo

        // Zone 2: Mid / Snare / Clap (Bands 3, 4, 5, 6: 250Hz, 500Hz, 1kHz, 2kHz)
        int bestMid = 3;
        float maxMidFlux = 0f;
        for (int i = 3; i <= 6; i++) {
            float f = mCalibWideFluxSum[i] / frames;
            if (f > maxMidFlux) {
                maxMidFlux = f;
                bestMid = i;
            }
        }
        mWidePatterns[3] = PATTERN_LEFT_RIGHT;
        mWidePatterns[4] = PATTERN_TOP_LEFT;
        mWidePatterns[5] = PATTERN_BOTTOM_RIGHT;
        mWidePatterns[6] = PATTERN_LEFT_RIGHT;
        mWidePatterns[bestMid] = PATTERN_LEFT_RIGHT; // Dominant mid gets crisp horizontal duet

        // Zone 3: Presence / High Percussion / Hats (Bands 7, 8, 9: 4kHz, 6kHz, 9kHz)
        int bestHigh = 7;
        float maxHighFlux = 0f;
        for (int i = 7; i <= 9; i++) {
            float f = mCalibWideFluxSum[i] / frames;
            if (f > maxHighFlux) {
                maxHighFlux = f;
                bestHigh = i;
            }
        }
        mWidePatterns[7] = PATTERN_TOP_RIGHT;
        mWidePatterns[8] = PATTERN_TOP_LEFT;
        mWidePatterns[9] = PATTERN_TOP;
        mWidePatterns[bestHigh] = PATTERN_TOP_BOTTOM;

        // Zone 4: Cymbals / Air (Bands 10, 11: 12kHz, 16kHz)
        mWidePatterns[10] = PATTERN_TOP_BOTTOM;
        mWidePatterns[11] = PATTERN_ALL;

        // Turn OFF only dead silent bands (< 0.015f peak)
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            if (mCalibWideMax[i] < 0.015f) {
                mWidePatterns[i] = PATTERN_OFF;
            }
        }

        if (mContext != null) {
            saveSettings(mContext);
        }

        if (cb != null) {
            mMainHandler.post(cb::onCalibrationComplete);
        }
    }


    public boolean isEnableBandThreshold() { return mEnableBandThreshold; }
    public void setEnableBandThreshold(boolean enable) { mEnableBandThreshold = enable; }

    public boolean isEnableOnset() { return mEnableOnset; }
    public void setEnableOnset(boolean enable) { mEnableOnset = enable; }
    public boolean isEnableLoudnessGate() { return mEnableLoudnessGate; }
    public void setEnableLoudnessGate(boolean enable) { mEnableLoudnessGate = enable; }
    public float getLoudnessGateThreshold() { return mLoudnessGateThreshold; }
    public void setLoudnessGateThreshold(float threshold) { mLoudnessGateThreshold = threshold; }
    public boolean isEnableCentroid() { return mEnableCentroid; }
    public void setEnableCentroid(boolean enable) { mEnableCentroid = enable; }
    public int getCentroidMode() { return mCentroidMode; }
    public void setCentroidMode(int mode) { mCentroidMode = mode; }

    public boolean isEnableMinHoldTime() { return mEnableMinHoldTime; }
    public void setEnableMinHoldTime(boolean enable) { mEnableMinHoldTime = enable; }
    public int getMinHoldTimeMs() { return mMinHoldTimeMs; }
    public void setMinHoldTimeMs(int ms) { mMinHoldTimeMs = Math.max(10, Math.min(500, ms)); }

    public boolean isEnableMaxHoldTime() { return mEnableMaxHoldTime; }
    public void setEnableMaxHoldTime(boolean enable) { mEnableMaxHoldTime = enable; }
    public int getMaxHoldTimeMs() { return mMaxHoldTimeMs; }
    public void setMaxHoldTimeMs(int ms) { mMaxHoldTimeMs = Math.max(30, Math.min(2000, ms)); }

    public boolean isEnableFInterp() { return mEnableFInterp; }
    public void setEnableFInterp(boolean enable) { mEnableFInterp = enable; }
    public float getFInterpSpeed() { return mFInterpSpeed; }
    public void setFInterpSpeed(float speed) { mFInterpSpeed = Math.max(1.0f, Math.min(50.0f, speed)); }

    public boolean isEnableRandomVariation() { return mEnableRandomVariation; }
    public void setEnableRandomVariation(boolean enable) { mEnableRandomVariation = enable; }
    public float getRandomVariationDepth() { return mRandomVariationDepth; }
    public void setRandomVariationDepth(float depth) { mRandomVariationDepth = Math.max(0.02f, Math.min(0.50f, depth)); }

    public boolean isEnableLimiter() { return mEnableLimiter; }
    public void setEnableLimiter(boolean enable) { mEnableLimiter = enable; }

    /**
     * Process raw 16-bit mono PCM audio from AudioPlaybackCapture (System Audio).
     */
    public AnalysisResult processPcm(short[] pcm, int length, int samplingRateHz) {
        if (pcm == null || length < 128) {
            return getEmptyResult();
        }

        int targetFft = (mFftSize == 4096) ? 4096 : (mFftSize == 2048 ? 2048 : 1024);
        int count = Math.min(length, targetFft);
        long sumSq = 0;
        float[] win = mUseTukeyWindow ? mTukeyWindow : mHannWindow;
        int stepWin = MAX_FFT_SIZE / targetFft;

        for (int i = 0; i < count; i++) {
            short s = pcm[i];
            sumSq += (long) s * s;
            mPcmReal[i] = (s / 32768.0f) * win[i * stepWin];
            mPcmImag[i] = 0f;
        }
        for (int i = count; i < targetFft; i++) {
            mPcmReal[i] = 0f;
            mPcmImag[i] = 0f;
        }

        mCurrentRms = (float) Math.sqrt((double) sumSq / count) / 32768.0f;

        computeRadix2Fft(mPcmReal, mPcmImag, targetFft);

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) mNarrowBands[i] = 0f;
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) mWideBands[i] = 0f;

        int[] narrowCounts = new int[NARROW_BANDS_COUNT];
        int[] wideCounts = new int[WIDE_BANDS_COUNT];

        float binWidth = (float) samplingRateHz / (float) targetFft;
        int halfN = targetFft / 2;
        // Peak normalized: full scale tone produces magnitude 1.0
        float scale = 2.0f / (float) targetFft;

        float sumFreqMag = 0f;
        float sumMag = 0f;

        for (int k = 1; k < halfN; k++) {
            float mag = (float) Math.hypot(mPcmReal[k], mPcmImag[k]) * scale;
            float freq = k * binWidth;

            sumFreqMag += freq * mag;
            sumMag += mag;

            // Narrow Bands (4) - Peak tracking
            if (freq >= 20f && freq < 80f) {
                if (mag > mNarrowBands[0]) mNarrowBands[0] = mag;
            } else if (freq >= 80f && freq < 180f) {
                if (mag > mNarrowBands[1]) mNarrowBands[1] = mag;
            } else if (freq >= 220f && freq < 900f) {
                if (mag > mNarrowBands[2]) mNarrowBands[2] = mag;
            } else if (freq >= 3500f && freq < 16000f) {
                if (mag > mNarrowBands[3]) mNarrowBands[3] = mag;
            }

            // Wide Bands (12 semi-octave spaced) - Peak tracking
            if (freq >= 20f && freq < 45f) {
                if (mag > mWideBands[0]) mWideBands[0] = mag;
            } else if (freq >= 45f && freq < 90f) {
                if (mag > mWideBands[1]) mWideBands[1] = mag;
            } else if (freq >= 90f && freq < 180f) {
                if (mag > mWideBands[2]) mWideBands[2] = mag;
            } else if (freq >= 180f && freq < 350f) {
                if (mag > mWideBands[3]) mWideBands[3] = mag;
            } else if (freq >= 350f && freq < 700f) {
                if (mag > mWideBands[4]) mWideBands[4] = mag;
            } else if (freq >= 700f && freq < 1400f) {
                if (mag > mWideBands[5]) mWideBands[5] = mag;
            } else if (freq >= 1400f && freq < 2800f) {
                if (mag > mWideBands[6]) mWideBands[6] = mag;
            } else if (freq >= 2800f && freq < 4500f) {
                if (mag > mWideBands[7]) mWideBands[7] = mag;
            } else if (freq >= 4500f && freq < 7000f) {
                if (mag > mWideBands[8]) mWideBands[8] = mag;
            } else if (freq >= 7000f && freq < 10500f) {
                if (mag > mWideBands[9]) mWideBands[9] = mag;
            } else if (freq >= 10500f && freq < 14500f) {
                if (mag > mWideBands[10]) mWideBands[10] = mag;
            } else if (freq >= 14500f && freq < 20000f) {
                if (mag > mWideBands[11]) mWideBands[11] = mag;
            }
        }

        mCurrentCentroid = (sumMag > 0.001f) ? (sumFreqMag / sumMag) : 0f;

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mNarrowBands[i] = mNarrowBands[i] * NARROW_WEIGHTS[i] * mNarrowGains[i];
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mWideBands[i] = mWideBands[i] * WIDE_WEIGHTS[i] * mWideGains[i];
        }

        return analyzeBands();
    }

    public void applyPreset(AudioPreset preset, Context context) {
        if (preset == null) return;
        mActivePresetId = preset.id;
        mFftSize = preset.fftSize;
        mUseTukeyWindow = preset.useTukeyWindow;
        mTriggerMode = preset.triggerMode;
        mCustomPattern = preset.patternIndex;
        mSpectrumMode = preset.spectrumMode;
        mStudioAnalysisMode = preset.studioAnalysisMode;
        mQuickTriggerPreset = preset.quickTriggerPreset;
        mSensitivity = preset.sensitivity;
        mDecayMs = preset.decayMs;
        mEnableOnset = preset.enableOnset;
        mEnableLoudnessGate = preset.enableLoudnessGate;
        mLoudnessGateThreshold = preset.loudnessGateThreshold;
        mEnableCentroid = preset.enableCentroid;
        mCentroidMode = preset.centroidMode;
        mEnableMinHoldTime = preset.enableMinHoldTime;
        mMinHoldTimeMs = preset.minHoldTimeMs;
        mEnableMaxHoldTime = preset.enableMaxHoldTime;
        mMaxHoldTimeMs = preset.maxHoldTimeMs;
        mEnableFInterp = preset.enableFInterp;
        mFInterpSpeed = preset.fInterpSpeed;
        mEnableRandomVariation = preset.enableRandomVariation;
        mRandomVariationDepth = preset.randomVariationDepth;
        mEnableLimiter = preset.enableLimiter;

        if (preset.narrowGains != null) {
            for (int i = 0; i < Math.min(NARROW_BANDS_COUNT, preset.narrowGains.length); i++) {
                mNarrowGains[i] = preset.narrowGains[i];
            }
        }
        if (preset.wideGains != null) {
            for (int i = 0; i < Math.min(WIDE_BANDS_COUNT, preset.wideGains.length); i++) {
                mWideGains[i] = preset.wideGains[i];
            }
        }
        if (preset.narrowPatterns != null) {
            for (int i = 0; i < Math.min(NARROW_BANDS_COUNT, preset.narrowPatterns.length); i++) {
                mNarrowPatterns[i] = preset.narrowPatterns[i];
            }
        }
        if (preset.widePatterns != null) {
            for (int i = 0; i < Math.min(WIDE_BANDS_COUNT, preset.widePatterns.length); i++) {
                mWidePatterns[i] = preset.widePatterns[i];
            }
        }
        if (preset.narrowThresholds != null) {
            for (int i = 0; i < Math.min(NARROW_BANDS_COUNT, preset.narrowThresholds.length); i++) {
                mNarrowThresholds[i] = preset.narrowThresholds[i];
            }
        }
        if (preset.wideThresholds != null) {
            for (int i = 0; i < Math.min(WIDE_BANDS_COUNT, preset.wideThresholds.length); i++) {
                mWideThresholds[i] = preset.wideThresholds[i];
            }
        }
        if (preset.narrowEnabled != null) {
            for (int i = 0; i < Math.min(NARROW_BANDS_COUNT, preset.narrowEnabled.length); i++) {
                mNarrowEnabled[i] = preset.narrowEnabled[i];
            }
        }
        if (preset.wideEnabled != null) {
            for (int i = 0; i < Math.min(WIDE_BANDS_COUNT, preset.wideEnabled.length); i++) {
                mWideEnabled[i] = preset.wideEnabled[i];
            }
        }

        saveSettings(context);
        AudioPresetManager.setActivePresetId(context, preset.id);
    }

    public AudioPreset exportCurrentAsPreset(String id, String name) {
        AudioPreset p = new AudioPreset(id, name, false);
        p.fftSize = mFftSize;
        p.useTukeyWindow = mUseTukeyWindow;
        p.triggerMode = mTriggerMode;
        p.patternIndex = mCustomPattern;
        p.spectrumMode = mSpectrumMode;
        p.studioAnalysisMode = mStudioAnalysisMode;
        p.quickTriggerPreset = mQuickTriggerPreset;
        p.sensitivity = mSensitivity;
        p.decayMs = mDecayMs;
        p.enableOnset = mEnableOnset;
        p.enableLoudnessGate = mEnableLoudnessGate;
        p.loudnessGateThreshold = mLoudnessGateThreshold;
        p.enableCentroid = mEnableCentroid;
        p.centroidMode = mCentroidMode;
        p.enableMinHoldTime = mEnableMinHoldTime;
        p.minHoldTimeMs = mMinHoldTimeMs;
        p.enableMaxHoldTime = mEnableMaxHoldTime;
        p.maxHoldTimeMs = mMaxHoldTimeMs;
        p.enableFInterp = mEnableFInterp;
        p.fInterpSpeed = mFInterpSpeed;
        p.enableRandomVariation = mEnableRandomVariation;
        p.randomVariationDepth = mRandomVariationDepth;
        p.enableLimiter = mEnableLimiter;
        p.narrowGains = mNarrowGains.clone();
        p.wideGains = mWideGains.clone();
        p.narrowPatterns = mNarrowPatterns.clone();
        p.widePatterns = mWidePatterns.clone();
        p.narrowThresholds = mNarrowThresholds.clone();
        p.wideThresholds = mWideThresholds.clone();
        p.narrowEnabled = mNarrowEnabled.clone();
        p.wideEnabled = mWideEnabled.clone();
        return p;
    }

    public String getActivePresetId() {
        return mActivePresetId;
    }

    public void setActivePresetId(String id) {
        mActivePresetId = id;
    }

    public int getFftSize() { return mFftSize; }
    public void setFftSize(int size) { mFftSize = size; }
    public boolean isUseTukeyWindow() { return mUseTukeyWindow; }
    public void setUseTukeyWindow(boolean use) { mUseTukeyWindow = use; }

    /**
     * Process raw FFT bytes from Visualizer (Fallback).
     */
    public AnalysisResult processFft(byte[] fft, int samplingRateHz) {
        if (fft == null || fft.length < 4) return getEmptyResult();

        int n = fft.length;
        int halfN = n / 2;
        float binWidth = (float) samplingRateHz / (float) n;

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) mNarrowBands[i] = 0f;
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) mWideBands[i] = 0f;

        int[] narrowCounts = new int[NARROW_BANDS_COUNT];
        int[] wideCounts = new int[WIDE_BANDS_COUNT];

        float sumFreqMag = 0f;
        float sumMag = 0f;

        for (int k = 1; k < halfN; k++) {
            byte r = fft[2 * k];
            byte im = fft[2 * k + 1];
            float mag = (float) Math.hypot(r, im) / 128.0f;
            float freq = k * binWidth;

            sumFreqMag += freq * mag;
            sumMag += mag;

            if (freq >= 20f && freq < 80f) {
                if (mag > mNarrowBands[0]) mNarrowBands[0] = mag;
            } else if (freq >= 80f && freq < 180f) {
                if (mag > mNarrowBands[1]) mNarrowBands[1] = mag;
            } else if (freq >= 220f && freq < 900f) {
                if (mag > mNarrowBands[2]) mNarrowBands[2] = mag;
            } else if (freq >= 3500f && freq < 16000f) {
                if (mag > mNarrowBands[3]) mNarrowBands[3] = mag;
            }

            // Wide Bands (12 semi-octave spaced) - Peak tracking
            if (freq >= 20f && freq < 45f) {
                if (mag > mWideBands[0]) mWideBands[0] = mag;
            } else if (freq >= 45f && freq < 90f) {
                if (mag > mWideBands[1]) mWideBands[1] = mag;
            } else if (freq >= 90f && freq < 180f) {
                if (mag > mWideBands[2]) mWideBands[2] = mag;
            } else if (freq >= 180f && freq < 350f) {
                if (mag > mWideBands[3]) mWideBands[3] = mag;
            } else if (freq >= 350f && freq < 700f) {
                if (mag > mWideBands[4]) mWideBands[4] = mag;
            } else if (freq >= 700f && freq < 1400f) {
                if (mag > mWideBands[5]) mWideBands[5] = mag;
            } else if (freq >= 1400f && freq < 2800f) {
                if (mag > mWideBands[6]) mWideBands[6] = mag;
            } else if (freq >= 2800f && freq < 4500f) {
                if (mag > mWideBands[7]) mWideBands[7] = mag;
            } else if (freq >= 4500f && freq < 7000f) {
                if (mag > mWideBands[8]) mWideBands[8] = mag;
            } else if (freq >= 7000f && freq < 10500f) {
                if (mag > mWideBands[9]) mWideBands[9] = mag;
            } else if (freq >= 10500f && freq < 14500f) {
                if (mag > mWideBands[10]) mWideBands[10] = mag;
            } else if (freq >= 14500f && freq < 20000f) {
                if (mag > mWideBands[11]) mWideBands[11] = mag;
            }
        }

        mCurrentCentroid = (sumMag > 0.001f) ? (sumFreqMag / sumMag) : 0f;

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            mNarrowBands[i] = mNarrowBands[i] * NARROW_WEIGHTS[i] * mNarrowGains[i];
        }
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            mWideBands[i] = mWideBands[i] * WIDE_WEIGHTS[i] * mWideGains[i];
        }

        return analyzeBands();
    }

    private static void computeRadix2Fft(float[] real, float[] imag, int n) {
        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                float tr = real[i]; real[i] = real[j]; real[j] = tr;
                float ti = imag[i]; imag[i] = imag[j]; imag[j] = ti;
            }
            int k = n >> 1;
            while (k <= j) {
                j -= k;
                k >>= 1;
            }
            j += k;
        }

        for (int len = 2; len <= n; len <<= 1) {
            int halfLen = len >> 1;
            double angle = -2.0 * Math.PI / len;
            float wStepR = (float) Math.cos(angle);
            float wStepI = (float) Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                float wR = 1.0f;
                float wI = 0.0f;
                for (int m = 0; m < halfLen; m++) {
                    int pos = i + m;
                    int match = pos + halfLen;
                    float uR = real[pos];
                    float uI = imag[pos];
                    float vR = real[match] * wR - imag[match] * wI;
                    float vI = real[match] * wI + imag[match] * wR;
                    real[pos] = uR + vR;
                    imag[pos] = uI + vI;
                    real[match] = uR - vR;
                    imag[match] = uI - vI;
                    float nextWR = wR * wStepR - wI * wStepI;
                    wI = wR * wStepI + wI * wStepR;
                    wR = nextWR;
                }
            }
        }
    }

    private AnalysisResult analyzeBands() {
        mResult.spectrumMode = mSpectrumMode;
        mResult.rmsLoudness = mCurrentRms;
        mResult.spectralCentroid = mCurrentCentroid;

        // 1. Adaptive Ceiling Tracking & High-Contrast Normalization
        // Prevents all columns from hitting 100% ceiling and makes the dominant band clearly stand out!
        float maxNarrow = 0f;
        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            if (mNarrowBands[i] > maxNarrow) maxNarrow = mNarrowBands[i];
        }
        mNarrowVisualCeiling = Math.max(0.18f, Math.max(mNarrowVisualCeiling * 0.982f, maxNarrow * 1.08f));

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            float ratio = (mNarrowBands[i] / mNarrowVisualCeiling) * mSpectrumVisualGain;
            // Power curve 1.28 expands contrast with high responsiveness
            float lvl = (float) Math.pow(Math.max(0.0f, Math.min(1.0f, ratio)), 1.28f);
            mResult.bandLevels[i] = lvl;
        }

        float maxWide = 0f;
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            if (mWideBands[i] > maxWide) maxWide = mWideBands[i];
        }
        mWideVisualCeiling = Math.max(0.16f, Math.max(mWideVisualCeiling * 0.982f, maxWide * 1.08f));

        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            float ratio = (mWideBands[i] / mWideVisualCeiling) * mSpectrumVisualGain;
            float lvl = (float) Math.pow(Math.max(0.0f, Math.min(1.0f, ratio)), 1.28f);
            mResult.wideLevels[i] = lvl;
            // Smooth curve points
            mWideCurvePoints[i] = 0.60f * mWideCurvePoints[i] + 0.40f * lvl;
            mResult.wideCurve[i] = mWideCurvePoints[i];
        }

        System.arraycopy(mNarrowEnabled, 0, mResult.narrowEnabled, 0, NARROW_BANDS_COUNT);
        System.arraycopy(mWideEnabled, 0, mResult.wideEnabled, 0, WIDE_BANDS_COUNT);

        long now = System.currentTimeMillis();

        // 2. Compute Onset (Spectral Flux) for Narrow Bands
        boolean[] narrowHit = new boolean[NARROW_BANDS_COUNT];
        float alpha = 0.88f;

        for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
            float diff = mNarrowBands[i] - mPrevNarrowBands[i];
            mNarrowFlux[i] = Math.max(0f, diff);
            mPrevNarrowBands[i] = mNarrowBands[i];
            mNarrowAvg[i] = alpha * mNarrowAvg[i] + (1.0f - alpha) * mNarrowFlux[i];
            float threshold = mEnableBandThreshold ? Math.max(mNarrowThresholds[i], mNarrowAvg[i] * mSensitivity) : (mNarrowAvg[i] * mSensitivity);
            boolean levelOk = !mEnableBandThreshold || (mNarrowBands[i] >= mNarrowThresholds[i] * 0.75f);

            if (mNarrowFlux[i] > threshold && levelOk && (now - mLastNarrowBeatTime[i] > MIN_BEAT_INTERVAL_MS)) {
                narrowHit[i] = true;
                mLastNarrowBeatTime[i] = now;
                mNarrowIntensities[i] = 1.0f;
            } else {
                long elapsed = now - mLastNarrowBeatTime[i];
                if (elapsed >= mDecayMs) {
                    mNarrowIntensities[i] = 0f;
                } else {
                    mNarrowIntensities[i] = 1.0f - ((float) elapsed / mDecayMs);
                }
            }

            if (!mNarrowEnabled[i]) {
                mNarrowIntensities[i] = 0f;
                narrowHit[i] = false;
            }
        }

        // 3. Compute Onset for Wide Bands
        boolean[] wideHit = new boolean[WIDE_BANDS_COUNT];
        for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
            float diff = mWideBands[i] - mPrevWideBands[i];
            mWideFlux[i] = Math.max(0f, diff);
            mPrevWideBands[i] = mWideBands[i];
            mWideAvg[i] = alpha * mWideAvg[i] + (1.0f - alpha) * mWideFlux[i];
            float threshold = mEnableBandThreshold ? Math.max(mWideThresholds[i], mWideAvg[i] * mSensitivity) : (mWideAvg[i] * mSensitivity);
            boolean levelOk = !mEnableBandThreshold || (mWideBands[i] >= mWideThresholds[i] * 0.75f);

            if (mWideFlux[i] > threshold && levelOk && (now - mLastWideBeatTime[i] > MIN_BEAT_INTERVAL_MS)) {
                wideHit[i] = true;
                mLastWideBeatTime[i] = now;
                mWideIntensities[i] = 1.0f;
            } else {
                long elapsed = now - mLastWideBeatTime[i];
                if (elapsed >= mDecayMs) {
                    mWideIntensities[i] = 0f;
                } else {
                    mWideIntensities[i] = 1.0f - ((float) elapsed / mDecayMs);
                }
            }

            if (!mWideEnabled[i]) {
                mWideIntensities[i] = 0f;
                wideHit[i] = false;
            }
        }

        // Calibration statistics collection
        if (mIsCalibrating) {
            mCalibFramesCount++;
            for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
                if (mNarrowBands[i] > mCalibNarrowMax[i]) mCalibNarrowMax[i] = mNarrowBands[i];
                mCalibNarrowFluxSum[i] += mNarrowFlux[i];
            }
            for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
                if (mWideBands[i] > mCalibWideMax[i]) mCalibWideMax[i] = mWideBands[i];
                mCalibWideFluxSum[i] += mWideFlux[i];
            }

            long elapsed = now - mCalibrationStartTime;
            int remainingSec = (int) Math.max(0, Math.ceil((mCalibrationDurationMs - elapsed) / 1000.0));
            if (remainingSec != mCalibLastReportedSec) {
                mCalibLastReportedSec = remainingSec;
                if (mCalibrationCallback != null) {
                    final int sec = remainingSec;
                    mMainHandler.post(() -> {
                        if (mCalibrationCallback != null) mCalibrationCallback.onCalibrationProgress(sec);
                    });
                }
            }

            if (elapsed >= mCalibrationDurationMs) {
                finishAutoCalibration();
            }
        }

        // 4. Студийные фильтры детекции (Gate Check)
        boolean passesFilter = true;

        if (mEnableLoudnessGate && mCurrentRms < mLoudnessGateThreshold) {
            passesFilter = false;
        }

        // 5. Trigger Decision based on Spectrum Orientation (Narrow / Wide)
        boolean beatHit = false;
        float maxIntensity = 0f;
        int activeMask = 0;

        if (passesFilter) {
            boolean kickHitNarrow = narrowHit[0] || narrowHit[1];
            float kickIntNarrow = Math.max(mNarrowIntensities[0], mNarrowIntensities[1]);

            boolean kickHitWide = wideHit[0] || wideHit[1];
            float kickIntWide = Math.max(mWideIntensities[0], mWideIntensities[1]);

            boolean kickHit = (mSpectrumMode == SPECTRUM_MODE_NARROW) ? kickHitNarrow : kickHitWide;
            float kickInt = (mSpectrumMode == SPECTRUM_MODE_NARROW) ? kickIntNarrow : kickIntWide;

            // Per-Band Pattern Trigger Mode
            if (mTriggerMode == MODE_CUSTOM_PATTERN) {
                if (mStudioAnalysisMode == STUDIO_MODE_FAST || mSpectrumMode == SPECTRUM_MODE_NARROW) {
                    for (int i = 0; i < NARROW_BANDS_COUNT; i++) {
                        boolean levelOk = !mEnableBandThreshold || (mNarrowBands[i] >= mNarrowThresholds[i] * 0.70f);
                        if (mNarrowIntensities[i] > 0.05f && levelOk) {
                            int mask = getPatternLedMask(mNarrowPatterns[i]);
                            activeMask |= mask;
                            maxIntensity = Math.max(maxIntensity, mNarrowIntensities[i]);
                            if (narrowHit[i] && mask != 0) beatHit = true;
                        }
                    }
                } else {
                    for (int i = 0; i < WIDE_BANDS_COUNT; i++) {
                        boolean levelOk = !mEnableBandThreshold || (mWideBands[i] >= mWideThresholds[i] * 0.70f);
                        if (mWideIntensities[i] > 0.05f && levelOk) {
                            int mask = getPatternLedMask(mWidePatterns[i]);
                            activeMask |= mask;
                            maxIntensity = Math.max(maxIntensity, mWideIntensities[i]);
                            if (wideHit[i] && mask != 0) beatHit = true;
                        }
                    }
                }
            } else if (mTriggerMode == MODE_KICK_ONLY) {
                if (kickHit) beatHit = true;
                if (kickInt > 0.05f) {
                    activeMask = RealmeGlyphDriver.LED_ALL;
                    maxIntensity = kickInt;
                }
            } else if (mTriggerMode == MODE_KICK_AND_SNARE) {
                boolean snareHit = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? (wideHit[3] || wideHit[4]) : narrowHit[2];
                float snareInt = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? Math.max(mWideIntensities[3], mWideIntensities[4]) : mNarrowIntensities[2];

                if (kickHit || snareHit) beatHit = true;

                if (kickInt > 0.05f) {
                    activeMask |= (RealmeGlyphDriver.LED_A | RealmeGlyphDriver.LED_C);
                    maxIntensity = Math.max(maxIntensity, kickInt);
                }
                if (snareInt > 0.05f) {
                    activeMask |= (RealmeGlyphDriver.LED_B | RealmeGlyphDriver.LED_D);
                    maxIntensity = Math.max(maxIntensity, snareInt);
                }
            } else if (mTriggerMode == MODE_FREQUENCY_SPLIT_4WAY) {
                float subInt = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? mWideIntensities[0] : mNarrowIntensities[0];
                float kInt = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? mWideIntensities[1] : mNarrowIntensities[1];
                float snInt = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? mWideIntensities[3] : mNarrowIntensities[2];
                float trInt = (mSpectrumMode == SPECTRUM_MODE_WIDE) ? mWideIntensities[7] : mNarrowIntensities[3];

                if (subInt > 0.05f) { activeMask |= RealmeGlyphDriver.LED_C; maxIntensity = Math.max(maxIntensity, subInt); }
                if (kInt > 0.05f) { activeMask |= RealmeGlyphDriver.LED_D; maxIntensity = Math.max(maxIntensity, kInt); }
                if (snInt > 0.05f) { activeMask |= RealmeGlyphDriver.LED_B; maxIntensity = Math.max(maxIntensity, snInt); }
                if (trInt > 0.05f) { activeMask |= RealmeGlyphDriver.LED_A; maxIntensity = Math.max(maxIntensity, trInt); }

                beatHit = kickHit || narrowHit[2] || narrowHit[3];
            } else if (mTriggerMode == MODE_ENERGY_LEVELS) {
                float totalEnergy = (mNarrowBands[0] * 1.5f + mNarrowBands[1] * 1.2f + mNarrowBands[2] + mNarrowBands[3] * 0.5f);
                float normEnergy = Math.min(1.0f, totalEnergy / 60.0f);
                maxIntensity = normEnergy;

                if (normEnergy > 0.15f) activeMask |= RealmeGlyphDriver.LED_C;
                if (normEnergy > 0.40f) activeMask |= RealmeGlyphDriver.LED_D;
                if (normEnergy > 0.65f) activeMask |= RealmeGlyphDriver.LED_B;
                if (normEnergy > 0.85f) {
                    activeMask |= RealmeGlyphDriver.LED_A;
                    beatHit = true;
                }
            }
        }

        // Apply Min & Max Hold Time logic
        if (beatHit) {
            mLastBeatTime = now;
            mIsBeatActive = true;
            mLastPeakIntensity = maxIntensity;
        }

        if (mEnableMinHoldTime && mIsBeatActive) {
            long pulseAge = now - mLastBeatTime;
            if (pulseAge < mMinHoldTimeMs) {
                maxIntensity = Math.max(maxIntensity, Math.max(0.40f, mLastPeakIntensity * 0.80f));
                if (activeMask == 0) {
                    activeMask = getPatternLedMask(mCustomPattern);
                }
            }
        }

        if (mEnableMaxHoldTime && mIsBeatActive) {
            long pulseAge = now - mLastBeatTime;
            if (pulseAge > mMaxHoldTimeMs) {
                maxIntensity = 0f;
                activeMask = 0;
                beatHit = false;
                mIsBeatActive = false;
            }
        }

        // Apply Organic Variation filter
        if (mEnableRandomVariation && maxIntensity > 0.05f) {
            float var = 1.0f + (mFilterRnd.nextFloat() * 2f - 1f) * mRandomVariationDepth;
            maxIntensity = Math.max(0.01f, Math.min(1.0f, maxIntensity * var));
        }

        // Apply Peak Limiter
        if (mEnableLimiter && maxIntensity > 0.05f) {
            maxIntensity = (float) Math.tanh(maxIntensity * 1.35) * 0.96f;
        }

        // Apply FInterp exponential smoothing (photo 3)
        if (mEnableFInterp) {
            float dt = (mLastFrameTime > 0) ? (now - mLastFrameTime) / 1000.0f : 0.02f;
            dt = Math.max(0.005f, Math.min(0.05f, dt));
            float alphaInterp = (float) (1.0 - Math.exp(-mFInterpSpeed * dt));
            mSmoothedIntensity += (maxIntensity - mSmoothedIntensity) * alphaInterp;
            maxIntensity = mSmoothedIntensity;
        } else {
            mSmoothedIntensity = maxIntensity;
        }
        mLastFrameTime = now;

        mResult.activeLedMask = activeMask;
        mResult.intensity = maxIntensity;
        mResult.isBeat = beatHit;
        return mResult;
    }
}
