package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.ScanRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScanRecordRepository extends JpaRepository<ScanRecord, Long> {

    Page<ScanRecord> findByRfidCode(String rfidCode, Pageable pageable);

    Page<ScanRecord> findByToolId(Long toolId, Pageable pageable);

    Page<ScanRecord> findByScanType(String scanType, Pageable pageable);

    @Query("SELECT s FROM ScanRecord s WHERE " +
           "(:rfidCode IS NULL OR s.rfidCode = :rfidCode) " +
           "AND (:scanType IS NULL OR s.scanType = :scanType) " +
           "AND (:startTime IS NULL OR s.scanTime >= :startTime) " +
           "AND (:endTime IS NULL OR s.scanTime <= :endTime)")
    Page<ScanRecord> search(@Param("rfidCode") String rfidCode,
                            @Param("scanType") String scanType,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime,
                            Pageable pageable);

    List<ScanRecord> findByToolIdOrderByScanTimeDesc(Long toolId);
}
