package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Remembers that an item of the auto-generated (flash card) grocery list was bought. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "for_date", "item_key"}))
public class PlanGroceryCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "for_date", nullable = false)
    public LocalDate date;
    @Column(name = "item_key", nullable = false)
    public String itemKey;
}
