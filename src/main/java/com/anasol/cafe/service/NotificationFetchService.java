//package com.anasol.cafe.service;
//
//import com.anasol.cafe.dto.NotificationResponseDTO;
//import com.anasol.cafe.entity.Notification;
//import com.anasol.cafe.entity.User;
//import com.anasol.cafe.exceptions.ResourceNotFoundException;
//import com.anasol.cafe.exceptions.ValidationException;
//import com.anasol.cafe.repository.NotificationRepository;
//import com.anasol.cafe.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationFetchService {
//
//    private final NotificationRepository notificationRepository;
//    private final UserRepository userRepository;
//
//    /**
//     * Get current authenticated user from token (SAME AS ORDER SERVICE)
//     */
//    private User getCurrentAuthenticatedUser() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            throw new ValidationException("User not authenticated");
//        }
//
//        String email = authentication.getName();
//        log.debug("Getting authenticated user with email: {}", email);
//
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
//    }
//
//    /**
//     * Get notifications for the currently authenticated user
//     */
//    @Transactional(readOnly = true)
//    public Page<NotificationResponseDTO> getMyNotifications(int page, int size,
//                                                           String sortBy, String direction,
//                                                           Boolean unreadOnly) {
//        User currentUser = getCurrentAuthenticatedUser();
//        log.info("Fetching notifications for user: {} (ID: {})",
//                currentUser.getEmail(), currentUser.getId());
//
//        // Create pageable with sorting
//        if (page < 0) page = 0;
//        if (size <= 0) size = 20;
//        if (sortBy == null || sortBy.isEmpty()) sortBy = "createdAt";
//        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ?
//                Sort.Direction.ASC : Sort.Direction.DESC;
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
//
//        Page<Notification> notificationPage;
//
//        if (unreadOnly != null && unreadOnly) {
//            // Get only unread notifications
//            notificationPage = notificationRepository.findByUserIdAndIsRead(
//                    currentUser.getId(), false, pageable);
//            log.info("Found {} unread notifications", notificationPage.getTotalElements());
//        } else {
//            // Get all notifications
//            notificationPage = notificationRepository.findByUserId(
//                    currentUser.getId(), pageable);
//            log.info("Found {} total notifications", notificationPage.getTotalElements());
//        }
//
//        // Convert to DTO
//        return notificationPage.map(this::convertToDTO);
//    }
//
//    /**
//     * Get count of unread notifications for current user
//     */
//    @Transactional(readOnly = true)
//    public Long getMyUnreadNotificationCount() {
//        User currentUser = getCurrentAuthenticatedUser();
//        return notificationRepository.countByUserIdAndIsRead(currentUser.getId(), false);
//    }
//
//    /**
//     * Mark a specific notification as read
//     */
//    @Transactional
//    public void markNotificationAsRead(Long notificationId) {
//        User currentUser = getCurrentAuthenticatedUser();
//
//        Notification notification = notificationRepository.findById(notificationId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Notification not found with id: " + notificationId));
//
//        // Verify the notification belongs to the current user
//        if (!notification.getUser().getId().equals(currentUser.getId())) {
//            throw new ValidationException("You can only mark your own notifications as read");
//        }
//
//        notification.setIsRead(true);
//        notificationRepository.save(notification);
//
//        log.info("Notification {} marked as read for user {}",
//                notificationId, currentUser.getEmail());
//    }
//
//    /**
//     * Mark all notifications as read for current user
//     */
//    @Transactional
//    public int markAllNotificationsAsRead() {
//        User currentUser = getCurrentAuthenticatedUser();
//
//        Pageable pageable = PageRequest.of(0, 100); // Process in batches
//        Page<Notification> unreadNotifications = notificationRepository
//                .findByUserIdAndIsRead(currentUser.getId(), false, pageable);
//
//        int markedCount = 0;
//
//        for (Notification notification : unreadNotifications) {
//            notification.setIsRead(true);
//            notificationRepository.save(notification);
//            markedCount++;
//        }
//
//        log.info("Marked {} notifications as read for user {}",
//                markedCount, currentUser.getEmail());
//
//        return markedCount;
//    }
//
//    /**
//     * Delete a notification (only if it belongs to current user)
//     */
//    @Transactional
//    public void deleteNotification(Long notificationId) {
//        User currentUser = getCurrentAuthenticatedUser();
//
//        Notification notification = notificationRepository.findById(notificationId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Notification not found with id: " + notificationId));
//
//        // Verify the notification belongs to the current user
//        if (!notification.getUser().getId().equals(currentUser.getId())) {
//            throw new ValidationException("You can only delete your own notifications");
//        }
//
//        notificationRepository.delete(notification);
//
//        log.info("Notification {} deleted for user {}",
//                notificationId, currentUser.getEmail());
//    }
//
//    /**
//     * Convert Notification entity to DTO
//     */
//    private NotificationResponseDTO convertToDTO(Notification notification) {
//        NotificationResponseDTO dto = new NotificationResponseDTO();
//        dto.setId(notification.getId());
//        dto.setType(notification.getType());
//        dto.setTitle(notification.getTitle());
//        dto.setMessage(notification.getMessage());
//        dto.setOrderId(notification.getOrderId());
//        dto.setReferenceId(notification.getReferenceId());
//        dto.setIsRead(notification.getIsRead());
//        dto.setCreatedAt(notification.getCreatedAt());
//
//        // Format the timestamp for display
//        if (notification.getCreatedAt() != null) {
//            dto.setFormattedTime(notification.getCreatedAt()
//                    .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm")));
//
//            // Calculate "time ago" string
//            dto.setTimeAgo(getTimeAgo(notification.getCreatedAt()));
//        }
//
//        return dto;
//    }
//
//    /**
//     * Helper method to calculate "time ago" string
//     */
//    private String getTimeAgo(java.time.LocalDateTime createdTime) {
//        java.time.Duration duration = java.time.Duration.between(
//                createdTime, java.time.LocalDateTime.now());
//
//        long minutes = duration.toMinutes();
//        long hours = duration.toHours();
//        long days = duration.toDays();
//
//        if (minutes < 1) {
//            return "Just now";
//        } else if (minutes < 60) {
//            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
//        } else if (hours < 24) {
//            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
//        } else if (days < 30) {
//            return days + " day" + (days > 1 ? "s" : "") + " ago";
//        } else {
//            return "Over a month ago";
//        }
//    }
//}