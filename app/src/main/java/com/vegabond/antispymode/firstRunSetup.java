package com.vegabond.antispymode;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import static android.content.Context.MODE_PRIVATE;

public class firstRunSetup {

    public static boolean checkFirstRunSettings(final Activity activity, String preferenceTag) {
        boolean isFirstRun = activity.getSharedPreferences("PREFERENCE", MODE_PRIVATE).getBoolean(preferenceTag, true);
        activity.getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .edit()
                .putBoolean(preferenceTag, false)
                .apply();
        return isFirstRun;
    }

    public static void SettingFirstRunSetup(Context mContext, Activity mActivity){
        if (checkFirstRunSettings(mActivity,"CameraSettingFirstRun")){
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            SharedPreferences.Editor preferenceEditor = sharedPreferences.edit();

            preferenceEditor.putBoolean("pref_face_recognition_mode",true);
            preferenceEditor.putBoolean("pref_display_widget",true);
            preferenceEditor.putBoolean("pref_keep_screen_on",true);

            preferenceEditor.putString("pref_no_of_faces","1");
            preferenceEditor.putString("pref_warning_no_face","10");
            preferenceEditor.putString("pref_warning_number_of_face","2");
            preferenceEditor.putString("pref_camera_location","1");
            preferenceEditor.putString("pref_preview_size","10");

            preferenceEditor.apply();
            preferenceEditor.commit();
            Toast.makeText(mContext, "Settings Saved Successfully",Toast.LENGTH_SHORT).show();

        }
    }
}
