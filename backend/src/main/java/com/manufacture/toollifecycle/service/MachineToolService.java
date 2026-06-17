package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import com.manufacture.toollifecycle.repository.MachineToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineToolService {

    private final MachineToolRepository machineToolRepository;
    private final HealthCalculator healthCalculator;

    public PageResult<MachineTool> search(String keyword, String status, String toolType,
                                          int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedTime"));
        Page<MachineTool> result = machineToolRepository.search(keyword, status, toolType, pageable);
        return toPageResult(result);
    }

    public MachineTool getById(Long id) {
        return machineToolRepository.findByIdAndIsDeleted(id, 0)
                .orElseThrow(() -> new RuntimeException("刀具不存在: " + id));
    }

    public MachineTool getByRfid(String rfidCode) {
        return machineToolRepository.findByRfidCodeAndIsDeleted(rfidCode, 0)
                .orElseThrow(() -> new RuntimeException("RFID编码对应的刀具不存在: " + rfidCode));
    }

    @Transactional
    public MachineTool create(MachineTool tool) {
        tool.setHealthScore(new BigDecimal("100"));
        tool.setTotalCuttingHours(BigDecimal.ZERO);
        tool.setAccumulatedWear(BigDecimal.ZERO);
        return machineToolRepository.save(tool);
    }

    @Transactional
    public MachineTool update(Long id, MachineTool updated) {
        MachineTool existing = getById(id);
        existing.setToolName(updated.getToolName());
        existing.setToolType(updated.getToolType());
        existing.setSpecification(updated.getSpecification());
        existing.setMachineId(updated.getMachineId());
        existing.setMachineName(updated.getMachineName());
        existing.setMaxCuttingHours(updated.getMaxCuttingHours());
        existing.setMaxWearLimit(updated.getMaxWearLimit());
        return machineToolRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        MachineTool tool = getById(id);
        tool.setIsDeleted(1);
        machineToolRepository.save(tool);
    }

    public List<MachineTool> findToolsBelowThreshold() {
        return machineToolRepository.findToolsBelowHealthThreshold(new BigDecimal("20"));
    }

    public long countByStatus(String status) {
        return machineToolRepository.countByStatusAndIsDeleted(status, 0);
    }

    @Transactional
    public MachineTool recalculateHealth(Long id) {
        MachineTool tool = getById(id);
        var result = healthCalculator.calculate(tool);
        tool.setHealthScore(result.getCurrentHealthScore());
        return machineToolRepository.save(tool);
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
