package org.robertogl.settingsextra;

import android.content.Context;
import android.media.AudioManager;
import android.os.Looper;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.util.Log;
import android.content.Intent;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;

import android.content.SharedPreferences;

import android.widget.Toast;

public class MainService extends AccessibilityService {
    private static final String TAG = "MainService";

    protected static final boolean DEBUG = false;

    // KeyCodes
    private static final int KEYCODE_APP_SELECT = 580;
    private static final int KEYCODE_BACK = 158;
    private static final int KEYCODE_F4 = 62;

    private static final int msDoubleClickThreshold = 250;
    private long msDoubleClick = 0;

    private static final String TriStatePath = "/sys/devices/virtual/switch/tri-state-key/state";

    private static int clickToShutdown = 0;

    private Context mContext;
    private AudioManager mAudioManager;

    private final CallRecording mCallRecording = new CallRecording();

    private final NfcStatusMonitor mNfcMonitor = new NfcStatusMonitor();

    private final AdaptiveBrightnessStatusMonitor mAutoBrightenessMonitor = new AdaptiveBrightnessStatusMonitor();

    private final BluetoothBatteryIcon mBluetoothBatteryIcon = new BluetoothBatteryIcon();

    private final ConnectivityManagerExtra mConnectivityManagerExtra = new ConnectivityManagerExtra();

    private final ImsMmTelManagerExtra mImsMmTelManagerExtra_1 = new ImsMmTelManagerExtra();
    private final ImsMmTelManagerExtra mImsMmTelManagerExtra_2 = new ImsMmTelManagerExtra();

