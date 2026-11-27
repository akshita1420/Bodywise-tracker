package com.bodywise.service;

import com.bodywise.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/**
 * Calorie and macro targets for a calorie-deficit plan.
 *
 *  BMR          Mifflin-St Jeor: 10·kg + 6.25·cm − 5·age + 5 (men) / −161 (women)
 *  Maintenance  BMR × activity factor (day-to-day activity, gym NOT included)
 *  Target       maintenance − deficit when above goal (never below 1200 women / 1500 men),
 *               maintenance + 300 when below goal, maintenance when at goal
 *  Gym          (MET − 1) × kg × hours, added on top of the target that day
 *  Protein      1.6 g per kg (goal weight while losing), fat 25 % of calories, carbs the rest
 *  Fibre        14 g per 1000 kcal, at least 25 g
 *  1 kg of body fat ≈ 7700 kcal, so a 550 kcal daily deficit ≈ 0.5 kg a week.
 */
@Service
public class NutritionService {

    public static final double KCAL_PER_KG_FAT = 7700;

    /** Everything that went into the numbers, so the app can explain them. */
    public record Targets(
            int calories, int maintenance, int bmr, double proteinG, double carbsG, double fatG, double fiberG,
            String mode, boolean profileComplete,
            // inputs actually used
            double weightKg, String weightSource, LocalDate weightDate, double heightCm, boolean heightIsDefault,
            int age, boolean ageIsDefault, String sex, String activityLevel, double activityFactor,
            Double goalWeightKg,
            // deficit
            int deficitChosen, int deficitActual, boolean floorApplied, int floorKcal,
            double weeklyChangeKg, Double weeksToGoal,
            // protein basis
            double proteinBasisKg) {}

    public static Integer age(LocalDate birthday, LocalDate on) {
        if (birthday == null) return null;
        return Period.between(birthday, on).getYears();
    }

    /** weightKg / weightDate: the latest logged weight on or before the day (null if never logged). */
    public Targets targets(User u, LocalDate on, Double weightKg, LocalDate weightDate) {
        Integer age = age(u.birthday, on);
        String weightSource;
        double w;
        if (weightKg != null) {
            w = weightKg;
            weightSource = "LOGGED";
        } else if (u.goalWeightKg != null) {
            w = u.goalWeightKg;
            weightSource = "GOAL";
        } else {
            w = 65;
            weightSource = "DEFAULT";
        }
        boolean complete = weightKg != null && u.heightCm != null && age != null;
        double h = u.heightCm != null ? u.heightCm : 160;
        int a = age != null ? age : 30;
        boolean male = "MALE".equalsIgnoreCase(u.sex);

        double bmr = 10 * w + 6.25 * h - 5 * a + (male ? 5 : -161);
        double factor = activityFactor(u.activityLevel);
        double maintenance = bmr * factor;

        int floor = male ? 1500 : 1200;
        int chosen = u.deficitKcal != null ? u.deficitKcal : 500;
        String mode = "MAINTAIN";
        double calories = maintenance;
        boolean floored = false;
        if (u.goalWeightKg != null && u.goalWeightKg < w - 0.5) {
            mode = "LOSE";
            calories = maintenance - chosen;
            if (calories < floor) {
                calories = floor;
                floored = true;
            }
        } else if (u.goalWeightKg != null && u.goalWeightKg > w + 0.5) {
            mode = "GAIN";
            calories = maintenance + 300;
        }
        int actualDeficit = (int) Math.round(maintenance - calories);
        double weekly = Math.round(-actualDeficit * 7 / KCAL_PER_KG_FAT * 100) / 100.0;
        Double weeksToGoal = null;
        if (u.goalWeightKg != null && weekly != 0) {
            double wk = (u.goalWeightKg - w) / weekly;
            if (wk > 0) weeksToGoal = Math.round(wk * 10) / 10.0;
        }

        double proteinBase = "LOSE".equals(mode) && u.goalWeightKg != null ? u.goalWeightKg : w;
        double protein = Math.round(1.6 * proteinBase);
        double fat = Math.round(calories * 0.25 / 9);
        double carbs = Math.max(0, Math.round((calories - protein * 4 - fat * 9) / 4));
        double fiber = Math.max(25, Math.round(14 * calories / 1000));

        return new Targets((int) Math.round(calories), (int) Math.round(maintenance), (int) Math.round(bmr),
                protein, carbs, fat, fiber, mode, complete,
                w, weightSource, weightDate, h, u.heightCm == null, a, age == null, male ? "MALE" : "FEMALE",
                u.activityLevel == null ? "SEDENTARY" : u.activityLevel, factor, u.goalWeightKg,
                chosen, actualDeficit, floored, floor, weekly, weeksToGoal, proteinBase);
    }

    /** Extra calories burned in the gym above resting: (MET − 1) × kg × hours. */
    public int gymBurn(User u, Integer minutes, double weightKg) {
        if (minutes == null || minutes <= 0) return 0;
        double met = u.gymMet != null ? u.gymMet : 5.0;
        return (int) Math.round(Math.max(0, met - 1) * weightKg * minutes / 60.0);
    }

    public static double activityFactor(String level) {
        if (level == null) return 1.2;
        return switch (level.toUpperCase()) {
            case "LIGHT" -> 1.375;
            case "MODERATE" -> 1.55;
            case "ACTIVE" -> 1.725;
            default -> 1.2;
        };
    }
}
