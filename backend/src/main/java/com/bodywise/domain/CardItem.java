package com.bodywise.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** One food line of a flash card. Macros are per unit. */
@Embeddable
public class CardItem {
    @Enumerated(EnumType.STRING)
    public MealType mealType;
    /** The text as typed, e.g. "5 soaked almonds". */
    public String raw;
    /** false when the text could not be matched to a food (calories then count as 0). */
    public boolean matched = true;
    public Long foodId;
    public String name;
    public String unit;
    public double quantity = 1;
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
}
