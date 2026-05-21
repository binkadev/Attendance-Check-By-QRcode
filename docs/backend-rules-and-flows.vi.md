# Backend Rules & Flows — Attendance Check By QR Code

> **Mục đích**  
> Tài liệu này là nơi tổng hợp chuẩn các **business rules**, **API flows**, và ranh giới triển khai của Backend trong project **Attendance Check By QR Code**.

---

## 0. Chiến lược đặt tài liệu

### 0.1. Giữ `README.md` cho portfolio

`README.md` nên giữ vai trò giới thiệu project cho recruiter, mentor, giảng viên hoặc người review GitHub. Nội dung nên tập trung vào:

- Project làm gì.
- Tính năng chính.
- Tech stack.
- Kiến trúc tổng quan.
- Cách chạy project.
- CI/testing.
- Screenshot/demo.

Không nên đưa toàn bộ business rules chi tiết vào `README.md`, vì file sẽ quá dài và khó đọc.

### 0.2. Đặt rules chi tiết trong `docs/backend-rules-and-flows.vi.md`

File này nên chứa:

- Authentication rules.
- Account lifecycle rules.
- Class/group rules.
- Member import rules.
- QR check-in rules.
- Fraud detection rules.
- Attendance summary rules.
- API response rules.
- Testing/finalization checklist.

### 0.3. Cấu trúc `docs/` đề xuất

```text
docs/
  backend-rules-and-flows.vi.md
  api/
    fraud-incident-api.md
    member-import-api.md
    attendance-api.md
  architecture/
    system-architecture.png
  screenshots/
    ...
```

### 0.4. Có nên commit lên GitHub không?

Có. Nên commit tài liệu này lên GitHub vì nó giải thích **vì sao Backend xử lý như vậy**, không chỉ là code.

Tài liệu này hữu ích cho:

- Review portfolio.
- Bảo trì project sau này.
- Đồng bộ Frontend/Backend.
- Chuẩn bị phỏng vấn.
- Tránh sửa code làm lệch rule cũ.

Commit message đề xuất:

```bash
git add docs/backend-rules-and-flows.vi.md
git commit -m "Document backend business rules and flows"
```

---

## 1. Project-level rules

### 1.1. Backend là source of truth

Backend là nơi quyết định tất cả rule quan trọng.

Frontend có thể hiển thị UI, nhưng Backend bắt buộc phải enforce:

- Authentication.
- Authorization.
- Class membership.
- QR validity.
- Time windows.
- Attendance status.
- Fraud marking.
- Attendance summary.
- Password change requirement.
- Manual override rules.

### 1.2. Nguyên tắc ổn định API

Không nên đổi route API nếu không thật sự cần.

Ưu tiên:

```text
Giữ route ổn định
Cải thiện request/response DTO
Thêm optional fields
Chỉ thêm endpoint mới khi nghiệp vụ thật sự mới
```

Tránh:

```text
Đổi tên route đang dùng
Đổi enum value mà không có migration plan
Xóa field Frontend đang dùng mà chưa thống nhất
```

### 1.3. Nguyên tắc Transaction

Các nghiệp vụ ghi nhiều bảng phải chạy trong transaction.

Ví dụ:

- Import members.
- QR check-in.
- Manual attendance correction.
- Password reset/change.
- Fraud incident action.
- Session open/close/cancel.

Nếu một bước ghi DB bắt buộc bị lỗi, toàn bộ nghiệp vụ phải rollback.

### 1.4. Nguyên tắc auditability

Các thay đổi trạng thái quan trọng phải có dấu vết kiểm tra.

Ví dụ:

- Attendance check-in nên tạo `attendance_event` và `checkin_attempt_log`.
- Manual attendance correction nên tạo `attendance_event`.
- Fraud incident action nên lưu actor và resolution note.
- Login/password reset attempts nên được log.
- Member import nên trả kết quả theo từng dòng.

---

## 2. Authentication và account lifecycle

### 2.1. Normal login

Endpoint:

```http
POST /api/v1/auth/login
```

Flow hiện tại:

1. Normalize email.
2. Kiểm tra login rate limit.
3. Tìm active user.
4. Validate password.
5. Ghi login attempt.
6. Issue access token và refresh token.
7. Lưu `user_session`.
8. Trả `LoginResponse`.

### 2.2. Register

Endpoint:

```http
POST /api/v1/auth/register
```

Rules:

