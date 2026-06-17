package com.manufacture.toollifecycle.controller;

import com.manufacture.toollifecycle.dto.ApiResponse;
import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ScanRecord;
import com.manufacture.toollifecycle.service.ScanRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/scan-records")
@RequiredArgsConstructor
public class ScanRecordController {

    private final ScanRecordService scanRecordService;

    @GetMapping
    public ApiResponse<PageResult<ScanRecord>> list(
            @RequestParam(required = false) String rfidCode,
            @RequestParam(required = false) String scanType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(scanRecordService.search(rfidCode, scanType, startTime, endTime, page, size));
    }
}
