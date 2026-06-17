package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.ToolWarning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ToolWarningRepository extends JpaRepository<ToolWarning, Long> {

    Page<ToolWarning> findByIsAcknowledged(Integer isAcknowledged, Pageable pageable);

    Page<ToolWarning> findByToolId(Long toolId, Pageable pageable);

    Page<ToolWarning> findByWarningLevel(String warningLevel, Pageable pageable);

    @Query("SELECT w FROM ToolWarning w WHERE " +
           "(:isAcknowledged IS NULL OR w.isAcknowledged = :isAcknowledged) " +
           "AND (:warningLevel IS NULL OR w.warningLevel = :warningLevel) " +
           "AND (:warningType IS NULL OR w.warningType = :warningType)")
    Page<ToolWarning> search(@Param("isAcknowledged") Integer isAcknowledged,
                             @Param("warningLevel") String warningLevel,
                             @Param("warningType") String warningType,
                             Pageable pageable);

    long countByIsAcknowledged(Integer isAcknowledged);
}
