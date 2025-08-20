package com.lingfeng.swapface.base;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.android.billingclient.api.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GooglePayManager implements PurchasesUpdatedListener {
    private static final String TAG = "GooglePayManager";
    private static GooglePayManager instance;
    private BillingClient billingClient;
    private Activity activity;
    private ProductDetailsResponseListener productDetailsListener;

    private GooglePayManager(Activity activity) {
        this.activity = activity;
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()) //谷歌要求必须启动一次性购买
                .enableAutoServiceReconnection() // Add this line to enable reconnection
                .build();
    }


    public static GooglePayManager getInstance(Activity activity) {
        if (instance == null) instance = new GooglePayManager(activity);
        return instance;
    }

    public void startConnect(ConnectCallBack callBack){
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // BillingClient 可以使用了，查询商品或购买
                    Log.d(TAG, "连接成功");
                    callBack.callBackConnect();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "断开连接");
            }
        });
    }

    public void queryProductDetails(List<String> productIds, ProductDetailsResponseListener listener) {
        this.productDetailsListener = listener;

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String id : productIds) {
            productList.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build());
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, listener);
    }

    public void launchPurchaseFlow(ProductDetails productDetails) {
        if (productDetails == null) return;
        BillingFlowParams.ProductDetailsParams productParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Arrays.asList(productParams))
                .build();
        billingClient.launchBillingFlow(activity, flowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) handlePurchase(purchase);
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
            AcknowledgePurchaseParams ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            billingClient.acknowledgePurchase(ackParams, result -> { });
        }
    }

    public interface ConnectCallBack{
        void callBackConnect();
    }
}
