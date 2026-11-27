package com.bodywise.service;

import com.bodywise.domain.DailyLog;
import com.bodywise.domain.GymEntry;
import com.bodywise.domain.User;
import com.bodywise.repo.DailyLogRepo;
import com.bodywise.repo.GymEntryRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Gym log and calorie estimate.
 *
 *  kcal    = (MET − 1) × body weight kg × hours       (the "− 1" removes what you'd burn at rest anyway)
 *  MET     = the exercise's value from the Compendium of Physical Activities, × 0.8 light / 1.0 moderate / 1.2 hard
 *  minutes = what you enter for machines and timed work; for sets × reps it is estimated as
 *            sets × (reps × 4 s + 90 s rest), unless you enter the minutes yourself.
 */
@Service
public class GymService {

    public record Exercise(String name, String kind, double met, String aliases) {}

    /** MET values at moderate effort (Compendium of Physical Activities, 2011/2024). */
    public static final List<Exercise> CATALOG = List.of(
        // strength (sets × reps)
        new Exercise("Squat", "STRENGTH", 6.0, "squats, goblet squat, barbell squat"),
        new Exercise("Deadlift", "STRENGTH", 6.0, "deadlifts, romanian deadlift, rdl"),
        new Exercise("Bench press", "STRENGTH", 5.0, "chest press, dumbbell press"),
        new Exercise("Shoulder press", "STRENGTH", 5.0, "overhead press, military press"),
        new Exercise("Leg press", "STRENGTH", 5.0, ""),
        new Exercise("Lunges", "STRENGTH", 5.0, "lunge, walking lunges, split squat"),
        new Exercise("Hip thrust", "STRENGTH", 5.0, "glute bridge, hip thrusts"),
        new Exercise("Lat pulldown", "STRENGTH", 4.5, "pulldown"),
        new Exercise("Seated row", "STRENGTH", 4.5, "cable row, rows"),
        new Exercise("Dumbbell row", "STRENGTH", 4.5, "bent over row, one arm row"),
        new Exercise("Pull-ups", "STRENGTH", 6.0, "pull up, chin ups, assisted pull-up"),
        new Exercise("Push-ups", "STRENGTH", 5.0, "push up, pushups, knee push-ups"),
        new Exercise("Chest fly", "STRENGTH", 4.0, "pec deck, cable fly"),
        new Exercise("Leg curl", "STRENGTH", 4.0, "hamstring curl"),
        new Exercise("Leg extension", "STRENGTH", 4.0, ""),
        new Exercise("Calf raise", "STRENGTH", 3.5, "calf raises"),
        new Exercise("Bicep curl", "STRENGTH", 3.5, "biceps curl, hammer curl, curls"),
        new Exercise("Tricep pushdown", "STRENGTH", 3.5, "triceps extension, skull crusher, dips"),
        new Exercise("Lateral raise", "STRENGTH", 3.5, "side raise"),
        new Exercise("Kettlebell swing", "STRENGTH", 6.0, "kb swing"),
        new Exercise("Crunches", "STRENGTH", 3.8, "crunch, sit ups, leg raises"),
        new Exercise("Russian twist", "STRENGTH", 4.0, ""),
        // time based (machines, classes, timed holds)
        new Exercise("Treadmill walk", "TIME", 4.3, "walk, treadmill, brisk walk"),
        new Exercise("Treadmill incline walk", "TIME", 6.0, "incline walk, 12-3-30"),
        new Exercise("Treadmill run", "TIME", 9.8, "run, running, jog, jogging"),
        new Exercise("Cycling (stationary)", "TIME", 6.8, "cycle, cycling, bike, stationary bike"),
        new Exercise("Spin class", "TIME", 8.5, "spinning"),
        new Exercise("Elliptical", "TIME", 5.0, "cross trainer"),
        new Exercise("Rowing machine", "TIME", 7.0, "rower, rowing"),
        new Exercise("Stair climber", "TIME", 9.0, "stairmaster, stairs, step mill"),
        new Exercise("Jump rope", "TIME", 11.0, "skipping, skipping rope"),
        new Exercise("HIIT", "TIME", 8.0, "circuit, tabata, crossfit"),
        new Exercise("Burpees", "TIME", 8.0, "burpee"),
        new Exercise("Mountain climbers", "TIME", 8.0, ""),
        new Exercise("Plank", "TIME", 3.8, "planks, side plank"),
        new Exercise("Zumba", "TIME", 6.5, "aerobics, dance workout"),
        new Exercise("Yoga", "TIME", 2.5, "hatha yoga, surya namaskar"),
        new Exercise("Power yoga", "TIME", 4.0, "vinyasa"),
        new Exercise("Stretching", "TIME", 2.3, "cool down, mobility"),
        new Exercise("Swimming", "TIME", 6.0, "swim, laps"),
        new Exercise("Outdoor walk", "TIME", 3.5, "walking"),
        new Exercise("Outdoor run", "TIME", 9.8, ""),
        new Exercise("Badminton", "TIME", 5.5, ""),
        new Exercise("Weight training (general)", "TIME", 5.0, "gym, weights, strength session")
    );

    private final GymEntryRepo entries;
    private final DailyLogRepo logs;

    public GymService(GymEntryRepo entries, DailyLogRepo logs) {
        this.entries = entries;
        this.logs = logs;
    }

    public static Exercise find(String name) {
        if (name == null) return null;
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (Exercise e : CATALOG) {
            if (e.name().equalsIgnoreCase(n)) return e;
            for (String a : e.aliases().split(",")) if (!a.isBlank() && a.trim().equalsIgnoreCase(n)) return e;
        }
        return null;
    }

    public static double factor(String intensity) {
        if (intensity == null) return 1.0;
        return switch (intensity.toUpperCase(Locale.ROOT)) {
            case "LIGHT" -> 0.8;
            case "HARD" -> 1.2;
            default -> 1.0;
        };
    }

    /** Seconds for sets × reps: about 4 s per rep plus 90 s rest after each set. */
    public static double strengthMinutes(int sets, int reps) {
        return sets * (reps * 4 + 90) / 60.0;
    }

    public static int kcal(double met, double weightKg, double minutes) {
        return (int) Math.round(Math.max(0, met - 1) * weightKg * minutes / 60.0);
    }

    /** Fills in minutes / MET / kcal for an entry and saves it, then updates the day's totals. */
    @Transactional
    public GymEntry add(User u, GymEntry e, double weightKg, Double baseMet) {
        Exercise known = find(e.name);
        if (known != null) {
            e.name = known.name();
            if (e.kind == null) e.kind = known.kind();
        }
        if (e.kind == null) e.kind = (e.sets != null && e.reps != null) ? "STRENGTH" : "TIME";
        double base = baseMet != null ? baseMet : known != null ? known.met() : 5.0;
        e.met = Math.round(base * factor(e.intensity) * 10) / 10.0;
        if ("STRENGTH".equals(e.kind) && e.minutes <= 0) {
            int s = e.sets != null ? e.sets : 3;
            int r = e.reps != null ? e.reps : 10;
            e.minutes = Math.round(strengthMinutes(s, r) * 10) / 10.0;
        }
        e.kcal = kcal(e.met, weightKg, e.minutes);
        e.userId = u.id;
        entries.save(e);
        recalc(u.id, e.date);
        return e;
    }

    @Transactional
    public void delete(GymEntry e) {
        entries.delete(e);
        recalc(e.userId, e.date);
    }

    /** The day's gym minutes and kcal become the sum of the logged exercises. */
    @Transactional
    public void recalc(Long userId, LocalDate date) {
        List<GymEntry> list = entries.findByUserIdAndDateOrderById(userId, date);
        DailyLog log = logs.findByUserIdAndDate(userId, date).orElseGet(() -> {
            DailyLog l = new DailyLog();
            l.userId = userId;
            l.date = date;
            return l;
        });
        if (list.isEmpty()) {
            log.gymKcal = null;
            log.gymMinutes = null;
        } else {
            log.gymKcal = list.stream().mapToInt(x -> x.kcal).sum();
            log.gymMinutes = (int) Math.round(list.stream().mapToDouble(x -> x.minutes).sum());
        }
        logs.save(log);
    }
}
