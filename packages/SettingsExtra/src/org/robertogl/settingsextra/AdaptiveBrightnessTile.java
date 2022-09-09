package org.robertogl.settingsextra;

import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

public class AdaptiveBrightnessTile extends TileService {

    private static String TAG = "AdaptiveBrightnessTile";

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
        updateTile();
    }

    private void updateTile() {
        Tile qsTile = getQsTile();
        int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (DEBUG) Log.d(TAG, "mode: " + mode);
        if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            // Auto brightness is enabled
            qsTile.setState(Tile.STATE_ACTIVE);
            qsTile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_adaptive_brightess));
        } else {
            // Auto brightness is disabled
            qsTile.setState(Tile.STATE_INACTIVE);
            qsTile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_auto_brightness_off));
        }
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        boolean isActive = (tile.getState() == Tile.STATE_ACTIVE);
        if (isActive) {
            tile.setState(Tile.STATE_INACTIVE);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_auto_brightness_off));
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            tile.setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_adaptive_brightess));
        }
        tile.updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}