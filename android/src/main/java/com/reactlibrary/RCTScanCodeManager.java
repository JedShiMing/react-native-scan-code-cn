package com.reactlibrary;

import android.app.Activity;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;

public class RCTScanCodeManager extends SimpleViewManager<CaptureActivity> {

    public static final String REACT_CLASS = "RNScanCode";
    private float density;
    CaptureActivity cap;

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @NonNull
    @Override
    protected CaptureActivity createViewInstance(@NonNull ThemedReactContext context) {
        Activity activity = context.getCurrentActivity();
        density = activity.getResources().getDisplayMetrics().density;
        cap = new CaptureActivity(activity, context);
        return cap;
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }
}
