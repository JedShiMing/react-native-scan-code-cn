package com.reactlibrary.view.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.google.zxing.BarcodeFormat;
import com.reactlibrary.R;
import com.reactlibrary.decoding.EncodeHandler;
import com.reactlibrary.util.ImageUtil;
import com.reactlibrary.util.RNScanCodeHelper;
import com.reactlibrary.util.TouchEventUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static android.content.Context.CAMERA_SERVICE;

/**
 * camera -> TextureView
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2View extends FrameLayout {
    private static final String TAG = Camera2View.class.getCanonicalName();
    private final Activity activity;
    private final ReactContext context;
    // 摄像头ID
    private String mCameraId;
    // 相机设备实例
    private CameraDevice mCameraDevice;
    // 摄像头管理类
    private CameraManager mCameraManager;
    // 预览,可以获取拍摄的图片信息，可以通过它的setRepeatingRequest()方法来控制预览界面
    private static CameraCaptureSession mCameraCaptureSession;
    // 配置CameraRequest
    private static CaptureRequest.Builder mPreviewRequestBuilder;
    private AutoFitTextureView mTextureView;
    private ImageReader mImageReader;
    private CameraCharacteristics characteristics;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Size mPreviewSize;
    private float maxZoom;

    EncodeHandler encodeHandler = new EncodeHandler();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Camera2View(Activity activity, @NonNull ReactContext context) {
        super(context);
        this.activity = activity;
        this.context = context;
        LayoutInflater.from(context).inflate(R.layout.camera2view, this);

        RNScanCodeHelper.setView(this);

        mTextureView = (AutoFitTextureView) findViewById(R.id.camera2_textureView);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    /**
     * 生命周期-onResume
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initCamera();
        startBackgroundThread();
    }

    /**
     * 生命周期-onDestroy
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closeCamera();
    }

    private void closeCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    // 初始化相机
    private void initCamera() {
        mCameraManager = (CameraManager) this.activity.getSystemService(CAMERA_SERVICE);
        mCameraId = getBackCameraId();
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        maxZoom = (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)) * 10;
    }

    private void startBackgroundThread() {
        mHandlerThread = new HandlerThread("camera-background");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    // 获取后置摄像头ID
    private String getBackCameraId() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                int cameraFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return null;
    }

    // 画布渲染回调
    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            try {
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCameraManager.openCamera(mCameraId, mCameraDeviceStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: -----------");
            int orientation = Camera2View.this.getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
//            Log.d(TAG, "onSurfaceTextureUpdated: -----------" + surface);
        }
    };

    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "相机已打开");
            //打开摄像头
            mCameraDevice = cameraDevice;
            setupImageReader();
            try {
                SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
                surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(surfaceTexture);

                mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // 自动对焦
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //设置自动曝光帧率范围
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, getRange());
                // 是否控制锁住AE当前的值
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, false);
                // 加载画布
                mPreviewRequestBuilder.addTarget(surface);
                mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), mSessionStateCallback, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "相机已关闭");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.d(TAG, "相机打开错误");
        }
    };

    private void setupImageReader() {
        //前三个参数分别是需要的尺寸和格式，最后一个参数代表每次最多获取几帧数据
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 3);
        //监听ImageReader的事件，当有图像流数据可用时会回调onImageAvailable方法，它的参数就是预览帧数据，可以对这帧数据进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try (Image image = reader.acquireNextImage()) {
                    Image.Plane[] planes = image.getPlanes();
                    if (planes.length > 0) {
                        ByteBuffer buffer = planes[0].getBuffer();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        // 识别光源
                        ImageUtil.recognitionLight(data, image.getWidth(), image.getHeight());
                        // 扫码
                        encodeHandler.encode(data, image.getWidth(), image.getHeight());
                    }
                }
            }
        }, null);
    }

    // session回调
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: session回调");
            mCameraCaptureSession = session;
            try {
                mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    };

    //选择sizeMap中大于并且最接近width和height的size
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum(lhs.getWidth() * lhs.getHeight() - rhs.getWidth() * rhs.getHeight());
                }
            });
        }
        return sizeMap[0];
    }

    private Range<Integer> getRange() {
//        CameraManager mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics chars = null;
        try {
            chars = mCameraManager.getCameraCharacteristics(mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Range<Integer>[] ranges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);

        Range<Integer> result = null;

        for (Range<Integer> range : ranges) {
            //帧率不能太低，大于10
            if (range.getLower() < 10)
                continue;
            if (result == null)
                result = range;
                //FPS下限小于15，弱光时能保证足够曝光时间，提高亮度。range范围跨度越大越好，光源足够时FPS较高，预览更流畅，光源不够时FPS较低，亮度更好。
            else if (range.getLower() <= 15 && (range.getUpper() - range.getLower()) > (result.getUpper() - result.getLower()))
                result = range;
        }
        return result;
    }

    public int zoom_level = 1;
    private float mOldDist = 1f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 双指缩放
        if (event.getPointerCount() == 2) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    mOldDist = TouchEventUtil.calculateFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = TouchEventUtil.calculateFingerSpacing(event);
//                    Log.d(TAG, "onTouchEvent: newDist = " + newDist + " : mOldDist = " + mOldDist + " abs " + Math.abs(newDist - mOldDist));
                    if (Math.abs(newDist - mOldDist) > 5) {
//                        Log.d(TAG, "onTouchEvent: newDist = " + newDist + " : mOldDist = " + mOldDist + " abs " + Math.abs(newDist - mOldDist));
                        if (newDist > mOldDist) {
//                            Log.d(TAG, "放大");
                            handleZoom(true);
                        } else {
//                            Log.d(TAG, "缩小");
                            handleZoom(false);
                        }
                        mOldDist = newDist;
                    }
                    break;
            }
        }
        return true;
    }

    private void handleZoom(boolean b) {
        Rect m = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if (b) {
            if (zoom_level + 1 < maxZoom / 2) {
                zoom_level++;
            } else {
                return;
            }
        } else {
            if (zoom_level - 2 > 0) {
                zoom_level--;
            } else {
                return;
            }
        }
//        Log.d(TAG, "handleZoom: " + zoom_level);
        int minW = (int) (m.width() / maxZoom);
        int minH = (int) (m.height() / maxZoom);
        int difW = m.width() - minW;
        int difH = m.height() - minH;
        int cropW = difW / 100 * (int) zoom_level;
        int cropH = difH / 100 * (int) zoom_level;
        cropW -= cropW & 3;
        cropH -= cropH & 3;
        Rect zoom = new Rect(cropW, cropH, m.width() - cropW, m.height() - cropH);
        mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 设置支持的扫码类型
    public void setCodeTypes(ReadableArray codeTypes) {
        if (codeTypes == null || codeTypes.size() == 0) {
            encodeHandler.setCodeType(null);
            return;
        }
        Vector<BarcodeFormat> result = new Vector<BarcodeFormat>(codeTypes.size());
        for (int i = 0; i < codeTypes.size(); i++) {
            BarcodeFormat format = BarcodeFormat.valueOf(codeTypes.getString(i));
            result.add(format);
        }
        encodeHandler.setCodeType(result);
    }

    // 设置闪光灯
    public static void setFlashlight(boolean isFlash) {
        Log.d(TAG, "设置闪光灯: " + isFlash);
        if (!isFlash) {
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
        }
        try {
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
