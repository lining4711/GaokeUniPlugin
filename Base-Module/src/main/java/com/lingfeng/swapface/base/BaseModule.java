package com.lingfeng.swapface.base;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class BaseModule extends UniModule{

    private static final String TAG = "FloatUniModule";
    private ProductDetails cachedProduct;
    private BillingManager billingManager = null;

    @UniJSMethod
    public void test(){
        showToast();
    }

    private void showToast() {
        Toast.makeText(mUniSDKInstance.getContext(), PayUtils.callNative(), Toast.LENGTH_LONG).show();
    }

    @UniJSMethod
    public void initClient(final UniJSCallback callback) {
        billingManager = new BillingManager(mUniSDKInstance.getContext(), new BillingManager.BillingCallback() {
            @Override
            public void onConnected() {
//                appendLog("Billing Service 已连接 ✅");
                // 构造返回数据
                JSONObject result = new JSONObject();
                try {
                    result.put("taskId", "onConnected");
                    result.put("data", "Billing Service 已连接 ✅" );
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
            }

            @Override
            public void onDisconnected() {
//                appendLog("Billing Service 已断开 ❌");
            }

            @Override
            public void onProductDetails(List<ProductDetails> products, List<ProductDetails> unfetched) {
//                appendLog("查询到商品数量: " + products.size());
                if (!products.isEmpty()) {
                    cachedProduct = products.get(0);
//                    appendLog("商品ID: " + cachedProduct.getProductId());
//                    appendLog("商品信息：" + cachedProduct.getDescription());
                    JSONObject result = new JSONObject();
                    try {
                        result.put("taskId", "onProductDetails");
                        result.put("data", "商品ID: " + cachedProduct.getProductId() + "商品信息：" + cachedProduct.getDescription());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // 回到主线程回调给 UniApp
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.invoke(result);
                            }
                        }
                    });
                }
                if (!unfetched.isEmpty()) {
//                    appendLog("未抓取商品: " + unfetched.size());
                }
            }

            @Override
            public void onPurchaseSuccess(Purchase purchase) {
//                appendLog("购买成功 ✅: " + purchase.getProducts());
                // 一次性商品必须消耗，否则不能再次购买
                billingManager.consumePurchase(purchase.getPurchaseToken());
            }

            @Override
            public void onPurchaseFailure(com.android.billingclient.api.BillingResult result) {
//                appendLog("购买失败 ❌: " + result.getDebugMessage());
            }

            @Override
            public void onConsumeSuccess(String purchaseToken) {
//                appendLog("消耗成功 ✅, token=" + purchaseToken);
            }
        });

        billingManager.startConnection();
    }

    @UniJSMethod
    public void queryProduct(String productId, @BillingClient.ProductType String type) {
        billingManager.queryProducts(Arrays.asList(productId), type);
    }

    @UniJSMethod
    public void queryProducts(List<String> productIds, @BillingClient.ProductType String type) {
        billingManager.queryProducts(productIds, type);
    }

    // 异步方法（携带业务ID）
    @UniJSMethod(uiThread = false)
    public void startTask(final String taskId, final UniJSCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 模拟耗时任务
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 构造返回数据
                JSONObject result = new JSONObject();
                try {
                    result.put("taskId", taskId);
                    result.put("data", "这是Java异步返回的数据，业务ID=" + taskId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.invoke(result);
                        }
                    }
                });
            }
        }).start();
    }
}