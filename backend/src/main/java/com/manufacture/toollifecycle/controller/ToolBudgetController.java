package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ToolBudget;
import com.manufacture.toollifecycle.service.ToolBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tool-budgets")
@RequiredArgsConstructor
public class ToolBudgetController {

    private final ToolBudgetService toolBudgetService;

    @GetMapping
    public ApiResponse<PageResult<ToolBudget>> list(
            @RequestParam(required = false) String budgetStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(toolBudgetService.search(budgetStatus, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ToolBudget> getById(@PathVariable Long id) {
        return ApiResponse.success(toolBudgetService.getById(id));
    }

    @PutMapping("/{id}/mark-used")
    public ApiResponse<ToolBudget> markUsed(@PathVariable Long id) {
        return ApiResponse.success(toolBudgetService.markUsed(id));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<ToolBudget> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return ApiResponse.success(toolBudgetService.cancel(id, reason));
    }
}
