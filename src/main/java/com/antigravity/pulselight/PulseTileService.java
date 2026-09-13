package com.antigravity.pulselight;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class PulseTileService extends TileService {
    private static final String TAG = "PulseTileService";

    public static void updateTileState(Context context) {
        if (context != null) {
            try {
                requestListeningState(context, new ComponentName(context, PulseTileService.class));
            } catch (Throwable t) {
                Log.w(TAG, "Failed to request listening state: " + t);
            }
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        refreshTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean isRunning = PulseAudioService.isRunning();
        boolean isEnabled = PulseAudioService.isEngineEnabled();

        if (isRunning) {
            if (isEnabled) {
                PulseAudioService.pauseEngine();
            } else {
                PulseAudioService.resumeEngine();
            }
        } else {
            if (PulseAudioService.hasProjectionData()) {
                PulseAudioService.startEngine(this);
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= 34) {
                    PendingIntent pi = PendingIntent.getActivity(
                            this, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    startActivityAndCollapse(pi);
                } else {
                    startActivityAndCollapse(intent);
                }
                return;
            }
        }

        refreshTile();
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean isRunning = PulseAudioService.isRunning();
        boolean isEnabled = PulseAudioService.isEngineEnabled();
        boolean active = isRunning && isEnabled;

        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Аудио-движок");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? "Включен" : "Выключен");
        }
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_pulse));
        tile.updateTile();
    }
}
