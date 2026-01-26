package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.data.dto.LookDto;
import com.example.selahbookingsystem.data.model.LookItem;
import com.example.selahbookingsystem.data.store.LooksRepository;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerLookDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView img;
    private TextView tvTitle, tvDesc;
    private LinearLayout addonsContainer;
    private ChipGroup chipTags;
    private MaterialButton btnBook;

    private SupabaseService supabase;
    private LooksRepository looksRepo;

    private String userId;
    private String lookId;

    private boolean isLiked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_look_detail);

        toolbar = findViewById(R.id.toolbarLookDetail);
        img = findViewById(R.id.imgLookDetail);
        tvTitle = findViewById(R.id.tvLookTitle);
        tvDesc = findViewById(R.id.tvLookDescription);
        addonsContainer = findViewById(R.id.addonsContainer);
        chipTags = findViewById(R.id.chipGroupTags);
        btnBook = findViewById(R.id.btnBookThisLook);

        toolbar.setNavigationOnClickListener(v -> finish());

        // ✅ Your menu is menu_explore, and the heart id is action_liked
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_liked) {
                toggleLike(item);
                return true;
            }
            return false;
        });

        supabase = ApiClient.get().create(SupabaseService.class);
        looksRepo = new LooksRepository();

        SharedPreferences prefs = getSharedPreferences("selah_auth", MODE_PRIVATE);
        userId = prefs.getString("auth_user_id", null);

        lookId = getIntent().getStringExtra("look_id");
        if (lookId == null) {
            Toast.makeText(this, "Missing look id.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBook.setOnClickListener(v -> startBookingFlow());

        loadLook();
        loadInitialLikeState();
    }

    private void loadLook() {
        supabase.getLookById(
                "id,provider_id,image_url,title,description,tags,created_at",
                "eq." + lookId
        ).enqueue(new Callback<List<LookDto>>() {
            @Override
            public void onResponse(Call<List<LookDto>> call, Response<List<LookDto>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    Toast.makeText(CustomerLookDetailActivity.this, "Look not found.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                LookDto d = response.body().get(0);

                Glide.with(CustomerLookDetailActivity.this)
                        .load(d.image_url)
                        .centerCrop()
                        .into(img);

                tvTitle.setText(d.title != null ? d.title : "Look");
                tvDesc.setText(d.description != null ? d.description : "");

                renderTags(d.tags);
                renderAddonsPlaceholder();
            }

            @Override
            public void onFailure(Call<List<LookDto>> call, Throwable t) {
                Toast.makeText(CustomerLookDetailActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadInitialLikeState() {
        if (userId == null) return;

        looksRepo.loadExploreFeed(userId, 200, new LooksRepository.ResultCallback<List<LookItem>>() {
            @Override
            public void onSuccess(List<LookItem> data) {
                for (LookItem li : data) {
                    if (lookId.equals(li.getId())) {
                        isLiked = li.isLiked();
                        runOnUiThread(() -> {
                            MenuItem heart = toolbar.getMenu().findItem(R.id.action_liked);
                            if (heart != null) {
                                heart.setIcon(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                            }
                        });
                        return;
                    }
                }
            }

            @Override
            public void onError(String message) {
                // ignore initial like state errors
            }
        });
    }

    private void renderTags(List<String> tags) {
        chipTags.removeAllViews();
        if (tags == null || tags.isEmpty()) return;

        for (String t : tags) {
            Chip chip = new Chip(this);
            chip.setText(t);
            chip.setClickable(false);
            chip.setCheckable(false);
            chipTags.addView(chip);
        }
    }

    private void renderAddonsPlaceholder() {
        addonsContainer.removeAllViews();
        addAddonRow("Extra length");
        addAddonRow("Chrome powder");
        addAddonRow("3D charms");
    }

    private void addAddonRow(String text) {
        TextView tv = new TextView(this);
        tv.setText("• " + text);
        tv.setTextSize(14f);
        tv.setTextColor(0xFF6B6B6B);
        tv.setPadding(0, 6, 0, 6);
        addonsContainer.addView(tv);
    }

    private void toggleLike(MenuItem item) {
        if (userId == null) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        isLiked = !isLiked;
        item.setIcon(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        if (isLiked) {
            looksRepo.like(userId, lookId, new LooksRepository.ResultCallback<Void>() {
                @Override public void onSuccess(Void data) { }
                @Override public void onError(String message) {
                    Toast.makeText(CustomerLookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            looksRepo.unlike(userId, lookId, new LooksRepository.ResultCallback<Void>() {
                @Override public void onSuccess(Void data) { }
                @Override public void onError(String message) {
                    Toast.makeText(CustomerLookDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startBookingFlow() {
        // TODO: replace with real booking flow activity
        Intent i = new Intent(this, CustomerHomeActivity.class);
        i.putExtra("look_id", lookId);
        startActivity(i);
    }
}

