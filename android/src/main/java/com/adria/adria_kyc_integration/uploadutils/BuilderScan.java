package com.adria.adria_kyc_integration.uploadutils;


import com.adria.adria_kyc_integration.AdriaApp;
import com.readystatesoftware.chuck.ChuckInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class BuilderScan {

    public static Retrofit retrofit;

    public static Retrofit build() {
        if (retrofit == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.addInterceptor(new ChuckInterceptor(AdriaApp.getInstance()));
            builder.addInterceptor(interceptor);
            builder.readTimeout(5, TimeUnit.MINUTES);
            builder.writeTimeout(5, TimeUnit.MINUTES);
            builder.connectTimeout(5, TimeUnit.MINUTES);

            OkHttpClient client = builder.build();
//            retrofit = new Retrofit.Builder().baseUrl("http://51.91.229.18:5005/")
//            retrofit = new Retrofit.Builder().baseUrl("http://51.91.193.152:5005/")
            retrofit = new Retrofit.Builder().baseUrl("http://51.89.99.137:5026/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    private void addDebugInterceptors(OkHttpClient.Builder builder) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        builder.addInterceptor(new ChuckInterceptor(AdriaApp.getInstance()));
        builder.addInterceptor(interceptor);
    }



}
