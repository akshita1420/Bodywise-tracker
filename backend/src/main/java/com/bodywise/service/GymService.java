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

    /** group = muscle group / machine type shown on the gym page; tip = one line on what it works. */
    public record Exercise(String group, String name, String kind, double met, String aliases, String tip) {}

    private static Exercise s(String group, String name, double met, String aliases, String tip) {
        return new Exercise(group, name, "STRENGTH", met, aliases, tip);
    }

    private static Exercise t(String group, String name, double met, String aliases, String tip) {
        return new Exercise(group, name, "TIME", met, aliases, tip);
    }

    /**
     * MET values at moderate effort, based on the Compendium of Physical Activities (2011/2024):
     * resistance training 3.5 (light isolation) to 6 (heavy compound), circuit-style full-body moves ~8,
     * cardio machines by their listed activity. Light × 0.8, hard × 1.2 is applied on top.
     */
    public static final List<Exercise> CATALOG = List.of(
        // ---- Chest
        s("Chest", "Flat dumbbell bench press", 5.0, "dumbbell bench press, db bench press, flat bench", "Standard horizontal press for overall chest mass"),
        s("Chest", "Incline dumbbell bench press", 5.0, "incline dumbbell press, incline db press, incline press", "30–45° bench, targets the upper chest"),
        s("Chest", "Decline dumbbell bench press", 5.0, "decline dumbbell press, decline press", "Targets the lower chest fibres"),
        s("Chest", "Dumbbell floor press", 4.5, "floor press", "Shorter range, easier on shoulders, builds lockout strength"),
        s("Chest", "Dumbbell chest fly", 4.0, "dumbbell fly, db fly, incline fly, decline fly, chest fly", "Isolates the pecs through a wide arc (flat, incline or decline)"),
        s("Chest", "Squeeze press", 4.5, "crush press", "Two dumbbells pressed together for inner-chest tension"),
        s("Chest", "Dumbbell pullover", 4.0, "pullover", "Chest and lats, depending on elbow angle"),
        s("Chest", "Barbell bench press", 5.0, "bench press, bench, bb bench", "Classic barbell press for chest, shoulders and triceps"),
        s("Chest", "Push-ups", 5.0, "push up, pushups, knee push-ups", "Bodyweight press: chest, shoulders, triceps, core"),
        // ---- Back
        s("Back", "Single-arm dumbbell row", 4.5, "one arm dumbbell row, one arm row, dumbbell row, db row", "Knee and hand on a bench; isolates lats and mid-back"),
        s("Back", "Bent-over dumbbell row", 5.0, "bent over row, two arm dumbbell row, bent-over two-arm dumbbell row", "Upper back, lats and rhomboids together (any grip)"),
        s("Back", "Chest-supported incline row", 4.5, "incline row, chest supported row", "Face down on an incline bench, no momentum"),
        s("Back", "Gorilla row", 5.5, "", "Alternating rows from the floor in a wide hinge"),
        s("Back", "Renegade row", 6.0, "", "Rows from a high plank: back plus anti-rotation core"),
        s("Back", "Dumbbell shrugs", 3.5, "shrugs, shrug", "Isolates the upper traps"),
        s("Back", "Dumbbell deadlift", 6.0, "db deadlift, deadlift, deadlifts", "Lower back, glutes and hamstrings"),
        s("Back", "Stiff-leg deadlift", 5.5, "stiff leg deadlift, sldl", "Hamstrings and lower back with straighter legs"),
        s("Back", "Barbell deadlift", 6.0, "conventional deadlift, sumo deadlift", "Full posterior chain with a barbell"),
        s("Back", "Pull-ups", 6.0, "pull up, chin ups, chin-up", "Bodyweight pull for lats and biceps"),
        // ---- Shoulders
        s("Shoulders", "Dumbbell shoulder press", 5.0, "overhead shoulder press, shoulder press, overhead press, seated shoulder press, standing shoulder press, military press", "Main press for front and side delts"),
        s("Shoulders", "Arnold press", 5.0, "", "Rotate palms during the press, hits all three delt heads"),
        s("Shoulders", "Dumbbell lateral raise", 3.5, "lateral raise, side raise, lateral raises", "Side delts, for shoulder width"),
        s("Shoulders", "Dumbbell front raise", 3.5, "front raise, front raises", "Front delts"),
        s("Shoulders", "Bent-over reverse fly", 3.5, "reverse fly, rear delt raise, rear delt fly", "Rear delts"),
        s("Shoulders", "Dumbbell upright row", 4.0, "upright row", "Side delts and upper traps"),
        s("Shoulders", "Dumbbell push press", 6.0, "push press", "A little leg drive to press heavier overhead"),
        // ---- Biceps & forearms
        s("Biceps & forearms", "Bicep curl", 3.5, "biceps curl, dumbbell curl, curls, supinated curl, alternating curl", "Classic palm-up curl"),
        s("Biceps & forearms", "Hammer curl", 3.5, "hammer curls, neutral grip curl", "Palms facing in: brachialis, biceps, forearms"),
        s("Biceps & forearms", "Incline dumbbell curl", 3.5, "incline curl", "Seated back on an incline, stretches the long head"),
        s("Biceps & forearms", "Concentration curl", 3.0, "", "Seated single arm braced on the thigh, strict isolation"),
        s("Biceps & forearms", "Dumbbell preacher curl", 3.5, "preacher curl", "Over an angled pad, no swinging"),
        s("Biceps & forearms", "Spider curl", 3.5, "", "Chest down on an incline, arms hanging, peak contraction"),
        s("Biceps & forearms", "Zottman curl", 3.5, "", "Curl palms up, lower palms down: biceps and forearms"),
        s("Biceps & forearms", "Cross-body hammer curl", 3.5, "cross body curl", "Curl across the body toward the opposite shoulder"),
        s("Biceps & forearms", "Dumbbell wrist curl", 2.5, "wrist curl, wrist curls", "Forearm flexors"),
        s("Biceps & forearms", "Reverse wrist curl", 2.5, "reverse wrist curls", "Forearm extensors"),
        // ---- Triceps
        s("Triceps", "Overhead tricep extension", 3.5, "overhead triceps extension, overhead extension, triceps extension", "One dumbbell behind the head, stretches the long head"),
        s("Triceps", "Dumbbell skull crusher", 3.5, "skull crusher, skull crushers, lying tricep extension", "Lying on a bench, lower dumbbells toward the temples"),
        s("Triceps", "Dumbbell tricep kickback", 3.5, "tricep kickback, kickbacks", "Extend the arm back from a hinge"),
        s("Triceps", "Close-grip dumbbell press", 4.5, "close grip press", "Dumbbells together, elbows tucked"),
        s("Triceps", "Dumbbell Tate press", 3.5, "tate press", "Elbows flared, lower to the centre of the chest"),
        // ---- Legs & glutes
        s("Legs & glutes", "Goblet squat", 5.5, "goblet squats", "One dumbbell at the chest: quads, glutes, core"),
        s("Legs & glutes", "Dumbbell front squat", 5.5, "front squat", "Dumbbells at shoulder height"),
        s("Legs & glutes", "Suitcase squat", 5.5, "dumbbell squat", "Dumbbells at your sides"),
        s("Legs & glutes", "Barbell squat", 6.0, "squat, squats, back squat", "Barbell on the back: quads and glutes"),
        s("Legs & glutes", "Bulgarian split squat", 6.0, "split squat, bss", "Rear foot on a bench, single-leg squat"),
        s("Legs & glutes", "Dumbbell lunges", 5.5, "lunges, lunge, walking lunges, reverse lunges, forward lunges", "Forward, reverse or walking: quads, glutes, hamstrings"),
        s("Legs & glutes", "Dumbbell step-ups", 6.0, "step ups, step-up", "Step onto a box or bench under load"),
        s("Legs & glutes", "Dumbbell Romanian deadlift", 5.5, "romanian deadlift, rdl, db rdl", "Hip hinge for hamstrings and glutes"),
        s("Legs & glutes", "Single-leg Romanian deadlift", 5.0, "single leg rdl, single-leg deadlift", "Balance, hip stability and hamstrings"),
        s("Legs & glutes", "Dumbbell hip thrust", 5.0, "hip thrust, hip thrusts, dumbbell glute bridge, glute bridge", "Dumbbell on the hips to isolate the glutes"),
        s("Legs & glutes", "Dumbbell calf raise", 3.5, "calf raise, calf raises, seated calf raise", "Up onto the balls of your feet holding dumbbells"),
        // ---- Core & full body
        s("Core & full body", "Dumbbell Russian twist", 4.0, "russian twist, russian twists", "Seated rotation for the obliques"),
        s("Core & full body", "Weighted crunches", 4.0, "weighted sit-ups, crunches, crunch, sit ups, situps", "Dumbbell at the chest or overhead"),
        s("Core & full body", "Dumbbell woodchopper", 4.5, "woodchopper, wood chop", "Diagonal rotational swing"),
        s("Core & full body", "Dumbbell side bend", 3.5, "side bend, side bends", "Obliques"),
        t("Core & full body", "Farmer's carry", 6.0, "farmers walk, farmer walk, farmers carry", "Walk with heavy dumbbells: grip, traps, core"),
        s("Core & full body", "Dumbbell thruster", 8.0, "thruster, thrusters", "Squat straight into an overhead press"),
        s("Core & full body", "Man makers", 8.0, "man maker", "Push-up, renegade rows, squat clean and press"),
        s("Core & full body", "Kettlebell swing", 6.0, "kb swing, kettlebell swings", "Explosive hip hinge"),
        t("Core & full body", "Plank", 3.8, "planks, side plank", "Hold for time"),
        t("Core & full body", "Burpees", 8.0, "burpee", "Full-body conditioning"),
        t("Core & full body", "Mountain climbers", 8.0, "", "Fast knees from a plank"),
        // ---- Cardio machines
        t("Cardio machines", "Treadmill walk", 4.3, "walk, treadmill, brisk walk", "Walking at about 5–6 km/h"),
        t("Cardio machines", "Treadmill incline walk", 6.0, "incline walk, 12-3-30", "Walking on an incline"),
        t("Cardio machines", "Treadmill jog", 7.0, "jog, jogging", "Easy jog, about 7–8 km/h"),
        t("Cardio machines", "Treadmill run", 9.8, "run, running", "Running, about 9–10 km/h"),
        t("Cardio machines", "Elliptical cross-trainer", 5.0, "elliptical, cross trainer", "Low-impact full-body cardio"),
        t("Cardio machines", "Upright exercise bike", 6.8, "cycling, cycle, bike, stationary bike, exercise bike, cycling (stationary)", "Moderate cycling"),
        t("Cardio machines", "Recumbent bike", 5.5, "recumbent", "Seated, back-supported cycling"),
        t("Cardio machines", "Spin bike", 8.5, "spin class, spinning, indoor cycling", "Indoor cycling class pace"),
        t("Cardio machines", "Stair climber", 9.0, "stairmaster, stepmill, step mill, stairs", "Rotating staircase, intense lower body"),
        t("Cardio machines", "Rowing machine", 7.0, "rower, rowing, ergometer, erg", "Full-body cardio, back and legs"),
        t("Cardio machines", "Assault bike", 10.0, "air bike, airbike, fan bike", "Fan resistance with moving handles, HIIT"),
        t("Cardio machines", "SkiErg", 8.0, "ski erg", "Standing upper-body and core pulling cardio"),
        // ---- Leg machines
        s("Leg machines", "Leg press machine", 5.0, "leg press", "Quads, glutes, hamstrings (45° or horizontal)"),
        s("Leg machines", "Hack squat machine", 5.5, "hack squat", "Guided squat for quads and glutes"),
        s("Leg machines", "Pendulum squat machine", 5.5, "pendulum squat", "Guided squat variation, quad focused"),
        s("Leg machines", "Smith machine squat", 5.5, "smith squat, smith machine", "Guided barbell squat"),
        s("Leg machines", "Smith machine lunge", 5.0, "smith lunge", "Guided lunges"),
        s("Leg machines", "Smith machine calf raise", 3.5, "smith calf raise", "Guided calf raises"),
        s("Leg machines", "Leg extension machine", 4.0, "leg extension, leg extensions", "Isolates the quads"),
        s("Leg machines", "Seated leg curl machine", 4.0, "seated leg curl, leg curl, hamstring curl", "Isolates the hamstrings"),
        s("Leg machines", "Lying leg curl machine", 4.0, "lying leg curl", "Isolates the hamstrings"),
        s("Leg machines", "Hip abductor machine", 3.5, "abductor, hip abduction", "Outer glutes and hips"),
        s("Leg machines", "Hip adductor machine", 3.5, "adductor, hip adduction", "Inner thighs"),
        s("Leg machines", "Glute kickback machine", 4.0, "glute kickback, kickback machine", "Isolates the glutes"),
        s("Leg machines", "Hip thrust machine", 5.0, "", "Guided hip thrust for the glutes"),
        s("Leg machines", "Standing calf raise machine", 3.5, "standing calf raise", "Calves"),
        s("Leg machines", "Seated calf raise machine", 3.0, "seated calf raise machine", "Calves (soleus)"),
        // ---- Chest & back machines
        s("Chest & back machines", "Chest press machine", 4.5, "machine chest press, incline chest press, decline chest press", "Flat, incline or decline press"),
        s("Chest & back machines", "Pec deck machine", 4.0, "pec deck, pec fly, chest fly machine", "Isolates the chest"),
        s("Chest & back machines", "Cable crossover", 4.0, "cable fly, cable crossovers, cable chest fly", "Cable flies at any height"),
        s("Chest & back machines", "Cable pullover", 4.0, "straight arm pulldown", "Lats with straight arms"),
        s("Chest & back machines", "Lat pulldown", 4.5, "lat pull down, pulldown", "Upper back and lats"),
        s("Chest & back machines", "Seated cable row", 4.5, "cable row, seated row, rows", "Mid-back and lats"),
        s("Chest & back machines", "T-bar row", 5.0, "t bar row", "Plate-loaded row for back thickness"),
        s("Chest & back machines", "Chest-supported row machine", 4.5, "row machine, machine row", "Supported row, no momentum"),
        s("Chest & back machines", "Assisted pull-up machine", 5.0, "assisted pull-up, assisted pull up", "Counterweight helps with pull-ups"),
        s("Chest & back machines", "Assisted dip machine", 5.0, "assisted dip, dips", "Counterweight helps with dips"),
        // ---- Shoulder machines
        s("Shoulder machines", "Shoulder press machine", 4.5, "machine shoulder press", "Front and side delts"),
        s("Shoulder machines", "Lateral raise machine", 3.5, "machine lateral raise", "Isolates the side delts"),
        s("Shoulder machines", "Reverse pec deck", 3.5, "rear delt machine, rear delt fly machine", "Isolates the rear delts"),
        s("Shoulder machines", "Cable lateral raise", 3.5, "cable lateral", "Side delts on a cable"),
        s("Shoulder machines", "Cable front raise", 3.5, "", "Front delts on a cable"),
        s("Shoulder machines", "Face pull", 3.5, "face pulls", "Rear delts and upper back with a rope"),
        // ---- Arm machines
        s("Arm machines", "Bicep curl machine", 3.5, "machine curl", "Isolates the biceps"),
        s("Arm machines", "Preacher curl machine", 3.5, "seated preacher curl machine, machine preacher curl", "Isolates the biceps"),
        s("Arm machines", "Cable curl", 3.5, "cable curls, rope curl, ez bar cable curl", "Low pulley with a bar or rope"),
        s("Arm machines", "Tricep extension machine", 3.5, "machine tricep extension", "Seated overhead or press-down"),
        s("Arm machines", "Tricep dip machine", 4.5, "seated dip machine, dip machine", "Lever machine like parallel-bar dips"),
        s("Arm machines", "Cable tricep pushdown", 3.5, "tricep pushdown, triceps pushdown, rope pushdown, pushdown", "High pulley with a rope, V-bar or bar"),
        // ---- Classes & other
        t("Classes & other", "HIIT", 8.0, "circuit, tabata, crossfit", "Intervals"),
        t("Classes & other", "Jump rope", 11.0, "skipping, skipping rope", "Fast skipping"),
        t("Classes & other", "Zumba", 6.5, "aerobics, dance workout", "Dance cardio"),
        t("Classes & other", "Yoga", 2.5, "hatha yoga, surya namaskar", "Gentle yoga"),
        t("Classes & other", "Power yoga", 4.0, "vinyasa", "Flowing, faster yoga"),
        t("Classes & other", "Stretching", 2.3, "cool down, mobility", "Stretching and mobility"),
        t("Classes & other", "Swimming", 6.0, "swim, laps", "Moderate laps"),
        t("Classes & other", "Outdoor walk", 3.5, "walking", "Walking outside"),
        t("Classes & other", "Outdoor run", 9.8, "", "Running outside"),
        t("Classes & other", "Badminton", 5.5, "", "Social / casual play"),
        t("Classes & other", "Weight training (general)", 5.0, "gym, weights, strength session", "Whole session as one entry")
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
