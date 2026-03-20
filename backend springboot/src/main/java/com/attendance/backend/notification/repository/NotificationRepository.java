package com.attendance.backend.notification.repository;

import com.attendance.backend.domain.entity.Notification;
import com.attendance.backend.domain.enums.NotificationSeverity;
import com.attendance.backend.domain.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("""
        select n
        from Notification n
        where n.recipientUserId = :recipientUserId
          and (:unreadOnly = false or n.isRead = false)
          and (:type is null or n.type = :type)
          and (:severity is null or n.severity = :severity)
        order by n.createdAt desc
    """)
    Page<Notification> findMyNotifications(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("type") NotificationType type,
            @Param("severity") NotificationSeverity severity,
            Pageable pageable
    );

    @Query("""
        select count(n)
        from Notification n
        where n.recipientUserId = :recipientUserId
          and n.isRead = false
    """)
    long countUnread(@Param("recipientUserId") UUID recipientUserId);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    Optional<Notification> findByDedupKey(String dedupKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.isRead = true,
            n.readAt = :readAt
        where n.id = :id
          and n.recipientUserId = :recipientUserId
          and n.isRead = false
    """)
    int markRead(
            @Param("id") UUID id,
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Notification n
        set n.isRead = true,
            n.readAt = :readAt
        where n.recipientUserId = :recipientUserId
          and n.isRead = false
    """)
    int markReadAll(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("readAt") Instant readAt
    );

    @Query("""
        select n
        from Notification n
        where n.recipientUserId = :recipientUserId
          and n.groupId = :groupId
        order by n.createdAt desc
    """)
    Page<Notification> findGroupNotifications(
            @Param("recipientUserId") UUID recipientUserId,
            @Param("groupId") UUID groupId,
            Pageable pageable
    );
}