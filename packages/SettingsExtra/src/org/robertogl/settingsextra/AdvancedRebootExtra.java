package org.robertogl.settingsextra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.util.Log;

public class AdvancedRebootExtra {

    private static String TAG = "AdvancedRebootExtra";

    private static boolean DEBUG = MainService.DEBUG;

    protected void showAdvancedRebootOptions(Context mContext) {
        String[] options = {"Shutdown", "Reboot", "Reboot in recovery", "Reboot in fastboot mode (bootloader)"};

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Select the reboot option");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (DEBUG) Log.d(TAG, "Clicked on Advanced Reboot");
                PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                if (which == 0) {
                    if (DEBUG) Log.d(TAG, "Shutting down");
                    pm.shutdown(true, null, false);
                } else if (which == 1) {
                    if (DEBUG) Log.d(TAG, "Rebooting");
                    pm.reboot(null);
                } else if (which == 2) {
                    if (DEBUG) Log.d(TAG, "Rebooting in recovery");
                    pm.reboot("recovery-update");
                } else if (which == 3) {
                    if (DEBUG) Log.d(TAG, "Rebooting in fastboot mode (bootloader)");
                    pm.reboot("bootloader");
                }
            }
        });
        builder.show();
    }
}
