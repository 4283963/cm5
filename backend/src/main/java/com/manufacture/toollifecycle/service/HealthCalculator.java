package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.ToolHealthResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class HealthCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal WEIGHT_HOURS = new BigDecimal("0.4");
    private static final BigDecimal WEIGHT_WEAR = new BigDecimal("0.6");

    public ToolHealthResult calculate(MachineTool tool) {
        BigDecimal previousHealth = tool.getHealthScore();

        BigDecimal hoursUsageRatio = BigDecimal.ZERO;
        if (tool.getMaxCuttingHours() != null
                && tool.getMaxCuttingHours().compareTo(BigDecimal.ZERO) > 0) {
            hoursUsageRatio = tool.getTotalCuttingHours()
                    .divide(tool.getMaxCuttingHours(), 4, RoundingMode.HALF_UP);
        }

        BigDecimal wearUsageRatio = BigDecimal.ZERO;
        if (tool.getMaxWearLimit() != null
                && tool.getMaxWearLimit().compareTo(BigDecimal.ZERO) > 0) {
            wearUsageRatio = tool.getAccumulatedWear()
                    .divide(tool.getMaxWearLimit(), 4, RoundingMode.HALF_UP);
        }

        BigDecimal hoursHealth = HUNDRED.multiply(BigDecimal.ONE.subtract(hoursUsageRatio));
        BigDecimal wearHealth = HUNDRED.multiply(BigDecimal.ONE.subtract(wearUsageRatio));

        if (hoursHealth.compareTo(BigDecimal.ZERO) < 0) {
            hoursHealth = BigDecimal.ZERO;
        }
        if (wearHealth.compareTo(BigDecimal.ZERO) < 0) {
            wearHealth = BigDecimal.ZERO;
        }

        BigDecimal healthScore = hoursHealth.multiply(WEIGHT_HOURS)
                .add(wearHealth.multiply(WEIGHT_WEAR))
                .setScale(2, RoundingMode.HALF_UP);

        if (healthScore.compareTo(BigDecimal.ZERO) < 0) {
            healthScore = BigDecimal.ZERO;
        }
        if (healthScore.compareTo(HUNDRED) > 0) {
            healthScore = HUNDRED;
        }

        BigDecimal warningThreshold = new BigDecimal("20");
        boolean needsWarning = healthScore.compareTo(warningThreshold) < 0;
        boolean needsReplacement = healthScore.compareTo(new BigDecimal("5")) < 0;

        return ToolHealthResult.builder()
                .toolId(tool.getId())
                .rfidCode(tool.getRfidCode())
                .previousHealthScore(previousHealth)
                .currentHealthScore(healthScore)
                .hoursUsageRatio(hoursUsageRatio)
                .wearUsageRatio(wearUsageRatio)
                .needsWarning(needsWarning)
                .needsReplacement(needsReplacement)
                .warningThreshold(warningThreshold)
                .build();
    }
}
