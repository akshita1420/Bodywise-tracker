package com.bodywise.repo;

import com.bodywise.domain.DayPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DayPlanRepo extends JpaRepository<DayPlan, Long> {
    Optional<DayPlan> findByUserIdAndDate(Long userId, LocalDate date);
    List<DayPlan> findByCardId(Long cardId);
}
