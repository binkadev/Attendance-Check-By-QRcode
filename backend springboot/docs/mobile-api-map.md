# Mobile API Map — Attendance QR

This document is the final FE/backend handoff map for the mobile screens completed in Tickets 1–6.

## Current status

| Ticket | Scope | Status |
|---|---|---|
| Ticket 1 | Group schedule dates: `startDate`, `plannedEndDate`, schedule planner | Done |
| Ticket 2 | Schedule conflict validation for lecturer and room | Done |
| Ticket 3 | OpenAPI sync for schedule dates and conflict validation | Done |
| Ticket 4 | Mobile class timeline: ongoing / upcoming today / upcoming tomorrow | Done |
| Ticket 5 | Mobile-friendly history aliases | Done |
| Ticket 6 | OpenAPI sync for timeline and history aliases | Done |
| Ticket 7 | Final API map for FE handoff | This document |

## Auth rule

All endpoints below require bearer JWT unless explicitly documented otherwise in OpenAPI.

```http
Authorization: Bearer <accessToken>
```

## Screen 1 — Create / Update Class

### Create class

```http
POST /api/v1/groups
```

Use this for the create-class screen.

Important request fields:

```json
{
  "name": "Android Development",
  "code": "ANDROID-D22-01",
  "courseCode": "MOB101",
  "classCode": "D22CQCNPM02-N",
  "semester": "HK2",
  "academicYear": "2025-2026",
  "campus": "PTIT HCM",
  "room": "A101",
  "startDate": "2026-05-05",
  "approvalMode": "MANUAL",
  "allowAutoJoinOnCheckin": false,
  "weeklySchedules": [
    {
      "dayOfWeek": "TUESDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    },
    {
      "dayOfWeek": "THURSDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    }
  ],
  "totalSessions": 15,
  "maxAllowedAbsences": 3
}
```

Backend behavior:

- `startDate` is required.
- `startDate` must match at least one `weeklySchedules.dayOfWeek`.
- `plannedEndDate` is calculated by backend.
- Schedule conflict is checked before saving.
- Conflict returns `409 SCHEDULE_CONFLICT`.
- Invalid schedule data returns `422`.

### Validate schedule before submit

```http
POST /api/v1/groups/schedule/validate
```

Use this when the user selects start date, room, and weekly schedules before pressing Create/Update.

Request:

```json
{
  "excludeGroupId": null,
  "campus": "PTIT HCM",
  "room": "A101",
  "startDate": "2026-05-05",
  "weeklySchedules": [
    {
      "dayOfWeek": "TUESDAY",
      "startTime": "08:00",
      "endTime": "10:00"
    }
  ],
  "totalSessions": 15
}
```

For update form, set `excludeGroupId` to the current group id.

Response shape:

```json
{
  "valid": false,
  "conflicts": [
    {
      "type": "LECTURER",
      "groupId": "...",
      "groupName": "Android Development",
      "campus": "PTIT HCM",
      "room": "A101",
      "dayOfWeek": "TUESDAY",
      "overlapFrom": "2026-05-05",
      "overlapTo": "2026-06-30",
      "requestedStartTime": "08:00",
      "requestedEndTime": "10:00",
      "existingStartTime": "08:30",
      "existingEndTime": "10:30"
    }
  ]
}
```

Conflict types:

- `LECTURER`: same lecturer has overlapping class time.
- `ROOM`: same campus and room has overlapping class time.

### Update class

```http
PATCH /api/v1/groups/{groupId}
```

Use this for edit-class screen.

Notes:

- Partial update is supported.
- If `weeklySchedules` is sent, existing schedules are replaced.
- If `startDate`, `weeklySchedules`, or `totalSessions` changes, backend recalculates `plannedEndDate`.
- Conflict validation is also enforced on update.

### Get class detail

```http
GET /api/v1/groups/{groupId}
```

Use this to prefill class form and class detail screen header.

Important response fields:

