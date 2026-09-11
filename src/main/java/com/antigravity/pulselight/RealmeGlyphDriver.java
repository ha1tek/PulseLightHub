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

    // Hardware-calibrated profiles verified directly on Realme GT 5 Qualcomm Lights HAL:
    // Every single profile uses Mode 5 (horse_race_lamp) for guaranteed 1-to-1 per-segment addressing
    // with exact physical LED colors from oplusLights.xml / Lights HAL table.
    private static final int[][] COLOR_PROFILES = new int[][]{
            // UI R, G, B,  Hardware 32-bit payload
            // --- 8 СТРОГО ЗАФИКСИРОВАННЫХ ЦВЕТОВ (БЕЗ ИЗМЕНЕНИЙ) ---
            { 253, 255, 251, (int) 0x8BFDFFFBL }, // 0: Белый (#FDFFFB) -> HAL: r:120, g:87, b:87
            { 255,  59,  48, (int) 0x8C790000L }, // 2: Красный (#FF3B30) -> HAL: r:121, g:0, b:0
            { 255, 149,   0, (int) 0x8BFFBE14L }, // 3: Оранжевый (#FF9500) -> HAL: r:121, g:9, b:0
            { 255, 234,   0, (int) 0x8BFFFC3BL }, // 4: Желтый (#FFEA00) -> HAL: r:69, g:57, b:0
            {   0, 230, 118, (int) 0x8D007400L }, // 5: Зеленый (#00E676) -> HAL: r:0, g:116, b:0
            { 113, 187, 255, (int) 0x8B00FF19L }, // 6: Голубой (#71BBFF) -> HAL: r:0, g:38, b:60
            {  41, 121, 255, (int) 0x8B74BBFFL }, // 7: Синий (#2979FF) -> HAL: r:0, g:0, b:85
            { 168,  32, 255, (int) 0x8C71BBFFL }, // 8: Фиолетовый (#A820FF) -> HAL: r:42, g:0, b:60

            // --- РОЗОВЫЙ ГРАДИЕНТ (Pink + Blue dual-tone) ---
            { 255,  45, 122, (int) 0x8BFFFFF0L }, // 1: Розовый (#FF2D7A / #FFFFF0) -> HAL: r:127,127,49,49 (Pink + Blue)

            // --- НОВЫЕ АППАРАТНЫЕ ЦВЕТА HAL ---
            { 255, 167, 255, (int) 0x8BFFA7FFL }, // 9: Неоновый Розовый (#FFA7FF) -> HAL: r:77, g:0, b:34 (Pure Pink)
            {   0, 255,  27, (int) 0x8B00FF1BL }, // 10: Изумрудный (#00FF1B) -> HAL: r:0, g:67, b:40 (Teal Green)
            { 255, 255, 241, (int) 0x8BFFFFF1L }, // 11: Оранжево-Розовый (#FFFFF1) -> HAL: Orange + Pink
            { 255, 255, 242, (int) 0x8BFFFFF2L }, // 12: Сине-Желтый (#FFFFF2) -> HAL: Blue + Yellow
            { 255, 255, 243, (int) 0x8BFFFFF3L }, // 13: Аква-Мята (#FFFFF3) -> HAL: Cyan + Mint
            { 255, 255, 244, (int) 0x8BFFFFF4L }  // 14: Золотой Лайм (#FFFFF4) -> HAL: Gold + Lime
    };

    public static int getHardwareColorForRgb(int color) {
        return getHardwareColorForRgb(color, LED_ALL);
    }

    public static int getHardwareColorForRgb(int color, int ledsMask) {
        int rgb = color & 0x00FFFFFF;
        if (rgb == 0xFFFF0 || rgb == 0xFF2D7A) return (int) 0x8BFFFFF0L;
        if (rgb == 0xFFFF1) return (int) 0x8BFFFFF1L;
        if (rgb == 0xFFFF2) return (int) 0x8BFFFFF2L;
        if (rgb == 0xFFFF3) return (int) 0x8BFFFFF3L;
        if (rgb == 0xFFFF4) return (int) 0x8BFFFFF4L;
        if (rgb == 0xFFA7FF) return (int) 0x8BFFA7FFL;
        if (rgb == 0x00FF1B) return (int) 0x8B00FF1BL;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        int bestPayload = (int) 0x8C71BBFFL;
        int minDistance = Integer.MAX_VALUE;
        for (int[] p : COLOR_PROFILES) {
            int dr = r - p[0];
            int dg = g - p[1];
            int db = b - p[2];
            int dist = dr * dr + dg * dg + db * db;
            if (dist < minDistance) {
                minDistance = dist;
                bestPayload = p[3];
            }
        }
        return bestPayload;
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

        // Keep Settings.Global synced on static preview flashes
        if (autoTurnOffMs > 0 && sAppContext != null) {
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
