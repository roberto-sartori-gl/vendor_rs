package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class UserBootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "SettingsExtraUserBootReceiver";

    private boolean DEBUG = MainService.DEBUG;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (DEBUG) Log.d(TAG, "Starting SettingsExtraUserBootReceiver");
        Context deviceProtectedContext = context.createDeviceProtectedStorageContext();

        Handler NfcHandler = new Handler(Looper.getMainLooper());
        // Update the NfcTile with current status
        Runnable nfcRunnable = new Runnable() {
            @Override
            public void run() {
                NfcTile.requestListeningState(deviceProtectedContext, new ComponentName(deviceProtectedContext, NfcTile.class));
            }
        };
        NfcHandler.post(nfcRunnable);

        Handler AdaptiveBrightnessHandler = new Handler(Looper.getMainLooper());
        // Update the AdaptiveBrightnessTile with current status
        Runnable adaptiveBrightnessRunnable = new Runnable() {
            @Override
            public void run() {
                AdaptiveBrightnessTile.requestListeningState(deviceProtectedContext, new ComponentName(deviceProtectedContext, AdaptiveBrightnessTile.class));
            }
        };
        AdaptiveBrightnessHandler.post(adaptiveBrightnessRunnable);

        Handler gamingTileHandler = new Handler(Looper.getMainLooper());
        // Update the Gaming Tile with current status
        Runnable gamingTileRunnable = new Runnable() {
            @Override
            public void run() {
                GamingModeTile.requestListeningState(deviceProtectedContext, new ComponentName(deviceProtectedContext, GamingModeTile.class));
                Utils.disableGamingMode(context);
            }
        };
        gamingTileHandler.post(gamingTileRunnable);

        // Disable volume and alarm AOSP icons
        // We will use our own
        Utils.removeUnwantendStatusBarIcon(deviceProtectedContext, "volume");
        Utils.removeUnwantendStatusBarIcon(deviceProtectedContext, "alarm_clock");

        // Remove rotate icon: we don't need it
        Utils.removeUnwantendStatusBarIcon(deviceProtectedContext, "rotate");

        // Add back wifi and cell network tile
        String currentTiles = Settings.Secure.getString(deviceProtectedContext.getContentResolver(), Settings.Secure.QS_TILES);
        if (!currentTiles.contains("wifi")) {
            Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
                Settings.Secure.QS_TILES,currentTiles + ",wifi");
        }

        currentTiles = Settings.Secure.getString(deviceProtectedContext.getContentResolver(), Settings.Secure.QS_TILES);
        if (!currentTiles.contains("cell")) {
            Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
                Settings.Secure.QS_TILES,currentTiles + ",cell");
        }
    }
}
