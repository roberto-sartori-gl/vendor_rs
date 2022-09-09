package org.robertogl.settingsextra;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;

import java.util.List;
import java.util.Set;

import static android.content.Context.MODE_PRIVATE;

public class DefaultPermissionGrantPolicyExtra {

    private final static String TAG = "DefaultPermissionGrantPolicyExtra";

    private final static boolean DEBUG = MainService.DEBUG;

    protected void onStartup(Context mContext) {

        if (DEBUG) Log.d(TAG, "DefaultPermissionGrantPolicyExtra initializing...");
        PackageManager mPackageManager = mContext.getPackageManager();

        UserHandle mUserHandle = mContext.getUser();

        SharedPreferences pref = mContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        String packageName;

        boolean permissions_already_allowed;
        boolean is_package_installed;

        // Google Quick Search Box
        packageName = "com.google.android.googlequicksearchbox";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Quick Search Box...");

            for (String mPermission : CALENDAR_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CAMERA_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CONTACTS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : ALWAYS_LOCATION_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : MICROPHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : PHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : SMS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : STORAGE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
    }

        // Google Play Services
        packageName = "com.google.android.gms";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Play Services...");

            for (String mPermission : SENSORS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CALENDAR_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CAMERA_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CONTACTS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : ALWAYS_LOCATION_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : MICROPHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : PHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : SMS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : STORAGE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }


        // Google Contacts Sync
        packageName = "com.google.android.syncadapters.contacts";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Contacts Sync...");

            for (String mPermission : CONTACTS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }

        // Google Play Framework
        packageName = "com.google.android.gsf";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Play Framework...");

            for (String mPermission : PHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }


        // Google Setup Wizard
        packageName = "com.google.android.setupwizard";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Setup Wizard...");

            for (String mPermission : CONTACTS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : PHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : ALWAYS_LOCATION_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : CAMERA_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }



        // Google Play Store
        packageName = "com.android.vending";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Google Play Store...");

            for (String mPermission : CONTACTS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : PHONE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : ALWAYS_LOCATION_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : SMS_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            for (String mPermission : STORAGE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }

        // Contacts
        packageName = "com.android.contacts";
        permissions_already_allowed = pref.getBoolean(packageName + "_processed", false);
        is_package_installed = isPackageInstalled(mPackageManager, packageName);
        if (is_package_installed && !permissions_already_allowed) {
            if (DEBUG)
                Log.d(TAG, "DefaultPermissionGrantPolicyExtra for Contacts...");

            for (String mPermission : STORAGE_PERMISSIONS) {
                mGrantRuntimePermission(packageName, mPermission, mUserHandle, mPackageManager);
            }

            editor.putBoolean(packageName + "_processed", true).apply();
        } else if (!is_package_installed){
            editor.putBoolean(packageName + "_processed", false).apply();
        }

    }

    private boolean isPackageInstalled(PackageManager mPackageManager, String targetPackage){
        List<ApplicationInfo> packages;

        packages = mPackageManager.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(targetPackage))
                return true;
        }
        return false;
    }

    private void mGrantRuntimePermission(String packageName, String mPermission, UserHandle mUserHandle, PackageManager mPackageManager) {
        try {
            mPackageManager.grantRuntimePermission(packageName, mPermission, mUserHandle);
        } catch (SecurityException e) {
            Log.d(TAG, "Package: " + packageName + " does not need permission: " + mPermission);
        }
    }


    private static final Set<String> PHONE_PERMISSIONS = new ArraySet<>();
    static {
        PHONE_PERMISSIONS.add(Manifest.permission.READ_PHONE_STATE);
        PHONE_PERMISSIONS.add(Manifest.permission.CALL_PHONE);
        PHONE_PERMISSIONS.add(Manifest.permission.READ_CALL_LOG);
        PHONE_PERMISSIONS.add(Manifest.permission.WRITE_CALL_LOG);
        PHONE_PERMISSIONS.add(Manifest.permission.ADD_VOICEMAIL);
        PHONE_PERMISSIONS.add(Manifest.permission.USE_SIP);
        PHONE_PERMISSIONS.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
    }

    private static final Set<String> CONTACTS_PERMISSIONS = new ArraySet<>();
    static {
        CONTACTS_PERMISSIONS.add(Manifest.permission.READ_CONTACTS);
        CONTACTS_PERMISSIONS.add(Manifest.permission.WRITE_CONTACTS);
        CONTACTS_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
    }

    private static final Set<String> ALWAYS_LOCATION_PERMISSIONS = new ArraySet<>();
    static {
        ALWAYS_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        ALWAYS_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        ALWAYS_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    private static final Set<String> FOREGROUND_LOCATION_PERMISSIONS = new ArraySet<>();
    static {
        FOREGROUND_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        FOREGROUND_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private static final Set<String> COARSE_BACKGROUND_LOCATION_PERMISSIONS = new ArraySet<>();
    static {
        COARSE_BACKGROUND_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        COARSE_BACKGROUND_LOCATION_PERMISSIONS.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    private static final Set<String> ACTIVITY_RECOGNITION_PERMISSIONS = new ArraySet<>();
    static {
        ACTIVITY_RECOGNITION_PERMISSIONS.add(Manifest.permission.ACTIVITY_RECOGNITION);
    }

    private static final Set<String> CALENDAR_PERMISSIONS = new ArraySet<>();
    static {
        CALENDAR_PERMISSIONS.add(Manifest.permission.READ_CALENDAR);
        CALENDAR_PERMISSIONS.add(Manifest.permission.WRITE_CALENDAR);
    }

    private static final Set<String> SMS_PERMISSIONS = new ArraySet<>();
    static {
        SMS_PERMISSIONS.add(Manifest.permission.SEND_SMS);
        SMS_PERMISSIONS.add(Manifest.permission.RECEIVE_SMS);
        SMS_PERMISSIONS.add(Manifest.permission.READ_SMS);
        SMS_PERMISSIONS.add(Manifest.permission.RECEIVE_WAP_PUSH);
        SMS_PERMISSIONS.add(Manifest.permission.RECEIVE_MMS);
        SMS_PERMISSIONS.add(Manifest.permission.READ_CELL_BROADCASTS);
    }

    private static final Set<String> MICROPHONE_PERMISSIONS = new ArraySet<>();
    static {
        MICROPHONE_PERMISSIONS.add(Manifest.permission.RECORD_AUDIO);
    }

    private static final Set<String> CAMERA_PERMISSIONS = new ArraySet<>();
    static {
        CAMERA_PERMISSIONS.add(Manifest.permission.CAMERA);
    }

    private static final Set<String> SENSORS_PERMISSIONS = new ArraySet<>();
    static {
        SENSORS_PERMISSIONS.add(Manifest.permission.BODY_SENSORS);
    }

    private static final Set<String> STORAGE_PERMISSIONS = new ArraySet<>();
    static {
        STORAGE_PERMISSIONS.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        STORAGE_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        STORAGE_PERMISSIONS.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
    }
}
