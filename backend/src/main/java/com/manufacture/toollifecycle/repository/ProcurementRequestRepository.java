package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.ProcurementRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcurementRequestRepository extends JpaRepository<ProcurementRequest, Long> {

    Page<ProcurementRequest> findByErpStatus(String erpStatus, Pageable pageable);

    Page<ProcurementRequest> findByToolId(Long toolId, Pageable pageable);

    @Query("SELECT p FROM ProcurementRequest p WHERE " +
           "(:erpStatus IS NULL OR p.erpStatus = :erpStatus) " +
           "AND (:keyword IS NULL OR p.requestNo LIKE %:keyword% OR p.toolCode LIKE %:keyword% OR p.toolName LIKE %:keyword%)")
    Page<ProcurementRequest> search(@Param("erpStatus") String erpStatus,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);
}
