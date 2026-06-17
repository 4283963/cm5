package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.TradeInVoucher;
import com.manufacture.toollifecycle.service.TradeInVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trade-in-vouchers")
@RequiredArgsConstructor
public class TradeInVoucherController {

    private final TradeInVoucherService tradeInVoucherService;

    @GetMapping
    public ApiResponse<PageResult<TradeInVoucher>> list(
            @RequestParam(required = false) String voucherStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(tradeInVoucherService.search(voucherStatus, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<TradeInVoucher> getById(@PathVariable Long id) {
        return ApiResponse.success(tradeInVoucherService.getById(id));
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<TradeInVoucher> approve(
            @PathVariable Long id,
            @RequestParam String approver) {
        return ApiResponse.success(tradeInVoucherService.approve(id, approver));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<TradeInVoucher> reject(
            @PathVariable Long id,
            @RequestParam String rejectReason,
            @RequestParam String approver) {
        return ApiResponse.success(tradeInVoucherService.reject(id, rejectReason, approver));
    }

    @PostMapping("/{id}/retry-supplier")
    public ApiResponse<TradeInVoucher> retrySupplierSync(@PathVariable Long id) {
        return ApiResponse.success(tradeInVoucherService.retrySupplierSync(id));
    }
}
