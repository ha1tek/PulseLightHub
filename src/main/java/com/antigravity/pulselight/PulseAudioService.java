package com.antigravity.pulselight;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.audiofx.Visualizer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

public class PulseAudioService extends Service {
    private static final String TAG = "PulseAudioService";
    private static final String CHANNEL_ID = "pulse_silent_service_channel";
    private static final String OLD_CHANNEL_ID = "pulse_audio_engine_channel";
    private static final int NOTIFICATION_ID = 4096;

    public static final String ACTION_TOGGLE_ENGINE = "com.antigravity.pulselight.ACTION_TOGGLE_ENGINE";
    public static final String ACTION_CALIBRATE_10S = "com.antigravity.pulselight.ACTION_CALIBRATE_10S";

    public interface EngineStateListener {
        void onEngineStateChanged(boolean isEnabled);
        void onCalibrationStateChanged(boolean isCalibrating, int remainingSeconds);
    }

    private static volatile EngineStateListener sStateListener = null;

    public static void setStateListener(EngineStateListener listener) {
        sStateListener = listener;
        if (listener != null && sInstance != null) {
            listener.onEngineStateChanged(sInstance.mIsEngineEnabled);
            listener.onCalibrationStateChanged(sInstance.mIsCalibrating, sInstance.mCalibRemainingSeconds);
        }
    }

    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_DATA = "extra_result_data";

    public interface OnAudioFrameListener {
        void onAudioFrame(AudioAnalyzer.AnalysisResult result, int currentColor);
    }

    private static volatile OnAudioFrameListener sFrameListener = null;

    public static void setFrameListener(OnAudioFrameListener listener) {
        sFrameListener = listener;
    }

    private static Intent sLastProjectionData = null;
    private static int sLastProjectionResultCode = Activity.RESULT_CANCELED;
    private static MediaProjection sActiveMediaProjection = null;

    public static boolean hasProjectionData() {
        return (sLastProjectionResultCode == Activity.RESULT_OK && sLastProjectionData != null)
                || sActiveMediaProjection != null;
    }

    private AudioAnalyzer mAnalyzer;
    private MediaProjection mMediaProjection;
    private AudioRecord mAudioRecord;
    private Visualizer mVisualizer;
    private Thread mCaptureThread;
    private volatile boolean mIsRunning = false;

    private volatile boolean mIsEngineEnabled = true;
    private volatile boolean mIsCalibrating = false;
    private volatile int mCalibRemainingSeconds = 0;
    private Handler mMainHandler = null;

    private int mCurrentBeatColor = GlyphColorManager.DEFAULT_BLUE;
    private int mPreviousActiveMask = 0;

    private HandlerThread mDelayThread = null;
    private Handler mDelayHandler = null;
    private volatile int mLastScheduledMask = 0;

    private static volatile PulseAudioService sInstance = null;

    public static PulseAudioService getInstance() {
        return sInstance;
    }

    public static boolean isRunning() {
        return sInstance != null && sInstance.mIsRunning;
    }

    public static boolean isEngineEnabled() {
        return sInstance != null && sInstance.mIsEngineEnabled;
    }

    public static void setEngineEnabled(boolean enabled) {
        if (sInstance != null) {
            sInstance.setEngineEnabledInternal(enabled);
        }
    }

    public static void pauseEngine() {
        setEngineEnabled(false);
    }

    public static void resumeEngine() {
        setEngineEnabled(true);
    }

