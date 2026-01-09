package com.anasol.cafe.repository;



import com.anasol.cafe.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {


    Page<Notification> findByReceiverAndReadFalse(String receiver, Pageable pageable);
    Page<Notification> findByReceiverAndDeletedFalse(String receiver, Pageable pageable);

    // List<Notification> findByReceiverAndReadFalseOrderByCreatedAtDesc(String receiver);
    Page<Notification> findByReceiver(String receiver, Pageable pageable);
    long countByReceiverAndReadFalseAndDeletedFalse(String receiver);
    @Query(
            value = "SELECT COUNT(*) FROM notification.notification_table " +
                    "WHERE receiver = :receiver " +
                    "AND read = false " +
                    "AND deleted = false " +
                    "AND LOWER(kind) <> 'chat'",
            nativeQuery = true)
    long countNonChatUnreadByReceiver(@Param("receiver") String receiver);

    List<Notification> findByDeletedTrue();
    @Query(
            value = "SELECT * FROM notification.notification_table nt " +
                    "WHERE nt.receiver = :receiver " +
                    "AND nt.deleted = false " +
                    "AND LOWER(nt.kind) <> 'chat'",
            countQuery = "SELECT COUNT(*) FROM notification.notification_table nt " +
                    "WHERE nt.receiver = :receiver " +
                    "AND nt.deleted = false " +
                    "AND LOWER(nt.kind) <> 'chat'",
            nativeQuery = true
    )
    Page<Notification> findNonChatNotificationsByReceiver(
            @Param("receiver") String receiver,
            Pageable pageable
    );


}
