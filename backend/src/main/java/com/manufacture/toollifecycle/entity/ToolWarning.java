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
@Table(name = "t_tool_warning")
public class ToolWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "rfid_code", nullable = false, length = 64)
    private String rfidCode;

    @Column(name = "warning_type", nullable = false, length = 32)
    private String warningType;

    @Column(name = "warning_level", nullable = false, length = 16)
    private String warningLevel;

    @Column(name = "health_score", precision = 5, scale = 2)
    private BigDecimal healthScore;

    @Column(name = "message", length = 512)
    private String message;

    @Column(name = "is_acknowledged")
    @Builder.Default
    private Integer isAcknowledged = 0;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "acknowledged_time")
    private LocalDateTime acknowledgedTime;

    @Column(name = "created_time", updatable = false)
    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();
}
