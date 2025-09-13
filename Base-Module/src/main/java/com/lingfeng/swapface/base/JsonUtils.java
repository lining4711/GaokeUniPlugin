package com.lingfeng.swapface.base;

import com.android.billingclient.api.ProductDetails;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Created by lining on 16/3/24.
 */
public class JsonUtils {
    private static Gson gson = null;

    static {
        if (gson == null) {
            gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd hh:mm:ss")
                    .setLenient().registerTypeAdapterFactory(new NullStringToEmptyAdapterFactory()).create();
        }
    }

    public static Gson getGsonInstance(){
        return gson;
    }

    /**
     * 转成json
     *
     * @param object
     * @return
     */
    public static String toString(Object object) {
        String gsonString = null;
        if (gson != null) {
            gsonString = gson.toJson(object);
        }
        return gsonString;
    }

    /**
     * 转成bean
     *
     * @param gsonString
     * @param cls
     * @return
     */
    public static <T> T toBean(String gsonString, Class<T> cls) {
        T t = null;
        if (gson != null) {
            t = gson.fromJson(gsonString, cls);
        }
        return t;
    }

    /**
     * 转成list
     *
     * @param gsonString
     * @param type
     * @return
     */
    public static <T> List<T> toBeanList(String gsonString, Type type) {
        List<T> list = null;
        if (gson != null) {
            list = gson.fromJson(gsonString, type);
        }
        return list;
    }

    /**
     * 转成list中有map的
     *
     * @param gsonString
     * @return
     */
    public static <T> List<Map<String, T>> toListMaps(String gsonString) {
        List<Map<String, T>> list = null;
        if (gson != null) {
            list = gson.fromJson(gsonString,
                    new TypeToken<List<Map<String, T>>>() {
                    }.getType());
        }
        return list;
    }

    /**
     * 转成map的
     *
     * @param gsonString
     * @return
     */
    public static <T> Map<String, T> toMaps(String gsonString) {
        Map<String, T> map = null;
        if (gson != null) {
            map = gson.fromJson(gsonString, new TypeToken<Map<String, T>>() {
            }.getType());
        }
        return map;
    }

    /**
     * String null字段修改为""，防止上层逻辑空指针异常
     * @param <T>
     */
    public static class NullStringToEmptyAdapterFactory<T> implements TypeAdapterFactory {
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<T> rawType = (Class<T>) type.getRawType();
            if (rawType != String.class) {
                return null;
            }
            return (TypeAdapter<T>) new StringNullAdapter();
        }
    }

    public static class StringNullAdapter extends TypeAdapter<String> {
        @Override
        public String read(JsonReader reader) throws IOException {
            // TODO Auto-generated method stub
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return "";
            }
            return reader.nextString();
        }
        @Override
        public void write(JsonWriter writer, String value) throws IOException {
            // TODO Auto-generated method stub
            if (value == null) {
                writer.nullValue();
                return;
            }
            writer.value(value);
        }
    }

    public static JSONArray convertToJSONArray(List<ProductDetails> productDetailsList) {
        JSONArray jsonArray = new JSONArray();

        if (productDetailsList == null) return jsonArray;

        try {
            for (ProductDetails product : productDetailsList) {
                JSONObject obj = new JSONObject();
                obj.put("productId", product.getProductId());
                obj.put("title", product.getTitle());
                obj.put("name", product.getName());
                obj.put("description", product.getDescription());
                obj.put("productType", product.getProductType()); // inapp / subs

                // 一次性商品价格
                ProductDetails.OneTimePurchaseOfferDetails oneTime = product.getOneTimePurchaseOfferDetails();
                if (oneTime != null) {
                    JSONObject priceObj = new JSONObject();
                    priceObj.put("price", oneTime.getFormattedPrice());
                    priceObj.put("priceAmountMicros", oneTime.getPriceAmountMicros());
                    priceObj.put("priceCurrencyCode", oneTime.getPriceCurrencyCode());
                    obj.put("oneTimePurchaseOfferDetails", priceObj);
                }

                // 订阅商品价格
                List<ProductDetails.SubscriptionOfferDetails> subs = product.getSubscriptionOfferDetails();
                if (subs != null && !subs.isEmpty()) {
                    JSONArray subsArray = new JSONArray();
                    for (ProductDetails.SubscriptionOfferDetails offer : subs) {
                        JSONObject offerObj = new JSONObject();
                        offerObj.put("basePlanId", offer.getBasePlanId());
                        offerObj.put("offerId", offer.getOfferId());

                        // 每个订阅可能有多个定价阶段（比如试用、折扣、正常价）
                        List<ProductDetails.PricingPhase> pricingPhases = offer.getPricingPhases().getPricingPhaseList();
                        JSONArray phasesArray = new JSONArray();
                        for (ProductDetails.PricingPhase phase : pricingPhases) {
                            JSONObject phaseObj = new JSONObject();
                            phaseObj.put("price", phase.getFormattedPrice());
                            phaseObj.put("priceAmountMicros", phase.getPriceAmountMicros());
                            phaseObj.put("priceCurrencyCode", phase.getPriceCurrencyCode());
                            phaseObj.put("billingPeriod", phase.getBillingPeriod());
                            phaseObj.put("recurrenceMode", phase.getRecurrenceMode());
                            phaseObj.put("billingCycleCount", phase.getBillingCycleCount());
                            phasesArray.put(phaseObj);
                        }
                        offerObj.put("pricingPhases", phasesArray);

                        subsArray.put(offerObj);
                    }
                    obj.put("subscriptionOfferDetails", subsArray);
                }

                jsonArray.put(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return jsonArray;
    }
}
