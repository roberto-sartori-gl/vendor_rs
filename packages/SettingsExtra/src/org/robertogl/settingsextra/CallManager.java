package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallManager {

    private final String TAG = "CallManagerService";

    private final boolean DEBUG = MainService.DEBUG;

    private MediaRecorder mCallRecorder;

    private String callNumber;

    protected boolean areWeRecordingACall = false;

    private boolean areWeVibrating = false;

    private Context mContext;

    protected boolean stopListening = false;

    protected boolean areWeAllowedToRecordACall = false;

    protected boolean areWeAllowedToVibrateDuringCalls = false;

    protected boolean isServiceRunning = false;

    protected void onStartup(Context context) {
        mContext = context;

        IntentFilter mRecordingFilter = new IntentFilter();
        mRecordingFilter.addAction("android.intent.action.PHONE_STATE");
        mContext.registerReceiver(CallRecorderReceiver, mRecordingFilter);
        IntentFilter OutGoingNumFilter = new IntentFilter();
        OutGoingNumFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        mContext.registerReceiver(OutGoingNumDetector, OutGoingNumFilter);
    }

    protected void onClose() {
        if (mContext == null) return;
        mContext.unregisterReceiver(CallRecorderReceiver);
        mContext.unregisterReceiver(OutGoingNumDetector);
    }

    BroadcastReceiver CallRecorderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                if (DEBUG) Log.d(TAG, "Call in progress");
                if (areWeAllowedToVibrateDuringCalls) {
                    if (DEBUG) Log.d(TAG, "CallVibration enabled");
                    vibrate(300);
                }
                if(!areWeAllowedToRecordACall) {
                    if (DEBUG) Log.d(TAG, "CallRecording disabled");
                    return;
                }
                startRecording();
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                if (DEBUG) Log.d(TAG, "Call finished");
                if (areWeAllowedToVibrateDuringCalls) {
                    vibrate(300);
                }
                stopRecording();
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                if (DEBUG) Log.d(TAG, "Call ringing");
                callNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }
        }
    };


    BroadcastReceiver OutGoingNumDetector = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!areWeAllowedToRecordACall) {
                if (DEBUG) Log.d(TAG, "CallRecording disabled");
                return;
            }
            if (DEBUG) Log.d(TAG, "Outgoing call starting");
            callNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }
    };

    private void vibrate (int length) {
        if (!areWeVibrating) {
            areWeVibrating = true;
            Utils.vibrate(length, mContext);
        }
        Handler handler = new Handler(Looper.myLooper());
        handler.postDelayed(() -> {
            areWeVibrating = false;
        }, length + 50);
    }

    public void startRecording() {
        if (!areWeRecordingACall) {
            Toast.makeText(mContext, "This call is being recorded", Toast.LENGTH_LONG).show();
            if (DEBUG) Log.d(TAG, "Starting MediaRecorder");
            mCallRecorder = new MediaRecorder();
            mCallRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mCallRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mCallRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

            String file = Environment.getExternalStorageDirectory().toString();
            String filepath = file + "/CallRecording";
            File dir = new File(filepath);
            dir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            filepath += "/" + currentDateandTime + "_" + callNumber + ".amr";
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
        if (areWeRecordingACall) {
            mCallRecorder.stop();
            mCallRecorder.reset();
            mCallRecorder.release();
            mCallRecorder = null;
            areWeRecordingACall = false;
        }

        if (stopListening || !isServiceRunning) {
            stopListening = false;
            onClose();
         }

    }

    protected void enableCallRecording(boolean status) {
        areWeAllowedToRecordACall = status;
        enableService();
    }

    protected void enableCallVibration(boolean status) {
        areWeAllowedToVibrateDuringCalls = status;
        enableService();
    }

    private void enableService() {
        if (areWeAllowedToRecordACall || areWeAllowedToVibrateDuringCalls) isServiceRunning = true;
        else isServiceRunning = false;
    }
}
