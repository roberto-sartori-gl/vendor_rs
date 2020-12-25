package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.provider.Settings;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "SettingsExtraBootReceiver";

    private boolean DEBUG = MainService.DEBUG;

    private static final String AccessibilityService = ".MainService";

    public boolean isAccessServiceEnabled(Context context, String accessibilityServiceClass) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString != null && prefString.contains(context.getPackageName() + "/" + accessibilityServiceClass);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String package_name = context.getPackageName();
        Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
        Log.d(TAG, "Starting");

        // Always enable the MainService at boot: we need it
        // It should be necessary only at first boot, but in case we change the AccessibilityService name
        // or the user disable it, just enable it again after the boot
        Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, package_name + "/" + package_name + AccessibilityService);
        Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, "1");

        if (isAccessServiceEnabled(deviceProtectedContext, package_name + AccessibilityService)) {
            Log.d(TAG, AccessibilityService + " enabled");
        }

        // Start looking into preferences available to the user
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(package_name + "_preferences", MODE_PRIVATE);
        //Editor editor = pref.edit();

        // Check if the capacitive buttons are swapped
        boolean areWeSwapping = pref.getBoolean("buttonSwap", false);
        if (areWeSwapping) Utils.writeToFile(Utils.keySwapNode, "1", deviceProtectedContext);

        // Check if flicker free is enabled
        boolean areWeFlickerFree = pref.getBoolean("flickerFree", false);
        if (areWeFlickerFree) Utils.writeToFile(Utils.flickerFreeNode, "1", deviceProtectedContext);

        // Check the selected display color gamut
        String displayMode = pref.getString("display_mode", "normal");
        if (displayMode.equals("srgb"))
            Utils.writeToFile(Utils.displayModeSRGBNode, "1", deviceProtectedContext);
        else if (displayMode.equals("dcip3"))
            Utils.writeToFile(Utils.displayModeDCIP3Node, "1", deviceProtectedContext);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        // Update the NfcTile with current status
        Runnable nfcRunnable = new Runnable() {
            @Override
            public void run() {
                NfcTile.requestListeningState(deviceProtectedContext, new ComponentName(deviceProtectedContext, NfcTile.class));
            }
        };
        // Update the AdaptiveBrightnessTile with current status
        Runnable adaptiveBrightnessRunnable = new Runnable() {
            @Override
            public void run() {
                AdaptiveBrightnessTile.requestListeningState(deviceProtectedContext, new ComponentName(deviceProtectedContext, AdaptiveBrightnessTile.class));
            }
        };
        mainHandler.post(nfcRunnable);
        mainHandler.post(adaptiveBrightnessRunnable);
    }
}
