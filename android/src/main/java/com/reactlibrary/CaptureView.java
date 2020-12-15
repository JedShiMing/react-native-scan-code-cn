package com.reactlibrary;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.reactlibrary.camera.CameraManager;
import com.reactlibrary.decoding.CaptureActivityHandler;
import com.reactlibrary.decoding.InactivityTimer;
import com.reactlibrary.util.RNScanCodeHelper;
import com.reactlibrary.util.TouchEventUtil;

import java.io.IOException;
import java.util.Vector;


/**
 * 扫码主页
 */
public class CaptureView extends FrameLayout implements Callback, LifecycleEventListener {
    private static final String TAG = "CaptureView";
    private CaptureActivityHandler handler;
    private Activity activity;
    private boolean hasSurface;
    // 支持的扫码类型,不赋值支持所有类型
    private Vector<BarcodeFormat> decodeFormats;
    // 指定字符串的编码类型
    private String characterSet;
    // 识别码线程
    private InactivityTimer inactivityTimer;
    // 两指距离
    private float mOldDist = 1f;
    private ReactContext context;

    public CaptureView(Activity activity, @NonNull ReactContext context) {
        super(context);
        this.activity = activity;
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.activity_scanner, this);
        CameraManager.init(activity.getApplication());
        RNScanCodeHelper.setView(this);
    }

    /**
     * 设置扫码类型
     *
     * @param codeTypes
     */
    public void setCodeTypes(ReadableArray codeTypes) {
        if (codeTypes == null || codeTypes.size() == 0) {
            return;
        }
        Vector<BarcodeFormat> result = new Vector<BarcodeFormat>(codeTypes.size());
        for (int i = 0; i < codeTypes.size(); i++) {
            BarcodeFormat format = BarcodeFormat.valueOf(codeTypes.getString(i));
            result.add(format);
        }
        decodeFormats = result;
    }

    /**
     * 生命周期-onResume
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        context.addLifecycleEventListener(this);
        hasSurface = false;
        inactivityTimer = new InactivityTimer(context.getCurrentActivity());
        init();
    }

    /**
     * 生命周期-onDestroy
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        context.removeLifecycleEventListener(this);
        inactivityTimer.shutdown();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    /**
     * 生命周期-onPause
     */
    public void capture_onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();
    }

    /**
     * 初始化视图
     */
    protected void init() {
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.sfv_surfaceView);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
    }

    /**
     * 初始化Camera
     *
     * @param surfaceHolder
     */
    private void initCamera(final SurfaceHolder surfaceHolder) {
        //加点延时使打开不卡顿
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(CaptureView.this, decodeFormats,
                    characterSet);
        }
    }

    /**
     * 扫描结果
     *
     * @param result
     */
    public void handleDecode(Result result) {
        inactivityTimer.onActivity();
        String resultString = result.getText();
        if (!TextUtils.isEmpty(resultString)) {
            BarcodeFormat format = (decodeFormats == null || decodeFormats.size() == 0) ? null : decodeFormats.get(0);
            RNScanCodeHelper.emitScanCodeResultEvent(resultString, format);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.d("CaptureView", "surfaceChanged surfaceChanged");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("CaptureView", "surfaceCreated surfaceCreated");
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }


    public Handler getHandler() {
        return handler;
    }


    /**
     * 设置闪光灯
     *
     * @param isFlash
     */
    public static void setFlashlight(boolean isFlash) {
        CameraManager.get().setFlashLight(isFlash);
    }

    /**
     * 重写手势
     *
     * @param event
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    mOldDist = TouchEventUtil.calculateFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = TouchEventUtil.calculateFingerSpacing(event);
                    //                    Log.d(TAG, "onTouchEvent: newDist = " + newDist + " : mOldDist = " + mOldDist);
                    if (Math.abs(newDist - mOldDist) > 3) {
                        if (newDist > mOldDist) {
                            //                        Log.d(TAG, "放大");
                            handleZoom(true, CameraManager.get().getCamera());
                        } else {
                            //                        Log.d(TAG, "缩小");
                            handleZoom(false, CameraManager.get().getCamera());
                        }
                        mOldDist = newDist;
                    }
                    break;
            }
        }
        return true;
    }

    /**
     * 缩放
     *
     * @param isZoomIn
     * @param camera
     */
    private static void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int zoom = params.getZoom();
            if (isZoomIn && zoom < params.getMaxZoom()) {
                //                Log.d(TAG, "handleZoom: 放大" + zoom);
                zoom++;
            } else if (!isZoomIn && zoom >= 2) {
                //                Log.d(TAG, "handleZoom: 缩小" + zoom);
                zoom -= 2;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        }
    }

    @Override
    public void onHostResume() {
    }

    @Override
    public void onHostPause() {
        // 扫码期间暂停,应当停止扫码进程和视频流
        capture_onPause();
    }

    @Override
    public void onHostDestroy() {
    }
}