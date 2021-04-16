package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaRecorder;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CallRecording {

    private final String TAG = "CallRecordingService";

    private final boolean DEBUG = MainService.DEBUG;

    private MediaRecorder mCallRecorder;

    private String callNumber;

    protected boolean areWeRecordingACall = false;

    private Context mContext;

    protected boolean stopListening = false;

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
                startRecording();
            }

            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state) && areWeRecordingACall == true) {
                if (DEBUG) Log.d(TAG, "Call finished");
                stopRecording();
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                if (DEBUG) Log.d(TAG, "Call ringing");
                callNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            }
        }

        ;
    };


    BroadcastReceiver OutGoingNumDetector = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {if (DEBUG) Log.d(TAG, "Outgoing call starting");
            callNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        }
    };

    public void startRecording() {
        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 1 ; i <= 2 ; i++) {
            try {
                TelephonyManager mTelephony = telephonyManager.createForSubscriptionId(1);
                int mNetworkType = mTelephony.getNetworkType(i);

                ImsManager mImsManager = new ImsManager(mContext);
                ImsMmTelManager mImsMmTelManager = mImsManager.getImsMmTelManager(i);

                boolean vowifiCall = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE, ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN) ||
                        mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO, ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);

                if (mNetworkType == TelephonyManager.NETWORK_TYPE_IWLAN && vowifiCall && telephonyManager.isImsRegistered()) {
                    Toast.makeText(mContext, "Cannot record VoWifi call", Toast.LENGTH_LONG).show();
                    if (DEBUG) Log.d(TAG, "Cannot record VoWifi call on sim + " + i + " due to network type: " + mNetworkType);
                    return;
                }

                boolean volteCall = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE, ImsRegistrationImplBase.REGISTRATION_TECH_LTE) ||
                        mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO, ImsRegistrationImplBase.REGISTRATION_TECH_LTE);

                if ((mNetworkType == TelephonyManager.NETWORK_TYPE_LTE || mNetworkType == TelephonyManager.NETWORK_TYPE_LTE_CA) && volteCall && telephonyManager.isImsRegistered()) {
                    Toast.makeText(mContext, "Cannot record VoLTE call", Toast.LENGTH_LONG).show();
                    if (DEBUG) Log.d(TAG, "Cannot record VoLTE call on sim + " + i + " due to network type: " + mNetworkType);;
                    return;
                }
            } catch (Exception e) {
                if (DEBUG)
                    Log.d(TAG, "Error getting the current network status, but we can try to record the call anyway");
            }
        }

        if (!areWeRecordingACall) {
            Toast.makeText(mContext, "This call is being recorded", Toast.LENGTH_LONG).show();
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
        if (areWeRecordingACall) {
            mCallRecorder.stop();
            mCallRecorder.reset();
            mCallRecorder.release();
            mCallRecorder = null;
            areWeRecordingACall = false;
        }

        if (stopListening) {
            onClose();
            stopListening = false;
        }
    }
}
