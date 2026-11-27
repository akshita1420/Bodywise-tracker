package com.bodywise.repo;

import com.bodywise.domain.DailyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLogRepo extends JpaRepository<DailyLog, Long> {
    Optional<DailyLog> findByUserIdAndDate(Long userId, LocalDate date);
    List<DailyLog> findByUserIdAndDateBetweenOrderByDate(Long userId, LocalDate from, LocalDate to);
    Optional<DailyLog> findFirstByUserIdAndDateLessThanEqualAndWeightKgNotNullOrderByDateDesc(Long userId, LocalDate date);
    Optional<DailyLog> findFirstByUserIdOrderByDateAsc(Long userId);
}
