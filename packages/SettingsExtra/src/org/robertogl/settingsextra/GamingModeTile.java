package org.robertogl.settingsextra;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class GamingModeTile extends TileService {

    private static String TAG = "GamingModeTile";

    private static boolean DEBUG = MainService.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    private void updateTile() {
        Tile qsTile = getQsTile();
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        Context mContext = getApplicationContext();
        boolean isActive = (tile.getState() == Tile.STATE_ACTIVE);
        if (isActive) {
            tile.setState(Tile.STATE_INACTIVE);
            Utils.disableGamingMode(mContext);
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            Utils.enableGamingMode(mContext);
        }
        tile.updateTile();
    }
}