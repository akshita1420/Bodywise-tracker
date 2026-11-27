package com.bodywise.repo;

import com.bodywise.domain.MealEntry;
import com.bodywise.domain.MealType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MealEntryRepo extends JpaRepository<MealEntry, Long> {
    List<MealEntry> findByUserIdAndDateOrderById(Long userId, LocalDate date);
    List<MealEntry> findByUserIdAndDateAndMealType(Long userId, LocalDate date, MealType mealType);
    List<MealEntry> findByUserIdAndMealTypeAndDateBetween(Long userId, MealType mealType, LocalDate from, LocalDate to);
}
