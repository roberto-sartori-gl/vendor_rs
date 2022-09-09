package org.robertogl.settingsextra;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Looper;
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

        // Remove rotate and bluetooth icons: we don't need them
        Utils.removeUnwantendStatusBarIcon(deviceProtectedContext, "rotate");
        Utils.removeUnwantendStatusBarIcon(deviceProtectedContext, "bluetooth");

        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);

        boolean addedBackIcons = pref.getBoolean("addedBackIcon", false);

        if (!addedBackIcons) {
                Log.d(TAG, "Adding back status bar icons");
		Editor editor = pref.edit();
                editor.putBoolean("addedBackIcon", true);
                editor.commit();
                Utils.addWantedStatusBarIconIfMissing(deviceProtectedContext, "volume");
                Utils.addWantedStatusBarIconIfMissing(deviceProtectedContext, "alarm_clock");
        }
    }
}
