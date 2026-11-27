package com.bodywise.repo;

import com.bodywise.domain.FlashCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlashCardRepo extends JpaRepository<FlashCard, Long> {
    List<FlashCard> findByUserIdOrderByTitleAsc(Long userId);
}
