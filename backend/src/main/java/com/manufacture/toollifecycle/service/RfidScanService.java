package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.RfidScanRequest;
import com.manufacture.toollifecycle.dto.ToolHealthResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import com.manufacture.toollifecycle.entity.ProcurementRequest;
import com.manufacture.toollifecycle.entity.ScanRecord;
import com.manufacture.toollifecycle.entity.ToolWarning;
import com.manufacture.toollifecycle.repository.MachineToolRepository;
import com.manufacture.toollifecycle.repository.ProcurementRequestRepository;
import com.manufacture.toollifecycle.repository.ScanRecordRepository;
import com.manufacture.toollifecycle.repository.ToolWarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RfidScanService {

    private final MachineToolRepository machineToolRepository;
    private final ScanRecordRepository scanRecordRepository;
    private final ToolWarningRepository toolWarningRepository;
    private final ProcurementRequestRepository procurementRequestRepository;
    private final HealthCalculator healthCalculator;
    private final ErpIntegrationService erpIntegrationService;
    private final WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public ToolHealthResult processScan(RfidScanRequest request) {
        MachineTool tool = machineToolRepository
                .findByRfidCodeAndIsDeleted(request.getRfidCode(), 0)
                .orElseThrow(() -> new RuntimeException("未找到RFID编码对应的刀具: " + request.getRfidCode()));

        ScanRecord scanRecord = buildScanRecord(request, tool);
        scanRecordRepository.save(scanRecord);

        updateToolMetrics(tool, request);
        machineToolRepository.save(tool);

        ToolHealthResult healthResult = healthCalculator.calculate(tool);
        tool.setHealthScore(healthResult.getCurrentHealthScore());
        tool.setLastScanTime(LocalDateTime.now());

        if ("SCRAP".equals(request.getScanType())) {
            tool.setStatus("SCRAPPED");
        } else if ("REPLACE".equals(request.getScanType())) {
            tool.setStatus("REPLACED");
        } else if (healthResult.isNeedsReplacement()) {
            tool.setStatus("PENDING_REPLACE");
        }

        machineToolRepository.save(tool);

        if (healthResult.isNeedsWarning()) {
            createWarning(tool, healthResult);
            webSocketNotificationService.sendWarningNotification(tool, healthResult);
        }

        if (healthResult.isNeedsWarning() && !hasPendingProcurement(tool.getId())) {
            ProcurementRequest procurement = createProcurementRequest(tool, healthResult);
            procurementRequestRepository.save(procurement);
            erpIntegrationService.submitProcurementAsync(procurement);
        }

        log.info("RFID扫描处理完成: rfid={}, scanType={}, healthScore={}",
                request.getRfidCode(), request.getScanType(), healthResult.getCurrentHealthScore());

        return healthResult;
    }

    private ScanRecord buildScanRecord(RfidScanRequest request, MachineTool tool) {
        return ScanRecord.builder()
                .rfidCode(request.getRfidCode())
                .toolId(tool.getId())
                .scanType(request.getScanType())
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .workstationId(request.getWorkstationId())
                .gatewayId(request.getGatewayId())
                .cuttingHours(request.getCuttingHours() != null ? request.getCuttingHours() : BigDecimal.ZERO)
                .wearValue(request.getWearValue() != null ? request.getWearValue() : BigDecimal.ZERO)
                .scanTime(LocalDateTime.now())
                .remark(request.getRemark())
                .build();
    }

    private void updateToolMetrics(MachineTool tool, RfidScanRequest request) {
        if (request.getCuttingHours() != null && request.getCuttingHours().compareTo(BigDecimal.ZERO) > 0) {
            tool.setTotalCuttingHours(tool.getTotalCuttingHours().add(request.getCuttingHours()));
        }
        if (request.getWearValue() != null && request.getWearValue().compareTo(BigDecimal.ZERO) > 0) {
            tool.setAccumulatedWear(tool.getAccumulatedWear().add(request.getWearValue()));
        }
    }

    private void createWarning(MachineTool tool, ToolHealthResult healthResult) {
        String warningType = "HEALTH_LOW";
        String warningLevel = "URGENT";
        if (healthResult.getCurrentHealthScore().compareTo(new BigDecimal("10")) >= 0) {
            warningLevel = "WARNING";
        }

        String message = String.format("刀具[%s]健康度降至%.1f%%，%s。切削工时使用率: %.1f%%，磨损率: %.1f%%",
                tool.getToolName(),
                healthResult.getCurrentHealthScore(),
                healthResult.isNeedsReplacement() ? "建议立即更换" : "请尽快安排更换",
                healthResult.getHoursUsageRatio().multiply(new BigDecimal("100")),
                healthResult.getWearUsageRatio().multiply(new BigDecimal("100")));

        ToolWarning warning = ToolWarning.builder()
                .toolId(tool.getId())
                .rfidCode(tool.getRfidCode())
                .warningType(warningType)
                .warningLevel(warningLevel)
                .healthScore(healthResult.getCurrentHealthScore())
                .message(message)
                .isAcknowledged(0)
                .build();

        toolWarningRepository.save(warning);
        log.warn("刀具预警已生成: toolId={}, healthScore={}", tool.getId(), healthResult.getCurrentHealthScore());
    }

    private boolean hasPendingProcurement(Long toolId) {
        return procurementRequestRepository.findByToolId(toolId, org.springframework.data.domain.Pageable.unpaged())
                .getRecords()
                .stream()
                .anyMatch(p -> "PENDING".equals(p.getErpStatus()) || "SUBMITTED".equals(p.getErpStatus()));
    }

    private ProcurementRequest createProcurementRequest(MachineTool tool, ToolHealthResult healthResult) {
        String requestNo = "PR-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        String reason = String.format("刀具[%s]健康度降至%.1f%%（低于预警阈值%.0f%%），需自动提报采购。RFID: %s",
                tool.getToolName(),
                healthResult.getCurrentHealthScore(),
                healthResult.getWarningThreshold(),
                tool.getRfidCode());

        return ProcurementRequest.builder()
                .requestNo(requestNo)
                .toolId(tool.getId())
                .rfidCode(tool.getRfidCode())
                .toolCode(tool.getToolCode())
                .toolName(tool.getToolName())
                .specification(tool.getSpecification())
                .quantity(1)
                .reason(reason)
                .erpStatus("PENDING")
                .requestedBy("SYSTEM")
                .requestedTime(LocalDateTime.now())
                .build();
    }
}
