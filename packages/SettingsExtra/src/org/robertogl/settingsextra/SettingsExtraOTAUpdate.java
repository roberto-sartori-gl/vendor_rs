package org.robertogl.settingsextra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SettingsExtraOTAUpdate {
    private final static String TAG = "SettingsExtraOTAUpdate";

    private final static boolean DEBUG = MainService.DEBUG;

    private static final String[] RECOVERY_PACKAGES = {"--update_package=/cache/recovery/update.zip\n",
                                                       "--update_package=/cache/recovery/magisk.zip\n",
                                                       "--update_package=/cache/recovery/gapps.zip\n",
                                                       "--update_package=/cache/recovery/decrypt.zip\n"};

    private static final String[] RECOVERY_PACKAGES_MAP = {"--update_package=@/cache/recovery/update.map\n",
                                                           "--update_package=@/cache/recovery/magisk.map\n",
                                                           "--update_package=@/cache/recovery/gapps.map\n",
                                                           "--update_package=@/cache/recovery/decrypt.map\n"};

    private static final String[] SDCARD_RECOVERY_PACKAGES = {"update.zip",
                                                              "magisk.zip",
                                                              "gapps.zip",
                                                              "decrypt.zip"};

    private static final String[] CACHE_RECOVERY_MAP = {"/cache/recovery/update.map",
                                                        "/cache/recovery/magisk.map",
                                                        "/cache/recovery/gapps.map",
                                                        "/cache/recovery/decrypt.map"};

    private static final String[] CACHE_RECOVERY = {"/cache/recovery/update.zip",
                                                    "/cache/recovery/magisk.zip",
                                                    "/cache/recovery/gapps.zip",
                                                    "/cache/recovery/decrypt.zip"};

    private static final String TEMP_DATA_PREFIX = "/data/cache/";

    private static final String INIT_SERVICE_UNCRYPT = "init.svc.uncrypt_custom";


    protected void startUpdate (Context mContext){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Select the updates to be applied");

        String[] updates = {"Android OS (/sdcard/update.zip)", "Magisk (/sdcard/magisk.zip)", "Gapps (/sdcard/gapps.zip), max 200MB",
                "Decryption zip (/sdcard/decrypt.zip)"};
        boolean[] checkedItems = {false, false, false, false};
        builder.setMultiChoiceItems(updates, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // The user checked or unchecked a box
            }
        });

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // The user clicked OK
                // Android OS = 0, Magisk = 1, Gapps = 2
                String command_string = "";
                boolean reboot = false;
                for (int i = 0 ; i < checkedItems.length; i++) {
                    File map_file = new File(CACHE_RECOVERY_MAP[i]);
                    if (map_file.exists()) map_file.delete();

                    if (checkedItems[i])  {
                        File origin_file = new File("/sdcard/" + SDCARD_RECOVERY_PACKAGES[i]);
                        if (!origin_file.exists()) {
                            if (DEBUG) Log.d(TAG,  SDCARD_RECOVERY_PACKAGES[i] + " not present!");
                            Toast.makeText(mContext, SDCARD_RECOVERY_PACKAGES[i] + " not present!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // OTA
                        if (i==0) {
                            String dest_path = TEMP_DATA_PREFIX + SDCARD_RECOVERY_PACKAGES[i];
                            File dest_file = new File(dest_path);
                            if (dest_file.exists()) dest_file.delete();
                            try {
                                Toast.makeText(mContext, "Copying files...", Toast.LENGTH_LONG).show();
                                copy(origin_file, dest_file);
                            } catch (IOException e) {
                                if (DEBUG) Log.d(TAG,  "Failed copying " + SDCARD_RECOVERY_PACKAGES[i]);
                                Toast.makeText(mContext, "Failed copying " + SDCARD_RECOVERY_PACKAGES[i], Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                                return;
                            }
                            Utils.setProp("sys.update_package.ota", dest_path);
                            Utils.setProp("sys.update_package.map", CACHE_RECOVERY_MAP[i]);
                            Utils.setProp("ctl.start", "uncrypt_custom");

                            // Wait for the uncrypt service to have completed this step
                            while (!(Utils.getProp(INIT_SERVICE_UNCRYPT).contains("stopped"))) {
                            }
                            command_string += RECOVERY_PACKAGES_MAP[i];
                        } else {
                            // Magisk or gapps
                            try {
                                File dest_file = new File(CACHE_RECOVERY[i]);
                                if (dest_file.exists()) dest_file.delete();
                                Toast.makeText(mContext, "Copying files...", Toast.LENGTH_LONG).show();
                                copy(origin_file, dest_file);
                            } catch (IOException e) {
                                if (DEBUG) Log.d(TAG,  "Failed copying " + CACHE_RECOVERY[i]);
                                Toast.makeText(mContext, "Failed copying " + CACHE_RECOVERY[i], Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                                return;
                            }
                            command_string += RECOVERY_PACKAGES[i];
                        }
                    }
                    reboot = true;
                }

                if (reboot) {
                    Utils.writeToFile("/cache/recovery/command", command_string, mContext);
                    PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    pm.reboot("recovery-update");
                }
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
