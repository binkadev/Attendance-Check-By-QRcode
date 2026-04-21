package com.attendance.backend.group.api;

import com.attendance.backend.domain.entity.GroupWeeklySchedule;
import com.attendance.backend.group.repository.GroupWeeklyScheduleRepository;
import com.attendance.backend.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GroupControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GroupWeeklyScheduleRepository groupWeeklyScheduleRepository;

    @BeforeEach
    void setUp() {
        cleanupTables();
    }

    @Test
    void create_group_step2_happy_path_success() throws Exception {
        UUID actorUserId = UUID.randomUUID();
        insertUser(actorUserId, "owner-test@example.com", "Owner Test");

        String requestBody = """
                {
                  "name": "Lập trình Android",
                  "code": "DBG001",
                  "courseCode": "INT1348",
                  "classCode": "D22CQCNPM02-N",
                  "joinCode": "DBG12345",
                  "description": "Link nhóm Zalo, tóm tắt đề cương...",
                  "semester": "HK2",
                  "academicYear": "2025-2026",
                  "campus": "CS Thu Duc",
                  "room": "A101",
                  "approvalMode": "AUTO",
                  "allowAutoJoinOnCheckin": false,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "08:00",
                      "endTime": "10:00"
                    },
                    {
                      "dayOfWeek": "THURSDAY",
                      "startTime": "13:00",
                      "endTime": "15:00"
                    }
                  ],
                  "totalSessions": 11,
                  "maxAllowedAbsences": 3
                }
                """;

        String responseBody = mockMvc.perform(post("/api/v1/groups")
                        .with(auth(actorUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Lập trình Android")))
                .andExpect(jsonPath("$.code", is("DBG001")))
                .andExpect(jsonPath("$.courseCode", is("INT1348")))
                .andExpect(jsonPath("$.classCode", is("D22CQCNPM02-N")))
                .andExpect(jsonPath("$.joinCode", is("DBG12345")))
                .andExpect(jsonPath("$.description", is("Link nhóm Zalo, tóm tắt đề cương...")))
                .andExpect(jsonPath("$.semester", is("HK2")))
                .andExpect(jsonPath("$.academicYear", is("2025-2026")))
                .andExpect(jsonPath("$.campus", is("CS Thu Duc")))
                .andExpect(jsonPath("$.room", is("A101")))
                .andExpect(jsonPath("$.totalSessions", is(11)))
                .andExpect(jsonPath("$.maxAllowedAbsences", is(3)))
                .andExpect(jsonPath("$.weeklySchedules", hasSize(2)))
                .andExpect(jsonPath("$.approvalMode", is("AUTO")))
                .andExpect(jsonPath("$.allowAutoJoinOnCheckin", is(false)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        String groupId = json.get("id").asText();

        JsonNode weeklySchedules = json.get("weeklySchedules");
        assertEquals(2, weeklySchedules.size());
        assertTrue(hasSchedule(weeklySchedules, "TUESDAY", "08:00", "10:00"));
        assertTrue(hasSchedule(weeklySchedules, "THURSDAY", "13:00", "15:00"));

        Map<String, Object> groupRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    code,
                    course_code,
                    class_code,
                    join_code,
                    description,
                    semester,
                    academic_year,
                    campus,
                    room,
                    total_sessions,
                    max_allowed_absences,
                    status
                FROM class_groups
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                groupId
        );

        assertEquals("DBG001", groupRow.get("code"));
        assertEquals("INT1348", groupRow.get("course_code"));
        assertEquals("D22CQCNPM02-N", groupRow.get("class_code"));
        assertEquals("DBG12345", groupRow.get("join_code"));
        assertEquals("Link nhóm Zalo, tóm tắt đề cương...", groupRow.get("description"));
        assertEquals("HK2", groupRow.get("semester"));
        assertEquals("2025-2026", groupRow.get("academic_year"));
        assertEquals("CS Thu Duc", groupRow.get("campus"));
        assertEquals("A101", groupRow.get("room"));
        assertEquals(11, ((Number) groupRow.get("total_sessions")).intValue());
        assertEquals(3, ((Number) groupRow.get("max_allowed_absences")).intValue());
        assertEquals("ACTIVE", groupRow.get("status"));

        Map<String, Object> ownerMemberRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    role,
                    member_status
                FROM group_members
                WHERE group_id = UUID_TO_BIN(?, 1)
                  AND user_id = UUID_TO_BIN(?, 1)
                """,
                groupId,
                actorUserId.toString()
        );

        assertEquals("OWNER", ownerMemberRow.get("role"));
        assertEquals("APPROVED", ownerMemberRow.get("member_status"));

        List<GroupWeeklySchedule> schedules = groupWeeklyScheduleRepository
                .findByGroupIdOrderByDayOfWeekAscStartTimeAsc(UUID.fromString(groupId));

        assertEquals(2, schedules.size());
        assertTrue(schedules.stream().anyMatch(item ->
                "TUESDAY".equals(item.getDayOfWeek())
                        && item.getStartTime().equals(LocalTime.of(8, 0))
                        && item.getEndTime().equals(LocalTime.of(10, 0))
        ));
        assertTrue(schedules.stream().anyMatch(item ->
                "THURSDAY".equals(item.getDayOfWeek())
                        && item.getStartTime().equals(LocalTime.of(13, 0))
                        && item.getEndTime().equals(LocalTime.of(15, 0))
        ));

        mockMvc.perform(get("/api/v1/me/classes?scope=TEACHING")
                        .with(auth(actorUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupId", is(groupId)))
                .andExpect(jsonPath("$.items[0].groupName", is("Lập trình Android")))
                .andExpect(jsonPath("$.items[0].courseCode", is("INT1348")))
                .andExpect(jsonPath("$.items[0].classCode", is("D22CQCNPM02-N")))
                .andExpect(jsonPath("$.items[0].academicYear", is("2025-2026")))
                .andExpect(jsonPath("$.items[0].campus", is("CS Thu Duc")))
                .andExpect(jsonPath("$.items[0].room", is("A101")))
                .andExpect(jsonPath("$.items[0].myRole", is("OWNER")))
                .andExpect(jsonPath("$.items[0].myMemberStatus", is("APPROVED")));
    }

    @Test
    void create_group_step2_invalid_schedule_returns_400() throws Exception {
        UUID actorUserId = UUID.randomUUID();
        insertUser(actorUserId, "owner-test@example.com", "Owner Test");

        String requestBody = """
                {
                  "name": "Invalid Schedule Group",
                  "code": "DBG002",
                  "courseCode": "INT1348",
                  "classCode": "D22CQCNPM02-N",
                  "joinCode": "DBG12346",
                  "description": "invalid schedule",
                  "semester": "HK2",
                  "academicYear": "2025-2026",
                  "campus": "CS Thu Duc",
                  "room": "A101",
                  "approvalMode": "AUTO",
                  "allowAutoJoinOnCheckin": false,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "10:00",
                      "endTime": "08:00"
                    }
                  ],
                  "totalSessions": 11,
                  "maxAllowedAbsences": 3
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .with(auth(actorUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_WEEKLY_SCHEDULE_TIME_RANGE")));
    }

    @Test
    void create_group_step2_invalid_policy_returns_400() throws Exception {
        UUID actorUserId = UUID.randomUUID();
        insertUser(actorUserId, "owner-test@example.com", "Owner Test");

        String requestBody = """
                {
                  "name": "Invalid Policy Group",
                  "code": "DBG003",
                  "courseCode": "INT1348",
                  "classCode": "D22CQCNPM02-N",
                  "joinCode": "DBG12347",
                  "description": "invalid policy",
                  "semester": "HK2",
                  "academicYear": "2025-2026",
                  "campus": "CS Thu Duc",
                  "room": "A101",
                  "approvalMode": "AUTO",
                  "allowAutoJoinOnCheckin": false,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "08:00",
                      "endTime": "10:00"
                    }
                  ],
                  "totalSessions": 3,
                  "maxAllowedAbsences": 5
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .with(auth(actorUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ABSENCE_POLICY")));
    }

    @Test
    void create_group_step2_duplicate_conflict_returns_409() throws Exception {
        UUID actorUserId = UUID.randomUUID();
        insertUser(actorUserId, "owner-test@example.com", "Owner Test");

        String requestBody = """
                {
                  "name": "Duplicate Group",
                  "code": "DBG004",
                  "courseCode": "INT1348",
                  "classCode": "D22CQCNPM02-N",
                  "joinCode": "DBG12348",
                  "description": "duplicate test",
                  "semester": "HK2",
                  "academicYear": "2025-2026",
                  "campus": "CS Thu Duc",
                  "room": "A101",
                  "approvalMode": "AUTO",
                  "allowAutoJoinOnCheckin": false,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "08:00",
                      "endTime": "10:00"
                    }
                  ],
                  "totalSessions": 11,
                  "maxAllowedAbsences": 3
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .with(auth(actorUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/groups")
                        .with(auth(actorUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DB_CONSTRAINT_VIOLATION")));
    }

    @Test
    void create_group_step2_unauthorized_returns_401() throws Exception {
        String requestBody = """
                {
                  "name": "Unauthorized Group",
                  "code": "DBG999",
                  "courseCode": "INT0000",
                  "classCode": "UNAUTH-01",
                  "joinCode": "UNAUTH99",
                  "description": "should fail",
                  "semester": "HK2",
                  "academicYear": "2025-2026",
                  "campus": "CS Thu Duc",
                  "room": "A999",
                  "approvalMode": "AUTO",
                  "allowAutoJoinOnCheckin": false,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "TUESDAY",
                      "startTime": "08:00",
                      "endTime": "10:00"
                    }
                  ],
                  "totalSessions": 11,
                  "maxAllowedAbsences": 3
                }
                """;

        mockMvc.perform(post("/api/v1/groups")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void get_group_detail_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-detail@example.com", "Owner Detail");
        insertGroup(
                groupId,
                ownerUserId,
                "Kiến trúc máy tính",
                "DBG010",
                "INT2001",
                "D22CQCNPM01-N",
                "JOIN0010",
                "Chi tiết lớp học",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "B201",
                14,
                4,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "MONDAY", "09:00", "11:00");
        insertSchedule(groupId, "WEDNESDAY", "13:00", "15:00");

        String responseBody = mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(groupId.toString())))
                .andExpect(jsonPath("$.ownerUserId", is(ownerUserId.toString())))
                .andExpect(jsonPath("$.name", is("Kiến trúc máy tính")))
                .andExpect(jsonPath("$.code", is("DBG010")))
                .andExpect(jsonPath("$.courseCode", is("INT2001")))
                .andExpect(jsonPath("$.classCode", is("D22CQCNPM01-N")))
                .andExpect(jsonPath("$.joinCode", is("JOIN0010")))
                .andExpect(jsonPath("$.description", is("Chi tiết lớp học")))
                .andExpect(jsonPath("$.semester", is("HK1")))
                .andExpect(jsonPath("$.academicYear", is("2025-2026")))
                .andExpect(jsonPath("$.campus", is("CS Thu Duc")))
                .andExpect(jsonPath("$.room", is("B201")))
                .andExpect(jsonPath("$.totalSessions", is(14)))
                .andExpect(jsonPath("$.maxAllowedAbsences", is(4)))
                .andExpect(jsonPath("$.approvalMode", is("AUTO")))
                .andExpect(jsonPath("$.allowAutoJoinOnCheckin", is(false)))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.weeklySchedules", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        JsonNode weeklySchedules = json.get("weeklySchedules");
        assertEquals(2, weeklySchedules.size());
        assertTrue(hasSchedule(weeklySchedules, "MONDAY", "09:00", "11:00"));
        assertTrue(hasSchedule(weeklySchedules, "WEDNESDAY", "13:00", "15:00"));
    }

    @Test
    void get_group_detail_forbidden_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-forbidden@example.com", "Owner Forbidden");
        insertUser(memberUserId, "member-forbidden@example.com", "Member Forbidden");

        insertGroup(
                groupId,
                ownerUserId,
                "Mạng máy tính",
                "DBG011",
                "INT3001",
                "D22CQCNPM03-N",
                "JOIN0011",
                "Forbidden detail",
                "HK2",
                "2025-2026",
                "CS Thu Duc",
                "B202",
                12,
                3,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, memberUserId, "MEMBER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        mockMvc.perform(get("/api/v1/groups/{groupId}", groupId)
                        .with(auth(memberUserId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void patch_group_success_replaces_schedules() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-update@example.com", "Owner Update");
        insertGroup(
                groupId,
                ownerUserId,
                "Lập trình Java",
                "DBG020",
                "INT4001",
                "D22CQCNPM04-N",
                "JOIN0020",
                "Before update",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A201",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "name": "Lập trình Java nâng cao",
                  "courseCode": "INT4999",
                  "classCode": "D22CQCNPM99-N",
                  "joinCode": "JOIN9999",
                  "description": "After update",
                  "semester": "HK3",
                  "academicYear": "2026-2027",
                  "campus": "CS Quan 9",
                  "room": "C301",
                  "approvalMode": "MANUAL",
                  "allowAutoJoinOnCheckin": true,
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "09:00",
                      "endTime": "11:00"
                    },
                    {
                      "dayOfWeek": "FRIDAY",
                      "startTime": "14:00",
                      "endTime": "16:00"
                    }
                  ],
                  "totalSessions": 15,
                  "maxAllowedAbsences": 4
                }
                """;

        String responseBody = mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(groupId.toString())))
                .andExpect(jsonPath("$.name", is("Lập trình Java nâng cao")))
                .andExpect(jsonPath("$.code", is("DBG020")))
                .andExpect(jsonPath("$.courseCode", is("INT4999")))
                .andExpect(jsonPath("$.classCode", is("D22CQCNPM99-N")))
                .andExpect(jsonPath("$.joinCode", is("JOIN9999")))
                .andExpect(jsonPath("$.description", is("After update")))
                .andExpect(jsonPath("$.semester", is("HK3")))
                .andExpect(jsonPath("$.academicYear", is("2026-2027")))
                .andExpect(jsonPath("$.campus", is("CS Quan 9")))
                .andExpect(jsonPath("$.room", is("C301")))
                .andExpect(jsonPath("$.totalSessions", is(15)))
                .andExpect(jsonPath("$.maxAllowedAbsences", is(4)))
                .andExpect(jsonPath("$.approvalMode", is("MANUAL")))
                .andExpect(jsonPath("$.allowAutoJoinOnCheckin", is(true)))
                .andExpect(jsonPath("$.weeklySchedules", hasSize(2)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        JsonNode weeklySchedules = json.get("weeklySchedules");
        assertEquals(2, weeklySchedules.size());
        assertTrue(hasSchedule(weeklySchedules, "MONDAY", "09:00", "11:00"));
        assertTrue(hasSchedule(weeklySchedules, "FRIDAY", "14:00", "16:00"));

        Map<String, Object> groupRow = jdbcTemplate.queryForMap(
                """
                SELECT
                    name,
                    code,
                    course_code,
                    class_code,
                    join_code,
                    description,
                    semester,
                    academic_year,
                    campus,
                    room,
                    total_sessions,
                    max_allowed_absences,
                    approval_mode,
                    allow_auto_join_on_checkin,
                    status
                FROM class_groups
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                groupId.toString()
        );

        assertEquals("Lập trình Java nâng cao", groupRow.get("name"));
        assertEquals("DBG020", groupRow.get("code"));
        assertEquals("INT4999", groupRow.get("course_code"));
        assertEquals("D22CQCNPM99-N", groupRow.get("class_code"));
        assertEquals("JOIN9999", groupRow.get("join_code"));
        assertEquals("After update", groupRow.get("description"));
        assertEquals("HK3", groupRow.get("semester"));
        assertEquals("2026-2027", groupRow.get("academic_year"));
        assertEquals("CS Quan 9", groupRow.get("campus"));
        assertEquals("C301", groupRow.get("room"));
        assertEquals(15, ((Number) groupRow.get("total_sessions")).intValue());
        assertEquals(4, ((Number) groupRow.get("max_allowed_absences")).intValue());
        assertEquals("MANUAL", groupRow.get("approval_mode"));
        assertEquals(1, ((Number) groupRow.get("allow_auto_join_on_checkin")).intValue());
        assertEquals("ACTIVE", groupRow.get("status"));

        List<GroupWeeklySchedule> schedules = groupWeeklyScheduleRepository
                .findByGroupIdOrderByDayOfWeekAscStartTimeAsc(groupId);

        assertEquals(2, schedules.size());
        assertTrue(schedules.stream().anyMatch(item ->
                "MONDAY".equals(item.getDayOfWeek())
                        && item.getStartTime().equals(LocalTime.of(9, 0))
                        && item.getEndTime().equals(LocalTime.of(11, 0))
        ));
        assertTrue(schedules.stream().anyMatch(item ->
                "FRIDAY".equals(item.getDayOfWeek())
                        && item.getStartTime().equals(LocalTime.of(14, 0))
                        && item.getEndTime().equals(LocalTime.of(16, 0))
        ));

        mockMvc.perform(get("/api/v1/me/classes?scope=TEACHING")
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].groupId", is(groupId.toString())))
                .andExpect(jsonPath("$.items[0].groupName", is("Lập trình Java nâng cao")))
                .andExpect(jsonPath("$.items[0].courseCode", is("INT4999")))
                .andExpect(jsonPath("$.items[0].classCode", is("D22CQCNPM99-N")))
                .andExpect(jsonPath("$.items[0].academicYear", is("2026-2027")))
                .andExpect(jsonPath("$.items[0].campus", is("CS Quan 9")))
                .andExpect(jsonPath("$.items[0].room", is("C301")))
                .andExpect(jsonPath("$.items[0].myRole", is("OWNER")))
                .andExpect(jsonPath("$.items[0].myMemberStatus", is("APPROVED")));
    }

    @Test
    void patch_group_invalid_schedule_returns_400() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-invalid-schedule@example.com", "Owner Invalid Schedule");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test invalid schedule",
                "DBG021",
                "INT5001",
                "D22CQCNPM05-N",
                "JOIN0021",
                "Before invalid schedule",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A202",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "weeklySchedules": [
                    {
                      "dayOfWeek": "MONDAY",
                      "startTime": "11:00",
                      "endTime": "09:00"
                    }
                  ]
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_WEEKLY_SCHEDULE_TIME_RANGE")));
    }

    @Test
    void patch_group_invalid_policy_returns_400() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-invalid-policy@example.com", "Owner Invalid Policy");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test invalid policy",
                "DBG022",
                "INT5002",
                "D22CQCNPM06-N",
                "JOIN0022",
                "Before invalid policy",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A203",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "totalSessions": 2,
                  "maxAllowedAbsences": 5
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_ABSENCE_POLICY")));
    }

    @Test
    void patch_group_forbidden_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID memberUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-patch-forbidden@example.com", "Owner Patch Forbidden");
        insertUser(memberUserId, "member-patch-forbidden@example.com", "Member Patch Forbidden");

        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test forbidden update",
                "DBG023",
                "INT5003",
                "D22CQCNPM07-N",
                "JOIN0023",
                "Forbidden update",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A204",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, memberUserId, "MEMBER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "name": "Should not update"
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                        .with(auth(memberUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void patch_group_unauthorized_returns_401() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-patch-unauthorized@example.com", "Owner Patch Unauthorized");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test unauthorized update",
                "DBG024",
                "INT5004",
                "D22CQCNPM08-N",
                "JOIN0024",
                "Unauthorized update",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A205",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "name": "Should not update"
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}", groupId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void patch_group_status_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-status@example.com", "Owner Status");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test status",
                "DBG030",
                "INT6001",
                "D22CQCNPM10-N",
                "JOIN0030",
                "Status test",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A301",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "status": "ARCHIVED"
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}/status", groupId)
                        .with(auth(ownerUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(groupId.toString())))
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status
                FROM class_groups
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                groupId.toString()
        );

        assertEquals("ARCHIVED", row.get("status"));
    }

    @Test
    void post_archive_group_compatibility_success() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-archive@example.com", "Owner Archive");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test archive",
                "DBG031",
                "INT6002",
                "D22CQCNPM11-N",
                "JOIN0031",
                "Archive test",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A302",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        mockMvc.perform(post("/api/v1/groups/{groupId}/archive", groupId)
                        .with(auth(ownerUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(groupId.toString())))
                .andExpect(jsonPath("$.status", is("ARCHIVED")));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                SELECT status
                FROM class_groups
                WHERE id = UUID_TO_BIN(?, 1)
                """,
                groupId.toString()
        );

        assertEquals("ARCHIVED", row.get("status"));
    }

    @Test
    void patch_group_status_forbidden_returns_403() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID coHostUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-status-forbidden@example.com", "Owner Status Forbidden");
        insertUser(coHostUserId, "cohost-status-forbidden@example.com", "Cohost Status Forbidden");

        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test status forbidden",
                "DBG032",
                "INT6003",
                "D22CQCNPM12-N",
                "JOIN0032",
                "Status forbidden test",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A303",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertMember(groupId, coHostUserId, "CO_HOST", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "status": "ARCHIVED"
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}/status", groupId)
                        .with(auth(coHostUserId))
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")));
    }

    @Test
    void patch_group_status_unauthorized_returns_401() throws Exception {
        UUID ownerUserId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        insertUser(ownerUserId, "owner-status-unauthorized@example.com", "Owner Status Unauthorized");
        insertGroup(
                groupId,
                ownerUserId,
                "Lớp test status unauthorized",
                "DBG033",
                "INT6004",
                "D22CQCNPM13-N",
                "JOIN0033",
                "Status unauthorized test",
                "HK1",
                "2025-2026",
                "CS Thu Duc",
                "A304",
                10,
                2,
                "AUTO",
                false,
                "ACTIVE"
        );
        insertMember(groupId, ownerUserId, "OWNER", "APPROVED");
        insertSchedule(groupId, "TUESDAY", "08:00", "10:00");

        String requestBody = """
                {
                  "status": "ARCHIVED"
                }
                """;

        mockMvc.perform(patch("/api/v1/groups/{groupId}/status", groupId)
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    private boolean hasSchedule(JsonNode schedules, String dayOfWeek, String startTime, String endTime) {
        for (JsonNode item : schedules) {
            if (dayOfWeek.equals(item.get("dayOfWeek").asText())
                    && startTime.equals(item.get("startTime").asText())
                    && endTime.equals(item.get("endTime").asText())) {
                return true;
            }
        }
        return false;
    }

    private RequestPostProcessor auth(UUID userId) {
        UserPrincipal principal = Mockito.mock(UserPrincipal.class);
        when(principal.getUserId()).thenReturn(userId);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authToken);
    }

    private void insertUser(UUID id, String email, String fullName) {
        LocalDateTime now = LocalDateTime.now().minusDays(1);

        jdbcTemplate.update(
                """
                INSERT INTO users (
                    id,
                    platform_role,
                    email,
                    password_hash,
                    full_name,
                    avatar_url,
                    user_code,
                    primary_device_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    'USER',
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    'ACTIVE',
                    ?,
                    ?,
                    ?
                )
                """,
                id.toString(),
                email,
                "$2a$10$abcdefghijklmnopqrstuv",
                fullName,
                null,
                null,
                null,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                null
        );
    }

    private void insertGroup(
            UUID groupId,
            UUID ownerUserId,
            String name,
            String code,
            String courseCode,
            String classCode,
            String joinCode,
            String description,
            String semester,
            String academicYear,
            String campus,
            String room,
            int totalSessions,
            int maxAllowedAbsences,
            String approvalMode,
            boolean allowAutoJoinOnCheckin,
            String status
    ) {
        LocalDateTime now = LocalDateTime.now().minusHours(2);

        jdbcTemplate.update(
                """
                INSERT INTO class_groups (
                    id,
                    owner_user_id,
                    name,
                    code,
                    course_code,
                    class_code,
                    join_code,
                    description,
                    thumbnail_url,
                    semester,
                    academic_year,
                    room,
                    campus,
                    total_sessions,
                    max_allowed_absences,
                    approval_mode,
                    allow_auto_join_on_checkin,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                groupId.toString(),
                ownerUserId.toString(),
                name,
                code,
                courseCode,
                classCode,
                joinCode,
                description,
                null,
                semester,
                academicYear,
                room,
                campus,
                totalSessions,
                maxAllowedAbsences,
                approvalMode,
                allowAutoJoinOnCheckin ? 1 : 0,
                status,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                null
        );
    }

    private void insertMember(UUID groupId, UUID userId, String role, String memberStatus) {
        LocalDateTime now = LocalDateTime.now().minusHours(1);

        jdbcTemplate.update(
                """
                INSERT INTO group_members (
                    group_id,
                    user_id,
                    role,
                    member_status,
                    joined_at,
                    invited_by
                ) VALUES (
                    UUID_TO_BIN(?, 1),
                    UUID_TO_BIN(?, 1),
                    ?,
                    ?,
                    ?,
                    ?
                )
                """,
                groupId.toString(),
                userId.toString(),
                role,
                memberStatus,
                Timestamp.valueOf(now),
                null
        );
    }

    private void insertSchedule(UUID groupId, String dayOfWeek, String startTime, String endTime) {
        GroupWeeklySchedule schedule = new GroupWeeklySchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setGroupId(groupId);
        schedule.setDayOfWeek(dayOfWeek);
        schedule.setStartTime(LocalTime.parse(startTime));
        schedule.setEndTime(LocalTime.parse(endTime));
        schedule.setDeletedAt(null);

        groupWeeklyScheduleRepository.saveAndFlush(schedule);
    }

    private void cleanupTables() {
        jdbcTemplate.update("DELETE FROM group_weekly_schedules");
        jdbcTemplate.update("DELETE FROM group_members");
        jdbcTemplate.update("DELETE FROM class_groups");
        jdbcTemplate.update("DELETE FROM users");
    }
}