package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.Context;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import android.content.ContentResolver;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "SettingsExtraBootReceiver";

    private boolean isThisFirstBoot = true;

    private boolean DEBUG = false;

    public boolean isAccessServiceEnabled(Context context, String accessibilityServiceClass)
    {
	String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
	return prefString!= null && prefString.contains(context.getPackageName() + "/" + accessibilityServiceClass);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
	String package_name = context.getPackageName();
	Context deviceProtectedContext = context.createDeviceProtectedStorageContext();
	Log.d(TAG, "Starting");
	SharedPreferences pref = deviceProtectedContext.getSharedPreferences("SettingsExtraPrivatePref", MODE_PRIVATE);
	Editor editor = pref.edit();

	isThisFirstBoot = pref.getBoolean("isThisFirstBoot", true);

	if (isThisFirstBoot) {
		Log.d(TAG, "Startingfirsttime");
		editor.putBoolean("isThisFirstBoot", false);
		editor.commit();
		Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
			Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, package_name + "/" + package_name + ".KeyHandler");
		Settings.Secure.putString(deviceProtectedContext.getContentResolver(),
			Settings.Secure.ACCESSIBILITY_ENABLED, "1");
	}

	if (isAccessServiceEnabled(deviceProtectedContext, package_name + ".KeyHandler")) {
		Log.d(TAG, "Key enabled");
	}

	// Start looking into preferences available to the user
	pref = deviceProtectedContext.getSharedPreferences(package_name + "_preferences", MODE_PRIVATE);
	editor = pref.edit();

	// Check if the capacitive buttons are swapped
	boolean areWeSwapping = pref.getBoolean("buttonSwap", false);
	if (areWeSwapping) Utils.writeToFile(Utils.keySwapNode, "1", deviceProtectedContext);

	// Check if flicker free is enabled
	boolean areWeFlickerFree = pref.getBoolean("flickerFree", false);
	if (areWeFlickerFree) Utils.writeToFile(Utils.flickerFreeNode, "1", deviceProtectedContext);

	// Check the selected display color gamut
	String displayMode = pref.getString("display_mode", "normal");
	if (displayMode.equals("srgb")) Utils.writeToFile(Utils.displayModeSRGBNode, "1", deviceProtectedContext);
	else if (displayMode.equals("dcip3")) Utils.writeToFile(Utils.displayModeDCIP3Node, "1", deviceProtectedContext);
    }
}
