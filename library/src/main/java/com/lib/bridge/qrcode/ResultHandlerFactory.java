package com.lib.bridge.qrcode;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

public final class ResultHandlerFactory {
    private ResultHandlerFactory() {
    }

    public static ResultHandler makeResultHandler(AppCompatActivity activity, Result rawResult) {
        ParsedResult result = parseResult(rawResult);
        switch (result.getType()) {

            default:
                return new WifiResultHandler(activity, result);

        }
    }

    private static ParsedResult parseResult(Result rawResult) {
        return ResultParser.parseResult(rawResult);
    }
}
