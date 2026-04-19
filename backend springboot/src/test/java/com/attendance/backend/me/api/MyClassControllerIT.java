package com.attendance.backend.me.api;

import com.attendance.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MyClassControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanupTables();
    }

    @Test
    void list_all_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.items", hasSize(7)));
    }

    @Test
    void q_search_by_group_name_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "trí tuệ nhân tạo")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Trí tuệ nhân tạo")));
    }

    @Test
    void q_search_by_legacy_code_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "LEG-999")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Phân tích dữ liệu")));
    }

    @Test
    void q_search_by_course_code_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "COURSE-888")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Machine Learning")));
    }

    @Test
    void q_search_by_class_code_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "CLASS-777")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Lập trình Web")));
    }

    @Test
    void q_search_by_lecturer_name_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "Lecturer Unique")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].lecturerName", is("Lecturer Unique")));
    }

    @Test
    void filter_semester_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("semester", "HK1")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    @Test
    void filter_academic_year_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("academicYear", "2025-2026")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    void filter_semester_and_academic_year_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("semester", "HK1")
                        .queryParam("academicYear", "2025-2026")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    void q_semester_and_academic_year_combine_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "java")
                        .queryParam("semester", "HK1")
                        .queryParam("academicYear", "2025-2026")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].groupName", is("Lập trình Java")));
    }

    @Test
    void scope_teaching_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("scope", "TEACHING")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void scope_studying_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("scope", "STUDYING")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.items[0].myRole", is("MEMBER")));
    }

    @Test
    void teaching_convenience_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes/teaching")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void teaching_convenience_search_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes/teaching")
                        .queryParam("q", "trí tuệ nhân tạo")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Trí tuệ nhân tạo")));
    }

    @Test
    void teaching_convenience_paging_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes/teaching")
                        .queryParam("page", "0")
                        .queryParam("size", "1")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void paging_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(7)))
                .andExpect(jsonPath("$.totalPages", is(4)));
    }

    @Test
    void blank_semester_and_academic_year_are_ignored() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("semester", "   ")
                        .queryParam("academicYear", "   ")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(7)));
    }

    @Test
    void representative_session_and_field_mapping_regression_success() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes")
                        .queryParam("q", "Trí tuệ nhân tạo")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupName", is("Trí tuệ nhân tạo")))
                .andExpect(jsonPath("$.items[0].courseCode", is("AI202")))
                .andExpect(jsonPath("$.items[0].classCode", is("CLC-AI-01")))
                .andExpect(jsonPath("$.items[0].campus", is("Cơ sở 1")))
                .andExpect(jsonPath("$.items[0].room", is("A101")))
                .andExpect(jsonPath("$.items[0].locationDisplay", is("Cơ sở 1 - A101")))
                .andExpect(jsonPath("$.items[0].semester", is("HK2")))
                .andExpect(jsonPath("$.items[0].academicYear", is("2025-2026")))
                .andExpect(jsonPath("$.items[0].lecturerName", is("Actor User")))
                .andExpect(jsonPath("$.items[0].approvedStudentCount", is(2)))
                .andExpect(jsonPath("$.items[0].myRole", is("OWNER")))
                .andExpect(jsonPath("$.items[0].myMemberStatus", is("APPROVED")));
    }

    @Test
    void semesters_endpoint_success_distinct_sorted_and_skip_null_null() throws Exception {
        TestData data = seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes/semesters")
                        .with(auth(data.actorUserId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].semester", is("HK2")))
                .andExpect(jsonPath("$[0].academicYear", is("2025-2026")))
                .andExpect(jsonPath("$[0].label", is("HK2, 2025-2026")))
                .andExpect(jsonPath("$[1].semester", is("HK1")))
                .andExpect(jsonPath("$[1].academicYear", is("2025-2026")))
                .andExpect(jsonPath("$[2].semester", is("HK1")))
                .andExpect(jsonPath("$[2].academicYear", is("2024-2025")));
    }

    @Test
    void unauthorized() throws Exception {
        seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void teaching_convenience_unauthorized() throws Exception {
        seedBaseDataset();

        mockMvc.perform(get("/api/v1/me/classes/teaching"))
                .andExpect(status().isUnauthorized());
    }

    private TestData seedBaseDataset() {
        LocalDateTime now = LocalDateTime.now().withNano(0);

        UUID actor = UUID.randomUUID();
        UUID lecturerHai = UUID.randomUUID();
        UUID lecturerLan = UUID.randomUUID();
        UUID lecturerUnique = UUID.randomUUID();
        UUID approvedMember1 = UUID.randomUUID();
        UUID approvedMember2 = UUID.randomUUID();
        UUID pendingMember = UUID.randomUUID();

        insertUser(actor, "actor@example.com", "Actor User", "https://img/actor.png");
        insertUser(lecturerHai, "hai@example.com", "Dr. Hải", "https://img/hai.png");
        insertUser(lecturerLan, "lan@example.com", "Cô Lan", "https://img/lan.png");
        insertUser(lecturerUnique, "unique@example.com", "Lecturer Unique", "https://img/unique.png");
        insertUser(approvedMember1, "m1@example.com", "Member 1", null);
        insertUser(approvedMember2, "m2@example.com", "Member 2", null);
        insertUser(pendingMember, "pending@example.com", "Pending Member", null);

        UUID g1 = UUID.randomUUID();
        UUID g2 = UUID.randomUUID();
        UUID g3 = UUID.randomUUID();
        UUID g4 = UUID.randomUUID();
        UUID g5 = UUID.randomUUID();
        UUID g6 = UUID.randomUUID();
        UUID g7 = UUID.randomUUID();

        insertGroup(g1, actor, "Trí tuệ nhân tạo", "AI-LEG-001", "HK2", "2025-2026", "AI202", "CLC-AI-01", "Cơ sở 1", "A101", "ACTIVE", now.minusDays(10), now.minusHours(1), "https://img/g1.png");
        insertGroup(g2, lecturerLan, "Lập trình Java", "JAVA-001", "HK1", "2025-2026", "SE330", "CLC-JAVA-02", "Cơ sở 2", "B202", "ACTIVE", now.minusDays(9), now.minusHours(2), "https://img/g2.png");
        insertGroup(g3, lecturerLan, "Phân tích dữ liệu", "LEG-999", "HK1", "2024-2025", "DA100", "CLC-DA-01", "Cơ sở 3", "C303", "ACTIVE", now.minusDays(8), now.minusHours(3), "https://img/g3.png");
        insertGroup(g4, lecturerHai, "Machine Learning", "ML-001", "HK2", "2025-2026", "COURSE-888", "CLC-ML-01", "Cơ sở 1", "D404", "ACTIVE", now.minusDays(7), now.minusHours(4), "https://img/g4.png");
        insertGroup(g5, lecturerUnique, "Lập trình Web", "WEB-001", "HK1", "2025-2026", "WEB100", "CLASS-777", "Cơ sở 4", "E505", "ACTIVE", now.minusDays(6), now.minusHours(5), "https://img/g5.png");
        insertGroup(g6, lecturerLan, "Không có học kỳ", "NO-SEM-001", null, null, "NULL100", "NULL-01", "Cơ sở 5", "F606", "ACTIVE", now.minusDays(5), now.minusHours(6), "https://img/g6.png");
        insertGroup(g7, lecturerHai, "Nhập môn Java", "JAVA-002", "HK1", "2025-2026", "SE331", "CLC-JAVA-03", "Cơ sở 2", "B203", "ACTIVE", now.minusDays(4), now.minusHours(7), "https://img/g7.png");

        insertMember(g1, actor, "OWNER", "APPROVED", now.minusDays(10));
        insertMember(g1, approvedMember1, "MEMBER", "APPROVED", now.minusDays(10));
        insertMember(g1, approvedMember2, "MEMBER", "APPROVED", now.minusDays(10));
        insertMember(g1, pendingMember, "MEMBER", "PENDING", now.minusDays(10));

        insertMember(g2, actor, "CO_HOST", "APPROVED", now.minusDays(9));
        insertMember(g3, actor, "MEMBER", "APPROVED", now.minusDays(8));
        insertMember(g4, actor, "MEMBER", "APPROVED", now.minusDays(7));
        insertMember(g5, actor, "MEMBER", "APPROVED", now.minusDays(6));
        insertMember(g6, actor, "MEMBER", "APPROVED", now.minusDays(5));
        insertMember(g7, actor, "MEMBER", "APPROVED", now.minusDays(4));

        insertSession(UUID.randomUUID(), g1, actor, "OPEN", now.plusHours(1), now.plusHours(3), now.minusHours(1));
        insertSession(UUID.randomUUID(), g1, actor, "CLOSED", now.minusDays(2), now.minusDays(2).plusHours(2), now.minusDays(2));

        insertSession(UUID.randomUUID(), g2, lecturerLan, "CLOSED", now.plusDays(1), now.plusDays(1).plusHours(2), now.minusDays(1));
        insertSession(UUID.randomUUID(), g3, lecturerLan, "CLOSED", now.minusDays(1), now.minusDays(1).plusHours(2), now.minusDays(1));
        insertSession(UUID.randomUUID(), g4, lecturerHai, "CANCELLED", now.plusDays(2), now.plusDays(2).plusHours(2), now.minusDays(1));
        insertSession(UUID.randomUUID(), g4, lecturerHai, "CLOSED", now.plusDays(3), now.plusDays(3).plusHours(2), now.minusDays(1));
        insertSession(UUID.randomUUID(), g5, lecturerUnique, "CLOSED", now.minusDays(5), now.minusDays(5).plusHours(2), now.minusDays(5));

        return new TestData(actor);
    }

    private void insertUser(UUID id, String email, String fullName, String avatarUrl) {
        jdbcTemplate.update(
                "INSERT INTO users (" +
                        "id, platform_role, email, password_hash, full_name, avatar_url, user_code, primary_device_id, status, created_at, updated_at, deleted_at" +
                        ") VALUES (?, 'USER', ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?)",
                uuidToBytes(id),
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                fullName,
                avatarUrl,
                null,
                null,
                Timestamp.valueOf(LocalDateTime.now().minusDays(20)),
                Timestamp.valueOf(LocalDateTime.now().minusDays(20)),
                null
        );
    }

    private void insertGroup(
            UUID id,
            UUID ownerUserId,
            String name,
            String code,
            String semester,
            String academicYear,
            String courseCode,
            String classCode,
            String campus,
            String room,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            String thumbnailUrl
    ) {
        jdbcTemplate.update(
                "INSERT INTO class_groups (" +
                        "id, owner_user_id, name, code, join_code, description, semester, room, approval_mode, allow_auto_join_on_checkin, status, created_at, updated_at, deleted_at, course_code, class_code, campus, academic_year, thumbnail_url" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'AUTO', 0, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuidToBytes(id),
                uuidToBytes(ownerUserId),
                name,
                code,
                "JOIN" + code.replace("-", ""),
                "desc-" + name,
                semester,
                room,
                status,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(updatedAt),
                null,
                courseCode,
                classCode,
                campus,
                academicYear,
                thumbnailUrl
        );
    }

    private void insertMember(UUID groupId, UUID userId, String role, String memberStatus, LocalDateTime joinedAt) {
        jdbcTemplate.update(
                "INSERT INTO group_members (" +
                        "group_id, user_id, role, member_status, joined_at, invited_by, created_at, updated_at, removed_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuidToBytes(groupId),
                uuidToBytes(userId),
                role,
                memberStatus,
                Timestamp.valueOf(joinedAt),
                null,
                Timestamp.valueOf(joinedAt),
                Timestamp.valueOf(joinedAt),
                null
        );
    }

    private void insertSession(
            UUID id,
            UUID groupId,
            UUID createdByUserId,
            String status,
            LocalDateTime startAt,
            LocalDateTime endAt,
            LocalDateTime createdAt
    ) {
        jdbcTemplate.update(
                "INSERT INTO attendance_sessions (" +
                        "id, group_id, created_by_user_id, title, session_date, status, start_at, checkin_open_at, checkin_close_at, end_at, time_window_minutes, late_after_minutes, qr_rotate_seconds, session_secret, allow_manual_override, note, created_at, updated_at, deleted_at" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                uuidToBytes(id),
                uuidToBytes(groupId),
                uuidToBytes(createdByUserId),
                "Session-" + id,
                LocalDate.from(startAt),
                status,
                Timestamp.valueOf(startAt),
                Timestamp.valueOf(startAt),
                Timestamp.valueOf(startAt.plusMinutes(15)),
                endAt == null ? null : Timestamp.valueOf(endAt),
                15,
                5,
                15,
                "secret-" + id,
                true,
                null,
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                null
        );
    }

    private RequestPostProcessor auth(UUID userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.emptyList()
        );

        return authentication(authToken);
    }

    private void cleanupTables() {
        jdbcTemplate.update("DELETE FROM session_attendance");
        jdbcTemplate.update("DELETE FROM attendance_events");
        jdbcTemplate.update("DELETE FROM absence_requests");
        jdbcTemplate.update("DELETE FROM attendance_sessions");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM class_groups");
        jdbcTemplate.update("DELETE FROM users");
    }

    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private record TestData(UUID actorUserId) {
    }
}