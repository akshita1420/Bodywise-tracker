package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "log_date"}))
public class DailyLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "log_date", nullable = false)
    public LocalDate date;
    public Double weightKg;
    public Integer gymMinutes;
    public Double waterLiters;
    public Integer steps;
    /** null = not answered yet, true = followed the flash card plan, false = entered own diet. */
    public Boolean planFollowed;
    /** Ate outside / restaurant food that day. Shown with a different tick on the habits page. */
    public Boolean ateOut;
    /** Went the day without added sugar. */
    public Boolean noSugar;
    /** Calories burned from the gym log (sum of exercises). null = no exercises logged, use gymMinutes. */
    public Integer gymKcal;
}
