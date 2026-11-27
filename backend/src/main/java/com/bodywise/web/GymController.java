package com.bodywise.web;

import com.bodywise.domain.DailyLog;
import com.bodywise.domain.GymEntry;
import com.bodywise.domain.User;
import com.bodywise.repo.GymEntryRepo;
import com.bodywise.service.GymService;
import com.bodywise.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Gym page: log exercises (sets × reps or minutes) and estimate calories burned. */
@RestController
@RequestMapping("/api")
public class GymController {

    private final GymService gym;
    private final GymEntryRepo entries;
    private final Support support;

    public GymController(GymService gym, GymEntryRepo entries, Support support) {
        this.gym = gym;
        this.entries = entries;
        this.support = support;
    }

    @GetMapping("/exercises")
    public List<ExerciseDto> exercises() {
        return GymService.CATALOG.stream().map(e -> new ExerciseDto(e.group(), e.name(), e.kind(), e.met(), e.tip())).toList();
    }

    @GetMapping("/days/{date}/gym")
    public GymDay day(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User u = support.user(jwt);
        List<GymEntryDto> list = entries.findByUserIdAndDateOrderById(u.id, date).stream().map(GymEntryDto::of).toList();
        double[] w = bodyWeight(u, date);
        return new GymDay(date, w[0], w[1] == 1 ? "LOGGED" : w[1] == 2 ? "GOAL" : "DEFAULT", list,
                Math.round(list.stream().mapToDouble(GymEntryDto::minutes).sum() * 10) / 10.0,
                list.stream().mapToInt(GymEntryDto::kcal).sum());
    }

    @PostMapping("/days/{date}/gym")
    public GymEntryDto add(@AuthenticationPrincipal Jwt jwt, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           @Valid @RequestBody GymRequest r) {
        User u = support.user(jwt);
        GymEntry e = new GymEntry();
        e.date = date;
        e.name = r.name().trim();
        e.kind = r.kind() == null || r.kind().isBlank() ? null : r.kind().toUpperCase();
        e.sets = r.sets();
        e.reps = r.reps();
        e.weightKg = r.weightKg();
        e.minutes = r.minutes() != null && r.minutes() > 0 ? r.minutes() : 0;
        e.intensity = r.intensity() == null ? "MODERATE" : r.intensity().toUpperCase();
        if ("TIME".equals(e.kind) && e.minutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter how many minutes");
        }
        return GymEntryDto.of(gym.add(u, e, bodyWeight(u, date)[0], r.met()));
    }

    @DeleteMapping("/gym/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        GymEntry e = entries.findById(id).filter(x -> x.userId.equals(Support.uid(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entry not found"));
        gym.delete(e);
    }

    /** [weight, source] with source 1 = logged, 2 = goal weight, 3 = 65 kg default. */
    private double[] bodyWeight(User u, LocalDate date) {
        DailyLog l = support.weightLogOn(u.id, date);
        if (l != null) return new double[]{l.weightKg, 1};
        if (u.goalWeightKg != null) return new double[]{u.goalWeightKg, 2};
        return new double[]{65, 3};
    }
}
