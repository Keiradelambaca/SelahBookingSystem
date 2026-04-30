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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.os.Build;

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
            Log.e(TAG, "uploadImage: missing access token");
            callback.onError("Missing access token", null);
            return;
        }

        byte[] bytes;
        String mimeType;
        String extension;

        try {
            ContentResolver resolver = context.getContentResolver();

            Log.d(TAG, "uploadImage: selected imageUri=" + imageUri);

            String detectedMimeType = resolver.getType(imageUri);
            if (detectedMimeType == null || detectedMimeType.trim().isEmpty()) {
                detectedMimeType = "image/jpeg";
            }

            Log.d(TAG, "uploadImage: detectedMimeType=" + detectedMimeType);

            boolean supportedDirectly =
                    "image/jpeg".equalsIgnoreCase(detectedMimeType) ||
                            "image/png".equalsIgnoreCase(detectedMimeType) ||
                            "image/gif".equalsIgnoreCase(detectedMimeType) ||
                            "image/webp".equalsIgnoreCase(detectedMimeType);

            if (supportedDirectly) {
                mimeType = detectedMimeType;

                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension == null || extension.trim().isEmpty()) {
                    extension = "jpg";
                }

                bytes = readAllBytes(resolver, imageUri);
                if (bytes == null || bytes.length == 0) {
                    Log.e(TAG, "uploadImage: selected image is empty");
                    callback.onError("Selected image is empty", null);
                    return;
                }

                Log.d(TAG, "uploadImage: using original bytes");
            } else {
                Log.d(TAG, "uploadImage: unsupported source mimeType, converting to JPEG");

                bytes = convertUriToJpegBytes(context, imageUri);
                mimeType = "image/jpeg";
                extension = "jpg";

                if (bytes == null || bytes.length == 0) {
                    Log.e(TAG, "uploadImage: JPEG conversion returned empty bytes");
                    callback.onError("Failed to convert selected image to JPEG", null);
                    return;
                }
            }

            Log.d(TAG, "uploadImage: final mimeType=" + mimeType);
            Log.d(TAG, "uploadImage: final extension=" + extension);
            Log.d(TAG, "uploadImage: bytes.length=" + bytes.length);

        } catch (Exception e) {
            Log.e(TAG, "uploadImage: failed to read/convert selected image", e);
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

        String publicUrl = ApiClient.getSupabaseUrl()
                + "/storage/v1/object/public/"
                + BUCKET_NAME
                + "/"
                + objectPath;

        Log.d(TAG, "uploadImage: objectPath=" + objectPath);
        Log.d(TAG, "uploadImage: uploadUrl=" + uploadUrl);
        Log.d(TAG, "uploadImage: publicUrl=" + publicUrl);

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
                Log.e(TAG, "uploadImage: upload request failed", e);
                callback.onError("Upload failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                Log.d(TAG, "uploadImage: upload response code=" + response.code());
                Log.d(TAG, "uploadImage: upload response body=" + body);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "uploadImage: upload failed code=" + response.code() + " body=" + body);
                    callback.onError("Upload failed: " + response.code(), null);
                    return;
                }

                Log.d(TAG, "uploadImage: upload success, returning publicUrl=" + publicUrl);
                callback.onSuccess(publicUrl);
            }
        });
    }

    private static byte[] convertUriToJpegBytes(@NonNull Context context, @NonNull Uri uri) throws IOException {
        Bitmap bitmap = null;
        IOException lastError = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source);
            } catch (IOException e) {
                Log.e(TAG, "convertUriToJpegBytes: ImageDecoder failed", e);
                lastError = e;
            }
        }

        if (bitmap == null) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream == null) {
                    throw new IOException("Could not open input stream for JPEG conversion");
                }
                bitmap = BitmapFactory.decodeStream(inputStream);
            } catch (IOException e) {
                Log.e(TAG, "convertUriToJpegBytes: BitmapFactory decode failed", e);
                if (lastError == null) {
                    lastError = e;
                }
            }
        }

        if (bitmap == null) {
            throw new IOException(
                    "Failed to decode image for JPEG conversion. The selected image may be an unsupported HEIF/HEIC variant on this emulator.",
                    lastError
            );
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream);

        if (!compressed) {
            throw new IOException("Failed to compress image as JPEG");
        }

        return outputStream.toByteArray();
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
