package com.lingfeng.swapface;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void callModule(View view){
//        Toast.makeText(this, PayUtils.callNative(), Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, BillingTestActivity.class));
    }
}