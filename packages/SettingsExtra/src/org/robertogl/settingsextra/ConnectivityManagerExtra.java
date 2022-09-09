package org.robertogl.settingsextra;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.telephony.RadioAccessFamily;

import static android.content.Context.MODE_PRIVATE;

public class ConnectivityManagerExtra {

    private final String TAG = "ConnectivityManagerExtra";

    private final boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private ConnectivityManager connMgr;

    private boolean isWifiConnected = false;

    protected void onStartup(Context context) {
        mContext = context;
        connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder mNetworksBuilder = new NetworkRequest.Builder();
        connMgr.requestNetwork(mNetworksBuilder.build(), mNetworkCallback);

        // Set a default mode based on wifi connection status
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getConnectionInfo().getNetworkId() == -1) {
            isWifiConnected = false;
            forceDefaultNetworkMode();
        }
        else  {
            isWifiConnected = true;
            forceNetworkMode();
        }
    }

    protected void onClose() {
        if (mContext == null) return;
        forceDefaultNetworkMode();
        connMgr.unregisterNetworkCallback(mNetworkCallback);
    }

    private final ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (DEBUG) Log.d(TAG, "network: connection event");
            NetworkCapabilities networkCapabilities = connMgr.getNetworkCapabilities(network);
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                if (DEBUG) Log.d(TAG, "wifi: connected");
                forceNetworkMode();
                isWifiConnected = true;
            } else if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                if (DEBUG) Log.d(TAG, "cellular network: connected");
                forceDefaultNetworkMode();
                isWifiConnected = false;
            }
        }

        @Override
        public void onLost(Network network) {
            if (DEBUG) Log.d(TAG, "network: disconnection event");
            WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            if ((wifiManager.getConnectionInfo().getNetworkId() == -1) && isWifiConnected) {
                if (DEBUG) Log.d(TAG, "wifi: off");
                // Wifi has been disconnected!
                forceDefaultNetworkMode();
                isWifiConnected = false;
                return;
            }
            NetworkCapabilities networkCapabilities = connMgr.getNetworkCapabilities(network);
            if (networkCapabilities == null) {
                if (DEBUG) Log.d(TAG, "No network available, we were probably connected to a 2/3/4G network");
                return;
            }
            if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI ) && isWifiConnected) {
                if (DEBUG) Log.d(TAG, "wifi: disconnected");
                forceDefaultNetworkMode();
                isWifiConnected = false;
            }
        }
    };

    protected void forceNetworkSettingsUpdate() {
        // Set a mode based on wifi connection status
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.getConnectionInfo().getNetworkId() == -1) {
            isWifiConnected = false;
            forceDefaultNetworkMode();
        }
        else  {
            isWifiConnected = true;
            forceNetworkMode();
        }
    }

    private void forceDefaultNetworkMode() {
        if (DEBUG)
            Log.d(TAG, "Setting default preferred network as wifi is disconnected");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        int networkMode = Integer.parseInt(pref.getString("preferred_network_mode_key", "0"));
        if (DEBUG) Log.d(TAG, "Default networkMode : " + networkMode);
        int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (DEBUG) Log.d(TAG, "No subscription ID found on forceDefaultNetworkMode");
            return;
        }
        if (DEBUG) Log.d(TAG, "Subscription id on forceDefaultNetworkMode: " + defaultSubId);
        TelephonyManager mTelephonyCreator = mContext.getSystemService(TelephonyManager.class);
        TelephonyManager mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId);
        mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
        if (DEBUG) Log.d(TAG, "We support " + mTelephony.getActiveModemCount() + " sim");
        // Just a safety check: on my device, I've always two active modems even with only one sim inserted
        if (mTelephony.getActiveModemCount() > 1) {
            if (defaultSubId == 1) {
                mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId + 1);
                mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
            } else if (defaultSubId == 2) {
                mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId - 1);
                mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
            }
        }
    }

    private void forceNetworkMode() {
        if (DEBUG) Log.d(TAG, "Setting default preferred network as wifi is connected");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        int networkMode = Integer.parseInt(pref.getString("preferred_network_mode_key_wifi", "0"));
        if (DEBUG) Log.d(TAG, "networkMode with wifi on: " + networkMode);
        int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (DEBUG) Log.d(TAG, "No subscription ID found on forceNetworkMode");
            return;
        }
        if (DEBUG) Log.d(TAG, "Subscription id on forceNetworkMode: " + defaultSubId);
        TelephonyManager mTelephonyCreator = mContext.getSystemService(TelephonyManager.class);
        TelephonyManager mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId);
        mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
        if (DEBUG) Log.d(TAG, "We support " + mTelephony.getActiveModemCount() + " sim");
        // Just a safety check: on my device, I've always two active modems even with only one sim inserted
        if (mTelephony.getActiveModemCount() > 1) {
            if (defaultSubId == 1) {
                mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId + 1);
                mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
            } else if (defaultSubId == 2) {
                mTelephony = mTelephonyCreator.createForSubscriptionId(defaultSubId - 1);
                mTelephony.setPreferredNetworkTypeBitmask(RadioAccessFamily.getRafFromNetworkType(networkMode));
            }
        }
    }
}


