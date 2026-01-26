package com.example.selahbookingsystem.data.store;

import androidx.annotation.NonNull;

import com.example.selahbookingsystem.data.dto.LikeDto;
import com.example.selahbookingsystem.data.dto.LookDto;
import com.example.selahbookingsystem.data.model.LookItem;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LooksRepository {

    public interface ResultCallback<T> {
        void onSuccess(T data);
        void onError(String message);
    }

    private final SupabaseService api;

    public LooksRepository() {
        this.api = ApiClient.get().create(SupabaseService.class);
    }

    public void loadExploreFeed(
            @NonNull String userId,
            int limit,
            @NonNull ResultCallback<List<LookItem>> cb
    ) {
        // 1) load likes for this user
        api.getLikesForUser(
                "look_id",
                "eq." + userId,
                "created_at.desc"
        ).enqueue(new Callback<List<LikeDto>>() {
            @Override
            public void onResponse(Call<List<LikeDto>> call, Response<List<LikeDto>> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    cb.onError("Failed to load likes: " + resp.code());
                    return;
                }

                Set<String> likedIds = new HashSet<>();
                for (LikeDto l : resp.body()) likedIds.add(l.look_id);

                // 2) load looks
                api.getLooks(
                        "id,provider_id,image_url,title,description,tags,created_at",
                        "created_at.desc",
                        limit,
                        0
                ).enqueue(new Callback<List<LookDto>>() {
                    @Override
                    public void onResponse(Call<List<LookDto>> call2, Response<List<LookDto>> resp2) {
                        if (!resp2.isSuccessful() || resp2.body() == null) {
                            cb.onError("Failed to load looks: " + resp2.code());
                            return;
                        }

                        List<LookItem> items = new ArrayList<>();
                        for (LookDto d : resp2.body()) {
                            LookItem item = LookItem.fromDto(d);
                            item.setLiked(likedIds.contains(item.getId()));
                            items.add(item);
                        }

                        cb.onSuccess(items);
                    }

                    @Override
                    public void onFailure(Call<List<LookDto>> call2, Throwable t) {
                        cb.onError("Looks error: " + t.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(Call<List<LikeDto>> call, Throwable t) {
                cb.onError("Likes error: " + t.getMessage());
            }
        });
    }

    public void like(@NonNull String userId, @NonNull String lookId, @NonNull ResultCallback<Void> cb) {
        api.like(Map.of(
                "user_id", userId,
                "look_id", lookId
        )).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> resp) {
                if (resp.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Like failed: " + resp.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError("Like failed: " + t.getMessage());
            }
        });
    }

    public void unlike(@NonNull String userId, @NonNull String lookId, @NonNull ResultCallback<Void> cb) {
        api.unlike(
                "eq." + userId,
                "eq." + lookId
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> resp) {
                if (resp.isSuccessful()) cb.onSuccess(null);
                else cb.onError("Unlike failed: " + resp.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                cb.onError("Unlike failed: " + t.getMessage());
            }
        });
    }
}