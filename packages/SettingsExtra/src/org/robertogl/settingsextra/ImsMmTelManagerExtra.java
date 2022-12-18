package org.robertogl.settingsextra;

import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.RegistrationManager;
import android.telephony.ims.feature.MmTelFeature;
import android.util.Log;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import static android.content.Context.MODE_PRIVATE;

import java.lang.Thread;

public class ImsMmTelManagerExtra {

    private final static String TAG = "ImsMmTelManagerExtra";

    private final static boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private Context deviceProtectedContext;

    private ImsMmTelManager mImsMmTelManager;

    private boolean imsRegistered = false;

    private boolean volteVoiceCapable = false;

    private boolean volteVideoCapable = false;

    private boolean vowifiVoiceCapable = false;

    private boolean vowifiVideoCapable = false;

    private int mNetworkType = 0;

    private PhoneStateListener mPhoneStateListener;

    private int mSubId = 0;

    private Handler mReceiverHandler;

    private StatusBarManager mStatusBarManager;

    private TelephonyManager mTelephony;

    private boolean isImsExtraEnabled = false;

    private int connectionState = 0;

    protected boolean isRunning = false;

    private boolean userWantsVolteIcon = false;

    private boolean userWantsVowifiIcon = false;

    private Handler mHandlerCheck;

    private boolean volteConfirmationCheckNeeded = true;

    private boolean vowifiConfirmationCheckNeeded = true;

    private static int showVolteIconTimeoutInt = 5000;

    private static int showVowifiIconTimeoutInt = 5000;

    protected void notifyUserSettingChange() {
        if (DEBUG) Log.d(TAG, "User has change some settings");
        // Check if we should show the icons
        volteConfirmationCheckNeeded = true;
        vowifiConfirmationCheckNeeded = true;
        showVolteIcon(isVolteAvailable());
        showVoWifiIcon(isVowifiAvailable());
    }

