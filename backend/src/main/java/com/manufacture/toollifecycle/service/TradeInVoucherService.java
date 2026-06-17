package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import com.manufacture.toollifecycle.entity.ToolBudget;
import com.manufacture.toollifecycle.entity.TradeInVoucher;
import com.manufacture.toollifecycle.repository.MachineToolRepository;
import com.manufacture.toollifecycle.repository.TradeInVoucherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeInVoucherService {

    private final TradeInVoucherRepository tradeInVoucherRepository;
    private final MachineToolRepository machineToolRepository;
    private final ResidualValueCalculator residualValueCalculator;
    private final ToolBudgetService toolBudgetService;
    private final RestTemplate restTemplate;

    @Value("${supplier.base-url:http://supplier.internal.local/api}")
    private String supplierBaseUrl;

    @Value("${supplier.trade-in-endpoint:/trade-in/submit}")
    private String tradeInEndpoint;

    @Value("${supplier.api-key:supplier-api-key-placeholder}")
    private String supplierApiKey;

    public PageResult<TradeInVoucher> search(String voucherStatus, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        Page<TradeInVoucher> result = tradeInVoucherRepository.search(
                voucherStatus != null && voucherStatus.isEmpty() ? null : voucherStatus,
                keyword != null && keyword.isEmpty() ? null : keyword,
                pageable);
        return toPageResult(result);
    }

    public TradeInVoucher getById(Long id) {
        return tradeInVoucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("折价凭证不存在: " + id));
    }

    @Transactional
    public TradeInVoucher createVoucherForScrappedTool(Long toolId) {
        MachineTool tool = machineToolRepository.findByIdAndIsDeleted(toolId, 0)
                .orElseThrow(() -> new RuntimeException("刀具不存在: " + toolId));

        if (tradeInVoucherRepository.hasPendingVoucherByToolId(toolId)) {
            log.info("该刀具已存在待处理折价凭证，跳过创建: toolId={}", toolId);
            return null;
        }

        ResidualValueCalculator.ResidualResult residual = residualValueCalculator.calculate(tool);

        tool.setResidualValue(residual.getResidualValue());
        machineToolRepository.save(tool);

        String voucherNo = "TV-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        TradeInVoucher voucher = TradeInVoucher.builder()
                .voucherNo(voucherNo)
                .toolId(tool.getId())
                .rfidCode(tool.getRfidCode())
                .toolCode(tool.getToolCode())
                .toolName(tool.getToolName())
                .specification(tool.getSpecification())
                .originalPrice(tool.getOriginalPrice())
                .totalCuttingHours(tool.getTotalCuttingHours())
                .threeMonthOutput(tool.getThreeMonthOutput())
                .healthScore(tool.getHealthScore())
                .residualValue(residual.getResidualValue())
                .discountRate(residual.getDiscountRate())
                .voucherStatus("PENDING")
                .supplierStatus("PENDING")
                .createdBy("SYSTEM")
                .createdTime(LocalDateTime.now())
                .build();

        TradeInVoucher saved = tradeInVoucherRepository.save(voucher);
        log.info("以旧换新折价凭证已生成: voucherNo={}, toolId={}, residualValue={}, discountRate={}",
                voucherNo, toolId, residual.getResidualValue(), residual.getDiscountRate());

        return saved;
    }

    @Transactional
    public TradeInVoucher approve(Long id, String approver) {
        TradeInVoucher voucher = getById(id);
        if (!"PENDING".equals(voucher.getVoucherStatus())) {
            throw new IllegalStateException("凭证状态不是待审批，无法审批通过，当前状态: " + voucher.getVoucherStatus());
        }

        voucher.setVoucherStatus("APPROVED");
        voucher.setApproveBy(approver);
        voucher.setApproveTime(LocalDateTime.now());
        tradeInVoucherRepository.save(voucher);

        ToolBudget budget = toolBudgetService.createFromTradeInVoucher(voucher);

        submitToSupplierAsync(voucher);

        log.info("折价凭证审批通过: voucherNo={}, approver={}, budgetNo={}",
                voucher.getVoucherNo(), approver, budget.getBudgetNo());

        return voucher;
    }

    @Transactional
    public TradeInVoucher reject(Long id, String rejectReason, String approver) {
        TradeInVoucher voucher = getById(id);
        if (!"PENDING".equals(voucher.getVoucherStatus())) {
            throw new IllegalStateException("凭证状态不是待审批，无法驳回，当前状态: " + voucher.getVoucherStatus());
        }

        voucher.setVoucherStatus("REJECTED");
        voucher.setApproveBy(approver);
        voucher.setApproveTime(LocalDateTime.now());
        voucher.setRejectReason(rejectReason);
        tradeInVoucherRepository.save(voucher);

        log.info("折价凭证被驳回: voucherNo={}, approver={}, reason={}",
                voucher.getVoucherNo(), approver, rejectReason);

        return voucher;
    }

    @Async
    public void submitToSupplierAsync(TradeInVoucher voucher) {
        try {
            submitToSupplier(voucher);
        } catch (Exception e) {
            log.error("推送折价凭证到供应商系统失败: voucherNo={}, error={}", voucher.getVoucherNo(), e.getMessage());
            voucher.setSupplierStatus("FAILED");
            voucher.setSupplierResponse(e.getMessage());
            tradeInVoucherRepository.save(voucher);
        }
    }

    private void submitToSupplier(TradeInVoucher voucher) {
        String url = supplierBaseUrl + tradeInEndpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", supplierApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("voucherNo", voucher.getVoucherNo());
        payload.put("rfidCode", voucher.getRfidCode());
        payload.put("toolCode", voucher.getToolCode());
        payload.put("toolName", voucher.getToolName());
        payload.put("specification", voucher.getSpecification());
        payload.put("originalPrice", voucher.getOriginalPrice());
        payload.put("residualValue", voucher.getResidualValue());
        payload.put("discountRate", voucher.getDiscountRate());
        payload.put("healthScore", voucher.getHealthScore());
        payload.put("totalCuttingHours", voucher.getTotalCuttingHours());
        payload.put("threeMonthOutput", voucher.getThreeMonthOutput());
        payload.put("approvedBy", voucher.getApproveBy());
        payload.put("approvedTime", voucher.getApproveTime().toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        log.info("向供应商系统推送以旧换新凭证: voucherNo={}, url={}", voucher.getVoucherNo(), url);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            voucher.setSupplierStatus("SUBMITTED");
            voucher.setSupplierRequestId(String.valueOf(body.getOrDefault("requestId", "")));
            voucher.setSupplierResponse(body.toString());
            voucher.setSupplierSyncTime(LocalDateTime.now());
            tradeInVoucherRepository.save(voucher);
            log.info("供应商系统接收成功: voucherNo={}, supplierRequestId={}",
                    voucher.getVoucherNo(), voucher.getSupplierRequestId());
        } else {
            voucher.setSupplierStatus("FAILED");
            voucher.setSupplierResponse("HTTP " + response.getStatusCode());
            tradeInVoucherRepository.save(voucher);
        }
    }

    public TradeInVoucher retrySupplierSync(Long id) {
        TradeInVoucher voucher = getById(id);
        if (!"APPROVED".equals(voucher.getVoucherStatus())) {
            throw new IllegalStateException("仅审批通过的凭证可重新推送供应商");
        }
        voucher.setSupplierStatus("PENDING");
        tradeInVoucherRepository.save(voucher);
        submitToSupplierAsync(voucher);
        return voucher;
    }

    private <T> PageResult<T> toPageResult(Page<T> page) {
        return PageResult.<T>builder()
                .records(page.getContent())
                .total(page.getTotalElements())
                .current(page.getNumber() + 1)
                .size(page.getSize())
                .pages(page.getTotalPages())
                .build();
    }
}
