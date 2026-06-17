package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.dto.RfidScanRequest;
import com.manufacture.toollifecycle.dto.ToolHealthResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import com.manufacture.toollifecycle.service.MachineToolService;
import com.manufacture.toollifecycle.service.RfidScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class MachineToolController {

    private final MachineToolService machineToolService;
    private final RfidScanService rfidScanService;

    @GetMapping
    public ApiResponse<PageResult<MachineTool>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String toolType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(machineToolService.search(keyword, status, toolType, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<MachineTool> getById(@PathVariable Long id) {
        return ApiResponse.success(machineToolService.getById(id));
    }

    @GetMapping("/rfid/{rfidCode}")
    public ApiResponse<MachineTool> getByRfid(@PathVariable String rfidCode) {
        return ApiResponse.success(machineToolService.getByRfid(rfidCode));
    }

    @PostMapping
    public ApiResponse<MachineTool> create(@RequestBody MachineTool tool) {
        return ApiResponse.success(machineToolService.create(tool));
    }

    @PutMapping("/{id}")
    public ApiResponse<MachineTool> update(@PathVariable Long id, @RequestBody MachineTool tool) {
        return ApiResponse.success(machineToolService.update(id, tool));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        machineToolService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/rfid-scan")
    public ApiResponse<ToolHealthResult> processRfidScan(@Valid @RequestBody RfidScanRequest request) {
        return ApiResponse.success(rfidScanService.processScan(request));
    }

    @GetMapping("/health-warning")
    public ApiResponse<List<MachineTool>> getToolsBelowThreshold() {
        return ApiResponse.success(machineToolService.findToolsBelowThreshold());
    }

    @PostMapping("/{id}/recalculate-health")
    public ApiResponse<MachineTool> recalculateHealth(@PathVariable Long id) {
        return ApiResponse.success(machineToolService.recalculateHealth(id));
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Long>> getDashboardStats() {
        long inUse = machineToolService.countByStatus("IN_USE");
        long pendingReplace = machineToolService.countByStatus("PENDING_REPLACE");
        long scrapped = machineToolService.countByStatus("SCRAPPED");
        long replaced = machineToolService.countByStatus("REPLACED");
        return ApiResponse.success(Map.of(
                "inUse", inUse,
                "pendingReplace", pendingReplace,
                "scrapped", scrapped,
                "replaced", replaced,
                "total", inUse + pendingReplace + scrapped + replaced
        ));
    }
}
