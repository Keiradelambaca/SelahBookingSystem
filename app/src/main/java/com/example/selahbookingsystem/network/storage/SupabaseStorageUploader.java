package com.example.selahbookingsystem.network.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseStorageUploader {

    private static final String TAG = "StorageUploader";

    private static final String BUCKET_NAME = "booking-photos";

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String message, @Nullable Throwable throwable);
    }

    public static void uploadImage(
            @NonNull Context context,
            @NonNull Uri imageUri,
            @NonNull String folderName,
            @NonNull UploadCallback callback
    ) {
        String accessToken = TokenStore.getAccessToken(context);
        if (accessToken == null || accessToken.trim().isEmpty()) {
            callback.onError("Missing access token", null);
            return;
        }

        byte[] bytes;
        String mimeType;
        String extension;

        try {
            ContentResolver resolver = context.getContentResolver();
            mimeType = resolver.getType(imageUri);
            if (mimeType == null || mimeType.trim().isEmpty()) {
                mimeType = "image/jpeg";
            }

            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null || extension.trim().isEmpty()) {
                extension = "jpg";
            }

            bytes = readAllBytes(resolver, imageUri);
            if (bytes == null || bytes.length == 0) {
                callback.onError("Selected image is empty", null);
                return;
            }
        } catch (Exception e) {
            callback.onError("Failed to read selected image", e);
            return;
        }

        String userId = TokenStore.getUserId(context);
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }

        String fileName = UUID.randomUUID().toString() + "." + extension.toLowerCase(Locale.ROOT);
        String objectPath = folderName + "/" + userId + "/" + fileName;

        String uploadUrl = ApiClient.getSupabaseUrl()
                + "/storage/v1/object/"
                + BUCKET_NAME
                + "/"
                + objectPath;

        RequestBody requestBody = RequestBody.create(
                bytes,
                MediaType.parse(mimeType)
        );

        Request request = new Request.Builder()
                .url(uploadUrl)
                .post(requestBody)
                .headers(new Headers.Builder()
                        .add("apikey", ApiClient.getSupabaseAnonKey())
                        .add("Authorization", "Bearer " + accessToken)
                        .add("Content-Type", mimeType)
                        .add("x-upsert", "false")
                        .build())
                .build();

        OkHttpClient client = new OkHttpClient();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError("Upload failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Upload failed code=" + response.code() + " body=" + body);
                    callback.onError("Upload failed: " + response.code(), null);
                    return;
                }

                String publicUrl = ApiClient.getSupabaseUrl()
                        + "/storage/v1/object/public/"
                        + BUCKET_NAME
                        + "/"
                        + objectPath;

                callback.onSuccess(publicUrl);
            }
        });
    }

    private static byte[] readAllBytes(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream inputStream = resolver.openInputStream(uri);
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                throw new IOException("Could not open input stream");
            }

            byte[] data = new byte[8192];
            int nRead;

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        }
    }
}
