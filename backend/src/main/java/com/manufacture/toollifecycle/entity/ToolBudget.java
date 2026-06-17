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
@Table(name = "t_tool_budget")
public class ToolBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "budget_no", nullable = false, unique = true, length = 64)
    private String budgetNo;

    @Column(name = "trade_in_voucher_id")
    private Long tradeInVoucherId;

    @Column(name = "tool_code", nullable = false, length = 64)
    private String toolCode;

    @Column(name = "tool_name", length = 128)
    private String toolName;

    @Column(name = "specification", length = 256)
    private String specification;

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "subsidy_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(name = "actual_payment", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal actualPayment = BigDecimal.ZERO;

    @Column(name = "budget_status", length = 32)
    @Builder.Default
    private String budgetStatus = "ACTIVE";

    @Column(name = "source_type", length = 32)
    @Builder.Default
    private String sourceType = "TRADE_IN";

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_time", updatable = false)
    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();

    @Column(name = "expire_time")
    private LocalDateTime expireTime;
}
