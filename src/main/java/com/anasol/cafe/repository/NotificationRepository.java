package com.anasol.cafe.repository;

import com.anasol.cafe.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Existing methods
    Page<Notification> findByReceiverAndReadFalse(String receiver, Pageable pageable);
    Page<Notification> findByReceiverAndDeletedFalse(String receiver, Pageable pageable);
    Long countByReceiverAndReadFalseAndDeletedFalse(String receiver);

    // Add these missing methods

    // 1. Find by receiver and deleted = true
    List<Notification> findByReceiverAndDeletedTrue(String receiver);

    // 2. Find by receiver and deleted = true with pagination
    Page<Notification> findByReceiverAndDeletedTrue(String receiver, Pageable pageable);

    // 3. Find all notifications for a receiver (regardless of deleted status)
    List<Notification> findByReceiver(String receiver);

    // 4. Find all notifications for a receiver with pagination
    Page<Notification> findByReceiver(String receiver, Pageable pageable);

    // 5. Find unread notifications count for a receiver
    Long countByReceiverAndReadFalse(String receiver);

    // 6. Find starred notifications for a receiver
    List<Notification> findByReceiverAndStaredTrueAndDeletedFalse(String receiver);

    // 7. Find starred notifications for a receiver with pagination
    Page<Notification> findByReceiverAndStaredTrueAndDeletedFalse(String receiver, Pageable pageable);

    // 8. Custom query to find notifications by multiple receivers
    @Query("SELECT n FROM Notification n WHERE n.receiver IN :receivers AND n.deleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByReceivers(@Param("receivers") List<String> receivers);

    // 9. Find notifications by category
    List<Notification> findByReceiverAndCategoryAndDeletedFalse(String receiver, String category);

    // 10. Find notifications by type
    List<Notification> findByReceiverAndTypeAndDeletedFalse(String receiver, String type);

    // 11. Find notifications created after a specific date
    @Query("SELECT n FROM Notification n WHERE n.receiver = :receiver AND n.createdAt >= :since AND n.deleted = false")
    List<Notification> findByReceiverAndCreatedAfter(@Param("receiver") String receiver,
                                                     @Param("since") java.time.LocalDateTime since);

    // 12. Mark multiple notifications as read
    @Query("UPDATE Notification n SET n.read = true WHERE n.id IN :ids AND n.receiver = :receiver")
    int markAsRead(@Param("ids") List<Long> ids, @Param("receiver") String receiver);

    // 13. Delete multiple notifications (soft delete)
    @Query("UPDATE Notification n SET n.deleted = true WHERE n.id IN :ids AND n.receiver = :receiver")
    int deleteMultiple(@Param("ids") List<Long> ids, @Param("receiver") String receiver);

    // 14. Find notifications by sender
    List<Notification> findBySenderAndDeletedFalse(String sender);

    // 15. Find notifications where message contains text
    @Query("SELECT n FROM Notification n WHERE n.receiver = :receiver AND n.message LIKE %:text% AND n.deleted = false")
    List<Notification> searchByMessage(@Param("receiver") String receiver, @Param("text") String text);
}