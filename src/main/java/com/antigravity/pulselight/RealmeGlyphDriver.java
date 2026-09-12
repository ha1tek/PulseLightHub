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
            return "Нативный драйвер: Прямой доступ — Готов к работе ✓";
        }
        if (sIsConnected) {
            return "Аппаратный драйвер: Активен — Порт 49152 ✓";
        }
        return "Аппаратный драйвер: Инициализация";
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

    // Hardware single-color profiles verified directly in oplusLights.xml on Realme GT 5:
    // Pure single colors only. Dual-tone profiles are excluded to prevent two-color artifacts.
    private static final int[][] HARDWARE_COLOR_TABLE = new int[][]{
            // UI R,   G,   B,    HAL Payload
            { 253, 255, 251, (int) 0x8BFDFFFBL }, // Белый: #FDFFFB
            { 255,  59,  48, (int) 0x8C790000L }, // Красный: #FF3B30
            { 255, 110,   0, (int) 0x8BFFBE15L }, // Темно-оранжевый: #FFBE15
            { 255, 149,   0, (int) 0x8BFFBE14L }, // Оранжевый: #FF9500
            { 255, 175,   0, (int) 0x8BFFBE13L }, // Янтарный: #FFBE13
            { 255, 190,   0, (int) 0x8BFFBE12L }, // Теплый янтарь: #FFBE12
            { 255, 205,   0, (int) 0x8BFFBE11L }, // Золотистый янтарь: #FFBE11
            { 255, 215,   0, (int) 0x8BFFFC3CL }, // Теплый желтый: #FFFC3C
            { 255, 234,   0, (int) 0x8BFFFC3BL }, // Желтый: #FFEA00
            { 230, 245,   0, (int) 0x8BFFFC3AL }, // Лимонный: #FFFC3A
            { 190, 255,   0, (int) 0x8BFFFC39L }, // Лаймово-желтый: #FFFC39
            {   0, 230, 118, (int) 0x8D007400L }, // Зеленый: #00E676
            {   0, 245, 130, (int) 0x8B00FF1DL }, // Весенний зеленый: #00FF1D
            {   0, 255, 150, (int) 0x8B00FF1CL }, // Мятный: #00FF1C
            {   0, 255,  27, (int) 0x8B00FF1BL }, // Изумрудный: #00FF1B
            {   0, 235, 200, (int) 0x8B00FF1AL }, // Бирюзовый: #00FF1A
            {   0, 220, 240, (int) 0x8B00FF19L }, // Аква-голубой, циан: #00FF19
            {  25, 195, 255, (int) 0x8B00FF18L }, // Небесно-голубой: #00FF18
            {  35, 160, 255, (int) 0x8B00FF18L }, // Светло-синий: #00FF18
            {  41, 121, 255, (int) 0x8B74BBFFL }, // Синий: #2979FF — HAL blue
            {  25,  28, 221, (int) 0x8B73BBFFL }, // Системный ультрамарин ColorOS Always-On: #191CDD — HAL blue1 #73BBFF
            {  50,  60, 235, (int) 0x8B73BBFFL }, // Ультрамарин / глубокий сине-фиолетовый: HAL blue1 #73BBFF
            {  75,  35, 235, (int) 0x8B72BBFFL }, // Электрик индиго: HAL blue2 #72BBFF
            {  92,  39, 245, (int) 0x8B72BBFFL }, // Сине-фиолетовый индиго: HAL blue2 #72BBFF
            { 127,  25, 245, (int) 0x8B71BBFFL }, // Самый бархатный фиолетовый: #7F19F5 — HAL blue3 #71BBFF
            { 168,  32, 255, (int) 0x8B71BBFFL }, // Глубокий фиолетовый: #A820FF — HAL blue3 #71BBFF
            { 183,  39, 246, (int) 0x8BFFA8FFL }, // Пурпурный ближе к красному: #B727F6 — HAL purple #FFA8FF
            { 207,  45, 246, (int) 0x8BFFA8FFL }, // Насыщенный пурпурный / маджента: #CF2DF6 — HAL purple #FFA8FF
            { 234,  52, 246, (int) 0x8BFFA7FFL }, // Неоновый розовый / маджента: #EA34F6 — HAL purple1 #FFA7FF
            { 255,  45, 122, (int) 0x8BFFA7FFL }  // Пурпурно-розовый: HAL purple1 #FFA7FF
    };

    public static int getHardwareColorForRgb(int color) {
        return getHardwareColorForRgb(color, LED_ALL);
    }

    public static int getHardwareColorForRgb(int color, int ledsMask) {
        int rgb = color & 0x00FFFFFF;

        // 1. Аппаратные двухцветные градиентные режимы HAL: только при явном выборе из третьего ряда палитры
        if (rgb == 0xFFFFF0 || rgb == 0xFFFF0) return (int) 0x8BFFFFF0L; // Розовый дуэт: Pink и Blue
        if (rgb == 0xFFFFF1 || rgb == 0xFFFF1) return (int) 0x8BFFFFF1L; // Оранжево-Розовый
        if (rgb == 0xFFFFF2 || rgb == 0xFFFF2) return (int) 0x8BFFFFF2L; // Сине-Желтый
        if (rgb == 0xFFFFF3 || rgb == 0xFFFF3) return (int) 0x8BFFFFF3L; // Аква-Мята
        if (rgb == 0xFFFFF4 || rgb == 0xFFFF4) return (int) 0x8BFFFFF4L; // Золотой Лайм

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // 2. Чистый белый цвет: калиброванная отметка на Color Picker или палитра #FDFFFB / #FFFFFF
        if (rgb == 0xFDFFFB || rgb == 0xFFFFFF || (r >= 200 && g >= 200 && b >= 200 && Math.abs(r - g) <= 30 && Math.abs(r - b) <= 30 && Math.abs(g - b) <= 30)) {
            return (int) 0x8BFDFFFBL;
        }

        // 3. Точные заводские одиночные цвета палитры
        if (rgb == 0xFFA7FF) return (int) 0x8BFFA7FFL; // Неоновый Розовый
        if (rgb == 0xFF3B30 || rgb == 0xFF8175) return (int) 0x8C790000L; // Красный
        if (rgb == 0xFF9500 || rgb == 0xFFBE14) return (int) 0x8BFFBE14L; // Оранжевый
        if (rgb == 0xFFEA00 || rgb == 0xFFFC3C) return (int) 0x8BFFFC3BL; // Желтый
        if (rgb == 0x00E676 || rgb == 0x00FF1E) return (int) 0x8D007400L; // Зеленый
        if (rgb == 0x00FF1B) return (int) 0x8B00FF1BL; // Изумрудный
        if (rgb == 0x71BBFF) return (int) 0x8B00FF19L; // Голубой: циан и аква в палитре
        if (rgb == 0x2979FF || rgb == 0x74BBFF) return (int) 0x8B74BBFFL; // Синий: чистый синий
        if (rgb == 0x73BBFF || rgb == 0x191CDD) return (int) 0x8B73BBFFL; // Системный ультрамарин из Always-On #73BBFF
        if (rgb == 0x72BBFF || rgb == 0x491AE6) return (int) 0x8B72BBFFL; // Сине-фиолетовый индиго #72BBFF
        if (rgb == 0xA820FF || rgb == 0x7F19F5) return (int) 0x8B71BBFFL; // Фиолетовый — самый насыщенный бархатный фиолетовый #71BBFF
        if (rgb == 0xFFA8FF || rgb == 0xB727F6 || rgb == 0xCF2DF6) return (int) 0x8BFFA8FFL; // Пурпурный ближе к красному #FFA8FF

        // 4. Поиск ближайшего аппаратного оттенка для Color Picker
        // Используется таблица из чистых однотонных оттенков HAL без двухцветных режимов
        int bestPayload = (int) 0x8B71BBFFL;
        int minDistance = Integer.MAX_VALUE;
        for (int[] p : HARDWARE_COLOR_TABLE) {
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
