package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One food item eaten in one meal slot. Macros are per unit, snapshotted when added. */
@Entity
@Table(indexes = @Index(columnList = "user_id,entry_date"))
public class MealEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "entry_date", nullable = false)
    public LocalDate date;
    // plain varchar (not a database enum) so new meal slots can be added later
    @Enumerated(EnumType.STRING) @Column(nullable = false, columnDefinition = "varchar(32)")
    public MealType mealType;
    public Long foodId;
    @Column(nullable = false)
    public String name;
    public String unit;
    public double quantity = 1;
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    public boolean fromPlan;
    public java.time.Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = java.time.Instant.now();
    }
}
