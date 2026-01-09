//package com.anasol.cafe.controller;
//
//
//
//import com.anasol.cafe.entity.Notification;
//import com.anasol.cafe.producer.NotificationProducer;
//import com.anasol.cafe.service.NotificationPushService;
//import com.anasol.cafe.service.NotificationService;
//import lombok.extern.slf4j.Slf4j;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/notification")
//@Slf4j
//public class NotificationController {
//
//    @Autowired
//    private NotificationService notificationService;
//
//    @Autowired
//    private NotificationProducer producer;
//
//    @Autowired
//    private NotificationPushService pushService;
//// @Autowired
//// private EmailService emailService;
//
//    @PostMapping("/send")
//    public ResponseEntity<String> sendNotification(@RequestBody Notification notification) {
//        log.info("Notification received: {}", notification);
//        notificationService.sendNotification(
//                notification.getReceiver(),
//                notification.getMessage(),
//                notification.getSender(),
//                notification.getType(),
//                notification.getLink(),
//                notification.getCategory(),
//                notification.getKind(),
//                notification.getSubject()
//        );
//        return ResponseEntity.ok("Notification sent");
//    }
//    @PostMapping("/sendList")
//    public ResponseEntity<String> sendNotificationList(@RequestBody List<Notification> notifications) {
//        log.info("Notification lsit of notifications: {}", notifications);
//        for(Notification notification:notifications) {
//            notificationService.sendNotification(
//                    notification.getReceiver(),
//                    notification.getMessage(),
//                    notification.getSender(),
//                    notification.getType(),
//                    notification.getLink(),
//                    notification.getCategory(),
//                    notification.getKind(),
//                    notification.getSubject()
//            );
//        }
//        return ResponseEntity.ok("Notification sent");
//    }
//
//    @GetMapping("/unread/{user}")
//    public ResponseEntity<List<Notification>> getUnread(
//            @PathVariable String user,
//            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
//            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
//        log.info("Getting unread notifications for user: {}", user);
//        return ResponseEntity.ok(notificationService.getUnreadNotifications(user, page, size));
//    }
//
//
//    @PutMapping("/stared/{messageId}")
//    public String Stared(@PathVariable Long messageId){
//        log.info("Staring message with id: {}", messageId);
//        notificationService.stardMessage(messageId);
//        return "done";
//    }
//
//    @PutMapping("/unStar/{messageId}")
//    public String unStar(@PathVariable Long messageId){
//        log.info("Unstaring message with id: {}", messageId);
//        notificationService.unstardMessage(messageId);
//        return "done";
//    }
//
//    @DeleteMapping("/delete/{messageId}")
//    public String delete(@PathVariable Long messageId){
//        log.info("Deleting message with id: {}", messageId);
//        notificationService.deleteMessage(messageId);
//        return "done";
//    }
//
//    @GetMapping("/all/{user}")
//    public ResponseEntity<List<Notification>> getAll(@PathVariable String user,
//    @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
//    @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
//        log.info("Getting all notifications for user: {}", user);
//        return ResponseEntity.ok(notificationService.getAllNotifications(user, page, size));
//    }
//
//    @PostMapping("/read/{id}")
//    public ResponseEntity<String> markRead(@PathVariable Long id) {
//        log.info("Marking notification with id: {} as read", id);
//        notificationService.markAsRead(id);
//        return ResponseEntity.ok("Notification marked as read");
//    }
//
//    @GetMapping("/unread-count/{receiver}")
//    public ResponseEntity<Long> getUnreadCount(@PathVariable String receiver) {
//        log.info("Getting unread count for user: {}", receiver);
//        return ResponseEntity.ok(notificationService.getUnreadCount(receiver));
//    }
//
//    @GetMapping("/isOnline/{username}")
//    public boolean isOnline(@PathVariable String username){
//        log.info("Checking if user is online: {}", username);
//        return pushService.isOnline(username);
//    }
//
//    @GetMapping("/subscribe/{username}")
//    public SseEmitter subscribe(@PathVariable String username) {
//        log.info("Subscribing user: {}", username);
//        return pushService.subscribe(username);
//    }
//
//    @GetMapping("/receive/{userId}")
//    public SseEmitter receive(@PathVariable String userId) {
//
//        return pushService.subscribe(userId);
//    }
//
//    @GetMapping("/unSubscribe/{username}")
//    public String unSubscribe(@PathVariable String username){
//        log.info("Unsubscribing user: {}", username);
//        return pushService.unSubscribe(username);
//    }
//    @GetMapping("/deletedMessages")
//    public List<Notification> getDeletedNotifications() {
//        log.info("Getting deleted notifications");
//        return notificationService.deletedMessage();
//    }
//}
