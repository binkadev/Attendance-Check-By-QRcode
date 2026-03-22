package com.attendance.backend.fraud.repository;

import com.attendance.backend.common.db.MysqlUuidSwap;
import com.attendance.backend.fraud.domain.CheckinFailureCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class CheckinAttemptLogRepositoryImpl implements CheckinAttemptLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public long countBySessionUserTokenHashFailureCodesWithinWindow(
            UUID sessionId,
            UUID userId,
            byte[] tokenHash,
            Collection<CheckinFailureCode> failureCodes,
            Instant from
    ) {
        String sql = """
            select count(*)
            from checkin_attempt_logs cal
            where cal.session_id = UUID_TO_BIN(:sessionId, 1)
              and (
                    (:userId is null and cal.user_id is null)
                    or cal.user_id = UUID_TO_BIN(:userId, 1)
                  )
              and cal.token_hash = :tokenHash
              and cal.outcome = 'FAIL'
              and cal.failure_code in (:failureCodes)
              and cal.created_at >= :fromTs
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("sessionId", uuidParam(sessionId));
        query.setParameter("userId", uuidParam(userId));
        query.setParameter("tokenHash", tokenHash);
        query.setParameter("failureCodes", failureCodes.stream().map(Enum::name).toList());
        query.setParameter("fromTs", Timestamp.from(from));

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public long countBySessionUserFailureCodeWithinWindow(
            UUID sessionId,
            UUID userId,
            CheckinFailureCode failureCode,
            Instant from
    ) {
        String sql = """
            select count(*)
            from checkin_attempt_logs cal
            where cal.session_id = UUID_TO_BIN(:sessionId, 1)
              and cal.user_id = UUID_TO_BIN(:userId, 1)
              and cal.outcome = 'FAIL'
              and cal.failure_code = :failureCode
              and cal.created_at >= :fromTs
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("sessionId", uuidParam(sessionId));
        query.setParameter("userId", uuidParam(userId));
        query.setParameter("failureCode", failureCode.name());
        query.setParameter("fromTs", Timestamp.from(from));

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public IpBurstAggregate aggregateIpBurst(UUID groupId, String ipAddress, Instant from) {
        String sql = """
            select count(*) as total_attempts,
                   sum(case when cal.outcome = 'FAIL' then 1 else 0 end) as fail_attempts
            from checkin_attempt_logs cal
            where cal.group_id = UUID_TO_BIN(:groupId, 1)
              and cal.ip_address = :ipAddress
              and cal.created_at >= :fromTs
            """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("groupId", uuidParam(groupId))
                .setParameter("ipAddress", ipAddress)
                .setParameter("fromTs", Timestamp.from(from))
                .getSingleResult();

        long totalAttempts = row[0] == null ? 0L : ((Number) row[0]).longValue();
        long failAttempts = row[1] == null ? 0L : ((Number) row[1]).longValue();

        return new IpBurstAggregate(totalAttempts, failAttempts);
    }

    @Override
    public DeviceMultiAccountAggregate aggregateSharedDevice(UUID groupId, String deviceId, Instant from) {
        String sql = """
            select count(distinct cal.user_id)
            from checkin_attempt_logs cal
            where cal.group_id = UUID_TO_BIN(:groupId, 1)
              and cal.device_id = :deviceId
              and cal.outcome = 'SUCCESS'
              and cal.user_id is not null
              and cal.created_at >= :fromTs
            """;

        Number result = (Number) entityManager.createNativeQuery(sql)
                .setParameter("groupId", uuidParam(groupId))
                .setParameter("deviceId", deviceId)
                .setParameter("fromTs", Timestamp.from(from))
                .getSingleResult();

        return new DeviceMultiAccountAggregate(result == null ? 0L : result.longValue());
    }

    @Override
    public List<UUID> findRecentAttemptIdsForEvidence(UUID sessionId, UUID userId, Instant from, int limit) {
        String sql = """
            select cal.id
            from checkin_attempt_logs cal
            where cal.session_id = UUID_TO_BIN(:sessionId, 1)
              and (
                    (:userId is null and cal.user_id is null)
                    or cal.user_id = UUID_TO_BIN(:userId, 1)
                  )
              and cal.created_at >= :fromTs
            order by cal.created_at desc
            """;

        @SuppressWarnings("unchecked")
        List<byte[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("sessionId", uuidParam(sessionId))
                .setParameter("userId", uuidParam(userId))
                .setParameter("fromTs", Timestamp.from(from))
                .setMaxResults(limit)
                .getResultList();

        return rows.stream()
                .map(MysqlUuidSwap::toUuid)
                .toList();
    }

    private String uuidParam(UUID value) {
        return value == null ? null : value.toString();
    }
}