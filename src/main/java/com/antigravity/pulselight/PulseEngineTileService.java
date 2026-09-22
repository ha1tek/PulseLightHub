package com.antigravity.pulselight;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class PulseEngineTileService extends TileService {
    private static final String TAG = "PulseEngineTile";

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        try {
            if (PulseAudioService.hasProjectionData()) {
                if (PulseAudioService.isEngineEnabled()) {
                    PulseLightingCoordinator.deactivateAudio(this);
                } else {
                    PulseLightingCoordinator.activateAudio(this);
                }
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("request_projection", true);
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
        } catch (Throwable t) {
            Log.e(TAG, "Error handling tile click: " + t, t);
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = PulseAudioService.isEngineEnabled();
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Аудиодвижок");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_engine));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? "Включен" : "Выключен");
        }
        tile.updateTile();
    }
}
