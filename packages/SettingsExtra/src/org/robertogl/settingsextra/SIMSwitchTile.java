package org.robertogl.settingsextra;

import android.content.Context;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class SIMSwitchTile extends TileService {

    final private static String TAG = "SIMSwitchTile";

    final private static boolean DEBUG = MainService.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
    }

    private void updateTile() {
        Context mContext = getApplicationContext();
        Tile qsTile = getQsTile();
        qsTile.setState(Tile.STATE_ACTIVE);

	SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        int sim_number = subscriptionManager.getDefaultDataPhoneId() + 1;
        if (sim_number == 1 || sim_number == 2) {
            qsTile.setLabel("SIM" + sim_number + " enabled");
        } else {
            qsTile.setLabel("No SIM available");
        }
        qsTile.setSubtitle("Switch SIM for data usage");

        if (DEBUG) Log.d(TAG, "sim1: "  + (Settings.Global.getInt(mContext.getContentResolver(), "mobile_data1", 0) == 1));
        if (DEBUG) Log.d(TAG, "sim2: "  + (Settings.Global.getInt(mContext.getContentResolver(), "mobile_data2", 0) == 1));

        boolean data_enabled = Settings.Global.getInt(mContext.getContentResolver(), "mobile_data1", 0) == 1 ||
                Settings.Global.getInt(mContext.getContentResolver(), "mobile_data2", 0) == 1;

        if (data_enabled)
            qsTile.setState(Tile.STATE_ACTIVE);
        else
            qsTile.setState(Tile.STATE_INACTIVE);

        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Context mContext = getApplicationContext();
        SubscriptionManager subscriptionManager = getSystemService(SubscriptionManager.class);
        TelephonyManager mTelephonyCreator = getSystemService(TelephonyManager.class);
        TelephonyManager mTelephonyManager1 = mTelephonyCreator.createForSubscriptionId(1);
        TelephonyManager mTelephonyManager2 = mTelephonyCreator.createForSubscriptionId(2);

        if (DEBUG) Log.d(TAG, "sim1: "  + (Settings.Global.getInt(mContext.getContentResolver(), "mobile_data1", 0) == 1));
        if (DEBUG) Log.d(TAG, "sim2: "  + (Settings.Global.getInt(mContext.getContentResolver(), "mobile_data2", 0) == 1));

        boolean data_enabled = Settings.Global.getInt(mContext.getContentResolver(), "mobile_data1", 0) == 1 ||
                Settings.Global.getInt(mContext.getContentResolver(), "mobile_data2", 0) == 1;

        if (subscriptionManager.getDefaultDataPhoneId() == 0) {
            if (DEBUG) Log.d(TAG, "current subid: " + subscriptionManager.getDefaultDataPhoneId());

            if (data_enabled) {
                mTelephonyManager1.setDataEnabled(false);
                mTelephonyManager2.setDataEnabled(true);
            }
            subscriptionManager.setDefaultDataSubId(2);

            if (DEBUG) Log.d(TAG, "new subid: " + subscriptionManager.getDefaultDataPhoneId());
        } else if (subscriptionManager.getDefaultDataPhoneId() == 1) {
            if (DEBUG) Log.d(TAG, "current subid: " + subscriptionManager.getDefaultDataPhoneId());

            if (data_enabled) {
                mTelephonyManager1.setDataEnabled(true);
                mTelephonyManager2.setDataEnabled(false);
            }
            subscriptionManager.setDefaultDataSubId(1);

            if (DEBUG) Log.d(TAG, "new subid: " + subscriptionManager.getDefaultDataPhoneId());
        }
        updateTile();
    }
}
