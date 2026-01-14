package com.anasol.cafe.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private String receiver;
    private String message;
    private String sender;
    private String type;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;
    private String category;
    private String kind;
    private String subject;
    private boolean stared;
    private boolean deleted;
    // Don't include User entity to avoid serialization issues
}