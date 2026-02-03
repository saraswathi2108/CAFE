//package com.anasol.cafe.dto;
//
//
//import com.anasol.cafe.entity.NotificationType;
//import lombok.Data;
//import java.time.LocalDateTime;
//
//@Data
//public class NotificationResponseDTO {
//    private Long id;
//    private NotificationType type;
//    private String title;
//    private String message;
//    private Long orderId;
//    private Long referenceId;
//    private Boolean isRead;
//    private LocalDateTime createdAt;
//
//    // Additional display fields
//    private String formattedTime;
//    private String timeAgo;
//
//    // Optional: Include related order/product info if needed
//    private OrderSummaryDTO orderSummary;
//    private ProductSummaryDTO productSummary;
//
//    @Data
//    public static class OrderSummaryDTO {
//        private Long id;
//        private String status;
//        private Double totalAmount;
//    }
//
//    @Data
//    public static class ProductSummaryDTO {
//        private Long id;
//        private String name;
//        private String imageUrl;
//    }
//}