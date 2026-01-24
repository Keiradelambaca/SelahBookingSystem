package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.adapter.ProvidersAdapter;
import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.network.service.SupabaseRestService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SelectProviderActivity extends AppCompatActivity implements ProvidersAdapter.OnProviderClick {
    public static final String EXTRA_SELECTED_PROVIDER_ID = "selected_provider_id";
    public static final String EXTRA_SELECTED_PROVIDER_NAME = "selected_provider_name";

    private RecyclerView recycler;
    private ProgressBar progress;
    private TextView empty;
    private ProvidersAdapter adapter;
    private SupabaseRestService rest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_provider);

        recycler = findViewById(R.id.providersRecycler);
        progress = findViewById(R.id.providersProgress);
        empty    = findViewById(R.id.emptyText);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProvidersAdapter(this);
        recycler.setAdapter(adapter);

        rest = ApiClient.get().create(SupabaseRestService.class);

        loadProviders();
    }

    private void loadProviders() {
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        recycler.setVisibility(View.INVISIBLE);

        rest.listProviders("eq.provider", "id,full_name").enqueue(new Callback<List<SupabaseRestService.ProviderDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.ProviderDto>> call,
                                   Response<List<SupabaseRestService.ProviderDto>> resp) {
                progress.setVisibility(View.GONE);
                if (resp.isSuccessful() && resp.body() != null) {
                    List<SupabaseRestService.ProviderDto> data = resp.body();
                    if (data.isEmpty()) {
                        empty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.submit(data);
                        recycler.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(SelectProviderActivity.this, "Failed to load providers", Toast.LENGTH_LONG).show();
                    empty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.ProviderDto>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                Toast.makeText(SelectProviderActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                empty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onProviderClicked(SupabaseRestService.ProviderDto p) {
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTED_PROVIDER_ID, p.id);
        data.putExtra(EXTRA_SELECTED_PROVIDER_NAME, p.full_name);
        setResult(RESULT_OK, data);
        finish();
    }
}