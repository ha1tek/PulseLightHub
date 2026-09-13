package com.antigravity.pulselight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AudioPreset {
    public String id;
    public String name;
    public boolean isBuiltIn;
    public int fftSize = 1024;
    public boolean useTukeyWindow = false;
    public int triggerMode = AudioAnalyzer.MODE_KICK_ONLY;
    public int patternIndex = AudioAnalyzer.PATTERN_ALL;
    public int spectrumMode = AudioAnalyzer.SPECTRUM_MODE_NARROW;
    public int studioAnalysisMode = AudioAnalyzer.STUDIO_MODE_FAST;
    public int quickTriggerPreset = 0;
    public float sensitivity = 1.35f;
    public int decayMs = 75;
    public boolean enableOnset = true;
    public boolean enableLoudnessGate = false;
    public float loudnessGateThreshold = 0.15f;
    public boolean enableCentroid = false;
    public int centroidMode = 0;
    public boolean enableMinHoldTime = false;
    public int minHoldTimeMs = 40;
    public boolean enableMaxHoldTime = false;
    public int maxHoldTimeMs = 250;
    public boolean enableFInterp = false;
    public float fInterpSpeed = 12.0f;
    public boolean enableRandomVariation = false;
    public float randomVariationDepth = 0.15f;
    public boolean enableLimiter = false;
    public float limiterThreshold = 0.90f;
    public boolean enableBandThreshold = true;
    public float spectrumGain = 1.40f;
    public int diagramIntervalMs = 1;
    public boolean[] narrowColorCycle = new boolean[]{false, false, false, false};
    public boolean[] wideColorCycle = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false};
    public float[] narrowCeilings = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    public float[] wideCeilings = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    public float[] narrowGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
    public float[] wideGains = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    public float[] narrowThresholds = new float[]{0.15f, 0.15f, 0.15f, 0.15f};
    public float[] wideThresholds = new float[]{0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f, 0.12f};
    public boolean[] narrowEnabled = new boolean[]{true, true, true, true};
    public boolean[] wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};

    // Per-band trigger patterns
    public int[] narrowPatterns = new int[]{
            AudioAnalyzer.PATTERN_BOTTOM,      // SUB (20-80 Hz)
            AudioAnalyzer.PATTERN_TOP,         // KICK (80-180 Hz)
            AudioAnalyzer.PATTERN_LEFT_RIGHT,  // SNARE (220-900 Hz)
            AudioAnalyzer.PATTERN_TOP          // TREBLE (3.5-16 kHz)
    };

    public int[] widePatterns = new int[]{
            AudioAnalyzer.PATTERN_BOTTOM,        // 30 Hz
            AudioAnalyzer.PATTERN_TOP_BOTTOM,   // 60 Hz
            AudioAnalyzer.PATTERN_BOTTOM,        // 120 Hz
            AudioAnalyzer.PATTERN_LEFT_RIGHT,    // 250 Hz
            AudioAnalyzer.PATTERN_LEFT,          // 500 Hz
            AudioAnalyzer.PATTERN_RIGHT,         // 1 kHz
            AudioAnalyzer.PATTERN_LEFT_RIGHT,    // 2 kHz
            AudioAnalyzer.PATTERN_TOP_LEFT,      // 4 kHz
            AudioAnalyzer.PATTERN_TOP_RIGHT,     // 6 kHz
            AudioAnalyzer.PATTERN_TOP,           // 9 kHz
            AudioAnalyzer.PATTERN_TOP_BOTTOM,    // 12 kHz
            AudioAnalyzer.PATTERN_ALL            // 16 kHz
    };

    public int deviceModel = DeviceModelManager.MODEL_GT_5;

    public AudioPreset() {}

    public AudioPreset(String id, String name, boolean isBuiltIn) {
        this(id, name, isBuiltIn, DeviceModelManager.MODEL_GT_5);
    }

    public AudioPreset(String id, String name, boolean isBuiltIn, int deviceModel) {
        this.id = id;
        this.name = name;
        this.isBuiltIn = isBuiltIn;
        this.deviceModel = deviceModel;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("name", name);
            obj.put("isBuiltIn", isBuiltIn);
            obj.put("deviceModel", deviceModel);
            obj.put("fftSize", fftSize);
            obj.put("useTukeyWindow", useTukeyWindow);
            obj.put("triggerMode", triggerMode);
            obj.put("patternIndex", patternIndex);
            obj.put("spectrumMode", spectrumMode);
            obj.put("studioAnalysisMode", studioAnalysisMode);
            obj.put("quickTriggerPreset", quickTriggerPreset);
            obj.put("sensitivity", (double) sensitivity);
            obj.put("decayMs", decayMs);
            obj.put("enableOnset", enableOnset);
            obj.put("enableLoudnessGate", enableLoudnessGate);
            obj.put("loudnessGateThreshold", (double) loudnessGateThreshold);
            obj.put("enableCentroid", enableCentroid);
            obj.put("centroidMode", centroidMode);
            obj.put("enableMinHoldTime", enableMinHoldTime);
            obj.put("minHoldTimeMs", minHoldTimeMs);
            obj.put("enableMaxHoldTime", enableMaxHoldTime);
            obj.put("maxHoldTimeMs", maxHoldTimeMs);
            obj.put("enableFInterp", enableFInterp);
            obj.put("fInterpSpeed", (double) fInterpSpeed);
            obj.put("enableRandomVariation", enableRandomVariation);
            obj.put("randomVariationDepth", (double) randomVariationDepth);
            obj.put("enableLimiter", enableLimiter);
            obj.put("limiterThreshold", (double) limiterThreshold);
            obj.put("enableBandThreshold", enableBandThreshold);
            obj.put("spectrumGain", (double) spectrumGain);
            obj.put("diagramIntervalMs", diagramIntervalMs);

            JSONArray ncc = new JSONArray();
            if (narrowColorCycle != null) {
                for (boolean b : narrowColorCycle) ncc.put(b);
            }
            obj.put("narrowColorCycle", ncc);

            JSONArray wcc = new JSONArray();
            if (wideColorCycle != null) {
                for (boolean b : wideColorCycle) wcc.put(b);
            }
            obj.put("wideColorCycle", wcc);

            JSONArray nc = new JSONArray();
            if (narrowCeilings != null) {
                for (float c : narrowCeilings) nc.put((double) c);
            }
            obj.put("narrowCeilings", nc);

            JSONArray wc = new JSONArray();
            if (wideCeilings != null) {
                for (float c : wideCeilings) wc.put((double) c);
            }
            obj.put("wideCeilings", wc);

            JSONArray ng = new JSONArray();
            if (narrowGains != null) {
                for (float g : narrowGains) ng.put((double) g);
            }
            obj.put("narrowGains", ng);

            JSONArray wg = new JSONArray();
            if (wideGains != null) {
                for (float g : wideGains) wg.put((double) g);
            }
            obj.put("wideGains", wg);

            JSONArray np = new JSONArray();
            if (narrowPatterns != null) {
                for (int p : narrowPatterns) np.put(p);
            }
            obj.put("narrowPatterns", np);

            JSONArray wp = new JSONArray();
            if (widePatterns != null) {
                for (int p : widePatterns) wp.put(p);
            }
            obj.put("widePatterns", wp);

            JSONArray nt = new JSONArray();
            if (narrowThresholds != null) {
                for (float t : narrowThresholds) nt.put((double) t);
            }
            obj.put("narrowThresholds", nt);

            JSONArray wt = new JSONArray();
            if (wideThresholds != null) {
                for (float t : wideThresholds) wt.put((double) t);
            }
            obj.put("wideThresholds", wt);

            JSONArray ne = new JSONArray();
            if (narrowEnabled != null) {
                for (boolean b : narrowEnabled) ne.put(b);
            }
            obj.put("narrowEnabled", ne);

            JSONArray we = new JSONArray();
            if (wideEnabled != null) {
                for (boolean b : wideEnabled) we.put(b);
            }
            obj.put("wideEnabled", we);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static AudioPreset fromJson(JSONObject obj) {
        AudioPreset p = new AudioPreset();
        if (obj == null) return p;
        try {
            p.id = obj.optString("id", "preset_" + System.currentTimeMillis());
            p.name = obj.optString("name", "Пользовательский");
            p.isBuiltIn = obj.optBoolean("isBuiltIn", false);
            p.deviceModel = obj.optInt("deviceModel", DeviceModelManager.MODEL_GT_5);
            p.fftSize = obj.optInt("fftSize", 1024);
            p.useTukeyWindow = obj.optBoolean("useTukeyWindow", false);
            p.triggerMode = obj.optInt("triggerMode", AudioAnalyzer.MODE_KICK_ONLY);
            p.patternIndex = obj.optInt("patternIndex", AudioAnalyzer.PATTERN_ALL);
            p.spectrumMode = obj.optInt("spectrumMode", AudioAnalyzer.SPECTRUM_MODE_NARROW);
            p.studioAnalysisMode = obj.optInt("studioAnalysisMode", AudioAnalyzer.STUDIO_MODE_FAST);
            p.quickTriggerPreset = obj.optInt("quickTriggerPreset", 0);
            p.sensitivity = (float) obj.optDouble("sensitivity", 1.35);
            p.decayMs = obj.optInt("decayMs", 75);
            p.enableOnset = obj.optBoolean("enableOnset", true);
            p.enableLoudnessGate = obj.optBoolean("enableLoudnessGate", false);
            p.loudnessGateThreshold = (float) obj.optDouble("loudnessGateThreshold", 0.15);
            p.enableCentroid = obj.optBoolean("enableCentroid", false);
            p.centroidMode = obj.optInt("centroidMode", 0);
            p.enableMinHoldTime = obj.optBoolean("enableMinHoldTime", false);
            p.minHoldTimeMs = obj.optInt("minHoldTimeMs", 40);
            p.enableMaxHoldTime = obj.optBoolean("enableMaxHoldTime", false);
            p.maxHoldTimeMs = obj.optInt("maxHoldTimeMs", 250);
            p.enableFInterp = obj.optBoolean("enableFInterp", false);
            p.fInterpSpeed = (float) obj.optDouble("fInterpSpeed", 12.0);
            p.enableRandomVariation = obj.optBoolean("enableRandomVariation", false);
            p.randomVariationDepth = (float) obj.optDouble("randomVariationDepth", 0.15);
            p.enableLimiter = obj.optBoolean("enableLimiter", false);
            p.limiterThreshold = (float) obj.optDouble("limiterThreshold", 0.90);
            p.enableBandThreshold = obj.optBoolean("enableBandThreshold", true);
            p.spectrumGain = (float) obj.optDouble("spectrumGain", 1.40);
            p.diagramIntervalMs = obj.optInt("diagramIntervalMs", 1);

            JSONArray ncc = obj.optJSONArray("narrowColorCycle");
            if (ncc != null) {
                p.narrowColorCycle = new boolean[Math.max(4, ncc.length())];
                for (int i = 0; i < ncc.length(); i++) p.narrowColorCycle[i] = ncc.optBoolean(i, false);
            } else {
                p.narrowColorCycle = new boolean[]{false, false, false, false};
            }

            JSONArray wcc = obj.optJSONArray("wideColorCycle");
            if (wcc != null) {
                p.wideColorCycle = new boolean[Math.max(12, wcc.length())];
                for (int i = 0; i < wcc.length(); i++) p.wideColorCycle[i] = wcc.optBoolean(i, false);
            } else {
                p.wideColorCycle = new boolean[]{false, false, false, false, false, false, false, false, false, false, false, false};
            }

            JSONArray nc = obj.optJSONArray("narrowCeilings");
            if (nc != null) {
                p.narrowCeilings = new float[Math.max(4, nc.length())];
                for (int i = 0; i < nc.length(); i++) p.narrowCeilings[i] = (float) nc.optDouble(i, 1.0);
            } else {
                p.narrowCeilings = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
            }

            JSONArray wc = obj.optJSONArray("wideCeilings");
            if (wc != null) {
                p.wideCeilings = new float[Math.max(12, wc.length())];
                for (int i = 0; i < wc.length(); i++) p.wideCeilings[i] = (float) wc.optDouble(i, 1.0);
            } else {
                p.wideCeilings = new float[]{1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
            }

            JSONArray ng = obj.optJSONArray("narrowGains");
            if (ng != null) {
                p.narrowGains = new float[Math.max(4, ng.length())];
                for (int i = 0; i < ng.length(); i++) p.narrowGains[i] = (float) ng.optDouble(i, 1.0);
            }

            JSONArray wg = obj.optJSONArray("wideGains");
            if (wg != null) {
                p.wideGains = new float[Math.max(12, wg.length())];
                for (int i = 0; i < wg.length(); i++) p.wideGains[i] = (float) wg.optDouble(i, 1.0);
            }

            JSONArray np = obj.optJSONArray("narrowPatterns");
            if (np != null) {
                p.narrowPatterns = new int[Math.max(4, np.length())];
                for (int i = 0; i < np.length(); i++) p.narrowPatterns[i] = np.optInt(i, AudioAnalyzer.PATTERN_ALL);
            }

            JSONArray wp = obj.optJSONArray("widePatterns");
            if (wp != null) {
                p.widePatterns = new int[Math.max(12, wp.length())];
                for (int i = 0; i < wp.length(); i++) p.widePatterns[i] = wp.optInt(i, AudioAnalyzer.PATTERN_ALL);
            }

            JSONArray nt = obj.optJSONArray("narrowThresholds");
            if (nt != null) {
                p.narrowThresholds = new float[Math.max(4, nt.length())];
                for (int i = 0; i < nt.length(); i++) p.narrowThresholds[i] = (float) nt.optDouble(i, 0.15);
            }

            JSONArray wt = obj.optJSONArray("wideThresholds");
            if (wt != null) {
                p.wideThresholds = new float[Math.max(12, wt.length())];
                for (int i = 0; i < wt.length(); i++) p.wideThresholds[i] = (float) wt.optDouble(i, 0.12);
            }

            JSONArray ne = obj.optJSONArray("narrowEnabled");
            if (ne != null) {
                p.narrowEnabled = new boolean[Math.max(4, ne.length())];
                for (int i = 0; i < ne.length(); i++) p.narrowEnabled[i] = ne.optBoolean(i, true);
            } else {
                p.narrowEnabled = new boolean[]{true, true, true, true};
            }

            JSONArray we = obj.optJSONArray("wideEnabled");
            if (we != null) {
                p.wideEnabled = new boolean[Math.max(12, we.length())];
                for (int i = 0; i < we.length(); i++) p.wideEnabled[i] = we.optBoolean(i, true);
            } else {
                p.wideEnabled = new boolean[]{true, true, true, true, true, true, true, true, true, true, true, true};
            }
        } catch (Throwable ignored) {}
        return p;
    }
}
