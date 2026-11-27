package com.bodywise.service;

import com.bodywise.domain.*;
import com.bodywise.repo.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Picks one whole-day flash card as the default plan for a date, and copies it into the food diary. */
@Service
public class PlanService {

    private final DayPlanRepo plans;
    private final FlashCardRepo cards;
    private final MealEntryRepo entries;
    private final DailyLogRepo logs;

    public PlanService(DayPlanRepo plans, FlashCardRepo cards, MealEntryRepo entries, DailyLogRepo logs) {
        this.plans = plans;
        this.cards = cards;
        this.entries = entries;
        this.logs = logs;
    }

    /** The card for the date, picking one at random (not yesterday's, when there is a choice) if none is set. */
    @Transactional
    public FlashCard ensurePlan(Long userId, LocalDate date) {
        List<FlashCard> options = cards.findByUserIdOrderByTitleAsc(userId);
        if (options.isEmpty()) return null;
        DayPlan plan = plans.findByUserIdAndDate(userId, date).orElse(null);
        if (plan != null) {
            FlashCard current = options.stream().filter(c -> c.id.equals(plan.cardId)).findFirst().orElse(null);
            if (current != null) return current;
        }
        Long yesterday = plans.findByUserIdAndDate(userId, date.minusDays(1)).map(p -> p.cardId).orElse(null);
        FlashCard pick = pickRandom(options, yesterday);
        save(plan, userId, date, pick.id);
        return pick;
    }

    @Transactional
    public FlashCard shuffle(Long userId, LocalDate date) {
        List<FlashCard> options = cards.findByUserIdOrderByTitleAsc(userId);
        if (options.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No flash cards yet");
        DayPlan plan = plans.findByUserIdAndDate(userId, date).orElse(null);
        FlashCard pick = pickRandom(options, plan != null ? plan.cardId : null);
        save(plan, userId, date, pick.id);
        return pick;
    }

    @Transactional
    public FlashCard setCard(Long userId, LocalDate date, Long cardId) {
        FlashCard card = cards.findById(cardId).filter(c -> c.userId.equals(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
        save(plans.findByUserIdAndDate(userId, date).orElse(null), userId, date, card.id);
        return card;
    }

    /** Copies the day's card into the diary. mealType null = every meal (and marks the plan as followed). */
    @Transactional
    public void follow(Long userId, LocalDate date, MealType mealOrNull) {
        FlashCard card = ensurePlan(userId, date);
        if (card == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No flash cards yet");
        for (MealType meal : MealType.values()) {
            if (mealOrNull != null && meal != mealOrNull) continue;
            List<CardItem> items = card.items.stream().filter(i -> i.mealType == meal && i.matched).toList();
            if (items.isEmpty() && mealOrNull == null) continue;
            entries.deleteAll(entries.findByUserIdAndDateAndMealType(userId, date, meal));
            for (CardItem item : items) {
                MealEntry m = new MealEntry();
                m.userId = userId;
                m.date = date;
                m.mealType = meal;
                m.foodId = item.foodId;
                m.name = item.name;
                m.unit = item.unit;
                m.quantity = item.quantity;
                m.kcal = item.kcal;
                m.protein = item.protein;
                m.carbs = item.carbs;
                m.fat = item.fat;
                m.fiber = item.fiber;
                m.fromPlan = true;
                entries.save(m);
            }
        }
        if (mealOrNull == null) setAnswer(userId, date, true);
    }

    @Transactional
    public void setAnswer(Long userId, LocalDate date, boolean followed) {
        DailyLog log = logs.findByUserIdAndDate(userId, date).orElseGet(() -> {
            DailyLog l = new DailyLog();
            l.userId = userId;
            l.date = date;
            return l;
        });
        log.planFollowed = followed;
        logs.save(log);
    }

    /** Called when a card is deleted: days that used it get a new random card when next viewed. */
    @Transactional
    public void forgetCard(Long cardId) {
        plans.deleteAll(plans.findByCardId(cardId));
    }

    private void save(DayPlan plan, Long userId, LocalDate date, Long cardId) {
        if (plan == null) {
            plan = new DayPlan();
            plan.userId = userId;
            plan.date = date;
        }
        plan.cardId = cardId;
        plans.save(plan);
    }

    private static FlashCard pickRandom(List<FlashCard> options, Long avoidId) {
        List<FlashCard> pool = options.size() > 1 && avoidId != null
                ? options.stream().filter(c -> !c.id.equals(avoidId)).toList()
                : options;
        if (pool.isEmpty()) pool = options;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }
}
