package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.SPClientAdapter;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPClientsActivity extends SPBaseActivity {

    private RecyclerView rvClients;
    private TextView tvEmptyClients;
    private ProgressBar progressClients;
    private TextInputEditText etSearch;

    private SPClientAdapter adapter;
    private final List<SPClientAdapter.ClientCard> allClients = new ArrayList<>();

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_sp_clients;
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_sp_clients;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rvClients = findViewById(R.id.rvClients);
        tvEmptyClients = findViewById(R.id.tvEmptyClients);
        progressClients = findViewById(R.id.progressClients);
        etSearch = findViewById(R.id.etSearch);

        adapter = new SPClientAdapter(new ArrayList<>());
        rvClients.setLayoutManager(new LinearLayoutManager(this));
        rvClients.setAdapter(adapter);

        setupSearch();
        loadClients();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterClients(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadClients() {
        String providerId = TokenStore.getUserId(this);

        if (providerId == null || providerId.trim().isEmpty()) {
            Toast.makeText(this, "Provider not logged in.", Toast.LENGTH_SHORT).show();
            showEmpty(true);
            return;
        }

        progressClients.setVisibility(View.VISIBLE);
        tvEmptyClients.setVisibility(View.GONE);

        String select =
                "id,client_id,provider_id,status,start_time,end_time," +
                        "client:profiles!bookings_client_id_fkey(id,full_name,email,phone)";

        ApiClient.supabase()
                .listConfirmedBookingsWithClient(
                        "eq." + providerId,
                        "eq.confirmed",
                        select,
                        "start_time.desc"
                )
                .enqueue(new Callback<List<SupabaseRestService.BookingWithClientDto>>() {
                    @Override
                    public void onResponse(
                            Call<List<SupabaseRestService.BookingWithClientDto>> call,
                            Response<List<SupabaseRestService.BookingWithClientDto>> response
                    ) {
                        progressClients.setVisibility(View.GONE);

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(
                                    SPClientsActivity.this,
                                    "Failed to load clients: code=" + response.code(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            showEmpty(true);
                            return;
                        }

                        buildUniqueClientCards(response.body());
                    }

                    @Override
                    public void onFailure(
                            Call<List<SupabaseRestService.BookingWithClientDto>> call,
                            Throwable t
                    ) {
                        progressClients.setVisibility(View.GONE);
                        Toast.makeText(
                                SPClientsActivity.this,
                                "Failed to load clients: " + t.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                        showEmpty(true);
                    }
                });
    }

    private void buildUniqueClientCards(List<SupabaseRestService.BookingWithClientDto> bookings) {
        Map<String, SPClientAdapter.ClientCard> uniqueClients = new LinkedHashMap<>();

        for (SupabaseRestService.BookingWithClientDto booking : bookings) {
            if (booking == null || booking.client_id == null) continue;

            SupabaseRestService.ProfileMiniDto client = booking.client;

            String name = client != null ? client.full_name : null;
            String email = client != null ? client.email : null;
            String phone = client != null ? client.phone : null;

            if (!uniqueClients.containsKey(booking.client_id)) {
                uniqueClients.put(
                        booking.client_id,
                        new SPClientAdapter.ClientCard(
                                booking.client_id,
                                safe(name, "Client"),
                                safe(email, "No email saved"),
                                safe(phone, "No phone saved"),
                                1
                        )
                );
            } else {
                SPClientAdapter.ClientCard existing = uniqueClients.get(booking.client_id);
                existing.appointmentCount++;
            }
        }

        allClients.clear();
        allClients.addAll(uniqueClients.values());

        String currentSearch = etSearch.getText() == null ? "" : etSearch.getText().toString();
        filterClients(currentSearch);
    }

    private void filterClients(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        List<SPClientAdapter.ClientCard> filtered = new ArrayList<>();

        if (q.isEmpty()) {
            filtered.addAll(allClients);
        } else {
            for (SPClientAdapter.ClientCard client : allClients) {
                String name = safe(client.name, "").toLowerCase();
                String email = safe(client.email, "").toLowerCase();
                String phone = safe(client.phone, "").toLowerCase();

                if (name.contains(q) || email.contains(q) || phone.contains(q)) {
                    filtered.add(client);
                }
            }
        }

        adapter.update(filtered);

        if (filtered.isEmpty()) {
            tvEmptyClients.setText(q.isEmpty()
                    ? "No confirmed clients yet."
                    : "No clients found.");
            showEmpty(true);
        } else {
            showEmpty(false);
        }
    }

    private void showEmpty(boolean empty) {
        tvEmptyClients.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvClients.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
