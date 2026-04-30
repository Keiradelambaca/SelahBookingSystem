package com.example.selahbookingsystem.network.api;

import android.content.Context;

import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ServicesApi;
import com.example.selahbookingsystem.network.service.SupabaseRestService;

import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String SUPABASE_URL = "https://hdcuyztqiudjuovriyai.supabase.co/";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhkY3V5enRxaXVkanVvdnJpeWFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0Nzk5NDcsImV4cCI6MjA3NjA1NTk0N30.TXwNemGU00BStqwwXKLtgGyNF1AIJmQUKbSeqP73Uyw";

    private static Retrofit retrofit;
    private static Context appContext;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit get() {
        if (retrofit == null) {

            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();

                String jwt = null;
                if (appContext != null) {
                    jwt = TokenStore.getAccessToken(appContext);
                }

                Request.Builder builder = original.newBuilder()
                        .addHeader("apikey", SUPABASE_ANON_KEY);

                if (jwt != null && !jwt.isEmpty()) {
                    builder.addHeader("Authorization", "Bearer " + jwt);
                } else {
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
                    .baseUrl(SUPABASE_URL)
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

    public static SupabaseRestService supabase() {
        return get().create(SupabaseRestService.class);
    }

    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseAnonKey() {
        return SUPABASE_ANON_KEY;
    }
}
