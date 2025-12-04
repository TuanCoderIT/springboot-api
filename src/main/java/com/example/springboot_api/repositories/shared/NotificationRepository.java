package com.example.springboot_api.repositories.shared;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.springboot_api.models.Notification;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

        Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

        List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);

        long countByUserIdAndIsReadFalse(UUID userId);

        @Query("""
                        SELECT n FROM Notification n
                        WHERE n.user.id = :userId
                        AND (:isRead IS NULL OR n.isRead = :isRead)
                        AND (:type IS NULL OR n.type = :type)
                        ORDER BY n.createdAt DESC
                        """)
        Page<Notification> findByUserIdWithFilters(
                        @Param("userId") UUID userId,
                        @Param("isRead") Boolean isRead,
                        @Param("type") String type,
                        Pageable pageable);

        @Query("""
                        SELECT n FROM Notification n
                        WHERE n.user.role = 'ADMIN'
                        AND (:isRead IS NULL OR n.isRead = :isRead)
                        AND (:type IS NULL OR n.type = :type)
                        ORDER BY n.createdAt DESC
                        """)
        Page<Notification> findAdminNotifications(
                        @Param("isRead") Boolean isRead,
                        @Param("type") String type,
                        Pageable pageable);

        /**
         * Đánh dấu tất cả notifications chưa đọc của user là đã đọc
         */
        @Modifying
        @Query("""
                        UPDATE Notification n
                        SET n.isRead = true,
                            n.readAt = :readAt,
                            n.updatedAt = :updatedAt
                        WHERE n.user.id = :userId
                        AND n.isRead = false
                        """)
        int markAllAsReadByUserId(
                        @Param("userId") UUID userId,
                        @Param("readAt") java.time.OffsetDateTime readAt,
                        @Param("updatedAt") java.time.OffsetDateTime updatedAt);
}
