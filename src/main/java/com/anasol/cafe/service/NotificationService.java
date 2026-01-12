//package com.anasol.cafe.service;
//
//import com.anasol.cafe.entity.Notification;
//import com.anasol.cafe.entity.NotificationType;
//import com.anasol.cafe.entity.User;
//
//import com.anasol.cafe.repository.NotificationRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationService {
//
//    private final NotificationRepository notificationRepository;
//
//
//    @Async
//    @Transactional
//    public void sendOrderNotification(User user, NotificationType type,
//                                     String title, String message,
//                                     Long orderId, Long referenceId) {
//        try {
//            // Save notification to database
//            Notification notification = Notification.builder()
//                    .user(user)
//                    .type(type)
//                    .title(title)
//                    .message(message)
//                    .orderId(orderId)
//                    .referenceId(referenceId)
//                    .isRead(false)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//
//            Notification savedNotification = notificationRepository.save(notification);
//            log.info("Notification saved: id={}, type={}, userId={}",
//                    savedNotification.getId(), type, user.getId());
//
//            // Send real-time notification via WebSocket
//            //sendRealTimeNotification(user, savedNotification);
//
//            // Send email notification (optional)
//            if (type.isEmailEnabled()) {
//                //sendEmailNotification(user, title, message);
//            }
//
//        } catch (Exception e) {
//            log.error("Failed to send notification: userId={}, type={}",
//                    user.getId(), type, e);
//        }
//    }
//
////    @Async
////    public void sendRealTimeNotification(User user, Notification notification) {
////        try {
////            webSocketService.sendNotification(user.getId(), notification);
////        } catch (Exception e) {
////            log.warn("Failed to send real-time notification to user: {}", user.getId(), e);
////        }
////    }
////
////    @Async
////    public void sendEmailNotification(User user, String subject, String content) {
////        try {
////            emailService.sendNotificationEmail(user.getEmail(), subject, content);
////        } catch (Exception e) {
////            log.warn("Failed to send email notification to user: {}", user.getId(), e);
////        }
////    }
////
//    // Send notification to admin users
//    @Async
//    @Transactional
//    public void sendAdminNotification(NotificationType type,
//                                     String title, String message,
//                                     Long orderId) {
//        try {
//            // This would typically fetch all admin users and send notifications to each
//            log.info("Admin notification: type={}, title={}, orderId={}",
//                    type, title, orderId);
//
//            // You can implement admin notification logic here
//
//        } catch (Exception e) {
//            log.error("Failed to send admin notification", e);
//        }
//    }
//}