    private final PocketModeService mPocketModeService = new PocketModeService();

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            switch (key) {
                case "areWeAllowedToRecordCall":
                    boolean areWeAllowedToRecordCall = prefs.getBoolean("areWeAllowedToRecordCall", false);
                    if (DEBUG)
                        Log.d(TAG, "Preference change received. areWeAllowedToRecordCall now is: " + areWeAllowedToRecordCall);
                    if (areWeAllowedToRecordCall) {
                        // Start the CallRecording service if the user enabled the service
                        mCallRecording.onStartup(mContext);
                    } else if (!mCallRecording.areWeRecordingACall) {
                        // Stop the CallRecording service now
                        mCallRecording.onClose();
                    } else {
                        mCallRecording.stopListening = true;
                        Toast.makeText(mContext, "Changes will be effective after the current call", Toast.LENGTH_LONG).show();
                    }
                    break;
                case "dynamicModem": {
                    boolean dynamicModem = prefs.getBoolean("dynamicModem", false);
                    if (dynamicModem) {
                        if (DEBUG) Log.d(TAG, "Starting connectivityManagerExtra service");
                        mConnectivityManagerExtra.onStartup(mContext);
                    } else
                        mConnectivityManagerExtra.onClose();
                    break;
                }
                case "preferred_network_mode_key_wifi":
                case "preferred_network_mode_key": {
                    // Settings changed for ConnectivityManagerExtra
                    if (DEBUG) Log.d(TAG, "Network settings for ConnectivityManagerExtra changed");
                    boolean dynamicModem = prefs.getBoolean("dynamicModem", false);
                    if (dynamicModem) {
                        if (DEBUG)
                            Log.d(TAG, "Updating network settings for ConnectivityManagerExtra");
                        mConnectivityManagerExtra.forceNetworkSettingsUpdate();
                    }
                    break;
                }
                case "showVolteIcon":
                case "showVowifiIcon": {
                    if (DEBUG) Log.d(TAG, "Settings for ImsMmTelManagerExtra changed");
                    boolean shouldWeShowVolteIcon = prefs.getBoolean("showVolteIcon", false);
                    boolean shouldWeShowVoWifiIcon = prefs.getBoolean("showVowifiIcon", false);
                    // Check if the services are running
                    boolean isImsMmTelManagerExtraRunning_1 = mImsMmTelManagerExtra_1.isRunning;
                    boolean isImsMmTelManagerExtraRunning_2 = mImsMmTelManagerExtra_2.isRunning;
                    // If volte or vowifi icons should be shown but the services are not running, start them
                    if (shouldWeShowVolteIcon || shouldWeShowVoWifiIcon) {
                        if (!isImsMmTelManagerExtraRunning_1) {
                            if (DEBUG) Log.d(TAG, "Starting the mImsMmTelManagerExtra_1 service");
                            mImsMmTelManagerExtra_1.onStartup(mContext, 1);
                        } else {
                            mImsMmTelManagerExtra_1.notifyUserSettingChange();
                        }
                        if (!isImsMmTelManagerExtraRunning_2) {
                            if (DEBUG) Log.d(TAG, "Starting the mImsMmTelManagerExtra_2 service");
                            mImsMmTelManagerExtra_2.onStartup(mContext, 2);
                        } else {
                            mImsMmTelManagerExtra_2.notifyUserSettingChange();
                        }

                    } else {
                        if (DEBUG) Log.d(TAG, "Closing the mImsMmTelManagerExtra services");
                        if (isImsMmTelManagerExtraRunning_1) mImsMmTelManagerExtra_1.onClose();
                        if (isImsMmTelManagerExtraRunning_2) mImsMmTelManagerExtra_2.onClose();
                    }
                }
                case "pocketModeEnabled":
                    if (DEBUG) Log.d(TAG, "Settings for PocketMode changed");
                    boolean isPocketModeEnabled = prefs.getBoolean("pocketModeEnabled", false);
                    if (isPocketModeEnabled) mPocketModeService.ProximitySensor(mContext);
                case "headsUpNotificationsEnabled":
                    if (DEBUG) Log.d(TAG, "Settings for Heads Up notifications changed");
                    boolean areHeadsUpEnabled = prefs.getBoolean("headsUpNotificationsEnabled", true);
                    if (areHeadsUpEnabled) Utils.setHeadsUpNotification("1", mContext);
                    else Utils.setHeadsUpNotification("0", mContext);
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenStateReceiver);
        mBluetoothBatteryIcon.onClose();
        mCallRecording.onClose();
        mNfcMonitor.onClose();
        mAutoBrightenessMonitor.onClose();
        mConnectivityManagerExtra.onClose();
        mImsMmTelManagerExtra_1.onClose();
        mImsMmTelManagerExtra_2.onClose();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (DEBUG) Log.d(TAG, "service is connected");
        mContext = this;
        mAudioManager = mContext.getSystemService(AudioManager.class);

        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);

        pref.registerOnSharedPreferenceChangeListener(mPreferenceListener);

        // Set the status at boot following the slider position
        // Do this in case the user changes the slider position while the phone is off, for example
        // Also, we solve an issue regarding the STREAM_MUSIC that was never mute at boot
        int tristate = Integer.parseInt(Utils.readFromFile(TriStatePath));
        if (DEBUG) Log.d(TAG, "Tri Key state: " + tristate);
        if (tristate == 1) {
            // Silent mode
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
        } else if (tristate == 2) {
            // Vibration mode
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
        } else if (tristate == 3) {
            // Normal mode
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
        }

        // Register here to get the SCREEN_OFF event
        // Used to turn off the capacitive buttons backlight
        IntentFilter screenActionFilter = new IntentFilter();
        screenActionFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenActionFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenStateReceiver, screenActionFilter);

        // Enable the Dynamic Modem if the user enabled it
        boolean dynamicModem = pref.getBoolean("dynamicModem", false);
        if (dynamicModem) {
            if (DEBUG) Log.d(TAG, "Starting Dynamic Modem");
            // At the moment, ConnectivityManagerExtra only manages the Dynamic Modem
            mConnectivityManagerExtra.onStartup(this);
        }

        // Start the NFC tile monitoring service
        mNfcMonitor.onStartup(this);

        // Start the auto brightness tile monitoring service
        mAutoBrightenessMonitor.onStartup(this);

        // Start the Bluetooth battery icon on the status bar monitoring service
        mBluetoothBatteryIcon.onStartup(this);

        // Start the CallRecording service if the user enabled the service
        boolean areWeAllowedToRecordCall = pref.getBoolean("areWeAllowedToRecordCall", false);
        if (areWeAllowedToRecordCall) {
            if (DEBUG) Log.d(TAG, "Starting the CallRecording service");
            mCallRecording.onStartup(this);
        }

        // Start the ImsMmTelManagerExtra service if the user wants VoLTE or VoWiFi icon on status bar
        boolean shouldWeShowImsIcons = pref.getBoolean("showVolteIcon", false);
        shouldWeShowImsIcons = shouldWeShowImsIcons || pref.getBoolean("showVowifiIcon", false);
        if (shouldWeShowImsIcons) {
            if (DEBUG) Log.d(TAG, "Starting the ImsMmTelManagerExtra service");
            mImsMmTelManagerExtra_1.onStartup(this, 1);
            mImsMmTelManagerExtra_2.onStartup(this, 2);
        }

        // Setup the PocketMode service if the user wants it
        boolean isPocketModeEnabled = pref.getBoolean("pocketModeEnabled", false);
        if (isPocketModeEnabled) {
            mPocketModeService.ProximitySensor(mContext);
        }

        // Check if Heads Up notification should be enabled
        boolean areHeadsUpEnabled = pref.getBoolean("headsUpNotificationsEnabled", true);
        if (areHeadsUpEnabled) Utils.setHeadsUpNotification("1", mContext);
        else Utils.setHeadsUpNotification("0", mContext);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return handleKeyEvent(event);
    }

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
            SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
            boolean isPocketModeEnabled = pref.getBoolean("pocketModeEnabled", false);
            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_OFF:
                    if (DEBUG) Log.d(TAG, "Screen OFF");
                    // Set the variable for the slider keys
                    clickToShutdown = 0;
                    if (DEBUG)
                        Log.d(TAG, "Always On Display is: " + Utils.isAlwaysOnDisplayEnabled(mContext));
                    if (Utils.isAlwaysOnDisplayEnabled(mContext))
                        Utils.writeToFile(Utils.dozeWakeupNode, "1", mContext);
                    Utils.setProp("sys.button_backlight.on", "false");
                    if (isPocketModeEnabled) mPocketModeService.enable();
                    break;
                case Intent.ACTION_SCREEN_ON:
                    if (DEBUG) Log.d(TAG, "Screen ON");
                    if (Utils.isAlwaysOnDisplayEnabled(mContext))
                        Utils.writeToFile(Utils.dozeWakeupNode, "0", mContext);
                    if (isPocketModeEnabled) mPocketModeService.disable();
                    break;
            }
        }
    };

    private boolean handleKeyEvent(KeyEvent event) {
        PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        int scanCode = event.getScanCode();
        if (DEBUG) Log.d(TAG, "key event detected: " + scanCode);
        switch (scanCode) {
            case KEYCODE_BACK:
            case KEYCODE_APP_SELECT:
                if (event.getAction() == 0) {
                    clickToShutdown += 1;
                    Utils.setProp("sys.button_backlight.on", "true");
                } else {
                    Handler handler = new Handler(Looper.myLooper());
                    handler.postDelayed(() -> {
                        clickToShutdown -= 1;
                        if (clickToShutdown <= 0) {
                            clickToShutdown = 0;
                            Utils.setProp("sys.button_backlight.on", "false");
                        }
                    }, 1500);
                }
                return false;
            case KEYCODE_F4:
                if (DEBUG) Log.d(TAG, "F4 detected");
                if (Integer.parseInt(Utils.readFromFile(Utils.dozeWakeupNode)) == 0) {
                    if (DEBUG) Log.d(TAG, "F4 ignored (not enabled)");
                    return false;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    if (DEBUG) Log.d(TAG, "F4 UP detected");
                    if (doubleClick()) {
                        PowerManager.WakeLock wakeLock;
                        wakeLock = manager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.ON_AFTER_RELEASE, "SettingsExtra: WakeLock");
                        wakeLock.acquire(5 * 1000L /*5 seconds*/);
                        wakeLock.release();
                        return true;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private boolean doubleClick() {
        boolean result = false;
        long thisTime = System.currentTimeMillis();

        if ((thisTime - msDoubleClick) < msDoubleClickThreshold) {
            if (DEBUG) Log.d(TAG, "doubleClick");
            result = true;
        } else {
            msDoubleClick = thisTime;
        }
        return result;
    }
}