```json
{
  "id": "...",
  "name": "Android Development",
  "courseCode": "MOB101",
  "classCode": "D22CQCNPM02-N",
  "semester": "HK2",
  "academicYear": "2025-2026",
  "campus": "PTIT HCM",
  "room": "A101",
  "startDate": "2026-05-05",
  "plannedEndDate": "2026-06-23",
  "totalSessions": 15,
  "maxAllowedAbsences": 3,
  "weeklySchedules": [],
  "studentCount": 30,
  "status": "ACTIVE"
}
```

## Screen 2 — Mobile Home / Class Timeline

### Timeline classes

```http
GET /api/v1/me/classes/timeline
```

Use this for the mobile Home/Dashboard section that shows:

- Class happening now
- Classes later today
- Classes tomorrow

Response shape:

```json
{
  "today": "2026-05-01",
  "tomorrow": "2026-05-02",
  "serverTime": "2026-05-01T10:30:00",
  "ongoing": [],
  "upcomingToday": [],
  "upcomingTomorrow": []
}
```

Each item includes:

```json
{
  "bucket": "UPCOMING_TODAY",
  "groupId": "...",
  "groupName": "Android Development",
  "roleLabel": "Sinh viên",
  "courseCode": "MOB101",
  "classCode": "D22CQCNPM02-N",
  "room": "A101",
  "campus": "PTIT HCM",
  "locationDisplay": "PTIT HCM - A101",
  "lecturerName": "Nguyen Van A",
  "approvedStudentCount": 30,
  "semester": "HK2",
  "academicYear": "2025-2026",
  "myRole": "MEMBER",
  "myMemberStatus": "APPROVED",
  "occurrenceDate": "2026-05-01",
  "dayOfWeek": "FRIDAY",
  "startAt": "2026-05-01T13:00:00",
  "endAt": "2026-05-01T15:00:00",
  "representativeSessionId": null,
  "representativeSessionStatus": null,
  "checkinOpenAt": null,
  "checkinCloseAt": null
}
```

Important behavior:

- Source of truth is planned weekly schedule, not only actual attendance sessions.
- The endpoint still works before attendance sessions are created.
- If a real attendance session exists for that date, representative session fields are included.

## Screen 3 — Class List / Search / Filter

### List my classes

```http
GET /api/v1/me/classes
```

Query params:

| Param | Example | Meaning |
|---|---|---|
| `q` | `android` | Quick search |
| `scope` | `ALL`, `TEACHING`, `JOINED` | Which classes to show |
| `status` | `ACTIVE`, `ARCHIVED` | Group status |
| `memberStatus` | `APPROVED` | Current user membership status |
| `semester` | `HK2` | Semester filter |
| `academicYear` | `2025-2026` | Academic year filter |
| `page` | `0` | Page index |
| `size` | `20` | Page size |
| `sortBy` | `updatedAt`, `createdAt`, `name`, `startAt` | Sort field |
| `sortDir` | `asc`, `desc` | Sort direction |

Common calls:

```http
GET /api/v1/me/classes?scope=ALL&page=0&size=20&sortBy=updatedAt&sortDir=desc
GET /api/v1/me/classes?scope=TEACHING&semester=HK2&academicYear=2025-2026
GET /api/v1/me/classes?scope=JOINED&q=android
```

### Teaching convenience endpoint

```http
GET /api/v1/me/classes/teaching
```

Equivalent to:

```http
GET /api/v1/me/classes?scope=TEACHING
```

### Semester dropdown

```http
GET /api/v1/me/classes/semesters
```

Use this to fill semester filter options.

## Screen 4 — Class Detail / Attendance History

### Session history of a class

```http
GET /api/v1/groups/{groupId}/sessions/history
```

Mobile-friendly alias of:

```http
GET /api/v1/groups/{groupId}/sessions
```

Query params:

| Param | Example | Meaning |
|---|---|---|
| `from` | `2026-05-01` | Start date |
| `to` | `2026-06-01` | End date |
| `status` | `OPEN`, `CLOSED`, `CANCELLED`, `DRAFT` | Session status |
| `page` | `0` | Page index |
| `size` | `20` | Page size |

