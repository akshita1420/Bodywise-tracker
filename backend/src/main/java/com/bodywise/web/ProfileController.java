package com.bodywise.web;

import com.bodywise.domain.User;
import com.bodywise.repo.UserRepo;
import com.bodywise.web.Dtos.ProfileDto;
import com.bodywise.web.Dtos.ProfileUpdate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final Support support;
    private final UserRepo users;

    public ProfileController(Support support, UserRepo users) {
        this.support = support;
        this.users = users;
    }

    @GetMapping
    public ProfileDto get(@AuthenticationPrincipal Jwt jwt) {
        return support.profile(support.user(jwt));
    }

    @PutMapping
    public ProfileDto update(@AuthenticationPrincipal Jwt jwt, @RequestBody ProfileUpdate p) {
        User u = support.user(jwt);
        u.name = p.name();
        u.birthday = p.birthday();
        u.heightCm = p.heightCm();
        if (p.sex() != null) u.sex = p.sex();
        u.goalWeightKg = p.goalWeightKg();
        if (p.activityLevel() != null) u.activityLevel = p.activityLevel();
        if (p.deficitKcal() != null) u.deficitKcal = p.deficitKcal();
        if (p.gymMet() != null) u.gymMet = p.gymMet();
        if (p.waterTargetL() != null) u.waterTargetL = p.waterTargetL();
        if (p.stepsTarget() != null) u.stepsTarget = p.stepsTarget();
        u.reminderEmail = p.reminderEmail();
        if (p.remindersEnabled() != null) u.remindersEnabled = p.remindersEnabled();
        users.save(u);
        return support.profile(u);
    }
}
