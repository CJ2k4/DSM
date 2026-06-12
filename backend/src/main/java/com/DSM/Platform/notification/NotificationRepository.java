package com.DSM.Platform.notification;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // join fetch the actor: the response DTO embeds it and open-in-view is off.
    @Query(
            value = """
                    select notification from Notification notification
                    join fetch notification.actor
                    where notification.recipient.id = :recipientId
                    order by notification.createdAt desc
                    """,
            countQuery = """
                    select count(notification) from Notification notification
                    where notification.recipient.id = :recipientId
                    """
    )
    Page<Notification> findForRecipient(@Param("recipientId") UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadFalse(UUID recipientId);

    @Modifying
    @Query("""
            update Notification notification
            set notification.read = true
            where notification.recipient.id = :recipientId and notification.read = false
            """)
    int markAllRead(@Param("recipientId") UUID recipientId);
}