Common call:

```http
GET /api/v1/groups/{groupId}/sessions/history?page=0&size=20
```

### My attendance history in a class

```http
GET /api/v1/groups/{groupId}/me/attendance-history
```

Mobile-friendly alias of:

```http
GET /api/v1/groups/{groupId}/me/attendances
```

Query params:

| Param | Example | Meaning |
|---|---|---|
| `page` | `0` | Page index |
| `size` | `20` | Page size |

Response shape:

```json
{
  "items": [
    {
      "sessionId": "...",
      "groupId": "...",
      "groupName": "Android Development",
      "sessionName": "Buổi 1",
      "sessionDate": "2026-05-01",
      "startTime": "2026-05-01T01:00:00Z",
      "endTime": "2026-05-01T03:00:00Z",
      "sessionStatus": "CLOSED",
      "attendanceStatus": "PRESENT",
      "checkInAt": "2026-05-01T01:05:00Z",
      "checkInMethod": "QR",
      "suspiciousFlag": false,
      "suspiciousReason": null
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

### My attendance policy status in a class

```http
GET /api/v1/groups/{groupId}/me/attendance-policy-status
```

Use this for student-facing class detail summary.

### Group attendance summary for lecturer

```http
GET /api/v1/groups/{groupId}/attendance/summary
```

Use this for lecturer-facing class detail summary.

## Screen 5 — Attendance Session / QR

### Create attendance session

```http
POST /api/v1/groups/{groupId}/sessions
```

### Get open session of a class

```http
GET /api/v1/groups/{groupId}/sessions/open
```

### Get session detail

```http
GET /api/v1/sessions/{sessionId}
```

### Close session

```http
POST /api/v1/sessions/{sessionId}/close
```

### Cancel session

```http
POST /api/v1/sessions/{sessionId}/cancel
```

### Get current QR token

```http
GET /api/v1/qr-tokens/current?sessionId={sessionId}
```

### Validate current QR token

```http
POST /api/v1/qr-tokens/current/validate
```

### Student check-in

```http
POST /api/v1/qr-tokens/{token}/check-in
```

## Recommended FE flow

### Create class flow

1. User fills class form.
2. FE calls `POST /groups/schedule/validate` after schedule fields are complete.
3. If `valid = false`, show conflict warning.
4. If `valid = true`, enable submit.
5. FE calls `POST /groups`.
6. Backend recalculates and revalidates again before saving.

### Mobile home flow

1. FE calls `GET /me/classes/timeline`.
2. Render `ongoing` first.
3. Render `upcomingToday` sorted by `startAt`.
4. Render `upcomingTomorrow` sorted by `startAt`.
5. If `representativeSessionId` exists and status is `OPEN`, show QR/check-in entry.

### Class detail flow

1. FE calls `GET /groups/{groupId}` for header/basic info.
2. FE calls `GET /groups/{groupId}/sessions/history` for session list.
3. If current user is student/member, FE calls `GET /groups/{groupId}/me/attendance-history`.
4. If current user is student/member, FE calls `GET /groups/{groupId}/me/attendance-policy-status`.
5. If current user is owner/co-host, FE calls `GET /groups/{groupId}/attendance/summary`.

## Final verification checklist

Before handing off to FE, verify:

- `./mvnw.cmd test` passes.
- OpenAPI loads in Swagger UI.
- `POST /groups` returns `startDate` and `plannedEndDate`.
- Conflict validate returns `valid=false` with conflict items when schedule overlaps.
- `/me/classes/timeline` returns three arrays even when empty.
- `/groups/{groupId}/sessions/history` returns same data as `/groups/{groupId}/sessions`.
- `/groups/{groupId}/me/attendance-history` returns same data as `/groups/{groupId}/me/attendances`.

## Completion note

After Tickets 1–7, the backend covers the requested mobile class creation, class listing, timeline, conflict validation, and class detail history needs.