1. Email bắt buộc và được normalize lowercase.
2. Password phải pass `PasswordPolicyService`.
3. Full name bắt buộc.
4. Email phải unique.
5. User code nếu có phải unique.
6. User tự register bình thường có status `ACTIVE`.
7. User tự register bình thường không cần `requirePasswordChange`.
8. Backend tạo refresh session và trả token.

### 2.3. Forgot/reset password

Endpoints:

```http
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

Rules:

1. Response của forgot password nên trung lập để tránh lộ email có tồn tại hay không.
2. Reset token nên là opaque token và chỉ lưu hash trong DB.
3. Active reset token cũ của cùng user nên bị supersede.
4. Token có thể expired, revoked hoặc used.
5. New password phải pass password policy.
6. Sau khi reset thành công:
   - update password,
   - mark token used,
   - revoke active sessions.

### 2.4. Change password khi đã login

Endpoint:

```http
POST /api/v1/auth/change-password
```

Rules:

1. User phải authenticated.
2. Current password phải đúng.
3. New password phải khác current password.
4. New password phải pass password policy.
5. Password phải hash trước khi lưu.
6. Active sessions hiện tại nên bị revoke.

### 2.5. Force password change cho account được import

Áp dụng cho account được tạo tự động khi import danh sách sinh viên.

Field đề xuất:

```text
users.require_password_change
```

Rules:

1. Auto-provisioned user account phải có `requirePasswordChange = true`.
2. User có `requirePasswordChange = true` không được vào hệ thống đầy đủ sau normal login.
3. Backend nên trả response/error đặc biệt:
   - status đề xuất: `428 Precondition Required`
   - code: `REQUIRE_PASSWORD_CHANGE`
4. User phải đổi password trước khi vào dashboard/app.
5. Sau khi đổi password:
   - password được update,
   - `requirePasswordChange = false`,
   - có thể issue official tokens.

Endpoint đề xuất cho Phase 1.5:

```http
POST /api/v1/auth/force-change-password
```

Request:

```json
{
  "email": "student@example.com",
  "currentPassword": "N22DCCN160phamvanphu",
  "newPassword": "NewStrongPassword123!",
  "deviceId": "PHONE-ABC-123"
}
```

Response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "sessionId": "..."
}
```

### 2.6. Long-term professional activation flow

Về lâu dài, không nên phụ thuộc default password. Nên chuyển sang email activation.

Flow:

```text
Teacher imports roster
  ↓
Backend creates INVITED account if user does not exist
  ↓
Backend creates activation token
  ↓
Backend queues activation email
  ↓
Student clicks activation link
  ↓
Student sets password
  ↓
Account becomes ACTIVE
```

Endpoint đề xuất:

```http
POST /api/v1/auth/invitations/accept
```

Request:

```json
{
  "token": "activation-token",
  "newPassword": "NewStrongPassword123!"
}
```

---

## 3. Class / group model

### 3.1. Các khái niệm chính

```text
ClassGroup
  = lớp học / course container.

GroupMember
  = quan hệ giữa class/group và user.

User
  = login identity.

AttendanceSession
  = phiên điểm danh cụ thể.

SessionAttendance
  = attendance row của một user trong một session.
```

### 3.2. `group_members` là source of truth cho class access

Một student thuộc lớp khi có row trong `group_members`:

```text
group_id = target group
user_id = authenticated user
member_status = APPROVED
```

Nếu row này không tồn tại, user không được:

- xem private class data,
- scan QR cho class đó,
- xuất hiện trong attendance summary của class,
- xem attendance history của class.

### 3.3. Roles

Ý nghĩa role đề xuất:

```text
OWNER
  Toàn quyền quản lý class.

CO_HOST
  Hỗ trợ các workflow class/session/student, trừ owner-only actions.

MEMBER
  Student bình thường.
```

### 3.4. Member statuses

Ý nghĩa status đề xuất:

```text
APPROVED
  Thành viên chính thức đang active trong class.

PENDING
  Đang chờ duyệt join request.

REJECTED
  Join request bị từ chối.

REMOVED
  Thành viên cũ đã bị remove khỏi class.
```

---

## 4. Member import / roster-first flow

### 4.1. Business goal

Member import trả lời câu hỏi thực tế:

> Lecturer có file Excel danh sách sinh viên chính thức. Backend làm sao biết sinh viên nào thuộc lớp, kể cả khi một số sinh viên chưa tạo account?

Câu trả lời:

```text
Import list
  ↓
Match hoặc create user accounts
  ↓
Insert group_members
  ↓
Class membership trở thành official trong Backend
```

### 4.2. Professional model đề xuất

Dùng:

```text
Roster-first + Account linking
```

Nghĩa là:

