package com.vegabond.antispymode;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.util.Arrays;

public class CameraViewService extends Service implements View.OnClickListener {

    static boolean noFaceDetected = false;


    private WindowManager mWindowManager;
    private View mFloatingView;
    private View collapsedView;
    private View expandedView;

    private TextureView cameraView;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    Thread thread;
    static boolean spying = false;
    static boolean spyingWindowDisplay = false;

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static int timer = 0;



    public CameraViewService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mFaceDetectionMatrix = new Matrix();
        mFaceDetectionMatrix.setRotate(90);
        startBackgroundThread();

//        new Thread( new Runnable() {
//            @Override public void run() {
//            // Run whatever background code you want here.
//                if (noFaceDetected){
//                    timer++;
//                    if (timer==10){
//                        startService(new Intent(CameraViewService.this, WarningView.class));
//                    }
//                }else{
//                    timer = 0;
//
//                }
//
//            }
//        } ).start();

        thread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!thread.isInterrupted()) {
                        Thread.sleep(1000);
                        if (noFaceDetected){
                            timer++;
                            if (timer==10){
                                startService(new Intent(CameraViewService.this, WarningView.class));
                            }
                        }else{
                            timer = 0;
                        }
                        if (spying==true){
                            if (spyingWindowDisplay==false){
                                spyingWindowDisplay = true;
                                startService(new Intent(CameraViewService.this, SpyingWarningView.class));
                            }
                        }else{
//                            spying = true;
                        }

                    }
                } catch (InterruptedException e) {
                }
            }
        };

        thread.start();


        //getting the widget layout from xml using layout inflater
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_camera_view, null);

        //setting the layout parameters
        final WindowManager.LayoutParams params;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
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


        //getting the collapsed and expanded view from the floating view
        collapsedView = mFloatingView.findViewById(R.id.layoutCollapsed);
        expandedView = mFloatingView.findViewById(R.id.layoutExpanded);

        //adding click listener to close button and expanded view
        mFloatingView.findViewById(R.id.buttonClose).setOnClickListener(this);
        expandedView.setOnClickListener(this);

        //adding an touchlistener to make drag movement of the floating widget
        mFloatingView.findViewById(R.id.relativeLayoutParent).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_UP:
                        //when the drag is ended switching the state of the widget
                        collapsedView.setVisibility(View.GONE);
                        expandedView.setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });

        cameraView = (TextureView) mFloatingView.findViewById(R.id.CameraTextureView);
        mOverlayView = (OverlayView) mFloatingView.findViewById(R.id.overlay_view);
        cameraView.setSurfaceTextureListener(textureListener);

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e("TAG", "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = cameraView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    try {
                        // Auto focus should be continuous for camera preview.
                        captureRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                                CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);
                        // Flash is automatically enabled when necessary.
//                        setAutoFlash(captureRequestBuilder);

                        // Finally, we start displaying the camera preview.
                        captureRequest = captureRequestBuilder.build();
                        cameraCaptureSession.setRepeatingRequest(captureRequest,
                                mCaptureCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(CameraViewService.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e("TAG", "is camera open");
        try {
            cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e("TAG", "openCamera X");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e("TAG", "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }




    @Override
    public void onDestroy() {
        super.onDestroy();
        closeCamera();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layoutExpanded:
                //switching views
                collapsedView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);
                break;

            case R.id.buttonClose:
                //closing the widget
                stopSelf();
                break;
        }
    }


    private OverlayView mOverlayView;
    private Matrix mFaceDetectionMatrix;
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            Log.d("Test","Mode : "+mode+" | Faces : ");
            if (faces!=null){
                if (faces.length==0){
                    noFaceDetected = true;
//                    startService(new Intent(CameraViewService.this, WarningView.class));
                }else{
                    noFaceDetected = false;
//                    stopService(new Intent(CameraViewService.this,WarningView.class));
                }
                Log.d("Test","Faces : "+faces.length);
                if (faces.length>1){
                    if (spying==true){
                        Toast.makeText(getApplicationContext(),"You are not alone",Toast.LENGTH_SHORT).show();
//                        startService(new Intent(CameraViewService.this, SpyingWarningView.class));
                    }else{
                        spying = true;
                    }

                }else{
                   spying = false;
                }
            }

            if (faces != null && mode != null) {
                if (faces.length > 0) {
                    for(int i = 0; i < faces.length; i++) {
                        if (faces[i].getScore() > 50) {
                            Log.d("Test", "faces : " + faces.length + " , mode : " + mode);
                            int left = faces[i].getBounds().left;
                            int top = faces[i].getBounds().top;
                            int right = faces[i].getBounds().right;
                            int bottom = faces[i].getBounds().bottom;
                            //float points[] = {(float)left, (float)top, (float)right, (float)bottom};

                            Rect uRect = new Rect(left, top, right, bottom);
                            RectF rectF = new RectF(uRect);
                            mFaceDetectionMatrix.mapRect(rectF);
                            //mFaceDetectionMatrix.mapPoints(points);
                            rectF.round(uRect);
                            //uRect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
                            Log.i("Test", "Activity rect" + i + " bounds: " + uRect);

                            final Rect rect = uRect;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    mOverlayView.setRect(rect);
                                    mOverlayView.requestLayout();
                                }
                            });
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };
}