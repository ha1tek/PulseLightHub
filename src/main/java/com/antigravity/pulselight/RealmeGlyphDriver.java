package com.antigravity.pulselight;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class RealmeGlyphDriver {
    private static final String TAG = "RealmeGlyphDriver";

    // 4 physical LED zones matching Realme GT 5 hardware (ColorOS 14/15/16 / Android 14/15/16)
    public static final int LED_A = 1;     // Top bracket
    public static final int LED_B = 8;     // Right vertical bar
    public static final int LED_C = 4;     // Bottom bracket
    public static final int LED_D = 2;     // Left vertical bar
    public static final int LED_ALL = 15;  // 1 | 8 | 4 | 2 = 15

    public static final int LIGHT_ID_HALO = 5; // Light ID 5 maps to rear Awakening Halo
    public static final int MODE_MULTI_LED = 5;

    public static final int DAEMON_PORT = 49152;
    private static final String DAEMON_HOST = "127.0.0.1";

    private static Context sAppContext = null;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static Runnable sAutoStopRunnable = null;

    private static final java.util.concurrent.ExecutorService sNetExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    private static volatile java.net.Socket sSocket = null;
    private static volatile java.io.OutputStream sOut = null;
    private static volatile boolean sIsConnecting = false;
    private static volatile boolean sIsConnected = false;

    private static android.os.IBinder sPowerBinder = null;
    private static final int TRANSACTION_SET_FLASHING = 75;
    private static final String DESCRIPTOR_POWER = "android.os.IPowerManager";
    private static boolean sNativeInitAttempted = false;

    public static void init(Context context) {
        if (context != null) {
            sAppContext = context.getApplicationContext();
            initNativeIpm(sAppContext);
            connectAsync();
        }
    }

    private static synchronized void initNativeIpm(Context context) {
        if (sNativeInitAttempted) return;
        sNativeInitAttempted = true;
        if (context == null) return;

        try {
            android.os.PowerManager pm = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            java.lang.reflect.Field mServiceField = android.os.PowerManager.class.getDeclaredField("mService");
            mServiceField.setAccessible(true);
            Object ipm = mServiceField.get(pm);
            if (ipm instanceof android.os.IInterface) {
                sPowerBinder = ((android.os.IInterface) ipm).asBinder();
                Log.i(TAG, "Native Power IBinder obtained via PowerManager.mService!");
            }
        } catch (Throwable t1) {
            Log.w(TAG, "PowerManager.mService reflection failed: " + t1);
        }

        if (sPowerBinder == null) {
            try {
                Class<?> smClass = Class.forName("android.os.ServiceManager");
                java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
                sPowerBinder = (android.os.IBinder) getService.invoke(null, "power");
                Log.i(TAG, "Native Power IBinder obtained via ServiceManager!");
            } catch (Throwable t2) {
                Log.e(TAG, "Failed to get Power IBinder via ServiceManager", t2);
            }
        }
    }

    private static boolean transactFlashing(int lightId, int hwColor, int onMS, int offMS, int mode) {
        if (sPowerBinder == null && sAppContext != null) {
            initNativeIpm(sAppContext);
        }
        if (sPowerBinder == null) return false;
        android.os.Parcel data = android.os.Parcel.obtain();
        android.os.Parcel reply = android.os.Parcel.obtain();
        try {
            data.writeInterfaceToken(DESCRIPTOR_POWER);
            data.writeInt(lightId);
            data.writeInt(hwColor);
            data.writeInt(onMS);
            data.writeInt(offMS);
            data.writeInt(mode);
            sPowerBinder.transact(TRANSACTION_SET_FLASHING, data, reply, 0);
            reply.readException();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "transactFlashing error: " + t);
            return false;
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private static boolean callNativeSetFlashing(int lightId, int hwColor, int ledsMask, int mode) {
        boolean ok = transactFlashing(lightId, hwColor, ledsMask, 0, mode);
        if (ok) {
            Log.d(TAG, "Native IBinder.transact(75) SUCCESS: lightId=" + lightId + " leds=" + ledsMask);
        }
        return ok;
    }

    private static boolean callNativeTurnOff(int lightId) {
        boolean ok1 = transactFlashing(lightId, 0, 0, 0, 5);
        boolean ok2 = transactFlashing(lightId, 0x11000000, 0, 0, 0);
        boolean ok3 = transactFlashing(lightId, 0, 0, 0, 0);
        return ok1 || ok3;
    }

    public static boolean isConnected() {
        return sPowerBinder != null || sIsConnected;
    }

    public static String getStatus() {
        if (sPowerBinder != null) {
            return "Нативный драйвер: Прямой доступ (Без отладки) ✓";
        }
        if (sIsConnected) {
            return "Аппаратный драйвер: Активен (Порт 49152) ✓";
        }
        if (sAppContext != null && PulseLightManager.hasPermission(sAppContext)) {
            return "Ожидание службы подсветки...";
        }
        return "Требуется WRITE_SECURE_SETTINGS";
    }

    public static synchronized void connectAsync() {
        if (sIsConnected || sIsConnecting) return;
        sIsConnecting = true;

        sNetExecutor.execute(() -> {
            try {
                closeSocket();
                java.net.Socket socket = new java.net.Socket();
                socket.setTcpNoDelay(true);
                socket.connect(new java.net.InetSocketAddress(DAEMON_HOST, DAEMON_PORT), 1500);
                sOut = socket.getOutputStream();
                sSocket = socket;
                sIsConnected = true;
                Log.i(TAG, "Connected to PulseDaemon on 127.0.0.1:" + DAEMON_PORT);
            } catch (Throwable t) {
                Log.d(TAG, "Cannot connect to PulseDaemon: " + t.getMessage());
                sIsConnected = false;
            } finally {
                sIsConnecting = false;
            }
        });
    }

    private static synchronized void closeSocket() {
        try {
            if (sOut != null) {
                sOut.close();
                sOut = null;
            }
        } catch (Throwable ignored) {}
        try {
            if (sSocket != null) {
                sSocket.close();
                sSocket = null;
            }
        } catch (Throwable ignored) {}
        sIsConnected = false;
    }

    public static void setFlashing(int lightId, int color, int ledsBitmask, int mode) {
        flashSegment(ledsBitmask, color, 0);
    }

    public static void flashSegment(int ledsMask, int color) {
        flashSegment(ledsMask, color, 0);
    }

    public static int getHardwareColorForRgb(int color) {
        return getHardwareColorForRgb(color, LED_ALL);
    }

    public static int getHardwareColorForRgb(int color, int ledsMask) {
        int high = (color >> 24) & 0xFF;
        if (high == 0x8A || high == 0x8B || high == 0x8C || high == 0x8D || high == 0x81 || high == 0x19) {
            return (high << 24) | (color & 0x00FFFFFF);
        }

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // Hardware-calibrated profiles verified on Realme GT 5 Qualcomm Lights HAL:
        // 0x8B: Pure White (#FDFFFB, r:120, g:87, b:87)
        // 0x19: Cyber Pink / Magenta (#FF2D7A, r:127, g:13, b:41)
        // 0x8C: Racing Red (#FF3B30, r:121, g:0, b:0)
        // 0x81: Cyber Amber / Orange (#FF9500, r:121, g:9, b:0)
        // 0x81: Neon Yellow / Warm Amber (#FFEA00, r:121, g:9, b:0)
        // 0x8D: Matrix Green (#00E676, r:0, g:116, b:0)
        // 0x8A: Realme Cyan / GT Blue (#71BBFF, r:42, g:0, b:60)
        // 0x8A: Electric Blue (#2979FF, r:42, g:0, b:60)
        // 0x8A: GT Purple / Violet (#A820FF, r:42, g:0, b:60)
        int[][] presets = new int[][]{
                {0x8B, 253, 255, 251}, // 0: Белый (#FDFFFB)
                {0x19, 255, 45, 122},  // 1: Розовый (#FF2D7A)
                {0x8C, 255, 59, 48},   // 2: Красный (#FF3B30)
                {0x81, 255, 149, 0},   // 3: Оранжевый (#FF9500)
                {0x81, 255, 234, 0},   // 4: Желтый (#FFEA00) -> 0x81 (Cyber Amber, 4 LEDs solid)
                {0x8D, 0, 230, 118},   // 5: Зеленый (#00E676)
                {0x8A, 113, 187, 255}, // 6: Голубой (#71BBFF) -> 0x8A (GT Blue, 4 LEDs solid)
                {0x8A, 41, 121, 255},  // 7: Синий (#2979FF) -> 0x8A (GT Blue, 4 LEDs solid)
                {0x8A, 168, 32, 255}   // 8: Фиолетовый (#A820FF) -> 0x8A (GT Purple, 4 LEDs solid)
        };

        int bestId = 0x8B;
        int minDistance = Integer.MAX_VALUE;
        for (int[] p : presets) {
            int dr = r - p[1];
            int dg = g - p[2];
            int db = b - p[3];
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestId = p[0];
            }
        }
        return (bestId << 24) | (color & 0x00FFFFFF);
    }

    public static void flashSegment(int ledsMask, int color, int autoTurnOffMs) {
        if (sAutoStopRunnable != null) {
            sHandler.removeCallbacks(sAutoStopRunnable);
            sAutoStopRunnable = null;
        }

        int hwColor = getHardwareColorForRgb(color, ledsMask);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        boolean nativeOk = callNativeSetFlashing(LIGHT_ID_HALO, hwColor, ledsMask, MODE_MULTI_LED);
        if (!nativeOk) {
            sendPacket((byte) LIGHT_ID_HALO, (byte) MODE_MULTI_LED, (byte) r, (byte) g, (byte) b, (byte) ledsMask);
        }

        // Keep Settings.Global synced
        if (sAppContext != null) {
            try {
                String hex = PulseLightManager.colorToHex(color);
                PulseLightManager.setGlobalString(sAppContext, PulseLightManager.KEY_MUSIC_COLOR, hex);
                PulseLightManager.setGlobalInt(sAppContext, PulseLightManager.KEY_MASTER_SWITCH, 1);
            } catch (Throwable ignored) {}
        }

        if (autoTurnOffMs > 0) {
            sAutoStopRunnable = RealmeGlyphDriver::turnOff;
            sHandler.postDelayed(sAutoStopRunnable, autoTurnOffMs);
        }
    }

    public static void pulse(int ledsMask, int color, int durationMs) {
        flashSegment(ledsMask, color, durationMs);
    }

    public static void turnOff() {
        if (sAutoStopRunnable != null) {
            sHandler.removeCallbacks(sAutoStopRunnable);
            sAutoStopRunnable = null;
        }
        boolean nativeOk = callNativeTurnOff(LIGHT_ID_HALO);
        if (!nativeOk) {
            sendPacket((byte) LIGHT_ID_HALO, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0);
        }
    }

    public static void turnOffImmediate() {
        turnOff();
    }

    public static void setAlwaysOn(boolean enable, int color) {
        if (sAppContext != null) {
            PulseLightManager.setAlwaysOnEnabled(sAppContext, enable);
        }
        if (enable) {
            flashSegment(LED_ALL, color, 0);
        } else {
            turnOff();
        }
    }

    private static void sendPacket(byte lightId, byte mode, byte r, byte g, byte b, byte ledsMask) {
        final byte[] packet = new byte[] {
                'P', 'L', lightId, mode, r, g, b, ledsMask
        };

        sNetExecutor.execute(() -> {
            try {
                if (sSocket == null || sSocket.isClosed() || sOut == null) {
                    java.net.Socket socket = new java.net.Socket();
                    socket.setTcpNoDelay(true);
                    socket.connect(new java.net.InetSocketAddress(DAEMON_HOST, DAEMON_PORT), 1000);
                    sOut = socket.getOutputStream();
                    sSocket = socket;
                    sIsConnected = true;
                }
                sOut.write(packet);
                sOut.flush();
            } catch (Throwable t) {
                closeSocket();
                connectAsync();
            }
        });
    }
}