- Imported roster là danh sách lớp chính thức.
- Backend link row import với user đã có nếu match được.
- Backend tạo account cho user chưa có nếu provisioning mode cho phép.
- Membership chính thức được lưu trong `group_members`.

### 4.3. Endpoint

```http
POST /api/v1/groups/{groupId}/members/import
Content-Type: application/json
```

### 4.4. Request body

Nên dùng wrapper object, không dùng raw array, vì wrapper dễ mở rộng sau này.

```json
{
  "importMode": "VALIDATE_AND_IMPORT",
  "syncMode": "APPEND_ONLY",
  "accountProvisioningMode": "CREATE_REQUIRE_PASSWORD_CHANGE",
  "notifyStudents": false,
  "members": [
    {
      "rowIndex": 2,
      "studentCode": "N22DCCN160",
      "fullName": "Phạm Văn Phú",
      "email": "phu.pvn22@ptit.edu.vn"
    }
  ]
}
```

### 4.5. Ý nghĩa request fields

#### `importMode`

```text
VALIDATE_ONLY
  Chỉ validate rows và trả errors/results.
  Không ghi DB.

VALIDATE_AND_IMPORT
  Validate toàn bộ rows.
  Nếu hợp lệ thì ghi DB.
```

#### `syncMode`

```text
APPEND_ONLY
  Add/link students trong file.
  Không remove members không có trong file.
  Nên dùng cho phase hiện tại.

SYNC_EXACT
  File là full roster chính thức.
  Student không còn trong file có thể bị mark removed.
  Chỉ nên làm ở future phase.
```

#### `accountProvisioningMode`

```text
LINK_EXISTING_ONLY
  Chỉ link users đã tồn tại.
  User thiếu thì báo validation error.

CREATE_REQUIRE_PASSWORD_CHANGE
  Nếu user chưa có, tạo user với default password và bắt đổi password.
  Phù hợp Phase 1.5.

INVITE_BY_EMAIL
  Nếu user chưa có, tạo invited account và gửi activation link.
  Professional option tốt nhất về lâu dài.
```

#### `notifyStudents`

```text
false
  Không gửi email.

true
  Queue/send invitation hoặc notification email tùy provisioning mode.
```

#### `members[].rowIndex`

Số dòng gốc trong Excel, dùng để trả lỗi chính xác cho FE.

#### `members[].studentCode`

Mã sinh viên. Đây là field match chính.

#### `members[].fullName`

Tên hiển thị của sinh viên.

#### `members[].email`

Email sinh viên. Dùng để match account và gửi invitation.

### 4.6. Validation rules

Trước khi ghi DB:

1. Request body không được null.
2. `members` không được rỗng.
3. Mỗi row phải có:
   - `studentCode`,
   - `fullName`,
   - `email`.
4. Email phải đúng format.
5. Normalize:
   - `studentCode`: trim + uppercase.
   - `email`: trim + lowercase.
   - `fullName`: trim + gộp khoảng trắng.
6. Check duplicate trong chính file import:
   - trùng `studentCode` = reject,
   - trùng `email` = reject.
7. Nếu bất kỳ row nào invalid, reject toàn bộ import.
8. Không partial import các row hợp lệ.

### 4.7. User matching rules

Với mỗi normalized row:

1. Tìm user bằng `studentCode` / `user_code`.
2. Tìm user bằng normalized email.
3. Nếu cả hai cùng match một user, dùng user đó.
4. Nếu chỉ `studentCode` match, dùng user đó.
5. Nếu chỉ email match, dùng user đó.
6. Nếu `studentCode` match User A nhưng email match User B, reject toàn bộ import với conflict.
7. Nếu không match user nào:
   - create user nếu provisioning mode cho phép,
   - nếu không thì reject row.

### 4.8. Account creation rule

Với `CREATE_REQUIRE_PASSWORD_CHANGE`:

Create user:

```text
id = new UUID
platform_role = USER
email = normalized email
user_code = normalized studentCode
full_name = normalized fullName
status = ACTIVE
require_password_change = true
password_hash = BCrypt(default password)
```

Default password rule:

```text
studentCode + unaccented lowercase no-space fullName
```

Ví dụ:

```text
studentCode = N22DCCN160
fullName = Phạm Văn Phú
defaultPassword = N22DCCN160phamvanphu
```

Quan trọng:

- Không bao giờ lưu plain default password.
- Chỉ lưu BCrypt hash.
- Không nên trả raw password trong response, trừ khi cố ý phục vụ demo có kiểm soát.
- Nên trả `defaultPasswordRule` thay vì password cụ thể.

