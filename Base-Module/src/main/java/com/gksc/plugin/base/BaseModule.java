package com.gksc.plugin.base;

import android.widget.Toast;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;

public class BaseModule extends UniModule{

    private static final String TAG = "FloatUniModule";

    @UniJSMethod
    public void test(){
        showToast();;
    }

    private void showToast() {
        Toast.makeText(mUniSDKInstance.getContext(), "lininghahh原生插件调用成功", Toast.LENGTH_LONG).show();
    }
}