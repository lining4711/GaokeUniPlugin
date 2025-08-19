package com.lingfeng.swapface.base;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.android.billingclient.api.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GooglePayManager implements PurchasesUpdatedListener {

    private static GooglePayManager instance;
    private BillingClient billingClient;
    private Activity activity;
    private ProductDetailsResponseListener productDetailsListener;

    private GooglePayManager(Activity activity) {
        this.activity = activity;
        billingClient = BillingClient.newBuilder(activity)
                .setListener(this)
//                .enablePendingPurchases(PendingPurchasesParams.newBuilder().build())
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) { }

            @Override
            public void onBillingServiceDisconnected() { }
        });
    }

    public static GooglePayManager getInstance(Activity activity) {
        if (instance == null) instance = new GooglePayManager(activity);
        return instance;
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
}
