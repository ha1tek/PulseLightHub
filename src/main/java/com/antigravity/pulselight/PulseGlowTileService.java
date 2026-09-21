package com.antigravity.pulselight;

import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class PulseGlowTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTileState();
    }

    @Override
    public void onClick() {
        super.onClick();
        int mode = PulseLightingCoordinator.getMode(this);
        if (mode == PulseLightingCoordinator.MODE_GLOW) {
            PulseLightingCoordinator.deactivateGlow(this);
        } else {
            PulseLightingCoordinator.activateGlow(this);
        }
        updateTileState();
    }

    private void updateTileState() {
        Tile tile = getQsTile();
        if (tile == null) return;

        boolean active = PulseLightingCoordinator.getMode(this) == PulseLightingCoordinator.MODE_GLOW;
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("Свечение");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_qs_glow));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.setSubtitle(active ? "Включено" : "Выключено");
        }
        tile.updateTile();
    }
}
