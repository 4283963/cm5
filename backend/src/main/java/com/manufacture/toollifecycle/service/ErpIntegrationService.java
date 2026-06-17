package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.entity.ProcurementRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpIntegrationService {

    @Value("${erp.base-url}")
    private String erpBaseUrl;

    @Value("${erp.procurement-endpoint}")
    private String procurementEndpoint;

    @Value("${erp.api-key}")
    private String erpApiKey;

    private final RestTemplate restTemplate;

    @Async
    public void submitProcurementAsync(ProcurementRequest procurement) {
        try {
            submitProcurement(procurement);
        } catch (Exception e) {
            log.error("ERP采购申请提交失败: requestNo={}, error={}", procurement.getRequestNo(), e.getMessage());
            procurement.setErpStatus("FAILED");
            procurement.setErpResponse(e.getMessage());
        }
    }

    private void submitProcurement(ProcurementRequest procurement) {
        String url = erpBaseUrl + procurementEndpoint;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", erpApiKey);

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestNo", procurement.getRequestNo());
        payload.put("materialCode", procurement.getToolCode());
        payload.put("materialName", procurement.getToolName());
        payload.put("specification", procurement.getSpecification());
        payload.put("quantity", procurement.getQuantity());
        payload.put("reason", procurement.getReason());
        payload.put("requestedBy", procurement.getRequestedBy());
        payload.put("requestedTime", procurement.getRequestedTime().toString());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        log.info("向ERP提交采购申请: requestNo={}, url={}", procurement.getRequestNo(), url);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            procurement.setErpStatus("SUBMITTED");
            procurement.setErpRequestId(String.valueOf(body.getOrDefault("requestId", "")));
            procurement.setErpResponse(body.toString());
            procurement.setSyncedTime(LocalDateTime.now());
            log.info("ERP采购申请提交成功: requestNo={}, erpRequestId={}",
                    procurement.getRequestNo(), procurement.getErpRequestId());
        } else {
            procurement.setErpStatus("FAILED");
            procurement.setErpResponse("HTTP " + response.getStatusCode());
            log.error("ERP采购申请提交失败: requestNo={}, httpStatus={}",
                    procurement.getRequestNo(), response.getStatusCode());
        }
    }
}
