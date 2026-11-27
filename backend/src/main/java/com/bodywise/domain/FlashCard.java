package com.bodywise.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A whole-day meal plan: what to eat in every meal slot, written as plain text per slot
 * ("5 soaked almonds, 1 glass coriander seed water") and parsed into food items with calories.
 */
@Entity
@Table(name = "day_card")
public class FlashCard {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public Long userId;
    @Column(nullable = false)
    public String title;
    @Column(length = 1000)
    public String notes;

    /** The text typed for each meal slot. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "day_card_text", joinColumns = @JoinColumn(name = "card_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "meal_type", columnDefinition = "varchar(32)")
    @Column(name = "text", length = 2000)
    public Map<MealType, String> mealText = new EnumMap<>(MealType.class);

    /** Parsed items for all slots, each tagged with its meal type. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "day_card_item", joinColumns = @JoinColumn(name = "card_id"))
    @OrderColumn(name = "position")
    public List<CardItem> items = new ArrayList<>();
}
