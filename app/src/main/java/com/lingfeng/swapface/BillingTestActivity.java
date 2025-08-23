package com.lingfeng.swapface;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.lingfeng.swapface.base.BillingManager;

import java.util.Arrays;
import java.util.List;

public class BillingTestActivity extends AppCompatActivity {

    private static final String TAG = "BillingTestActivity";

    private BillingManager billingManager;
    private ProductDetails cachedProduct;

    private TextView logView;
    private Button btnQuery, btnBuy, btnStartConnect;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_test);

        logView = findViewById(R.id.tvLog);
        btnStartConnect = findViewById(R.id.btnStartConnect);
        btnQuery = findViewById(R.id.btnQuery);
        btnBuy = findViewById(R.id.btnBuy);

        billingManager = new BillingManager(this, new BillingManager.BillingCallback() {
            @Override
            public void onConnected() {
                appendLog("Billing Service 已连接 ✅");
            }

            @Override
            public void onDisconnected() {
                appendLog("Billing Service 已断开 ❌");
            }

            @Override
            public void onProductDetails(List<ProductDetails> products, List<ProductDetails> unfetched) {
                appendLog("查询到商品数量: " + products.size());
                if (!products.isEmpty()) {
                    cachedProduct = products.get(0);
                    appendLog("商品ID: " + cachedProduct.getProductId());
                    appendLog("商品信息：" + cachedProduct.getDescription());
                }
                if (!unfetched.isEmpty()) {
                    appendLog("未抓取商品: " + unfetched.size());
                }
            }

            @Override
            public void onPurchaseSuccess(Purchase purchase) {
                appendLog("购买成功 ✅: " + purchase.getProducts());
                // 一次性商品必须消耗，否则不能再次购买
                billingManager.consumePurchase(purchase.getPurchaseToken());
            }

            @Override
            public void onPurchaseFailure(com.android.billingclient.api.BillingResult result) {
                appendLog("购买失败 ❌: " + result.getDebugMessage());
            }

            @Override
            public void onConsumeSuccess(String purchaseToken) {
                appendLog("消耗成功 ✅, token=" + purchaseToken);
            }
        });


        btnStartConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 开始连接
                billingManager.startConnection();
            }
        });

        btnQuery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                appendLog("正在查询商品...");
                billingManager.queryProducts(
                        Arrays.asList("faceswap_1"), // ⚠️ 这里替换成你在 Play Console 配置的商品ID
                        BillingClient.ProductType.INAPP  // 或 BillingClient.ProductType.SUBS
                );
            }
        });

        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingManager.launchPurchase("faceswap_1");
            }
        });
    }

    private void appendLog(String msg) {
        Log.d(TAG, msg);
        logView.append(msg + "\n");
    }
}

