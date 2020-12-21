package com.reactlibrary.decoding;

import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.reactlibrary.util.RNScanCodeHelper;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

public class EncodeHandler {
    private static final String TAG = EncodeHandler.class.getCanonicalName();

    private Map<DecodeHintType, Object> hints;
    private MultiFormatReader multiFormatReader;
    private boolean isRes = false;

    public EncodeHandler() {
        hints = new Hashtable<DecodeHintType, Object>(3);
        multiFormatReader = new MultiFormatReader();
    }

    /**
     * 设置扫码类型,不传默认识别所有
     *
     * @param types
     */
    public void setCodeType(Vector<BarcodeFormat> types) {
        Collection<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
        if (types == null) {
            decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        } else {
            decodeFormats = types;
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        multiFormatReader.setHints(hints);
    }

    /**
     * 识别
     *
     * @param data
     * @param width
     * @param height
     */
    public void encode(byte[] data, int width, int height) {
        if (isRes) {
            return;
        }
        Result result = null;
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }
        // 这里要交换下,否则会识别失败
        int tmp = width;
        width = height;
        height = tmp;

        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(rotatedData, width, height, 0, 0, width, height, false);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            result = multiFormatReader.decodeWithState(bitmap);
            Log.d(TAG, "扫码结果 = " + result.getText() + " : " + result.getBarcodeFormat());
            RNScanCodeHelper.emitScanCodeResultEvent(result.getText(), result.getBarcodeFormat());
            isRes = true;
        } catch (ReaderException re) {
//            Log.d(TAG, "扫了个寂寞");
        } finally {
            multiFormatReader.reset();
        }
    }
}
