package com.lingfeng.swapface;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.lingfeng.swapface.base.JsonUtils;
import com.lingfeng.swapface.base.forapp.AppUsedCallBack;
import com.lingfeng.swapface.base.forapp.BillingManagerForAPP;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BillingTestActivity extends AppCompatActivity {

    private static final String TAG = "BillingTestActivity";

    private BillingManagerForAPP billingManager;
    private ProductDetails cachedProduct;

    private ListView logView;
    private Button btnQuery, btnBuy, btnStartConnect, queryBuy, btnQuerySubs, buySubs;

    private ArrayList<String> dataList;
    private ArrayAdapter<String> adapter;
    private int counter = 1; // 用来生成测试数据

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing_test);

        logView = findViewById(R.id.list_view);
        btnStartConnect = findViewById(R.id.btnStartConnect);
        btnQuery = findViewById(R.id.btnQuery);
        btnBuy = findViewById(R.id.btnBuy);
        queryBuy = findViewById(R.id.queryBuy);

        btnQuerySubs = findViewById(R.id.btnQuerySubs);
        buySubs = findViewById(R.id.buySubs);

        dataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, dataList);
        logView.setAdapter(adapter);

        billingManager = new BillingManagerForAPP(this, new BillingManagerForAPP.BillingCallback() {
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
            }

            @Override
            public void onPurchaseFailure(com.android.billingclient.api.BillingResult result) {
                appendLog("购买失败: " + result.getDebugMessage());
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
                billingManager.queryProductsAPPTest(
                        Arrays.asList("faceswap_1"), // ⚠️ 这里替换成你在 Play Console 配置的商品ID
                        BillingClient.ProductType.INAPP,  // 或 BillingClient.ProductType.SUBS
                        new AppUsedCallBack() {
                            @Override
                            public void responseData(JSONObject result) {
                                appendLog(JsonUtils.toString(result));
                            }
                        }
                );
            }
        });

        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingManager.launchPurchase("faceswap_1");
            }
        });

        btnQuerySubs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingManager.queryProductsAPPTest(
                        Arrays.asList("faceswap_1"), // ⚠️ 这里替换成你在 Play Console 配置的商品ID
                        BillingClient.ProductType.SUBS,  // 或 BillingClient.ProductType.SUBS
                        new AppUsedCallBack() {
                            @Override
                            public void responseData(JSONObject result) {
                                appendLog(JsonUtils.toString(result));
                            }
                        }
                );
            }
        });

        buySubs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingManager.launchPurchaseSubs("inapp");
            }
        });


        // 查询购买
        queryBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                billingManager.queryPurchasesUsedInAPP("inapp", new AppUsedCallBack() {
                    @Override
                    public void responseData(JSONObject result) {
                        appendLog("查询购买详情:" + "\n" +JsonUtils.toString(result));
                    }
                });
            }
        });



        findViewById(R.id.handConsumeProduct).setOnClickListener(new View.OnClickListener() {
            String buyProductId = "faceswap_1";
            @Override
            public void onClick(View v) {
                billingManager.queryPurchasesUsedInAPP("inapp", new AppUsedCallBack() {
                    @Override
                    public void responseData(JSONObject result) {
                        appendLog("查询购买详情:");

                        try {
                            List<Purchase> purchases =(List<Purchase>) result.get("data");
                            appendLog(JsonUtils.toString(purchases));

                            for (Purchase purchase : purchases) {
                                Log.d("Billing", "已购买但未消费: " + purchase.getProducts() + " , token=" + purchase.getPurchaseToken());

                                // 在这里判断是不是你要处理的那个商品
                                if (purchase.getProducts().contains(buyProductId)) {
                                    billingManager.consumePurchase(purchase.getPurchaseToken());
                                }
                            }
                            Log.d(TAG, JsonUtils.toString(purchases));
                        } catch (Exception e) {
                            Log.e(TAG, e.getLocalizedMessage());
                        }

                    }
                });



            }
        });

    }

    private void appendLog(String msg) {
        dataList.add(msg);
        adapter.notifyDataSetChanged();

        // 滚动到最后一行
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.setSelection(adapter.getCount() - 1);
            }
        });
    }
}

