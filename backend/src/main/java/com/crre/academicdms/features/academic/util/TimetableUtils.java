package com.crre.academicdms.features.academic.util;

import com.crre.academicdms.features.academic.model.TimetableEntry;

import java.util.Comparator;
import java.util.Map;

public final class TimetableUtils {

    private TimetableUtils() {}

    public static final Map<String, Integer> DAY_ORDER = Map.of(
        "MONDAY", 1,
        "TUESDAY", 2,
        "WEDNESDAY", 3,
        "THURSDAY", 4,
        "FRIDAY", 5,
        "SATURDAY", 6,
        "SUNDAY", 7
    );

    public static final Comparator<TimetableEntry> CHRONOLOGICAL_COMPARATOR =
        Comparator.comparingInt((TimetableEntry e) -> {
            if (e == null || e.getDayOfWeek() == null) return 99;
            return DAY_ORDER.getOrDefault(e.getDayOfWeek().trim().toUpperCase(), 99);
        })
        .thenComparing(e -> e.getStartTime() != null ? e.getStartTime() : java.time.LocalTime.MIN)
        .thenComparingInt(TimetableEntry::getPeriodNumber);

    public static int getDayOrder(String dayOfWeek) {
        if (dayOfWeek == null) return 99;
        return DAY_ORDER.getOrDefault(dayOfWeek.trim().toUpperCase(), 99);
    }
}
