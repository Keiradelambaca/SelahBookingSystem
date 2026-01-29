package com.example.selahbookingsystem.util;

import java.util.Map;

public class NailDurationCalculator {

    // Phase 1 simple rules
    public static int estimateMinutes(Map<String, String> sel) {
        int mins = 0;

        String service = val(sel, "Service");
        String length  = val(sel, "Length");
        String shape   = val(sel, "Shape");
        String design  = val(sel, "Design");

        // base service
        if ("Removal".equalsIgnoreCase(service)) mins += 25;
        else if ("Refill".equalsIgnoreCase(service)) mins += 35;
        else if ("Full set".equalsIgnoreCase(service)) mins += 45;
        else mins += 35; // default

        // length adds
        if (length != null) {
            String L = length.toLowerCase();
            if (L.contains("long")) mins += 10;
            if (L.contains("xl")) mins += 15;
            if (L.contains("xxl")) mins += 20;
        }

        // shape adds (minor)
        if (shape != null) {
            String S = shape.toLowerCase();
            if (S.contains("stiletto") || S.contains("almond")) mins += 5;
        }

        // design adds (very rough)
        if (design != null && !design.trim().isEmpty()) {
            String D = design.toLowerCase();
            if (D.contains("french")) mins += 15;
            else if (D.contains("ombre")) mins += 20;
            else if (D.contains("airbrush")) mins += 25;
            else if (D.contains("3d") || D.contains("flower")) mins += 25;
            else if (D.contains("charm") || D.contains("gem")) mins += 10;
            else mins += 10; // custom prompt baseline
        }

        // floor/ceiling
        if (mins < 45) mins = 45;
        if (mins > 240) mins = 240;

        // round to nearest 5
        mins = (int) (Math.round(mins / 5.0) * 5);

        return mins;
    }

    private static String val(Map<String, String> sel, String key) {
        if (sel == null) return null;
        return sel.get(key);
    }

    public static String pretty(int mins) {
        int h = mins / 60;
        int m = mins % 60;
        if (h <= 0) return mins + " min";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }
}
