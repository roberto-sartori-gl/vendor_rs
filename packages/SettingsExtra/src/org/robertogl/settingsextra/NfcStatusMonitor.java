package org.robertogl.settingsextra;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.util.Log;

public class NfcStatusMonitor {

    private final static String TAG = "NfcStatusMonitorTile";

    private final static boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private final BroadcastReceiver NfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF);
                switch (state) {
                    case NfcAdapter.STATE_OFF:
                    case NfcAdapter.STATE_ON:
                        NfcTile.requestListeningState(context, new ComponentName(context, NfcTile.class));
                        break;
                }
            }
        }
    };

    protected void onStartup(Context context) {
        if (DEBUG) Log.d(TAG, "Starting");
        mContext = context;
        // Listen for NFC events (ON/OFF)
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        mContext.registerReceiver(NfcReceiver, filter);
    }

    protected void onClose() {
        mContext.unregisterReceiver(NfcReceiver);
    }

}


