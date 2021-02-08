package com.lib.bridge.qrcode;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.lib.bridge.R;

public class WifiQR extends Fragment {


    private UpdateView mCallback;
    private String security;

    public static WifiQR newInstance( ) {
        return new WifiQR();
    }
    public WifiQR() {
    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_wifi_input, container, false);

        //Button button =  view.findViewById(R.id.create_button);

        //RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.network_type_input);

        TextView ssidText = ((TextView) view.findViewById(R.id.ssid_input));
        TextView passwordText = ((TextView) view.findViewById(R.id.password_input));

        Bundle bundle = getArguments();
        String ssid = bundle.getString("ssid");
        String password = bundle.getString("password");

        security = "WPA";

        WiFi wifi = new WiFi()
                .setSsid(ssid)
                .setPassword(password)
                .setType(security);
        mCallback.showQr(wifi.toString(), BarcodeFormat.QR_CODE.toString());


        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Activity activity = getActivity();
        try {
            mCallback = (UpdateView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement HeadlineListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

}