package com.lingfeng.swapface.base;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;

import java.util.Arrays;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class GooglePayModule extends UniModule{

    private static final String TAG = "FloatUniModule";
    private ProductDetails cachedProduct;
    private BillingManager billingManager = null;

    private UniJSCallback successPayCallback;

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
                PayResult result = new PayResult();
                result.setResultCode(200);
                result.setData("Billing Service 已连接");

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
                PayResult result = new PayResult();
                result.setResultCode(300);
                result.setErrorMsg("Billing Service 断开连接");
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
            public void onProductDetails(List<ProductDetails> products, List<ProductDetails> unfetched) {
//                appendLog("查询到商品数量: " + products.size());
                if (!products.isEmpty()) {
                    cachedProduct = products.get(0);
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
                // 构造返回数据
                PayResult result = new PayResult();
                result.setResultCode(200);
                result.setData("购买成功");
                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (successPayCallback != null) {
                            successPayCallback.invoke(result);
                        }
                    }
                });
            }

            @Override
            public void onPurchaseFailure(BillingResult purchaseResult) {
                PayResult result = new PayResult();
                result.setResultCode(300);
                result.setErrorMsg("购买失败");

                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (successPayCallback != null) {
                            successPayCallback.invoke(result);
                        }
                    }
                });
            }

            @Override
            public void onConsumeSuccess(String purchaseToken) {
//                JSONObject result = new JSONObject();
//                try {
//                    result.put("taskId", "onConsumeSuccess");
//                    result.put("data", "消费tocken成功，token=" + purchaseToken);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                // 回到主线程回调给 UniApp
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (successPayCallback != null) {
//                            successPayCallback.invoke(result);
//                        }
//                    }
//                });
            }
        });

        billingManager.startConnection();
    }

    @UniJSMethod
    public void queryProduct(String productId, @BillingClient.ProductType String type, final UniJSCallback callback) {
        billingManager.queryProducts(Arrays.asList(productId), type, callback);
    }

    @UniJSMethod
    public void queryProducts(List<String> productIds, @BillingClient.ProductType String type, final UniJSCallback callback) {
        billingManager.queryProducts(productIds, type, callback);
    }

    /**
     * 发起购买
     *
     * @param productId
     * @param callback
     */
    @UniJSMethod
    public void launchPurchase(String productId, final UniJSCallback callback) {
        successPayCallback = callback;
        billingManager.launchPurchase(productId);
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