### 4.9. Group membership insertion rule

Sau khi user được match hoặc create:

1. Nếu chưa có membership:
   - insert `GroupMember` mới.
   - role = `MEMBER`.
   - status = `APPROVED`.
   - joinedAt = now.
   - invitedBy = actor user id nếu support.
2. Nếu membership đã tồn tại và là `APPROVED`:
   - skip as already member.
3. Nếu membership đã tồn tại và là `PENDING`, `REJECTED`, hoặc `REMOVED`:
   - restore/update về `APPROVED`.
   - role = `MEMBER`.
   - removedAt = null.
   - joinedAt = now.

### 4.10. Import response

Success:

```json
{
  "groupId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "totalRows": 2,
  "createdUsers": 1,
  "linkedExistingUsers": 1,
  "addedMembers": 2,
  "skippedExistingMembers": 0,
  "restoredMembers": 0,
  "invitationEmailsQueued": 0,
  "items": [
    {
      "rowIndex": 2,
      "studentCode": "N22DCCN160",
      "fullName": "Phạm Văn Phú",
      "email": "phu.pvn22@ptit.edu.vn",
      "userId": "11111111-1111-1111-1111-111111111111",
      "memberStatus": "APPROVED",
      "accountStatus": "ACTIVE",
      "action": "CREATED_USER_REQUIRE_PASSWORD_CHANGE_AND_ADDED"
    }
  ]
}
```

Validation error:

```json
{
  "status": 400,
  "code": "MEMBER_IMPORT_VALIDATION_FAILED",
  "message": "Danh sách import có dữ liệu không hợp lệ.",
  "errors": [
    {
      "rowIndex": 4,
      "field": "email",
      "code": "INVALID_EMAIL",
      "message": "Email không đúng định dạng."
    }
  ]
}
```

### 4.11. Vì sao flow này tốt?

Flow này hỗ trợ:

- Student đã có account.
- Student chưa có account.
- Teacher import danh sách lớp chính thức.
- Attendance summary chính xác.
- Absent list chính xác.
- Không tạo duplicate user.
- Có đường nâng cấp lên invitation email.
- Có đường nâng cấp lên SSO linking.

---

## 5. Attendance session flow

### 5.1. Session lifecycle

States đề xuất:

```text
OPEN
CLOSED
CANCELLED
```

Rules:

1. Chỉ authorized teacher/owner/co-host được open session.
2. Một session thuộc một group.
3. Một session có:
   - startAt,
   - endAt,
   - checkinOpenAt,
   - checkinCloseAt,
   - lateAfterMinutes,
   - qrRotateSeconds.
4. Chỉ `OPEN` session nhận QR check-in.
5. Session đã closed/cancelled không nhận check-in.

### 5.2. Open session

Endpoint ví dụ:

```http
POST /api/v1/groups/{groupId}/sessions
```

Rules:

1. Actor phải có quyền manage group.
2. Group phải active.
3. Time window của session phải hợp lệ.
4. Session được tạo với trạng thái `OPEN`.
5. Attendance rows có thể được init trước hoặc lazy create tùy design hiện tại.

### 5.3. Close session

Endpoint ví dụ:

```http
POST /api/v1/sessions/{sessionId}/close
```

Rules:

1. Actor phải có quyền manage group.
2. Session phải là `OPEN`.
3. Session chuyển sang `CLOSED`.
4. Missing approved members có thể bị mark `ABSENT` tùy service rule.
5. Events nên được audit.

### 5.4. Cancel session

Rules:

1. Actor phải có quyền manage group.
2. Cancelled session không được tính như session điểm danh bình thường.
3. Cancelled session không xuất hiện như open check-in target.
4. QR tokens liên quan không còn usable.

---

## 6. QR check-in rules

### 6.1. Endpoint

```http
POST /api/v1/sessions/{sessionId}/checkin/qr
```

### 6.2. Request

```json
{
  "token": "tokenId.secret",
  "deviceId": "PHONE-ABC-12345678",
  "geoLat": 10.8451234,
  "geoLng": 106.7945678
}
```

### 6.3. Hard validation rules

Backend phải reject khi:

