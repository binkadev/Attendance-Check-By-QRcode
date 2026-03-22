package com.attendance.backend.fraud.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FraudSessionLookupService {

    private final JdbcTemplate jdbcTemplate;

    public FraudSessionLookupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID requireGroupIdBySessionId(UUID sessionId) {
        String value = jdbcTemplate.query(
                """
                select BIN_TO_UUID(s.group_id, 1)
                from attendance_sessions s
                where s.id = UUID_TO_BIN(?, 1)
                  and s.deleted_at is null
                limit 1
                """,
                ps -> ps.setString(1, sessionId.toString()),
                rs -> rs.next() ? rs.getString(1) : null
        );

        if (value == null) {
            throw new IllegalArgumentException("Session not found for fraud session lookup: " + sessionId);
        }

        return UUID.fromString(value);
    }
}