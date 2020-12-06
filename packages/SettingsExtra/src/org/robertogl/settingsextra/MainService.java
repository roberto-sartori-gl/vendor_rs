/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robertogl.settingsextra;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.util.Log;
import android.content.Intent;
import android.os.IBinder;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.*;
import java.text.SimpleDateFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.nfc.NfcAdapter;
import android.content.ComponentName;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;

import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;

import android.telephony.TelephonyManager;
import android.media.MediaRecorder;

import android.os.Environment;
import android.widget.Toast;

public class MainService extends AccessibilityService {
    private static final String TAG = "MainService";

    private static final boolean DEBUG = false;

    // Slider key codes
    private static final int MODE_NORMAL = 603;
    private static final int MODE_VIBRATION = 602;
    private static final int MODE_SILENCE = 601;
    private static final int KEYCODE_APP_SELECT = 580;
    private static final int KEYCODE_BACK = 158;
    private static final int KEYCODE_F4 = 62;

    // Vibration duration in ms
    private static final int msSilentVibrationLenght = 300;
    private static final int msVibrateVibrationLenght = 200;

    private static final int msDoubleClickThreshold = 250;
    private long msDoubleClick = 0;

    private static final String TriStatePath = "/sys/devices/virtual/switch/tri-state-key/state";

    private boolean wasScreenOff = true;
    private long msPreviousEventMaxDistance = 1000;

    private long currentEventTime = 0;
    private long previousEventTime = 0;

    private static int clickToShutdown = 0;

    private long doubleClickEventTime = 0;

    private boolean isBluetoothOff = true;

    private List<String> mBluetoothBatteryConnectedAddress = new ArrayList<String>();

    private int bluetoothDeviceConnected = 0;

    private String callNumber;

    private boolean areWeRecordingACall = false;

    private Context mContext;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;
    private StatusBarManager mStatusBarManager;
    private MediaRecorder mCallRecorder;

