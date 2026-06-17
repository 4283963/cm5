package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ToolBudget;
import com.manufacture.toollifecycle.entity.TradeInVoucher;
import com.manufacture.toollifecycle.repository.ToolBudgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ToolBudgetService {

    private final ToolBudgetRepository toolBudgetRepository;

    public PageResult<ToolBudget> search(String budgetStatus, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        Page<ToolBudget> result = toolBudgetRepository.search(
                budgetStatus != null && budgetStatus.isEmpty() ? null : budgetStatus,
                keyword != null && keyword.isEmpty() ? null : keyword,
                pageable);
        return toPageResult(result);
    }

    public ToolBudget getById(Long id) {
        return toolBudgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算单不存在: " + id));
    }

    @Transactional
    public ToolBudget createFromTradeInVoucher(TradeInVoucher voucher) {
        String budgetNo = "BG-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        BigDecimal unitPrice = voucher.getOriginalPrice();
        BigDecimal totalAmount = unitPrice;
        BigDecimal subsidyAmount = voucher.getResidualValue();
        BigDecimal actualPayment = totalAmount.subtract(subsidyAmount);

        if (actualPayment.compareTo(BigDecimal.ZERO) < 0) {
            actualPayment = BigDecimal.ZERO;
        }

        ToolBudget budget = ToolBudget.builder()
                .budgetNo(budgetNo)
                .tradeInVoucherId(voucher.getId())
                .toolCode(voucher.getToolCode())
                .toolName(voucher.getToolName())
                .specification(voucher.getSpecification())
                .quantity(1)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .subsidyAmount(subsidyAmount)
                .actualPayment(actualPayment)
                .budgetStatus("ACTIVE")
                .sourceType("TRADE_IN")
                .createdBy("SYSTEM")
                .createdTime(LocalDateTime.now())
                .expireTime(LocalDateTime.now().plusMonths(3))
                .build();

        ToolBudget saved = toolBudgetRepository.save(budget);
        log.info("以旧换新预算单已创建: budgetNo={}, voucherNo={}, 原价={}, 补贴={}, 实付={}",
                budgetNo, voucher.getVoucherNo(), totalAmount, subsidyAmount, actualPayment);

        return saved;
    }

    @Transactional
    public ToolBudget markUsed(Long id) {
        ToolBudget budget = getById(id);
        if (!"ACTIVE".equals(budget.getBudgetStatus())) {
            throw new IllegalStateException("预算单状态不是可用，无法核销");
        }
        budget.setBudgetStatus("USED");
        return toolBudgetRepository.save(budget);
    }

    @Transactional
    public ToolBudget cancel(Long id, String reason) {
        ToolBudget budget = getById(id);
        budget.setBudgetStatus("CANCELLED");
        return toolBudgetRepository.save(budget);
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
