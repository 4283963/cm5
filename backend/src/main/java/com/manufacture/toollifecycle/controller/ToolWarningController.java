package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ToolWarning;
import com.manufacture.toollifecycle.service.ToolWarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warnings")
@RequiredArgsConstructor
public class ToolWarningController {

    private final ToolWarningService toolWarningService;

    @GetMapping
    public ApiResponse<PageResult<ToolWarning>> list(
            @RequestParam(required = false) Integer isAcknowledged,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(required = false) String warningType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(toolWarningService.search(isAcknowledged, warningLevel, warningType, page, size));
    }

    @GetMapping("/unacknowledged-count")
    public ApiResponse<Map<String, Long>> getUnacknowledgedCount() {
        long count = toolWarningService.countUnacknowledged();
        return ApiResponse.success(Map.of("count", count));
    }

    @PutMapping("/{id}/acknowledge")
    public ApiResponse<ToolWarning> acknowledge(
            @PathVariable Long id,
            @RequestParam String acknowledgedBy) {
        return ApiResponse.success(toolWarningService.acknowledge(id, acknowledgedBy));
    }
}