```text
Missing token                 -> QR_TOKEN_REQUIRED
Invalid token format           -> QR_TOKEN_INVALID_FORMAT
Token not found/hash mismatch  -> QR_TOKEN_INVALID
Wrong session token            -> QR_TOKEN_NOT_FOR_SESSION
Token revoked                  -> QR_TOKEN_REVOKED
Token expired                  -> QR_TOKEN_EXPIRED
Session not found              -> SESSION_NOT_FOUND
Session not OPEN               -> SESSION_NOT_OPEN
User not approved member       -> NOT_A_GROUP_MEMBER
Before checkinOpenAt           -> CHECKIN_NOT_OPEN_YET
After checkinCloseAt           -> CHECKIN_CLOSED
Already EXCUSED                -> ATTENDANCE_ALREADY_EXCUSED
Missing deviceId               -> DEVICE_ID_REQUIRED
Invalid deviceId               -> DEVICE_ID_INVALID
Missing geo when required      -> GEO_LOCATION_REQUIRED
Invalid geo                    -> GEO_LOCATION_INVALID
Out of allowed radius          -> CHECKIN_OUT_OF_RANGE
```

### 6.4. Attendance status calculation

```text
now <= lateThreshold
  -> PRESENT

lateThreshold < now <= checkinCloseAt
  -> LATE
```

### 6.5. Idempotency

Nếu user đã check-in thành công cho cùng session:

1. Không tạo attendance record mới.
2. Trả lại check-in result hiện có.
3. Tránh duplicate attendance event.

### 6.6. EXCUSED và ABSENT

Rules:

1. `EXCUSED` không bị QR check-in overwrite.
2. `ABSENT` có thể đổi thành `PRESENT` hoặc `LATE` nếu session vẫn open và policy cho phép.
3. Manual rules tách biệt với QR rules.

---

## 7. Device fraud rules

### 7.1. Device ID requirement

QR check-in bắt buộc có stable device ID.

Device ID rules:

```text
required
length 8-120
allowed characters: A-Z, a-z, 0-9, ".", "_", ":", "-"
```

### 7.2. Stable device identity

Mobile phải generate device ID một lần theo thiết bị vật lý.

```text
User A logs in on phone X -> deviceId = PHONE-X
User A logs out
User B logs in on same phone X -> deviceId remains PHONE-X
```

Nếu device ID đổi theo từng user, shared-device detection sẽ không hoạt động.

### 7.3. Shared device detection

Nếu cùng session và cùng device ID được dùng bởi nhiều user khác nhau:

1. Vẫn cho check-in thành công.
2. Mark current row là suspicious.
3. Mark các related rows cùng session/device là suspicious.
4. Set:
   - `suspicious_flag = true`
   - `suspicious_reason = SHARED_DEVICE_MULTI_ACCOUNT`
5. Create hoặc update `fraud_incidents` với type:
   - `SHARED_DEVICE_MULTI_ACCOUNT`

### 7.4. Vì sao không block shared device?

Không hard-block shared device vì có thể có case hợp lệ:

- Student mượn điện thoại bạn.
- Điện thoại hết pin.
- Teacher cho phép dùng tạm trong tình huống khẩn cấp.

Nhưng hệ thống phải flag để teacher/admin review.

---

## 8. IP fraud rules

### 8.1. IP source

Backend phải lấy IP từ `HttpServletRequest`.

Không tin IP gửi từ request body.

Priority đề xuất:

```text
X-Forwarded-For first IP
X-Real-IP
request.getRemoteAddr()
```

### 8.2. IP burst incident

Nếu nhiều user check-in từ cùng IP trong thời gian ngắn, create/update:

```text
IP_BURST_MULTI_ATTEMPT
```

Không hard-block chỉ vì IP, vì sinh viên có thể cùng dùng campus Wi-Fi.

### 8.3. Severity examples

```text
LOW:
same session + same IP + >= 5 users within 3 minutes

MEDIUM:
same session + same IP + same userAgent + >= 3 users within 60 seconds

HIGH:
same session + same IP + >= 10 users within 5 minutes
```

---

## 9. Location rules

### 9.1. Location policy fields

Fields đề xuất trong attendance policy:

```text
require_location
location_lat
location_lng
allowed_radius_meter
```

### 9.2. General rules

1. Mobile có thể gửi `geoLat` và `geoLng`.
2. Nếu chỉ có một trong lat/lng thì reject.
3. Latitude phải trong khoảng -90 đến 90.
4. Longitude phải trong khoảng -180 đến 180.
5. Backend phải tự calculate distance.
6. Backend không được tin distance từ client.

### 9.3. Nếu `require_location = false`

Rules:

1. Không bắt location.
2. Nếu mobile gửi location thì vẫn lưu.
3. Không reject out-of-range.

### 9.4. Nếu `require_location = true`

Rules:

