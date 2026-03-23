package com.attendance.backend.adminsecurity.repository;

import com.attendance.backend.adminsecurity.api.dto.SecurityAbuseItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityDeadOutboxItemResponse;
import com.attendance.backend.adminsecurity.api.dto.SecurityOverviewResponse;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class AdminSecurityQueryRepository {

    private static final String UNKNOWN_SOURCE = "UNKNOWN";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AdminSecurityQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isActiveAdmin(UUID userId) {
        String sql = """
            select count(*)
            from users
            where id = UUID_TO_BIN(:userId, 1)
              and platform_role = 'ADMIN'
              and status = 'ACTIVE'
              and deleted_at is null
            """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId.toString()),
                Integer.class
        );
        return count != null && count > 0;
    }

    public SecurityOverviewResponse fetchOverview(int hours, Instant since) {
        String sql = """
            select
                coalesce((
                    select count(*)
                    from password_reset_attempts pra
                    where pra.created_at >= :since
                      and upper(pra.outcome) in ('ISSUED', 'TOKEN_ISSUED', 'MAIL_ENQUEUED', 'SUCCESS')
                ), 0) as password_reset_issued,

                coalesce((
                    select count(*)
                    from password_reset_attempts pra
                    where pra.created_at >= :since
                      and upper(pra.outcome) in ('THROTTLED', 'RATE_LIMITED')
                ), 0) as password_reset_throttled,

                coalesce((
                    select count(*)
                    from password_reset_attempts pra
                    where pra.created_at >= :since
                      and upper(pra.outcome) in ('MAIL_FAILED', 'OUTBOX_FAILED', 'EMAIL_OUTBOX_FAILED')
                ), 0) as password_reset_mail_failed,

                coalesce((
                    select count(*)
                    from login_attempts la
                    where la.created_at >= :since
                      and upper(la.outcome) in ('SUCCESS', 'LOGIN_SUCCESS')
                ), 0) as login_success,

                coalesce((
                    select count(*)
                    from login_attempts la
                    where la.created_at >= :since
                      and upper(la.outcome) in ('INVALID_CREDENTIALS', 'BAD_CREDENTIALS', 'WRONG_PASSWORD')
                ), 0) as login_invalid_credentials,

                coalesce((
                    select count(*)
                    from login_attempts la
                    where la.created_at >= :since
                      and upper(la.outcome) in ('THROTTLED', 'RATE_LIMITED')
                ), 0) as login_throttled,

                coalesce((
                    select count(*)
                    from email_outbox eo
                    where upper(eo.status) = 'PENDING'
                ), 0) as email_outbox_pending,

                coalesce((
                    select count(*)
                    from email_outbox eo
                    where upper(eo.status) = 'RETRY'
                ), 0) as email_outbox_retry,

                coalesce((
                    select count(*)
                    from email_outbox eo
                    where upper(eo.status) = 'DEAD'
                ), 0) as email_outbox_dead,

                coalesce((
                    select count(*)
                    from user_sessions us
                    where us.revoked_at >= :since
                      and upper(us.revoked_reason) in ('PASSWORD_RESET', 'RESET_PASSWORD', 'PASSWORD_RESET_ALL')
                ), 0) as sessions_revoked_by_password_reset
            from dual
            """;

        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("since", Timestamp.from(since)),
                (rs, rowNum) -> new SecurityOverviewResponse(
                        hours,
                        rs.getLong("password_reset_issued"),
                        rs.getLong("password_reset_throttled"),
                        rs.getLong("password_reset_mail_failed"),
                        rs.getLong("login_success"),
                        rs.getLong("login_invalid_credentials"),
                        rs.getLong("login_throttled"),
                        rs.getLong("email_outbox_pending"),
                        rs.getLong("email_outbox_retry"),
                        rs.getLong("email_outbox_dead"),
                        rs.getLong("sessions_revoked_by_password_reset")
                )
        );
    }

    public List<SecurityAbuseItemResponse> findPasswordResetAbuse(Instant since, int limit) {
        String sql = """
            select
                coalesce(nullif(trim(requested_ip), ''), :unknownSource) as source,
                count(*) as total_count,
                sum(case when upper(outcome) in ('THROTTLED', 'RATE_LIMITED') then 1 else 0 end) as throttled_count
            from password_reset_attempts
            where created_at >= :since
            group by coalesce(nullif(trim(requested_ip), ''), :unknownSource)
            order by total_count desc, throttled_count desc, source asc
            limit :limit
            """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("since", Timestamp.from(since))
                        .addValue("limit", limit)
                        .addValue("unknownSource", UNKNOWN_SOURCE),
                (rs, rowNum) -> new SecurityAbuseItemResponse(
                        rs.getString("source"),
                        rs.getLong("total_count"),
                        rs.getLong("throttled_count")
                )
        );
    }

    public List<SecurityAbuseItemResponse> findLoginAbuse(Instant since, int limit) {
        String sql = """
            select
                coalesce(nullif(trim(requested_ip), ''), :unknownSource) as source,
                count(*) as total_count,
                sum(case when upper(outcome) in ('THROTTLED', 'RATE_LIMITED') then 1 else 0 end) as throttled_count
            from login_attempts
            where created_at >= :since
            group by coalesce(nullif(trim(requested_ip), ''), :unknownSource)
            order by total_count desc, throttled_count desc, source asc
            limit :limit
            """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("since", Timestamp.from(since))
                        .addValue("limit", limit)
                        .addValue("unknownSource", UNKNOWN_SOURCE),
                (rs, rowNum) -> new SecurityAbuseItemResponse(
                        rs.getString("source"),
                        rs.getLong("total_count"),
                        rs.getLong("throttled_count")
                )
        );
    }

    public List<SecurityDeadOutboxItemResponse> findRetryAndDeadEmailOutbox(int limit) {
        String sql = """
            select
                BIN_TO_UUID(id, 1) as id,
                to_email,
                status,
                retry_count,
                last_error_code,
                last_error_message,
                next_attempt_at,
                created_at
            from email_outbox
            where upper(status) in ('RETRY', 'DEAD')
            order by
                case when upper(status) = 'DEAD' then 0 else 1 end,
                created_at desc
            limit :limit
            """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("limit", limit),
                (rs, rowNum) -> new SecurityDeadOutboxItemResponse(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("to_email"),
                        rs.getString("status"),
                        rs.getInt("retry_count"),
                        rs.getString("last_error_code"),
                        rs.getString("last_error_message"),
                        toInstant(rs.getTimestamp("next_attempt_at")),
                        toInstant(rs.getTimestamp("created_at"))
                )
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}