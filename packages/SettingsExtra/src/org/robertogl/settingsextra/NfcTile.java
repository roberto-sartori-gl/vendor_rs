package org.robertogl.settingsextra;

import android.content.Context;
import android.nfc.NfcAdapter;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.Thread;

public class NfcTile extends TileService {

    private static String TAG = "NfcTile";

    private static boolean DEBUG = MainService.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    private void updateTile() {
        int i = 0;
        int max = 30;
        Tile qsTile = getQsTile();
        NfcAdapter mNfcAdapter = null;

        while (mNfcAdapter == null) {
            try {
                mNfcAdapter = NfcAdapter.getNfcAdapter(this);
                break;
            } catch (UnsupportedOperationException e) {
                i++;
            }
            if (DEBUG) Log.d(TAG, "Waiting for the NfcAdapter to be online...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (i > max) return;
        }
        if (!mNfcAdapter.isEnabled()) {
            // NFC is disabled
            qsTile.setState(Tile.STATE_INACTIVE);
        } else {
            // NFC is enabled
            qsTile.setState(Tile.STATE_ACTIVE);
        }
        qsTile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        // We have access to hidden APIs, so just enable/disable the NFC with the NfcAdapter class
        NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        boolean isActive = (tile.getState() == Tile.STATE_ACTIVE);
        if (isActive) {
            tile.setState(Tile.STATE_INACTIVE);
            mNfcAdapter.disable();
        } else {
            tile.setState(Tile.STATE_ACTIVE);
            mNfcAdapter.enable();
        }
        tile.updateTile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

