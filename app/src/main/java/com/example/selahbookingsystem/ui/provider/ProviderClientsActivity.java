package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.ClientsAdapter;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.data.model.ClientSummary;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProviderClientsActivity extends AppCompatActivity {

    private ClientsAdapter adapter;
    private final List<ClientSummary> masterList = new ArrayList<>();

    private TextInputEditText etSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sp_clients);

        MaterialToolbar toolbar = findViewById(R.id.toolbarClients);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etSearch = findViewById(R.id.etSearch);

        RecyclerView rv = findViewById(R.id.rvClients);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ClientsAdapter(client -> {
            // Placeholder for Stripe later:
            Toast.makeText(this, "Charge remainder for " + client.fullName, Toast.LENGTH_SHORT).show();
        });
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadClientsFromConfirmedBookings();
    }

    private void loadClientsFromConfirmedBookings() {
        String providerId = TokenStore.getUserId(this);
        if (providerId == null || providerId.isEmpty()) {
            masterList.clear();
            adapter.submitList(masterList);
            return;
        }

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        String select = "id,client_id,provider_id,status,start_time,end_time,client:profiles(id,full_name,email,phone)";
        rest.listConfirmedBookingsWithClient(
                "eq." + providerId,
                "eq.confirmed",
                select,
                "start_time.desc"
        ).enqueue(new Callback<List<SupabaseRestService.BookingWithClientDto>>() {
            @Override
            public void onResponse(Call<List<SupabaseRestService.BookingWithClientDto>> call,
                                   Response<List<SupabaseRestService.BookingWithClientDto>> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    masterList.clear();
                    adapter.submitList(masterList);
                    return;
                }

                // Group bookings by client_id
                HashMap<String, ClientSummary> map = new HashMap<>();

                for (SupabaseRestService.BookingWithClientDto b : resp.body()) {
                    if (b == null || b.client_id == null) continue;

                    ClientSummary cs = map.get(b.client_id);
                    if (cs == null) {
                        cs = new ClientSummary(b.client_id);

                        if (b.client != null) {
                            cs.fullName = b.client.full_name;
                            cs.email = b.client.email;
                            cs.phone = b.client.phone;
                        }
                        cs.timesBooked = 0;
                        cs.paymentText = "Saved card: —"; // placeholder until you wire Stripe
                        map.put(b.client_id, cs);
                    }

                    cs.timesBooked += 1;

                    // Because we ordered start_time.desc, the first booking seen for a client is their latest
                    if (cs.lastAppointmentText == null) {
                        cs.lastAppointmentText = "Last appointment: " + formatAppt(b.start_time, b.end_time);
                    }
                }

                masterList.clear();
                masterList.addAll(map.values());

                // Optional: sort by name (stable UX)
                masterList.sort((a, b) -> safe(a.fullName).compareToIgnoreCase(safe(b.fullName)));

                adapter.submitList(new ArrayList<>(masterList));
                filter(etSearch.getText() != null ? etSearch.getText().toString() : "");
            }

            @Override
            public void onFailure(Call<List<SupabaseRestService.BookingWithClientDto>> call, Throwable t) {
                masterList.clear();
                adapter.submitList(masterList);
            }
        });
    }

    private void filter(String q) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);

        if (query.isEmpty()) {
            adapter.submitList(new ArrayList<>(masterList));
            return;
        }

        List<ClientSummary> filtered = new ArrayList<>();
        for (ClientSummary c : masterList) {
            String name = safe(c.fullName).toLowerCase(Locale.ROOT);
            if (name.contains(query)) filtered.add(c);
        }
        adapter.submitList(filtered);
    }

    private String formatAppt(String startIso, String endIso) {
        try {
            Instant start = Instant.parse(startIso);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm")
                    .withLocale(Locale.UK)
                    .withZone(ZoneId.systemDefault());

            String startTxt = fmt.format(start);
            return startTxt; // keep simple; add end time if you want
        } catch (Exception e) {
            return "—";
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
