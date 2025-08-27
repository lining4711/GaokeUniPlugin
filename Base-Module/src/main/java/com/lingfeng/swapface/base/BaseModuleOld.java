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
import java.util.Collections;
import java.util.List;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class BaseModuleOld extends UniModule{

    private static final String TAG = "FloatUniModule";

    @UniJSMethod
    public void test(){
        showToast();
    }

    private void showToast() {
        Toast.makeText(mUniSDKInstance.getContext(), PayUtils.callNative(), Toast.LENGTH_LONG).show();
    }

    private BillingClient billingClient;

    // 购买结果监听
    private PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
            Toast.makeText(mUniSDKInstance.getContext(), "onPurchasesUpdated", Toast.LENGTH_LONG).show();

            JSONObject result = new JSONObject();
            result.put("responseCode", billingResult.getResponseCode());
            result.put("debugMessage", billingResult.getDebugMessage());

            if (purchases != null && purchases.size() > 0) {
                JSONArray purchaseArray = new JSONArray();
                for (Purchase purchase : purchases) {
                    JSONObject obj = new JSONObject();
                    obj.put("orderId", purchase.getOrderId());
                    obj.put("productIds", purchase.getProducts());
                    obj.put("purchaseToken", purchase.getPurchaseToken());
                    obj.put("originalJson", purchase.getOriginalJson());
                    purchaseArray.add(obj);
                }
                result.put("purchases", purchaseArray);
            }

            Toast.makeText(mUniSDKInstance.getContext(), "onPurchasesUpdated = " + result.toJSONString(), Toast.LENGTH_LONG).show();


            mUniSDKInstance.fireGlobalEventCallback("IAP_PURCHASE_UPDATE", result);
        }
    };

    // 初始化 BillingClient
    @UniJSMethod
    public void initClient() {
        Toast.makeText(mUniSDKInstance.getContext(), "initClient", Toast.LENGTH_LONG).show();

        if (billingClient == null) {
            billingClient = BillingClient.newBuilder(mUniSDKInstance.getContext())
                    .setListener(purchasesUpdatedListener)
                    .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
                    .build();
        }

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                JSONObject result = new JSONObject();
                result.put("responseCode", billingResult.getResponseCode());
                result.put("msg", billingResult.getDebugMessage());

                Toast.makeText(mUniSDKInstance.getContext(), "onBillingSetupFinished", Toast.LENGTH_LONG).show();

                mUniSDKInstance.fireGlobalEventCallback("IAP_INIT", result);
            }

            @Override
            public void onBillingServiceDisconnected() {
                mUniSDKInstance.fireGlobalEventCallback("IAP_DISCONNECT", new JSONObject());
            }
        });
    }

    // 查询商品详情
    @UniJSMethod(uiThread = true)
    public void queryProduct(String productId, String productType) {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<QueryProductDetailsParams.Product>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType) // "inapp" 或 "subs"
                        .build()
        );

        Toast.makeText(mUniSDKInstance.getContext(), "productList = " + productList.get(0).toString(), Toast.LENGTH_LONG).show();

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        Toast.makeText(mUniSDKInstance.getContext(), "QueryProductDetailsParams = " + params.toString(), Toast.LENGTH_LONG).show();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                 @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                JSONObject result = new JSONObject();
                result.put("responseCode", billingResult.getResponseCode());

                JSONArray array = new JSONArray();
                List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();
                if (productDetailsList != null) {
                    for (ProductDetails details : productDetailsList) {
                        JSONObject obj = new JSONObject();
                        obj.put("productId", details.getProductId());
                        obj.put("title", details.getTitle());
                        obj.put("description", details.getDescription());
                        if (details.getOneTimePurchaseOfferDetails() != null) {
                            obj.put("price", details.getOneTimePurchaseOfferDetails().getFormattedPrice());
                        }
                        array.add(obj);
                    }
                }
                result.put("products", array);

                Toast.makeText(mUniSDKInstance.getContext(), "onProductDetailsResponse = " + array.toJSONString(), Toast.LENGTH_LONG).show();

                mUniSDKInstance.fireGlobalEventCallback("IAP_PRODUCT_QUERY", result);
            }
        });
    }

    // 发起购买
    // 发起购买
    @UniJSMethod(uiThread = true)
    public void purchase(final String productId, final String productType) {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<QueryProductDetailsParams.Product>();
        productList.add(
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(productType)
                        .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult,
                                                 @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                List<ProductDetails> productDetailsList = queryProductDetailsResult.getProductDetailsList();
                if (productDetailsList != null && productDetailsList.size() > 0) {
                    ProductDetails productDetails = productDetailsList.get(0);

                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(
                                    Collections.singletonList(
                                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                                    .setProductDetails(productDetails)
                                                    .build()
                                    )
                            )
                            .build();

                    Activity activity = (Activity) mUniSDKInstance.getContext();
                    billingClient.launchBillingFlow(activity, billingFlowParams);
                }
            }
        });
    }

    // 查询购买记录
    @UniJSMethod(uiThread = true)
    public void queryPurchases(String productType) {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(productType)
                        .build(),
                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                        JSONObject result = new JSONObject();
                        result.put("responseCode", billingResult.getResponseCode());

                        JSONArray array = new JSONArray();
                        for (Purchase purchase : purchases) {
                            JSONObject obj = new JSONObject();
                            obj.put("orderId", purchase.getOrderId());
                            obj.put("purchaseToken", purchase.getPurchaseToken());
                            obj.put("products", purchase.getProducts());
                            obj.put("originalJson", purchase.getOriginalJson());
                            array.add(obj);
                        }
                        result.put("purchases", array);

                        mUniSDKInstance.fireGlobalEventCallback("IAP_QUERY_PURCHASES", result);
                    }
                }
        );
    }

    // 确认购买
    @UniJSMethod(uiThread = true)
    public void acknowledgePurchase(final String purchaseToken) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();

        billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                JSONObject result = new JSONObject();
                result.put("responseCode", billingResult.getResponseCode());
                result.put("purchaseToken", purchaseToken);

                mUniSDKInstance.fireGlobalEventCallback("IAP_ACKNOWLEDGE", result);
            }
        });
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