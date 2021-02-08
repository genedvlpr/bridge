package com.genedev.bridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import android.widget.Toolbar;

import java.lang.reflect.Method;

public class Donate extends Activity {

    Toolbar toolbar;
    private RewardedAd rewardedAd;

    private InterstitialAd mInterstitialAd;

    private MaterialButton btn_play_rewarded;

    private FloatingActionButton fab;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);

        btn_play_rewarded = findViewById(R.id.btn_play_rewarded);


        rewardedAd = new RewardedAd(this, "ca-app-pub-3113729740390193/3106677088");

        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        btn_play_rewarded.setEnabled(false);
        btn_play_rewarded.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.btn_disabled)));
        btn_play_rewarded.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.btn_disabled)));

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void checkMobileData(){
        boolean mobileDataEnabled = false; // Assume disabled
        boolean wifiEnabled = false;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            // wifi is enabled
            wifiEnabled = true;
        }
        try {
            Class cmClass = Class.forName(cm.getClass().getName());
            Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
            method.setAccessible(true); // Make the method callable
            // get the setting for "mobile data"
            mobileDataEnabled = (Boolean)method.invoke(cm);
        } catch (Exception e) {
            // Some problem accessible private API
            // TODO do whatever error handling you want here
        }

        if (mobileDataEnabled || wifiEnabled){
            Snackbar.make(btn_play_rewarded, "Internet Connection is available.", Snackbar.LENGTH_SHORT).show();
            loadAds();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Donate.this);
            builder.setIcon(getResources().getDrawable(R.drawable.ic_report_problem_24px))
                    .setTitle("Turn on Mobile Data or Wifi")
                    .setMessage("Enable mobile data or WiFi to be able to view ads that will serve as your help to the developer.")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    }).setCancelable(false)
                    .show();
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {

        int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home:
                Intent i = new Intent(Donate.this,Home.class);
                startActivity(i);
                Donate.this.finish();

                // Toast.makeText(this, "home pressed", Toast.LENGTH_LONG).show();
                break;

        }

        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed(){
        Intent i = new Intent(Donate.this,Home.class);
        startActivity(i);
        Donate.this.finish();
    }

    @Override
    public void onStart(){
        super.onStart();
        checkMobileData();

    }

    private void loadAds(){
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                AdView adView = new AdView(Donate.this);
                adView.setAdSize(AdSize.BANNER);
                adView.setAdUnitId("ca-app-pub-3113729740390193/2215093781");

                mInterstitialAd = new InterstitialAd(Donate.this);
                mInterstitialAd.setAdUnitId("ca-app-pub-3113729740390193/3184436816");
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }

                RewardedAdLoadCallback adLoadCallback = new RewardedAdLoadCallback() {
                    @Override
                    public void onRewardedAdLoaded() {
                        // Ad successfully loaded.
                        Toast.makeText(Donate.this,"Video Ad loaded", Toast.LENGTH_SHORT).show();
                        btn_play_rewarded.setEnabled(true);
                        btn_play_rewarded.setStrokeColor(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                        btn_play_rewarded.setTextColor(ColorStateList.valueOf(getResources().getColor(R.color.colorPrimary)));
                        btn_play_rewarded.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Activity activityContext = Donate.this;
                                RewardedAdCallback adCallback = new RewardedAdCallback() {
                                    @Override
                                    public void onRewardedAdOpened() {
                                        // Ad opened.
                                    }

                                    @Override
                                    public void onRewardedAdClosed() {
                                        // Ad closed.
                                        Snackbar.make(fab,"Please finish every ads.",Snackbar.LENGTH_SHORT).show();
                                        loadAds();
                                    }

                                    @Override
                                    public void onUserEarnedReward(@NonNull RewardItem reward) {
                                        // User earned reward.
                                    }

                                    @Override
                                    public void onRewardedAdFailedToShow(int errorCode) {
                                        // Ad failed to display.
                                    }
                                };
                                rewardedAd.show(activityContext, adCallback);
                            }
                        });
                    }

                    @Override
                    public void onRewardedAdFailedToLoad(int errorCode) {
                        // Ad failed to load.
                    }
                };

                rewardedAd.loadAd(new AdRequest.Builder().build(), adLoadCallback);
            }
        });
    }
}
