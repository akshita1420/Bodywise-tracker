package com.bodywise.web;

import com.bodywise.domain.*;
import com.bodywise.service.FoodParser;
import com.bodywise.service.NutritionService.Targets;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Request / response shapes of the REST API. */
public final class Dtos {
    private Dtos() {}

    // ---- auth & profile
    public record SignupRequest(@Email @NotBlank String email, @NotBlank @Size(min = 6) String password, String name) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, ProfileDto profile) {}

    public record ProfileDto(Long id, String email, String name, LocalDate birthday, Integer age, Double heightCm,
                             String sex, Double goalWeightKg, String activityLevel, Integer deficitKcal, Double gymMet,
                             Double waterTargetL, Integer stepsTarget, String reminderEmail, boolean remindersEnabled,
                             Double currentWeightKg) {}

    public record ProfileUpdate(String name, LocalDate birthday, Double heightCm, String sex, Double goalWeightKg,
                                String activityLevel, Integer deficitKcal, Double gymMet, Double waterTargetL,
                                Integer stepsTarget, String reminderEmail, Boolean remindersEnabled) {}

    // ---- foods & items
    public record Macros(double kcal, double protein, double carbs, double fat, double fiber) {
        public static final Macros ZERO = new Macros(0, 0, 0, 0, 0);
        public Macros plus(Macros o) {
            return new Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat, fiber + o.fiber);
        }
        public Macros rounded() {
            return new Macros(r(kcal), r(protein), r(carbs), r(fat), r(fiber));
        }
        private static double r(double v) { return Math.round(v * 10) / 10.0; }
    }

    public record FoodDto(Long id, String name, String unit, double kcal, double protein, double carbs, double fat,
                          double fiber, boolean custom) {
        static FoodDto of(Food f) {
            return new FoodDto(f.id, f.name, f.unit, f.kcal, f.protein, f.carbs, f.fat, f.fiber, f.userId != null);
        }
    }

    public record FoodRequest(@NotBlank String name, @NotBlank String unit, double kcal, double protein, double carbs,
                              double fat, double fiber, String aliases, String ingredients) {}

    /** Macros are per ONE unit; multiply by quantity for the item's total. */
    public record ItemDto(Long id, Long foodId, String name, String unit, double quantity, double kcal, double protein,
                          double carbs, double fat, double fiber, boolean fromPlan, boolean matched, String raw,
                          Instant createdAt) {
        Macros total() {
            return new Macros(kcal * quantity, protein * quantity, carbs * quantity, fat * quantity, fiber * quantity);
        }
        static ItemDto of(MealEntry e) {
            return new ItemDto(e.id, e.foodId, e.name, e.unit, e.quantity, e.kcal, e.protein, e.carbs, e.fat, e.fiber,
                    e.fromPlan, true, null, e.createdAt);
        }
        static ItemDto of(CardItem c) {
            return new ItemDto(null, c.foodId, c.name, c.unit, c.quantity, c.kcal, c.protein, c.carbs, c.fat, c.fiber,
                    true, c.matched, c.raw, null);
        }
    }

    /** Either foodId (macros copied from the food) or name + macros for a one-off item. */
    public record ItemRequest(Long foodId, String name, String unit, Double quantity, Double kcal, Double protein,
                              Double carbs, Double fat, Double fiber) {}

    public record EntryRequest(@NotNull MealType mealType, Long foodId, String name, String unit, Double quantity,
                               Double kcal, Double protein, Double carbs, Double fat, Double fiber) {
        ItemRequest item() { return new ItemRequest(foodId, name, unit, quantity, kcal, protein, carbs, fat, fiber); }
    }

    public record BulkEntryRequest(@NotNull MealType mealType, @NotNull List<ItemRequest> items) {}

    public record EntryUpdate(@NotNull Double quantity) {}

    // ---- text parsing
    public record ParseRequest(String text) {}

    /** One parsed line. Macros are per unit. matched=false means "unknown food, please define it". */
    public record ParsedItem(String raw, String query, boolean matched, Long foodId, String name, String unit,
                             double quantity, double kcal, double protein, double carbs, double fat, double fiber,
                             String note, boolean approximate) {
        public static ParsedItem of(FoodParser.Parsed p) {
            Food f = p.food();
            if (f == null) return new ParsedItem(p.raw(), p.query(), false, null, p.query(), null, p.quantity(),
                    0, 0, 0, 0, 0, p.note(), false);
            return new ParsedItem(p.raw(), p.query(), true, f.id, f.name, f.unit, p.quantity(), f.kcal, f.protein,
                    f.carbs, f.fat, f.fiber, p.note(), p.approximate());
        }
    }

    // ---- flash cards (whole-day plans)
    public record CardMealDto(MealType mealType, String text, List<ItemDto> items, Macros totals) {}

    public record CardDto(Long id, String title, String notes, List<CardMealDto> meals, Macros totals, int unmatched) {
        public static CardDto of(FlashCard c) {
            List<CardMealDto> meals = new ArrayList<>();
            Macros all = Macros.ZERO;
            int unmatched = 0;
            for (MealType m : MealType.values()) {
                List<ItemDto> items = c.items.stream().filter(i -> i.mealType == m).map(ItemDto::of).toList();
                Macros t = items.stream().map(ItemDto::total).reduce(Macros.ZERO, Macros::plus);
                all = all.plus(t);
                unmatched += (int) items.stream().filter(i -> !i.matched()).count();
                meals.add(new CardMealDto(m, c.mealText.getOrDefault(m, ""), items, t.rounded()));
            }
            return new CardDto(c.id, c.title, c.notes, meals, all.rounded(), unmatched);
        }

        public CardMealDto meal(MealType m) {
            return meals.stream().filter(x -> x.mealType() == m).findFirst().orElse(null);
        }
    }

    /** meals: the text typed for each slot, e.g. {"BREAKFAST": "1 bowl paneer masala oats, 1 bowl sprouts"}. */
    public record CardRequest(@NotBlank String title, String notes, Map<MealType, String> meals) {}

    // ---- day
    public record LogDto(Double weightKg, Integer gymMinutes, Double waterLiters, Integer steps, Boolean planFollowed,
                         Boolean ateOut, Boolean noSugar, Integer gymKcal) {
        static LogDto of(DailyLog l) {
            return l == null ? new LogDto(null, null, null, null, null, null, null, null)
                    : new LogDto(l.weightKg, l.gymMinutes, l.waterLiters, l.steps, l.planFollowed, l.ateOut, l.noSugar, l.gymKcal);
        }
    }

    /** plan = what the day's flash card says for this meal (null if no card). */
    public record MealDto(MealType mealType, List<ItemDto> items, Macros totals, CardMealDto plan) {}

    public record PlanRef(Long id, String title) {}

    public record DaySummary(LocalDate date, Integer age, Double heightCm, Double currentWeightKg, LogDto log,
                             List<MealDto> meals, Macros totals, Targets targets, int gymBurnKcal, double netKcal,
                             double remainingKcal, boolean proteinReached, boolean fiberReached,
                             boolean caloriesWithin, PlanRef plan, double gymMet) {}

    public record FollowRequest(MealType mealType) {}
    public record AnswerRequest(boolean followed) {}
    public record SetCardRequest(@NotNull Long cardId) {}

    // ---- weight
    public record WeightPoint(LocalDate date, double weightKg) {}
    /** One week ending on Sunday: the Sunday weigh-in, the weekly average, and change vs. the previous Sunday. */
    public record WeekPoint(LocalDate sunday, Double sundayWeight, Double avgWeight, Double change, int daysLogged) {}

    // ---- habits
    public record HabitRow(LocalDate date, Double waterLiters, Integer steps, Integer gymMinutes,
                           boolean waterOk, boolean stepsOk, boolean gymOk, Boolean planFollowed, boolean ateOut,
                           boolean noSugar, List<String> sugarItems, int sugarKcal, double sugarCarbs) {}
    public record HabitsResponse(double waterTargetL, int stepsTarget, LocalDate startDate, List<HabitRow> rows) {}

    // ---- grocery
    public record GroceryIngredient(String key, String name, boolean purchased) {}
    /**
     * One dish of the plan and what to buy for it. ingredients = only those not already listed
     * under an earlier dish; alreadyListed = the ones it shares with earlier dishes.
     */
    public record GroceryDish(MealType mealType, String dish, String amount, List<GroceryIngredient> ingredients,
                              List<String> alreadyListed) {}
    public record GItemDto(Long id, String name, String quantity, boolean purchased) {}
    public record ListDto(Long id, String name, List<GItemDto> items) {}
    public record ToBuy(String source, String name, String quantity) {}
    public record GroceryView(LocalDate forDate, String planTitle, List<GroceryDish> dishes, List<ListDto> lists,
                              List<ToBuy> toBuy, boolean mailConfigured, String reminderEmail, boolean remindersEnabled) {}
    public record PlanCheck(@NotNull LocalDate date, @NotBlank String key, boolean purchased) {}
    public record NameRequest(@NotBlank String name) {}
    public record GItemRequest(@NotBlank String name, String quantity) {}
    public record GItemUpdate(String name, String quantity, Boolean purchased) {}
    public record SendResult(boolean sent, String message) {}

    // ---- gym
    public record ExerciseDto(String group, String name, String kind, double met, String tip) {}
    public record GymEntryDto(Long id, String name, String kind, Integer sets, Integer reps, Double weightKg,
                              double minutes, String intensity, double met, int kcal) {
        static GymEntryDto of(GymEntry e) {
            return new GymEntryDto(e.id, e.name, e.kind, e.sets, e.reps, e.weightKg, e.minutes, e.intensity, e.met, e.kcal);
        }
    }
    /** bodyWeightKg / weightSource: the weight the calorie estimate used. */
    public record GymDay(LocalDate date, double bodyWeightKg, String weightSource, List<GymEntryDto> entries,
                         double totalMinutes, int totalKcal) {}
    /** STRENGTH: sets + reps (+ weightKg); TIME: minutes. minutes on STRENGTH overrides the estimate. met overrides the catalog. */
    public record GymRequest(@NotBlank String name, String kind, Integer sets, Integer reps, Double weightKg,
                             Double minutes, String intensity, Double met) {}
}
