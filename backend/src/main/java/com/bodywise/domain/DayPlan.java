package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Which whole-day flash card is the default plan for a date. */
@Entity
@Table(name = "day_card_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "plan_date"}))
public class DayPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "plan_date", nullable = false)
    public LocalDate date;
    @Column(nullable = false)
    public Long cardId;
}
