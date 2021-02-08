package com.lib.bridge.qrcode;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatImageButton;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ToggleButton;

import com.google.zxing.ResultPoint;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.lib.bridge.R;

import java.util.List;

import butterknife.ButterKnife;

import static com.lib.bridge.sender.SHAREthemActivity.ADD_TO_CLIP;
import static com.lib.bridge.sender.SHAREthemActivity.ADD_TO_HISTORY;
import static com.lib.bridge.sender.SHAREthemActivity.BROWSE_IMAGE_URI;
import static com.lib.bridge.sender.SHAREthemActivity.QR_STRING;
import static com.lib.bridge.sender.SHAREthemActivity.QR_TYPE;

public class ScanActivity extends Activity {
    private static final String TAG = ScanActivity.class.getSimpleName();

    DecoratedBarcodeView barcodeView;

    ToggleButton flash;
    private BeepManager beepManager;
    private Bitmap qrBitmap;
    private boolean cameraFlashOn = false;
    public static final int BROWSE_IMAGE_REQUEST_CODE = 99;



    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null  ) {

                return;
            }

            try {

                QRCode  qrCode =  new QRCode(result.getText());
                qrBitmap =  qrCode.getSimpleBitmap(Color.BLACK, null, result.getBarcodeFormat()+"");
                if (qrBitmap!= null)
                {
                    Intent intent =  new Intent(ScanActivity.this, DetailsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra(QR_STRING,result.getText());
                    intent.putExtra(QR_TYPE,result.getBarcodeFormat().toString());
                    intent.putExtra(ADD_TO_HISTORY,true);
                    intent.putExtra(ADD_TO_CLIP,true);
                    intent.putExtra("task", "receive");
                    startActivity(intent);


                    beepManager.setBeepEnabled(true);
                    beepManager.setVibrateEnabled(false);
                    beepManager.updatePrefs();
                    beepManager.playBeepSoundAndVibrate();
                    finish();
                }

            } catch (WriterException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        ButterKnife.bind(this);

        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        Window window = getWindow();
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        //} else {
            //window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //}

        barcodeView = findViewById(R.id.barcode_scanner);

        flash = findViewById(R.id.flash);
        flash.setVisibility(View.INVISIBLE);
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);

        AppCompatImageButton img_scan = findViewById(R.id.image_scan);
        img_scan.setVisibility(View.INVISIBLE);
        beepManager = new BeepManager(this);
        TorchEventListener torchEventListener = new TorchEventListener(this);
        barcodeView.setTorchListener(torchEventListener);

        flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cameraFlashOn){
                    barcodeView.setTorchOff();
                }else{
                    barcodeView.setTorchOn();
                }
            }
        });


    }

    public void pressedBack(View v)
    {
        onBackPressed();
    }

    public void openGallery(View v)
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select the image with the QR Code"), BROWSE_IMAGE_REQUEST_CODE);

    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
        if(cameraFlashOn){
            barcodeView.setTorchOff();
        }
    }

    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }


    public void updateView(){
        if(cameraFlashOn){
            flash.setChecked(true);
        }else{
            flash.setChecked(false);

        }
    }




    class TorchEventListener implements DecoratedBarcodeView.TorchListener{
        private ScanActivity activity;

        TorchEventListener(ScanActivity activity){
            this.activity = activity;
        }

        @Override
        public void onTorchOn() {
            cameraFlashOn = true;
            this.activity.updateView();
        }

        @Override
        public void onTorchOff() {
            cameraFlashOn = false;
            this.activity.updateView();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BROWSE_IMAGE_REQUEST_CODE && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Intent intent =  new Intent(this, DetailsActivity.class);
            intent.putExtra(BROWSE_IMAGE_URI,uri.toString());
            intent.putExtra(ADD_TO_HISTORY,true);
            startActivity(intent);
            finish();
        }
    }
}
