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
@Table(name = "t_scan_record")
public class ScanRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfid_code", nullable = false, length = 64)
    private String rfidCode;

    @Column(name = "tool_id", nullable = false)
    private Long toolId;

    @Column(name = "scan_type", nullable = false, length = 32)
    private String scanType;

    @Column(name = "operator_id", length = 64)
    private String operatorId;

    @Column(name = "operator_name", length = 64)
    private String operatorName;

    @Column(name = "workstation_id", length = 64)
    private String workstationId;

    @Column(name = "gateway_id", length = 64)
    private String gatewayId;

    @Column(name = "cutting_hours", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cuttingHours = BigDecimal.ZERO;

    @Column(name = "wear_value", precision = 10, scale = 4)
    @Builder.Default
    private BigDecimal wearValue = BigDecimal.ZERO;

    @Column(name = "scan_time")
    @Builder.Default
    private LocalDateTime scanTime = LocalDateTime.now();

    @Column(name = "remark", length = 512)
    private String remark;
}
