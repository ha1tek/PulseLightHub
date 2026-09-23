package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AudioPresetManager {

    private static final String PREFS_NAME = "pulse_audio_presets";
    private static final String KEY_USER_PRESETS = "user_presets_json";
    private static final String KEY_FAVORITE_PRESET_IDS = "favorite_preset_ids_json";
    private static final String KEY_ACTIVE_PRESET_ID = "active_preset_id";
    private static final String KEY_ACTIVE_PRESET_ID_GT5 = "active_preset_id_gt5";
    private static final String KEY_ACTIVE_PRESET_ID_NEO5 = "active_preset_id_neo5";

    public static final String PRESET_STUDIO_PRO_ID = "studio_pro";
    public static final String PRESET_NOTHING_PURE_ID = "nothing_pure";
    public static final String PRESET_PHONK_808_ID = "phonk_808";
    public static final String PRESET_ROCK_DRUMS_ID = "rock_drums";
    public static final String PRESET_EDM_CLUB_ID = "edm_club";

    public static final String PRESET_NEO5_BASS_SNARE_ID = "neo5_bass_snare";
    public static final String PRESET_NEO5_BASS_HIHAT_ID = "neo5_bass_hihat";
    public static final String PRESET_NEO5_FULL_DRIVE_ID = "neo5_full_drive";
    public static final String PRESET_NEO5_NEON_CHAOS_ID = "neo5_neon_chaos";
    public static final String PRESET_NEO5_MINIMAL_BASS_ID = "neo5_minimal_bass";

    public static AudioPreset createStudioProPreset() {
        AudioPreset p = new AudioPreset(PRESET_STUDIO_PRO_ID, "Студийный Pro", true, DeviceModelManager.MODEL_GT_5, "Базовые профили");
        p.fftSize = 4096;
        p.useTukeyWindow = true;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.45f;
        p.decayMs = 85;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.08f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 85;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 260;
        p.enableFInterp = false;
        p.fInterpSpeed = 14.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.12f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{2.0f, 1.7f, 1.2f, 0.9f};
        p.wideGains = new float[]{2.2f, 2.0f, 1.8f, 1.6f, 1.3f, 1.1f, 1.0f, 0.9f, 0.9f, 0.8f, 0.8f, 0.8f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_ALL};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, false, false, false};
        p.wideColorCycle = new boolean[12];
        return p;
    }

    public static AudioPreset createNothingPurePreset() {
        AudioPreset p = new AudioPreset(PRESET_NOTHING_PURE_ID, "Nothing Phone Pure", true, DeviceModelManager.MODEL_GT_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_KICK_BASS_ONLY;
        p.sensitivity = 1.40f;
        p.decayMs = 70;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.30f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 80;
        p.enableMaxHoldTime = false;
        p.maxHoldTimeMs = 250;
        p.enableFInterp = false;
        p.fInterpSpeed = 12.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = false;
        p.narrowGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        p.wideGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        p.narrowEnabled = new boolean[]{true, true, false, false};
        p.wideEnabled = new boolean[]{true, true, true, true, false, false, false, false, false, false, false, false};
        p.narrowColorCycle = new boolean[]{false, false, false, false};
        p.wideColorCycle = new boolean[12];
        return p;
    }

    public static AudioPreset createPhonk808Preset() {
        AudioPreset p = new AudioPreset(PRESET_PHONK_808_ID, "Phonk / 808 Bass", true, DeviceModelManager.MODEL_GT_5, "Базовые профили");
        p.fftSize = 2048;
        p.useTukeyWindow = true;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.20f;
        p.decayMs = 130;
        p.diagramIntervalMs = 15;
        p.spectrumVisualGain = 1.50f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.10f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 90;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 300;
        p.enableFInterp = true;
        p.fInterpSpeed = 10.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{2.2f, 1.8f, 1.0f, 0.8f};
        p.wideGains = new float[]{2.4f, 2.2f, 2.0f, 1.5f, 1.2f, 1.0f, 0.9f, 0.8f, 0.8f, 0.7f, 0.7f, 0.7f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
        };
        p.narrowThresholds = new float[]{0.18f, 0.18f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        p.narrowEnabled = new boolean[]{true, true, false, false};
        p.wideEnabled = new boolean[]{true, true, true, true, false, false, false, false, false, false, false, false};
        p.narrowColorCycle = new boolean[]{false, false, false, false};
        p.wideColorCycle = new boolean[12];
        return p;
    }

    public static AudioPreset createRockDrumsPreset() {
        AudioPreset p = new AudioPreset(PRESET_ROCK_DRUMS_ID, "Rock / Drums", true, DeviceModelManager.MODEL_GT_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_TOP_BOTTOM;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_CLASSIC_SPLIT;
        p.sensitivity = 1.30f;
        p.decayMs = 80;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 80;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 220;
        p.enableFInterp = false;
        p.fInterpSpeed = 12.0f;
        p.enableRandomVariation = true;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{1.1f, 1.4f, 1.5f, 1.1f};
        p.wideGains = new float[]{1.1f, 1.3f, 1.4f, 1.5f, 1.2f, 1.1f, 1.0f, 1.0f, 0.9f, 0.9f, 0.9f, 0.9f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, false, false, false};
        p.wideColorCycle = new boolean[12];
        return p;
    }

    public static AudioPreset createEdmClubPreset() {
        AudioPreset p = new AudioPreset(PRESET_EDM_CLUB_ID, "EDM / Club", true, DeviceModelManager.MODEL_GT_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_CLUB_DRIVE;
        p.sensitivity = 1.25f;
        p.decayMs = 90;
        p.diagramIntervalMs = 8;
        p.spectrumVisualGain = 1.45f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 75;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 240;
        p.enableFInterp = false;
        p.fInterpSpeed = 15.0f;
        p.enableRandomVariation = true;
        p.randomVariationDepth = 0.10f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{1.2f, 1.3f, 1.3f, 1.4f};
        p.wideGains = new float[]{1.2f, 1.3f, 1.3f, 1.2f, 1.2f, 1.3f, 1.4f, 1.3f, 1.2f, 1.1f, 1.1f, 1.1f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_BOTTOM,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_LEFT, AudioAnalyzer.PATTERN_RIGHT,
                AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_LEFT, AudioAnalyzer.PATTERN_TOP_RIGHT,
                AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, true, false, true};
        p.wideColorCycle = new boolean[]{false, false, false, false, true, false, true, false, true, false, true, false};
        return p;
    }

    public static AudioPreset createNeo5BassSnarePreset() {
        AudioPreset p = new AudioPreset(PRESET_NEO5_BASS_SNARE_ID, "Бас + Смена на Снейр", true, DeviceModelManager.MODEL_GT_NEO_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.35f;
        p.decayMs = 65;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 85;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 220;
        p.enableFInterp = false;
        p.fInterpSpeed = 12.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = false;
        p.narrowGains = new float[]{1.3f, 1.4f, 1.2f, 1.0f};
        p.wideGains = new float[]{1.2f, 1.3f, 1.3f, 1.2f, 1.2f, 1.1f, 1.1f, 1.0f, 1.0f, 0.9f, 0.9f, 0.9f};
        p.narrowThresholds = new float[]{0.14f, 0.14f, 0.12f, 0.15f};
        p.wideThresholds = new float[]{0.14f, 0.14f, 0.14f, 0.12f, 0.12f, 0.12f, 0.12f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
        };
        p.narrowEnabled = new boolean[]{true, true, true, false};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, false, false, false, false, false, false};
        p.narrowColorCycle = new boolean[]{false, false, true, false};
        p.wideColorCycle = new boolean[]{false, false, false, false, true, true, false, false, false, false, false, false};
        return p;
    }

    public static AudioPreset createNeo5BassHihatPreset() {
        AudioPreset p = new AudioPreset(PRESET_NEO5_BASS_HIHAT_ID, "Бас + Смена на Хэты", true, DeviceModelManager.MODEL_GT_NEO_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.40f;
        p.decayMs = 55;
        p.diagramIntervalMs = 10;
        p.spectrumVisualGain = 1.40f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 78;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 200;
        p.enableFInterp = false;
        p.fInterpSpeed = 14.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = false;
        p.narrowGains = new float[]{1.2f, 1.3f, 1.2f, 1.4f};
        p.wideGains = new float[]{1.2f, 1.3f, 1.2f, 1.1f, 1.1f, 1.0f, 1.0f, 1.1f, 1.2f, 1.4f, 1.3f, 1.2f};
        p.narrowThresholds = new float[]{0.14f, 0.14f, 0.13f, 0.10f};
        p.wideThresholds = new float[]{0.14f, 0.14f, 0.14f, 0.13f, 0.13f, 0.12f, 0.12f, 0.11f, 0.10f, 0.10f, 0.10f, 0.12f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL,
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE
        };
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, false, false, true};
        p.wideColorCycle = new boolean[]{false, false, false, false, false, false, false, true, true, true, true, true};
        return p;
    }

    public static AudioPreset createNeo5FullDrivePreset() {
        AudioPreset p = new AudioPreset(PRESET_NEO5_FULL_DRIVE_ID, "Полный драйв", true, DeviceModelManager.MODEL_GT_NEO_5, "Базовые профили");
        p.fftSize = 2048;
        p.useTukeyWindow = true;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.45f;
        p.decayMs = 70;
        p.diagramIntervalMs = 8;
        p.spectrumVisualGain = 1.50f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLimiter = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.07f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 75;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 220;
        p.enableFInterp = false;
        p.fInterpSpeed = 15.0f;
        p.enableRandomVariation = true;
        p.randomVariationDepth = 0.10f;
        p.narrowGains = new float[]{1.4f, 1.4f, 1.3f, 1.2f};
        p.wideGains = new float[]{1.4f, 1.4f, 1.3f, 1.3f, 1.2f, 1.2f, 1.1f, 1.1f, 1.1f, 1.0f, 1.0f, 1.0f};
        p.narrowThresholds = new float[]{0.11f, 0.11f, 0.12f, 0.13f};
        p.wideThresholds = new float[]{0.11f, 0.11f, 0.11f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.13f, 0.13f, 0.13f, 0.13f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE,
                AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, false, true, true};
        p.wideColorCycle = new boolean[]{false, false, false, true, true, true, true, true, true, false, false, false};
        return p;
    }

    public static AudioPreset createNeo5NeonChaosPreset() {
        AudioPreset p = new AudioPreset(PRESET_NEO5_NEON_CHAOS_ID, "Неоновый хаос", true, DeviceModelManager.MODEL_GT_NEO_5, "Базовые профили");
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.50f;
        p.decayMs = 50;
        p.diagramIntervalMs = 6;
        p.spectrumVisualGain = 1.60f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.loudnessGateThreshold = 0.15f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 75;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 180;
        p.enableFInterp = false;
        p.fInterpSpeed = 16.0f;
        p.enableRandomVariation = true;
        p.randomVariationDepth = 0.20f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{1.3f, 1.4f, 1.4f, 1.2f};
        p.wideGains = new float[]{1.3f, 1.4f, 1.4f, 1.3f, 1.3f, 1.2f, 1.2f, 1.3f, 1.4f, 1.3f, 1.2f, 1.1f};
        p.narrowThresholds = new float[]{0.11f, 0.11f, 0.10f, 0.12f};
        p.wideThresholds = new float[]{0.11f, 0.11f, 0.11f, 0.10f, 0.10f, 0.10f, 0.11f, 0.11f, 0.11f, 0.12f, 0.12f, 0.12f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE,
                AudioAnalyzer.PATTERN_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_FLASH_AND_COLOR_CYCLE, AudioAnalyzer.PATTERN_ALL
        };
        p.narrowEnabled = new boolean[]{true, true, true, true};
        p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
        p.narrowColorCycle = new boolean[]{false, true, true, true};
        p.wideColorCycle = new boolean[]{false, true, false, true, true, true, true, true, true, false, true, false};
        return p;
    }

    public static AudioPreset createNeo5MinimalBassPreset() {
        AudioPreset p = new AudioPreset(PRESET_NEO5_MINIMAL_BASS_ID, "Минимал бас", true, DeviceModelManager.MODEL_GT_NEO_5, "Базовые профили");
        p.fftSize = 2048;
        p.useTukeyWindow = true;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = 0;
        p.sensitivity = 1.20f;
        p.decayMs = 110;
        p.diagramIntervalMs = 12;
        p.spectrumVisualGain = 1.35f;
        p.enableBandThreshold = true;
        p.enableBluetoothDelay = false;
        p.bluetoothDelayMs = 150;
        p.enableOnset = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.12f;
        p.enableCentroid = false;
        p.centroidMode = 0;
        p.enableMinHoldTime = true;
        p.minHoldTimeMs = 90;
        p.enableMaxHoldTime = true;
        p.maxHoldTimeMs = 280;
        p.enableFInterp = true;
        p.fInterpSpeed = 10.0f;
        p.enableRandomVariation = false;
        p.randomVariationDepth = 0.15f;
        p.enableLimiter = true;
        p.narrowGains = new float[]{1.5f, 1.6f, 1.0f, 1.0f};
        p.wideGains = new float[]{1.6f, 1.6f, 1.4f, 1.2f, 1.0f, 0.9f, 0.8f, 0.8f, 0.8f, 0.7f, 0.7f, 0.7f};
        p.narrowThresholds = new float[]{0.18f, 0.18f, 0.20f, 0.20f};
        p.wideThresholds = new float[]{0.18f, 0.18f, 0.18f, 0.16f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f, 0.15f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF};
        p.widePatterns = new int[]{
                AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_OFF,
                AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF,
                AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF
        };
        p.narrowEnabled = new boolean[]{true, true, false, false};
        p.wideEnabled = new boolean[]{true, true, true, false, false, false, false, false, false, false, false, false};
        p.narrowColorCycle = new boolean[]{false, false, false, false};
        p.wideColorCycle = new boolean[12];
        return p;
    }

    public static List<AudioPreset> getBuiltInPresets(int deviceModel) {
        List<AudioPreset> list = new ArrayList<>();
        if (deviceModel == DeviceModelManager.MODEL_GT_NEO_5) {
            list.add(createNeo5BassSnarePreset());
            list.add(createNeo5BassHihatPreset());
            list.add(createNeo5FullDrivePreset());
            list.add(createNeo5NeonChaosPreset());
            list.add(createNeo5MinimalBassPreset());
        } else {
            list.add(createStudioProPreset());
            list.add(createNothingPurePreset());
            list.add(createPhonk808Preset());
            list.add(createRockDrumsPreset());
            list.add(createEdmClubPreset());
        }
        list.addAll(GenrePresetCatalog.getPresetsForModel(deviceModel));
        return list;
    }

    public static List<AudioPreset> getAllPresets(Context context) {
        int model = (context != null) ? DeviceModelManager.getDeviceModel(context) : DeviceModelManager.MODEL_GT_5;
        return getAllPresets(context, model);
    }

    public static List<AudioPreset> getAllPresets(Context context, int deviceModel) {
        List<AudioPreset> list = new ArrayList<>(getBuiltInPresets(deviceModel));
        list.addAll(getUserPresets(context, deviceModel));
        return list;
    }

    public static List<AudioPreset> getUserPresets(Context context, int deviceModel) {
        List<AudioPreset> userList = new ArrayList<>();
        if (context == null) return userList;
        for (AudioPreset p : getUserPresets(context)) {
            if (p.deviceModel == deviceModel) {
                userList.add(p);
            }
        }
        return userList;
    }

    public static AudioPreset getPresetById(Context context, String id) {
        if (id == null) {
            boolean isNeo5 = (context != null) && DeviceModelManager.isGtNeo5(context);
            return isNeo5 ? createNeo5BassSnarePreset() : createStudioProPreset();
        }
        if ("ue5_blueprint".equals(id)) return createStudioProPreset();

        if (PRESET_NEO5_BASS_SNARE_ID.equals(id)) return createNeo5BassSnarePreset();
        if (PRESET_NEO5_BASS_HIHAT_ID.equals(id)) return createNeo5BassHihatPreset();
        if (PRESET_NEO5_FULL_DRIVE_ID.equals(id)) return createNeo5FullDrivePreset();
        if (PRESET_NEO5_NEON_CHAOS_ID.equals(id)) return createNeo5NeonChaosPreset();
        if (PRESET_NEO5_MINIMAL_BASS_ID.equals(id)) return createNeo5MinimalBassPreset();

        if (PRESET_STUDIO_PRO_ID.equals(id)) return createStudioProPreset();
        if (PRESET_NOTHING_PURE_ID.equals(id)) return createNothingPurePreset();
        if (PRESET_PHONK_808_ID.equals(id)) return createPhonk808Preset();
        if (PRESET_ROCK_DRUMS_ID.equals(id)) return createRockDrumsPreset();
        if (PRESET_EDM_CLUB_ID.equals(id)) return createEdmClubPreset();

        AudioPreset catPreset = GenrePresetCatalog.getPresetById(id);
        if (catPreset != null) return catPreset;

        for (AudioPreset p : getUserPresets(context)) {
            if (id.equals(p.id)) return p;
        }

        boolean isNeo5 = (context != null) && DeviceModelManager.isGtNeo5(context);
        return isNeo5 ? createNeo5BassSnarePreset() : createStudioProPreset();
    }

    public static void saveUserPreset(Context context, AudioPreset newPreset) {
        if (newPreset == null || context == null) return;
        newPreset.isBuiltIn = false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<AudioPreset> userList = getUserPresets(context);

        boolean found = false;
        for (int i = 0; i < userList.size(); i++) {
            if (userList.get(i).id.equals(newPreset.id)) {
                userList.set(i, newPreset);
                found = true;
                break;
            }
        }
        if (!found) {
            userList.add(newPreset);
        }

        JSONArray arr = new JSONArray();
        for (AudioPreset p : userList) {
            arr.put(p.toJson());
        }
        prefs.edit().putString(KEY_USER_PRESETS, arr.toString()).apply();
    }

    public static void deleteUserPreset(Context context, String id) {
        if (id == null || context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<AudioPreset> userList = getUserPresets(context);
        for (int i = userList.size() - 1; i >= 0; i--) {
            if (id.equals(userList.get(i).id)) {
                userList.remove(i);
            }
        }

        JSONArray arr = new JSONArray();
        for (AudioPreset p : userList) {
            arr.put(p.toJson());
        }
        prefs.edit().putString(KEY_USER_PRESETS, arr.toString()).apply();
    }

    public static List<AudioPreset> getUserPresets(Context context) {
        List<AudioPreset> userList = new ArrayList<>();
        if (context == null) return userList;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_USER_PRESETS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    userList.add(AudioPreset.fromJson(arr.getJSONObject(i)));
                }
            } catch (Throwable ignored) {}
        }
        return userList;
    }

    public static String getActivePresetId(Context context) {
        if (context == null) return PRESET_STUDIO_PRO_ID;
        int model = DeviceModelManager.getDeviceModel(context);
        return getActivePresetId(context, model);
    }

    public static String getActivePresetId(Context context, int deviceModel) {
        if (context == null) return (deviceModel == DeviceModelManager.MODEL_GT_NEO_5) ? PRESET_NEO5_BASS_SNARE_ID : PRESET_STUDIO_PRO_ID;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (deviceModel == DeviceModelManager.MODEL_GT_NEO_5) {
            return sp.getString(KEY_ACTIVE_PRESET_ID_NEO5, PRESET_NEO5_BASS_SNARE_ID);
        } else {
            String id = sp.getString(KEY_ACTIVE_PRESET_ID_GT5, null);
            if (id == null) {
                id = sp.getString(KEY_ACTIVE_PRESET_ID, PRESET_STUDIO_PRO_ID);
            }
            if ("ue5_blueprint".equals(id)) {
                id = PRESET_STUDIO_PRO_ID;
            }
            return id;
        }
    }

    public static void setActivePresetId(Context context, String id) {
        if (context == null || id == null) return;
        int model = DeviceModelManager.getDeviceModel(context);
        if (id.startsWith("neo5_")) {
            model = DeviceModelManager.MODEL_GT_NEO_5;
        } else {
            AudioPreset p = getPresetById(context, id);
            if (p != null) {
                model = p.deviceModel;
            }
        }
        setActivePresetId(context, id, model);
    }

    public static void setActivePresetId(Context context, String id, int deviceModel) {
        if (context == null) return;
        SharedPreferences.Editor ed = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        if (deviceModel == DeviceModelManager.MODEL_GT_NEO_5) {
            ed.putString(KEY_ACTIVE_PRESET_ID_NEO5, id);
        } else {
            ed.putString(KEY_ACTIVE_PRESET_ID_GT5, id);
            ed.putString(KEY_ACTIVE_PRESET_ID, id);
        }
        ed.apply();
    }

    public static boolean isFavorite(Context context, String id) {
        if (context == null || id == null) return false;
        return getFavoritePresetIds(context).contains(id);
    }

    public static boolean toggleFavorite(Context context, String id) {
        if (context == null || id == null) return false;
        Set<String> set = new HashSet<>(getFavoritePresetIds(context));
        boolean added;
        if (set.contains(id)) {
            set.remove(id);
            added = false;
        } else {
            set.add(id);
            added = true;
        }
        saveFavorites(context, set);
        return added;
    }

    public static Set<String> getFavoritePresetIds(Context context) {
        Set<String> set = new HashSet<>();
        if (context == null) return set;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_FAVORITE_PRESET_IDS, null);
        if (json != null && !json.isEmpty()) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    set.add(arr.getString(i));
                }
            } catch (Throwable ignored) {}
        }
        return set;
    }

    private static void saveFavorites(Context context, Set<String> set) {
        if (context == null) return;
        JSONArray arr = new JSONArray();
        if (set != null) {
            for (String s : set) {
                arr.put(s);
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_FAVORITE_PRESET_IDS, arr.toString()).apply();
    }

    public static String exportPresetToJson(AudioPreset preset) {
        if (preset == null) return "";
        try {
            return preset.toJson().toString(2);
        } catch (Throwable t) {
            return preset.toJson().toString();
        }
    }

    public static AudioPreset importPresetFromJson(Context context, String jsonStr) {
        if (jsonStr == null || jsonStr.trim().isEmpty() || context == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonStr.trim());
            AudioPreset p = AudioPreset.fromJson(obj);
            p.id = "user_" + System.currentTimeMillis();
            p.isBuiltIn = false;
            if (p.name == null || p.name.trim().isEmpty()) {
                p.name = "Импорт №" + (getUserPresets(context).size() + 1);
            }
            saveUserPreset(context, p);
            setActivePresetId(context, p.id);
            return p;
        } catch (Throwable t) {
            return null;
        }
    }
}
