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
@Table(name = "t_machine_tool")
public class MachineTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfid_code", nullable = false, unique = true, length = 64)
    private String rfidCode;

    @Column(name = "tool_code", nullable = false, length = 64)
    private String toolCode;

    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    @Column(name = "tool_type", length = 64)
    private String toolType;

    @Column(name = "specification", length = 256)
    private String specification;

    @Column(name = "machine_id", length = 64)
    private String machineId;

    @Column(name = "machine_name", length = 128)
    private String machineName;

    @Column(name = "status", length = 32)
    @Builder.Default
    private String status = "IN_USE";

    @Column(name = "total_cutting_hours", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalCuttingHours = BigDecimal.ZERO;

    @Column(name = "accumulated_wear", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal accumulatedWear = BigDecimal.ZERO;

    @Column(name = "health_score", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal healthScore = new BigDecimal("100");

    @Column(name = "max_cutting_hours", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal maxCuttingHours = new BigDecimal("500");

    @Column(name = "max_wear_limit", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal maxWearLimit = new BigDecimal("0.5000");

    @Column(name = "original_price", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal originalPrice = BigDecimal.ZERO;

    @Column(name = "residual_value", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Column(name = "three_month_output", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal threeMonthOutput = BigDecimal.ZERO;

    @Column(name = "installed_time")
    private LocalDateTime installedTime;

    @Column(name = "last_scan_time")
    private LocalDateTime lastScanTime;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "is_deleted")
    @Builder.Default
    private Integer isDeleted = 0;

    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
        updatedTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }
}
