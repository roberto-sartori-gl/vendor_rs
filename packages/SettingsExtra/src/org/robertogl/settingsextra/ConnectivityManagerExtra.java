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

import static android.content.Context.MODE_PRIVATE;

public class ConnectivityManagerExtra {

    private String TAG = "ConnectivityManagerExtra";

    private boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private ConnectivityManager connMgr;

    private boolean isWifiConnected = false;

    /*private SubscriptionManager mSubMgr;

    private int currentEnabledSIM;*/

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

        /*currentEnabledSIM = SubscriptionManager.getDefaultDataSubscriptionId();
        mSubMgr = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mSubMgr.addOnSubscriptionsChangedListener(mSubscriptionsChangedListener);*/
    }

    protected void onClose() {
        if (mContext == null) return;
        forceDefaultNetworkMode();
        connMgr.unregisterNetworkCallback(mNetworkCallback);
    }


    /*private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DEBUG) Log.d(TAG, "Subscription changed");
            int newDefaultSIM = SubscriptionManager.getDefaultDataSubscriptionId();
            if (currentEnabledSIM == newDefaultSIM) {
                if (DEBUG) Log.d(TAG, "Old subscription is the same as new subscription...");
                return;
            }
            currentEnabledSIM = newDefaultSIM;
            if (isWifiConnected) forceNetworkMode();
            else forceDefaultNetworkMode();
        }
    };*/

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        /*@Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            // Code!
        }*/

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
                forceNetworkSettingsUpdate();
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
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                if (DEBUG) Log.d(TAG, "wifi: disconnected");
                forceDefaultNetworkMode();
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
        int networkMode = Integer.valueOf(pref.getString("preferred_network_mode_key", "0"));
        if (DEBUG) Log.d(TAG, "Default networkMode : " + networkMode);
        int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (DEBUG) Log.d(TAG, "No subscription ID found on forceDefaultNetworkMode");
            return;
        }
        if (DEBUG) Log.d(TAG, "Subscription id on forceDefaultNetworkMode: " + defaultSubId);
        TelephonyManager mTelephony = mContext.getSystemService(TelephonyManager.class);
        mTelephony.setPreferredNetworkType(defaultSubId, networkMode);
        if (DEBUG) Log.d(TAG, "We support " +  mTelephony.getActiveModemCount() + " sim");
        // Just a safety check: on my device, I've always two active modems even with only one sim inserted
        if (mTelephony.getActiveModemCount() > 1) {
            if (defaultSubId == 1)
                mTelephony.setPreferredNetworkType(defaultSubId + 1, networkMode);
            else if (defaultSubId == 2)
                mTelephony.setPreferredNetworkType(defaultSubId - 1, networkMode);
        }
    }

    private void forceNetworkMode() {
        if (DEBUG) Log.d(TAG, "Setting default preferred network as wifi is connected");
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        int networkMode = Integer.valueOf(pref.getString("preferred_network_mode_key_wifi", "0"));
        if (DEBUG) Log.d(TAG, "networkMode with wifi on: " + networkMode);
        int defaultSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            if (DEBUG) Log.d(TAG, "No subscription ID found on forceNetworkMode");
            return;
        }
        if (DEBUG) Log.d(TAG, "Subscription id on forceNetworkMode: " + defaultSubId);
        TelephonyManager mTelephony = mContext.getSystemService(TelephonyManager.class);
        mTelephony.setPreferredNetworkType(defaultSubId, networkMode);
        if (DEBUG) Log.d(TAG, "We support " +  mTelephony.getActiveModemCount() + " sim");
        // Just a safety check: on my device, I've always two active modems even with only one sim inserted
        if (mTelephony.getActiveModemCount() > 1) {
            if (defaultSubId == 1)
                mTelephony.setPreferredNetworkType(defaultSubId + 1, networkMode);
            else if (defaultSubId == 2)
                mTelephony.setPreferredNetworkType(defaultSubId - 1, networkMode);
        }
    }
}


