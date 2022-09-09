package org.robertogl.settingsextra;

import android.annotation.SuppressLint;
import android.app.StatusBarManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.STATUS_BAR_SERVICE;

public class BluetoothBatteryIcon {

    private static final String TAG = "BluetoothBatteryIcon";

    private static final boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private StatusBarManager mStatusBarManager;

    private boolean isBluetoothOff = true;

    private List<String> mBluetoothBatteryConnectedAddress = new ArrayList<String>();

    private int bluetoothDeviceConnected = 0;

    protected void onStartup(Context context) {
        mContext = context;
        // Listen for Bluetooth events (ON/OFF/CONNECTED/DISCONNECTED/bluetooth battery level changes)
        IntentFilter mBluetoothFilter = new IntentFilter();
        mBluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothFilter.addAction(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED);
        mBluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mBluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        mContext.registerReceiver(mBluetoothReceiver, mBluetoothFilter);
    }

    protected void onClose() {
        mContext.unregisterReceiver(mBluetoothReceiver);
    }

    @SuppressLint("WrongConstant")
    private void updateBatteryLevel(int batteryLevel) {
        // If we are showing the default bluetooth icon, remove that now
        Utils.removeUnwantendStatusBarIcon(mContext, "bluetooth");
        if (DEBUG)
            Log.d(TAG, "bluetooth: updating icon on status bar with battery level: " + batteryLevel);
        int iconId = 0;
        mStatusBarManager = (StatusBarManager) mContext.getSystemService(mContext.STATUS_BAR_SERVICE);
        if (batteryLevel == -1 || batteryLevel == -100) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected;
        } else if (batteryLevel == 100) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_9;
        } else if (batteryLevel >= 90) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_8;
        } else if (batteryLevel >= 80) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_7;
        } else if (batteryLevel >= 70) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_6;
        } else if (batteryLevel >= 60) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_5;
        } else if (batteryLevel >= 50) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_4;
        } else if (batteryLevel >= 40) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_3;
        } else if (batteryLevel >= 30) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_2;
        } else if (batteryLevel >= 20) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_1;
        } else if (batteryLevel >= 10) {
            iconId = R.drawable.stat_sys_data_bluetooth_connected_battery_0;
        }
        mStatusBarManager.setIcon("bluetooth_extra", iconId, 0, null);
        // Actually show the icon
        mStatusBarManager.setIconVisibility("bluetooth_extra", true);
    }

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @SuppressLint("WrongConstant")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            mStatusBarManager = (StatusBarManager) mContext.getSystemService(mContext.STATUS_BAR_SERVICE);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (DEBUG) Log.d(TAG, "bluetooth in ACTION_STATE_CHANGED");
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_ON:
                        if (DEBUG) Log.d(TAG, "bluetooth in STATE_ON");
                        // If we are showing the default bluetooth icon, try to remove that now
                        // Remove also the 'rotate' icon that for some reason appears when other icons disappear
                        // If this does not work, we'll try again when it's time to use our own icon
                        Utils.removeUnwantendStatusBarIcon(context, "rotate");
                        Utils.removeUnwantendStatusBarIcon(context, "bluetooth");
                        break;
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        if (DEBUG) Log.d(TAG, "bluetooth in STATE_OFF/DISCONNECTED");
                        mBluetoothBatteryConnectedAddress.clear();
                        mStatusBarManager.setIconVisibility("bluetooth_extra", false);
                        mStatusBarManager.removeIcon("bluetooth_extra");
                        isBluetoothOff = true;
                        bluetoothDeviceConnected = 0;
                        break;
                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                if (DEBUG) Log.d(TAG, "bluetooth in ACTION_ACL_DISCONNECTED");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mBluetoothBatteryConnectedAddress.contains(device.getAddress()))
                    mBluetoothBatteryConnectedAddress.remove(device.getAddress());
                bluetoothDeviceConnected -= 1;
                if (bluetoothDeviceConnected <= 0) {
                    if (DEBUG) Log.d(TAG, "bluetooth devices all disconnected");
                    mBluetoothBatteryConnectedAddress.clear();
                    mStatusBarManager.setIconVisibility("bluetooth_extra", false);
                    mStatusBarManager.removeIcon("bluetooth_extra");
                    isBluetoothOff = true;
                    bluetoothDeviceConnected = 0;
                } else if (mBluetoothBatteryConnectedAddress.size() > 0) {
                    if (DEBUG) Log.d(TAG, "bluetooth devices with battery are still connected");
                    isBluetoothOff = false;
                } else if (bluetoothDeviceConnected > 0) {
                    if (DEBUG) Log.d(TAG, "bluetooth devices without battery are still connected");
                    mBluetoothBatteryConnectedAddress.clear();
                    updateBatteryLevel(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
                    isBluetoothOff = true;
                }
            } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                if (DEBUG) Log.d(TAG, "bluetooth in ACTION_ACL_CONNECTED");
                if (!(mBluetoothBatteryConnectedAddress.size() > 0))
                    updateBatteryLevel(BluetoothDevice.BATTERY_LEVEL_UNKNOWN);
                bluetoothDeviceConnected += 1;
                isBluetoothOff = false;
            } else if (action.equals(BluetoothDevice.ACTION_BATTERY_LEVEL_CHANGED)) {
                int batteryLevel = intent.getIntExtra(BluetoothDevice.EXTRA_BATTERY_LEVEL, BluetoothDevice.BATTERY_LEVEL_BLUETOOTH_OFF);
                if (!isBluetoothOff) updateBatteryLevel(batteryLevel);
                if (batteryLevel >= 0) {
                    if (DEBUG) Log.d(TAG, "bluetooth device has battery");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!mBluetoothBatteryConnectedAddress.contains(device.getAddress()))
                        mBluetoothBatteryConnectedAddress.add(device.getAddress());
                }
                if (DEBUG) Log.d(TAG, "bluetooth battery change detected: " + batteryLevel);
            }
        }
    };
}
