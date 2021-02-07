package org.robertogl.settingsextra;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.BatteryManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class LedLightManager extends NotificationListenerService {
    private static final String TAG = "LedManagerExtra";

    private static final boolean DEBUG = MainService.DEBUG;

    private String configPath = "/sdcard/SettingsExtraLedConfiguration.conf";

    private Boolean configFileAlreadyLoaded = false;
    private Boolean isListenerConnected = false;
    private Boolean wasServiceStarted = false;

    private Context mContext = null;
    static int RAMP_SIZE = 8;
    static int RAMP_STEP_DURATION = 50;

    static int[] BRIGHTNESS_RAMP = {0, 12, 25, 37, 50, 72, 85, 100};

    private final List<String> enabledApps = new ArrayList<>();
    private final List<String> enabledStringForApps = new ArrayList<>();
    private final List<String> enabledColorsForApps = new ArrayList<>();
    private final List<Boolean> enabledBlinkForApps = new ArrayList<>();
    private final List<String> enabledOnMsForApps = new ArrayList<>();
    private final List<String> enabledOffMsForApps = new ArrayList<>();

    private final List<String> currentEnabledApps = new ArrayList<>();
    private final List<String> currentEnabledStringForApps = new ArrayList<>();
    private final List<String> currentEnabledColorsForApps = new ArrayList<>();
    private final List<Boolean> currentEnabledBlinkForApps = new ArrayList<>();
    private final List<String> currentEnabledOnMsForApps = new ArrayList<>();
    private final List<String> currentEnabledOffMsForApps = new ArrayList<>();

    int offMs = 0;
    int onMs = 0;

    public LedLightManager() {
        super();
    }

    @Override
    public void onListenerConnected(){
        isListenerConnected = true;
    }

    @Override
    public void onListenerDisconnected(){
        isListenerConnected = false;
    }

    private void onClose() {
        // Disable our Light daemon
        if (DEBUG) Log.d(TAG, "Disabling LED manager");
        disableLed();
        Utils.setProp("persist.sys.disable.rgb", "");
        configFileAlreadyLoaded = false;
        if (isListenerConnected) {
            requestUnbind();
            isListenerConnected = false;
        }
    }

    private void onPowerOn() {
        if (!wasServiceStarted) {
            IntentFilter powerActionFilter = new IntentFilter();
            powerActionFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            powerActionFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            powerActionFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(mPowerReceiver, powerActionFilter);

            if (isPowerConnected()) setColor("FFFF00", false, "0", "0");
            if (isBatteryFull()) setColor("00FF00", false, "0", "0");
            wasServiceStarted = true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplication();

        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);

        pref.registerOnSharedPreferenceChangeListener(mPreferenceListener);

        if (DEBUG) Log.d(TAG, "onCreate");

        if (!Utils.getProp("persist.sys.disable.rgb").equals("1")) {
            if (DEBUG) Log.d(TAG, "Service is disabled");
            requestUnbind();
            return;
        }

        onPowerOn();

    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "ledManagerExtraEnabled":
                    if (DEBUG) Log.d(TAG, "Settings for Led Manager changed");
                    boolean isLedManagerEnabled = prefs.getBoolean("ledManagerExtraEnabled", false);
                    if (isLedManagerEnabled) {
                        if (DEBUG) Log.d(TAG, "Enabling Led Manager Extra");
                        Utils.setProp("persist.sys.disable.rgb", "1");
                        onPowerOn();
                        requestRebind(new ComponentName(getApplicationContext(), LedLightManager.class));
                    } else {
                        if (DEBUG) Log.d(TAG, "Disabling Led Manager Extra");
                        Utils.setProp("persist.sys.disable.rgb", "");
                        onClose();
                    }
            }
        }
    };

    private final BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Utils.getProp("persist.sys.disable.rgb").equals("1")){
                if (DEBUG) Log.d(TAG, "Service is disabled");
                unregisterReceiver(mPowerReceiver);
                return;
            }

            switch (intent.getAction()) {
                case Intent.ACTION_BATTERY_CHANGED:
                case Intent.ACTION_POWER_CONNECTED:
                    if (DEBUG) Log.d(TAG, "Power connected or battery status changed");
                    if (DEBUG) Log.d(TAG, "currentEnabledApps: " + currentEnabledApps.size());
                    if (currentEnabledApps.isEmpty() || currentEnabledApps == null) {
                        if (isBatteryFull() && isPowerConnected()) {
                            if (currentEnabledApps.isEmpty() || currentEnabledApps == null) {
                                // Set color for battery full (green)
                                if (DEBUG) Log.d(TAG, "Setting battery full led");
                                setColor("00FF00", false,
                                        "0", "0");
                            }
                        } else if (isPowerConnected()) {
                            // Set color for charging (yellow)
                            if (DEBUG) Log.d(TAG, "Setting charging led");
                            setColor("FFFF00", false,
                                    "0", "0");
                        }
                    }
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    if (DEBUG) Log.d(TAG, "Power disconnected");
                    if (currentEnabledApps.isEmpty() || currentEnabledApps == null) {
                        // Turn off led
                        if (DEBUG) Log.d(TAG, "Turning off led");
                        turnOffLed();
                    }
                    break;
            }
        }
    };

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        if (DEBUG) Log.d(TAG, "Status: " + Utils.getProp("persist.sys.disable.rgb"));
        if (!Utils.getProp("persist.sys.disable.rgb").equals("1")){
            if (DEBUG) Log.d(TAG, "Service is disabled");
            return;
        }

        if (notification != null && notification.getNotification() != null) {
            getNotificationAndSetLed(notification);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (!Utils.getProp("persist.sys.disable.rgb").equals("1")){
            if (DEBUG) Log.d(TAG, "Service is disabled");
            return;
        }

        String packageName = notification.getPackageName();
        if (DEBUG) Log.d(TAG, packageName);
        String title = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        /*String message = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        String message_lines = null;
        CharSequence[] lines = notification.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (!(lines == null) && !(lines.length == 0)) {
            for (CharSequence line : lines) {
                message_lines += line.toString();
            }
        }*/
        if (currentEnabledApps.contains(packageName)) {
            if (DEBUG) Log.d(TAG, "package found: " + packageName);

            int[] occurencesArray = Utils.indexOfMultiple(currentEnabledApps, packageName);
            if (DEBUG) Log.d(TAG, "Occurrences found: " + occurencesArray.length);
            boolean isFound = false;
            int defaultIndex = -1;
            for (int i = 0; i < occurencesArray.length ; i++) {
                // Check if we have strings for this package
                if (DEBUG) Log.d(TAG, "title: " + title);
                if (title != null) {
                    isFound = title.toLowerCase().contains(currentEnabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }
                /*if (message != null) {
                    isFound = isFound || message.toLowerCase().contains(currentEnabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }
                if (message_lines != null) {
                    isFound = isFound || message_lines.toLowerCase().contains(currentEnabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }*/
                if (isFound && !enabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    currentEnabledApps.remove(occurencesArray[i]);
                    currentEnabledStringForApps.remove(occurencesArray[i]);
                    currentEnabledColorsForApps.remove(occurencesArray[i]);
                    currentEnabledOnMsForApps.remove(occurencesArray[i]);
                    currentEnabledOffMsForApps.remove(occurencesArray[i]);
                    currentEnabledBlinkForApps.remove(occurencesArray[i]);
                    break;
                } else if (enabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    if (DEBUG) Log.d(TAG, "found an empty string removing led");
                    defaultIndex = occurencesArray[i];
                }
                isFound = false;
            }
            if (defaultIndex != -1 && !isFound) {
                currentEnabledApps.remove(defaultIndex);
                currentEnabledStringForApps.remove(defaultIndex);
                currentEnabledColorsForApps.remove(defaultIndex);
                currentEnabledOnMsForApps.remove(defaultIndex);
                currentEnabledOffMsForApps.remove(defaultIndex);
                currentEnabledBlinkForApps.remove(defaultIndex);
            }
            // Set the color of the latest notification available
            if (currentEnabledColorsForApps.size() > 0) {
                setColor(currentEnabledColorsForApps.get(currentEnabledColorsForApps.size() - 1),
                        currentEnabledBlinkForApps.get(currentEnabledColorsForApps.size() - 1),
                        currentEnabledOnMsForApps.get(currentEnabledOnMsForApps.size() - 1),
                        currentEnabledOffMsForApps.get(currentEnabledOffMsForApps.size() - 1));
            } else {
                disableLed();
            }
        }
        if (isPowerConnected() && (currentEnabledApps.isEmpty() || currentEnabledApps == null)) {
            if (isBatteryFull() && isPowerConnected()) {
                // Show led for full charge (green)
                if (DEBUG) Log.d(TAG, "Setting battery full led");
                setColor("00FF00", false,
                        "0", "0");
            } else if (isPowerConnected()) {
                // Show led for charging (yellow)
                if (DEBUG) Log.d(TAG, "Setting charging led");
                setColor("FFFF00", false,
                        "0", "0");
            }
        }
    }

    private void disableLed() {
        if(enabledApps.size() > 0) enabledApps.clear();
        if(enabledStringForApps.size() > 0) enabledStringForApps.clear();
        if(enabledColorsForApps.size() > 0) enabledColorsForApps.clear();
        if(enabledBlinkForApps.size() > 0) enabledBlinkForApps.clear();
        if(enabledOnMsForApps.size() > 0) enabledOnMsForApps.clear();
        if(enabledOffMsForApps.size() > 0) enabledOffMsForApps.clear();

        if(currentEnabledApps.size() > 0) currentEnabledApps.clear();
        if(currentEnabledStringForApps.size() > 0) currentEnabledStringForApps.clear();
        if(currentEnabledColorsForApps.size() > 0) currentEnabledColorsForApps.clear();
        if(currentEnabledBlinkForApps.size() > 0) currentEnabledBlinkForApps.clear();
        if(currentEnabledOnMsForApps.size() > 0) currentEnabledOnMsForApps.clear();
        if(currentEnabledOffMsForApps.size() > 0) currentEnabledOffMsForApps.clear();

        turnOffLed();
    }

    private void turnOffLed() {
        setColor("000000", false, "0", "0");
    }
    private void loadConfig()  {
        if (DEBUG) Log.d(TAG, "loadConfig");
        String data = getDataFromSharedPref();
        if (data == null) return;
        if (DEBUG) Log.d(TAG, "data: " + data);
        Reader inputString = new StringReader(data);
        BufferedReader reader2 = new BufferedReader(inputString);

        if (data != null && !data.isEmpty()) {
            enabledApps.clear();
            enabledStringForApps.clear();
            enabledColorsForApps.clear();
            enabledBlinkForApps.clear();
            enabledOnMsForApps.clear();
            enabledOffMsForApps.clear();
            String line;
            try {
                line = reader2.readLine();
            } catch (IOException e) {
                e.printStackTrace();
                if (DEBUG) Log.d(TAG, "error reading configuration");
                return;
            }
            while(line != null && !line.isEmpty()){
                String delim = ",";
                if (DEBUG) Log.d(TAG, "line: " + line);
                try {
                    List<String> lineArray = new ArrayList<>(Arrays.asList(line.split(delim)));
                    if (DEBUG) Log.d(TAG, "number of elements: " + lineArray.size());
                    enabledApps.add(lineArray.get(0));
                    if (DEBUG) Log.d(TAG, "first element: " + lineArray.get(0));
                    enabledStringForApps.add(lineArray.get(1));
                    if (DEBUG) Log.d(TAG, "second element: " + lineArray.get(1));
                    enabledColorsForApps.add(lineArray.get(2));
                    if (DEBUG) Log.d(TAG, "third element: " + lineArray.get(2));
                    if (lineArray.size() > 3) {
                        if (DEBUG) Log.d(TAG, "fourth element: ");
                        if(lineArray.get(3).equals("blink")) {
                            enabledBlinkForApps.add(true);
                            enabledOnMsForApps.add(lineArray.get(4));
                            enabledOffMsForApps.add(lineArray.get(5));
                        } else {
                            enabledBlinkForApps.add(false);
                            enabledOnMsForApps.add("0");
                            enabledOffMsForApps.add("0");
                        }
                    } else {
                        enabledBlinkForApps.add(false);
                        enabledOnMsForApps.add("0");
                        enabledOffMsForApps.add("0");
                    }
                    if (DEBUG)
                        Log.d(TAG, "App: " + lineArray.get(0) + ", string: " + lineArray.get(1) + ", color: " + lineArray.get(2) + ", blink: " + enabledBlinkForApps.get(enabledBlinkForApps.size() -1));
                } catch (Exception e) {
                    Log.d(TAG, "Error adding lines from configuration file");
                    enabledApps.clear();
                    enabledStringForApps.clear();
                    enabledColorsForApps.clear();
                    enabledBlinkForApps.clear();
                    enabledOnMsForApps.clear();
                    enabledOffMsForApps.clear();
                }
                try {
                    line = reader2.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (DEBUG) Log.d(TAG, "Found: " + enabledApps.size() + " configurations");
        }
    }
    private void getNotificationAndSetLed(StatusBarNotification notification){
        loadConfig();
        String packageName = notification.getPackageName();
        if (DEBUG) Log.d(TAG, String.valueOf(enabledApps.size()));
        String title = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        /*String message = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();
        String message_lines = null;
        CharSequence[] lines = notification.getNotification().extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (!(lines == null) && !(lines.length == 0)) {
            for (CharSequence line : lines) {
                message_lines += line.toString();
            }
        }*/
        // Check if we have rules for this package
        if (enabledApps.contains(packageName)) {
            if (DEBUG) Log.d(TAG, "package found: " + packageName);
            // Add this to the current notifications
            currentEnabledApps.add(packageName);

            int[] occurencesArray = Utils.indexOfMultiple(enabledApps, packageName);
            if (DEBUG) Log.d(TAG, "Occurrences found: " + occurencesArray.length);
            boolean isFound = false;
            int defaultIndex = -1;
            for (int i = 0; i < occurencesArray.length ; i++) {
                // Check if we have strings for this package
                if (DEBUG) Log.d(TAG, "title: " + title);
                if (title != null) {
                    isFound = title.toLowerCase().contains(enabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }
                /*if (message != null) {
                    isFound = isFound || message.toLowerCase().contains(enabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }
                if (message_lines != null) {
                    isFound = isFound || message_lines.toLowerCase().contains(enabledStringForApps.get(occurencesArray[i]).toLowerCase());
                }*/
                if (isFound && !enabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    if (DEBUG) Log.d(TAG, "found a matching string: " + enabledColorsForApps.get(occurencesArray[i]));
                    setColor(enabledColorsForApps.get(occurencesArray[i]), enabledBlinkForApps.get(occurencesArray[i]),
                            enabledOnMsForApps.get(occurencesArray[i]),enabledOffMsForApps.get(occurencesArray[i]));
                    // Add these infos
                    currentEnabledColorsForApps.add(enabledColorsForApps.get(occurencesArray[i]));
                    currentEnabledStringForApps.add(enabledStringForApps.get(occurencesArray[i]));
                    currentEnabledBlinkForApps.add(enabledBlinkForApps.get(occurencesArray[i]));
                    currentEnabledOnMsForApps.add(enabledOnMsForApps.get(occurencesArray[i]));
                    currentEnabledOffMsForApps.add(enabledOffMsForApps.get(occurencesArray[i]));
                    break;
                } else if(enabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    if (DEBUG) {
                        Log.d(TAG, "found an empty string setting led");
                    }
                    defaultIndex = occurencesArray[i];
                }
                isFound = false;
            }
            if (defaultIndex != -1 && !isFound) {
                setColor(enabledColorsForApps.get(defaultIndex), enabledBlinkForApps.get(defaultIndex),
                        enabledOnMsForApps.get(defaultIndex),enabledOffMsForApps.get(defaultIndex));
                // Add these infos
                currentEnabledStringForApps.add("");
                currentEnabledColorsForApps.add(enabledColorsForApps.get(defaultIndex));
                currentEnabledBlinkForApps.add(enabledBlinkForApps.get(defaultIndex));
                currentEnabledOnMsForApps.add(enabledOnMsForApps.get(defaultIndex));
                currentEnabledOffMsForApps.add(enabledOffMsForApps.get(defaultIndex));
            }
        }
    }
    private void setColor (String hexColor, Boolean blink, String onMsStr, String offMsStr) {
        if (DEBUG) Log.d(TAG,"Setting color: " + hexColor);
        int color = Color.parseColor("#" + hexColor);
        int blinkInt = blink ? 1 : 0;
        onMs = Integer.valueOf(onMsStr);
        offMs = Integer.valueOf(offMsStr);
        Utils.setProp("sys.blink_light", String.valueOf(blinkInt));
        setRedLight(Color.red(color));
        setGreenLight(Color.green(color));
        setBlueLight(Color.blue(color));
        Utils.setProp("ctl.start", "light_daemon");
    }

    private void setBlueLight(int value){
            int mStartIdx = 2* RAMP_SIZE;
            String mDutyPcts = getScaledDutyPcts(value);
            int mPauseLo = offMs;
            int mRampStepMs = RAMP_STEP_DURATION;
            int mPauseHi = onMs - (mRampStepMs * RAMP_SIZE * 2);
            if (mRampStepMs * RAMP_SIZE * 2 > onMs) {
                mRampStepMs = onMs / (RAMP_SIZE * 2);
                mPauseHi = 0;
            }

            Utils.setProp("sys.blue_light", String.valueOf(value) + "-" + String.valueOf(mStartIdx)
                            + "-" + mDutyPcts + "-" + String.valueOf(mPauseLo) + "-" + String.valueOf(mPauseHi) +"-" + String.valueOf(mRampStepMs));
    }

    private void setRedLight(int value){
            int mStartIdx = 0;
            String mDutyPcts = getScaledDutyPcts(value);
            int mPauseLo = offMs;
            int mRampStepMs = RAMP_STEP_DURATION;
            int mPauseHi = onMs - (mRampStepMs * RAMP_SIZE * 2);
            if (mRampStepMs * RAMP_SIZE * 2 > onMs) {
                mRampStepMs = onMs / (RAMP_SIZE * 2);
                mPauseHi = 0;
            }
        Utils.setProp("sys.red_light", String.valueOf(value) + "-" + String.valueOf(mStartIdx)
                + "-" + mDutyPcts + "-" + String.valueOf(mPauseLo) + "-" + String.valueOf(mPauseHi) +"-" + String.valueOf(mRampStepMs));
    }

    private void setGreenLight(int value){
            int mStartIdx = RAMP_SIZE;
            String mDutyPcts = getScaledDutyPcts(value);
            int mPauseLo = offMs;
            int mRampStepMs = RAMP_STEP_DURATION;
            int mPauseHi = onMs - (mRampStepMs * RAMP_SIZE * 2);
            if (mRampStepMs * RAMP_SIZE * 2 > onMs) {
                mRampStepMs = onMs / (RAMP_SIZE * 2);
                mPauseHi = 0;
            }

            Utils.setProp("sys.green_light", String.valueOf(value) + "-" + String.valueOf(mStartIdx)
                    + "-" + mDutyPcts + "-" + String.valueOf(mPauseLo) + "-" + String.valueOf(mPauseHi) +"-" + String.valueOf(mRampStepMs));
    }

    String getScaledDutyPcts(int brightness) {
        String buf;
        buf = "";

        for (int i : BRIGHTNESS_RAMP) {
            buf += String.valueOf(i * brightness / 255);
            buf += ",";
        }
        return removeLastChars(buf, 1);
    }

    private static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }

    private void saveDataOnSharedPref() {
        if (DEBUG) Log.d(TAG, "saveDataOnSharedPref");
        if (configFileAlreadyLoaded) return;
        String data;
        try {
            data = getStringFromFile(configPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (DEBUG) Log.d(TAG, "data: " + data);
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        SharedPreferences.Editor edit=pref.edit();
        if (DEBUG) Log.d(TAG, "data encoded: " + Base64.getEncoder().encodeToString(data.getBytes()));
        edit.remove("ledSettingsBase64");
        edit.commit();
        edit.putString("ledSettingsBase64", Base64.getEncoder().encodeToString(data.getBytes()));
        edit.commit();
        configFileAlreadyLoaded = true;
    }

    private String getDataFromSharedPref() {
        saveDataOnSharedPref();
        if (DEBUG) Log.d(TAG, "getDataFromSharedPref");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        String prefData = pref.getString("ledSettingsBase64", null);
        if (prefData == null) return null;
        String data = new String(Base64.getDecoder().decode(prefData), StandardCharsets.UTF_8);
        return data;
    }

    private static String getStringFromFile (String filePath) throws IOException {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        Boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if(firstLine){
                sb.append(line);
                firstLine = false;
            } else {
                sb.append("\n").append(line);
            }
        }
        reader.close();
        return sb.toString();
    }

    private Boolean isBatteryFull() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mContext.registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        int batteryPct = level * 100 / scale;
        if (batteryPct == 100) return true;
        else return false;
    }

    public boolean isPowerConnected() {
        Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
    }

}