    private void updateBatteryLevel(int batteryLevel) {
	Utils.removeUnwantendStatusBarIcon(this, "rotate");
	Utils.removeUnwantendStatusBarIcon(this, "bluetooth");
	if (DEBUG) Log.d(TAG, "bluetooth: updating icon on status bar with battery level: " + batteryLevel);
	int iconId = 0;
	mStatusBarManager = (StatusBarManager)getSystemService(STATUS_BAR_SERVICE);
	if (batteryLevel == -1 || batteryLevel == -100) {
	     iconId = R.drawable.stat_sys_data_bluetooth_connected;
	} else if (batteryLevel == 100) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_9;
        } else if (batteryLevel >= 90) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_8;
        } else if (batteryLevel >= 80) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_7;
        } else if (batteryLevel >= 70) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_6;
        } else if (batteryLevel >= 60) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_5;
        } else if (batteryLevel >= 50) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_4;
        } else if (batteryLevel >= 40) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_3;
        } else if (batteryLevel >= 30) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_2;
        } else if (batteryLevel >= 20) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_1;
        } else if (batteryLevel >= 10) {
             iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_0;
        }
	mStatusBarManager.setIcon("bluetooth_extra", iconId, 0, null);
	// Actually show the icon
	mStatusBarManager.setIconVisibility("bluetooth_extra", true);
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();
	    mStatusBarManager = (StatusBarManager)getSystemService(STATUS_BAR_SERVICE);
	    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
		if (DEBUG) Log.d(TAG, "bluetooth in ACTION_STATE_CHANGED");
		final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
		switch(state) {
		    case BluetoothAdapter.STATE_OFF:
		    case BluetoothAdapter.STATE_DISCONNECTED:
			if (DEBUG) Log.d(TAG, "bluetooth in STATE_OFF/DISCONNECTED");
			mBluetoothBatteryConnectedAddress.clear();
			mStatusBarManager.setIconVisibility("bluetooth_extra", false);
			mStatusBarManager.removeIcon("bluetooth_extra");
			isBluetoothOff = true;
			bluetoothDeviceConnected = 0;
			break;
	        }
	    } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
		if (DEBUG) Log.d(TAG, "bluetooth in ACTION_ACL_DISCONNECTED");
		BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		if (mBluetoothBatteryConnectedAddress.contains(device.getAddress())) mBluetoothBatteryConnectedAddress.remove(device.getAddress());
		bluetoothDeviceConnected -= 1;
		if (bluetoothDeviceConnected == 0) {
			if (DEBUG) Log.d(TAG, "bluetooth devices all disconnected");
			mBluetoothBatteryConnectedAddress.clear();
			mStatusBarManager.setIconVisibility("bluetooth_extra", false);
			mStatusBarManager.removeIcon("bluetooth_extra");
			isBluetoothOff = true;
		} else if (mBluetoothBatteryConnectedAddress.size() > 0) {
			if (DEBUG) Log.d(TAG, "bluetooth devices with battery are still connected");
			isBluetoothOff = false;
		} else if (bluetoothDeviceConnected > 0) {
			if (DEBUG) Log.d(TAG, "bluetooth devices without battery are still connected");
			mBluetoothBatteryConnectedAddress.clear();
			updateBatteryLevel(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
			isBluetoothOff = true;
		}
	    } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
		if (DEBUG) Log.d(TAG, "bluetooth in ACTION_ACL_CONNECTED");
		if (!(mBluetoothBatteryConnectedAddress.size() > 0)) updateBatteryLevel(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
		bluetoothDeviceConnected += 1;
		isBluetoothOff = false;
	    } else if (action.equals(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED)) {
		int batteryLevel = intent.getIntExtra(BluetoothDevice.EXTRA_BATTERY_LEVEL, BluetoothDevice.BATTERY_LEVEL_BLUETOOTH_OFF);
		if (!isBluetoothOff) updateBatteryLevel(batteryLevel);
		if (batteryLevel >= 0) {
		        if (DEBUG) Log.d(TAG, "bluetooth device has battery");
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			if (!mBluetoothBatteryConnectedAddress.contains(device.getAddress())) mBluetoothBatteryConnectedAddress.add(device.getAddress());
		}
		if (DEBUG) Log.d(TAG, "bluetooth battery change detected: " + batteryLevel);
	    }
	}
    };

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
	@Override
	public void onReceive(Context context, Intent intent) {
	    switch (intent.getAction()) {
		case Intent.ACTION_SCREEN_OFF:
		    if (DEBUG) Log.d(TAG, "Screen OFF");
		    clickToShutdown = 0;
		    if (DEBUG) Log.d(TAG, "Always On Display is: " + Utils.isAlwaysOnDisplayEnabled(mContext));
		    if (Utils.isAlwaysOnDisplayEnabled(mContext)) Utils.writeToFile(Utils.dozeWakeupNode, "1", mContext);
		    Utils.setProp("sys.button_backlight.on", "false");
		    break;
		case Intent.ACTION_SCREEN_ON:
		    if (DEBUG) Log.d(TAG, "Screen ON");
		    if (Utils.isAlwaysOnDisplayEnabled(mContext)) Utils.writeToFile(Utils.dozeWakeupNode, "0", mContext);
		    break;
	    }
	}
    };

    private final BroadcastReceiver NfcReceiver = new BroadcastReceiver() {
        @Override
         public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF);
                switch (state) {
                    case NfcAdapter.STATE_OFF:
                        NfcTile.requestListeningState(getApplicationContext(), new ComponentName(getApplicationContext(), NfcTile.class));
                        break;
                    case NfcAdapter.STATE_ON:
                        NfcTile.requestListeningState(getApplicationContext(), new ComponentName(getApplicationContext(), NfcTile.class));
                        break;
                }
            }
        }
    };

    BroadcastReceiver CallRecorderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
            SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
            if (!pref.getBoolean("areWeAllowedToRecordCall", false) && !areWeRecordingACall) return;

            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state))
            {
                if (DEBUG) Log.d(TAG, "Call in progress");
                startRecording();
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state) && areWeRecordingACall == true)
            {
                if (DEBUG) Log.d(TAG, "Call finished");
                stopRecording();
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state))
            {
                if (DEBUG) Log.d(TAG, "Call ringing");
                callNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }
        };
    };

    BroadcastReceiver OutGoingNumDetector = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
	    Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
	    SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
	    if (!pref.getBoolean("areWeAllowedToRecordCall", false) && !areWeRecordingACall) return;

	    if (DEBUG) Log.d(TAG, "Outgoing call starting");
	    callNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }
    };

    @Override
    protected void onServiceConnected() {
	super.onServiceConnected();
	if (DEBUG) Log.d(TAG, "service is connected");
	mContext = this;
	mAudioManager = mContext.getSystemService(AudioManager.class);
	mVibrator = mContext.getSystemService(Vibrator.class);

	// Register here to get the SCREEN_OFF event
	// Used to turn off the capacitive buttons backlight
	IntentFilter screenActionFilter = new IntentFilter();
	screenActionFilter.addAction(Intent.ACTION_SCREEN_OFF);
	screenActionFilter.addAction(Intent.ACTION_SCREEN_ON);
	registerReceiver(mScreenStateReceiver, screenActionFilter);

	// Set the status at boot following the slider position
	// Do this in case the user changes the slider position while the phone is off, for example
	// Also, we solve an issue regarding the STREAM_MUSIC that was never mute at boot
	int tristate = Integer.parseInt(Utils.readFromFile(TriStatePath));
	if (DEBUG) Log.d(TAG, "Tri Key state: " + tristate);
	if (tristate == 1) {
		// Silent mode
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
		mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
	}
	else if (tristate == 2) {
		// Vibration mode
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
		mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
	}
	else if (tristate == 3) {
		// Normal mode
		mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
		mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
	}

	// Listen for NFC events (ON/OFF)
	IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
	this.registerReceiver(NfcReceiver, filter);

	// Listen for Bluetooth events (ON/OFF/CONNECTED/DISCONNECTED/bluetooth battery level changes)
	IntentFilter mBluetoothFilter = new IntentFilter();
	mBluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
	mBluetoothFilter.addAction(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED);
	mBluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
	mBluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
	this.registerReceiver(mBluetoothReceiver, mBluetoothFilter);

	// Listen for incoming or outgoing call
	IntentFilter mRecordingFilter = new IntentFilter();
	mRecordingFilter.addAction("android.intent.action.PHONE_STATE");
	this.registerReceiver(CallRecorderReceiver, mRecordingFilter);
	IntentFilter OutGoingNumFilter = new IntentFilter();
	OutGoingNumFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
	this.registerReceiver(OutGoingNumDetector, OutGoingNumFilter);
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

    public boolean handleKeyEvent(KeyEvent event) {
	PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	int scanCode = event.getScanCode();
	if (DEBUG) Log.d(TAG, "key event detected: " + scanCode);
	if (previousEventTime == 0) previousEventTime = System.currentTimeMillis() - msPreviousEventMaxDistance - 1;
	if (currentEventTime == 0) currentEventTime = System.currentTimeMillis();
	switch (scanCode) {
	    case MODE_NORMAL:
		if (!Utils.isScreenOn(mContext)) {
			previousEventTime = System.currentTimeMillis();
			wasScreenOff = true;
		}
		currentEventTime = System.currentTimeMillis();
		if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.goToSleep(SystemClock.uptimeMillis());
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_NORMAL) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
		}
		return true;
	    case MODE_VIBRATION:
		if (!Utils.isScreenOn(mContext)) {
			previousEventTime = System.currentTimeMillis();
			wasScreenOff = true;
		}
		currentEventTime = System.currentTimeMillis();
		if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.goToSleep(SystemClock.uptimeMillis());
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_VIBRATE) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
			mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
			Utils.doHapticFeedback(mVibrator,msVibrateVibrationLenght);
		}
		return true;
	    case MODE_SILENCE:
		if (!Utils.isScreenOn(mContext)) {
			previousEventTime = System.currentTimeMillis();
			wasScreenOff = true;
		}
		currentEventTime = System.currentTimeMillis();
		if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.goToSleep(SystemClock.uptimeMillis());
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_SILENT) {
			mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
			mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
			Utils.doHapticFeedback(mVibrator, msSilentVibrationLenght);
		}
		return true;
	    case KEYCODE_BACK:
	    case KEYCODE_APP_SELECT:
		if (event.getAction() == 0) {
			clickToShutdown += 1;
			Utils.setProp("sys.button_backlight.on", "true");
		}
		else {
		    Handler handler = new Handler();
		    handler.postDelayed(new Runnable() {
			public void run() {
				clickToShutdown -= 1;
				if (clickToShutdown <= 0) {
					clickToShutdown = 0;
					Utils.setProp("sys.button_backlight.on", "false");
				}
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
					PowerManager.ON_AFTER_RELEASE, "WakeLock");
				wakeLock.acquire();
				wakeLock.release();
				wakeLock = null;
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
        }
        else {
            msDoubleClick = thisTime;
        }
        return result;
    }

    public void startRecording() {
        if(!areWeRecordingACall) {
            Toast.makeText(this, "This call is being recorded", Toast.LENGTH_LONG).show();
            if (DEBUG) Log.d(TAG, "Starting MediaRecorder");
            mCallRecorder = new MediaRecorder();
            mCallRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mCallRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mCallRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            String file = Environment.getExternalStorageDirectory().toString();
            String filepath = file + "/CallRecording";
            File dir = new File(filepath);
            dir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            filepath += "/" + currentDateandTime + "_" + callNumber + ".3gp";
            mCallRecorder.setOutputFile(filepath);

            try {
                mCallRecorder.prepare();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCallRecorder.start();
            areWeRecordingACall = true;
        }
    }

    public void stopRecording() {
        if (areWeRecordingACall)
        {
            mCallRecorder.stop();
            mCallRecorder.reset();
            mCallRecorder.release();
            mCallRecorder = null;
            areWeRecordingACall = false;
        }
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	unregisterReceiver(mScreenStateReceiver);
	unregisterReceiver(NfcReceiver);
	unregisterReceiver(mBluetoothReceiver);
	unregisterReceiver(CallRecorderReceiver);
	unregisterReceiver(OutGoingNumDetector);
    }

}
