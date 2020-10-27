/*
 * Copyright (c) 2016 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.robertogl.keyhandler;

import android.content.BroadcastReceiver;
import android.content.Context;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.util.Log;
import android.provider.Settings;
import android.content.ContentResolver;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "KeyHandler";

    private boolean isThisFirstBoot = true;

    public boolean isAccessServiceEnabled(Context context, String accessibilityServiceClass)
    {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return prefString!= null && prefString.contains(context.getPackageName() + "/" + accessibilityServiceClass);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "Starting");
	SharedPreferences pref = context.getSharedPreferences("KeyHandlerPref", MODE_PRIVATE);
	Editor editor = pref.edit();

	isThisFirstBoot = pref.getBoolean("isThisFirstBoot", true);

	if (isThisFirstBoot) {
		Log.d(TAG, "Startingfirsttime");
		editor.putBoolean("isThisFirstBoot", false);
		editor.commit();
		Settings.Secure.putString(context.getContentResolver(),
    			Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "org.robertogl.keyhandler/org.robertogl.keyhandler.KeyHandler");
		Settings.Secure.putString(context.getContentResolver(),
    			Settings.Secure.ACCESSIBILITY_ENABLED, "1");
	}

	if (isAccessServiceEnabled(context,"org.robertogl.keyhandler.KeyHandler")) {
		Log.d(TAG, "Key enabled");
	}
    }
}
