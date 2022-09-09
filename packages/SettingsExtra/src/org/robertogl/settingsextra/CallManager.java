package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.PreciseCallState;
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

    private boolean isAcallInProgress = false;

    protected void onStartup(Context context) {
        mContext = context;

        IntentFilter mRecordingFilter = new IntentFilter();
        mRecordingFilter.addAction("android.intent.action.PHONE_STATE");
        mContext.registerReceiver(CallRecorderReceiver, mRecordingFilter);
        IntentFilter OutGoingNumFilter = new IntentFilter();
        OutGoingNumFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
        mContext.registerReceiver(OutGoingNumDetector, OutGoingNumFilter);

        // Create listeners for sim1 and sim2 states
        TelephonyManager mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        TelephonyManager mTelephonySim1 = mTelephonyManager.createForSubscriptionId(1);
        mTelephonySim1.listen(mPhoneStateListenerSim1, PhoneStateListener.LISTEN_PRECISE_CALL_STATE);

        TelephonyManager mTelephonySim2 = mTelephonyManager.createForSubscriptionId(2);
        mTelephonySim2.listen(mPhoneStateListenerSim2, PhoneStateListener.LISTEN_PRECISE_CALL_STATE);

    }

    /*
    public static final int PRECISE_CALL_STATE_IDLE =           0; //Call idle
    public static final int PRECISE_CALL_STATE_ACTIVE =         1; //Calling (active)
    public static final int PRECISE_CALL_STATE_HOLDING =        2; //The call is suspended (for example, I am talking to multiple people, one of the calls is active, and the other calls will enter the suspended state)
    public static final int PRECISE_CALL_STATE_DIALING =        3; //Dial start
    public static final int PRECISE_CALL_STATE_ALERTING =       4; //Calling out (remind the other party to answer the call)
    public static final int PRECISE_CALL_STATE_INCOMING =       5; //Call from the other party
    public static final int PRECISE_CALL_STATE_WAITING =        6; //Third-party call waiting (for example, I am talking to someone, and when other people call in, it will enter the waiting state)
    public static final int PRECISE_CALL_STATE_DISCONNECTED =   7; //Hang up completed
    public static final int PRECISE_CALL_STATE_DISCONNECTING =  8; //Is hanging up
    */

    private final PhoneStateListener mPhoneStateListenerSim1 = new PhoneStateListener() {
        @Override
        public void onPreciseCallStateChanged(PreciseCallState preciseState) {
            if (preciseState.getForegroundCallState() == PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                if (DEBUG) Log.d(TAG, "Call in progress on SIM 1");
                manageCallStarting();
            }

            if (preciseState.getForegroundCallState() == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                if (DEBUG) Log.d(TAG, "SIM 1 is idle");
                manageCallEnding();
            }
        }
    };

    private final PhoneStateListener mPhoneStateListenerSim2 = new PhoneStateListener() {
        @Override
        public void onPreciseCallStateChanged(PreciseCallState preciseState) {
            if (preciseState.getForegroundCallState() == PreciseCallState.PRECISE_CALL_STATE_ACTIVE) {
                if (DEBUG) Log.d(TAG, "Call in progress on SIM 2");
                manageCallStarting();
            }

            if (preciseState.getForegroundCallState() == PreciseCallState.PRECISE_CALL_STATE_IDLE) {
                if (DEBUG) Log.d(TAG, "SIM 2 is idle");
                manageCallEnding();
            }
        }
    };

    private void manageCallStarting(){
        isAcallInProgress = true;
        if (areWeAllowedToVibrateDuringCalls) {
            if (DEBUG) Log.d(TAG, "CallVibration enabled");
            vibrate(300);
        }
        if(!areWeAllowedToRecordACall) {
            if (DEBUG) Log.d(TAG, "CallRecording disabled");
            return;
        }
        if (!areWeRecordingACall) startRecording();
    }

    private void manageCallEnding(){
        if (!isAcallInProgress) return;
        isAcallInProgress = false;

        if (DEBUG) Log.d(TAG, "Call finished");
        if (areWeAllowedToVibrateDuringCalls) {
            vibrate(300);
        }
        stopRecording();
    }

    protected void onClose() {
        if (mContext == null) return;
        mContext.unregisterReceiver(CallRecorderReceiver);
        mContext.unregisterReceiver(OutGoingNumDetector);
    }

    BroadcastReceiver CallRecorderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) { String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                manageCallEnding();
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
            if (DEBUG) Log.d(TAG, "Starting MediaRecorder");
            mCallRecorder = new MediaRecorder();
            mCallRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            mCallRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
            mCallRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);

            String file = Environment.getExternalStorageDirectory().toString();

            Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
            SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", Context.MODE_PRIVATE);

            String callRecordingDirectory = pref.getString("callRecordingDirectory", "CallRecording");
            if (callRecordingDirectory.startsWith("/sdcard")) {
                callRecordingDirectory = callRecordingDirectory.substring(8);
            }
            if (callRecordingDirectory.isEmpty()) callRecordingDirectory = "CallRecording";
            String filepath = file + "/" + callRecordingDirectory;
            filepath = filepath.replaceAll("\\\\", "/");
            File dir = new File(filepath);
            dir.mkdirs();

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());

            filepath += "/" + currentDateandTime + "_" + callNumber + ".amr";
            mCallRecorder.setOutputFile(filepath);

            try {
                mCallRecorder.prepare();
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
                Toast.makeText(mContext, "Cannot use selected directory: call will not be recorded", Toast.LENGTH_LONG).show();
                mCallRecorder.reset();
                mCallRecorder.release();
                return;
            }

            Toast.makeText(mContext, "This call is being recorded", Toast.LENGTH_LONG).show();
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
