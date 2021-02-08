package com.genedev.bridge;

import android.content.Intent;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class SplashScreen extends AppCompatActivity {
    private static int TIME_OUT = 1000;
    private FirebaseAnalytics mFirebaseAnalytics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(SplashScreen.this, Home.class);
                startActivity(i);
                SplashScreen.this.finish();
            }
        }, TIME_OUT);
    }
}
