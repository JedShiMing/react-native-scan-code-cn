package com.reactlibrary;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.google.zxing.BarcodeFormat;
import com.reactlibrary.view.camera2.Camera2View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class RCTScanCodeManager extends SimpleViewManager<Camera2View> {

    private static final String TAG = "RCTScanCodeManager";

    // 事件名,这里写个enum方便循环
    public enum Events {
        //        EVENT_CODE_TYPES("codeTypes"),
        EVENT_ON_BAR_CODE_READ("onBarCodeRead"),
        EVENT_ON_LIGHT_BRIGHT("onLightBright");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    public static final String REACT_CLASS = "RNScanCode";

    /**
     * 设置别名
     *
     * @return
     */
    @Override
    public String getName() {
        return REACT_CLASS;
    }

    /**
     * 初始化入口
     *
     * @param context
     * @return
     */
    @NonNull
    @Override
    protected Camera2View createViewInstance(@NonNull ThemedReactContext context) {
        Activity activity = context.getCurrentActivity();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Camera2View cap = new Camera2View(activity, context);
            return cap;
        }
        return null;
    }

    /**
     * 注册事件
     *
     * @return
     */
    @Override
    @Nullable
    public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    /**
     * 设置属性,参数要加@Nullable,否则会报错
     * @param captureView
     * @param codeTypes
     */
    @ReactProp(name = "codeTypes")
    public void setCodeTypes(Camera2View captureView, @Nullable ReadableArray codeTypes) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            captureView.setCodeTypes(codeTypes);
        }
    }

}
