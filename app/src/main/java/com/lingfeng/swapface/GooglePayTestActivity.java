package com.lingfeng.swapface;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.lingfeng.swapface.base.GooglePayManager;

import java.util.ArrayList;
import java.util.List;

public class GooglePayTestActivity extends AppCompatActivity {

    private ListView productListView;
    private List<ProductDetails> productDetailsList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> displayList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_googlepay_test);

        productListView = findViewById(R.id.product_list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        productListView.setAdapter(adapter);

        fetchProducts();
        setupClickListener();
    }

    private void fetchProducts() {
        List<String> productIds = new ArrayList<>();
        productIds.add("swapface_1");
        productIds.add("swapface_2");

        GooglePayManager.getInstance(this).queryProductDetails(productIds, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult result) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<ProductDetails> list = result.getProductDetailsList();
                    productDetailsList.clear();
                    productDetailsList.addAll(list);
                    displayList.clear();
                    for (ProductDetails pd : list) {
                        if (pd.getOneTimePurchaseOfferDetails() != null) {
                            displayList.add(pd.getName() + " - " + pd.getOneTimePurchaseOfferDetails().getFormattedPrice());
                        } else {
                            displayList.add(pd.getName());
                        }
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(GooglePayTestActivity.this, "查询失败: " + billingResult.getDebugMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void setupClickListener() {
        productListView.setOnItemClickListener((parent, view, position, id) -> {
            ProductDetails pd = productDetailsList.get(position);
            GooglePayManager.getInstance(this).launchPurchaseFlow(pd);
        });
    }
}
