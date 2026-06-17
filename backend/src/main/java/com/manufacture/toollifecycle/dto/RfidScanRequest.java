package com.manufacture.toollifecycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RfidScanRequest {

    @NotBlank(message = "RFID编码不能为空")
    private String rfidCode;

    @NotBlank(message = "扫描类型不能为空")
    private String scanType;

    private String operatorId;

    private String operatorName;

    private String workstationId;

    private String gatewayId;

    private BigDecimal cuttingHours;

    private BigDecimal wearValue;

    private String remark;
}
