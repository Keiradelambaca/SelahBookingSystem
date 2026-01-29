package com.example.selahbookingsystem.network.api;

import android.content.Context;

import com.example.selahbookingsystem.data.store.TokenStore;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.selahbookingsystem.network.api.ServicesApi;


public class ApiClient {

    private static final String SUPABASE_URL = "https://hdcuyztqiudjuovriyai.supabase.co/";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhkY3V5enRxaXVkanVvdnJpeWFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0Nzk5NDcsImV4cCI6MjA3NjA1NTk0N30.TXwNemGU00BStqwwXKLtgGyNF1AIJmQUKbSeqP73Uyw";

    private static Retrofit retrofit;
    private static Context appContext;

    /**
     * Call once from your Application class (recommended),
     * or before the first ApiClient.get() call.
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit get() {
        if (retrofit == null) {

            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();

                // Pull the saved user JWT (set during login: TokenStore.save(...))
                String jwt = null;
                if (appContext != null) {
                    jwt = TokenStore.getAccessToken(appContext); // <-- must exist in your TokenStore
                }

                Request.Builder builder = original.newBuilder()
                        .addHeader("apikey", SUPABASE_ANON_KEY);

                // Use real user JWT when available (required for RLS + likes + provider uploads)
                if (jwt != null && !jwt.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + jwt);
                } else {
                    // Fallback (read-only / not logged in). RLS writes will fail without JWT.
                    builder.addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY);
                }

                return chain.proceed(builder.build());
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

    public static BookingsApi bookings() {
        return get().create(BookingsApi.class);
    }

    public static ServicesApi services() {
        return get().create(ServicesApi.class);
    }


}