/*
 * Copyright 2017 Srihari Yachamaneni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lib.bridge.receiver;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;
import com.lib.bridge.R;
import com.lib.bridge.utils.RecyclerViewArrayAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lists all files available to download by making network calls using {@link ContactSenderAPITask}
 * <p>
 * Functionalities include:
 * <ul>
 * <li>Adds file downloads to {@link DownloadManager}'s Queue</li>
 * * <li>Checks Sender API availability and throws error after certain retry limit</li>
 * </ul>
 * <p>
 * Created by Sri on 21/12/16.
 */

public class FilesListingFragment extends Fragment {
    private static final String TAG = "FilesListingFragment";

    public static final String PATH_FILES = "http://%s:%s/files";
    public static final String PATH_STATUS = "http://%s:%s/status";
    public static final String PATH_FILE_DOWNLOAD = "http://%s:%s/file/%s";

    private String mSenderIp = null, mSenderSSID;
    private ContactSenderAPITask mUrlsTask;
    private ContactSenderAPITask mStatusCheckTask;

    private String mPort, mSenderName;

    static final int CHECK_SENDER_STATUS = 100;
    static final int SENDER_DATA_FETCH = 101;

    RecyclerView mFilesListing;
    ProgressBar mLoading, progressbar;
    TextView mEmptyListText, tv_progress, count;

    private SenderFilesListingAdapter mFilesAdapter;

    private UiUpdateHandler uiUpdateHandler;

