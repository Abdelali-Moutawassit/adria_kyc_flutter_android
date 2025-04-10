package com.adria.adria_kyc_integration;

import android.app.Application;


public class AdriaApp extends Application {

    private static AdriaApp adriaApp;


    public static AdriaApp getInstance() {
        if (adriaApp == null)
            adriaApp = new AdriaApp();

        return adriaApp;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        adriaApp = this;

    }
}
