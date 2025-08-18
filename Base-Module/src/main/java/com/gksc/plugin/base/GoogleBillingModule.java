package com.gksc.plugin.base;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClient.ProductType;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.taobao.weex.bridge.JSCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.common.UniModule;
import io.dcloud.feature.uniapp.utils.UniLogUtils;

@SuppressWarnings("unused")
public class GoogleBillingModule extends UniModule {

    private static final String TAG = "GoogleBillingModule";
    private BillingClient billingClient;
    private boolean isReady = false;

    // 暂存查询结果
    private final Map<String, ProductDetails> productDetailsMap = new HashMap<>();

    // 事件回调（JS 侧可注册一个全局回调）
    private JSCallback eventCallback;

    // ---- 工具：发事件到 JS ----
    private void emit(String event, Object data) {
        if (eventCallback != null) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event);
            payload.put("data", data);
            try {
                eventCallback.invokeAndKeepAlive(payload);
            } catch (Throwable t) {
                Log.e(TAG, "emit error", t);
            }
        } else {
            UniLogUtils.d(TAG + " event(" + event + ") but no JS listener");
        }
    }

    private Activity getActivitySafe() {
        Activity a = mWXSDKInstance != null ? mWXSDKInstance.getContext() instanceof Activity ? (Activity) mWXSDKInstance.getContext() : null : null;
        if (a == null && mUniSDKInstance != null) {
            a = mUniSDKInstance.getContext() instanceof Activity ? (Activity) mUniSDKInstance.getContext() : null;
        }
        return a;
    }

    // ---- 1) 初始化 ----

    /**
     * 初始化 BillingClient
     * js: billing.init({enablePending:true}, cb)
     */
    @UniJSMethod(uiThread = true)
    public void init(JSONObject options, JSCallback callback) {
        boolean enablePending = options != null && options.getBooleanValue("enablePending");
        if (billingClient != null && isReady) {
            if (callback != null) callback.invoke(success("already_ready"));
            return;
        }

        billingClient = BillingClient
                .newBuilder(getActivitySafe())
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().build()) // v8 强制要求调用一次；可不与 enablePending 变量绑定
                .setListener((billingResult, purchases) -> {
                    // 购买更新回调
                    JSONObject obj = new JSONObject();
                    obj.put("code", billingResult.getResponseCode());
                    obj.put("msg", billingResult.getDebugMessage());
                    obj.put("purchases", purchasesToJson(purchases));
                    emit("onPurchasesUpdated", obj);

                    // 自动处理：对已购买但未确认的内购进行自动确认（一次性/订阅），消耗型留给业务决定是否 consume
                    if (purchases != null) {
                        for (Purchase p : purchases) {
                            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged()) {
                                // 建议只对 非消耗型 商品自动确认；这里简单示例：全部尝试确认
                                acknowledge(p.getPurchaseToken(), null);
                            }
                        }
                    }
                })
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingServiceDisconnected() {
                isReady = false;
                emit("onDisconnected", null);
            }

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                isReady = billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK;
                if (callback != null) {
                    if (isReady) callback.invoke(success("ready"));
                    else callback.invoke(fail(billingResult));
                }
                if (isReady) emit("onReady", null);
            }
        });
    }

    @UniJSMethod(uiThread = false)
    public JSONObject isReady() {
        JSONObject o = new JSONObject();
        o.put("ready", isReady);
        return o;
    }

    // ---- 2) 事件监听注册 ----

    /**
     * 注册一个持续回调，接收模块事件：onReady / onDisconnected / onPurchasesUpdated 等
     * js: billing.setEventListener(cb)
     */
    @UniJSMethod(uiThread = true)
    public void setEventListener(JSCallback cb) {
        this.eventCallback = cb;
    }

    // ---- 3) 查询商品详情 ----

    /**
     * 批量查询商品详情
     * 参数示例：
     * {
     *   "inapp": ["coins_6","vip_lifetime"],
     *   "subs":  ["vip_month","vip_year"]
     * }
     */
    @UniJSMethod(uiThread = true)
    public void queryProducts(JSONObject req, JSCallback cb) {
        if (!isReady) {
            if (cb != null) cb.invoke(error("not_ready"));
            return;
        }
        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        if (req != null) {
            JSONArray inapp = req.getJSONArray("inapp");
            JSONArray subs = req.getJSONArray("subs");
            if (inapp != null) {
                for (int i = 0; i < inapp.size(); i++) {
                    list.add(
                            QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(inapp.getString(i))
                                    .setProductType(ProductType.INAPP)
                                    .build()
                    );
                }
            }
            if (subs != null) {
                for (int i = 0; i < subs.size(); i++) {
                    list.add(
                            QueryProductDetailsParams.Product.newBuilder()
                                    .setProductId(subs.getString(i))
                                    .setProductType(ProductType.SUBS)
                                    .build()
                    );
                }
            }
        }

        if (list.isEmpty()) {
            if (cb != null) cb.invoke(error("empty_request"));
            return;
        }

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(list)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            JSONObject resp = new JSONObject();
            resp.put("code", billingResult.getResponseCode());
            resp.put("msg", billingResult.getDebugMessage());

            JSONArray arr = new JSONArray();
            productDetailsMap.clear();
            if (productDetailsList != null) {
                for (ProductDetails pd : productDetailsList.getProductDetailsList()) {
                    productDetailsMap.put(pd.getProductId(), pd);
                    arr.add(productToJson(pd));
                }
            }
            resp.put("list", arr);
            if (cb != null) cb.invoke(resp);
        });
    }

    // ---- 4) 发起购买 ----

    /**
     * 发起购买
     * 参数示例（一次性商品）：
     * { "productId":"coins_6", "type":"inapp", "offerToken":"" }
     *
     * 参数示例（订阅）：
     * { "productId":"vip_month", "type":"subs", "offerToken":"xxx", "basePlanId":"basic" }
     */
    @UniJSMethod(uiThread = true)
    public void purchase(JSONObject req, JSCallback cb) {
        if (!isReady) {
            if (cb != null) cb.invoke(error("not_ready"));
            return;
        }
        String productId = req.getString("productId");
        String type = req.getString("type"); // "inapp" or "subs"
        String offerToken = req.getString("offerToken"); // 订阅或多层级价格使用
        String basePlanId = req.getString("basePlanId"); // 可选

        ProductDetails pd = productDetailsMap.get(productId);
        if (pd == null) {
            if (cb != null) cb.invoke(error("product_not_cached_query_first"));
            return;
        }

        BillingFlowParams.ProductDetailsParams.Builder pdBuilder =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(pd);

        // 订阅需要设置 offerToken
        if (ProductType.SUBS.equals(type)) {
            if (offerToken == null || offerToken.isEmpty()) {
                // 自动取第一个 offerToken（如果存在）
                List<ProductDetails.SubscriptionOfferDetails> offers = pd.getSubscriptionOfferDetails();
                if (offers != null && !offers.isEmpty()) {
                    offerToken = offers.get(0).getOfferToken();
                }
            }
            if (offerToken != null) {
                pdBuilder.setOfferToken(offerToken);
            }
        } else {
            // 一次性内购（INAPP）通常不需要 offerToken
        }

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                        java.util.Collections.singletonList(pdBuilder.build())
                )
                .build();

        Activity activity = getActivitySafe();
        if (activity == null) {
            if (cb != null) cb.invoke(error("no_activity"));
            return;
        }

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        JSONObject resp = new JSONObject();
        resp.put("code", result.getResponseCode());
        resp.put("msg", result.getDebugMessage());
        if (cb != null) cb.invoke(resp);
    }

    // ---- 5) 消耗型商品（消耗） ----

    /**
     * 消耗已购（INAPP）
     * { "purchaseToken":"xxxxxx" }
     */
    @UniJSMethod(uiThread = true)
    public void consume(JSONObject req, JSCallback cb) {
        if (!isReady) {
            if (cb != null) cb.invoke(error("not_ready"));
            return;
        }
        String token = req.getString("purchaseToken");
        if (token == null || token.isEmpty()) {
            if (cb != null) cb.invoke(error("empty_token"));
            return;
        }
        ConsumeParams params = ConsumeParams.newBuilder()
                .setPurchaseToken(token)
                .build();
        billingClient.consumeAsync(params, (billingResult, outToken) -> {
            if (cb != null) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    cb.invoke(success("consumed"));
                } else {
                    cb.invoke(fail(billingResult));
                }
            }
        });
    }

    // ---- 6) 确认购买（非消耗型/订阅） ----

    /**
     * 确认购买（acknowledge）
     * { "purchaseToken":"xxxxxx" }
     */
    @UniJSMethod(uiThread = true)
    public void acknowledge(JSONObject req, JSCallback cb) {
        String token = req.getString("purchaseToken");
        acknowledge(token, cb);
    }

    private void acknowledge(String token, JSCallback cb) {
        if (!isReady) {
            if (cb != null) cb.invoke(error("not_ready"));
            return;
        }
        if (token == null || token.isEmpty()) {
            if (cb != null) cb.invoke(error("empty_token"));
            return;
        }
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(token)
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (cb != null) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    cb.invoke(success("acknowledged"));
                } else {
                    cb.invoke(fail(billingResult));
                }
            }
        });
    }

    // ---- 7) 查询已购 ----

    /**
     * 查询已购
     * { "type":"inapp" } 或 { "type":"subs" }
     */
    @UniJSMethod(uiThread = true)
    public void queryPurchases(JSONObject req, JSCallback cb) {
        if (!isReady) {
            if (cb != null) cb.invoke(error("not_ready"));
            return;
        }
        String type = req.getString("type");
        if (!ProductType.INAPP.equals(type) && !ProductType.SUBS.equals(type)) {
            if (cb != null) cb.invoke(error("type_must_be_inapp_or_subs"));
            return;
        }
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(type)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, list) -> {
            JSONObject resp = new JSONObject();
            resp.put("code", billingResult.getResponseCode());
            resp.put("msg", billingResult.getDebugMessage());
            resp.put("purchases", purchasesToJson(list));
            if (cb != null) cb.invoke(resp);
        });
    }

    // ---- JSON 辅助 ----
    private JSONObject success(String msg) {
        JSONObject o = new JSONObject();
        o.put("code", 0);
        o.put("msg", msg);
        return o;
    }

    private JSONObject error(String msg) {
        JSONObject o = new JSONObject();
        o.put("code", -1);
        o.put("msg", msg);
        return o;
    }

    private JSONObject fail(BillingResult r) {
        JSONObject o = new JSONObject();
        o.put("code", r.getResponseCode());
        o.put("msg", r.getDebugMessage());
        return o;
    }

    private JSONObject productToJson(ProductDetails pd) {
        JSONObject o = new JSONObject();
        o.put("productId", pd.getProductId());
        o.put("title", pd.getTitle());
        o.put("name", pd.getName());
        o.put("description", pd.getDescription());
        o.put("productType", pd.getProductType());

        // 一次性价格
        if (ProductType.INAPP.equals(pd.getProductType()) && pd.getOneTimePurchaseOfferDetails() != null) {
            JSONObject price = new JSONObject();
            price.put("priceMicros", String.valueOf(pd.getOneTimePurchaseOfferDetails().getPriceAmountMicros()));
            price.put("price", pd.getOneTimePurchaseOfferDetails().getFormattedPrice());
            price.put("currency", pd.getOneTimePurchaseOfferDetails().getPriceCurrencyCode());
            o.put("oneTime", price);
        }

        // 订阅 offers
        if (ProductType.SUBS.equals(pd.getProductType()) && pd.getSubscriptionOfferDetails() != null) {
            JSONArray offers = new JSONArray();
            for (ProductDetails.SubscriptionOfferDetails so : pd.getSubscriptionOfferDetails()) {
                JSONObject jo = new JSONObject();
                jo.put("offerToken", so.getOfferToken());
                jo.put("basePlanId", so.getBasePlanId());
                JSONArray pricingPhases = new JSONArray();
                for (ProductDetails.PricingPhase phase : so.getPricingPhases().getPricingPhaseList()) {
                    JSONObject ph = new JSONObject();
                    ph.put("priceMicros", String.valueOf(phase.getPriceAmountMicros()));
                    ph.put("price", phase.getFormattedPrice());
                    ph.put("currency", phase.getPriceCurrencyCode());
                    ph.put("billingPeriod", phase.getBillingPeriod());
                    ph.put("recurrenceMode", phase.getRecurrenceMode());
                    pricingPhases.add(ph);
                }
                jo.put("pricingPhases", pricingPhases);
                offers.add(jo);
            }
            o.put("offers", offers);
        }
        return o;
    }

    private JSONArray purchasesToJson(List<Purchase> purchases) {
        JSONArray arr = new JSONArray();
        if (purchases == null) return arr;
        for (Purchase p : purchases) {
            JSONObject o = new JSONObject();
            o.put("orderId", p.getOrderId());
            o.put("packageName", p.getPackageName());
            o.put("purchaseToken", p.getPurchaseToken());
            o.put("purchaseTime", p.getPurchaseTime());
            o.put("purchaseState", p.getPurchaseState());
            o.put("isAcknowledged", p.isAcknowledged());
            o.put("products", p.getProducts());
            o.put("originalJson", p.getOriginalJson());
            o.put("signature", p.getSignature());
            o.put("accountIdentifiers", p.getAccountIdentifiers() != null ? p.getAccountIdentifiers().toString() : null);
            arr.add(o);
        }
        return arr;
    }
}
