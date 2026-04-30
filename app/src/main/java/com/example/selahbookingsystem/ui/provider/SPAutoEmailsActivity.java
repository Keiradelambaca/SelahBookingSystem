package com.example.selahbookingsystem.ui.provider;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.EmailTemplatesAdapter;
import com.example.selahbookingsystem.data.dto.EmailTemplateDto;
import com.example.selahbookingsystem.data.store.TokenStore;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.ui.base.SPBaseActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SPAutoEmailsActivity extends SPBaseActivity {

    private RecyclerView rv;
    private MaterialButton btnSaveAll;

    private EmailTemplatesAdapter adapter;
    private final List<EmailTemplateDto> items = new ArrayList<>();

    @Override protected int getLayoutResourceId() { return R.layout.activity_sp_auto_emails; }
    @Override protected int getSelectedNavItemId() { return R.id.nav_sp_scheduling; } // still under Scheduling

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv = findViewById(R.id.rvTemplates);
        btnSaveAll = findViewById(R.id.btnSaveAll);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmailTemplatesAdapter(items);
        rv.setAdapter(adapter);

        btnSaveAll.setOnClickListener(v -> saveAll());

        loadTemplates();
    }

    private void loadTemplates() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            toast("You must be logged in");
            return;
        }

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);
        api.listEmailTemplatesForProvider(
                "eq." + providerId,
                "id,provider_id,type,subject,body,is_enabled,updated_at"
        ).enqueue(new Callback<List<EmailTemplateDto>>() {
            @Override
            public void onResponse(Call<List<EmailTemplateDto>> call, Response<List<EmailTemplateDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    items.clear();
                    items.addAll(ensureAllFour(providerId, response.body()));
                    adapter.notifyDataSetChanged();
                } else {
                    toast("Failed to load templates");
                    items.clear();
                    items.addAll(ensureAllFour(providerId, new ArrayList<>()));
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<EmailTemplateDto>> call, Throwable t) {
                toast("Network error loading templates");
                items.clear();
                items.addAll(ensureAllFour(providerId, new ArrayList<>()));
                adapter.notifyDataSetChanged();
            }
        });
    }

    // Ensure UI always shows all 4 templates even if DB is empty
    private List<EmailTemplateDto> ensureAllFour(String providerId, List<EmailTemplateDto> existing) {
        Set<String> have = new HashSet<>();
        for (EmailTemplateDto e : existing) {
            if (e != null && e.type != null) have.add(e.type);
        }

        List<EmailTemplateDto> out = new ArrayList<>(existing);

        addIfMissing(out, have, providerId, "BOOKING_CONFIRMATION",
                "Booking Confirmed",
                "Hi {{client_name}},\n\nYour booking with {{provider_name}} is confirmed for {{start_time_local}}.\n\nThanks,\n{{provider_name}}");

        addIfMissing(out, have, providerId, "CANCELLATION",
                "Booking Cancelled",
                "Hi {{client_name}},\n\nYour appointment for {{start_time_local}} has been cancelled.\n\n— {{provider_name}}");

        addIfMissing(out, have, providerId, "RESCHEDULED",
                "Booking Rescheduled",
                "Hi {{client_name}},\n\nYour appointment has been rescheduled.\nNew time: {{start_time_local}}\n\n— {{provider_name}}");

        addIfMissing(out, have, providerId, "REMINDER_24H",
                "Reminder: appointment tomorrow",
                "Hi {{client_name}},\n\nReminder: you have an appointment tomorrow at {{start_time_local}}.\n\n— {{provider_name}}");

        out.sort((a, b) -> orderFor(a.type) - orderFor(b.type));
        return out;
    }

    private void addIfMissing(List<EmailTemplateDto> out, Set<String> have,
                              String providerId, String type, String subject, String body) {
        if (have.contains(type)) return;

        EmailTemplateDto t = new EmailTemplateDto();
        t.provider_id = providerId;
        t.type = type;
        t.subject = subject;
        t.body = body;
        t.is_enabled = true;

        out.add(t);
        have.add(type);
    }
    private int orderFor(String type) {
        if ("BOOKING_CONFIRMATION".equals(type)) return 0;
        if ("CANCELLATION".equals(type)) return 1;
        if ("RESCHEDULED".equals(type)) return 2;
        if ("REMINDER_24H".equals(type)) return 3;
        return 99;
    }

    private void saveAll() {
        String providerId = TokenStore.getUserId(this);
        if (TextUtils.isEmpty(providerId)) {
            toast("You must be logged in");
            return;
        }

        List<EmailTemplateDto> toSave = adapter.collectCurrentValues(providerId);

        // basic cleanup
        for (EmailTemplateDto t : toSave) {
            if (t.subject == null) t.subject = "";
            if (t.body == null) t.body = "";
            if (t.is_enabled == null) t.is_enabled = true;
            t.provider_id = providerId; // ensure correct
        }

        SupabaseRestService api = ApiClient.get().create(SupabaseRestService.class);

        btnSaveAll.setEnabled(false);

        api.upsertEmailTemplates("provider_id,type", toSave)
                .enqueue(new Callback<List<EmailTemplateDto>>() {
                    @Override
                    public void onResponse(Call<List<EmailTemplateDto>> call,
                                           Response<List<EmailTemplateDto>> response) {
                        btnSaveAll.setEnabled(true);

                        if (response.isSuccessful()) {
                            toast("Saved");
                            loadTemplates(); // reload to get ids/updated_at
                        } else {
                            toast("Save failed (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<EmailTemplateDto>> call, Throwable t) {
                        btnSaveAll.setEnabled(true);
                        toast("Network error saving templates");
                    }
                });
    }


    private String prettyType(String type) {
        if ("BOOKING_CONFIRMATION".equals(type)) return "Booking confirmation";
        if ("CANCELLATION".equals(type)) return "Cancellation";
        if ("RESCHEDULED".equals(type)) return "Rescheduled";
        if ("REMINDER_24H".equals(type)) return "24h reminder";
        return type;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    // helper for addIfMissing
    private static class AddIfMissingList extends ArrayList<EmailTemplateDto> {
        void addIfMissing(Set<String> have, String providerId, String type, String subject, String body) {
            if (have.contains(type)) return;
            EmailTemplateDto t = new EmailTemplateDto();
            t.provider_id = providerId;
            t.type = type;
            t.subject = subject;
            t.body = body;
            t.is_enabled = true;
            add(t);
        }
    }
}