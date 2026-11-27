package com.bodywise.repo;

import com.bodywise.domain.GymEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface GymEntryRepo extends JpaRepository<GymEntry, Long> {
    List<GymEntry> findByUserIdAndDateOrderById(Long userId, LocalDate date);
}