1. Missing lat/lng -> `GEO_LOCATION_REQUIRED`.
2. Invalid lat/lng -> `GEO_LOCATION_INVALID`.
3. Distance > allowed radius -> `CHECKIN_OUT_OF_RANGE`.
4. Out-of-range attempt phải được log.
5. Repeated out-of-range nên create/update:
   - `REPEATED_OUT_OF_RANGE`.

---

## 10. Attempt log rules

### 10.1. Luôn log QR attempts

Backend nên log:

```text
Mọi QR check-in thành công
Mọi QR check-in thất bại
```

### 10.2. Attempt log nên có

```text
groupId
sessionId
userId if known
qrTokenId if known
tokenHash if available
deviceId
ipAddress
userAgent
geoLat
geoLng
distanceMeter
outcome
failureCode
payloadJson
createdAt
```

### 10.3. Vì sao attempt logs quan trọng?

Attempt logs phục vụ:

- Fraud incident detection.
- Security auditing.
- Debug lỗi mobile check-in.
- Giải thích vì sao student không check-in được.

---

## 11. Fraud incident API rules

### 11.1. Routes

Giữ route hiện tại:

```http
GET /api/v1/groups/{groupId}/fraud-incidents
GET /api/v1/groups/{groupId}/fraud-incidents/{incidentId}
PATCH /api/v1/groups/{groupId}/fraud-incidents/{incidentId}
```

Không đổi sang `/classes` trừ khi toàn bộ API convention đổi.

### 11.2. Top-level response

Fraud incident response nên có:

```text
id
groupId
sessionId
userId
type
severity
status
displayStatus
title
description
confidence
student
firstDetectedAt
lastDetectedAt
occurrenceCount
evidenceSummary
assignedToUserId
lastActionByUserId
resolvedAt
resolutionNote
createdAt
updatedAt
```

### 11.3. Status lifecycle

Giữ backend enum:

```text
OPEN
ACKNOWLEDGED
RESOLVED
FALSE_POSITIVE
```

UI có thể map:

```text
OPEN -> Đang chờ / PENDING
ACKNOWLEDGED -> Đã ghi nhận
RESOLVED -> Đã xử lý
FALSE_POSITIVE -> Báo sai
```

### 11.4. Evidence summary compact contract

Tránh DTO kiểu "kitchen sink".

Compact DTO đề xuất:

```json
{
  "occurrenceCount": 1,
  "reason": "Phát hiện một thiết bị điểm danh cho nhiều tài khoản",
  "deviceId": "PHONE-ABC",
  "ipAddress": "116.102.13.44",
  "userAgent": "Mozilla/5.0",
  "distinctUserCount": 2,
  "involvedUserIds": ["..."],
  "involvedUsers": [
    {
      "id": "...",
      "studentCode": "N22DCCN160",
      "name": "Nguyễn Văn A",
      "avatar": null
    }
  ],
  "distanceMeter": 250,
  "allowedRadiusMeter": 50,
  "location": {
    "lat": 21.037,
    "lng": 105.783
  },
  "lastFailureCode": "TOKEN_INVALID"
}
```

### 11.5. Fields không nên expose trong response

Không expose internal/duplicated fields:

```text
threshold
ruleWindowSeconds
distinctDeviceCount
distinctIpCount
maxDistanceMeter
sampleAttemptIds
sampleUserIds
sampleIpAddresses
sampleDeviceIds
notes
geoLat
geoLng
location as string
otherUserIds
```

### 11.6. Shared device response example

```json
{
  "type": "SHARED_DEVICE_MULTI_ACCOUNT",
  "title": "Trùng ID thiết bị",
  "description": "Thiết bị PHONE-ABC được dùng để điểm danh cho 2 sinh viên khác nhau",
  "confidence": 98,
  "evidenceSummary": {
    "occurrenceCount": 1,
    "reason": "Phát hiện một thiết bị điểm danh cho nhiều tài khoản",
    "deviceId": "PHONE-ABC",
    "distinctUserCount": 2,
    "involvedUserIds": ["...", "..."],
    "involvedUsers": []
  }
}
```

### 11.7. Out-of-range response example

```json
{
  "type": "REPEATED_OUT_OF_RANGE",
  "title": "Điểm danh ngoài phạm vi",
  "description": "Vị trí điểm danh cách lớp học 250m, vượt bán kính cho phép 50m",
  "confidence": 95,
  "evidenceSummary": {
    "occurrenceCount": 3,
    "reason": "Vị trí điểm danh nằm ngoài bán kính cho phép nhiều lần",
    "distanceMeter": 250,
    "allowedRadiusMeter": 50,
    "location": {
      "lat": 21.037,
      "lng": 105.783
    }
  }
}
```

