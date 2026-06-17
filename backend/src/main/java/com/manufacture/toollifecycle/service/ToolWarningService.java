package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ToolWarning;
import com.manufacture.toollifecycle.repository.ToolWarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ToolWarningService {

    private final ToolWarningRepository toolWarningRepository;

    public PageResult<ToolWarning> search(Integer isAcknowledged, String warningLevel,
                                          String warningType, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        Page<ToolWarning> result = toolWarningRepository.search(isAcknowledged, warningLevel, warningType, pageable);
        return toPageResult(result);
    }

    public long countUnacknowledged() {
        return toolWarningRepository.countByIsAcknowledged(0);
    }

    @Transactional
    public ToolWarning acknowledge(Long id, String acknowledgedBy) {
        ToolWarning warning = toolWarningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预警记录不存在: " + id));
        warning.setIsAcknowledged(1);
        warning.setAcknowledgedBy(acknowledgedBy);
        warning.setAcknowledgedTime(LocalDateTime.now());
        return toolWarningRepository.save(warning);
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
