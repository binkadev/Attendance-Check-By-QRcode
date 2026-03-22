package com.attendance.backend.fraud.service;

import com.attendance.backend.common.exception.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FraudGroupAccessServiceImpl implements FraudGroupAccessService {

    private final JdbcTemplate jdbcTemplate;

    public FraudGroupAccessServiceImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void requireIncidentReadAccess(UUID actorUserId, UUID groupId) {
        if (!hasIncidentAccess(actorUserId, groupId)) {
            throw ApiException.forbidden(
                    "FRAUD_ACCESS_DENIED",
                    "You do not have permission to view fraud incidents for this group"
            );
        }
    }

    @Override
    public void requireIncidentWriteAccess(UUID actorUserId, UUID groupId) {
        if (!hasIncidentAccess(actorUserId, groupId)) {
            throw ApiException.forbidden(
                    "FRAUD_ACCESS_DENIED",
                    "You do not have permission to manage fraud incidents for this group"
            );
        }
    }

    private boolean hasIncidentAccess(UUID actorUserId, UUID groupId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                select count(*)
                from users u
                where u.id = UUID_TO_BIN(?, 1)
                  and u.deleted_at is null
                  and (
                        u.platform_role = 'ADMIN'
                        or exists (
                            select 1
                            from group_members gm
                            where gm.group_id = UUID_TO_BIN(?, 1)
                              and gm.user_id = u.id
                              and gm.member_status = 'APPROVED'
                              and gm.role in ('OWNER', 'CO_HOST')
                              and gm.removed_at is null
                        )
                      )
                """,
                Integer.class,
                actorUserId.toString(),
                groupId.toString()
        );

        return count != null && count > 0;
    }
}