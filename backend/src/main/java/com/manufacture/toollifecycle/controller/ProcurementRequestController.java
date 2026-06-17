package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ProcurementRequest;
import com.manufacture.toollifecycle.service.ProcurementRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementRequestController {

    private final ProcurementRequestService procurementRequestService;

    @GetMapping
    public ApiResponse<PageResult<ProcurementRequest>> list(
            @RequestParam(required = false) String erpStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(procurementRequestService.search(erpStatus, keyword, page, size));
    }

    @PostMapping("/{id}/retry")
    public ApiResponse<ProcurementRequest> retryErpSubmission(@PathVariable Long id) {
        return ApiResponse.success(procurementRequestService.retryErpSubmission(id));
    }
}
