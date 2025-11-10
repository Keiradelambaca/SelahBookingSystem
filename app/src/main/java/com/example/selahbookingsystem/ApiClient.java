package com.example.selahbookingsystem;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String SUPABASE_URL = "https://hdcuyztqiudjuovriyai.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhkY3V5enRxaXVkanVvdnJpeWFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0Nzk5NDcsImV4cCI6MjA3NjA1NTk0N30.TXwNemGU00BStqwwXKLtgGyNF1AIJmQUKbSeqP73Uyw";          // <- your anon key

    private static Retrofit retrofit;

    public static Retrofit get() {
        if (retrofit == null) {
            // Add Supabase headers to every request
            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();
                Request req = original.newBuilder()
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .build();
                return chain.proceed(req);
            };

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL + "/") // trailing slash required
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }
}