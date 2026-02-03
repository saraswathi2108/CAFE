package com.anasol.cafe.controller;

import com.anasol.cafe.dto.NotificationDTO;
import com.anasol.cafe.dto.NotificationRequest;
import com.anasol.cafe.entity.Notification;
import com.anasol.cafe.service.NotificationService;
import com.anasol.cafe.service.NotificationPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/cafe/notifications")
@RequiredArgsConstructor
@Slf4j

public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPushService pushService;
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {

        log.info("Getting unread notifications for authenticated user");
        List<NotificationDTO> notifications = notificationService.getUnreadNotifications(page, size);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/all")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {

        log.info("Getting all notifications for authenticated user");
        List<NotificationDTO> notifications = notificationService.getAllNotifications(page, size);
        return ResponseEntity.ok(notifications);
    }
    @PreAuthorize("hasAnyRole('MANAGER', 'STAFF', 'ADMIN','GODOWN_MANAGER')")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        log.info("Getting unread count for authenticated user");
        Long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable Long id) {
        log.info("Marking notification as read: {}", id);
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PutMapping("/star/{id}")
    public ResponseEntity<String> starNotification(@PathVariable Long id) {
        log.info("Starring notification: {}", id);
        notificationService.stardMessage(id);
        return ResponseEntity.ok("Notification starred");
    }

    @PutMapping("/unstar/{id}")
    public ResponseEntity<String> unstarNotification(@PathVariable Long id) {
        log.info("Unstarring notification: {}", id);
        notificationService.unstardMessage(id);
        return ResponseEntity.ok("Notification unstarred");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNotification(@PathVariable Long id) {
        log.info("Deleting notification: {}", id);
        boolean deleted = notificationService.deleteMessage(id);
        return ResponseEntity.ok("Notification deleted");
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<Notification>> getDeletedMessages() {
        log.info("Getting deleted notifications for authenticated user");
        List<Notification> notifications = notificationService.getDeletedMessages();
        return ResponseEntity.ok(notifications);
    }

    // SSE endpoints for real-time notifications
    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        log.info("Subscribing authenticated user to notifications");

        return pushService.subscribeToCurrentUser();
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe() {
        log.info("Unsubscribing authenticated user from notifications");
        String result = pushService.unsubscribeCurrentUser();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/online")
    public ResponseEntity<Boolean> isOnline() {
        log.info("Checking if authenticated user is online");
        // This would check if current user is online
        boolean isOnline = pushService.isCurrentUserOnline();
        return ResponseEntity.ok(isOnline);
    }

    // Admin endpoints (if needed)
    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @RequestBody NotificationRequest request) {
        log.info("Sending notification from authenticated user");
        notificationService.sendNotification(
                request.getMessage(),
                request.getSender(),
                request.getType(),
                request.getLink(),
                request.getCategory(),
                request.getKind(),
                request.getSubject()
        );
        return ResponseEntity.ok("Notification sent");
    }


}