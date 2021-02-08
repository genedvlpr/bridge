package com.lib.bridge.qrcode;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;

import java.util.Map;

public class QRCode {
    public final static int DEFAULT_BG = 0xFFFFFFFF;
    public final static int DEFAULT_FG = 0xFF000000;
    public static int foreground = DEFAULT_FG;
    public static int background = DEFAULT_BG;
    public static int WIDTH = 400;
    public final static int HEIGHT = 400;

    private String str;

    public QRCode(QRCodeScheme codeObject) {
        this(codeObject.toString());
    }

    public QRCode(String string) {
        this.str = string;
    }

    @Override
    public String toString() {
        return "QRCode{" + "str='" + str + '\'' + '}';
    }

    public Bitmap getSimpleBitmap(@Nullable Map<EncodeHintType, Object> hints) throws WriterException {
        return getSimpleBitmap(DEFAULT_FG, hints);
    }

    public Bitmap getSimpleBitmap(int foregroundColor, @Nullable Map<EncodeHintType, Object> hints) throws WriterException {


        QRCodeEncoder qrCode = new QRCodeEncoder(str, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), HEIGHT,HEIGHT); // HEIGHT AND WIDTH

        return qrCode.encodeAsBitmap(foregroundColor, hints);
    }

    public Bitmap getSimpleBitmap(int foregroundColor, @Nullable Map<EncodeHintType, Object> hints, String type) throws WriterException {

        Log.d("checkgetSimpleBitmap",type);

        switch (type)
        {
            case "QR_CODE":
            case "AZTEC_CODE":
            case "DATA_MATRIX":
                WIDTH = 400;
                break;

            default:
                WIDTH = 800;
                break;
        }


        QRCodeEncoder qrCode = new QRCodeEncoder(str, null,
                Contents.Type.TEXT, type, WIDTH, HEIGHT); // HEIGHT AND WIDTH

        return qrCode.encodeAsBitmap(foregroundColor, hints);
    }


}
