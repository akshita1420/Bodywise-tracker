package com.bodywise.web;

import com.bodywise.domain.DailyLog;
import com.bodywise.domain.User;
import com.bodywise.repo.DailyLogRepo;
import com.bodywise.web.Dtos.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Weight history (calendar + Sunday-to-Sunday weekly change) and the habits grid. */
@RestController
@RequestMapping("/api")
public class TrackController {

    private final Support support;
    private final DailyLogRepo logs;
    private final com.bodywise.repo.MealEntryRepo entries;

    public TrackController(Support support, DailyLogRepo logs, com.bodywise.repo.MealEntryRepo entries) {
        this.support = support;
        this.logs = logs;
        this.entries = entries;
    }

    @GetMapping("/weights")
    public List<WeightPoint> weights(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return logs.findByUserIdAndDateBetweenOrderByDate(Support.uid(jwt), from, to).stream()
                .filter(l -> l.weightKg != null)
                .map(l -> new WeightPoint(l.date, l.weightKg))
                .toList();
    }

    /**
     * Weeks run Monday..Sunday. The week's figure is the Sunday weigh-in (or the last weigh-in of that week
     * if Sunday was skipped); change is compared with the previous week's figure.
     */
    @GetMapping("/weights/weekly")
    public List<WeekPoint> weekly(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "12") int weeks) {
        weeks = Math.min(Math.max(weeks, 1), 104);
        LocalDate lastSunday = support.today().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate firstMonday = lastSunday.minusWeeks(weeks).plusDays(1);
        List<DailyLog> all = logs.findByUserIdAndDateBetweenOrderByDate(Support.uid(jwt), firstMonday.minusWeeks(1), lastSunday);

        List<WeekPoint> out = new ArrayList<>();
        Double prev = null;
        for (int i = weeks; i >= 0; i--) {
            LocalDate sunday = lastSunday.minusWeeks(i);
            LocalDate monday = sunday.minusDays(6);
            List<DailyLog> week = all.stream()
                    .filter(l -> l.weightKg != null && !l.date.isBefore(monday) && !l.date.isAfter(sunday))
                    .toList();
            Double figure = week.isEmpty() ? null : week.get(week.size() - 1).weightKg;
            Double avg = week.isEmpty() ? null
                    : Math.round(week.stream().mapToDouble(l -> l.weightKg).average().orElse(0) * 10) / 10.0;
            Double change = figure != null && prev != null ? Math.round((figure - prev) * 10) / 10.0 : null;
            // The extra oldest week (i == weeks) is only used as the baseline for the first change.
            if (i < weeks) out.add(new WeekPoint(sunday, figure, avg, change, week.size()));
            if (figure != null) prev = figure;
        }
        return out;
    }

    /**
     * One row per day from the day you started (account creation, or your first log) up to today,
     * newest first, so each new day appears on top. At most {@code limit} rows.
     */
    @GetMapping("/habits")
    public HabitsResponse habits(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "120") int limit) {
        User u = support.user(jwt);
        double water = u.waterTargetL != null ? u.waterTargetL : 3.45;
        int steps = u.stepsTarget != null ? u.stepsTarget : 10000;
        LocalDate to = support.today();
        LocalDate start = u.createdOn;
        LocalDate firstLog = logs.findFirstByUserIdOrderByDateAsc(u.id).map(l -> l.date).orElse(null);
        if (start == null || (firstLog != null && firstLog.isBefore(start))) start = firstLog;
        if (start == null || start.isAfter(to)) start = to;
        LocalDate from = start;
        LocalDate earliest = to.minusDays(Math.max(1, Math.min(limit, 1000)) - 1);
        if (from.isBefore(earliest)) from = earliest;
        Map<LocalDate, DailyLog> byDate = new HashMap<>();
        for (DailyLog l : logs.findByUserIdAndDateBetweenOrderByDate(u.id, from, to)) byDate.put(l.date, l);
        // Sweets logged per day (the SWEETS slot): names, calories and carbs.
        Map<LocalDate, List<com.bodywise.domain.MealEntry>> sweets = new HashMap<>();
        for (com.bodywise.domain.MealEntry e : entries.findByUserIdAndMealTypeAndDateBetween(u.id, com.bodywise.domain.MealType.SWEETS, from, to)) {
            sweets.computeIfAbsent(e.date, k -> new ArrayList<>()).add(e);
        }

        List<HabitRow> rows = new ArrayList<>();
        for (LocalDate d = to; !d.isBefore(from); d = d.minusDays(1)) {
            DailyLog l = byDate.get(d);
            Double w = l != null ? l.waterLiters : null;
            Integer s = l != null ? l.steps : null;
            Integer g = l != null ? l.gymMinutes : null;
            rows.add(new HabitRow(d, w, s, g, w != null && w >= water, s != null && s >= steps, g != null && g > 0,
                    l != null ? l.planFollowed : null, l != null && Boolean.TRUE.equals(l.ateOut),
                    l != null && Boolean.TRUE.equals(l.noSugar),
                    sweets.getOrDefault(d, List.of()).stream().map(e -> e.name).toList(),
                    (int) Math.round(sweets.getOrDefault(d, List.of()).stream().mapToDouble(e -> e.kcal * e.quantity).sum()),
                    Math.round(sweets.getOrDefault(d, List.of()).stream().mapToDouble(e -> e.carbs * e.quantity).sum() * 10) / 10.0));
        }
        return new HabitsResponse(water, steps, start, rows);
    }
}
