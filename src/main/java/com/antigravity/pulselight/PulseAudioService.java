package com.antigravity.pulselight;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class PulseAudioService extends Service {
    private static final String TAG = "PulseAudioService";
    private static final String CHANNEL_ID = "pulse_audio_engine_channel";
    private static final int NOTIFICATION_ID = 4096;

    public interface OnAudioFrameListener {
        void onAudioFrame(AudioAnalyzer.AnalysisResult result, int currentColor);
    }

    private static volatile OnAudioFrameListener sFrameListener = null;

    public static void setFrameListener(OnAudioFrameListener listener) {
        sFrameListener = listener;
    }

    private AudioAnalyzer mAnalyzer;
    private Visualizer mVisualizer;
    private Thread mCaptureThread;
    private volatile boolean mIsRunning = false;

    private int mCurrentBeatColor = GlyphColorManager.DEFAULT_BLUE;
    private int mPreviousActiveMask = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        mAnalyzer = new AudioAnalyzer(this);
        createNotificationChannel();
    }

    public static void startEngine(Context context) {
        Intent intent = new Intent(context, PulseAudioService.class);
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
        startVisualizer();
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

        builder.setContentTitle("Pulse Light Engine")
                .setContentText("Аудио-синхронизация глифов Awakening Halo активна")
                .setSmallIcon(R.drawable.ic_soundwave_black)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            try {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } catch (Throwable t) {
                startForeground(NOTIFICATION_ID, notification);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
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

    private void startVisualizer() {
        if (mIsRunning) return;
        mIsRunning = true;

        mCaptureThread = new Thread(() -> {
            try {
                int[] range = Visualizer.getCaptureSizeRange();
                int captureSize = Math.min(1024, range[1]);

                mVisualizer = new Visualizer(0); // Audio Session 0 = Global mix
                mVisualizer.setEnabled(false);
                mVisualizer.setCaptureSize(captureSize);
                mVisualizer.setEnabled(true);

                int samplingRate = mVisualizer.getSamplingRate() / 1000; // mHz to Hz
                if (samplingRate <= 0) samplingRate = 48000;

                byte[] fftBuffer = new byte[captureSize];
                Log.d(TAG, "Visualizer initialized at " + samplingRate + " Hz, captureSize=" + captureSize);

                int silentFrames = 0;

                while (mIsRunning) {
                    int status = mVisualizer.getFft(fftBuffer);
                    if (status != Visualizer.SUCCESS) {
                        Thread.sleep(30);
                        continue;
                    }

                    // Check for total silence
                    boolean isSilent = true;
                    for (int i = 0; i < Math.min(fftBuffer.length, 32); i++) {
                        if (fftBuffer[i] != 0 && fftBuffer[i] != -1) {
                            isSilent = false;
                            break;
                        }
                    }

                    if (isSilent) {
                        silentFrames++;
                        if (silentFrames > 15) {
                            if (mPreviousActiveMask != 0) {
                                RealmeGlyphDriver.turnOff();
                                mPreviousActiveMask = 0;
                            }
                            // Deep sleep when no audio is playing (saves 100% battery)
                            Thread.sleep(150);
                            continue;
                        }
                    } else {
                        silentFrames = 0;
                    }

                    // Reload active settings dynamically
                    mAnalyzer.loadSettings(PulseAudioService.this);

                    AudioAnalyzer.AnalysisResult result = mAnalyzer.processFft(fftBuffer, samplingRate);

                    // Color selection
                    int colorMode = GlyphColorManager.getColorMode(PulseAudioService.this);
                    if (colorMode == GlyphColorManager.COLOR_MODE_RANDOM) {
                        if (result.isBeat) {
                            mCurrentBeatColor = GlyphColorManager.getRandomColor();
                        }
                    } else if (colorMode == GlyphColorManager.COLOR_MODE_PER_SEGMENT) {
                        mCurrentBeatColor = GlyphColorManager.getSegmentColor(PulseAudioService.this, result.activeLedMask);
                    } else {
                        mCurrentBeatColor = GlyphColorManager.getUnifiedColor(PulseAudioService.this);
                    }

                    // Stream to hardware HAL driver
                    if (result.activeLedMask != 0) {
                        RealmeGlyphDriver.flashSegment(result.activeLedMask, mCurrentBeatColor);
                        mPreviousActiveMask = result.activeLedMask;
                    } else {
                        if (mPreviousActiveMask != 0) {
                            RealmeGlyphDriver.turnOff();
                            mPreviousActiveMask = 0;
                        }
                    }

                    // Dispatch to UI listener if app is open
                    OnAudioFrameListener listener = sFrameListener;
                    if (listener != null) {
                        listener.onAudioFrame(result, mCurrentBeatColor);
                    }

                    // ~80 FPS processing rate (12 ms)
                    Thread.sleep(12);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Visualizer capture error: " + t.getMessage(), t);
            } finally {
                releaseVisualizer();
            }
        }, "PulseAudioLoop");

        mCaptureThread.setPriority(Thread.MAX_PRIORITY);
        mCaptureThread.start();
    }

    private void releaseVisualizer() {
        try {
            if (mVisualizer != null) {
                mVisualizer.setEnabled(false);
                mVisualizer.release();
                mVisualizer = null;
            }
        } catch (Throwable ignored) {}
        RealmeGlyphDriver.turnOff();
    }

    @Override
    public void onDestroy() {
        mIsRunning = false;
        if (mCaptureThread != null) {
            mCaptureThread.interrupt();
            mCaptureThread = null;
        }
        releaseVisualizer();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