    //@SuppressLint("WrongConstant")
    protected void onStartup(Context context, int subId) {
        mContext = context;
        mSubId = subId;
        if (DEBUG) Log.d(TAG, "Registering to OnSubscriptionsChanged: " + mSubId);
        SubscriptionManager mSubMgr = (SubscriptionManager) mContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mSubMgr.addOnSubscriptionsChangedListener(mSubscriptionsChangedListener);

        TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        mTelephony = telephonyManager.createForSubscriptionId(mSubId);

        mReceiverHandler = new Handler(Looper.myLooper());

        mHandlerCheck = new Handler(Looper.myLooper());

        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state, int networkType) {
                super.onDataConnectionStateChanged(state, networkType);
                if (DEBUG) {
                    Log.d(TAG, "onDataConnectionStateChanged: state=" + state
                            + " type=" + networkType + " on subId: " + mSubId);
                }
                connectionState = state;
                mNetworkType = networkType;
                volteConfirmationCheckNeeded = true;
                vowifiConfirmationCheckNeeded = true;
                showVolteIcon(isVolteAvailable());
                showVoWifiIcon(isVowifiAvailable());
            }
        };

        //mTelephony
        registerListener();

        if (DEBUG) Log.d(TAG, "Registering to ImsMmTelManager");
        ImsManager mImsManager = new ImsManager(mContext);
        mImsMmTelManager = mImsManager.getImsMmTelManager(mSubId);

        // Before showing anything, check the user will
        deviceProtectedContext = mContext.createDeviceProtectedStorageContext();

        // Check if Volte or vowifi already enabled
        volteConfirmationCheckNeeded = true;
        vowifiConfirmationCheckNeeded = true;
        showVolteIcon(isVolteAvailable());
        showVoWifiIcon(isVowifiAvailable());

        // The service is now running
        isRunning = true;
    }

    protected void onClose() {
        if (DEBUG) Log.d(TAG, "Service is being shutdown on mSubId: " + mSubId);
        // Set the service as stopped
        isRunning = false;
        volteConfirmationCheckNeeded = false;
        vowifiConfirmationCheckNeeded = false;
        showVolteIcon(false);
        showVoWifiIcon(false);
        if (!(mImsMmTelManager == null)) {
            unregisterAll();
        }
        mReceiverHandler.removeCallbacksAndMessages(null);
        mHandlerCheck.removeCallbacksAndMessages(null);
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    private void registerListener() {
        if (DEBUG) Log.d(TAG, "registerListener" + mSubId);
        mTelephony.listen(mPhoneStateListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                        | PhoneStateListener.LISTEN_CALL_STATE
                        | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                        | PhoneStateListener.LISTEN_DATA_ACTIVITY
                        | PhoneStateListener.LISTEN_DATA_ACTIVATION_STATE
                        | PhoneStateListener.LISTEN_CARRIER_NETWORK_CHANGE
                        | PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED);
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.MOBILE_DATA + mSubId),
                true, mObserver);
        mContext.getContentResolver().registerContentObserver(Uri.withAppendedPath(SubscriptionManager.ADVANCED_CALLING_ENABLED_CONTENT_URI, Integer.toString(mSubId)),
                true, mObserver);
        mContext.getContentResolver().registerContentObserver(Uri.withAppendedPath(SubscriptionManager.WFC_ENABLED_CONTENT_URI, Integer.toString(mSubId)),
                true, mObserver);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (DEBUG) Log.d(TAG, "mObserver on subId: " + mSubId);
            volteConfirmationCheckNeeded = true;
            vowifiConfirmationCheckNeeded = true;
            showVolteIcon(isVolteAvailable());
            showVoWifiIcon(isVowifiAvailable());
        }
    };

    private void unregisterListener() {
        mTelephony.listen(mPhoneStateListener, 0);
    }

    private void unregisterAll() {
        mImsMmTelManager.unregisterImsRegistrationCallback(mImsRegistrationCallback);
        mImsMmTelManager.unregisterMmTelCapabilityCallback(mImsCapabilityCallback);
        unregisterListener();
        isImsExtraEnabled = false;
    }

    private void checkCurrentSituation() {
        imsRegistered = mTelephony.isImsRegistered();

        volteVoiceCapable = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE, ImsRegistrationImplBase.REGISTRATION_TECH_LTE);

        volteVideoCapable = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO, ImsRegistrationImplBase.REGISTRATION_TECH_LTE);

        vowifiVoiceCapable = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE, ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);

        vowifiVideoCapable = mImsMmTelManager.isAvailable(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO, ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
    }

    // We only care about this: is Volte actually available?
    private boolean isVolteAvailable() {
        try {
            checkCurrentSituation();
        } catch (RuntimeException e) {
            if (DEBUG) Log.d(TAG, "Telephony Service is not ready yet");
            return false;
        }
        if (DEBUG)
            Log.d(TAG, "Status on " + mSubId + ": volte switch: " + isVolteSwitchOn() + ", voice capable: "
                    + volteVoiceCapable + ", video capable: " + volteVideoCapable + ", imsRegistered: " + imsRegistered + ", connection state: " + connectionState + ", network type:" + mNetworkType);
        return isVolteSwitchOn() && (volteVoiceCapable || volteVideoCapable) && imsRegistered;
    }

    // Well, we also care about this: is VoWifi actually available?
    private boolean isVowifiAvailable() {
        try {
            checkCurrentSituation();
        } catch (RuntimeException e) {
            if (DEBUG) Log.d(TAG, "Telephony Service is not ready yet");
            return false;
        }
        if (DEBUG)
            Log.d(TAG, "Status on " + mSubId + ": vowifi switch: " + isVoWifiSwitchOn() + ", voice capable: "
                    + vowifiVoiceCapable + ", video capable: " + vowifiVideoCapable + ", imsRegistered: " + imsRegistered + ", connection state: " + connectionState + ", network type:" + mNetworkType);
        return isVoWifiSwitchOn() && (vowifiVoiceCapable || vowifiVideoCapable) && imsRegistered;
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DEBUG) Log.d(TAG, "Subscription event");
            int newSIM = SubscriptionManager.getDefaultDataSubscriptionId();

            // If the subscription is invalid, just do nothing
            if (newSIM == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                if (DEBUG) Log.d(TAG, "mSubId: " + mSubId + ", INVALID_SUBSCRIPTION_ID, return");
                return;
            }

            // We only care about the subscription on one sim
            if (mSubId != newSIM) {
                if (DEBUG) Log.d(TAG, "We don't care about subscription: " + newSIM);
                return;
            }
            // If we are already enabled, don't enable the callbacks again
            if (isImsExtraEnabled) {
                if (DEBUG) Log.d(TAG, "We are already subscribed to " + mSubId);
                return;
            }

            if (DEBUG) Log.d(TAG, "Subscription ID: " + mSubId);

            // Enable Ims callbacks
            enableImsCallbacks();
        }
    };

    private void enableImsCallbacks() {
        try {
            mImsMmTelManager.registerImsRegistrationCallback(mContext.getMainExecutor(), mImsRegistrationCallback);
            isImsExtraEnabled = true;
        } catch (ImsException e) {
            isImsExtraEnabled = false;
            mImsMmTelManager.unregisterImsRegistrationCallback(mImsRegistrationCallback);
            if (DEBUG) Log.d(TAG, "Failed to enable RegistrationCallback for mSubId: " + mSubId);
            return;
        }
        try {
            mImsMmTelManager.registerMmTelCapabilityCallback(mContext.getMainExecutor(), mImsCapabilityCallback);
            isImsExtraEnabled = true;
        } catch (ImsException e) {
            isImsExtraEnabled = false;
            mImsMmTelManager.unregisterMmTelCapabilityCallback(mImsCapabilityCallback);
            if (DEBUG) Log.d(TAG, "Failed to enable CapabilityCallback for mSubId: " + mSubId);
        }
    }

    private final ImsMmTelManager.CapabilityCallback mImsCapabilityCallback =
            new ImsMmTelManager.CapabilityCallback() {
                @Override
                public void onCapabilitiesStatusChanged(MmTelFeature.MmTelCapabilities config) {
                    if (DEBUG) Log.d(TAG, "onCapabilitiesStatusChanged");
                    volteConfirmationCheckNeeded = true;
                    vowifiConfirmationCheckNeeded = true;
                    showVolteIcon(isVolteAvailable());
                    showVoWifiIcon(isVowifiAvailable());
                }
            };

    private final RegistrationManager.RegistrationCallback mImsRegistrationCallback =
            new RegistrationManager.RegistrationCallback() {
                @Override
                public void onRegistered(int imsRadioTech) {
                    if (DEBUG) Log.d(TAG, "onRegistered");
                    volteConfirmationCheckNeeded = true;
                    vowifiConfirmationCheckNeeded = true;
                    showVolteIcon(isVolteAvailable());
                    showVoWifiIcon(isVowifiAvailable());
                }

                @Override
                public void onUnregistered(ImsReasonInfo info) {
                    if (DEBUG) Log.d(TAG, "onUnregistered");
                    volteConfirmationCheckNeeded = true;
                    vowifiConfirmationCheckNeeded = true;
                    showVolteIcon(isVolteAvailable());
                    showVoWifiIcon(isVowifiAvailable());
                }
            };

    private boolean isVolteSwitchOn() {
        try {
            return mImsMmTelManager.isAdvancedCallingSettingEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVoWifiSwitchOn() {
        try {
            return mImsMmTelManager.isVoWiFiSettingEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressLint("WrongConstant")
    private void showVolteIcon(boolean isVolteAvailable) {
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        userWantsVolteIcon = pref.getBoolean("showVolteIcon", false);

        if (volteConfirmationCheckNeeded) {
            mHandlerCheck.postDelayed(() -> {
                   volteConfirmationCheckNeeded = false;
                   showVolteIcon(isVolteAvailable());
            }, showVolteIconTimeoutInt);
        }

        if (DEBUG)
            Log.d(TAG, "isVolteAvailable: " + isVolteAvailable + ", userWantsVolteIcon: " + userWantsVolteIcon);

        boolean show = isVolteAvailable && userWantsVolteIcon;
        String volteIconName = "volte_extra_" + mSubId;
        if (DEBUG) Log.d(TAG, "Volte icon change detected");
        mStatusBarManager = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        if (!show) {
            if (DEBUG) Log.d(TAG, "Removing Volte icon");
            mStatusBarManager.setIconVisibility(volteIconName, false);
            mStatusBarManager.removeIcon(volteIconName);
        } else {
            if (DEBUG) Log.d(TAG, "Showing Volte icon");
            mStatusBarManager.setIcon(volteIconName, R.drawable.ic_volte, 0, null);
            mStatusBarManager.setIconVisibility(volteIconName, true);
        }
    }

    @SuppressLint("WrongConstant")
    private void showVoWifiIcon(boolean isVowifiAvailable) {
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        userWantsVowifiIcon = pref.getBoolean("showVowifiIcon", false);

        if (vowifiConfirmationCheckNeeded) {
            mHandlerCheck.postDelayed(() -> {
                   vowifiConfirmationCheckNeeded = false;
                   showVoWifiIcon(isVowifiAvailable());
            }, showVowifiIconTimeoutInt);
        }

        if (DEBUG)
            Log.d(TAG, "isVowifiAvailable: " + isVowifiAvailable + ", userWantsVowifiIcon: " + userWantsVowifiIcon);

        boolean show = isVowifiAvailable && userWantsVowifiIcon;
        String volteIconName = "vowifi_extra_" + mSubId;
        if (DEBUG) Log.d(TAG, "VoWifi icon change detected");
        mStatusBarManager = (StatusBarManager) mContext.getSystemService(Context.STATUS_BAR_SERVICE);
        if (!show) {
            if (DEBUG) Log.d(TAG, "Removing VoWifi icon");
            mStatusBarManager.setIconVisibility(volteIconName, false);
            mStatusBarManager.removeIcon(volteIconName);
        } else {
            if (DEBUG) Log.d(TAG, "Showing VoWifi icon");
            mStatusBarManager.setIcon(volteIconName, R.drawable.ic_vowifi, 0, null);
            mStatusBarManager.setIconVisibility(volteIconName, true);
        }
    }
}
