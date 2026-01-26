package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.LooksAdapter;
import com.example.selahbookingsystem.data.model.LookItem;              // ✅ correct import
import com.example.selahbookingsystem.data.store.LooksRepository;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseService;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class CustomerLikedLooksActivity extends AppCompatActivity {

    // ✅ Keep your anon key here ONLY if you haven't centralised it yet.
    // Better later: move to BuildConfig or a Constants file.
    private static final String SUPABASE_ANON_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhkY3V5enRxaXVkanVvdnJpeWFpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA0Nzk5NDcsImV4cCI6MjA3NjA1NTk0N30.TXwNemGU00BStqwwXKLtgGyNF1AIJmQUKbSeqP73Uyw";

    private RecyclerView rv;
    private LinearLayout emptyState;

    private LooksAdapter adapter;
    private final List<LookItem> likedLooks = new ArrayList<>();

    private LooksRepository repo;

    private String userId;
    private String jwt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_liked_looks);

        rv = findViewById(R.id.rvLikedLooks);
        emptyState = findViewById(R.id.emptyLikedState);

        MaterialToolbar toolbar = findViewById(R.id.toolbarLiked);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        adapter = new LooksAdapter(
                likedLooks,
                look -> {
                    // If you have a LookDetail page, point to it here instead.
                    Intent i = new Intent(this, CustomerLookDetailActivity.class);
                    i.putExtra("look_id", look.getId()); // repo uses public fields, so we do too
                    startActivity(i);
                },
                this::unlike
        );
        rv.setAdapter(adapter);

        // ✅ Build repository exactly how your constructor expects
        SupabaseService service = ApiClient.get().create(SupabaseService.class);
        repo = new LooksRepository();

        // ✅ Get userId from your prefs (you already saved it on login)
        SharedPreferences prefs = getSharedPreferences("selah_auth", MODE_PRIVATE);
        userId = prefs.getString("auth_user_id", null);

        // ✅ Get JWT from TokenStore (you saved it on login)
        jwt = com.example.selahbookingsystem.data.store.TokenStore.getAccessToken(this);

        if (userId == null || jwt == null || jwt.isEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadLiked();
    }

    private void loadLiked() {
        repo.loadExploreFeed(userId, 200, new LooksRepository.ResultCallback<List<LookItem>>() {
            @Override
            public void onSuccess(List<LookItem> result) {
                likedLooks.clear();

                for (LookItem l : result) {
                    if (l.isLiked()) likedLooks.add(l);
                }

                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(CustomerLikedLooksActivity.this, message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private void unlike(LookItem look) {
        repo.unlike(userId, look.getId(), new LooksRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                int idx = indexOfLook(look.getId());
                if (idx != -1) {
                    likedLooks.remove(idx);
                    runOnUiThread(() -> {
                        adapter.notifyItemRemoved(idx);
                        updateEmptyState();
                    });
                } else {
                    loadLiked();
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(CustomerLikedLooksActivity.this, "Unlike failed: " + message, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }


    private int indexOfLook(String lookId) {
        for (int i = 0; i < likedLooks.size(); i++) {
            if (likedLooks.get(i).getId().equals(lookId)) return i;
        }
        return -1;
    }

    private void updateEmptyState() {
        boolean empty = likedLooks.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}