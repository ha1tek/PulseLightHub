package com.antigravity.pulselight;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class PulseBreatheTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        int mode = PulseLightingCoordinator.getMode(this);
        if (mode == PulseLightingCoordinator.MODE_BREATHE) {
            PulseLightingCoordinator.deactivateBreathe(this);
        } else {
            PulseLightingCoordinator.activateBreathe(this);
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = PulseLightingCoordinator.getMode(this) == PulseLightingCoordinator.MODE_BREATHE;
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Пульсация");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_breathe));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? "Включена" : "Выключена");
        }
        tile.updateTile();
    }
}
