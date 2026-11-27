package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

/** One exercise done on a day: sets × reps (STRENGTH) or minutes on a machine / activity (TIME). */
@Entity
@Table(indexes = @Index(columnList = "user_id,gym_date"))
public class GymEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "gym_date", nullable = false)
    public LocalDate date;
    @Column(nullable = false)
    public String name;
    /** STRENGTH or TIME */
    @Column(nullable = false)
    public String kind;
    public Integer sets;
    public Integer reps;
    /** weight lifted per rep, for your own record (does not change the calorie estimate) */
    public Double weightKg;
    /** for STRENGTH this is estimated from sets × reps unless you enter it */
    public double minutes;
    /** LIGHT, MODERATE or HARD */
    public String intensity;
    /** MET used for the estimate */
    public double met;
    public int kcal;
    public Instant createdAt = Instant.now();
}
