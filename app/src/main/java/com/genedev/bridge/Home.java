package com.genedev.bridge;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.provider.Settings;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.lib.bridge.qrcode.ScanActivity;
import com.lib.bridge.sender.SHAREthemActivity;
import com.lib.bridge.sender.SHAREthemService;
import com.lib.bridge.utils.HotspotControl;
import com.lib.bridge.utils.Utils;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.activity.AudioPickActivity;
import com.vincent.filepicker.activity.ImagePickActivity;
import com.vincent.filepicker.activity.NormalFilePickActivity;
import com.vincent.filepicker.activity.VideoPickActivity;
import com.vincent.filepicker.filter.entity.AudioFile;
import com.vincent.filepicker.filter.entity.ImageFile;
import com.vincent.filepicker.filter.entity.NormalFile;
import com.vincent.filepicker.filter.entity.VideoFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;

import static com.lib.bridge.utils.Utils.DEFAULT_PORT_OREO;


public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private ParcelFileDescriptor mInputPFD;
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    String[] permissions = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.VIBRATE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WRITE_SETTINGS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_NOTIFICATION_POLICY,
    };

    public static final int PICKFILE_RESULT_CODE = 0;
    private TextView tvItemPath;
    private static final int EX_FILE_PICKER_RESULT = 0;
    private Uri fileUri;
    private String filePath;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusCheck();
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //if (mReservation != null) {
        try{
            mReservation.close();
        } catch (Exception e){

        }
        checkPermissions();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(Home.this, ImagePickActivity.class);
                startActivityForResult(intent1, Constant.REQUEST_CODE_PICK_IMAGE);
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void checkMobileData(){
        boolean mobileDataEnabled = false; // Assume disabled
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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

        if (mobileDataEnabled){
            AlertDialog.Builder builder = new AlertDialog.Builder(Home.this);
            builder.setIcon(getResources().getDrawable(R.drawable.ic_report_problem_24px))
                    .setTitle("Turn off Mobile Data")
                    .setMessage("Disable mobile data to be ableto send files to other devices.")
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constant.REQUEST_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    ArrayList<ImageFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_IMAGE);

                    ArrayList<String> arrayList = new ArrayList<>();
                    for (ImageFile  file : list) {
                        arrayList.add(file.getPath());
                    }

                    if (arrayList.size() == 0){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(Home.this);
                        dialog.setTitle("Empty Selection")
                                .setMessage("You must select atleast one file to be able to share to other devices, selection must not be empty.")
                                .setIcon(getResources().getDrawable(R.drawable.ic_alert_box))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        String [] files = GetStringArray(arrayList);
                        Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                        intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);
                        /*
                         * PORT value is hardcoded for Oreo & above since it's not possible to set SSID with which port info can be extracted on Receiver side.
                         */
                        intent.putExtra(SHAREthemService.EXTRA_PORT, DEFAULT_PORT_OREO);
                        /*
                         * Sender name can't be relayed to receiver for Oreo & above
                         */
                        intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Sri");
                        startActivity(intent);
                    }

                }
                break;
            case Constant.REQUEST_CODE_PICK_VIDEO:
                if (resultCode == RESULT_OK) {
                    ArrayList<VideoFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_VIDEO);
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (VideoFile file : list) {
                        arrayList.add(file.getPath());
                    }
                    if (arrayList.size() == 0){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(Home.this);
                        dialog.setTitle("Empty Selection")
                                .setMessage("You must select atleast one file to be able to share to other devices, selection must not be empty.")
                                .setIcon(getResources().getDrawable(R.drawable.ic_alert_box))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        String [] files = GetStringArray(arrayList);
                        Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                        intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);
                        /*
                         * PORT value is hardcoded for Oreo & above since it's not possible to set SSID with which port info can be extracted on Receiver side.
                         */
                        intent.putExtra(SHAREthemService.EXTRA_PORT, DEFAULT_PORT_OREO);
                        /*
                         * Sender name can't be relayed to receiver for Oreo & above
                         */
                        intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Sri");
                        startActivity(intent);
                    }

                }
                break;
            case Constant.REQUEST_CODE_PICK_AUDIO:
                if (resultCode == RESULT_OK) {
                    ArrayList<AudioFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_AUDIO);
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (AudioFile file : list) {
                        arrayList.add(file.getPath());
                    }
                    if (arrayList.size() == 0){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(Home.this);
                        dialog.setTitle("Empty Selection")
                                .setMessage("You must select atleast one file to be able to share to other devices, selection must not be empty.")
                                .setIcon(getResources().getDrawable(R.drawable.ic_alert_box))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        String [] files = GetStringArray(arrayList);
                        Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                        intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);
                        /*
                         * PORT value is hardcoded for Oreo & above since it's not possible to set SSID with which port info can be extracted on Receiver side.
                         */
                        intent.putExtra(SHAREthemService.EXTRA_PORT, DEFAULT_PORT_OREO);
                        /*
                         * Sender name can't be relayed to receiver for Oreo & above
                         */
                        intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Sri");
                        startActivity(intent);
                    }

                }
                break;
            case Constant.REQUEST_CODE_PICK_FILE:
                if (resultCode == RESULT_OK) {
                    ArrayList<NormalFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE);
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (NormalFile file : list) {
                        arrayList.add(file.getPath());
                    }
                    if (arrayList.size() == 0){
                        AlertDialog.Builder dialog = new AlertDialog.Builder(Home.this);
                        dialog.setTitle("Empty Selection")
                                .setMessage("You must select atleast one file to be able to share to other devices, selection must not be empty.")
                                .setIcon(getResources().getDrawable(R.drawable.ic_alert_box))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    } else {
                        String [] files = GetStringArray(arrayList);
                        Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                        intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);
                        /*
                         * PORT value is hardcoded for Oreo & above since it's not possible to set SSID with which port info can be extracted on Receiver side.
                         */
                        intent.putExtra(SHAREthemService.EXTRA_PORT, DEFAULT_PORT_OREO);
                        /*
                         * Sender name can't be relayed to receiver for Oreo & above
                         */
                        intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Sri");
                        startActivity(intent);
                    }

                }
                break;
        }

        if (requestCode == EX_FILE_PICKER_RESULT) {
            ExFilePickerResult result = ExFilePickerResult.getFromIntent(data);
            if (result != null && result.getCount() > 0) {
                // Here is object contains selected files names and path

                StringBuilder stringBuilder = new StringBuilder();
                ArrayList<String> arrayList = new ArrayList<>();
                for (int i = 0; i < result.getCount(); i++) {
                    stringBuilder.append(result.getNames().get(i));
                    if (i < result.getCount() - 1) {
                        stringBuilder.append(", ");
                    }
                    arrayList.add(result.getPath()+result.getNames().get(i));
                    Log.d("Path", ""+ result.getPath()+" names "+ result.getNames().get(i) +" name"+result);
                }


                if (arrayList.size() == 0){
                    AlertDialog.Builder dialog = new AlertDialog.Builder(Home.this);
                    dialog.setTitle("Empty Selection")
                            .setMessage("You must select atleast one file to be able to share to other devices, selection must not be empty.")
                            .setIcon(getResources().getDrawable(R.drawable.ic_alert_box))
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                } else {
                    String [] files = GetStringArray(arrayList);
                    Intent intent = new Intent(getApplicationContext(), SHAREthemActivity.class);
                    intent.putExtra(SHAREthemService.EXTRA_FILE_PATHS, files);

                    intent.putExtra(SHAREthemService.EXTRA_PORT, DEFAULT_PORT_OREO);

                    intent.putExtra(SHAREthemService.EXTRA_SENDER_NAME, "Sri");
                    startActivity(intent);
                }


            }
        }
    }

    public static String[] GetStringArray(ArrayList<String> arr) {

        // declaration and initialise String Array
        String str[] = new String[arr.size()];

        // ArrayList to Array Conversion
        for (int j = 0; j < arr.size(); j++) {

            // Assign each value to String array
            str[j] = String.valueOf(arr.get(j));
        }

        return str;
    }

    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        createFolders();
        //checkMobileData();
    }

    @Override
    public void onStart(){
        super.onStart();

        checkMobileData();
    }
    private void createFolders(){
        File fd_main = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge");
        File fd_backup = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Backup");
        File fd_files = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Files");
        File fd_photos = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Photos");
        File fd_audio = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Audio");
        File fd_video = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Videos");
        File fd_apps = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Apps");
        boolean success = true;
        if (!fd_main.exists()) {
            success = fd_main.mkdirs();
            if (success) {
                fd_backup.mkdirs();
                fd_files.mkdirs();
                fd_photos.mkdirs();
                fd_audio.mkdirs();
                fd_video.mkdirs();
                fd_apps.mkdirs();
                backup();
                Toast.makeText(Home.this, "Bridge folder has been successfully created.",Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(Home.this, "Bridge folder failed to be created.",Toast.LENGTH_SHORT).show();
            }
        } if (fd_main.exists()){
            Log.d("Folder Creation","Folder exists");
            backup();
        }
    }

    private void backup(){
        ApplicationInfo app = getApplicationContext().getApplicationInfo();
        String filePath = app.sourceDir;

        Log.d("Path", filePath+"");
        File sourceFile = new File(filePath);
        File destFile = new File(Environment.getExternalStorageDirectory().getPath()+"/Bridge/Backup/"+"Bridge.apk");

        File sourceLocation = new File (String.valueOf(sourceFile));

        // make sure your target location folder exists!
        File targetLocation = new File (String.valueOf(destFile));

        // just to take note of the location sources
        Log.v("Backup", "sourceLocation: " + sourceLocation);
        Log.v("Backup", "targetLocation: " + targetLocation);

        try {

            // 1 = move the file, 2 = copy the file
            int actionChoice = 2;

            // moving the file to another directory
            if(actionChoice==1){

                if(sourceLocation.renameTo(targetLocation)){
                    Log.v("Backup", "Move file successful.");
                }else{
                    Log.v("Backup", "Move file failed.");
                }

            }

            // we will copy the file
            else{

                // make sure the target file exists

                if(sourceLocation.exists()){

                    InputStream in = new FileInputStream(sourceLocation);
                    OutputStream out = new FileOutputStream(targetLocation);

                    // Copy the bits from instream to outstream
                    byte[] buf = new byte[1024];
                    int len;

                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    in.close();
                    out.close();

                    Log.v("Backup", "Copy file successful.");

                }else{
                    Log.v("Backup", "Copy file failed. Source file missing.");
                }

            }

        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Ion.with(Home.this)
                //.load(filePath)
                //.write(new File(Environment.getExternalStorageDirectory().getPath()+"/Bridge/Backup/"+"Bridge.apk"))
                //.setCallback(new FutureCallback<File>() {
                    //@Override
                    //public void onCompleted(Exception e, File file) {
                        //Log.d("Backup", "App backup done.");
                    //}
                //});
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, enable it to use Bridge.")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
            Context context = getApplicationContext();

            boolean settingsCanWrite = Settings.System.canWrite(context);
            if(!settingsCanWrite) {
                // If do not have write settings permission then open the Can modify system settings panel.

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Check")
                        .setMessage("Allow System Write Setting Permission.\n" +
                                "Click 'Ok' and find 'Bridge' app, tap the app in the list and toggle the 'switch button' then your'e ready to go. Thanks.")
                        .setCancelable(false)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                startActivity(intent);
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            }else {
                // If has permission then show an alert dialog with message.
                File fd_main = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge");
                File fd_backup = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Backup");
                File fd_files = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Files");
                File fd_photos = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Photos");
                File fd_audio = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Audio");
                File fd_video = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Videos");
                File fd_apps = new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Apps");
                boolean success = true;
                if (!fd_main.exists()) {
                    success = fd_main.mkdirs();
                    if (success) {
                        fd_backup.mkdirs();
                        fd_files.mkdirs();
                        fd_photos.mkdirs();
                        fd_audio.mkdirs();
                        fd_video.mkdirs();
                        fd_apps.mkdirs();
                        backup();
                        Toast.makeText(Home.this, "Bridge folder has been successfully created.",Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Home.this, "Bridge folder failed to be created.",Toast.LENGTH_SHORT).show();
                    }
                } if (fd_main.exists()){
                    Log.d("Folder Creation","Folder exists");
                    backup();
                }

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
                Intent i = new Intent(Home.this, Home.class);
                startActivity(i);
                Home.this.finish();
            }
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(Home.this,Donate.class);
            startActivity(i);
            Home.this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_donate) {
            Intent i = new Intent(Home.this,Donate.class);
            startActivity(i);
            Home.this.finish();
        } else if (id == R.id.nav_info) {
            Intent i = new Intent(Home.this,About.class);
            startActivity(i);
            Home.this.finish();
        } else if (id == R.id.nav_share) {
            ApplicationInfo app = getApplicationContext().getApplicationInfo();
            String filePath = Environment.getExternalStorageDirectory() + File.separator + "Bridge/Backup/"+"Bridge.apk";

            Intent intent = new Intent(Intent.ACTION_SEND);

            // MIME of .apk is "application/vnd.android.package-archive".
            // but Bluetooth does not accept this. Let's use "*/*" instead.
            intent.setType("*/*");


            // Append file and send Intent
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
            startActivity(Intent.createChooser(intent, "Share app via"));
        } else if (id == R.id.nav_exit) {
            Home.this.finish();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void sendApps(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        Intent intent4 = new Intent(this, NormalFilePickActivity.class);
        intent4.putExtra(Constant.MAX_NUMBER, 50);
        intent4.putExtra(NormalFilePickActivity.SUFFIX, new String[] {"apk"});
        startActivityForResult(intent4, Constant.REQUEST_CODE_PICK_FILE);
    }

    public void sendPhotos(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        Intent intent1 = new Intent(this, ImagePickActivity.class);
        //intent1.putExtra(IS_NEED_CAMERA, true);
        intent1.putExtra(Constant.MAX_NUMBER, 50);
        startActivityForResult(intent1, Constant.REQUEST_CODE_PICK_IMAGE);
    }

    public void sendVideos(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        Intent intent2 = new Intent(this, VideoPickActivity.class);
        //intent2.putExtra(IS_NEED_CAMERA, true);
        intent2.putExtra(Constant.MAX_NUMBER, 50);
        startActivityForResult(intent2, Constant.REQUEST_CODE_PICK_VIDEO);
    }

    public void sendAudio(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        Intent intent3 = new Intent(this, AudioPickActivity.class);
        //intent3.putExtra(IS_NEED_RECORDER, true);
        intent3.putExtra(Constant.MAX_NUMBER, 50);
        startActivityForResult(intent3, Constant.REQUEST_CODE_PICK_AUDIO);
    }

    public void sendDocs(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        Intent intent4 = new Intent(this, NormalFilePickActivity.class);
        intent4.putExtra(Constant.MAX_NUMBER, 50);
        intent4.putExtra(NormalFilePickActivity.SUFFIX, new String[] {"xlsx", "xls", "doc", "docx", "ppt", "pptx", "pdf"});
        startActivityForResult(intent4, Constant.REQUEST_CODE_PICK_FILE);
    }

    public void sendAllTypes(View view) {
        if (Utils.isShareServiceRunning(getApplicationContext())) {
            startActivity(new Intent(getApplicationContext(), SHAREthemActivity.class));
            return;
        }
        ExFilePicker exFilePicker = new ExFilePicker();
        exFilePicker.setChoiceType(ExFilePicker.ChoiceType.ALL);
        exFilePicker.setHideHiddenFilesEnabled(true);
        exFilePicker.setNewFolderButtonDisabled(false);
        exFilePicker.setUseFirstItemAsUpEnabled(true);
        exFilePicker.setQuitButtonEnabled(false);
        exFilePicker.start(Home.this, EX_FILE_PICKER_RESULT);

    }

    public void receiveFiles(View view) {
        HotspotControl hotspotControl = HotspotControl.getInstance(getApplicationContext());
        if (null != hotspotControl && hotspotControl.isEnabled()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Sender(Hotspot) mode is active. Please disable it to proceed with Receiver mode");
            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
            return;
        }
        startActivity(new Intent(getApplicationContext(), ScanActivity.class));
    }


}
