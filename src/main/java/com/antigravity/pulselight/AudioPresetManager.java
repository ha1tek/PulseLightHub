package com.antigravity.pulselight;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class AudioPresetManager {

    private static final String PREFS_NAME = "pulse_audio_presets";
    private static final String KEY_USER_PRESETS = "user_presets_json";
    private static final String KEY_ACTIVE_PRESET_ID = "active_preset_id";

    public static final String PRESET_STUDIO_PRO_ID = "studio_pro";
    public static final String PRESET_NOTHING_PURE_ID = "nothing_pure";
    public static final String PRESET_PHONK_808_ID = "phonk_808";
    public static final String PRESET_ROCK_DRUMS_ID = "rock_drums";
    public static final String PRESET_EDM_CLUB_ID = "edm_club";

    public static AudioPreset createStudioProPreset() {
        AudioPreset p = new AudioPreset(PRESET_STUDIO_PRO_ID, "Студийный Pro", true);
        p.fftSize = 4096;
        p.useTukeyWindow = true;
        p.triggerMode = AudioAnalyzer.MODE_CUSTOM_PATTERN;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.sensitivity = 1.45f;
        p.decayMs = 85;
        p.enableOnset = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.08f;
        p.enableCentroid = false;
        p.centroidMode = 0;
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
        return p;
    }

    public static AudioPreset createNothingPurePreset() {
        AudioPreset p = new AudioPreset(PRESET_NOTHING_PURE_ID, "Nothing Phone Pure", true);
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.triggerMode = AudioAnalyzer.MODE_CUSTOM_PATTERN;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_KICK_BASS_ONLY;
        p.sensitivity = 1.40f;
        p.decayMs = 70;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.narrowGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        p.wideGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_ALL, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF};
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        return p;
    }

    public static AudioPreset createPhonk808Preset() {
        AudioPreset p = new AudioPreset(PRESET_PHONK_808_ID, "Phonk / 808 Bass", true);
        p.fftSize = 2048;
        p.useTukeyWindow = true;
        p.triggerMode = AudioAnalyzer.MODE_CUSTOM_PATTERN;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_WIDE;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_DEEP;
        p.sensitivity = 1.20f;
        p.decayMs = 130;
        p.enableOnset = true;
        p.enableLoudnessGate = true;
        p.loudnessGateThreshold = 0.10f;
        p.narrowGains = new float[]{2.2f, 1.8f, 1.0f, 0.8f};
        p.wideGains = new float[]{2.4f, 2.2f, 2.0f, 1.5f, 1.2f, 1.0f, 0.9f, 0.8f, 0.8f, 0.7f, 0.7f, 0.7f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_OFF, AudioAnalyzer.PATTERN_OFF};
        p.narrowThresholds = new float[]{0.18f, 0.18f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        return p;
    }

    public static AudioPreset createRockDrumsPreset() {
        AudioPreset p = new AudioPreset(PRESET_ROCK_DRUMS_ID, "Rock / Drums", true);
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.triggerMode = AudioAnalyzer.MODE_CUSTOM_PATTERN;
        p.patternIndex = AudioAnalyzer.PATTERN_TOP_BOTTOM;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_CLASSIC_SPLIT;
        p.sensitivity = 1.30f;
        p.decayMs = 80;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.narrowGains = new float[]{1.1f, 1.4f, 1.5f, 1.1f};
        p.wideGains = new float[]{1.1f, 1.3f, 1.4f, 1.5f, 1.2f, 1.1f, 1.0f, 1.0f, 0.9f, 0.9f, 0.9f, 0.9f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_TOP, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP};
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        return p;
    }

    public static AudioPreset createEdmClubPreset() {
        AudioPreset p = new AudioPreset(PRESET_EDM_CLUB_ID, "EDM / Club", true);
        p.fftSize = 1024;
        p.useTukeyWindow = false;
        p.triggerMode = AudioAnalyzer.MODE_CUSTOM_PATTERN;
        p.patternIndex = AudioAnalyzer.PATTERN_ALL;
        p.spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
        p.studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
        p.quickTriggerPreset = AudioAnalyzer.QUICK_PRESET_CLUB_DRIVE;
        p.sensitivity = 1.25f;
        p.decayMs = 90;
        p.enableOnset = true;
        p.enableLoudnessGate = false;
        p.narrowGains = new float[]{1.2f, 1.3f, 1.3f, 1.4f};
        p.wideGains = new float[]{1.2f, 1.3f, 1.3f, 1.2f, 1.2f, 1.3f, 1.4f, 1.3f, 1.2f, 1.1f, 1.1f, 1.1f};
        p.narrowPatterns = new int[]{AudioAnalyzer.PATTERN_BOTTOM, AudioAnalyzer.PATTERN_LEFT_RIGHT, AudioAnalyzer.PATTERN_TOP_BOTTOM, AudioAnalyzer.PATTERN_ALL};
        p.narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
        p.wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
        return p;
    }

    public static List<AudioPreset> getAllPresets(Context context) {
        List<AudioPreset> list = new ArrayList<>();
        list.add(createStudioProPreset());
        list.add(createNothingPurePreset());
        list.add(createPhonk808Preset());
        list.add(createRockDrumsPreset());
        list.add(createEdmClubPreset());

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(KEY_USER_PRESETS, null);
            if (json != null && !json.isEmpty()) {
                try {
                    JSONArray arr = new JSONArray(json);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.getJSONObject(i);
                        AudioPreset p = AudioPreset.fromJson(o);
                        p.isBuiltIn = false;
                        list.add(p);
                    }
                } catch (Throwable ignored) {}
            }
        }
        return list;
    }

    public static AudioPreset getPresetById(Context context, String id) {
        if (id == null) return createStudioProPreset();
        if ("ue5_blueprint".equals(id)) return createStudioProPreset();
        for (AudioPreset p : getAllPresets(context)) {
            if (id.equals(p.id)) return p;
        }
        return createStudioProPreset();
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
        String id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_PRESET_ID, PRESET_STUDIO_PRO_ID);
        if ("ue5_blueprint".equals(id)) {
            id = PRESET_STUDIO_PRO_ID;
            setActivePresetId(context, id);
        }
        return id;
    }

    public static void setActivePresetId(Context context, String id) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_ACTIVE_PRESET_ID, id).apply();
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
                p.name = "Импорт (" + (getUserPresets(context).size() + 1) + ")";
            }
            saveUserPreset(context, p);
            setActivePresetId(context, p.id);
            return p;
        } catch (Throwable t) {
            return null;
        }
    }
}
