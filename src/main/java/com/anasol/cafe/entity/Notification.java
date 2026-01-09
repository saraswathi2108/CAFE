

package com.anasol.cafe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notification_table",schema="notification")
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiver;  // person who will receive the notification
    @Lob
    @Column(columnDefinition = "text")
    private String message;  // message of the notification
    private String type;  // type of the notification can be "file" or "email" or "message" or "link"(external url) etc..
    private boolean read = false; // if the notification is read or not
    private String sender; // person who send the notification can be id of user or team or department, can be anything you like or can be null
    private String link; // onClick destination of the notification can be frontend url(this will be uppended with the base url), not an external url
    private String category; // can be live or project or team or department (if you menction team or deplartment you must menction team or department id's)
    private String kind; 

    private boolean stared=false;

    @Lob
    @Column(columnDefinition = "text")
    private String subject;

    @Column(nullable = false)
    private Boolean deleted = false;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
