package com.example.selahbookingsystem.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.selahbookingsystem.R;
import com.example.selahbookingsystem.adapter.TimeslotsAdapter;
import com.example.selahbookingsystem.data.dto.BookingDto;
import com.example.selahbookingsystem.data.dto.DailyAvailabilityDto;
import com.example.selahbookingsystem.data.dto.WeeklyAvailabilityDto;
import com.example.selahbookingsystem.network.api.ApiClient;
import com.example.selahbookingsystem.network.service.SupabaseRestService;
import com.example.selahbookingsystem.util.NailDurationCalculator;
import com.example.selahbookingsystem.util.TimeslotUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PickTimeslotActivity extends AppCompatActivity {

    public static final String EXTRA_PROVIDER_ID = "extra_provider_id";
    public static final String EXTRA_PROVIDER_NAME = "extra_provider_name";
    public static final String EXTRA_CURRENT_URI = "extra_current_uri";
    public static final String EXTRA_INSPO_URI = "extra_inspo_uri";
    public static final String EXTRA_SELECTED_MAP = "extra_selected_map";
    public static final String EXTRA_EST_MINS = "extra_est_mins";
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_SELECTED_SLOT = "extra_selected_slot";

    // ✅ NEW
    public static final String EXTRA_TOTAL_PRICE_CENTS = "extra_total_price_cents";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_timeslot);

        String providerId = getIntent().getStringExtra(EXTRA_PROVIDER_ID);
        String providerName = getIntent().getStringExtra(EXTRA_PROVIDER_NAME);
        String currentUri = getIntent().getStringExtra(EXTRA_CURRENT_URI);
        String inspoUri = getIntent().getStringExtra(EXTRA_INSPO_URI);
        String rawMap = getIntent().getStringExtra(EXTRA_SELECTED_MAP);
        int estMins = getIntent().getIntExtra(EXTRA_EST_MINS, 60);

        // ✅ NEW
        int totalPriceCents = getIntent().getIntExtra(EXTRA_TOTAL_PRICE_CENTS, 0);

        if (TextUtils.isEmpty(providerId) || TextUtils.isEmpty(currentUri) || TextUtils.isEmpty(rawMap)) {
            Toast.makeText(this, "Missing booking info. Go back.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextView tvDuration = findViewById(R.id.tvDuration);
        tvDuration.setText("Duration: " + NailDurationCalculator.pretty(estMins));

        RecyclerView rv = findViewById(R.id.rvTimes);
        rv.setLayoutManager(new LinearLayoutManager(this));

        TimeslotsAdapter adapter = new TimeslotsAdapter(slotLabel -> {
            Intent i = new Intent(this, ConfirmBookingActivity.class);
            i.putExtra(ConfirmBookingActivity.EXTRA_PROVIDER_ID, providerId);
            i.putExtra(ConfirmBookingActivity.EXTRA_PROVIDER_NAME, providerName);
            i.putExtra(ConfirmBookingActivity.EXTRA_CURRENT_URI, currentUri);
            if (!TextUtils.isEmpty(inspoUri)) i.putExtra(ConfirmBookingActivity.EXTRA_INSPO_URI, inspoUri);
            i.putExtra(ConfirmBookingActivity.EXTRA_SELECTED_MAP, rawMap);
            i.putExtra(ConfirmBookingActivity.EXTRA_EST_MINS, estMins);
            i.putExtra(ConfirmBookingActivity.EXTRA_SELECTED_SLOT, slotLabel);

            // ✅ pass price to ConfirmBooking (required for deposits)
            i.putExtra(ConfirmBookingActivity.EXTRA_TOTAL_PRICE_CENTS, totalPriceCents);

            startActivity(i);
        });

        rv.setAdapter(adapter);

        SupabaseRestService rest = ApiClient.get().create(SupabaseRestService.class);

        ProgressBar progress = findViewById(R.id.timesProgress);
        TextView empty = findViewById(R.id.timesEmpty);

        if (progress != null) progress.setVisibility(View.VISIBLE);
        if (empty != null) empty.setVisibility(View.GONE);

        loadNext7DaysSlots(rest, providerId, estMins, adapter, progress, empty);
    }

    private void loadNext7DaysSlots(SupabaseRestService rest,
                                    String providerId,
                                    int durationMins,
                                    TimeslotsAdapter adapter,
                                    @Nullable ProgressBar progress,
                                    @Nullable TextView empty) {

        LocalDate today = LocalDate.now(TimeslotUtils.PROVIDER_ZONE);

        rest.listWeeklyAvailability(
                "eq." + providerId,
                "id,day_of_week,start_time,end_time,enabled",
                "day_of_week.asc"
        ).enqueue(new retrofit2.Callback<List<WeeklyAvailabilityDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<WeeklyAvailabilityDto>> call,
                                   retrofit2.Response<List<WeeklyAvailabilityDto>> resp) {

                if (!resp.isSuccessful() || resp.body() == null) {
                    if (progress != null) progress.setVisibility(View.GONE);
                    if (empty != null) { empty.setText("Failed to load availability."); empty.setVisibility(View.VISIBLE); }
                    return;
                }

                List<WeeklyAvailabilityDto> rules = normalizeWeekly(resp.body());

                List<String> allLabels = new ArrayList<>();
                loadDayRecursive(rest, providerId, durationMins, rules, today, 0, allLabels, adapter, progress, empty);
            }

            @Override
            public void onFailure(retrofit2.Call<List<WeeklyAvailabilityDto>> call, Throwable t) {
                if (progress != null) progress.setVisibility(View.GONE);
                if (empty != null) { empty.setText("Network error: " + t.getMessage()); empty.setVisibility(View.VISIBLE); }
            }
        });
    }

    private void loadDayRecursive(SupabaseRestService rest,
                                  String providerId,
                                  int durationMins,
                                  List<WeeklyAvailabilityDto> weeklyRules7,
                                  LocalDate startDay,
                                  int offset,
                                  List<String> outLabels,
                                  TimeslotsAdapter adapter,
                                  @Nullable ProgressBar progress,
                                  @Nullable TextView empty) {

        if (offset >= 7) {
            if (progress != null) progress.setVisibility(View.GONE);

            if (outLabels.isEmpty()) {
                if (empty != null) {
                    empty.setText("No slots available in the next 7 days.");
                    empty.setVisibility(View.VISIBLE);
                }
            } else {
                if (empty != null) empty.setVisibility(View.GONE);
            }

            adapter.submit(outLabels);
            return;
        }

        LocalDate date = startDay.plusDays(offset);
        int dbDow = date.getDayOfWeek().getValue() % 7; // Sun=0..Sat=6
        WeeklyAvailabilityDto weeklyRule = weeklyRules7.get(dbDow);

        loadDailyOverrideThenBookings(rest, providerId, date, weeklyRule, durationMins,
                weeklyRules7, startDay, offset, outLabels, adapter, progress, empty);
    }

    private void loadDailyOverrideThenBookings(SupabaseRestService rest,
                                               String providerId,
                                               LocalDate date,
                                               WeeklyAvailabilityDto weeklyRule,
                                               int durationMins,
                                               List<WeeklyAvailabilityDto> weeklyRules7,
                                               LocalDate startDay,
                                               int offset,
                                               List<String> outLabels,
                                               TimeslotsAdapter adapter,
                                               @Nullable ProgressBar progress,
                                               @Nullable TextView empty) {

        rest.listDailyAvailabilityForDate(
                "eq." + providerId,
                "eq." + date.toString(),
                "id,date,start_time,end_time,enabled",
                "start_time.asc"
        ).enqueue(new retrofit2.Callback<List<DailyAvailabilityDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<DailyAvailabilityDto>> call,
                                   retrofit2.Response<List<DailyAvailabilityDto>> resp) {

                List<DailyAvailabilityDto> daily = (resp.isSuccessful() ? resp.body() : null);

                String startTime;
                String endTime;
                boolean enabled;

                DailyAvailabilityDto d = firstEnabledDaily(daily);
                if (d != null) {
                    startTime = d.start_time;
                    endTime = d.end_time;
                    enabled = d.enabled;
                } else {
                    startTime = weeklyRule.start_time;
                    endTime = weeklyRule.end_time;
                    enabled = weeklyRule.enabled;
                }

                if (!enabled) {
                    loadDayRecursive(rest, providerId, durationMins, weeklyRules7,
                            startDay, offset + 1, outLabels, adapter, progress, empty);
                    return;
                }

                fetchBookingsThenGenerate(rest, providerId, date, startTime, endTime, enabled, durationMins,
                        weeklyRules7, startDay, offset, outLabels, adapter, progress, empty);
            }

            @Override
            public void onFailure(retrofit2.Call<List<DailyAvailabilityDto>> call, Throwable t) {
                if (!weeklyRule.enabled) {
                    loadDayRecursive(rest, providerId, durationMins, weeklyRules7,
                            startDay, offset + 1, outLabels, adapter, progress, empty);
                    return;
                }

                fetchBookingsThenGenerate(rest, providerId, date,
                        weeklyRule.start_time, weeklyRule.end_time, weeklyRule.enabled, durationMins,
                        weeklyRules7, startDay, offset, outLabels, adapter, progress, empty);
            }
        });
    }

    private void fetchBookingsThenGenerate(SupabaseRestService rest,
                                           String providerId,
                                           LocalDate date,
                                           String startTime,
                                           String endTime,
                                           boolean enabled,
                                           int durationMins,
                                           List<WeeklyAvailabilityDto> weeklyRules7,
                                           LocalDate startDay,
                                           int offset,
                                           List<String> outLabels,
                                           TimeslotsAdapter adapter,
                                           @Nullable ProgressBar progress,
                                           @Nullable TextView empty) {

        Instant dayStart = TimeslotUtils.dayStartUtc(date);
        Instant nextDay  = TimeslotUtils.nextDayStartUtc(date);

        List<String> startFilters = Arrays.asList(
                "gte." + dayStart.toString(),
                "lt." + nextDay.toString()
        );

        rest.listBookingsForProviderBetween(
                "eq." + providerId,
                startFilters,
                "start_time,end_time,duration_mins,status",
                "start_time.asc"
        ).enqueue(new retrofit2.Callback<List<BookingDto>>() {
            @Override
            public void onResponse(retrofit2.Call<List<BookingDto>> call,
                                   retrofit2.Response<List<BookingDto>> resp) {

                List<TimeslotUtils.Range> booked = new ArrayList<>();

                if (resp.isSuccessful() && resp.body() != null) {
                    for (BookingDto b : resp.body()) {
                        if (b.status != null && b.status.equalsIgnoreCase("cancelled")) continue;
                        if (b.start_time == null) continue;

                        Instant s = Instant.parse(b.start_time);

                        Instant e;
                        if (b.end_time != null && !b.end_time.isEmpty()) {
                            e = Instant.parse(b.end_time);
                        } else if (b.duration_mins != null) {
                            e = s.plusSeconds(b.duration_mins * 60L);
                        } else {
                            e = s.plusSeconds(60L * 60L);
                        }

                        booked.add(new TimeslotUtils.Range(s, e));
                    }
                }

                outLabels.addAll(
                        TimeslotUtils.generateSlotLabels30Min(
                                date, startTime, endTime, enabled, durationMins, booked
                        )
                );

                loadDayRecursive(rest, providerId, durationMins, weeklyRules7,
                        startDay, offset + 1, outLabels, adapter, progress, empty);
            }

            @Override
            public void onFailure(retrofit2.Call<List<BookingDto>> call, Throwable t) {
                loadDayRecursive(rest, providerId, durationMins, weeklyRules7,
                        startDay, offset + 1, outLabels, adapter, progress, empty);
            }
        });
    }

    private DailyAvailabilityDto firstEnabledDaily(List<DailyAvailabilityDto> daily) {
        if (daily == null) return null;
        for (DailyAvailabilityDto d : daily) {
            if (d != null && d.enabled) return d;
        }
        return null;
    }

    private List<WeeklyAvailabilityDto> normalizeWeekly(List<WeeklyAvailabilityDto> data) {
        Map<Integer, WeeklyAvailabilityDto> map = new HashMap<>();
        if (data != null) {
            for (WeeklyAvailabilityDto r : data) {
                if (r != null && r.day_of_week >= 0 && r.day_of_week <= 6) map.put(r.day_of_week, r);
            }
        }

        List<WeeklyAvailabilityDto> out = new ArrayList<>();
        for (int dow = 0; dow <= 6; dow++) {
            WeeklyAvailabilityDto r = map.get(dow);
            if (r == null) {
                r = new WeeklyAvailabilityDto();
                r.day_of_week = dow;
                r.start_time = "09:00:00";
                r.end_time = "17:00:00";
                r.enabled = (dow >= 1 && dow <= 5);
            } else {
                if (r.start_time == null) r.start_time = "09:00:00";
                if (r.end_time == null) r.end_time = "17:00:00";
            }
            out.add(r);
        }
        return out;
    }
}