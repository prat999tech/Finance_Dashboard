package com.finance.dashboard.repository;

import com.finance.dashboard.entity.Transaction;
import com.finance.dashboard.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ─── Filtered listing (all filters optional) ───────────────────────────
    @Query("""
            SELECT t FROM Transaction t
            WHERE (:type     IS NULL OR t.type     = :type)
              AND (:category IS NULL OR LOWER(t.category) = LOWER(:category))
              AND (:from     IS NULL OR t.date     >= :from)
              AND (:to       IS NULL OR t.date     <= :to)
            ORDER BY t.date DESC
            """)
    Page<Transaction> findWithFilters(
            @Param("type")     TransactionType type,
            @Param("category") String category,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to,
            Pageable pageable);

    // ─── Dashboard aggregations ─────────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type")
    BigDecimal sumByType(@Param("type") TransactionType type);

    // Category-level breakdown: returns [category, totalAmount]
    @Query("""
            SELECT t.category, SUM(t.amount)
            FROM Transaction t
            WHERE t.type = :type
            GROUP BY t.category
            ORDER BY SUM(t.amount) DESC
            """)
    List<Object[]> sumByCategory(@Param("type") TransactionType type);

    // Monthly trend: returns [year, month, type, totalAmount]
    @Query("""
            SELECT YEAR(t.date), MONTH(t.date), t.type, SUM(t.amount)
            FROM Transaction t
            WHERE t.date >= :from
            GROUP BY YEAR(t.date), MONTH(t.date), t.type
            ORDER BY YEAR(t.date) ASC, MONTH(t.date) ASC
            """)
    List<Object[]> monthlyTrend(@Param("from") LocalDate from);

    // Recent activity (top N by date)
    List<Transaction> findTop10ByOrderByDateDesc();

    // Count active (non-deleted) transactions
    long countByDeletedAtIsNull();
}
