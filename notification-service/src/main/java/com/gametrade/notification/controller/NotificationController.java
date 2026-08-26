package com.gametrade.notification.controller;

import com.gametrade.common.api.ApiResponse;
import com.gametrade.common.web.GatewayHeaders;
import com.gametrade.notification.dto.NotificationResponse;
import com.gametrade.notification.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(@RequestHeader(GatewayHeaders.USER_ID) Long userId) {
        List<NotificationResponse> items = notificationService.listByUser(userId).stream()
                .map(NotificationResponse::from)
                .toList();
        return ApiResponse.success(items);
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ApiResponse.success();
    }
}
