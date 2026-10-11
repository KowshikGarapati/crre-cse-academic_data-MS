package com.crre.academicdms.features.academic.model;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

public enum AcademicPeriod {
    PERIOD_1(1, LocalTime.of(9, 30), LocalTime.of(10, 20), "09:30 - 10:20"),
    PERIOD_2(2, LocalTime.of(10, 20), LocalTime.of(11, 10), "10:20 - 11:10"),
    PERIOD_3(3, LocalTime.of(11, 30), LocalTime.of(12, 20), "11:30 - 12:20"),
    PERIOD_4(4, LocalTime.of(12, 20), LocalTime.of(13, 10), "12:20 - 13:10"),
    PERIOD_5(5, LocalTime.of(14, 10), LocalTime.of(15, 0), "14:10 - 15:00"),
    PERIOD_6(6, LocalTime.of(15, 0), LocalTime.of(15, 50), "15:00 - 15:50"),
    PERIOD_7(7, LocalTime.of(15, 50), LocalTime.of(16, 30), "15:50 - 16:30");

    private final int periodNumber;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String timeRange;

    AcademicPeriod(int periodNumber, LocalTime startTime, LocalTime endTime, String timeRange) {
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timeRange = timeRange;
    }

    public int getPeriodNumber() {
        return periodNumber;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getLabel() {
        return "Period " + periodNumber;
    }

    public static AcademicPeriod of(int number) {
        return fromNumber(number);
    }

    public static AcademicPeriod fromNumber(int number) {
        for (AcademicPeriod p : values()) {
            if (p.periodNumber == number) {
                return p;
            }
        }
        throw new IllegalArgumentException("Invalid academic period number: " + number + " (must be 1-7)");
    }

    public static List<AcademicPeriod> getAllPeriods() {
        return Arrays.asList(values());
    }
}
