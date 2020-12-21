package com.reactlibrary;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.google.zxing.BarcodeFormat;
import com.reactlibrary.view.camera2.Camera2View;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RCTScanCodeModule extends ReactContextBaseJavaModule {
    private static ReactApplicationContext _reactContext;

    @NonNull
    @Override
    public String getName() {
        return "RCTScanCodeModule";
    }

    public RCTScanCodeModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }

    public static ReactApplicationContext getReactContextSingleton() {
        return _reactContext;
    }

    /**
     * 设置闪光灯
     * @param onFlash
     */
    @ReactMethod
    public void setFlashlight(boolean onFlash) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Camera2View.setFlashlight(onFlash);
        }
    }

    /**
     * 导出原生属性,供js调用
     * @return
     */
    @Nullable
    @Override
    public Map<String, Object> getConstants() {
        return Collections.unmodifiableMap(new HashMap<String, Object>() {
            {
                put("CodeType", getCodeConstants());
            }
            private Map<String, Object> getCodeConstants() {
                return Collections.unmodifiableMap(new HashMap<String, Object>() {
                    {
                        put("aztec", BarcodeFormat.AZTEC.toString());
                        put("ean13", BarcodeFormat.EAN_13.toString());
                        put("ean8", BarcodeFormat.EAN_8.toString());
                        put("qr", BarcodeFormat.QR_CODE.toString());
                        put("pdf417", BarcodeFormat.PDF_417.toString());
                        put("upc_e", BarcodeFormat.UPC_E.toString());
                        put("datamatrix", BarcodeFormat.DATA_MATRIX.toString());
                        put("code39", BarcodeFormat.CODE_39.toString());
                        put("code93", BarcodeFormat.CODE_93.toString());
                        put("interleaved2of5", BarcodeFormat.ITF.toString());
                        put("codabar", BarcodeFormat.CODABAR.toString());
                        put("code128", BarcodeFormat.CODE_128.toString());
                        put("maxicode", BarcodeFormat.MAXICODE.toString());
                        put("rss14", BarcodeFormat.RSS_14.toString());
                        put("rssexpanded", BarcodeFormat.RSS_EXPANDED.toString());
                        put("upc_a", BarcodeFormat.UPC_A.toString());
                        put("upc_ean", BarcodeFormat.UPC_EAN_EXTENSION.toString());
                    }
                });
            }
        });
    }
}
