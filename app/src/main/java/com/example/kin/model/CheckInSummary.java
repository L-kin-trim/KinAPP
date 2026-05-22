package com.example.kin.model;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class CheckInSummary {
    public boolean signedToday;
    public int currentStreakDays;
    public int totalSignedDays;
    public int userLevel;
    public int userExperience;
    public int levelProgressExperience;
    public int nextLevelExperience;
    public int gainedExperience;
    public int year;
    public int month;
    public final Set<Integer> signedDays = new LinkedHashSet<>();

    public static CheckInSummary currentMonthPreview() {
        LocalDate today = LocalDate.now();
        CheckInSummary summary = new CheckInSummary();
        summary.year = today.getYear();
        summary.month = today.getMonthValue();
        summary.userLevel = 1;
        summary.nextLevelExperience = 200;
        return summary;
    }
}
