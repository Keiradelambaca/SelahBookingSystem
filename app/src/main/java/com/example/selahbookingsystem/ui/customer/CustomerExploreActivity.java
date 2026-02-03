package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.LooksAdapter;
import com.example.selahbookingsystem.data.model.LookItem;
import com.example.selahbookingsystem.data.store.LooksRepository;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseService;
import com.example.selahbookingsystem.ui.base.BaseActivity;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class CustomerExploreActivity extends BaseActivity {

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvLooks;
    private LinearLayout emptyState;

    private LooksAdapter adapter;
    private final List<LookItem> looks = new ArrayList<>();

    private LooksRepository repo;

    private String userId;
    private String jwt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_explore);

        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefreshExplore);
        rvLooks = findViewById(R.id.rvExploreLooks);
        emptyState = findViewById(R.id.emptyExploreState);


        // user id
        SharedPreferences prefs = getSharedPreferences("selah_auth", MODE_PRIVATE);
        userId = prefs.getString("auth_user_id", null);

        // jwt
        jwt = com.example.selahbookingsystem.data.store.TokenStore.getAccessToken(this);

        if (userId == null || jwt == null || jwt.isEmpty()) {
            Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // repo (your version expects constructor args)
        SupabaseService service = ApiClient.get().create(SupabaseService.class);
        repo = new LooksRepository();

        setupToolbar();
        setupRecycler();

        swipeRefresh.setOnRefreshListener(this::loadExploreFeed);

        loadExploreFeed();
    }

    private void setupToolbar() {
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_liked) {
                startActivity(new Intent(this, CustomerLikedLooksActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupRecycler() {
        StaggeredGridLayoutManager lm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        rvLooks.setLayoutManager(lm);

        adapter = new LooksAdapter(
                looks,
                look -> {
                    Intent i = new Intent(this, CustomerLookDetailActivity.class);
                    i.putExtra("look_id", look.getId());
                    startActivity(i);
                },
                this::toggleLike
        );

        rvLooks.setAdapter(adapter);
    }

    private void loadExploreFeed() {
        swipeRefresh.setRefreshing(true);

        repo.loadExploreFeed(userId, 40, new LooksRepository.ResultCallback<List<LookItem>>() {
            @Override
            public void onSuccess(List<LookItem> data) {
                looks.clear();
                looks.addAll(data);

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    showEmptyStateIfNeeded();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(CustomerExploreActivity.this, message, Toast.LENGTH_SHORT).show();
                    showEmptyStateIfNeeded();
                });
            }
        });

    }

    private void showEmptyStateIfNeeded() {
        boolean isEmpty = looks.isEmpty();
        if (emptyState != null) emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvLooks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void toggleLike(LookItem look) {
        boolean newState = !look.isLiked();
        look.setLiked(newState);

        int idx = adapter.indexOf(look);
        if (idx != -1) adapter.notifyItemChanged(idx);

        if (newState) {
            repo.like(userId, look.getId(), new LooksRepository.ResultCallback<Void>() {
                @Override public void onSuccess(Void data) {}
                @Override public void onError(String message) {
                    Toast.makeText(CustomerExploreActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            repo.unlike(userId, look.getId(), new LooksRepository.ResultCallback<Void>() {
                @Override public void onSuccess(Void data) {}
                @Override public void onError(String message) {
                    Toast.makeText(CustomerExploreActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_explore;
    }
}
