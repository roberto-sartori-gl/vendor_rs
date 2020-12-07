package org.robertogl.settingsextra;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceFragmentCompat;

import android.util.Log;

public class SettingsExtraActivity extends AppCompatActivity {

    private static String TAG = "SettingsExtraActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_setting, new SettingsExtraFragment())
                .commit();
    }

}
