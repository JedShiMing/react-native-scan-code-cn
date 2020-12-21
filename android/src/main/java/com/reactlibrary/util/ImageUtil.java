package com.reactlibrary.util;

import android.util.Log;

public class ImageUtil {
    private static final String TAG = "ImageUtil";

    // 上次环境亮度记录的时间戳
    private static long mLastAmbientBrightnessRecordTime = System.currentTimeMillis();
    // 上次环境亮度记录的索引
    private static int mAmbientBrightnessDarkIndex = 0;
    // 环境亮度历史记录的数组，255 是代表亮度最大值
    private static long[] AMBIENT_BRIGHTNESS_DARK_LIST = new long[]{255, 255, 255, 255};
    // 环境亮度扫描间隔
    private static int AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME = 1000;

    /**
     * 识别光源
     * @param data
     * @param width
     * @param height
     */
    public static void recognitionLight(byte[] data, int width, int height) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastAmbientBrightnessRecordTime < AMBIENT_BRIGHTNESS_WAIT_SCAN_TIME) {
            return;
        }
        mLastAmbientBrightnessRecordTime = currentTime;
        // 像素点的总亮度
        long pixelLightCount = 0L;
        // 像素点的总数
        long pixelCount = width * height;
        // 采集步长，因为没有必要每个像素点都采集，可以跨一段采集一个，减少计算负担，必须大于等于1。
        int step = 10;
        for (int i = 0; i < pixelCount; i += step) {
            pixelLightCount += ((long) data[i]) & 0xffL;
        }
        // 平均亮度
        long cameraLight = pixelLightCount / (pixelCount / step);
        // 更新历史记录
        int lightSize = AMBIENT_BRIGHTNESS_DARK_LIST.length;
        AMBIENT_BRIGHTNESS_DARK_LIST[mAmbientBrightnessDarkIndex = mAmbientBrightnessDarkIndex % lightSize] = cameraLight;
        mAmbientBrightnessDarkIndex++;

//        Log.d("光源 ------ ", String.valueOf(cameraLight));
        RNScanCodeHelper.emitLightBrightEvent(String.valueOf(cameraLight));
    }

}
