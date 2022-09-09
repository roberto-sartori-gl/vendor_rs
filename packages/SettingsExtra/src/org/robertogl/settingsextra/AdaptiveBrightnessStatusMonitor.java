package org.robertogl.settingsextra;

import android.content.ComponentName;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

public class AdaptiveBrightnessStatusMonitor {

    private final static String TAG = "AutoBrightnessMonitorTile";

    private final static boolean DEBUG = MainService.DEBUG;

    private Context mContext;

    private final Uri BRIGHTNESS_MODE_URI = Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE);

    private final ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (DEBUG) Log.d(TAG, "BRIGHTNESS_MODE changed");
            AdaptiveBrightnessTile.requestListeningState(mContext, new ComponentName(mContext, AdaptiveBrightnessTile.class));
        }
    };

    protected void onStartup(Context context) {
        if (DEBUG) Log.d(TAG, "Starting");
        mContext = context;
        // Listen for Auto brightness events (ON/OFF)
        mContext.getContentResolver().registerContentObserver(BRIGHTNESS_MODE_URI, true, mObserver);
    }

    protected void onClose() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

}