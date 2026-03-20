package com.attendance.backend.notification.repository;

import com.attendance.backend.domain.entity.NotificationDelivery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class NotificationDeliveryClaimRepositoryImpl implements NotificationDeliveryClaimRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public List<NotificationDelivery> claimDueEmailDeliveries(Instant now, Instant staleBefore, int limit) {
        return em.createNativeQuery("""
            select *
            from notification_deliveries nd
            where nd.channel = 'EMAIL'
              and nd.status in ('PENDING', 'RETRY')
              and nd.email_outbox_id is null
              and nd.next_attempt_at <= :now
              and (nd.locked_at is null or nd.locked_at < :staleBefore)
            order by nd.next_attempt_at asc, nd.created_at asc
            limit :limit
            for update skip locked
        """, NotificationDelivery.class)
                .setParameter("now", now)
                .setParameter("staleBefore", staleBefore)
                .setParameter("limit", limit)
                .getResultList();
    }
}