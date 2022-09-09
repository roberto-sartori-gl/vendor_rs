package org.robertogl.settingsextra;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;

public class VibrationExtra {

    private static String TAG = "VibrationExtra";

    private static boolean DEBUG = MainService.DEBUG;

    private String m_Text = "";

    protected void showVibrationOptions(Context mContext) {
        Context deviceProtectedContext = mContext.createDeviceProtectedStorageContext();
        SharedPreferences pref = deviceProtectedContext.getSharedPreferences(mContext.getPackageName() + "_preferences", MODE_PRIVATE);

        String vibrationIntensityFloat = pref.getString(Utils.vibrationIntensityString, "58");

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Vibration Intensity (0 - 100)");

        // Set up the input
        final EditText input = new EditText(mContext);
        input.setText(vibrationIntensityFloat);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
                if (m_Text.matches("-?\\d+") && Integer.valueOf(m_Text) >=0 && Integer.valueOf(m_Text) <=100) {
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString(Utils.vibrationIntensityString, m_Text);
                    editor.commit();
                    Utils.setVibrationIntensity(m_Text, mContext);
                } else {
                    Toast.makeText(mContext, "Set an integer between 0 and 100", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
