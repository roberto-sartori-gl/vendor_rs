package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;

import static android.content.Context.MODE_PRIVATE;

public class UserBootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "SettingsExtraUserBootReceiver";

    private boolean DEBUG = MainService.DEBUG;

    private final LedLightManager mLedLightManager = new LedLightManager();

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

        // Start the LedLightManagerService if needed
        /*SharedPreferences pref = deviceProtectedContext.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);

        boolean isLedLightManagerEnabled = pref.getBoolean("ledLightManagerEnabled", false);
        if (isLedLightManagerEnabled) {
            try {
                mLedLightManager.onStartUnlocked();
            } catch (Exception e) {
                e.printStackTrace();
                //mLedLightManager.onClose();
            }
        }*/
    }
}

