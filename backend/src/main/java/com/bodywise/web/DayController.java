package com.bodywise.web;

import com.bodywise.domain.*;
import com.bodywise.repo.DailyLogRepo;
import com.bodywise.repo.MealEntryRepo;
import com.bodywise.service.NutritionService;
import com.bodywise.service.NutritionService.Targets;
import com.bodywise.service.PlanService;
import com.bodywise.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Page 1: one day's weight, gym, meals, plan and calorie / macro progress. */
@RestController
@RequestMapping("/api")
public class DayController {

    private final Support support;
    private final PlanService planService;
    private final NutritionService nutrition;
    private final MealEntryRepo entries;
    private final DailyLogRepo logs;

    public DayController(Support support, PlanService planService, NutritionService nutrition,
                         MealEntryRepo entries, DailyLogRepo logs) {
        this.support = support;
        this.planService = planService;
        this.nutrition = nutrition;
        this.entries = entries;
        this.logs = logs;
    }

    @GetMapping("/days/{date}")
    @Transactional
    public DaySummary day(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User u = support.user(jwt);
        FlashCard card = planService.ensurePlan(u.id, date);
        CardDto plan = card != null ? CardDto.of(card) : null;
        List<MealEntry> all = entries.findByUserIdAndDateOrderById(u.id, date);
        DailyLog log = logs.findByUserIdAndDate(u.id, date).orElse(null);
        DailyLog wlog = support.weightLogOn(u.id, date);
        Double weight = wlog != null ? wlog.weightKg : null;
        Targets t = nutrition.targets(u, date, weight, wlog != null ? wlog.date : null);
        // Exercises logged on the gym page win; otherwise estimate from plain gym minutes.
        int burn = log != null && log.gymKcal != null ? log.gymKcal : nutrition.gymBurn(u, log != null ? log.gymMinutes : null, t.weightKg());

        List<MealDto> meals = new ArrayList<>();
        Macros total = Macros.ZERO;
        for (MealType meal : MealType.values()) {
            List<ItemDto> items = all.stream().filter(e -> e.mealType == meal).map(ItemDto::of).toList();
            Macros mt = items.stream().map(ItemDto::total).reduce(Macros.ZERO, Macros::plus);
            total = total.plus(mt);
            meals.add(new MealDto(meal, items, mt.rounded(), plan != null ? plan.meal(meal) : null));
        }
        total = total.rounded();
        double net = Math.round((total.kcal() - burn) * 10) / 10.0;
        double remaining = Math.round((t.calories() - net) * 10) / 10.0;

        return new DaySummary(date, NutritionService.age(u.birthday, date), u.heightCm, weight, LogDto.of(log),
                meals, total, t, burn, net, remaining,
                total.protein() >= t.proteinG(), total.fiber() >= t.fiberG(),
                total.kcal() > 0 && net <= t.calories(), plan != null ? new PlanRef(plan.id(), plan.title()) : null,
                u.gymMet != null ? u.gymMet : 5.0);
    }

    /** Partial update: only keys present in the body change. Send null to clear a value. */
    @PatchMapping("/days/{date}/log")
    public LogDto updateLog(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestBody Map<String, Object> body) {
        DailyLog log = support.logFor(Support.uid(jwt), date);
        if (body.containsKey("weightKg")) log.weightKg = dbl(body.get("weightKg"));
        if (body.containsKey("gymMinutes")) log.gymMinutes = integer(body.get("gymMinutes"));
        if (body.containsKey("waterLiters")) log.waterLiters = dbl(body.get("waterLiters"));
        if (body.containsKey("steps")) log.steps = integer(body.get("steps"));
        if (body.containsKey("noSugar")) log.noSugar = body.get("noSugar") == null ? null : Boolean.valueOf(body.get("noSugar").toString());
        if (body.containsKey("ateOut")) log.ateOut = body.get("ateOut") == null ? null : Boolean.valueOf(body.get("ateOut").toString());
        return LogDto.of(logs.save(log));
    }

    @PostMapping("/days/{date}/entries")
    public ItemDto addEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @Valid @RequestBody EntryRequest r) {
        Long uid = Support.uid(jwt);
        return ItemDto.of(entries.save(entry(uid, date, r.mealType(), support.resolve(uid, r.item()))));
    }

    /** Adds several items to one meal at once (what the "type what you ate" box sends). */
    @PostMapping("/days/{date}/entries/bulk")
    @Transactional
    public List<ItemDto> addEntries(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @Valid @RequestBody BulkEntryRequest r) {
        Long uid = Support.uid(jwt);
        List<ItemDto> out = new ArrayList<>();
        for (ItemRequest item : r.items()) {
            out.add(ItemDto.of(entries.save(entry(uid, date, r.mealType(), support.resolve(uid, item)))));
        }
        return out;
    }

    private static MealEntry entry(Long uid, LocalDate date, MealType meal, CardItem it) {
        MealEntry e = new MealEntry();
        e.userId = uid;
        e.date = date;
        e.mealType = meal;
        e.foodId = it.foodId;
        e.name = it.name;
        e.unit = it.unit;
        e.quantity = it.quantity;
        e.kcal = it.kcal;
        e.protein = it.protein;
        e.carbs = it.carbs;
        e.fat = it.fat;
        e.fiber = it.fiber;
        return e;
    }

    @PutMapping("/entries/{id}")
    public ItemDto updateEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody EntryUpdate r) {
        MealEntry e = own(jwt, id);
        if (r.quantity() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be more than 0");
        e.quantity = r.quantity();
        return ItemDto.of(entries.save(e));
    }

    @DeleteMapping("/entries/{id}")
    public void deleteEntry(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        entries.delete(own(jwt, id));
    }

    /** The "Clear" button of a meal section. */
    @DeleteMapping("/days/{date}/meals/{meal}")
    @Transactional
    public void clearMeal(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @PathVariable MealType meal) {
        entries.deleteAll(entries.findByUserIdAndDateAndMealType(Support.uid(jwt), date, meal));
    }

    // ---- flash-card plan for the day

    /** Body {mealType: null} follows the whole day's plan; with a meal type, only that meal. */
    @PostMapping("/days/{date}/plan/follow")
    public void follow(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestBody(required = false) FollowRequest r) {
        planService.follow(Support.uid(jwt), date, r != null ? r.mealType() : null);
    }

    /** Answer to "Did you follow the flash card plan?" */
    @PostMapping("/days/{date}/plan/answer")
    public void answer(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestBody AnswerRequest r) {
        if (r.followed()) planService.follow(Support.uid(jwt), date, null);
        else planService.setAnswer(Support.uid(jwt), date, false);
    }

    /** Pick a different random card for the day. */
    @PostMapping("/days/{date}/plan/shuffle")
    public CardDto shuffle(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return CardDto.of(planService.shuffle(Support.uid(jwt), date));
    }

    @PutMapping("/days/{date}/plan")
    public CardDto setCard(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @Valid @RequestBody SetCardRequest r) {
        return CardDto.of(planService.setCard(Support.uid(jwt), date, r.cardId()));
    }

    private MealEntry own(Jwt jwt, Long id) {
        return entries.findById(id).filter(e -> e.userId.equals(Support.uid(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
    }

    static Double dbl(Object o) {
        if (o == null || "".equals(o)) return null;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.valueOf(o.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a number: " + o);
        }
    }

    static Integer integer(Object o) {
        Double d = dbl(o);
        return d == null ? null : (int) Math.round(d);
    }
}
