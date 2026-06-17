package com.manufacture.toollifecycle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_trade_in_voucher")
public class TradeInVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_no", nullable = false, unique = true, length = 64)
    private String voucherNo;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "rfid_code", nullable = false, length = 64)
    private String rfidCode;

    @Column(name = "tool_code", nullable = false, length = 64)
    private String toolCode;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "specification", length = 256)
    private String specification;

    @Column(name = "original_price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal originalPrice = BigDecimal.ZERO;

    @Column(name = "total_cutting_hours", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalCuttingHours = BigDecimal.ZERO;

    @Column(name = "three_month_output", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal threeMonthOutput = BigDecimal.ZERO;

    @Column(name = "health_score", precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(name = "residual_value", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "discount_rate", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal discountRate = BigDecimal.ZERO;

    @Column(name = "voucher_status", length = 32)
    @Builder.Default
    private String voucherStatus = "PENDING";

    @Column(name = "approve_by", length = 64)
    private String approveBy;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "reject_reason", length = 512)
    private String rejectReason;

    @Column(name = "supplier_status", length = 32)
    @Builder.Default
    private String supplierStatus = "PENDING";

    @Column(name = "supplier_request_id", length = 128)
    private String supplierRequestId;

    @Column(name = "supplier_response", length = 1024)
    private String supplierResponse;

    @Column(name = "supplier_sync_time")
    private LocalDateTime supplierSyncTime;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_time", updatable = false)
    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();
}
