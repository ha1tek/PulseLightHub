package com.antigravity.pulselight;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.TileService;
import android.util.Log;

public class PulseLightingCoordinator {
    private static final String TAG = "PulseCoordinator";
    private static final String PREFS_NAME = "pulse_breathe_settings";

    public static final String KEY_BREATHE_INTERVAL_MS = "breathe_interval_ms";
    public static final String KEY_BREATHE_DURATION_MS = "breathe_duration_ms";

    public static final int DEFAULT_INTERVAL_MS = 1500;
    public static final int DEFAULT_DURATION_MS = 700;

    public static final int MODE_NONE = 0;
    public static final int MODE_AUDIO = 1;
    public static final int MODE_GLOW = 2;
    public static final int MODE_BREATHE = 3;

    public interface AudioEngineStateListener {
        void onAudioEngineStateChanged(boolean enabled);
    }

    private static final java.util.List<AudioEngineStateListener> sEngineListeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addAudioEngineStateListener(AudioEngineStateListener listener) {
        if (listener != null && !sEngineListeners.contains(listener)) {
            sEngineListeners.add(listener);
        }
    }

    public static void removeAudioEngineStateListener(AudioEngineStateListener listener) {
        if (listener != null) {
            sEngineListeners.remove(listener);
        }
    }

    public static void notifyEngineStateChanged(final boolean enabled) {
        sHandler.post(() -> {
            for (AudioEngineStateListener listener : sEngineListeners) {
                try {
                    listener.onAudioEngineStateChanged(enabled);
                } catch (Throwable t) {
                    Log.w(TAG, "Error notifying engine listener: " + t);
                }
            }
        });
    }

    private static volatile int sCurrentMode = MODE_NONE;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable sBreatheRunnable = null;

    public static synchronized int getMode(Context context) {
        if (PulseAudioService.isEngineEnabled()) {
            sCurrentMode = MODE_AUDIO;
            return MODE_AUDIO;
        }
        if (sCurrentMode == MODE_AUDIO) {
            sCurrentMode = MODE_NONE;
        }
        return sCurrentMode;
    }

    public static synchronized void activateAudio(Context context) {
        stopNonAudioLighting();
        sCurrentMode = MODE_AUDIO;
        if (PulseAudioService.isRunning()) {
            PulseAudioService.resumeEngine();
        } else {
            PulseAudioService.startEngine(context);
        }
        updateAllTiles(context);
        notifyEngineStateChanged(true);
    }

    public static synchronized void deactivateAudio(Context context) {
        if (PulseAudioService.isRunning()) {
            PulseAudioService.pauseEngine();
        } else {
            AudioAnalyzer.setEngineEnabled(context, false);
        }
        sCurrentMode = MODE_NONE;
        RealmeGlyphDriver.turnOffImmediate();
        updateAllTiles(context);
        notifyEngineStateChanged(false);
    }

    public static synchronized void activateGlow(Context context) {
        if (PulseAudioService.isRunning()) {
            PulseAudioService.pauseEngine();
        }
        stopNonAudioLighting();
        sCurrentMode = MODE_GLOW;

        RealmeGlyphDriver.init(context);
        int color = GlyphColorManager.getUnifiedColor(context);
        RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, color, 0);

        updateAllTiles(context);
        notifyEngineStateChanged(false);
    }

    public static synchronized void deactivateGlow(Context context) {
        if (sCurrentMode == MODE_GLOW) {
            sCurrentMode = MODE_NONE;
        }
        RealmeGlyphDriver.turnOffImmediate();
        updateAllTiles(context);
    }

    public static synchronized void activateBreathe(Context context) {
        if (PulseAudioService.isRunning()) {
            PulseAudioService.pauseEngine();
        }
        stopNonAudioLighting();
        sCurrentMode = MODE_BREATHE;

        RealmeGlyphDriver.init(context);
        startBreatheLoop(context);

        updateAllTiles(context);
        notifyEngineStateChanged(false);
    }

    public static synchronized void deactivateBreathe(Context context) {
        if (sCurrentMode == MODE_BREATHE) {
            sCurrentMode = MODE_NONE;
        }
        stopBreatheLoop();
        RealmeGlyphDriver.turnOffImmediate();
        updateAllTiles(context);
    }

    public static synchronized void stopAll(Context context) {
        if (PulseAudioService.isRunning()) {
            PulseAudioService.pauseEngine();
        }
        stopNonAudioLighting();
        sCurrentMode = MODE_NONE;
        RealmeGlyphDriver.turnOffImmediate();
        updateAllTiles(context);
        notifyEngineStateChanged(false);
    }

    public static synchronized void onColorChanged(Context context, int newColor) {
        if (sCurrentMode == MODE_GLOW) {
            RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, newColor, 0);
        } else if (sCurrentMode == MODE_BREATHE) {
            int duration = getBreatheDurationMs(context);
            RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, newColor, duration);
        }
    }

    // =========================================================================
    // Breathe Preferences Accessors
    // =========================================================================

    public static int getBreatheIntervalMs(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_BREATHE_INTERVAL_MS, DEFAULT_INTERVAL_MS);
    }

    public static void setBreatheIntervalMs(Context context, int ms) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BREATHE_INTERVAL_MS, ms).apply();
        if (sCurrentMode == MODE_BREATHE) {
            startBreatheLoop(context);
        }
    }

    public static int getBreatheDurationMs(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_BREATHE_DURATION_MS, DEFAULT_DURATION_MS);
    }

    public static void setBreatheDurationMs(Context context, int ms) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_BREATHE_DURATION_MS, ms).apply();
        if (sCurrentMode == MODE_BREATHE) {
            startBreatheLoop(context);
        }
    }

    // =========================================================================
    // Internal Lighting Loops
    // =========================================================================

    private static void stopNonAudioLighting() {
        stopBreatheLoop();
        RealmeGlyphDriver.turnOffImmediate();
    }

    private static void startBreatheLoop(final Context context) {
        stopBreatheLoop();
        final Context appContext = context.getApplicationContext();

        sBreatheRunnable = new Runnable() {
            @Override
            public void run() {
                if (sCurrentMode != MODE_BREATHE) return;

                int interval = Math.max(100, Math.min(10000, getBreatheIntervalMs(appContext)));
                int duration = Math.max(50, Math.min(10000, getBreatheDurationMs(appContext)));
                int effDuration = Math.min(duration, Math.max(50, interval - 50));
                int color = GlyphColorManager.getUnifiedColor(appContext);

                RealmeGlyphDriver.flashSegment(RealmeGlyphDriver.LED_ALL, color, effDuration);
                sHandler.postDelayed(this, interval);
            }
        };
        sHandler.post(sBreatheRunnable);
    }

    private static void stopBreatheLoop() {
        if (sBreatheRunnable != null) {
            sHandler.removeCallbacks(sBreatheRunnable);
            sBreatheRunnable = null;
        }
    }

    public static void updateAllTiles(Context context) {
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                TileService.requestListeningState(context, new ComponentName(context, PulseEngineTileService.class));
                TileService.requestListeningState(context, new ComponentName(context, PulseGlowTileService.class));
                TileService.requestListeningState(context, new ComponentName(context, PulseBreatheTileService.class));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to request listening state for tiles: " + t);
            }
        }
    }
}
