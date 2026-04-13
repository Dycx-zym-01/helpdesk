package com.example.helpdesk.controller;

import com.example.helpdesk.dto.NotificationModels;
import com.example.helpdesk.service.NotificationService;
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
    public List<NotificationModels.NotificationResponse> listNotifications(
            @RequestHeader(name = "X-USER-ID", required = false) Long userId) {
        return notificationService.listNotifications(userId);
    }

    @PostMapping("/{id}/read")
    public void markRead(@RequestHeader(name = "X-USER-ID", required = false) Long userId,
                         @PathVariable Long id) {
        notificationService.markRead(userId, id);
    }

    @PostMapping("/read-all")
    public void markAllRead(@RequestHeader(name = "X-USER-ID", required = false) Long userId) {
        notificationService.markAllRead(userId);
    }
}
