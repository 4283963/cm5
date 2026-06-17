package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ScanRecord;
import com.manufacture.toollifecycle.repository.ScanRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScanRecordService {

    private final ScanRecordRepository scanRecordRepository;

    public PageResult<ScanRecord> search(String rfidCode, String scanType,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "scanTime"));
        Page<ScanRecord> result = scanRecordRepository.search(rfidCode, scanType, startTime, endTime, pageable);
        return toPageResult(result);
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
