package com.codencode.opencvtest;

import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

public class OcrManager {

    TessBaseAPI baseAPI = null;
    public void initApi()
    {
        baseAPI = new TessBaseAPI();
        String dataPath = MainApplication.instance.getTessDataParentDirectory();
        baseAPI.init(dataPath , "eng");
    }

    public String startRecognition(Bitmap bitmap)
    {
        if(baseAPI == null)
            initApi();
        baseAPI.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST , "x0123456789+-()");
        baseAPI.setImage(bitmap);
        return baseAPI.getUTF8Text();
    }
}
