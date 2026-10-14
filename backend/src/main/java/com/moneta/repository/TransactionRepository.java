package com.moneta.repository;

import com.moneta.entity.Category;
import com.moneta.entity.CategoryType;
import com.moneta.entity.Transaction;
import com.moneta.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUser(Long id, User user);

    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
           "AND (:startDate IS NULL OR t.date >= :startDate) " +
           "AND (:endDate IS NULL OR t.date <= :endDate) " +
           "AND (:category IS NULL OR LOWER(t.category) = LOWER(:category)) " +
           "AND (:categoryId IS NULL OR t.categoryEntity.id = :categoryId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "ORDER BY t.date DESC, t.id DESC")
    List<Transaction> findFilteredTransactions(@Param("user") User user,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate,
                                               @Param("category") String category,
                                               @Param("categoryId") Long categoryId,
                                               @Param("type") CategoryType type);

    boolean existsByUserAndCategoryEntity(User user, Category category);

    boolean existsByUserAndCategoryIgnoreCase(User user, String category);

    List<Transaction> findByUserAndDateGreaterThanEqual(User user, LocalDate startDate);

    List<Transaction> findByUserAndDateBetween(User user, LocalDate startDate, LocalDate endDate);
}
