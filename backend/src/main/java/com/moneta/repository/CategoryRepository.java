package com.moneta.repository;

import com.moneta.entity.Category;
import com.moneta.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserIsNullOrderByIdAsc();

    List<Category> findByUserOrderByIdAsc(User user);

    @Query("SELECT c FROM Category c WHERE c.user IS NULL OR c.user.id = :userId ORDER BY c.id ASC")
    List<Category> findAllAccessibleCategories(@Param("userId") Long userId);

    Optional<Category> findByNameIgnoreCaseAndUserIsNull(String name);

    Optional<Category> findByNameIgnoreCaseAndUser(String name, User user);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (c.user IS NULL OR c.user.id = :userId)")
    Optional<Category> findAccessibleCategoryByName(@Param("name") String name, @Param("userId") Long userId);

    boolean existsByNameIgnoreCaseAndUserIsNull(String name);

    boolean existsByNameIgnoreCaseAndUser(String name, User user);
}
