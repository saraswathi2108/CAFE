package com.anasol.cafe.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private String message;
    private String sender;
    private String type;
    private String link;
    private String category;
    private String kind;
    private String subject;
}