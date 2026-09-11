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
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class PulseAudioService extends Service {
    private static final String TAG = "PulseAudioService";
    private static final String CHANNEL_ID = "pulse_audio_engine_channel";
    private static final int NOTIFICATION_ID = 4096;

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

    private int mCurrentBeatColor = GlyphColorManager.DEFAULT_BLUE;
    private int mPreviousActiveMask = 0;

    private static volatile PulseAudioService sInstance = null;

    public static PulseAudioService getInstance() {
        return sInstance;
    }

    public static boolean isRunning() {
        return sInstance != null && sInstance.mIsRunning;
    }

    public static void startAutoCalibration(AudioAnalyzer.CalibrationCallback callback) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.startAutoCalibration(callback);
        }
    }

    public static void startAutoCalibration(int durationMs, boolean calibGains, boolean calibThresholds,
                                           boolean calibSens, boolean calibLoudness, boolean calibDecay,
                                           AudioAnalyzer.CalibrationCallback callback) {
        if (sInstance != null && sInstance.mAnalyzer != null) {
            sInstance.mAnalyzer.startAutoCalibration(durationMs, calibGains, calibThresholds, calibSens, calibLoudness, calibDecay, callback);
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
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mAnalyzer = new AudioAnalyzer(this);
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
        Intent intent = new Intent(context, PulseAudioService.class);
        context.stopService(intent);
        AudioAnalyzer.setEngineEnabled(context, false);
        RealmeGlyphDriver.turnOff();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        return START_STICKY;
    }

    private void startAsForeground() {
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle("Pulse Light Hub")
                .setContentText("Аудио-движок активен • Системный звук")
                .setSmallIcon(R.drawable.ic_soundwave_black)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } catch (Throwable t) {
                Log.w(TAG, "startForeground with mediaProjection type failed: " + t);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pulse Audio Engine",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Фоновый аудио-движок подсветки Realme GT 5");
            channel.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
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

                    if (avgAmp < 10.0f) {
                        // Digital silence (track paused or between songs)
                        if (mPreviousActiveMask != 0) {
                            RealmeGlyphDriver.turnOff();
                            mPreviousActiveMask = 0;
                        }
                        OnAudioFrameListener listener = sFrameListener;
                        if (listener != null) {
                            listener.onAudioFrame(mAnalyzer.getEmptyResult(), mCurrentBeatColor);
                        }
                        Thread.sleep(30);
                        continue;
                    }

                    AudioAnalyzer.AnalysisResult result = mAnalyzer.processPcm(pcmBuffer, read, sampleRate);
                    dispatchAnalysisResult(result);
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

                if (isSilent) {
                    if (mPreviousActiveMask != 0) {
                        RealmeGlyphDriver.turnOff();
                        mPreviousActiveMask = 0;
                    }
                    OnAudioFrameListener listener = sFrameListener;
                    if (listener != null) {
                        listener.onAudioFrame(mAnalyzer.getEmptyResult(), mCurrentBeatColor);
                    }
                    Thread.sleep(60);
                    continue;
                }

                AudioAnalyzer.AnalysisResult result = mAnalyzer.processFft(fftBuffer, samplingRate);
                dispatchAnalysisResult(result);

                Thread.sleep(12);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Visualizer loop error: " + t.getMessage());
        } finally {
            releaseVisualizer();
        }
    }

    private void dispatchAnalysisResult(AudioAnalyzer.AnalysisResult result) {
        int colorMode = GlyphColorManager.getColorMode(PulseAudioService.this);
        int currentColor;
        if (colorMode == GlyphColorManager.COLOR_MODE_RANDOM) {
            if (result.isBeat) {
                mCurrentBeatColor = GlyphColorManager.getRandomColor();
            }
            currentColor = mCurrentBeatColor;
        } else if (colorMode == GlyphColorManager.COLOR_MODE_PER_SEGMENT) {
            currentColor = GlyphColorManager.getSegmentColor(PulseAudioService.this, result.activeLedMask);
        } else {
            currentColor = GlyphColorManager.getUnifiedColor(PulseAudioService.this);
        }

        if (result.isBeat || (result.activeLedMask != mPreviousActiveMask && result.activeLedMask != 0)) {
            RealmeGlyphDriver.flashSegment(result.activeLedMask, currentColor, 0);
            mPreviousActiveMask = result.activeLedMask;
        } else if (result.activeLedMask == 0 && mPreviousActiveMask != 0) {
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
    public void onDestroy() {
        mIsRunning = false;
        if (mCaptureThread != null) {
            mCaptureThread.interrupt();
            mCaptureThread = null;
        }
        releaseAudioRecord();
        releaseVisualizer();
        mMediaProjection = null;
        RealmeGlyphDriver.turnOff();
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
