//package com.anasol.cafe.entity;
//
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "notifications")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class Notification {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private NotificationType type;
//
//    @Column(nullable = false)
//    private String title;
//
//    @Column(length = 500)
//    private String message;
//
//    @Column(name = "order_id")
//    private Long orderId;
//
//    @Column(name = "reference_id")
//    private Long referenceId;
//
//    @Column(name = "is_read")
//    private Boolean isRead;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//}