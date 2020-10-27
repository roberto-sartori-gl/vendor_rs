/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robertogl.keyhandler;

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
import android.app.admin.DevicePolicyManager;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class KeyHandler extends AccessibilityService {
    private static final String TAG = "KeyHandler";

    private static final boolean DEBUG = false;

    // Slider key codes
    private static final int MODE_NORMAL = 603;
    private static final int MODE_VIBRATION = 602;
    private static final int MODE_SILENCE = 601;

    // Vibration duration in ms
    private static final int msSilentVibrationLenght = 300;
    private static final int msVibrateVibrationLenght = 200;

    private boolean wasScreenOff = true;
    private long msPreviousEventMaxDistance = 1000;

    private long currentEventTime = 0;
    private long previousEventTime = 0;

    private Context mContext;
    private AudioManager mAudioManager;
    private Vibrator mVibrator;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (DEBUG) Log.d(TAG, "service is connected");
        mContext = this;
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mVibrator = mContext.getSystemService(Vibrator.class);
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

    private boolean isScreenOn() {
        DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }

    public boolean handleKeyEvent(KeyEvent event) {
	DevicePolicyManager manager = ((DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE));
        int scanCode = event.getScanCode();
        if (DEBUG) Log.d(TAG, "key event detected: " + scanCode);
	if (previousEventTime == 0) previousEventTime = System.currentTimeMillis() - msPreviousEventMaxDistance - 1;
	if (currentEventTime == 0) currentEventTime = System.currentTimeMillis();
        switch (scanCode) {
            case MODE_NORMAL:
		if (!isScreenOn()) {
			previousEventTime = System.currentTimeMillis();
			wasScreenOff = true;
		}
		currentEventTime = System.currentTimeMillis();
		if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.lockNow();
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_NORMAL) {
                        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                        mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
		}
		return true;
            case MODE_VIBRATION:
		if (!isScreenOn()) {
                        previousEventTime = System.currentTimeMillis();
                        wasScreenOff = true;
                }
		currentEventTime = System.currentTimeMillis();
                if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.lockNow();
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_VIBRATE) {
	                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
	                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
			doHapticFeedback(msVibrateVibrationLenght);
		}
                return true;
            case MODE_SILENCE:
		if (!isScreenOn()) {
                        previousEventTime = System.currentTimeMillis();
                        wasScreenOff = true;
                }
		currentEventTime = System.currentTimeMillis();
                if (currentEventTime - previousEventTime > msPreviousEventMaxDistance) wasScreenOff = false;
		if (wasScreenOff) {
			manager.lockNow();
			previousEventTime = System.currentTimeMillis();
		}
		if (mAudioManager.getRingerModeInternal() != AudioManager.RINGER_MODE_SILENT) {
	                mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
	                mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
			doHapticFeedback(msSilentVibrationLenght);
		}
                return true;
            default:
                return false;
        }
    }

    private void doHapticFeedback(int msVibrationLenght) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(VibrationEffect.createOneShot(msVibrationLenght,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
