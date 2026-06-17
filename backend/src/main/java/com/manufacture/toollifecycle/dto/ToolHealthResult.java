package com.manufacture.toollifecycle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolHealthResult {

    private Long toolId;

    private String rfidCode;

    private BigDecimal previousHealthScore;

    private BigDecimal currentHealthScore;

    private BigDecimal hoursUsageRatio;

    private BigDecimal wearUsageRatio;

    private boolean needsWarning;

    private boolean needsReplacement;

    private BigDecimal warningThreshold;
}
