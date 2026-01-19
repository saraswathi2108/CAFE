package com.anasol.cafe.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Data
public class NotificationDTO {
    private Long id;
    private String receiver;
    private String message;
    private String sender;
    private String type;
    private String link;
    private boolean read;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String category;
    private String kind;
    private String subject;
    private boolean stared;
    private boolean deleted;
    private String istFormattedTime;
    private String timeAgo;

    // Add getters and setters for new fields
    public String getIstFormattedTime() {
        return istFormattedTime;
    }

    public void setIstFormattedTime(String istFormattedTime) {
        this.istFormattedTime = istFormattedTime;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public boolean isRecent() {
        if (createdAt == null) return false;
        // Consider notification as "recent" if created within last 24 hours
        return createdAt.isAfter(LocalDateTime.now().minusHours(24));
    }

    public String getFormattedTime() {
        if (createdAt == null) return "";

        // Format as "2 hours ago", "Yesterday", etc.
        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " minutes ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + " hours ago";
        } else {
            return duration.toDays() + " days ago";
        }
    }

    // FIXED: Helper method to calculate time ago in IST
    public void calculateTimeAgo() {
        if (createdAt == null) {
            this.timeAgo = "";
            this.istFormattedTime = "";
            return;
        }

        // IMPORTANT: First, determine the timezone in which createdAt is stored
        // If your database stores UTC times (common practice), use UTC
        ZoneId sourceZone;

        // Option 1: If createdAt is stored in UTC (recommended)
        //sourceZone = ZoneId.of("UTC");

        // Option 2: If createdAt is stored in system default timezone
        // sourceZone = ZoneId.systemDefault();

        ZoneId istZone = ZoneId.of("Asia/Kolkata");

        // Convert from source timezone to IST
        ZonedDateTime sourceTime = createdAt.atZone(istZone);
        ZonedDateTime istCreatedAt = sourceTime.withZoneSameInstant(istZone);
        ZonedDateTime istNow = ZonedDateTime.now(istZone);

        // Format IST time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        this.istFormattedTime = istCreatedAt.format(formatter);

        // Calculate time ago
        long minutes = ChronoUnit.MINUTES.between(istCreatedAt, istNow);
        long hours = ChronoUnit.HOURS.between(istCreatedAt, istNow);
        long days = ChronoUnit.DAYS.between(istCreatedAt, istNow);

        if (minutes < 1) {
            this.timeAgo = "Just now";
        } else if (minutes < 60) {
            this.timeAgo = minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (hours < 24) {
            this.timeAgo = hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            this.timeAgo = days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }
}