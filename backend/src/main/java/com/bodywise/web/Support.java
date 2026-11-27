package com.bodywise.web;

import com.bodywise.domain.CardItem;
import com.bodywise.domain.DailyLog;
import com.bodywise.domain.Food;
import com.bodywise.domain.User;
import com.bodywise.repo.DailyLogRepo;
import com.bodywise.repo.FoodRepo;
import com.bodywise.repo.UserRepo;
import com.bodywise.service.NutritionService;
import com.bodywise.web.Dtos.ItemRequest;
import com.bodywise.web.Dtos.ProfileDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;

/** Small helpers shared by controllers. */
@Component
public class Support {

    private final UserRepo users;
    private final FoodRepo foods;
    private final DailyLogRepo logs;
    private final Clock clock;

    public Support(UserRepo users, FoodRepo foods, DailyLogRepo logs, Clock clock) {
        this.users = users;
        this.foods = foods;
        this.logs = logs;
        this.clock = clock;
    }

    public static Long uid(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public User user(Jwt jwt) {
        return users.findById(uid(jwt)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** The latest log with a weight on or before the date. */
    public DailyLog weightLogOn(Long userId, LocalDate date) {
        return logs.findFirstByUserIdAndDateLessThanEqualAndWeightKgNotNullOrderByDateDesc(userId, date).orElse(null);
    }

    public Double weightOn(Long userId, LocalDate date) {
        return logs.findFirstByUserIdAndDateLessThanEqualAndWeightKgNotNullOrderByDateDesc(userId, date)
                .map(l -> l.weightKg).orElse(null);
    }

    public DailyLog logFor(Long userId, LocalDate date) {
        return logs.findByUserIdAndDate(userId, date).orElseGet(() -> {
            DailyLog l = new DailyLog();
            l.userId = userId;
            l.date = date;
            return l;
        });
    }

    public ProfileDto profile(User u) {
        LocalDate today = today();
        return new ProfileDto(u.id, u.email, u.name, u.birthday, NutritionService.age(u.birthday, today), u.heightCm,
                u.sex, u.goalWeightKg, u.activityLevel, u.deficitKcal, u.gymMet, u.waterTargetL, u.stepsTarget,
                u.reminderEmail, u.remindersEnabled, weightOn(u.id, today));
    }

    /** Turns a request item into per-unit macros, either from a saved food or from the values given. */
    public CardItem resolve(Long userId, ItemRequest r) {
        CardItem it = new CardItem();
        it.quantity = r.quantity() != null && r.quantity() > 0 ? r.quantity() : 1;
        if (r.foodId() != null) {
            Food f = foods.findById(r.foodId())
                    .filter(x -> x.userId == null || x.userId.equals(userId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));
            it.foodId = f.id;
            it.name = f.name;
            it.unit = f.unit;
            it.kcal = f.kcal;
            it.protein = f.protein;
            it.carbs = f.carbs;
            it.fat = f.fat;
            it.fiber = f.fiber;
            return it;
        }
        if (r.name() == null || r.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item needs a food or a name");
        }
        it.name = r.name().trim();
        it.unit = r.unit() != null && !r.unit().isBlank() ? r.unit().trim() : "serving";
        it.kcal = nz(r.kcal());
        it.protein = nz(r.protein());
        it.carbs = nz(r.carbs());
        it.fat = nz(r.fat());
        it.fiber = nz(r.fiber());
        return it;
    }

    private static double nz(Double d) {
        return d == null ? 0 : Math.max(0, d);
    }
}
