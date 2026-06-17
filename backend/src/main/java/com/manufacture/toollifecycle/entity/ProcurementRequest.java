package com.manufacture.toollifecycle.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_procurement_request")
public class ProcurementRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_no", nullable = false, unique = true, length = 64)
    private String requestNo;

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

    @Column(name = "quantity")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "erp_status", length = 32)
    @Builder.Default
    private String erpStatus = "PENDING";

    @Column(name = "erp_request_id", length = 128)
    private String erpRequestId;

    @Column(name = "erp_response", length = 1024)
    private String erpResponse;

    @Column(name = "requested_by", length = 64)
    private String requestedBy;

    @Column(name = "requested_time")
    @Builder.Default
    private LocalDateTime requestedTime = LocalDateTime.now();

    @Column(name = "synced_time")
    private LocalDateTime syncedTime;

    @Column(name = "created_time", updatable = false)
    @Builder.Default
    private LocalDateTime createdTime = LocalDateTime.now();
}
