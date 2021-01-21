package org.robertogl.settingsextra;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LedLightManager extends NotificationListenerService {
    private static final String TAG = "LedManagerExtra";

    private static final boolean DEBUG = MainService.DEBUG;

    private String configPath = "/sdcard/SettingsExtraLedConfiguration.conf";

    private boolean isServiceEnabled = false;

    private boolean shouldServiceBeEnabled = false;

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
    private final List<String> currentEnabledOnMsForApps = new ArrayList<>();
    private final List<String> currentEnabledOffMsForApps = new ArrayList<>();

    int offMs = 0;
    int onMs = 0;

    public LedLightManager() {
        super();
    }

    protected void onClose() {
        // Disable our Light daemon
        disableLed();
        Utils.setProp("persist.sys.disable.rgb", "");
        shouldServiceBeEnabled = false;
    }

    protected void onStartUnlocked() throws IOException {
        if (DEBUG) Log.d(TAG, "onStartUnlocked");
        onStart();
        // Parse configuration file if exist
        loadConfigFromFile();
        /*NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] mCurrentNotification = mNotificationManager.getActiveNotifications();
        for (StatusBarNotification mNotification : mCurrentNotification) {
            if (DEBUG) Log.d(TAG, "Found active notification");
            getNotificationAndSetLed(mNotification);
        }*/

    }

    protected void onStart(){
        if (DEBUG) Log.d(TAG, "onStart");
        // Disable the Android Light service
        Utils.setProp("persist.sys.disable.rgb", "1");
        shouldServiceBeEnabled = true;
        disableLed();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        if (!shouldServiceBeEnabled) {
            if(DEBUG) Log.d(TAG, "Service is disabled");
            //return;
        }
        try {
            if (!isServiceEnabled) {
                Log.d(TAG, "Enabling service from notification");
                onStartUnlocked();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!isServiceEnabled) {
            Log.d(TAG, "Service cannot be enabled");
            return;
        }
        if (notification != null && notification.getNotification() != null) {
            getNotificationAndSetLed(notification);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification notification) {
        if (DEBUG) Log.d(TAG, "onNotificationRemoved");
        String packageName = notification.getPackageName();
        if (DEBUG) Log.d(TAG, packageName);
        String title = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        if (currentEnabledApps.contains(packageName)) {
            if (DEBUG) Log.d(TAG, "package found: " + packageName);

            int[] occurencesArray = Utils.indexOfMultiple(currentEnabledApps, packageName);
            if (DEBUG) Log.d(TAG, "Occurrences found: " + occurencesArray.length);
            boolean isFound = false;
            int defaultIndex = -1;
            for (int i = 0; i < occurencesArray.length ; i++) {
                // Check if we have strings for this package
                if (currentEnabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    if (DEBUG) Log.d(TAG, "found an empty string");
                    defaultIndex = occurencesArray[i];
                }
                if (DEBUG) Log.d(TAG, "title: " + title);
                isFound = title.toLowerCase().contains(currentEnabledStringForApps.get(occurencesArray[i]).toLowerCase());
                if (isFound) {
                    currentEnabledApps.remove(occurencesArray[i]);
                    currentEnabledStringForApps.remove(occurencesArray[i]);
                    currentEnabledColorsForApps.remove(occurencesArray[i]);
                    currentEnabledOnMsForApps.remove(occurencesArray[i]);
                    currentEnabledOffMsForApps.remove(occurencesArray[i]);
                    break;
                }
            }
            if (defaultIndex != -1 && !isFound) {
                currentEnabledApps.remove(defaultIndex);
                currentEnabledStringForApps.remove(defaultIndex);
                currentEnabledColorsForApps.remove(defaultIndex);
                currentEnabledOnMsForApps.remove(defaultIndex);
                currentEnabledOffMsForApps.remove(defaultIndex);
            }
            // Set the color of the latest notification available
            if (currentEnabledColorsForApps.size() > 0) {
                setColor(currentEnabledColorsForApps.get(currentEnabledColorsForApps.size() - 1),
                        enabledBlinkForApps.get(currentEnabledColorsForApps.size() - 1),
                        enabledOnMsForApps.get(currentEnabledOnMsForApps.size() - 1),
                        enabledOffMsForApps.get(currentEnabledOffMsForApps.size() - 1));
            } else {
                disableLed();
            }
        }
    }

    private void disableLed() {
        Utils.setProp("sys.blink_light", "0");
        setBlueLight(0);
        setRedLight(0);
        setGreenLight(0);
        Utils.setProp("ctl.start", "light_daemon");
    }
    private void loadConfigFromFile() throws IOException {
        FileInputStream is;
        BufferedReader reader;
        final File file = new File(configPath);
        if (file.exists()) {
            enabledApps.clear();
            enabledStringForApps.clear();
            enabledColorsForApps.clear();
            enabledBlinkForApps.clear();
            enabledOnMsForApps.clear();
            enabledOffMsForApps.clear();
            is = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
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
                line = reader.readLine();
            }
            if (DEBUG) Log.d(TAG, "Found: " + enabledApps.size() + " configurations");
            isServiceEnabled = true;
        }
    }
    private void getNotificationAndSetLed(StatusBarNotification notification){
        String packageName = notification.getPackageName();
        if (DEBUG) Log.d(TAG, String.valueOf(enabledApps.size()));
        String title = notification.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        //String text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT).toString();

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
                if (enabledStringForApps.get(occurencesArray[i]).isEmpty()) {
                    if (DEBUG) {
                        Log.d(TAG, "found an empty string");
                    }
                    currentEnabledStringForApps.add("");
                    defaultIndex = occurencesArray[i];
                }
                if (DEBUG) Log.d(TAG, "title: " + title);
                isFound = title.toLowerCase().contains(enabledStringForApps.get(occurencesArray[i]).toLowerCase());
                if (isFound) {
                    if (DEBUG) Log.d(TAG, "found a matching string: " + enabledColorsForApps.get(occurencesArray[i]));
                    setColor(enabledColorsForApps.get(occurencesArray[i]), enabledBlinkForApps.get(occurencesArray[i]),
                            enabledOnMsForApps.get(occurencesArray[i]),enabledOffMsForApps.get(occurencesArray[i]));
                    // Add these infos
                    currentEnabledColorsForApps.add(enabledColorsForApps.get(occurencesArray[i]));
                    currentEnabledStringForApps.add(enabledStringForApps.get(occurencesArray[i]));
                    currentEnabledOnMsForApps.add(enabledOnMsForApps.get(occurencesArray[i]));
                    currentEnabledOffMsForApps.add(enabledOffMsForApps.get(occurencesArray[i]));
                    break;
                }
            }
            if (defaultIndex != -1 && !isFound) {
                setColor(enabledColorsForApps.get(defaultIndex), enabledBlinkForApps.get(occurencesArray[defaultIndex]),
                        enabledOnMsForApps.get(occurencesArray[defaultIndex]),enabledOffMsForApps.get(occurencesArray[defaultIndex]));
                // Add these infos
                currentEnabledColorsForApps.add(enabledColorsForApps.get(occurencesArray[defaultIndex]));
                currentEnabledOnMsForApps.add(enabledOnMsForApps.get(occurencesArray[defaultIndex]));
                currentEnabledOffMsForApps.add(enabledOffMsForApps.get(occurencesArray[defaultIndex]));
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

    public static String removeLastChars(String str, int chars) {
        return str.substring(0, str.length() - chars);
    }
}