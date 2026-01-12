//package com.anasol.cafe.entity;
//
//public enum NotificationType {
//    ORDER_PLACED("Order Placed", true, true),
//    ORDER_APPROVED("Order Approved", true, true),
//    ORDER_REJECTED("Order Rejected", true, true),
//    ORDER_SHIPPED("Order Shipped", true, true),
//    ORDER_DELIVERED("Order Delivered", true, true),
//    ORDER_CANCELLED("Order Cancelled", true, true),
//    ORDER_UPDATED("Order Updated", true, false),
//    LOW_STOCK("Low Stock Alert", true, true),
//    NEW_USER("New User Registration", false, true),
//    SYSTEM_ALERT("System Alert", false, true);
//
//    private final String displayName;
//    private final boolean emailEnabled;
//    private final boolean pushEnabled;
//
//    NotificationType(String displayName, boolean emailEnabled, boolean pushEnabled) {
//        this.displayName = displayName;
//        this.emailEnabled = emailEnabled;
//        this.pushEnabled = pushEnabled;
//    }
//
//    public String getDisplayName() {
//        return displayName;
//    }
//
//    public boolean isEmailEnabled() {
//        return emailEnabled;
//    }
//
//    public boolean isPushEnabled() {
//        return pushEnabled;
//    }
//}