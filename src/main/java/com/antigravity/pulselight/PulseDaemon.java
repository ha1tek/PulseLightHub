package com.antigravity.pulselight;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class PulseDaemon {
    public static final int PORT = 49152;
    private static Object ipm = null;
    private static java.lang.reflect.Method setFlashingMethod = null;

    public static final int LIGHT_MULTI_COLOR_PRE = 0x8B000000;

    public static void main(String[] args) {
        System.out.println("=== PulseDaemon Initializing ===");
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            android.os.IBinder binder = (android.os.IBinder) getService.invoke(null, "power");
            Class<?> stubClass = Class.forName("android.os.IPowerManager$Stub");
            java.lang.reflect.Method asInterface = stubClass.getMethod("asInterface", android.os.IBinder.class);
            ipm = asInterface.invoke(null, binder);
            setFlashingMethod = ipm.getClass().getMethod("setFlashing", int.class, int.class, int.class, int.class, int.class);
            System.out.println("PulseDaemon: IPowerManager hooked successfully!");
        } catch (Throwable t) {
            System.err.println("PulseDaemon: Failed to hook IPowerManager: " + t);
            t.printStackTrace();
            return;
        }
        if (args.length > 0 && "--dump".equals(args[0])) {
            System.out.println("--- IPowerManager Methods ---");
            for (java.lang.reflect.Method m : ipm.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("flash") || m.getName().toLowerCase().contains("light") || m.getName().toLowerCase().contains("breath")) {
                    System.out.println("  " + m);
                }
            }
            try {
                Class<?> smClass = Class.forName("android.os.ServiceManager");
                java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
                android.os.IBinder lightsBinder = (android.os.IBinder) getService.invoke(null, "lights");
                if (lightsBinder != null) {
                    System.out.println("--- Lights Binder found: " + lightsBinder.getInterfaceDescriptor());
                }
            } catch (Throwable ignored) {}
            return;
        }

        if (args.length > 0 && "--test".equals(args[0])) {
            int testId = args.length > 1 ? Integer.parseInt(args[1]) : 5;
            long rawColor = args.length > 2 ? Long.parseLong(args[2], 16) : 0x00FF00L;
            int testLeds = args.length > 3 ? Integer.parseInt(args[3]) : 15;
            int testMode = args.length > 4 ? Integer.parseInt(args[4]) : 5;
            int hwColor = (int) rawColor;
            if (hwColor != 0 && (hwColor & 0xFF000000) == 0) {
                hwColor = (hwColor & 0x00FFFFFF) | LIGHT_MULTI_COLOR_PRE;
            }
            System.out.println("Testing setFlashing: id=" + testId + " hwColor=0x" + Integer.toHexString(hwColor) + " leds=" + testLeds + " mode=" + testMode);
            try {
                Object ret = setFlashingMethod.invoke(ipm, testId, hwColor, testLeds, 0, testMode);
                System.out.println("Invocation returned: " + ret);
            } catch (Throwable t) {
                System.err.println("Invocation failed: " + t);
                t.printStackTrace();
            }
            return;
        }

        try {
            Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "customize_breath_light_flat_switch", "0"}).waitFor();
            Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "customize_breath_light_time", "00002359"}).waitFor();
            Runtime.getRuntime().exec(new String[]{"settings", "put", "global", "customize_breath_light_master_switch", "1"}).waitFor();
        } catch (Throwable ignored) {}

        try {
            // Clean any previous stale states in LightsServiceExtImpl arbitration
            setFlashingMethod.invoke(ipm, 7, 0, 0, 0, 0);
            setFlashingMethod.invoke(ipm, 5, 0, 0, 0, 0);
            setFlashingMethod.invoke(ipm, 2, 0, 0, 0, 0);
        } catch (Throwable ignored) {}

        try {
            ServerSocket server = new ServerSocket();
            server.setReuseAddress(true);
            server.bind(new java.net.InetSocketAddress("127.0.0.1", PORT), 50);
            System.out.println("PulseDaemon: Listening on 127.0.0.1:" + PORT);
            while (true) {
                try {
                    final Socket client = server.accept();
                    new Thread(() -> handleClient(client, ipm, setFlashingMethod)).start();
                } catch (Throwable t) {
                    // server accept error
                }
            }
        } catch (Throwable t) {
            System.err.println("PulseDaemon: Server error: " + t);
            t.printStackTrace();
        }
    }

    private static void handleClient(Socket client, Object ipm, java.lang.reflect.Method setFlashingMethod) {
        try {
            client.setTcpNoDelay(true);
            InputStream in = client.getInputStream();
            byte[] buf = new byte[8];
            while (true) {
                int total = 0;
                while (total < 8) {
                    int r = in.read(buf, total, 8 - total);
                    if (r == -1) break;
                    total += r;
                }
                if (total < 8) break;

                // Protocol:
                // byte 0: 'P' (0x50)
                // byte 1: 'L' (0x4C)
                // byte 2: lightId (default 7 -> decrements to 6 for rear Awakening Halo)
                // byte 3: mode (default 5, 0 for off)
                // byte 4: red
                // byte 5: green
                // byte 6: blue
                // byte 7: leds bitmask (1=A, 8=B, 4=C, 2=D, 15=ALL, 0=OFF)
                if (buf[0] == 0x50 && buf[1] == 0x4C) {
                    int lightId = buf[2] & 0xFF;
                    int mode = buf[3] & 0xFF;
                    int red = buf[4] & 0xFF;
                    int green = buf[5] & 0xFF;
                    int blue = buf[6] & 0xFF;
                    int rgb = (red << 16) | (green << 8) | blue;
                    int leds = buf[7] & 0xFF;

                    int targetId = (lightId > 0) ? lightId : 5;
                    int targetMode = (mode > 0) ? mode : 5;

                    if (leds == 0 || rgb == 0) {
                        System.out.println("PulseDaemon: Turn OFF (targetId=" + targetId + ")");
                        try {
                            setFlashingMethod.invoke(ipm, targetId, 0, 0, 0, 5);
                        } catch (Throwable ignored) {}
                        try {
                            setFlashingMethod.invoke(ipm, targetId, 0x11000000, 0, 0, 0);
                        } catch (Throwable ignored) {}
                        try {
                            setFlashingMethod.invoke(ipm, targetId, 0, 0, 0, 0);
                        } catch (Throwable ignored) {}
                    } else {
                        int hwColor = RealmeGlyphDriver.getHardwareColorForRgb(rgb);
                        System.out.println("PulseDaemon: Flash (targetId=" + targetId + ", hwColor=0x" + Integer.toHexString(hwColor) + ", leds=" + leds + ", mode=" + targetMode + ")");
                        try {
                            setFlashingMethod.invoke(ipm, targetId, hwColor, leds, 0, targetMode);
                        } catch (Throwable t) {
                            System.err.println("PulseDaemon: invoke(" + targetId + ") error: " + t);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        } finally {
            try { client.close(); } catch (Throwable ignored) {}
        }
    }
}
