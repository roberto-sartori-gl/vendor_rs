package org.robertogl.settingsextra;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NfcPreference extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
        // We need to kill this activity immediately, or sometimes we'll see this activity UI instead of the NFC settings
        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
