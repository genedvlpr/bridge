package com.genedev.bridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.Toolbar;

import android.view.View;

public class About extends AppCompatActivity {

    private Toolbar toolbar;
    private AppCompatImageButton btn_back;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btn_back = findViewById(R.id.back_btn);
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(About.this,Home.class);
                startActivity(i);
                About.this.finish();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(About.this,Donate.class);
                startActivity(i);
                About.this.finish();
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public void onBackPressed(){
        Intent i = new Intent(About.this,Home.class);
        startActivity(i);
        About.this.finish();
    }


    public void update(View view){
        Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.genedev.bridge"); // missing 'http://' will cause crashed
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }
}
