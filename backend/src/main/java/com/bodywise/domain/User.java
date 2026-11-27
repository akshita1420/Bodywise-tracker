package com.bodywise.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(unique = true, nullable = false)
    public String email;
    @Column(nullable = false)
    public String passwordHash;
    public String name;
    public LocalDate birthday;
    public Double heightCm;
    /** FEMALE or MALE, used by the BMR formula. */
    public String sex = "FEMALE";
    public Double goalWeightKg;
    /** SEDENTARY, LIGHT, MODERATE, ACTIVE: day-to-day activity, gym excluded (gym is counted separately). */
    public String activityLevel = "SEDENTARY";
    public Integer deficitKcal = 500;
    /** MET value of a typical gym session, used to estimate calories burned. */
    public Double gymMet = 5.0;
    public Double waterTargetL = 3.45;
    public Integer stepsTarget = 10000;
    public String reminderEmail;
    public boolean remindersEnabled = true;
    /** The day the account was made; the habits page starts here. */
    public LocalDate createdOn;
}
