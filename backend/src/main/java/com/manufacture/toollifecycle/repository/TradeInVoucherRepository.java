package com.manufacture.toollifecycle.repository;

import com.manufacture.toollifecycle.entity.TradeInVoucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeInVoucherRepository extends JpaRepository<TradeInVoucher, Long> {

    Page<TradeInVoucher> findByVoucherStatus(String voucherStatus, Pageable pageable);

    Page<TradeInVoucher> findByToolId(Long toolId, Pageable pageable);

    @Query("SELECT v FROM TradeInVoucher v WHERE " +
           "(:voucherStatus IS NULL OR v.voucherStatus = :voucherStatus) " +
           "AND (:keyword IS NULL OR v.voucherNo LIKE %:keyword% " +
                "OR v.toolCode LIKE %:keyword% OR v.toolName LIKE %:keyword%)")
    Page<TradeInVoucher> search(@Param("voucherStatus") String voucherStatus,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    @Query("SELECT COUNT(v) > 0 FROM TradeInVoucher v WHERE v.toolId = :toolId AND v.voucherStatus = 'PENDING'")
    boolean hasPendingVoucherByToolId(@Param("toolId") Long toolId);
}
