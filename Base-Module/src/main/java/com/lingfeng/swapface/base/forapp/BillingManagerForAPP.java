package com.lingfeng.swapface.base.forapp;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.lingfeng.swapface.base.JsonUtils;
import com.lingfeng.swapface.base.PayResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;

public class BillingManagerForAPP {
    private List<ProductDetails> mProductDetailsList = new ArrayList<>();

    public List<ProductDetails> getProductDetailsList() {
        return mProductDetailsList;
    }

    public interface BillingCallback {
        void onConnected();

        void onDisconnected();

        void onProductDetails(List<ProductDetails> products, List<ProductDetails> unfetchedProducts);

        void onPurchaseSuccess(Purchase purchase);

        void onPurchaseFailure(BillingResult result);

        void onConsumeSuccess(String purchaseToken);
    }

    private static final String TAG = "BillingManager";
    private final BillingClient billingClient;
    private final Context context;
    private BillingCallback coreCallback = null;

    public BillingManagerForAPP(Context context, BillingCallback callback) {
        this.context = context;
        this.coreCallback = callback;
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .enableAutoServiceReconnection()
                .build();
    }

    public void startConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult result) {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Connected to Billing");
                    coreCallback.onConnected();
                } else {
                    Log.e(TAG, "Connection failed: " + result.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Disconnected from Billing");
                coreCallback.onDisconnected();
            }
        });
    }

    /**
     * APP
     *
     * @param productIds
     * @param type
     * @return
     */
    public void queryProductsAPPTest(List<String> productIds, @BillingClient.ProductType String type, AppUsedCallBack appUsedCallBack) {
        List<QueryProductDetailsParams.Product> prodList = new ArrayList<>();
        for (String id : productIds) {
            prodList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(type)
                    .build());
        }
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(prodList)
                .build();

        billingClient.queryProductDetailsAsync(params, (result, productDetailsList) -> {

            JSONObject resultAsyn = new JSONObject();
            try {
                if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mProductDetailsList.clear();
                    mProductDetailsList.addAll(productDetailsList.getProductDetailsList());

                    resultAsyn.put("code", 200);
                    resultAsyn.put("data", JsonUtils.convertToJSONArray(mProductDetailsList));
                } else {
                    resultAsyn.put("code", 300);
                    resultAsyn.put("errorMsg", "queryProductDetailsAsync failed, errorCode: " + result.getResponseCode());
                }
            } catch (JSONException e) {
            }

            appUsedCallBack.responseData(resultAsyn);
        });
    }

    /**
     * 发起购买
     *
     * @param productId
     */
    public void launchPurchase(String productId) {
        ProductDetails findProductDetails = null;
        for (ProductDetails item : mProductDetailsList) { //先查询后购买
            if (productId.equals(item.getProductId())) {
                findProductDetails = item;
                break;
            }
        }
        if (null == findProductDetails) {
            return;
        }

        BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(findProductDetails)
                .build();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdp))
                .build();

        BillingResult result = billingClient.launchBillingFlow((Activity) context, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) { //这里只是发起购买成功
            Log.e(TAG, "Launch purchase failed: " + result.getDebugMessage());
        }
    }

    /**
     * 发起购买订阅
     */
    public void launchPurchaseSubs(String subsId) {
        ProductDetails findProductDetails = null;
        for (ProductDetails item : mProductDetailsList) {
            if (subsId.equals(item.getProductId())) {
                findProductDetails = item;
                break;
            }
        }
        if (null == findProductDetails) {
            return;
        }


        // 订阅必须选择一个定价方案 (base plan / offer)
        List<ProductDetails.SubscriptionOfferDetails> offerDetailsList = findProductDetails.getSubscriptionOfferDetails();
        if (offerDetailsList != null && !offerDetailsList.isEmpty()) {
            ProductDetails.SubscriptionOfferDetails offerDetails = offerDetailsList.get(0);

            BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(findProductDetails)
                    .setOfferToken(offerDetails.getOfferToken()) // 订阅需要传递tocken
                    .build();

            BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Collections.singletonList(pdp))
                    .build();

            BillingResult result = billingClient.launchBillingFlow((Activity) context, flowParams);
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) { //这里只是发起购买成功
                Log.e(TAG, "Launch purchase failed: " + result.getDebugMessage());
            }
        }
    }

    public void consumePurchase(String purchaseToken) {
        ConsumeParams cp = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.consumeAsync(cp, (result, token) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                coreCallback.onConsumeSuccess(token); //一次性商品购买之后 消耗掉
            } else {
                Log.e(TAG, "Consume failed: " + result.getDebugMessage());
            }
        });
    }

    public void queryPurchases(String productType, final UniJSCallback callback) {
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(productType)
                        .build(),
                new PurchasesResponseListener() {
                    @Override
                    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                        PayResult resultAsyn = new PayResult();
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                            JSONArray array = new JSONArray();
                            for (Purchase purchase : purchases) {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("orderId", purchase.getOrderId());
                                    obj.put("purchaseToken", purchase.getPurchaseToken());
                                    obj.put("products", purchase.getProducts());
                                    obj.put("originalJson", purchase.getOriginalJson());
                                    array.put(obj);
                                } catch (Exception e) {
                                    Log.e(TAG, e.getLocalizedMessage());
                                }
                            }
                            resultAsyn.setResultCode(200);
                            resultAsyn.setData(array.toString());
                        } else {
                            resultAsyn.setResultCode(300);
                            resultAsyn.setErrorMsg("queryPurchases failed: " + billingResult.getDebugMessage());
                        }

                        // 回到主线程回调给 UniApp
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.invoke(resultAsyn);
                                }
                            }
                        });
                    }
                }
        );
    }

    /**
     * 更新订阅
     *
     * @param oldTocken    需要替换的套餐 赌赢的tocken
     * @param newProductId 新的套餐的ID
     * @param callback
     */
    public void upgradeSubscription(String oldTocken, String newProductId, final UniJSCallback callback) throws JSONException {
        ProductDetails findNewProductDetails = null;
        for (ProductDetails item : mProductDetailsList) {
            if (newProductId.equals(item.getProductId())) {
                findNewProductDetails = item;
                break;
            }
        }

        JSONObject resultAsyn = new JSONObject();
        // 假设你已经通过 queryProductDetailsAsync 拿到新的订阅 ProductDetails
        if (findNewProductDetails == null || findNewProductDetails.getSubscriptionOfferDetails() == null) {
            Log.e("BillingDemo", "订阅商品信息无效");
            resultAsyn.put("code", 300);
            resultAsyn.put("data","没有查到商品信息，请先查询商品信息");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.invoke(resultAsyn);
                    }
                }
            });
        } else {
            ProductDetails.SubscriptionOfferDetails offerNewDetails =
                    findNewProductDetails.getSubscriptionOfferDetails().get(0); //作用：指定你要用户升级/降级到哪个订阅。 包含信息：商品 ID、价格、订阅周期、是否有优惠等。

            // 构建 BillingFlowParams
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(Arrays.asList(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(findNewProductDetails)
                                    .setOfferToken(offerNewDetails.getOfferToken())
                                    .build()
                    ))
                    .setSubscriptionUpdateParams(
                            BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                                    .setOldPurchaseToken(oldTocken)  // 旧订阅 token
                                    .setSubscriptionReplacementMode(
                                            BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE
                                    ) // 立即生效，剩余时长折算
                                    .build()
                    )
                    .build();

            BillingResult result = billingClient.launchBillingFlow((Activity) context, billingFlowParams);
            if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) { //这里只是发起购买成功
                resultAsyn.put("code", 200);
                resultAsyn.put("data", "更新订阅成功");
            } else {
                resultAsyn.put("code", 300);
                resultAsyn.put("errorMsg" ,"更新订阅失败：" + result.getResponseCode());
            }

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.invoke(resultAsyn);
                    }
                }
            });
        }

    }


    public void queryPurchasesUsedInAPP(String productType, AppUsedCallBack appUsedCallBack) {
        // 创建查询参数
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(productType) // 查询一次性购买
                // 对于订阅，使用 SkuType.SUBS
                .build();

        // 执行查询
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
//            JSONObject resultAsyn = new JSONObject();
//            try {
//                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
//                    resultAsyn.put("code", 200);
//                    resultAsyn.put("data", JsonUtils.toString(purchases));
//                } else {
//                    resultAsyn.put("code", 300);
//                    resultAsyn.put("errorMsg", "queryPurchases failed,errorCode: " + billingResult.getResponseCode());
//                }
//            } catch (JSONException e) {
//                throw new RuntimeException(e);
//            }

//            appUsedCallBack.responseData(resultAsyn);

            JSONObject result = new JSONObject();
            try {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    result.put("code", 200);
                    JSONArray array = new JSONArray();
                    for (Purchase purchase : purchases) {
                        JSONObject p = new JSONObject();
                        p.put("orderId", purchase.getOrderId());
                        p.put("productId", purchase.getProducts().toString());
                        p.put("purchaseTime", purchase.getPurchaseTime());
                        p.put("purchaseState", purchase.getPurchaseState());
                        array.put(p);
                    }
                    result.put("data", array);
                } else {
                    result.put("code", 300);
                    result.put("errorMsg", "queryPurchases failed: " + billingResult.getDebugMessage());
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON 构建失败：" + e.getLocalizedMessage());
            }

            appUsedCallBack.responseData(result);
        });
    }

    // 使用 acknowledgePurchase 确认购买
    public void acknowledgePurchase(String purchaseTocken) {
        AcknowledgePurchaseParams acknowledgeParams =
                AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchaseTocken)
                        .build();

        billingClient.acknowledgePurchase(acknowledgeParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "购买已确认，可查询");
            } else {
                Log.e(TAG, "确认失败：" + billingResult.getDebugMessage());
            }
        });
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = (result, purchases) -> {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) {
                // 那些购买成功了
                coreCallback.onPurchaseSuccess(p);
            }
        } else {
            coreCallback.onPurchaseFailure(result);
        }
    };
}
