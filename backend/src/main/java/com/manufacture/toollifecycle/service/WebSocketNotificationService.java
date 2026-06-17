package com.manufacture.toollifecycle.service;

import com.manufacture.toollifecycle.dto.ToolHealthResult;
import com.manufacture.toollifecycle.entity.MachineTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendWarningNotification(MachineTool tool, ToolHealthResult healthResult) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "TOOL_WARNING");
        notification.put("toolId", tool.getId());
        notification.put("rfidCode", tool.getRfidCode());
        notification.put("toolName", tool.getToolName());
        notification.put("healthScore", healthResult.getCurrentHealthScore());
        notification.put("needsReplacement", healthResult.isNeedsReplacement());
        notification.put("message", String.format("刀具[%s]健康度: %.1f%%，%s",
                tool.getToolName(),
                healthResult.getCurrentHealthScore(),
                healthResult.isNeedsReplacement() ? "建议立即更换！" : "请尽快安排更换"));
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/warnings", notification);
        log.info("WebSocket预警通知已推送: toolId={}", tool.getId());
    }

    public void sendScanNotification(MachineTool tool, String scanType) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", "SCAN_EVENT");
        notification.put("toolId", tool.getId());
        notification.put("rfidCode", tool.getRfidCode());
        notification.put("toolName", tool.getToolName());
        notification.put("scanType", scanType);
        notification.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSend("/topic/scans", notification);
    }
}
