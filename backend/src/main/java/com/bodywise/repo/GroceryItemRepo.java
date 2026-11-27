package com.bodywise.repo;

import com.bodywise.domain.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroceryItemRepo extends JpaRepository<GroceryItem, Long> {
    List<GroceryItem> findByUserIdOrderById(Long userId);
    List<GroceryItem> findByListId(Long listId);
}
