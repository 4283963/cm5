package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.ToolBudget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolBudgetRepository extends JpaRepository<ToolBudget, Long> {

    Page<ToolBudget> findByBudgetStatus(String budgetStatus, Pageable pageable);

    Page<ToolBudget> findByTradeInVoucherId(Long tradeInVoucherId, Pageable pageable);

    @Query("SELECT b FROM ToolBudget b WHERE " +
           "(:budgetStatus IS NULL OR b.budgetStatus = :budgetStatus) " +
           "AND (:keyword IS NULL OR b.budgetNo LIKE %:keyword% " +
                "OR b.toolCode LIKE %:keyword% OR b.toolName LIKE %:keyword%)")
    Page<ToolBudget> search(@Param("budgetStatus") String budgetStatus,
                            @Param("keyword") String keyword,
                            Pageable pageable);
}
