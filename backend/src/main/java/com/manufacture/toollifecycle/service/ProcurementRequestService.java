package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.PageResult;
import com.manufacture.toollifecycle.entity.ProcurementRequest;
import com.manufacture.toollifecycle.repository.ProcurementRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcurementRequestService {

    private final ProcurementRequestRepository procurementRequestRepository;
    private final ErpIntegrationService erpIntegrationService;

    public PageResult<ProcurementRequest> search(String erpStatus, String keyword,
                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdTime"));
        Page<ProcurementRequest> result = procurementRequestRepository.search(erpStatus, keyword, pageable);
        return toPageResult(result);
    }

    @Transactional
    public ProcurementRequest retryErpSubmission(Long id) {
        ProcurementRequest request = procurementRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("采购申请不存在: " + id));
        request.setErpStatus("PENDING");
        procurementRequestRepository.save(request);
        erpIntegrationService.submitProcurementAsync(request);
        return request;
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
