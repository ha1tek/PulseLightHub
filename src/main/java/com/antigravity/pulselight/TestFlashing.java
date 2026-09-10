package com.antigravity.pulselight;

public class TestFlashing {
    public static void main(String[] args) {
        int color = 0x8B00FF00;
        if (args.length > 0) color = (int) Long.parseLong(args[0], 16);
        int lightId = 5;
        if (args.length > 1) lightId = Integer.parseInt(args[1]);
        int leds = 15;
        if (args.length > 2) leds = Integer.parseInt(args[2]);
        int mode = 5;
        if (args.length > 3) mode = Integer.parseInt(args[3]);

        System.out.println("TESTING EXACT REALME GT 5 CALL: lightId=" + lightId + " color=0x" + Integer.toHexString(color) + " leds=" + leds + " mode=" + mode);
        try {
            Class<?> smClass = Class.forName("android.os.ServiceManager");
            java.lang.reflect.Method getService = smClass.getMethod("getService", String.class);
            android.os.IBinder binder = (android.os.IBinder) getService.invoke(null, "power");
            Class<?> stubClass = Class.forName("android.os.IPowerManager$Stub");
            java.lang.reflect.Method asInterface = stubClass.getMethod("asInterface", android.os.IBinder.class);
            Object ipm = asInterface.invoke(null, binder);
            java.lang.reflect.Method m = ipm.getClass().getMethod("setFlashing", int.class, int.class, int.class, int.class, int.class);
            m.invoke(ipm, lightId, color, leds, 0, mode);
            System.out.println("TEST: setFlashing call SUCCESSFUL!");
        } catch (Throwable t) {
            System.out.println("TEST: call failed: " + t);
            t.printStackTrace(System.out);
        }
    }
}
