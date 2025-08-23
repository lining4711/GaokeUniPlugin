package com.lingfeng.swapface.base;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BillingManager {

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
    private BillingCallback callback = null;

    public BillingManager(Context context, BillingCallback callback) {
        this.context = context;
        this.callback = callback;
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
                    callback.onConnected();
                } else {
                    Log.e(TAG, "Connection failed: " + result.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Disconnected from Billing");
                callback.onDisconnected();
            }
        });
    }

    public void queryProducts(List<String> productIds, @BillingClient.ProductType String type) {
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
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // 注意：新版还可能返回未抓取产品，可进一步处理
                callback.onProductDetails(productDetailsList.getProductDetailsList(), Collections.emptyList());
            } else {
                Log.e(TAG, "Query failed: " + result.getDebugMessage());
            }
        });
    }

    public void launchPurchase(ProductDetails productDetails) {
        BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(pdp))
                .build();

        BillingResult result = billingClient.launchBillingFlow((Activity) context, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Launch purchase failed: " + result.getDebugMessage());
        }
    }

    public void consumePurchase(String purchaseToken) {
        ConsumeParams cp = ConsumeParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        billingClient.consumeAsync(cp, (result, token) -> {
            if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                callback.onConsumeSuccess(token); //一次性商品购买之后 消耗掉
            } else {
                Log.e(TAG, "Consume failed: " + result.getDebugMessage());
            }
        });
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = (result, purchases) -> {
        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase p : purchases) {
                callback.onPurchaseSuccess(p);
            }
        } else {
            callback.onPurchaseFailure(result);
        }
    };
}
