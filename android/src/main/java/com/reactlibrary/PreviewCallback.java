package com.reactlibrary;

import android.hardware.Camera;
import android.util.Log;

public class PreviewCallback implements Camera.PreviewCallback {
//    private static final String TAG = com.example.camera.PreviewCallback.class.getCanonicalName();
    // 是否识别
    private boolean canLight;
    // 上次环境亮度记录的时间戳
    private long mLastAmbientBrightnessRecordTime = System.currentTimeMillis();
    // 上次环境亮度记录的索引
    private int mAmbientBrightnessDarkIndex = 0;
    // 环境亮度历史记录的数组，255 是代表亮度最大值
    private long[] AMBIENT_BRIGHTNESS_DARK_LIST = new long[]{255, 255, 255, 255};
    // 环境亮度扫描间隔
    private int AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME = 1000;

    public PreviewCallback(boolean isLight) {
        this.canLight = isLight;
    }

    public void setLight(boolean light) {
        this.canLight = light;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (canLight) {
            handleAmbientBrightness(bytes, camera);
        }
    }

    // 识别光源
    private void handleAmbientBrightness(byte[] data, Camera camera) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastAmbientBrightnessRecordTime < AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME) {
            return;
        }
        mLastAmbientBrightnessRecordTime = currentTime;

        int width = camera.getParameters().getPreviewSize().width;
        int height = camera.getParameters().getPreviewSize().height;
        // 像素点的总亮度
        long pixelLightCount = 0L;
        // 像素点的总数
        long pixelCount = width * height;
        // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
        int step = 10;
        // data.length - allCount * 1.5f 的目的是判断图像格式是不是 YUV420 格式，只有是这种格式才相等
        //因为 int 整形与 float 浮点直接比较会出问题，所以这么比
        if (Math.abs(data.length - pixelCount * 1.5f) < 0.00001f) {
            for (int i = 0; i < pixelCount; i += step) {
                // 如果直接加是不行的，因为 data[i] 记录的是色值并不是数值，byte 的范围是 +127 到 —128，
                // 而亮度 FFFFFF 是 11111111 是 -127，所以这里需要先转为无符号 unsigned long 参考 Byte.toUnsignedLong()
                pixelLightCount += ((long) data[i]) & 0xffL;
            }
            // 平均亮度
            long cameraLight = pixelLightCount / (pixelCount / step);
            // 更新历史记录
            int lightSize = AMBIENT_BRIGHTNESS_DARK_LIST.length;
            AMBIENT_BRIGHTNESS_DARK_LIST[mAmbientBrightnessDarkIndex = mAmbientBrightnessDarkIndex % lightSize] = cameraLight;
            mAmbientBrightnessDarkIndex++;

            Log.i("光源 ------ ", String.valueOf(cameraLight));
//            RNScanCodeHelper.emitLightBrightEvent(String.valueOf(cameraLight));
        }
    }
}