    public static void startAutoCalibration(AudioAnalyzer.CalibrationCallback callback) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.startAutoCalibration(callback);
        }
    }

    public static void startAutoCalibration(int durationMs, boolean calibGains, boolean calibThresholds,
                                            boolean calibSens, boolean calibLoudness, boolean calibDecay,
                                            AudioAnalyzer.CalibrationCallback callback) {
        startAutoCalibration(durationMs, calibGains, calibThresholds, calibSens, calibLoudness, calibDecay, true, callback);
    }

    public static void startAutoCalibration(int durationMs, boolean calibGains, boolean calibThresholds,
                                            boolean calibSens, boolean calibLoudness, boolean calibDecay,
                                            boolean calibMinHold, AudioAnalyzer.CalibrationCallback callback) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.startAutoCalibration(durationMs, calibGains, calibThresholds, calibSens, calibLoudness, calibDecay, calibMinHold, callback);
        }
    }

    public static boolean isCalibrating() {
        return sInstance != null && sInstance.mAnalyzer != null && sInstance.mAnalyzer.isCalibrating();
    }

    public static void resetAnalyzerDefaults(Context context) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.resetToDefaults(context);
        }
    }

    public static void randomizeAnalyzerConfig(Context context) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.generateRandomConfig(context);
        }
    }

    public static AudioAnalyzer getAnalyzer() {
        return (sInstance != null) ? sInstance.mAnalyzer : null;
    }

    public static void reloadSettings(Context context) {
        if (sInstance != null && sInstance.mAnalyzer != null && context != null) {
            sInstance.mAnalyzer.loadSettings(context);
            if (!sInstance.mAnalyzer.isBluetoothDelayEnabled()) {
                clearDelayQueue();
            }
        }
    }

    public static void clearDelayQueue() {
        if (sInstance != null) {
            sInstance.mLastScheduledMask = 0;
            if (sInstance.mDelayHandler != null) {
                sInstance.mDelayHandler.removeCallbacksAndMessages(null);
                sInstance.mDelayHandler.post(() -> {
                    if (sInstance.mPreviousActiveMask != 0) {
                        RealmeGlyphDriver.turnOff();
                        sInstance.mPreviousActiveMask = 0;
                    }
                });
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mMainHandler = new Handler(Looper.getMainLooper());
        mIsEngineEnabled = AudioAnalyzer.isEngineEnabled(this);
        mAnalyzer = new AudioAnalyzer(this);
        mDelayThread = new HandlerThread("PulseDelayDispatcher");
        mDelayThread.start();
        mDelayHandler = new Handler(mDelayThread.getLooper());
        createNotificationChannel();
    }

    public static void startEngine(Context context) {
        Intent intent = new Intent(context, PulseAudioService.class);
        if (hasProjectionData()) {
            intent.putExtra(EXTRA_RESULT_CODE, sLastProjectionResultCode);
            intent.putExtra(EXTRA_RESULT_DATA, sLastProjectionData);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        AudioAnalyzer.setEngineEnabled(context, true);
    }

    public static void startSystemAudioEngine(Context context, int resultCode, Intent data) {
        sLastProjectionResultCode = resultCode;
        sLastProjectionData = (Intent) data.clone();

        Intent intent = new Intent(context, PulseAudioService.class);
        intent.putExtra(EXTRA_RESULT_CODE, resultCode);
        intent.putExtra(EXTRA_RESULT_DATA, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        AudioAnalyzer.setEngineEnabled(context, true);
    }

    public static void stopEngine(Context context) {
        clearDelayQueue();
        Intent intent = new Intent(context, PulseAudioService.class);
        context.stopService(intent);
        AudioAnalyzer.setEngineEnabled(context, false);
        RealmeGlyphDriver.turnOff();
    }

    public void setEngineEnabledInternal(boolean enabled) {
        mIsEngineEnabled = enabled;
        AudioAnalyzer.setEngineEnabled(this, enabled);
        if (!enabled) {
            clearDelayQueue();
            RealmeGlyphDriver.turnOff();
            mPreviousActiveMask = 0;
            mLastScheduledMask = 0;
        }
        PulseTileService.updateTileState(this);
        EngineStateListener listener = sStateListener;
        if (listener != null) {
            listener.onEngineStateChanged(mIsEngineEnabled);
        }
    }

    private void handleCalibrate10s() {
        if (mIsCalibrating) {
            cancelAutoCalibrationInternal();
            return;
        }
        if (!mIsEngineEnabled) {
            setEngineEnabledInternal(true);
        }
        startAutoCalibrationInternal(10000);
    }

    private synchronized void startAutoCalibrationInternal(int durationMs) {
        if (mAnalyzer == null) return;
        mIsCalibrating = true;
        mCalibRemainingSeconds = durationMs / 1000;

        EngineStateListener stateListener = sStateListener;
        if (stateListener != null) {
            stateListener.onCalibrationStateChanged(true, mCalibRemainingSeconds);
        }

        mAnalyzer.startAutoCalibration(durationMs, true, true, true, true, true, true, new AudioAnalyzer.CalibrationCallback() {
            @Override
            public void onCalibrationProgress(int secondsRemaining) {
                mCalibRemainingSeconds = secondsRemaining;
                EngineStateListener l = sStateListener;
                if (l != null) {
                    l.onCalibrationStateChanged(true, secondsRemaining);
                }
            }

            @Override
            public void onCalibrationComplete() {
                mIsCalibrating = false;
                mCalibRemainingSeconds = 0;
                PulseTileService.updateTileState(PulseAudioService.this);
                EngineStateListener l = sStateListener;
                if (l != null) {
                    l.onCalibrationStateChanged(false, 0);
                }
            }
        });
    }

    private synchronized void cancelAutoCalibrationInternal() {
        mIsCalibrating = false;
        mCalibRemainingSeconds = 0;
        if (mAnalyzer != null) {
            mAnalyzer.cancelCalibration();
        }
        PulseTileService.updateTileState(this);
        EngineStateListener l = sStateListener;
        if (l != null) {
            l.onCalibrationStateChanged(false, 0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_TOGGLE_ENGINE.equals(action)) {
                if (mIsCalibrating) {
                    cancelAutoCalibrationInternal();
                } else {
                    setEngineEnabledInternal(!mIsEngineEnabled);
                }
                return START_STICKY;
            } else if (ACTION_CALIBRATE_10S.equals(action)) {
                handleCalibrate10s();
                return START_STICKY;
            }
        }

        startAsForeground();

        int resultCode = intent != null ? intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) : Activity.RESULT_CANCELED;
        Intent resultData = intent != null ? intent.getParcelableExtra(EXTRA_RESULT_DATA) : null;

        if (sActiveMediaProjection != null) {
            mMediaProjection = sActiveMediaProjection;
            Log.i(TAG, "Reusing existing active MediaProjection for engine restart");
        } else if (resultCode == Activity.RESULT_OK && resultData != null) {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mpm != null) {
                try {
                    mMediaProjection = mpm.getMediaProjection(resultCode, resultData);
                    if (mMediaProjection != null) {
                        sActiveMediaProjection = mMediaProjection;
                        mMediaProjection.registerCallback(new MediaProjection.Callback() {
                            @Override
                            public void onStop() {
                                Log.i(TAG, "MediaProjection stopped by system");
                                sActiveMediaProjection = null;
                                sLastProjectionData = null;
                                sLastProjectionResultCode = Activity.RESULT_CANCELED;
                                stopEngine(PulseAudioService.this);
                            }
                        }, new Handler(Looper.getMainLooper()));
                        Log.i(TAG, "MediaProjection successfully obtained for system audio capture!");
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "Failed to obtain MediaProjection", t);
                }
            }
        }

        startCaptureThread();
        PulseTileService.updateTileState(this);
        return START_STICKY;
    }

    private void startAsForeground() {
        Notification notification = buildSilentNotification();
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                if (mMediaProjection != null || sActiveMediaProjection != null) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
                } else {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                }
            } catch (Throwable t) {
                Log.w(TAG, "startForeground with specific type failed: " + t);
                try {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                } catch (Throwable t2) {
                    startForeground(NOTIFICATION_ID, notification);
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    public void updateNotification() {
        PulseTileService.updateTileState(this);
    }

    private Notification buildSilentNotification() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 100, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_qs_pulse)
                .setContentTitle("Pulse Light")
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setShowWhen(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_DEFERRED);
        }

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                try {
                    nm.deleteNotificationChannel(OLD_CHANNEL_ID);
                } catch (Throwable ignored) {}

                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Pulse Silent Service",
                        NotificationManager.IMPORTANCE_MIN
                );
                channel.setDescription("Фоновый сервис аудио-движка");
                channel.setShowBadge(false);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                nm.createNotificationChannel(channel);
            }
        }
    }

    private synchronized void startCaptureThread() {
        if (mIsRunning) {
            if (mMediaProjection != null && (mAudioRecord == null || mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)) {
                stopCaptureThreadInternal();
            } else {
                return;
            }
        }
        mIsRunning = true;

        mCaptureThread = new Thread(() -> {
            mAnalyzer.loadSettings(PulseAudioService.this);

            if (mMediaProjection != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runPlaybackCaptureLoop(mMediaProjection);
            } else {
                runVisualizerLoop();
            }
        }, "PulseAudioSystemLoop");

        mCaptureThread.setPriority(Thread.MAX_PRIORITY);
        mCaptureThread.start();
    }

    private synchronized void stopCaptureThreadInternal() {
        mIsRunning = false;
        if (mCaptureThread != null) {
            mCaptureThread.interrupt();
            mCaptureThread = null;
        }
        releaseAudioRecord();
        releaseVisualizer();
    }

    /**
     * Pure System Audio Capture via Android 10+ AudioPlaybackCaptureConfiguration.
     * ZERO microphone usage. 100% digital capture of media, games, and system sound.
     */
    private void runPlaybackCaptureLoop(MediaProjection projection) {
        Log.i(TAG, "Starting AudioPlaybackCapture (PURE SYSTEM AUDIO) loop...");
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        int minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding);
        int bufferSize = Math.max(minBuf, 16384);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AudioPlaybackCaptureConfiguration.Builder configBuilder =
                        new AudioPlaybackCaptureConfiguration.Builder(projection);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_MEDIA);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_GAME);
                configBuilder.addMatchingUsage(AudioAttributes.USAGE_UNKNOWN);
                AudioPlaybackCaptureConfiguration captureConfig = configBuilder.build();

                AudioFormat audioFormat = new AudioFormat.Builder()
                        .setEncoding(audioEncoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build();

                mAudioRecord = new AudioRecord.Builder()
                        .setAudioPlaybackCaptureConfig(captureConfig)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .build();

                if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioPlaybackCapture AudioRecord failed to initialize, falling back to Visualizer...");
                    releaseAudioRecord();
                    runVisualizerLoop();
                    return;
                }

                mAudioRecord.startRecording();
                Log.i(TAG, "AudioPlaybackCapture recording ACTIVE - capturing system sound without microphone!");

                short[] pcmBuffer = new short[1024];

                while (mIsRunning) {
                    int read = mAudioRecord.read(pcmBuffer, 0, pcmBuffer.length);
                    if (read <= 0) {
                        Thread.sleep(10);
                        continue;
                    }

                    // Calculate average amplitude to detect silence / pause
                    long sum = 0;
                    for (int i = 0; i < read; i++) {
                        sum += Math.abs(pcmBuffer[i]);
                    }
                    float avgAmp = (float) sum / read;

                    if (avgAmp < 10.0f && !mIsCalibrating) {
                        // Digital silence (track paused or between songs)
                        routeAnalysisResult(mAnalyzer.getEmptyResult());
                        Thread.sleep(30);
                        continue;
                    }

                    AudioAnalyzer.AnalysisResult result = mAnalyzer.processPcm(pcmBuffer, read, sampleRate);
                    routeAnalysisResult(result);
                }
            } else {
                runVisualizerLoop();
            }
        } catch (Throwable t) {
            Log.e(TAG, "PlaybackCapture error: " + t.getMessage(), t);
        } finally {
            releaseAudioRecord();
        }
    }

    /**
     * Fallback system audio capture via Visualizer(0) session.
     * Strictly system output mix - ZERO microphone usage.
     */
    private void runVisualizerLoop() {
        Log.i(TAG, "Starting Visualizer capture loop (System Audio session 0)...");
        try {
            int[] range = Visualizer.getCaptureSizeRange();
            int captureSize = Math.min(1024, range[1]);

            mVisualizer = new Visualizer(0);
            mVisualizer.setEnabled(false);
            mVisualizer.setCaptureSize(captureSize);
            mVisualizer.setEnabled(true);

            int samplingRate = mVisualizer.getSamplingRate() / 1000;
            if (samplingRate <= 0) samplingRate = 48000;

            byte[] fftBuffer = new byte[captureSize];

            while (mIsRunning) {
                int status = mVisualizer.getFft(fftBuffer);
                if (status != Visualizer.SUCCESS) {
                    Thread.sleep(25);
                    continue;
                }

                boolean isSilent = true;
                for (int i = 0; i < Math.min(fftBuffer.length, 32); i++) {
                    if (fftBuffer[i] != 0 && fftBuffer[i] != -1) {
                        isSilent = false;
                        break;
                    }
                }

                if (isSilent && !mIsCalibrating) {
                    routeAnalysisResult(mAnalyzer.getEmptyResult());
                    Thread.sleep(60);
                    continue;
                }

                AudioAnalyzer.AnalysisResult result = mAnalyzer.processFft(fftBuffer, samplingRate);
                routeAnalysisResult(result);

                Thread.sleep(12);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Visualizer loop error: " + t.getMessage());
        } finally {
            releaseVisualizer();
        }
    }

    private void routeAnalysisResult(AudioAnalyzer.AnalysisResult result) {
        if (result == null || mDelayHandler == null) return;

        if (!mIsEngineEnabled && !mIsCalibrating) {
            if (mPreviousActiveMask != 0) {
                clearDelayQueue();
                RealmeGlyphDriver.turnOff();
                mPreviousActiveMask = 0;
                mLastScheduledMask = 0;
            }
            return;
        }

        boolean delayEnabled = mAnalyzer != null && mAnalyzer.isBluetoothDelayEnabled();
        int delayMs = (mAnalyzer != null) ? mAnalyzer.getBluetoothDelayMs() : 0;

        // Digital silence optimization: avoid flooding handler with duplicate empty frames
        if (result.activeLedMask == 0 && mLastScheduledMask == 0) {
            OnAudioFrameListener listener = sFrameListener;
            if (listener != null) {
                listener.onAudioFrame(result, mCurrentBeatColor);
            }
            return;
        }

        mLastScheduledMask = result.activeLedMask;

        if (delayEnabled && delayMs > 0) {
            AudioAnalyzer.AnalysisResult copy = result.copy();
            mDelayHandler.postDelayed(() -> dispatchAnalysisResult(copy), delayMs);
        } else {
            mDelayHandler.post(() -> dispatchAnalysisResult(result));
        }
    }

    private void dispatchAnalysisResult(AudioAnalyzer.AnalysisResult result) {
        if (!mIsEngineEnabled && !mIsCalibrating) {
            if (mPreviousActiveMask != 0) {
                RealmeGlyphDriver.turnOff();
                mPreviousActiveMask = 0;
            }
            return;
        }

        int colorMode = GlyphColorManager.getColorMode(PulseAudioService.this);
        boolean isNeo5 = DeviceModelManager.isGtNeo5(PulseAudioService.this);

        int targetMask = result.activeLedMask;
        if (isNeo5) {
            targetMask = (targetMask != 0) ? RealmeGlyphDriver.LED_ALL : 0;
            result.activeLedMask = targetMask;
        }

        boolean isNewFlash = result.isBeat
                || (mPreviousActiveMask == 0 && targetMask != 0)
                || (targetMask != mPreviousActiveMask && targetMask != 0);

        if (result.isColorCycle) {
            if (isNeo5) {
                mCurrentBeatColor = GlyphColorManager.getRandomNeo5Color();
            } else {
                mCurrentBeatColor = GlyphColorManager.getNextRainbowColor();
            }
            GlyphColorManager.setUnifiedColor(PulseAudioService.this, mCurrentBeatColor);
        } else if (colorMode == GlyphColorManager.COLOR_MODE_RANDOM) {
            if (isNewFlash) {
                mCurrentBeatColor = GlyphColorManager.getNextRainbowColor();
            }
        } else {
            mCurrentBeatColor = GlyphColorManager.getUnifiedColor(PulseAudioService.this);
        }
        int currentColor = mCurrentBeatColor;

        if (isNewFlash) {
            RealmeGlyphDriver.flashSegment(targetMask, currentColor, 0);
            mPreviousActiveMask = targetMask;
        } else if (result.isColorCycle && (targetMask != 0 || mPreviousActiveMask != 0)) {
            // Instant live color switch without turning off while burning/holding!
            int activeMask = (targetMask != 0) ? targetMask : mPreviousActiveMask;
            RealmeGlyphDriver.flashSegment(activeMask, currentColor, 0);
        } else if (targetMask == 0 && mPreviousActiveMask != 0) {
            RealmeGlyphDriver.turnOff();
            mPreviousActiveMask = 0;
        }

        OnAudioFrameListener listener = sFrameListener;
        if (listener != null) {
            listener.onAudioFrame(result.copy(), currentColor);
        }
    }

    private void releaseVisualizer() {
        try {
            if (mVisualizer != null) {
                mVisualizer.setEnabled(false);
                mVisualizer.release();
                mVisualizer = null;
            }
        } catch (Throwable ignored) {}
    }

    private void releaseAudioRecord() {
        try {
            if (mAudioRecord != null) {
                if (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    mAudioRecord.stop();
                }
                mAudioRecord.release();
                mAudioRecord = null;
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "Application swiped from Recents, stopping audio engine and turning off lights...");
        stopEngine(this);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;
        if (mCaptureThread != null) {
            mCaptureThread.interrupt();
            mCaptureThread = null;
        }
        if (mAnalyzer != null) {
            mAnalyzer.cancelCalibration();
        }
        clearDelayQueue();
        if (mDelayThread != null) {
            mDelayThread.quitSafely();
            mDelayThread = null;
            mDelayHandler = null;
        }
        releaseAudioRecord();
        releaseVisualizer();
        mMediaProjection = null;
        RealmeGlyphDriver.turnOff();
        sInstance = null;
        PulseTileService.updateTileState(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
