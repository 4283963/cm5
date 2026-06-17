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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RfidScanService {

    private static final Set<String> TERMINAL_STATUSES = new HashSet<>(Arrays.asList(
            "SCRAPPED", "REPLACED"
    ));

    private static final String STATUS_IN_USE = "IN_USE";
    private static final String STATUS_PENDING_REPLACE = "PENDING_REPLACE";
    private static final String STATUS_SCRAPPED = "SCRAPPED";
    private static final String STATUS_REPLACED = "REPLACED";

    private static final String SCAN_TYPE_INSPECT = "INSPECT";
    private static final String SCAN_TYPE_SCRAP = "SCRAP";
    private static final String SCAN_TYPE_REPLACE = "REPLACE";

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

        if (TERMINAL_STATUSES.contains(tool.getStatus())) {
            ScanRecord auditRecord = buildScanRecord(request, tool);
            auditRecord.setRemark(String.format(
                    "[非法操作被拦截] 刀具已处于终态[%s]，不允许执行扫描类型[%s]。原remark: %s",
                    tool.getStatus(),
                    request.getScanType(),
                    request.getRemark() == null ? "" : request.getRemark()
            ));
            scanRecordRepository.save(auditRecord);
            log.error("终态刀具非法扫描拦截: rfid={}, toolId={}, currentStatus={}, scanType={}, operator={}",
                    request.getRfidCode(), tool.getId(), tool.getStatus(),
                    request.getScanType(), request.getOperatorName());
            throw new IllegalStateException(String.format(
                    "刀具[%s]当前状态为[%s]，已终止生命周期，不允许再次扫描操作。",
                    tool.getToolName(), statusLabel(tool.getStatus())));
        }

        validateScanTransition(tool, request);

        ScanRecord scanRecord = buildScanRecord(request, tool);
        scanRecordRepository.save(scanRecord);

        updateToolMetrics(tool, request);

        ToolHealthResult healthResult = healthCalculator.calculate(tool);
        tool.setHealthScore(healthResult.getCurrentHealthScore());
        tool.setLastScanTime(LocalDateTime.now());

        if (SCAN_TYPE_SCRAP.equals(request.getScanType())) {
            tool.setStatus(STATUS_SCRAPPED);
        } else if (SCAN_TYPE_REPLACE.equals(request.getScanType())) {
            tool.setStatus(STATUS_REPLACED);
        } else if (healthResult.isNeedsReplacement()) {
            tool.setStatus(STATUS_PENDING_REPLACE);
        } else if (!STATUS_PENDING_REPLACE.equals(tool.getStatus())) {
            tool.setStatus(STATUS_IN_USE);
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

        log.info("RFID扫描处理完成: rfid={}, scanType={}, status={}, healthScore={}",
                request.getRfidCode(), request.getScanType(),
                tool.getStatus(), healthResult.getCurrentHealthScore());

        return healthResult;
    }

    private void validateScanTransition(MachineTool tool, RfidScanRequest request) {
        String current = tool.getStatus();
        String scanType = request.getScanType();

        if (SCAN_TYPE_SCRAP.equals(scanType)) {
            if (!STATUS_IN_USE.equals(current) && !STATUS_PENDING_REPLACE.equals(current)) {
                throw new IllegalStateException(String.format(
                        "刀具[%s]当前状态[%s]不允许执行报废操作，仅在用/待更换状态可报废。",
                        tool.getToolName(), statusLabel(current)));
            }
        } else if (SCAN_TYPE_REPLACE.equals(scanType)) {
            if (!STATUS_IN_USE.equals(current) && !STATUS_PENDING_REPLACE.equals(current)) {
                throw new IllegalStateException(String.format(
                        "刀具[%s]当前状态[%s]不允许执行更换操作，仅在用/待更换状态可更换。",
                        tool.getToolName(), statusLabel(current)));
            }
        }
    }

    private String statusLabel(String status) {
        switch (status) {
            case STATUS_IN_USE: return "在用";
            case STATUS_PENDING_REPLACE: return "待更换";
            case STATUS_SCRAPPED: return "已报废";
            case STATUS_REPLACED: return "已替换";
            default: return status;
        }
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
