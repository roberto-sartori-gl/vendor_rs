package org.robertogl.settingsextra;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.util.Log;
import android.content.Intent;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

import static android.content.Context.MODE_PRIVATE;

public final class Utils {

    private static final String TAG = "SettingsExtraUtils";

    private static final boolean DEBUG = MainService.DEBUG;

    protected static String keySwapNode = "/proc/s1302/key_rep";

    protected static String displayModeSRGBNode = "/sys/devices/virtual/graphics/fb0/srgb";

    protected static String displayModeDCIP3Node = "/sys/devices/virtual/graphics/fb0/dci_p3";

    protected static String flickerFreeNode = "/proc/flicker_free/flicker_free";

    protected static String dozeWakeupNode = "/proc/touchpanel/tp_f4";

    protected static String flickerFreeMinBrightness = "/proc/flicker_free/flicker_free";

    protected static String doubleTapToWakeNode = "/proc/touchpanel/double_tap_enable";

    protected static String disableCapacitiveKeyNode = "/proc/touchpanel/key_disable";

    protected static final String vibrationIntensityString = "vibrationIntensityString";

    protected static final String CHEESEBURGER_FP_PROXIMITY_FILE =
            "/sys/devices/soc/soc:fpc_fpc1020/proximity_state";

    protected static String readFromFile(String path) {
        String aBuffer = "";
        try {
            File myFile = new File(path);
            FileInputStream fIn = new FileInputStream(myFile);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                aBuffer += aDataRow;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return aBuffer;
    }

    protected static void writeToFile(String path, String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(path)));

            //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(path, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    protected static void doHapticFeedback(Vibrator mVibrator, int msVibrationLenght) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(VibrationEffect.createOneShot(msVibrationLenght,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    protected static boolean isScreenOn(Context mContext) {
        DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (DEBUG) Log.d(TAG, "Display state: " + display.getState());
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }

    protected static void setProp(String property, String value) {
        Process sh = null;
        String[] cmd = {"setprop", property, value};
        try {
            sh = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            sh.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected static String getProp(String property) {
        Process sh = null;
        BufferedReader reader = null;
        String[] cmd = {"getprop", property};
        try {
            sh = Runtime.getRuntime().exec(cmd);
            reader = new BufferedReader(new InputStreamReader(sh.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return reader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected static boolean isAlwaysOnDisplayEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                "double_tap_to_wake", 0) != 0;
    }

    protected static void removeUnwantendStatusBarIcon(Context context, String unWantedIcon) {
        String currentBlackList = "";
        currentBlackList = Settings.Secure.getString(context.getContentResolver(), "icon_blacklist");
        if (currentBlackList == null) {
            if (DEBUG) Log.d(TAG, "currentBlackList is null, adding out unwanted icon");
            Settings.Secure.putString(context.getContentResolver(), "icon_blacklist", "," + unWantedIcon);
            return;
        }
        if (currentBlackList.contains(unWantedIcon)) {
            if (DEBUG) Log.d(TAG, "currentBlackList already contains our unwated icon");
            return;
        } else {
            if (DEBUG) Log.d(TAG, "currentBlackList does not contain our unwated icon, adding it!");
            Settings.Secure.putString(context.getContentResolver(), "icon_blacklist", currentBlackList + "," + unWantedIcon);
        }
    }

    protected static void setHeadsUpNotification(String value, Context context){
        Settings.Global.putString(context.getContentResolver(),
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, value);
    }

    protected static void setVibrationIntensity(String value, Context context){
        int max = 3596;
        int min = 116;
        int f_value = (max / 100) * Integer.valueOf(value);
        if (f_value < min) f_value = min + 1;
        if (DEBUG) Log.d (TAG, "vibration intensity: " + f_value);
        try {
                writeToFile("/sys/devices/virtual/timed_output/vibrator/vtg_level", String.valueOf(f_value), context);
        } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "This is not Lazy kernel!");
        }
        try {
                writeToFile("/sys/devices/virtual/timed_output/vibrator/vmax_mv", String.valueOf(f_value), context);
        } catch (Exception e) {
                if (DEBUG) Log.d(TAG, "This is not Lineage kernel!");
        }
    }

    protected static void vibrate(int vibrationLength, Context context) {
        try {
            writeToFile("/sys/devices/virtual/timed_output/vibrator/enable", String.valueOf(vibrationLength), context);
        } catch (Exception e) {
            if (DEBUG) Log.d(TAG, "Cannot start vibration!");
        }
    }

    public static <T> int[] indexOfMultiple(List<T> list, T object) {
        return IntStream.range(0, list.size())
                .filter(i -> Objects.equals(object, list.get(i)))
                .toArray();
    }

    protected static void enableGamingMode(Context mContext) {
        if (DEBUG) Log.d(TAG, "Enabling Gaming Mode");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();

        // Disable capacitive keys
        Utils.writeToFile(Utils.disableCapacitiveKeyNode, "1", mContext);
        // Hack: we can already disable the home button using the proximity feature of the fingerprint senso
        // Instead of adding new property, we just use the same
        Utils.writeToFile(Utils.CHEESEBURGER_FP_PROXIMITY_FILE, "1", mContext);
        // Save current status for automatic brightness
        int mode = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        if (DEBUG) Log.d(TAG, "Auto brightness mode: " + mode);
        prefEditor.putString("gm_extra.autobrightness", String.valueOf(mode));
        // Disable auto brightness
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        // Commit changes to the shared preferences
        prefEditor.commit();
        // Disable heads up notifications
        Utils.setHeadsUpNotification("0", deviceProtectedContext);
        if (DEBUG) Log.d(TAG, "Enabled Gaming Mode");
    }

    protected static void disableGamingMode(Context mContext) {
        if (DEBUG) Log.d(TAG, "Disabling Gaming Mode");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = pref.edit();
        // Enable capacitive keys
        Utils.writeToFile(Utils.disableCapacitiveKeyNode, "0", mContext);
        // Enable fingerprint sensor
        Utils.writeToFile(Utils.CHEESEBURGER_FP_PROXIMITY_FILE, "0", mContext);
        // Restore previous status for heads up and automatic brightness
        String autoBrightnessValue = pref.getString("gm_extra.autobrightness", "2");
        boolean areHeadsUpEnabled = pref.getBoolean("headsUpNotificationsEnabled", true);
        if (!autoBrightnessValue.equals("2")) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE, Integer.valueOf(autoBrightnessValue));
            prefEditor.remove("gm_extra.autobrightness");
        }
        if (areHeadsUpEnabled) {
            Utils.setHeadsUpNotification("1", deviceProtectedContext);
        }
        prefEditor.commit();
        if (DEBUG) Log.d(TAG, "Disabled Gaming Mode");
    }
}
