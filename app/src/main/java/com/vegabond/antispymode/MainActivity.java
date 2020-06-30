package com.vegabond.antispymode;

import android.Manifest;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import static com.vegabond.antispymode.firstRunSetup.SettingFirstRunSetup;
import android.content.DialogInterface;

import com.vegabond.antispymode.FaceRecognition_Module.training_Main;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION = 2084;

    public static SettingsUtility.SettingsControl settingControl;
    static boolean stop = false;
    Thread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayHint();

        SettingFirstRunSetup(getApplicationContext(),this);

        settingControl = SettingsUtility.getOnDisplayControlSettings(getApplicationContext());

        if (settingControl.getKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        Thread displaySettingsThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1 * 1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                settingControl = SettingsUtility.getOnDisplayControlSettings(getApplicationContext());
                            }
                        });

                    }
                } catch (InterruptedException e) {
                }
            }
        };
        displaySettingsThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            askPermission();
        }

        findViewById(R.id.buttonCreateWidget).setOnClickListener(this);

        final Button btnTraining = findViewById(R.id.buttonTraining);
        if (settingControl.getFaceRecognitionMode()){
            btnTraining.setVisibility(View.VISIBLE);
        }else{
            btnTraining.setVisibility(View.INVISIBLE);
        }

        btnTraining.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, training_Main.class));
            }
        });

        findViewById(R.id.buttonSetting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,SettingsActivity.class));
            }
        });

        findViewById(R.id.buttonRefreshCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop = true;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
//                        stopService(new Intent(MainActivity.this,CameraViewService.class));
                        startService(new Intent(MainActivity.this, CameraViewService.class));
                        finish();
                        stop = false;
                    }
                }, 5000);   //5 seconds

            }
        });

        findViewById(R.id.buttonCloseCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop = true;

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        stop = false;
                    }
                }, 5000);   //5 seconds

            }
        });

        findViewById(R.id.buttonTutorial).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(),"Comming Soon...",Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION);
    }

    @Override
    public void onClick(View v) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            startService(new Intent(MainActivity.this, CameraViewService.class));
            finish();
        } else if (Settings.canDrawOverlays(this)) {
            startService(new Intent(MainActivity.this, CameraViewService.class));
            finish();
        } else {
            askPermission();
            Toast.makeText(this, "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        Log.d("TAG", "onStart");
        super.onStart();

        int camera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int writeStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (writeStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (readStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            requestPermissions(listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 101);
        } else {

        }

    }

    public void displayHint(){
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
//        adb.setView(alertDialogView);
        adb.setTitle("Special Hint");
        adb.setMessage("One Left D is Extra Life\nUse when Needed");
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Got Extra Life :)",
                        Toast.LENGTH_SHORT).show();
            }
        });
        adb.show();
    }
}