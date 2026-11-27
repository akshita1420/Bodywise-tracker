package com.bodywise.service;

import com.bodywise.domain.CardItem;
import com.bodywise.domain.FlashCard;
import com.bodywise.domain.MealType;
import com.bodywise.repo.FlashCardRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Builds a whole-day card's food items from the text typed for each meal slot. */
@Service
public class CardService {

    private final FoodParser parser;
    private final FlashCardRepo cards;

    public CardService(FoodParser parser, FlashCardRepo cards) {
        this.parser = parser;
        this.cards = cards;
    }

    public void setMeals(FlashCard card, Map<MealType, String> texts) {
        card.mealText.clear();
        if (texts != null) {
            texts.forEach((meal, text) -> {
                if (meal != null && text != null && !text.isBlank()) card.mealText.put(meal, text.trim());
            });
        }
        reparse(card);
    }

    /** Re-reads every slot's text; call after foods change so earlier unknown items get calories. */
    public void reparse(FlashCard card) {
        card.items.clear();
        for (MealType meal : MealType.values()) {
            String text = card.mealText.get(meal);
            if (text == null) continue;
            for (FoodParser.Parsed p : parser.parse(card.userId, text)) {
                CardItem it = new CardItem();
                it.mealType = meal;
                it.raw = p.raw();
                it.quantity = p.quantity();
                if (p.food() != null) {
                    it.matched = true;
                    it.foodId = p.food().id;
                    it.name = p.food().name;
                    it.unit = p.food().unit;
                    it.kcal = p.food().kcal;
                    it.protein = p.food().protein;
                    it.carbs = p.food().carbs;
                    it.fat = p.food().fat;
                    it.fiber = p.food().fiber;
                } else {
                    it.matched = false;
                    it.name = p.query().isBlank() ? p.raw() : p.query();
                    it.unit = "serving";
                }
                card.items.add(it);
            }
        }
    }

    @Transactional
    public void reparseAll(Long userId) {
        for (FlashCard c : cards.findByUserIdOrderByTitleAsc(userId)) {
            if (c.items.stream().anyMatch(i -> !i.matched)) {
                reparse(c);
                cards.save(c);
            }
        }
    }
}
