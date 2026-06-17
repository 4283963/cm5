package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.entity.MachineTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
public class ResidualValueCalculator {

    private static final BigDecimal MIN_RESIDUAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal WEIGHT_HEALTH = new BigDecimal("0.4");
    private static final BigDecimal WEIGHT_OUTPUT = new BigDecimal("0.4");
    private static final BigDecimal WEIGHT_HOURS = new BigDecimal("0.2");

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("2000.00");
    private static final BigDecimal HIGH_VALUE_BONUS = new BigDecimal("0.05");

    public ResidualResult calculate(MachineTool tool) {
        BigDecimal originalPrice = tool.getOriginalPrice() != null
                ? tool.getOriginalPrice() : BigDecimal.ZERO;

        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return ResidualResult.builder()
                    .residualValue(BigDecimal.ZERO)
                    .discountRate(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal healthFactor = calculateHealthFactor(tool);
        BigDecimal outputFactor = calculateOutputFactor(tool);
        BigDecimal hoursFactor = calculateHoursFactor(tool);

        BigDecimal baseRate = healthFactor.multiply(WEIGHT_HEALTH)
                .add(outputFactor.multiply(WEIGHT_OUTPUT))
                .add(hoursFactor.multiply(WEIGHT_HOURS));

        if (originalPrice.compareTo(HIGH_VALUE_THRESHOLD) >= 0) {
            baseRate = baseRate.add(HIGH_VALUE_BONUS);
        }

        if (baseRate.compareTo(MIN_RESIDUAL_RATE) < 0) {
            baseRate = MIN_RESIDUAL_RATE;
        }
        if (baseRate.compareTo(BigDecimal.ONE) > 0) {
            baseRate = BigDecimal.ONE;
        }

        BigDecimal residualValue = originalPrice.multiply(baseRate)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("残值计算完成: toolId={}, 原价={}, 健康度系数={}, 产值系数={}, 工时系数={}, 折价率={}, 残值={}",
                tool.getId(), originalPrice, healthFactor, outputFactor, hoursFactor, baseRate, residualValue);

        return ResidualResult.builder()
                .residualValue(residualValue)
                .discountRate(baseRate)
                .healthFactor(healthFactor)
                .outputFactor(outputFactor)
                .hoursFactor(hoursFactor)
                .build();
    }

    private BigDecimal calculateHealthFactor(MachineTool tool) {
        BigDecimal healthScore = tool.getHealthScore() != null
                ? tool.getHealthScore() : BigDecimal.ZERO;
        return healthScore.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOutputFactor(MachineTool tool) {
        BigDecimal output = tool.getThreeMonthOutput() != null
                ? tool.getThreeMonthOutput() : BigDecimal.ZERO;
        BigDecimal originalPrice = tool.getOriginalPrice() != null
                ? tool.getOriginalPrice() : BigDecimal.ONE;

        if (originalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal outputMultiple = output.divide(originalPrice, 4, RoundingMode.HALF_UP);

        if (outputMultiple.compareTo(new BigDecimal("2")) >= 0) {
            return new BigDecimal("0.20");
        } else if (outputMultiple.compareTo(new BigDecimal("1")) >= 0) {
            return new BigDecimal("0.15");
        } else if (outputMultiple.compareTo(new BigDecimal("0.5")) >= 0) {
            return new BigDecimal("0.10");
        } else {
            return new BigDecimal("0.05");
        }
    }

    private BigDecimal calculateHoursFactor(MachineTool tool) {
        BigDecimal maxHours = tool.getMaxCuttingHours();
        BigDecimal totalHours = tool.getTotalCuttingHours();

        if (maxHours == null || maxHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal usageRatio = totalHours.divide(maxHours, 4, RoundingMode.HALF_UP);
        BigDecimal remainingRatio = BigDecimal.ONE.subtract(usageRatio);

        if (remainingRatio.compareTo(BigDecimal.ZERO) < 0) {
            remainingRatio = BigDecimal.ZERO;
        }

        return remainingRatio.multiply(new BigDecimal("0.3"));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ResidualResult {
        private BigDecimal residualValue;
        private BigDecimal discountRate;
        private BigDecimal healthFactor;
        private BigDecimal outputFactor;
        private BigDecimal hoursFactor;
    }
}