---

## 12. Fraud incident action rules

### 12.1. Actions

Actions đề xuất:

```text
ACKNOWLEDGE
RESOLVE
FALSE_POSITIVE
```

### 12.2. Transition rules

```text
OPEN
  -> ACKNOWLEDGE
  -> RESOLVE
  -> FALSE_POSITIVE

ACKNOWLEDGED
  -> RESOLVE
  -> FALSE_POSITIVE

RESOLVED
  no manual transition

FALSE_POSITIVE
  no manual transition
```

### 12.3. Reopen rule

Nếu cùng dedup key xuất hiện lại sau `RESOLVED` hoặc `FALSE_POSITIVE`:

1. status chuyển về `OPEN`,
2. resolvedAt bị clear,
3. resolutionNote bị clear,
4. occurrenceCount tăng,
5. evidence được update.

---

## 13. Upcoming sessions timeline rules

### 13.1. Goal

Mobile home screen cần sectioned timeline:

```text
TODAY
UPCOMING
```

### 13.2. Response shape

```json
{
  "sections": [
    {
      "key": "TODAY",
      "title": "Hôm nay",
      "date": "2026-05-06",
      "items": []
    },
    {
      "key": "UPCOMING",
      "title": "Sắp tới",
      "items": []
    }
  ]
}
```

### 13.3. Item fields

Mỗi item nên có:

```text
groupId
groupName
room
lecturerName
sessionName
attendanceSessionId
attendanceStatus
checkinOpenAt
checkinCloseAt
sessionDate
startTime
endTime
startAt
endAt
```

### 13.4. Classification rules

```text
sessionDate == local today
  -> TODAY

sessionDate > local today
  -> UPCOMING
```

Rules:

1. Giữ item hôm nay kể cả khi start time đã qua.
2. Chỉ approved memberships được thấy.
3. Sort theo date và start time.
4. Attach real attendance session metadata nếu session đã tồn tại.
5. Planned sessions có thể generate từ group schedules.

---

## 14. Attendance summary by term rules

### 14.1. Endpoint

Giữ route:

```http
GET /api/v1/me/attendance/summary?semester=HK2&academicYear=2025-2026
```

### 14.2. Rule

Summary tổng hợp tất cả classes trong semester/academicYear được request cho current student.

### 14.3. Tránh duplicate route

Không thêm duplicate summary endpoint trong attendance read controller nếu stats module đang own phần này.

Controller nên accept optional:

```text
semester
academicYear
```

### 14.4. Mục đích response

API này phục vụ home/dashboard cards:

```text
total sessions
present count
late count
absent count
excused count
attendance rate
risk/warning if applicable
```

---

## 15. Manual attendance rules

### 15.1. Manual mark

Manual attendance là administrative action.

Rules:

1. Actor phải có quyền manage group/session.
2. Manual mark nên có note/reason.
3. Manual mark không tạo QR fraud device/IP/location incidents.
4. Manual action nên tạo attendance event.

### 15.2. Reset attendance

Reset nên clear:

```text
checkInAt
qrTokenId
deviceId
ipAddress
userAgent
geoLat
geoLng
distanceMeter
suspiciousFlag
suspiciousReason
```

---

## 16. Absence request rules

Absence lifecycle đề xuất:

```text
SUBMITTED
APPROVED
REJECTED
CANCELLED
```

Rules:

1. Student submit absence request.
2. Teacher/owner review.
3. Approved absence map attendance thành `EXCUSED`.
4. Rejected absence không đổi attendance thành excused.
5. Cancelled request không còn active.
6. Transitions nên được audit.

---

## 17. Notification và email rules

### 17.1. Email outbox pattern

Không nên gửi important emails trực tiếp trong business transaction nếu có thể tránh.

Nên dùng:

```text
Business transaction
  -> insert email_outbox row
  -> worker sends email later
```

Cách này an toàn hơn vì SMTP fail không làm hỏng domain writes chính.

### 17.2. Email types

Email types hiện tại hoặc tương lai:

```text
password reset
student invitation / activation
notification email
```

### 17.3. Student invitation email

Với member import professional phase:

```text
Teacher imports roster
  -> create invited account
  -> create activation token
  -> enqueue invitation email
```

Email nên hiển thị:

```text
Class name
Student email
Student code
Activation link
Expiration time
```

---

## 18. OpenAPI rules

### 18.1. OpenAPI phải khớp Backend

Mọi endpoint mới hoặc response field mới phải update trong `openapi.yaml`.

### 18.2. API docs nên có

