//package com.anasol.cafe.repository;
//
//import com.anasol.cafe.entity.Notification;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.time.LocalDateTime;
//import java.util.List;
//@Repository
//public interface NotificationRepository extends JpaRepository<Notification, Long> {
//
//    // For getting user notifications
//    Page<Notification> findByUserId(Long userId, Pageable pageable);
//
//    // For getting unread notifications
//    Page<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead, Pageable pageable);
//
//    // For counting unread notifications
//    Long countByUserIdAndIsRead(Long userId, Boolean isRead);
//
//    // Optional: Find by order ID (for debugging)
//    List<Notification> findByOrderId(Long orderId);
//}