//package com.anasol.cafe.controller;
//
//import com.anasol.cafe.dto.NotificationResponseDTO;
//import com.anasol.cafe.service.NotificationFetchService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/notifications")
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationController {
//
//    private final NotificationFetchService notificationFetchService;
//
//    /**
//     * Get notifications for the currently authenticated user
//     * Same authentication pattern as cart and order services
//     */
//    @GetMapping("/my")
//    public ResponseEntity<Page<NotificationResponseDTO>> getMyNotifications(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(defaultValue = "createdAt") String sortBy,
//            @RequestParam(defaultValue = "desc") String direction,
//            @RequestParam(required = false) Boolean unreadOnly) {
//
//        String methodName = "getMyNotifications";
//        log.info("{} - Getting notifications for authenticated user. Page: {}, Size: {}, UnreadOnly: {}",
//                methodName, page, size, unreadOnly);
//
//        try {
//            Page<NotificationResponseDTO> notifications = notificationFetchService
//                    .getMyNotifications(page, size, sortBy, direction, unreadOnly);
//
//            log.info("{} - Successfully retrieved {} notifications",
//                    methodName, notifications.getNumberOfElements());
//
//            return ResponseEntity.ok(notifications);
//
//        } catch (Exception e) {
//            log.error("{} - Error retrieving notifications", methodName, e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    /**
//     * Get unread notification count for the current user
//     */
//    @GetMapping("/my/unread-count")
//    public ResponseEntity<Map<String, Object>> getMyUnreadCount() {
//        String methodName = "getMyUnreadCount";
//        log.info("{} - Getting unread notification count", methodName);
//
//        try {
//            Long unreadCount = notificationFetchService.getMyUnreadNotificationCount();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("unreadCount", unreadCount);
//            response.put("timestamp", java.time.LocalDateTime.now());
//
//            log.info("{} - User has {} unread notifications", methodName, unreadCount);
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("{} - Error getting unread count", methodName, e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    /**
//     * Mark a notification as read
//     */
//    @PatchMapping("/{notificationId}/read")
//    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
//        String methodName = "markAsRead";
//        log.info("{} - Marking notification {} as read", methodName, notificationId);
//
//        try {
//            notificationFetchService.markNotificationAsRead(notificationId);
//            log.info("{} - Successfully marked notification {} as read", methodName, notificationId);
//            return ResponseEntity.ok().build();
//
//        } catch (Exception e) {
//            log.error("{} - Error marking notification as read", methodName, e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    /**
//     * Mark all notifications as read for current user
//     */
//    @PatchMapping("/mark-all-read")
//    public ResponseEntity<Map<String, Object>> markAllAsRead() {
//        String methodName = "markAllAsRead";
//        log.info("{} - Marking all notifications as read", methodName);
//
//        try {
//            int markedCount = notificationFetchService.markAllNotificationsAsRead();
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("markedCount", markedCount);
//            response.put("message", "Successfully marked " + markedCount + " notifications as read");
//            response.put("timestamp", java.time.LocalDateTime.now());
//
//            log.info("{} - Successfully marked {} notifications as read", methodName, markedCount);
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            log.error("{} - Error marking all notifications as read", methodName, e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    /**
//     * Delete a specific notification
//     */
//    @DeleteMapping("/{notificationId}")
//    public ResponseEntity<Void> deleteNotification(@PathVariable Long notificationId) {
//        String methodName = "deleteNotification";
//        log.info("{} - Deleting notification {}", methodName, notificationId);
//
//        try {
//            notificationFetchService.deleteNotification(notificationId);
//            log.info("{} - Successfully deleted notification {}", methodName, notificationId);
//            return ResponseEntity.ok().build();
//
//        } catch (Exception e) {
//            log.error("{} - Error deleting notification", methodName, e);
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//}