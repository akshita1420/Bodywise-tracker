package com.bodywise.repo;

import com.bodywise.domain.PlanGroceryCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PlanGroceryCheckRepo extends JpaRepository<PlanGroceryCheck, Long> {
    List<PlanGroceryCheck> findByUserIdAndDate(Long userId, LocalDate date);
    Optional<PlanGroceryCheck> findByUserIdAndDateAndItemKey(Long userId, LocalDate date, String itemKey);
}
