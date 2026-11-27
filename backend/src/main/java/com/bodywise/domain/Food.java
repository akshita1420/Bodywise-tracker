package com.bodywise.domain;

import jakarta.persistence.*;

/** A food with macros for ONE unit (e.g. 1 piece, 1 bowl (150 g), 100 g). userId null = built-in. */
@Entity
public class Food {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    @Column(nullable = false)
    public String name;
    @Column(nullable = false)
    public String unit;
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    /** Other names it is written as, comma separated (e.g. "chapati, phulka"). Used by the text parser. */
    @Column(length = 500)
    public String aliases;
    /**
     * What you buy to make it, comma separated ("Lauki, Tomato, Turmeric, Oil").
     * null = the food itself is the ingredient (Apple, Almonds…); "" = nothing to buy (water).
     */
    @Column(length = 1000)
    public String ingredients;

    public Food() {}

    public Food(String name, String unit, double kcal, double protein, double carbs, double fat, double fiber) {
        this.name = name; this.unit = unit; this.kcal = kcal; this.protein = protein;
        this.carbs = carbs; this.fat = fat; this.fiber = fiber;
    }
}
