package com.anasol.cafe.service;

import com.anasol.cafe.dto.NotificationDTO;
import com.anasol.cafe.entity.Notification;
import com.anasol.cafe.exceptions.ResourceNotFoundException;
import com.anasol.cafe.exceptions.ValidationException;
import com.anasol.cafe.repository.NotificationRepository;
import com.anasol.cafe.repository.UserRepository;
import com.anasol.cafe.entity.User;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationPushService pushService;


    private User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ValidationException("User not authenticated");
        }

        String email = authentication.getName();
        log.debug("Getting authenticated user with email: {}", email);

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public void sendNotificationToUser(String receiverEmail, String message, String sender, String type,
                                       String link, String category, String kind, String subject) {

        log.info("Sending notification to: {}, type: {}, category: {}", receiverEmail, type, category);

        // Get user by email
        User receiverUser = userRepository.findByEmail(receiverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + receiverEmail));

        // Use IST timezone
        ZonedDateTime istNow = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

        Notification notification = Notification.builder()
                .receiver(receiverEmail)
                .message(message)
                .sender(sender)
                .type(type)
                .link(link)
                .read(false)
                .createdAt(istNow.toLocalDateTime()) // Convert to LocalDateTime for storage
                .category(category)
                .kind(kind)
                .subject(subject)
                .stared(false)
                .deleted(false)
                .user(receiverUser)
                .build();

        sendNotificationAsync(notification);
    }


    public void sendNotification(String message, String sender, String type,
                                 String link, String category, String kind, String subject) {

        User currentUser = getCurrentAuthenticatedUser();
        String receiverEmail = currentUser.getEmail();

        log.info("Sending notification to authenticated user: {}, type: {}, category: {}",
                receiverEmail, type, category);

        // Use IST timezone
        ZonedDateTime istNow = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));

        Notification notification = Notification.builder()
                .receiver(receiverEmail)
                .message(message)
                .sender(sender)
                .type(type)
                .link(link)
                .read(false)
                .createdAt(istNow.toLocalDateTime()) // Convert to LocalDateTime
                .category(category)
                .kind(kind)
                .subject(subject)
                .stared(false)
                .deleted(false)
                .user(currentUser)
                .build();

        sendNotificationAsync(notification);
    }
    @Async("notificationExecutor")
    @Transactional
    public void sendNotificationAsync(Notification notification) {
        String methodName = "sendNotificationAsync";
        logEntry(methodName, notification.getReceiver());

        try {
            // Make sure user is set
            if (notification.getUser() == null && notification.getReceiver() != null) {
                User receiverUser = userRepository.findByEmail(notification.getReceiver())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "User not found with email: " + notification.getReceiver()));
                notification.setUser(receiverUser);
            }

            // Save to database
            Notification savedNotification = notificationRepository.save(notification);
            logSuccess(methodName, "Notification saved to DB: " + savedNotification.getId());

            // Send real-time notification if user is online
            pushService.sendNotificationToUser(notification.getReceiver(), savedNotification);

        } catch (ResourceNotFoundException e) {
            log.error("User not found for notification: {}", notification.getReceiver());
            // If user not found, create notification without user (if allowed)
            try {
                notification.setUser(null); // Only if your entity allows null user
                Notification savedNotification = notificationRepository.save(notification);
                log.info("Notification saved without user: {}", savedNotification.getId());
            } catch (Exception ex) {
                log.error("Failed to save notification without user: {}", ex.getMessage());
                throw new RuntimeException("Failed to save notification: " + ex.getMessage());
            }
        } catch (DataAccessException e) {
            String errorMsg = "Failed to send notification due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Error sending notification asynchronously";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }



    @Transactional()
    public List<NotificationDTO> getUnreadNotifications(Integer page, Integer size) {
        String methodName = "getUnreadNotifications";
        logEntry(methodName, "page=" + page + ", size=" + size);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by("createdAt").descending() // Ensure descending order
            );

            Page<Notification> notifications = notificationRepository
                    .findByReceiverAndReadFalse(currentUser.getEmail(), pageable);

            logSuccess(methodName, "Retrieved " + notifications.getContent().size() +
                    " unread notifications for user: " + currentUser.getEmail());

            return notifications.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get unread notifications due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get unread notifications due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    private NotificationDTO convertToDTO(Notification notification) {
        try {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(notification.getId());
            dto.setReceiver(notification.getReceiver());
            dto.setMessage(notification.getMessage());
            dto.setSender(notification.getSender());
            dto.setType(notification.getType());
            dto.setLink(notification.getLink());
            dto.setRead(notification.isRead());
            dto.setCreatedAt(notification.getCreatedAt());
            dto.setCategory(notification.getCategory());
            dto.setKind(notification.getKind());
            dto.setSubject(notification.getSubject());
            dto.setStared(notification.isStared());
            dto.setDeleted(notification.isDeleted());

            // Calculate IST formatted time and time ago
            dto.calculateTimeAgo();

            return dto;
        } catch (Exception e) {
            log.error("Error converting notification to DTO: {}", notification.getId(), e);
            throw new RuntimeException("Failed to convert notification to DTO");
        }
    }

    /**
     * Star notification (must belong to current user)
     */
    public void stardMessage(Long id) {
        String methodName = "stardMessage";
        logEntry(methodName, "id=" + id);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

            // Verify notification belongs to current user
            if (!notification.getReceiver().equals(currentUser.getEmail())) {
                throw new ValidationException("Unauthorized access to notification");
            }

            notification.setStared(true);
            notificationRepository.save(notification);
            logSuccess(methodName, "Notification starred: " + id);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to star notification due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to star notification due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Unstar notification (must belong to current user)
     */
    public void unstardMessage(Long id) {
        String methodName = "unstardMessage";
        logEntry(methodName, "id=" + id);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

            // Verify notification belongs to current user
            if (!notification.getReceiver().equals(currentUser.getEmail())) {
                throw new ValidationException("Unauthorized access to notification");
            }

            notification.setStared(false);
            notificationRepository.save(notification);
            logSuccess(methodName, "Notification unstarred: " + id);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to unstar notification due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to unstar notification due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    @Transactional
    public boolean deleteMessage(Long id) {
        String methodName = "deleteMessage";
        logEntry(methodName, "id=" + id);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

            // Verify notification belongs to current user
            if (!notification.getReceiver().equals(currentUser.getEmail())) {
                throw new ValidationException("Unauthorized access to notification");
            }

            notification.setDeleted(true);
            notificationRepository.save(notification);
            logSuccess(methodName, "Notification deleted (soft delete): " + id);

            return true;

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to delete notification due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to delete notification due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    @Cacheable(value = "getAllNotifications", key = "#page + '_' + #size")
    @Transactional()
    public List<NotificationDTO> getAllNotifications(Integer page, Integer size) {
        String methodName = "getAllNotifications";
        logEntry(methodName, "page=" + page + ", size=" + size);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by("createdAt").descending() // This should put recent first
            );

            Page<Notification> notifications = notificationRepository
                    .findByReceiverAndDeletedFalse(currentUser.getEmail(), pageable);

            logSuccess(methodName, "Retrieved " + notifications.getContent().size() +
                    " notifications for user: " + currentUser.getEmail());

            // Convert to DTOs before returning
            return notifications.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get all notifications due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get all notifications due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }
    /**
     * Mark notification as read (must belong to current user)
     */
    public void markAsRead(Long id) {
        String methodName = "markAsRead";
        logEntry(methodName, "id=" + id);

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

            // Verify notification belongs to current user
            if (!notification.getReceiver().equals(currentUser.getEmail())) {
                throw new ValidationException("Unauthorized access to notification");
            }

            notification.setRead(true);
            notificationRepository.save(notification);
            logSuccess(methodName, "Notification marked as read: " + id);

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to mark notification as read due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to mark notification as read due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Get unread count for current authenticated user
     */
    @CachePut(value = "unreadCount")
    @Transactional()
    public Long getUnreadCount() {
        String methodName = "getUnreadCount";
        logEntry(methodName, "");

        try {
            User currentUser = getCurrentAuthenticatedUser();
            Long count = notificationRepository
                    .countByReceiverAndReadFalseAndDeletedFalse(currentUser.getEmail());

            logSuccess(methodName, "Unread count for user " + currentUser.getEmail() + ": " + count);
            return count;

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get unread count due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get unread count due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Get deleted messages for current authenticated user
     */
    @Transactional()
    public List<Notification> getDeletedMessages() {
        String methodName = "getDeletedMessages";
        logEntry(methodName, "");

        try {
            User currentUser = getCurrentAuthenticatedUser();
            List<Notification> notifications = notificationRepository
                    .findByReceiverAndDeletedTrue(currentUser.getEmail());

            logSuccess(methodName, "Retrieved " + notifications.size() +
                    " deleted notifications for user: " + currentUser.getEmail());
            return notifications;

        } catch (ResourceNotFoundException | ValidationException e) {
            logValidationError(methodName, e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            String errorMsg = "Failed to get deleted messages due to database error";
            logDatabaseError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        } catch (Exception e) {
            String errorMsg = "Failed to get deleted messages due to unexpected error";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }

    /**
     * Admin method: Send batch notifications to multiple users
     */
    @Async("notificationExecutor")
    @Transactional
    public void sendBatchNotifications(List<String> receivers, String message, String sender,
                                       String type, String link, String category,
                                       String kind, String subject) {
        String methodName = "sendBatchNotifications";
        logEntry(methodName, "receivers=" + receivers.size());

        try {
            for (String receiver : receivers) {
                Notification notification = Notification.builder()
                        .receiver(receiver)
                        .message(message)
                        .sender(sender)
                        .type(type)
                        .link(link)
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .category(category)
                        .kind(kind)
                        .subject(subject)
                        .stared(false)
                        .deleted(false)
                        .build();

                sendNotificationAsync(notification);
            }

            logSuccess(methodName, "Batch notifications sent to " + receivers.size() + " users");

        } catch (Exception e) {
            String errorMsg = "Failed to send batch notifications";
            logUnexpectedError(methodName, errorMsg, e);
            throw new RuntimeException(errorMsg);
        }
    }



    // Enhanced logging methods (matching CartService pattern)
    private void logEntry(String methodName, Object params) {
        log.debug("Entering {} with params: {}", methodName, params);
    }

    private void logSuccess(String methodName, String message) {
        log.info("{} - Success: {}", methodName, message);
    }

    private void logValidationError(String methodName, String error) {
        log.warn("{} - Validation Error: {}", methodName, error);
    }

    private void logDatabaseError(String methodName, String error, Exception e) {
        log.error("{} - Database Error: {}", methodName, error, e);
    }

    private void logUnexpectedError(String methodName, String error, Exception e) {
        log.error("{} - Unexpected Error: {}", methodName, error, e);
    }
}