package com.bodywise.repo;

import com.bodywise.domain.GroceryList;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroceryListRepo extends JpaRepository<GroceryList, Long> {
    List<GroceryList> findByUserIdOrderById(Long userId);
}
