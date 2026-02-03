package com.anasol.cafe.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String receiver;  // Email or username

    @Column(nullable = false)
    private String message;

    private String sender;

    @Column(nullable = false)
    private String type;

    private String link;

    @Column(nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private String category;
    private String kind;
    private String subject;

    @Column(nullable = false)
    private boolean stared;

    @Column(nullable = false)
    private boolean deleted;

    // Check if you have a User relationship
    // If you have this field, it must be set when saving
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}