Mỗi endpoint cần:

```text
path
method
auth requirement
request body
response body
error codes
business notes
```

### 18.3. Tránh stale docs

Nếu implementation đổi:

```text
DTO changed
enum changed
new request field
new response field
new error code
```

thì update OpenAPI trong cùng commit.

---

## 19. Testing rules

### 19.1. Lệnh local bắt buộc

Trước khi commit:

```powershell
./mvnw "-DskipTests" compile
./mvnw test
```

### 19.2. Context test

Nếu thay đổi wiring lớn:

```powershell
./mvnw "-Dtest=BackendSpringbootApplicationTests" test
```

### 19.3. Integration tests nên có

Các flow quan trọng nên có tests:

```text
auth login
force password change
member import validation
member import creates user
member import links existing user
QR check-in success
QR check-in missing device
shared device fraud
location out-of-range
fraud incident response
attendance summary by term
upcoming timeline
```

### 19.4. Flyway migration rule

Không sửa migration đã apply ở shared environments.

Nếu migration đã apply:

```text
Tạo Vxx migration mới thay vì sửa file cũ.
```

Sửa migration cũ dễ gây checksum mismatch.

Với local-only dev database, checksum repair có thể chấp nhận, nhưng không nên là team workflow.

---

## 20. GitHub workflow recommendation

### 20.1. Có nên commit docs?

Có.

Repository `README.md` nên giới thiệu hệ thống production-like, còn detailed rules nên ở `docs/`.

### 20.2. Documentation commit đề xuất

```bash
git checkout -b docs/backend-rules-and-flows
mkdir -p docs
# add docs/backend-rules-and-flows.vi.md
git add docs/backend-rules-and-flows.vi.md
git commit -m "Document backend business rules and flows"
git push origin docs/backend-rules-and-flows
```

Pull Request title:

```text
Document backend business rules and flows
```

### 20.3. Thứ tự commit implementation đề xuất

Không gom mọi feature vào một commit khổng lồ.

Thứ tự tốt:

```text
1. docs: backend rules and flows
2. feat(auth): require password change for imported users
3. feat(groups): import members with account provisioning
4. feat(fraud): enrich incident dashboard response
5. test: cover member import and fraud dashboard contracts
```

### 20.4. Không nên commit gì lúc này?

Không nên commit `.docx` làm tài liệu kỹ thuật chính, trừ khi cần nộp báo cáo.

Ưu tiên Markdown:

```text
docs/backend-rules-and-flows.vi.md
```

vì Markdown:

- dễ diff,
- dễ review,
- searchable,
- đọc tốt trên GitHub.

---

## 21. Current phase recommendation

### 21.1. Phase nên làm ngay

Dùng Phase 1.5:

```text
Import members
  -> match user by studentCode/email
  -> create user if missing
  -> add group_members
  -> requirePasswordChange for created users
```

Lý do:

- Thực tế với project hiện tại.
- Đủ tốt cho demo/portfolio.
- Chưa cần invitation-token infrastructure đầy đủ.
- Làm attendance summary và class membership chính xác hơn.

### 21.2. Future professional phase

Bổ sung:

```text
group_roster_entries
invitation tokens
activation email
SSO linking
SYNC_EXACT roster sync
```

---

## 22. Final product flow summary

```text
Teacher creates class
  ↓
Teacher imports official student list
  ↓
Backend validates all rows
  ↓
Backend matches or creates user accounts
  ↓
Backend adds approved group memberships
  ↓
Student logs in or activates account
  ↓
Student sees class
  ↓
Teacher opens attendance session
  ↓
Student scans QR
  ↓
Backend validates token/time/member/device/location
  ↓
Backend records attendance and attempt log
  ↓
Backend creates fraud incident if needed
  ↓
Teacher reviews attendance/fraud/summary
```

---

## 23. Final rule summary

```text
1. Backend is source of truth.
2. group_members quyết định class membership.
3. Import roster là cách official student list đi vào hệ thống.
4. Users được match bằng studentCode trước, sau đó email.
5. Conflicting studentCode/email matches phải reject.
6. Missing users có thể auto-create với requirePasswordChange.
7. QR check-in bắt buộc stable deviceId.
8. Location chỉ enforce khi policy yêu cầu.
9. Shared device được cho qua nhưng phải suspicious.
10. IP burst là warning signal, không hard block.
11. Mọi QR attempt nên được log.
12. Fraud incident response phải compact và UI-friendly.
13. OpenAPI phải khớp implementation.
14. Các major writes phải transactional.
15. Compile và full test phải pass trước commit.
```
