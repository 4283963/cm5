package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.MachineTool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineToolRepository extends JpaRepository<MachineTool, Long> {

    Optional<MachineTool> findByRfidCodeAndIsDeleted(String rfidCode, Integer isDeleted);

    Optional<MachineTool> findByIdAndIsDeleted(Long id, Integer isDeleted);

    Page<MachineTool> findByIsDeleted(Integer isDeleted, Pageable pageable);

    Page<MachineTool> findByStatusAndIsDeleted(String status, Integer isDeleted, Pageable pageable);

    @Query("SELECT t FROM MachineTool t WHERE t.isDeleted = 0 " +
           "AND (:status IS NULL OR t.status = :status) " +
           "AND (:toolType IS NULL OR t.toolType = :toolType) " +
           "AND (:keyword IS NULL OR t.toolName LIKE %:keyword% OR t.toolCode LIKE %:keyword% OR t.rfidCode LIKE %:keyword%)")
    Page<MachineTool> search(@Param("keyword") String keyword,
                             @Param("status") String status,
                             @Param("toolType") String toolType,
                             Pageable pageable);

    @Query("SELECT t FROM MachineTool t WHERE t.isDeleted = 0 AND t.healthScore < :threshold")
    List<MachineTool> findToolsBelowHealthThreshold(@Param("threshold") java.math.BigDecimal threshold);

    long countByStatusAndIsDeleted(String status, Integer isDeleted);
}
