package com.attendance.backend.attendance.api;

import com.attendance.backend.attendance.repository.AttendanceEventRepository;
import com.attendance.backend.domain.entity.AttendanceEvent;
import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AttendanceAdminManualOverrideIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    AttendanceEventRepository attendanceEventRepository;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.update("delete from attendance_events");
        jdbcTemplate.update("delete from absence_requests");
        jdbcTemplate.update("delete from session_attendance");
        jdbcTemplate.update("delete from attendance_sessions");
        jdbcTemplate.update("delete from group_members");
        jdbcTemplate.update("delete from class_groups");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void owner_manual_mark_present_success() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "QR lỗi, giảng viên xác minh trực tiếp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(s.sessionId().toString()))
                .andExpect(jsonPath("$.userId").value(s.targetStudentId().toString()))
                .andExpect(jsonPath("$.attendanceStatus").value("PRESENT"))
                .andExpect(jsonPath("$.checkInMethod").value("MANUAL"));

        Map<String, Object> row = findAttendanceRow(s.sessionId(), s.targetStudentId());
        assertThat(row.get("attendance_status")).isEqualTo("PRESENT");
        assertThat(row.get("check_in_method")).isEqualTo("MANUAL");
        assertThat(row.get("check_in_at")).isNotNull();
        assertThat(row.get("qr_token_id")).isNull();
        assertThat(row.get("device_id")).isNull();
        assertThat(row.get("ip_address")).isNull();
        assertThat(row.get("user_agent")).isNull();
        assertThat(row.get("excused_by_request_id")).isNull();

        AttendanceEvent event = latestEventFor(s.sessionId(), s.targetStudentId());
        assertThat(event.eventType.name()).isEqualTo("MARK_MANUAL_PRESENT");
        assertThat(event.oldStatus.name()).isEqualTo("ABSENT");
        assertThat(event.newStatus.name()).isEqualTo("PRESENT");
        assertThat(event.actorUserId).isEqualTo(s.ownerId());
        assertThat(event.eventPayload.get("action").asText()).isEqualTo("MANUAL_OVERRIDE");
        assertThat(event.eventPayload.get("note").asText()).isEqualTo("QR lỗi, giảng viên xác minh trực tiếp");
    }

    @Test
    void owner_manual_mark_late_success() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "LATE",
                                  "note": "Đến muộn, giảng viên xác nhận"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("LATE"))
                .andExpect(jsonPath("$.checkInMethod").value("MANUAL"));

        AttendanceEvent event = latestEventFor(s.sessionId(), s.targetStudentId());
        assertThat(event.eventType.name()).isEqualTo("MARK_MANUAL_LATE");
        assertThat(event.newStatus.name()).isEqualTo("LATE");
    }

    @Test
    void owner_manual_mark_absent_success() throws Exception {
        Scenario s = seedScenario("CLOSED", true);
        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "ABSENT",
                                  "note": "Xác minh lại và chỉnh về vắng"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        Map<String, Object> row = findAttendanceRow(s.sessionId(), s.targetStudentId());
        assertThat(row.get("attendance_status")).isEqualTo("ABSENT");
        assertThat(row.get("check_in_at")).isNull();
        assertThat(row.get("qr_token_id")).isNull();
        assertThat(row.get("device_id")).isNull();
        assertThat(row.get("ip_address")).isNull();
        assertThat(row.get("user_agent")).isNull();

        AttendanceEvent event = latestEventFor(s.sessionId(), s.targetStudentId());
        assertThat(event.eventType.name()).isEqualTo("MARK_MANUAL_ABSENT");
        assertThat(event.newStatus.name()).isEqualTo("ABSENT");
    }

    @Test
    void cohost_manual_mark_present_success() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.coHostId(), s.coHostEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "CO_HOST xác minh"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("PRESENT"));

        AttendanceEvent event = latestEventFor(s.sessionId(), s.targetStudentId());
        assertThat(event.actorUserId).isEqualTo(s.coHostId());
        assertThat(event.eventType.name()).isEqualTo("MARK_MANUAL_PRESENT");
    }

    @Test
    void approved_member_forbidden() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.memberId(), s.memberEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Không đủ quyền"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void outsider_student_forbidden() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.outsiderId(), s.outsiderEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Không thuộc group"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void session_not_found_404() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", UUID.randomUUID(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Session không tồn tại"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void attendance_row_not_found_404() throws Exception {
        Scenario s = seedScenario("OPEN", true);
        UUID noAttendanceUserId = UUID.randomUUID();
        insertUser(noAttendanceUserId, "no-attendance@example.com", "No Attendance User");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), noAttendanceUserId)
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Attendance row không tồn tại"
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void target_status_excused_rejected() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "EXCUSED",
                                  "note": "Không hợp lệ"
                                }
                                """))
                .andExpect(status().is4xxClientError());

        assertThat(eventCountFor(s.sessionId(), s.targetStudentId())).isZero();
        assertThat(findAttendanceRow(s.sessionId(), s.targetStudentId()).get("attendance_status")).isEqualTo("ABSENT");
    }

    @Test
    void current_status_excused_conflict() throws Exception {
        Scenario s = seedScenario("OPEN", true);
        UUID absenceRequestId = insertApprovedAbsenceRequest(s.groupId(), s.targetStudentId(), s.sessionId(), s.ownerId());

        jdbcTemplate.update("""
                update session_attendance
                set attendance_status = 'EXCUSED',
                    check_in_at = null,
                    excused_by_request_id = UUID_TO_BIN(?, 1)
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                absenceRequestId.toString(),
                s.sessionId().toString(),
                s.targetStudentId().toString()
        );

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Muốn override row EXCUSED"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(eventCountFor(s.sessionId(), s.targetStudentId())).isZero();
    }

    @Test
    void same_status_noop_no_event_spam_and_updated_at_not_changed() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        Instant seededAt = Instant.now().minusSeconds(600);
        jdbcTemplate.update("""
                update session_attendance
                set attendance_status = 'PRESENT',
                    check_in_at = ?,
                    check_in_method = 'MANUAL',
                    qr_token_id = null,
                    device_id = null,
                    ip_address = null,
                    user_agent = null,
                    geo_lat = null,
                    geo_lng = null,
                    distance_meter = null,
                    suspicious_flag = 0,
                    suspicious_reason = null,
                    excused_by_request_id = null,
                    updated_at = ?
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                Timestamp.from(seededAt),
                Timestamp.from(seededAt),
                s.sessionId().toString(),
                s.targetStudentId().toString()
        );

        long beforeCount = eventCountFor(s.sessionId(), s.targetStudentId());
        Instant beforeUpdatedAt = attendanceUpdatedAt(s.sessionId(), s.targetStudentId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "No-op test"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("PRESENT"))
                .andExpect(jsonPath("$.checkInMethod").value("MANUAL"));

        long afterCount = eventCountFor(s.sessionId(), s.targetStudentId());
        Instant afterUpdatedAt = attendanceUpdatedAt(s.sessionId(), s.targetStudentId());

        assertThat(afterCount).isEqualTo(beforeCount);
        assertThat(afterUpdatedAt).isEqualTo(beforeUpdatedAt);
    }

    @Test
    void allow_manual_override_false_conflict() throws Exception {
        Scenario s = seedScenario("OPEN", false);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Manual override đang disable"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(eventCountFor(s.sessionId(), s.targetStudentId())).isZero();
    }

    @Test
    void cancelled_session_conflict() throws Exception {
        Scenario s = seedScenario("CANCELLED", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Session cancelled"
                                }
                                """))
                .andExpect(status().isConflict());

        assertThat(eventCountFor(s.sessionId(), s.targetStudentId())).isZero();
    }

    @Test
    void note_blank_validation_400() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": " "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void note_too_short_validation_400() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "ok"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reset_allow_manual_override_false_conflict() throws Exception {
        Scenario s = seedScenario("OPEN", false);
        makeAttendancePresentQrLike(s.sessionId(), s.targetStudentId(), s.ownerId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}/reset", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void reset_excused_conflict() throws Exception {
        Scenario s = seedScenario("OPEN", true);
        UUID absenceRequestId = insertApprovedAbsenceRequest(s.groupId(), s.targetStudentId(), s.sessionId(), s.ownerId());

        jdbcTemplate.update("""
                update session_attendance
                set attendance_status = 'EXCUSED',
                    check_in_at = null,
                    excused_by_request_id = UUID_TO_BIN(?, 1)
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                absenceRequestId.toString(),
                s.sessionId().toString(),
                s.targetStudentId().toString()
        );

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}/reset", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf()))
                .andExpect(status().isConflict());

        assertThat(eventCountFor(s.sessionId(), s.targetStudentId())).isZero();
    }

    @Test
    void reset_clean_row_noop_no_event_spam_and_updated_at_not_changed() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        long beforeCount = eventCountFor(s.sessionId(), s.targetStudentId());
        Instant beforeUpdatedAt = attendanceUpdatedAt(s.sessionId(), s.targetStudentId());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}/reset", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceStatus").value("ABSENT"));

        long afterCount = eventCountFor(s.sessionId(), s.targetStudentId());
        Instant afterUpdatedAt = attendanceUpdatedAt(s.sessionId(), s.targetStudentId());

        assertThat(afterCount).isEqualTo(beforeCount);
        assertThat(afterUpdatedAt).isEqualTo(beforeUpdatedAt);
    }

    @Test
    void list_attendance_events_contains_manual_event() throws Exception {
        Scenario s = seedScenario("OPEN", true);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/attendance/{userId}", s.sessionId(), s.targetStudentId())
                        .with(authAs(s.ownerId(), s.ownerEmail()))
                        .with(csrf())
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "PRESENT",
                                  "note": "Kiểm tra event list"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/attendance-events", s.sessionId())
                        .queryParam("userId", s.targetStudentId().toString())
                        .queryParam("limit", "20")
                        .with(authAs(s.ownerId(), s.ownerEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(s.sessionId().toString()))
                .andExpect(jsonPath("$.items[0].userId").value(s.targetStudentId().toString()))
                .andExpect(jsonPath("$.items[0].actorUserId").value(s.ownerId().toString()))
                .andExpect(jsonPath("$.items[0].eventType").value("MARK_MANUAL_PRESENT"))
                .andExpect(jsonPath("$.items[0].eventPayload.action").value("MANUAL_OVERRIDE"))
                .andExpect(jsonPath("$.items[0].eventPayload.note").value("Kiểm tra event list"));
    }

    // =========================
    // Seed helpers
    // =========================

    private void insertQrToken(UUID sessionId, String tokenId, UUID issuedByUserId) {
        Instant issuedAt = Instant.now().minusSeconds(120);
        Instant expiresAt = issuedAt.plusSeconds(300);

        jdbcTemplate.update("""
            insert into qr_tokens (
                token_id,
                session_id,
                token_hash,
                issued_at,
                expires_at,
                revoked_at,
                revoked_reason,
                rotated_from_token_id,
                issued_by_user_id,
                note
            )
            values (
                ?,
                UUID_TO_BIN(?, 1),
                UNHEX(SHA2(?, 256)),
                ?,
                ?,
                null,
                null,
                null,
                UUID_TO_BIN(?, 1),
                ?
            )
            """,
                tokenId,
                sessionId.toString(),
                tokenId,
                Timestamp.from(issuedAt),
                Timestamp.from(expiresAt),
                issuedByUserId.toString(),
                "seed qr token for integration test"
        );
    }

    private Scenario seedScenario(String sessionStatus, boolean allowManualOverride) {
        UUID ownerId = UUID.randomUUID();
        UUID coHostId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID targetStudentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        String ownerEmail = "owner+" + ownerId + "@example.com";
        String coHostEmail = "cohost+" + coHostId + "@example.com";
        String memberEmail = "member+" + memberId + "@example.com";
        String outsiderEmail = "outsider+" + outsiderId + "@example.com";

        insertUser(ownerId, ownerEmail, "Owner User");
        insertUser(coHostId, coHostEmail, "CoHost User");
        insertUser(memberId, memberEmail, "Member User");
        insertUser(outsiderId, outsiderEmail, "Outsider User");
        insertUser(targetStudentId, "student+" + targetStudentId + "@example.com", "Target Student");

        insertGroup(groupId, ownerId);
        insertGroupMember(groupId, ownerId, "OWNER", "APPROVED");
        insertGroupMember(groupId, coHostId, "CO_HOST", "APPROVED");
        insertGroupMember(groupId, memberId, "MEMBER", "APPROVED");
        insertGroupMember(groupId, targetStudentId, "MEMBER", "APPROVED");

        insertSession(sessionId, groupId, ownerId, sessionStatus, allowManualOverride);
        insertAttendance(sessionId, targetStudentId, "ABSENT", "QR", null, null, null, null, null);

        return new Scenario(
                ownerId,
                ownerEmail,
                coHostId,
                coHostEmail,
                memberId,
                memberEmail,
                outsiderId,
                outsiderEmail,
                targetStudentId,
                groupId,
                sessionId
        );
    }

    private void insertUser(UUID userId, String email, String fullName) {
        Instant now = Instant.now().minusSeconds(3600);

        jdbcTemplate.update("""
                insert into users (
                    id, platform_role, email, password_hash, full_name, status, created_at, updated_at, deleted_at
                )
                values (
                    UUID_TO_BIN(?, 1), 'USER', ?, ?, ?, 'ACTIVE', ?, ?, null
                )
                """,
                userId.toString(),
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                fullName,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertGroup(UUID groupId, UUID ownerUserId) {
        Instant now = Instant.now().minusSeconds(3600);

        jdbcTemplate.update("""
                insert into class_groups (
                    id, owner_user_id, name, code, join_code, description, semester, room,
                    approval_mode, allow_auto_join_on_checkin, status, created_at, updated_at, deleted_at
                )
                values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?, ?, ?, ?, ?, ?,
                    'AUTO', 0, 'ACTIVE', ?, ?, null
                )
                """,
                groupId.toString(),
                ownerUserId.toString(),
                "Test Group " + groupId.toString().substring(0, 8),
                "GRP" + groupId.toString().substring(0, 8),
                "JOIN" + groupId.toString().replace("-", "").substring(0, 8),
                "Integration test group",
                "2025-2026",
                "A101",
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertGroupMember(UUID groupId, UUID userId, String role, String memberStatus) {
        Instant now = Instant.now().minusSeconds(3500);

        jdbcTemplate.update("""
                insert into group_members (
                    group_id, user_id, role, member_status, joined_at, invited_by, created_at, updated_at, removed_at
                )
                values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?, ?, ?, null, ?, ?, null
                )
                """,
                groupId.toString(),
                userId.toString(),
                role,
                memberStatus,
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertSession(UUID sessionId, UUID groupId, UUID creatorUserId, String status, boolean allowManualOverride) {
        Instant now = Instant.now().minusSeconds(3000);
        Instant startAt = Instant.now().minusSeconds(1800);
        Instant checkinOpenAt = startAt.minusSeconds(300);
        Instant checkinCloseAt = startAt.plusSeconds(900);
        Instant endAt = startAt.plusSeconds(5400);

        jdbcTemplate.update("""
                insert into attendance_sessions (
                    id, group_id, created_by_user_id, title, session_date, status,
                    start_at, checkin_open_at, checkin_close_at, end_at,
                    time_window_minutes, late_after_minutes, qr_rotate_seconds,
                    session_secret, allow_manual_override, note, created_at, updated_at, deleted_at
                )
                values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?, ?, ?,
                    ?, ?, ?, ?,
                    ?, ?, ?,
                    ?, ?, ?, ?, ?, null
                )
                """,
                sessionId.toString(),
                groupId.toString(),
                creatorUserId.toString(),
                "Manual Override Test Session",
                Date.valueOf(LocalDate.now()),
                status,
                Timestamp.from(startAt),
                Timestamp.from(checkinOpenAt),
                Timestamp.from(checkinCloseAt),
                Timestamp.from(endAt),
                30,
                5,
                15,
                "secret-" + sessionId.toString().substring(0, 8),
                allowManualOverride ? 1 : 0,
                "test session",
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertAttendance(
            UUID sessionId,
            UUID userId,
            String attendanceStatus,
            String checkInMethod,
            Instant checkInAt,
            String qrTokenId,
            String deviceId,
            String ipAddress,
            String userAgent
    ) {
        Instant now = Instant.now().minusSeconds(2400);

        jdbcTemplate.update("""
                insert into session_attendance (
                    session_id, user_id, attendance_status, check_in_at, check_in_method, qr_token_id,
                    device_id, ip_address, user_agent, geo_lat, geo_lng, distance_meter,
                    suspicious_flag, suspicious_reason, excused_by_request_id, created_at, updated_at
                )
                values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?, ?, ?, ?,
                    ?, ?, ?, null, null, null,
                    0, null, null, ?, ?
                )
                """,
                sessionId.toString(),
                userId.toString(),
                attendanceStatus,
                checkInAt == null ? null : Timestamp.from(checkInAt),
                checkInMethod,
                qrTokenId,
                deviceId,
                ipAddress,
                userAgent,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void makeAttendancePresentQrLike(UUID sessionId, UUID userId, UUID issuerUserId) {
        Instant now = Instant.now().minusSeconds(120);
        String tokenId = "qr-old-token-" + UUID.randomUUID().toString().substring(0, 8);

        insertQrToken(sessionId, tokenId, issuerUserId);

        jdbcTemplate.update("""
            update session_attendance
            set attendance_status = 'PRESENT',
                check_in_at = ?,
                check_in_method = 'QR',
                qr_token_id = ?,
                device_id = 'device-123',
                ip_address = '127.0.0.1',
                user_agent = 'JUnit',
                geo_lat = 10.7768890,
                geo_lng = 106.7008060,
                distance_meter = 12,
                suspicious_flag = 1,
                suspicious_reason = 'old suspicious',
                excused_by_request_id = null,
                updated_at = ?
            where session_id = UUID_TO_BIN(?, 1)
              and user_id = UUID_TO_BIN(?, 1)
            """,
                Timestamp.from(now),
                tokenId,
                Timestamp.from(now),
                sessionId.toString(),
                userId.toString()
        );
    }

    private UUID insertApprovedAbsenceRequest(UUID groupId, UUID requesterUserId, UUID linkedSessionId, UUID reviewerUserId) {
        UUID requestId = UUID.randomUUID();
        Instant now = Instant.now().minusSeconds(200);

        jdbcTemplate.update("""
                insert into absence_requests (
                    id, group_id, requester_user_id, linked_session_id, requested_date, reason, evidence_url,
                    request_status, reviewer_user_id, reviewer_note, reviewed_at, created_at, updated_at,
                    cancelled_at, reverted_by_user_id, reverted_at, revert_note, pending_session_key
                )
                values (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    null, ?, null,
                    'APPROVED', UUID_TO_BIN(?, 1), ?, ?, ?, ?,
                    null, null, null, null, null
                )
                """,
                requestId.toString(),
                groupId.toString(),
                requesterUserId.toString(),
                linkedSessionId.toString(),
                "Approved absence for excused state test",
                reviewerUserId.toString(),
                "approved",
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );

        return requestId;
    }

    // =========================
    // Query helpers
    // =========================

    private Map<String, Object> findAttendanceRow(UUID sessionId, UUID userId) {
        return jdbcTemplate.queryForMap("""
                select
                    attendance_status,
                    check_in_at,
                    check_in_method,
                    qr_token_id,
                    device_id,
                    ip_address,
                    user_agent,
                    geo_lat,
                    geo_lng,
                    distance_meter,
                    suspicious_flag,
                    suspicious_reason,
                    excused_by_request_id,
                    updated_at
                from session_attendance
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                sessionId.toString(),
                userId.toString()
        );
    }

    private Instant attendanceUpdatedAt(UUID sessionId, UUID userId) {
        Timestamp ts = jdbcTemplate.queryForObject("""
                select updated_at
                from session_attendance
                where session_id = UUID_TO_BIN(?, 1)
                  and user_id = UUID_TO_BIN(?, 1)
                """,
                Timestamp.class,
                sessionId.toString(),
                userId.toString()
        );
        return ts.toInstant();
    }

    private long eventCountFor(UUID sessionId, UUID userId) {
        return attendanceEventRepository
                .findBySessionIdAndUserIdOrderByCreatedAtDesc(sessionId, userId, of(0, 200))
                .size();
    }

    private AttendanceEvent latestEventFor(UUID sessionId, UUID userId) {
        List<AttendanceEvent> events =
                attendanceEventRepository.findBySessionIdAndUserIdOrderByCreatedAtDesc(sessionId, userId, of(0, 1));

        assertThat(events).isNotEmpty();
        return events.get(0);
    }

    // =========================
    // Auth helper
    // =========================

    private RequestPostProcessor authAs(UUID userId, String email) {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        return authentication(authentication);
    }

    // =========================
    // Test scenario
    // =========================

    private record Scenario(
            UUID ownerId,
            String ownerEmail,
            UUID coHostId,
            String coHostEmail,
            UUID memberId,
            String memberEmail,
            UUID outsiderId,
            String outsiderEmail,
            UUID targetStudentId,
            UUID groupId,
            UUID sessionId
    ) {}
}