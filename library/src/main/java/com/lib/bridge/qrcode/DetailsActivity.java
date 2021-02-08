package com.lib.bridge.qrcode;

import androidx.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.HybridBinarizer;
import com.lib.bridge.R;
import com.lib.bridge.receiver.ReceiverActivity;
import com.lib.bridge.sender.SHAREthemActivity;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import butterknife.ButterKnife;
import io.reactivex.annotations.NonNull;

import static com.lib.bridge.qrcode.Utils.isStoragePermissionGranted;
import static com.lib.bridge.sender.SHAREthemActivity.ADD_TO_CLIP;
import static com.lib.bridge.sender.SHAREthemActivity.ADD_TO_HISTORY;
import static com.lib.bridge.sender.SHAREthemActivity.BROWSE_IMAGE_URI;
import static com.lib.bridge.sender.SHAREthemActivity.QR_STRING;
import static com.lib.bridge.sender.SHAREthemActivity.QR_TYPE;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    ImageView imageView;
    RecyclerView recyclerView;
    TextView textView;
    ScrollView scrollView;
    TextView contentType;
    Toolbar toolbar;
    RelativeLayout parentView;
    TextView time;
    private Bitmap qrBitmap;
    private ScanViewModel scanViewModel;
    private Boolean addToHistory;
    private Boolean addToClip = false;
    private SharedPreferences preferences;

    private String textString = "";

    private String task = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        ButterKnife.bind(this);

        imageView = findViewById(R.id.qr_image);
        recyclerView = findViewById(R.id.recycler_view);
        time = findViewById(R.id.time);
        parentView = findViewById(R.id.parent);
        toolbar = findViewById(R.id.toolbar);
        contentType = findViewById(R.id.content_type);
        textView = findViewById(R.id.resultText);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        String result1 = getIntent().getExtras().getString(QR_STRING);
        String type = getIntent().getExtras().getString(QR_TYPE);
        addToHistory = getIntent().getExtras().getBoolean(ADD_TO_HISTORY);
        addToClip = getIntent().getExtras().getBoolean(ADD_TO_CLIP);
        task = Objects.requireNonNull(getIntent().getExtras().get("task")).toString();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        scanViewModel = ViewModelProviders.of(this).get(ScanViewModel.class);

        if (result1 != null && type != null) {
            QRCode qrCode = new QRCode(result1);
            try {
                qrBitmap = qrCode.getSimpleBitmap(getResources().getColor(R.color.colorPrimary), null, type);
                if (qrBitmap != null) {
                    ScanBitmap scanBitmap = new ScanBitmap();
                    scanBitmap.execute(qrBitmap);
                }


            } catch (WriterException e) {
                e.printStackTrace();
            }
        } else {
            Uri uri = Uri.parse(getIntent().getExtras().getString(BROWSE_IMAGE_URI));
            try {
                qrBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                ScanBitmap scanBitmap = new ScanBitmap();
                scanBitmap.execute(qrBitmap);



            } catch (Exception e) {

            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.details_menu, menu);
        return true;
    }

    public void back(View v){
        if (task.equals("send")){
            Intent i = new Intent(DetailsActivity.this, SHAREthemActivity.class);
            startActivity(i);
            DetailsActivity.this.finish();
        } else {
            Intent i = new Intent(DetailsActivity.this, ScanActivity.class);
            startActivity(i);
            DetailsActivity.this.finish();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
        } else if (i == R.id.save_image) {
            if (isStoragePermissionGranted(this, this)) {
                new Utils().savaImage(qrBitmap, parentView);
            }
        } else if (i == R.id.menu_copy) {
            if (!textString.equals("")) {
                copyText(textString);
            }
        } else if (i == R.id.menu_share) {
            if (!textString.equals("")) {
                String shareBody = textString;
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        }
        return super.onOptionsItemSelected(item);
    }


    class ScanBitmap extends AsyncTask<Bitmap, Void, Result> {

        @Override
        protected Result doInBackground(Bitmap... bitmaps) {

            Reader reader = new MultiFormatReader();
            Result result = null;
            int[] intArray = new int[bitmaps[0].getWidth() * bitmaps[0].getHeight()];
            bitmaps[0].getPixels(intArray, 0, bitmaps[0].getWidth(), 0, 0, bitmaps[0].getWidth(), bitmaps[0].getHeight());
            LuminanceSource source = new RGBLuminanceSource(bitmaps[0].getWidth(), bitmaps[0].getHeight(), intArray);
            BinaryBitmap bMap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                result = reader.decode(bMap);
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (ChecksumException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Result result) {
            super.onPostExecute(result);
            if (result != null) {
                final ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(DetailsActivity.this, result);
                getSupportActionBar().setTitle(result.getBarcodeFormat().toString().replace("_", " "));
                textView.setText(result.getText());
                contentType.setText(result.getBarcodeFormat().toString().replace("_", " "));
                int menuItemsCount = resultHandler.getButtonCount();

                final String[] menuItems = new String[menuItemsCount];
                for (int i = 0; i < menuItemsCount; i++) {
                    menuItems[i] = getString(resultHandler.getButtonText(i));
                }
                textView.setText(result.getText());
                Timestamp stamp = new Timestamp(result.getTimestamp());
                Date date = new Date(stamp.getTime());

                time.setText(new SimpleDateFormat("dd MMM yyyy   HH:mm:SS").format(date));
                textString = result.getText();
                ActionAdapter actionAdapter = new ActionAdapter(DetailsActivity.this, menuItems);
                recyclerView.setLayoutManager(new GridLayoutManager(DetailsActivity.this, 3));
                recyclerView.setAdapter(actionAdapter);
                if (task.equals("send")){
                    recyclerView.setVisibility(View.GONE);
                } else {
                    resultHandler.handleButtonPress(0);
                    recyclerView.setVisibility(View.INVISIBLE);
                    Intent i =new Intent(DetailsActivity.this, ReceiverActivity.class);
                    i.putExtra("server", "http://192.168.43.1:52287");
                    startActivity(i);
                    DetailsActivity.this.finish();
                }
                //actionAdapter.setOnItemClickListener(new ActionAdapter.OnItemClickListener() {
                    //@Override
                    //public void onItemClick(View view, int position) {

                        //Toast.makeText(DetailsActivity.this, position+"", Toast.LENGTH_SHORT).show();
                    //}
                //});



                if (qrBitmap != null) {
                    imageView.setImageBitmap(qrBitmap);
                }

                ///  add to history
                if (addToHistory && preferences.getBoolean("add_to_history", true)) {
                    scanViewModel.insert(new Scan(result.getText(), result.getBarcodeFormat().toString(), resultHandler.getImageResource(), result.getTimestamp()));

                }
                /// copy to clip after result
                if (addToClip && preferences.getBoolean("auto_copy_switch", true)) {
                    copyText(result.getText());
                }
            } else {
                Toast.makeText(DetailsActivity.this, "Unsupported Code try again", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new Utils().savaImage(qrBitmap, parentView);
        }
    }

    public void copyText(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Text", text);
        clipboard.setPrimaryClip(clip);
    }

}