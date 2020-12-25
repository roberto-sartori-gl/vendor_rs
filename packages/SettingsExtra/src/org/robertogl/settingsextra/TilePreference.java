package org.robertogl.settingsextra;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import static android.app.PendingIntent.getActivity;

public class TilePreference extends Activity {

    private static String TAG = "TilePreference";

    private static boolean DEBUG = MainService.DEBUG;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        ComponentName qsTile = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);

        if (DEBUG) Log.d(TAG, "class name: " + qsTile.getClassName());
        if (qsTile.getClassName().equals("org.robertogl.settingsextra.NfcTile")) {
            startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
        } else if (qsTile.getClassName().equals("org.robertogl.settingsextra.AdaptiveBrightnessTile")) {
            startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));
        }
        // We need to kill this activity immediately, or sometimes we'll see this activity UI instead of the settings
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
