package com.example.selahbookingsystem.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeslotUtils {

    public static final ZoneId PROVIDER_ZONE = ZoneId.of("Europe/Dublin");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);

    /** Booked range in UTC instants (recommended) */
    public static class Range {
        public final Instant start;
        public final Instant end;
        public Range(Instant s, Instant e) { start = s; end = e; }
    }

    /** Availability window in local provider time (e.g. 09:00-17:00) */
    public static class Window {
        public final String startTime; // "09:00:00"
        public final String endTime;   // "17:00:00"
        public final boolean enabled;

        public Window(String startTime, String endTime, boolean enabled) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.enabled = enabled;
        }
    }

    /** A generated appointment slot */
    public static class Slot {
        public final Instant startUtc;
        public final Instant endUtc;

        public Slot(Instant startUtc, Instant endUtc) {
            this.startUtc = startUtc;
            this.endUtc = endUtc;
        }

        /** label shown to user (local time) */
        public String label(LocalDate date) {
            LocalTime t = startUtc.atZone(PROVIDER_ZONE).toLocalTime();
            return date.toString() + " • " + t.format(TIME_FMT);
        }
    }

    public static Instant dayStartUtc(LocalDate d) {
        return d.atStartOfDay(PROVIDER_ZONE).toInstant();
    }

    public static Instant nextDayStartUtc(LocalDate d) {
        return d.plusDays(1).atStartOfDay(PROVIDER_ZONE).toInstant();
    }

    /**
     * Preferred generator:
     * - supports MULTIPLE windows (e.g. 09:00-12:00 + 13:00-17:00)
     * - supports custom step (15/30)
     * - returns real UTC instants for start/end (easy to create bookings)
     */
    public static List<Slot> generateSlots(
            LocalDate date,
            List<Window> windows,
            int durationMins,
            int stepMins,
            List<Range> bookedUtc
    ) {
        List<Slot> out = new ArrayList<>();
        if (windows == null || windows.isEmpty()) return out;

        Duration step = Duration.ofMinutes(stepMins);
        Duration dur  = Duration.ofMinutes(durationMins);

        for (Window w : windows) {
            if (w == null || !w.enabled) continue;

            LocalTime startT = parseLocalTime(w.startTime);
            LocalTime endT   = parseLocalTime(w.endTime);

            // guard
            if (!endT.isAfter(startT)) continue;

            ZonedDateTime zStart = ZonedDateTime.of(date, startT, PROVIDER_ZONE);
            ZonedDateTime zEnd   = ZonedDateTime.of(date, endT, PROVIDER_ZONE);

            Instant wStart = zStart.toInstant();
            Instant wEnd   = zEnd.toInstant();

            for (Instant s = wStart; !s.plus(dur).isAfter(wEnd); s = s.plus(step)) {
                Instant e = s.plus(dur);

                if (!overlapsAny(s, e, bookedUtc)) {
                    out.add(new Slot(s, e));
                }
            }
        }

        // sort just in case multiple windows
        out.sort(Comparator.comparing(a -> a.startUtc));
        return out;
    }

    private static boolean overlapsAny(Instant s, Instant e, List<Range> bookedUtc) {
        if (bookedUtc == null || bookedUtc.isEmpty()) return false;
        for (Range b : bookedUtc) {
            if (s.isBefore(b.end) && b.start.isBefore(e)) return true;
        }
        return false;
    }

    private static LocalTime parseLocalTime(String hhmmss) {
        if (hhmmss == null || hhmmss.trim().isEmpty()) return LocalTime.of(9, 0);
        // Supports "09:00:00" or "09:00"
        if (hhmmss.length() == 5) return LocalTime.parse(hhmmss);
        return LocalTime.parse(hhmmss);
    }

    // -------------------------------------------------------
    // Backwards-compatible method (your old one, still works)
    // -------------------------------------------------------
    public static List<String> generateSlotLabels30Min(
            LocalDate date,
            String startTime,
            String endTime,
            boolean enabled,
            int durationMins,
            List<Range> bookedUtc
    ) {
        List<Window> windows = Collections.singletonList(new Window(startTime, endTime, enabled));
        List<Slot> slots = generateSlots(date, windows, durationMins, 30, bookedUtc);

        List<String> labels = new ArrayList<>();
        for (Slot s : slots) labels.add(s.label(date));
        return labels;
    }
}
