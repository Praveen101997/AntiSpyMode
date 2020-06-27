package com.vegabond.antispymode;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import static com.vegabond.antispymode.CameraViewService.noFaceDetected;
import static com.vegabond.antispymode.CameraViewService.timer;
import static com.vegabond.antispymode.MainActivity.stop;


public class WarningView extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    Thread thread;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startBackgroundThread();

        thread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!thread.isInterrupted()) {
                        Thread.sleep(1000);
                        Log.d("Check77","In run NoFace Detected :"+ noFaceDetected);
                        if (!noFaceDetected){
                            Log.d("Check77","In IF NoFace Detected :"+ noFaceDetected);
//                            stopSelf();
//                            break;
                        }else{
                            Log.d("Check77","In else NoFace Detected :"+ noFaceDetected);
                        }
                        if (stop==true){
                            thread.interrupt();
                            stopSelf();
                        }


                    }
                } catch (InterruptedException e) {
                }
            }
        };

        thread.start();

        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_warning_view, null);

        //setting the layout parameters
        final WindowManager.LayoutParams params;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }else{
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }


        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        mFloatingView.findViewById(R.id.collapsed_iv).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timer = 0;
                stopSelf();
            }
        });




    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }
}
