package com.antigravity.pulselight;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

public class AppItem {
    private final String appName;
    private final String packageName;
    private Drawable icon;
    private final ResolveInfo resolveInfo;
    private boolean isEnabled;
    private final boolean isSystemApp;

    public AppItem(String appName, String packageName, ResolveInfo resolveInfo, Drawable icon, boolean isEnabled, boolean isSystemApp) {
        this.appName = appName;
        this.packageName = packageName;
        this.resolveInfo = resolveInfo;
        this.icon = icon;
        this.isEnabled = isEnabled;
        this.isSystemApp = isSystemApp;
    }

    public String getAppName() {
        return appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public ResolveInfo getResolveInfo() {
        return resolveInfo;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public boolean isSystemApp() {
        return isSystemApp;
    }

    public Drawable loadIconSync(PackageManager pm) {
        if (icon != null) {
            return icon;
        }
        if (resolveInfo != null) {
            try {
                icon = resolveInfo.loadIcon(pm);
                return icon;
            } catch (Throwable ignored) {}
        }
        try {
            icon = pm.getApplicationIcon(packageName);
            return icon;
        } catch (Throwable ignored) {}
        return null;
    }
}
