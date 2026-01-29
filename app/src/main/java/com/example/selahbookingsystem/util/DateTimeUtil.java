package com.example.selahbookingsystem.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    public static String nowIso() {
        return OffsetDateTime.now().toString();
    }

    public static String addMinutesIso(String startIso, int minutes) {
        return OffsetDateTime.parse(startIso).plusMinutes(minutes).toString();
    }

    public static String prettyFromIso(String iso) {
        OffsetDateTime dt = OffsetDateTime.parse(iso);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEE d MMM • HH:mm");
        return dt.atZoneSameInstant(ZoneId.systemDefault()).format(fmt);
    }
}