    private static final int SENDER_DATA_FETCH_RETRY_LIMIT = 3;
    private int senderDownloadsFetchRetry = SENDER_DATA_FETCH_RETRY_LIMIT, senderStatusCheckRetryLimit = SENDER_DATA_FETCH_RETRY_LIMIT;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_files_listing, null);
        mFilesListing = (RecyclerView) v.findViewById(R.id.files_list);
        mLoading = (ProgressBar) v.findViewById(R.id.loading);
        mEmptyListText = (TextView) v.findViewById(R.id.empty_listing_text);
        mEmptyListText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmptyListText.setVisibility(View.GONE);
                mLoading.setVisibility(View.VISIBLE);
                fetchSenderFiles();
            }
        });

        count = v.findViewById(R.id.count);
        progressbar = v.findViewById(R.id.progress);
        tv_progress = v.findViewById(R.id.tv_progress);
        mFilesListing.setLayoutManager(new LinearLayoutManager(getActivity()));
        //mFilesListing.addItemDecoration(new DividerItemDecoration(getResources().getDrawable(R.drawable.list_divider)));
        mFilesAdapter = new SenderFilesListingAdapter(new ArrayList<String>());
        mFilesListing.setAdapter(mFilesAdapter);
        uiUpdateHandler = new UiUpdateHandler(this);
        return v;
    }

    public static FilesListingFragment getInstance(String senderIp, String ssid, String senderName, String port) {
        FilesListingFragment fragment = new FilesListingFragment();
        Bundle data = new Bundle();
        data.putString("senderIp", senderIp);
        data.putString("ssid", ssid);
        data.putString("name", senderName);
        data.putString("port", port);
        fragment.setArguments(data);
        return fragment;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        if (null != getArguments()) {
            mSenderIp = getArguments().getString("senderIp");
            mSenderSSID = getArguments().getString("ssid");
            mPort = getArguments().getString("port");
            mSenderName = getArguments().getString("name");
            Log.d(TAG, "sender ip: " + mSenderIp);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchSenderFiles();
        checkSenderAPIAvailablity();
    }

    private void fetchSenderFiles() {
        //mLoading.setVisibility(View.VISIBLE);
        if (null != mUrlsTask)
            mUrlsTask.cancel(true);
        mUrlsTask = new ContactSenderAPITask(SENDER_DATA_FETCH);
        mUrlsTask.execute(String.format(PATH_FILES, mSenderIp, mPort));
    }

    private void checkSenderAPIAvailablity() {
        if (null != mStatusCheckTask)
            mStatusCheckTask.cancel(true);
        mStatusCheckTask = new ContactSenderAPITask(CHECK_SENDER_STATUS);
        mStatusCheckTask.execute(String.format(PATH_STATUS, mSenderIp, mPort));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mUrlsTask)
            mUrlsTask.cancel(true);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != uiUpdateHandler)
            uiUpdateHandler.removeCallbacksAndMessages(null);
        if (null != mStatusCheckTask)
            mStatusCheckTask.cancel(true);
    }

    public String getSenderSSID() {
        return mSenderSSID;
    }

    public String getSenderIp() {
        return mSenderIp;
    }

    private void loadListing(String contentAsString) {
        Type collectionType = new TypeToken<List<String>>() {
        }.getType();
        ArrayList<String> files = new Gson().fromJson(contentAsString, collectionType);
        //mLoading.setVisibility(View.GONE);
        if (null == files || files.size() == 0) {
            mEmptyListText.setText("No Downloads found.\n Tap to Retry");
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
            mFilesAdapter.updateData(files);
        }
    }

    private void onDataFetchError() {
        //mLoading.setVisibility(View.GONE);
        //mEmptyListText.setVisibility(View.VISIBLE);
        //mEmptyListText.setText("Error occurred while fetching data.\n Tap to Retry");
    }

    @SuppressLint("ObsoleteSdkInt")
    private void postDownloadRequestToDM(Uri uri, final String fileName) {

        Log.d("URI", String.valueOf(uri));
        Log.d("File Name", fileName);

        if (fileName.contains(".jpeg")||fileName.contains(".jpg") ||fileName.contains(".tiff")||fileName.contains(".bmp") ||fileName.contains(".png") ||fileName.contains(".gif") ||fileName.contains(".psd") ||fileName.contains(".eps") ||fileName.contains(".ai") ||fileName.contains(".raw")){
            Ion.with(Objects.requireNonNull(getContext()))
                    .load(String.valueOf(uri))
                    // have a ProgressBar get updated automatically with the percent
                    .progressBar(progressbar)
                    // and a ProgressDialog
                    .progressHandler(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        tv_progress.setText(fileName);
                        count.setText(downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Photos/"+fileName))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                        }
                    });
        } else if (fileName.contains(".mp4")||fileName.contains(".mpeg") ||fileName.contains(".avi")||fileName.contains(".vob") ||fileName.contains(".wmv")||fileName.contains(".flv") ||fileName.contains(".ogg")||fileName.contains(".m4p") ||fileName.contains(".mpv")||fileName.contains(".mpe") ||fileName.contains(".webm")){
            Ion.with(Objects.requireNonNull(getContext()))
                    .load(String.valueOf(uri))
                    // have a ProgressBar get updated automatically with the percent
                    .progressBar(progressbar)
                    // and a ProgressDialog
                    .progressHandler(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        tv_progress.setText(fileName);
                        count.setText(downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Videos/"+fileName))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                        }
                    });
        } else if (fileName.contains("apk")){
            Ion.with(Objects.requireNonNull(getContext()))
                    .load(String.valueOf(uri))
                    // have a ProgressBar get updated automatically with the percent
                    .progressBar(progressbar)
                    // and a ProgressDialog
                    .progressHandler(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        tv_progress.setText(fileName);
                        count.setText(downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Apps/"+fileName))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                        }
                    });
        } else if (fileName.contains("PCM")||fileName.contains("PCM")||fileName.contains("pcm")||fileName.contains("wav")||fileName.contains("aiff")||fileName.contains("mp3")||fileName.contains("aac")||fileName.contains("ogg")||fileName.contains("wma")||fileName.contains("flac")){
            Ion.with(Objects.requireNonNull(getContext()))
                    .load(String.valueOf(uri))
                    // have a ProgressBar get updated automatically with the percent
                    .progressBar(progressbar)
                    // and a ProgressDialog
                    .progressHandler(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        tv_progress.setText(fileName);
                        count.setText(downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Audio/"+fileName))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                        }
                    });
        } else {
            Ion.with(Objects.requireNonNull(getContext()))
                    .load(String.valueOf(uri))
                    // have a ProgressBar get updated automatically with the percent
                    .progressBar(progressbar)
                    // and a ProgressDialog
                    .progressHandler(new ProgressCallback() {@Override
                    public void onProgress(long downloaded, long total) {
                        tv_progress.setText(fileName);
                        count.setText(downloaded + " / " + total);
                    }
                    })
                    .write(new File(Environment.getExternalStorageDirectory() + File.separator + "Bridge/Files/"+fileName))
                    .setCallback(new FutureCallback<File>() {
                        @Override
                        public void onCompleted(Exception e, File file) {
                            // download done...
                            // do stuff with the File or error
                        }
                    });
        }


    }

    private class SenderFilesListingAdapter extends RecyclerViewArrayAdapter<String, SenderFilesListItemHolder> {
        SenderFilesListingAdapter(List<String> objects) {
            super(objects);
        }

        void updateData(List<String> objects) {
            clear();
            mObjects = objects;
            notifyDataSetChanged();
        }

        @Override
        public SenderFilesListItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).
                    inflate(R.layout.listitem_file, parent, false);
            return new SenderFilesListItemHolder(itemView);
        }

        @Override
        public void onBindViewHolder(SenderFilesListItemHolder holder, int position) {
            final String senderFile = mObjects.get(position);
            holder.itemView.setTag(senderFile);
            final String fileName = senderFile.substring(senderFile.lastIndexOf('/') + 1, senderFile.length());
            holder.title.setText(fileName);

            if (mObjects.get(position).contains(".jpeg")||mObjects.get(position).contains(".jpg")
                    ||mObjects.get(position).contains(".tiff")||mObjects.get(position).contains(".bmp")
                    ||mObjects.get(position).contains(".png")
                    ||mObjects.get(position).contains(".gif")
                    ||mObjects.get(position).contains(".psd")
                    ||mObjects.get(position).contains(".eps")
                    ||mObjects.get(position).contains(".ai")
                    ||mObjects.get(position).contains(".raw")){
                holder.file_type.setText("Image");
                holder.file_img.setImageResource(R.drawable.ic_google_photos);
            } else if (mObjects.get(position).contains(".mp4")||mObjects.get(position).contains(".mpeg")
                    ||mObjects.get(position).contains(".avi")||mObjects.get(position).contains(".vob")
                    ||mObjects.get(position).contains(".wmv")||mObjects.get(position).contains(".flv")
                    ||mObjects.get(position).contains(".ogg")||mObjects.get(position).contains(".m4p")
                    ||mObjects.get(position).contains(".mpv")||mObjects.get(position).contains(".mpe")
                    ||mObjects.get(position).contains(".webm")){
                holder.file_type.setText("Video");
                holder.file_img.setImageResource(R.drawable.ic_filmstrip);
            } else if (mObjects.get(position).contains("apk")){
                holder.file_type.setText("App");
                holder.file_img.setImageResource(R.drawable.ic_android);
            } else {
                holder.file_type.setText("Files");
                holder.file_img.setImageResource(R.drawable.ic_file_check);
            }
            holder.download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    postDownloadRequestToDM(Uri.parse(String.format(PATH_FILE_DOWNLOAD, mSenderIp, mPort, mObjects.indexOf(senderFile))), fileName);
                    //Toast.makeText(getActivity(), "Downloading " + fileName + "", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    static class SenderFilesListItemHolder extends RecyclerView.ViewHolder {
        TextView title, file_type;
        ImageButton download;
        ImageView file_img;

        SenderFilesListItemHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.sender_list_item_name);
            download = itemView.findViewById(R.id.sender_list_start_download);
            file_img = itemView.findViewById(R.id.file_img);
            file_type = itemView.findViewById(R.id.file_type);
        }
    }


    /**
     * Performs network calls to fetch data/status from Sender.
     * Retries on error for times bases on values of {@link FilesListingFragment#senderDownloadsFetchRetry}
     */
    private class ContactSenderAPITask extends AsyncTask<String, Void, String> {

        int mode;
        boolean error;

        ContactSenderAPITask(int mode) {
            this.mode = mode;
        }

        @Override
        protected String doInBackground(String... urls) {
            error = false;
            try {
                return downloadDataFromSender(urls[0]);
            } catch (IOException e) {
                e.printStackTrace();
                error = true;
                Log.e(TAG, "Exception: " + e.getMessage());
                return null;
            }
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            switch (mode) {
                case SENDER_DATA_FETCH:
                    if (error) {
                        if (senderDownloadsFetchRetry >= 0) {
                            --senderDownloadsFetchRetry;
                            if (getView() == null || getActivity() == null || null == uiUpdateHandler)
                                return;
                            uiUpdateHandler.removeMessages(SENDER_DATA_FETCH);
                            uiUpdateHandler.sendMessageDelayed(uiUpdateHandler.obtainMessage(mode), 800);
                            return;
                        } else senderDownloadsFetchRetry = SENDER_DATA_FETCH_RETRY_LIMIT;
                        if (null != getView())
                            onDataFetchError();
                    } else if (null != getView())
                        loadListing(result);
                    else Log.e(TAG, "fragment may have been removed, File fetch");
                    break;
                case CHECK_SENDER_STATUS:
                    if (error) {
                        if (senderStatusCheckRetryLimit > 1) {
                            --senderStatusCheckRetryLimit;
                            uiUpdateHandler.removeMessages(CHECK_SENDER_STATUS);
                            uiUpdateHandler.sendMessageDelayed(uiUpdateHandler.obtainMessage(CHECK_SENDER_STATUS), 800);
                        } else if (getActivity() instanceof ReceiverActivity) {
                            senderStatusCheckRetryLimit = SENDER_DATA_FETCH_RETRY_LIMIT;
                            ((ReceiverActivity) getActivity()).resetSenderSearch();
                            Toast.makeText(getActivity(), getString(R.string.p2p_receiver_error_sender_disconnected), Toast.LENGTH_SHORT).show();
                        } else
                            Log.e(TAG, "Activity is not instance of ReceiverActivity");
                    } else if (null != getView()) {
                        senderStatusCheckRetryLimit = SENDER_DATA_FETCH_RETRY_LIMIT;
                        uiUpdateHandler.removeMessages(CHECK_SENDER_STATUS);
                        uiUpdateHandler.sendMessageDelayed(uiUpdateHandler.obtainMessage(CHECK_SENDER_STATUS), 1000);
                    } else
                        Log.e(TAG, "fragment may have been removed: Sender api check");
                    break;
            }

        }

        private String downloadDataFromSender(String apiUrl) throws IOException {
            InputStream is = null;
            try {
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(1200000 /* milliseconds */);
                conn.setConnectTimeout(1200000  /* milliseconds */);

                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
//                int response =
                //conn.getResponseCode();
//                Log.d(TAG, "The response is: " + response);
                is = conn.getInputStream();
                // Convert the InputStream into a string
                return readIt(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        private String readIt(InputStream stream) throws IOException {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(stream, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                stream.close();
            }
            return writer.toString();
        }
    }

    private static class UiUpdateHandler extends Handler {
        WeakReference<FilesListingFragment> mFragment;

        UiUpdateHandler(FilesListingFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            FilesListingFragment fragment = mFragment.get();
            if (null == mFragment)
                return;
            switch (msg.what) {
                case CHECK_SENDER_STATUS:
                    fragment.checkSenderAPIAvailablity();
                    break;
                case SENDER_DATA_FETCH:
                    fragment.fetchSenderFiles();
                    break;
            }
        }
    }

}
