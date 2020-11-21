package org.robertogl.settingsextra;

import android.app.Service;
import android.content.Context;
import android.media.AudioManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.util.Log;
import android.content.Intent;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;


public final class Utils {

   protected static String keySwapNode = "/proc/s1302/key_rep";

   protected static String displayModeSRGBNode = "/sys/devices/virtual/graphics/fb0/srgb";

   protected static String displayModeDCIP3Node = "/sys/devices/virtual/graphics/fb0/dci_p3";

   protected static String flickerFreeNode = "/proc/flicker_free/flicker_free";

   protected static String flickerFreeMinBrightness = "/proc/flicker_free/flicker_free";

   protected static String readFromFile(String path) {
        String aBuffer = "";
        try  {
             File myFile = new File(path);
             FileInputStream fIn = new FileInputStream(myFile);
             BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
             String aDataRow = "";
             while ((aDataRow = myReader.readLine()) != null) {
                     aBuffer += aDataRow;
             }
             myReader.close();
        } catch (FileNotFoundException e) {
             e.printStackTrace();
        } catch (IOException e) {
             e.printStackTrace();
        }
        return aBuffer;
    }

    protected static void writeToFile(String path, String data, Context context) {
        try  {
	     OutputStreamWriter outputStreamWriter =  new OutputStreamWriter(new FileOutputStream(new File(path)));

             //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(path, Context.MODE_PRIVATE));
             outputStreamWriter.write(data);
             outputStreamWriter.close();
             }
        catch (IOException e) {
             Log.e("Exception", "File write failed: " + e.toString());
             }
    }

    protected static void doHapticFeedback(Vibrator mVibrator, int msVibrationLenght) {
        if (mVibrator != null && mVibrator.hasVibrator()) {
            mVibrator.vibrate(VibrationEffect.createOneShot(msVibrationLenght,
                    VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    protected static boolean isScreenOn(Context mContext) {
        DisplayManager dm = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        for (Display display : dm.getDisplays()) {
            if (display.getState() != Display.STATE_OFF) {
                return true;
            }
        }
        return false;
    }

    protected static void setProp(String property, String value) {
        Process sh = null;
        String[] cmd = {"setprop", property, value};
        try {
             sh = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
             e.printStackTrace();
        }
        try {
            sh.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
