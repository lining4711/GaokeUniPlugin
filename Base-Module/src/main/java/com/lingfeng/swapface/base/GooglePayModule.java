package com.lingfeng.swapface.base;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class GooglePayModule extends UniModule {

    private static final String TAG = "FloatUniModule";
    private BillingManager billingManager = null;

    private UniJSCallback successPayCallback;

    private UniJSCallback queryProductCallback;

    @UniJSMethod
    public void test() {
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
                JSONObject resultAsyn = new JSONObject();
                try {
                    resultAsyn.put("code", 200);
                    resultAsyn.put("data", "Billing Service 已连接");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.invoke(resultAsyn.toString()); //可以
//                            callback.invoke(resultAsyn); //JSONObject直接返回 不可以
                        }
                    }
                });
            }

            @Override
            public void onDisconnected() {
                JSONObject resultAsyn = new JSONObject();

                try {
                    resultAsyn.put("code", 300);
                    resultAsyn.put("errorMsg", "断开连接");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                // 回到主线程回调给 UniApp
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.invoke(resultAsyn.toString());
                        }
                    }
                });
            }

            @Override
            public void onProductDetails(List<ProductDetails> products, List<ProductDetails> unfetched) {
                JSONObject resultAsyn = new JSONObject();
                try {
                    resultAsyn.put("code", 200);
                    resultAsyn.put("data", "3333");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

//                PayResult resultBack = new PayResult();
//                resultBack.setResultCode(200);
//                resultBack.setData(JsonUtils.convertToJSONArray(products).toString());

                String currentId = Thread.currentThread().getName();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(mUniSDKInstance.getContext(), "onProductDetails enter:"  + currentId + "===" + JsonUtils.toString(resultBack), Toast.LENGTH_LONG).show();

                        if (queryProductCallback != null) {
//                            queryProductCallback.invokeAndKeepAlive("onProductDetails enter:"  + currentId + "==" +  Thread.currentThread().getName());// 可以直接回调
                            queryProductCallback.invokeAndKeepAlive(resultAsyn.toString());
                        }
                    }
                });
                try {
//                    if (products.isEmpty()) {
//                        resultAsyn.put("code", 300);
//                        resultAsyn.put("errorMsg", "未查询到商品，确认参数");
//
//                    } else {
//                        resultAsyn.put("code", 200);
//                        resultAsyn.put("data", JsonUtils.convertToJSONArray(products));
//                    }
                } catch (Exception e) {

                }
            }

            @Override
            public void onProductDetailsFailed(int code, String errorMsg) {
                JSONObject resultAsyn = new JSONObject();
                try {
                    resultAsyn.put("code", code);
                    resultAsyn.put("errorMsg", errorMsg);
                } catch (Exception e) {

                }
                Toast.makeText(mUniSDKInstance.getContext(), "onProductDetailsFailed" + resultAsyn.toString() , Toast.LENGTH_LONG).show();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (queryProductCallback != null) {
                            queryProductCallback.invokeAndKeepAlive(resultAsyn.toString());
                        }
                    }
                });
            }

            /**
             * 所有的购买走到这里
             * @param purchase
             */
            @Override
            public void onPurchaseSuccess(Purchase purchase) {
                Toast.makeText(mUniSDKInstance.getContext(), "onPurchaseSuccess enter" , Toast.LENGTH_LONG).show();

                for (String productId : purchase.getProducts()) {
                    for (ProductDetails productDetails : billingManager.getProductDetailsList()) {
                        if (productDetails == null) continue;

                        if (productId.equals(productDetails.getProductId())) {
                            if (BillingClient.ProductType.SUBS.equals(productDetails.getProductType())) {
                                // 订阅消耗：acknowledge
                                // 一次性解锁型商品 → 应该用 acknowledgePurchase()，才能在 queryPurchasesAsync() 查到记录
                                billingManager.acknowledgePurchase(purchase.getPurchaseToken());
                            } else if (BillingClient.ProductType.INAPP.equals(productDetails.getProductType())) {
                                // 一次性商品必须消耗，否则不能再次购买(这里不再区分是消耗性还是非消耗性，由业务自己定义)
                                billingManager.consumePurchase(purchase.getPurchaseToken());
                            }
                        }
                    }
                }

                try {
                    JSONObject resultAsyn = new JSONObject();
                    resultAsyn.put("code", 200);
                    resultAsyn.put("data", purchase.getPurchaseToken()); //返回订阅成功的tocken

                    // 回到主线程回调给 UniApp
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (successPayCallback != null) {
                                successPayCallback.invoke(resultAsyn.toString());
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }

            @Override
            public void onPurchaseFailure(BillingResult purchaseResult) {
                Toast.makeText(mUniSDKInstance.getContext(), "onPurchaseFailure enter" , Toast.LENGTH_LONG).show();

                try {
                    JSONObject resultAsyn = new JSONObject();
                    resultAsyn.put("code", 300);
                    resultAsyn.put("errorMsg", "onPurchaseFailure, errorCode" + purchaseResult.getResponseCode()); //返回订阅成功的tocken
                    // 回到主线程回调给 UniApp
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (successPayCallback != null) {
                                successPayCallback.invoke(resultAsyn.toString());
                            }
                        }
                    });
                } catch (Exception e) {

                }
            }

            @Override
            public void onConsumeSuccess(String purchaseToken) {
                Toast.makeText(mUniSDKInstance.getContext(), "onConsumeSuccess enter" , Toast.LENGTH_LONG).show();

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

    /**
     * 不能直接将 UniJSCallback callback 设为参数，超出作用域了
     *
     * @param productId
     * @param type
     * @param queryProductsCall
     */
    @UniJSMethod
    public void queryProduct(String productId, @BillingClient.ProductType String type, final UniJSCallback queryProductsCall) {
        queryProductCallback = queryProductsCall;
        billingManager.queryProducts(Arrays.asList(productId), type, queryProductsCall);
    }

    @UniJSMethod
    public void queryProducts(List<String> productIds, @BillingClient.ProductType String type, final UniJSCallback queryProductsCall) {
        queryProductCallback = queryProductsCall;
        billingManager.queryProducts(productIds, type, queryProductsCall);
    }

    /**
     * 发起购买  包括内购以及订阅购买
     *
     * @param productId
     * @param callback
     */
    @UniJSMethod
    public void launchPurchase(String productId, final UniJSCallback callback) {
        successPayCallback = callback;
        billingManager.launchPurchase(productId);
    }

    @UniJSMethod
    public void launchPurchaseSubs(String productId, final UniJSCallback callback) {
        successPayCallback = callback;
        billingManager.launchPurchaseSubs(productId);
    }

    /**
     * 查询购买
     *
     * @param productType
     * @param fromVueCallback
     */
    @UniJSMethod(uiThread = true)
    public void queryPurchases(String productType, final UniJSCallback fromVueCallback) {
        billingManager.queryPurchases(productType, fromVueCallback);
    }


    /**
     * 手动消耗购买
     *
     * @param productId
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void handConsumePurchase(String productId, final UniJSCallback callback) {
        billingManager.handConsumePurchase(productId, callback);
    }


    /**
     * 手动处理订阅类消耗
     *
     * @param subsProductId
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void handAcknowledgePurchase(String subsProductId, final UniJSCallback callback) {
        billingManager.handAcknowledgePurchase(subsProductId, callback);
    }

    /**
     * 更新订阅
     *
     * @param oldTocken        需要替换的套餐 赌赢的tocken
     * @param newSubsProductId 新的套餐的ID
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void upgradeSubscription(String oldTocken, String newSubsProductId, final UniJSCallback callback) {
        billingManager.upgradeSubscription(oldTocken, newSubsProductId, callback);
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