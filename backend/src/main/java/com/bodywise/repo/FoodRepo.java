package com.bodywise.repo;

import com.bodywise.domain.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FoodRepo extends JpaRepository<Food, Long> {
    @Query("select f from Food f where (f.userId is null or f.userId = :userId) "
            + "and lower(f.name) like lower(concat('%', :q, '%')) order by f.name")
    List<Food> search(@Param("userId") Long userId, @Param("q") String q);

    Optional<Food> findFirstByUserIdIsNullAndNameIgnoreCase(String name);

    @Query("select f from Food f where f.userId is null or f.userId = :userId")
    List<Food> visibleTo(@Param("userId") Long userId);
}
