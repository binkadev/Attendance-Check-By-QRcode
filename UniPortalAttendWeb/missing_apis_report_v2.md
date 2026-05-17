# Báo cáo các API & Trường dữ liệu còn thiếu (Class Management)

Dựa trên quá trình rà soát toàn bộ mã nguồn Frontend (các màn hình trong `src/features/classes/components`), dưới đây là danh sách chi tiết các API, Params và các trường dữ liệu (Fields) còn thiếu hoặc cần bổ sung từ phía Backend để hệ thống hoạt động hoàn chỉnh với dữ liệu thật.

---

## 1. Màn hình Chi tiết lớp học & QR Động (`ClassDetail.jsx`, `DynamicQRTab.jsx`)

### 1.1 API Cập nhật: `GET /api/v1/groups/{groupId}`
Hiện tại màn hình máy chiếu (Projector) và màn hình thông tin lớp đang phải hardcode một số thông tin quan trọng vì API chưa trả về hoặc trả về thiếu.
**Các trường cần bổ sung trong Response:**
- `room` (String): Phòng học (VD: "2E03").
- `semester` (String): Học kỳ (VD: "HK1" hoặc "Kỳ Thu").
- `academicYear` (String): Năm học (VD: "2025-2026").
- `schedule` / `weeklySchedules` (Array/String): Lịch học cụ thể trong tuần.
  - Định dạng lý tưởng (Array): `[ { "dayOfWeek": "MONDAY", "startTime": "17:00", "endTime": "22:30" } ]`.

### 1.2 API Mới: Gia hạn thời gian điểm danh
Giảng viên có nút **"Gia hạn thêm (+10p)"** khi phiên điểm danh đang chạy.
- **Endpoint:** `POST /api/v1/sessions/{sessionId}/extend`
- **Request Body:**
  ```json
  {
    "extendMinutes": 10
  }
  ```
- **Hành vi mong muốn:** Cộng thêm `extendMinutes` vào trường `checkinCloseAt` của phiên hiện tại.

---

## 2. Màn hình Quản lý Sinh viên (`StudentsTab.jsx`)

### 2.1 API Cập nhật: `GET /api/v1/groups/{groupId}/members`
Hiện tại Frontend đang lấy toàn bộ danh sách sinh viên về và tự thực hiện filter, search, phân trang (pagination) ở phía Client. Điều này không tối ưu khi lớp đông.
**Yêu cầu cải tiến API:**
- **Query Params cần hỗ trợ:**
  - `page`, `size`: Dùng cho phân trang (Pagination).
  - `q` (String): Tìm kiếm theo tên hoặc mã sinh viên (MSSV).
  - `memberStatus` (String): Lọc theo trạng thái `ACTIVE`, `PENDING`, `INACTIVE`.
  - `attendanceStatus` (String): Lọc theo mức độ điểm danh (Tốt, Nghi vấn, Nguy cơ).
- **Các trường bổ sung trong Item của Response:**
  - `avatarUrl` (String): Ảnh đại diện của sinh viên (hiện tại đang bị null).
  - `attendanceRate` (Number): Tỷ lệ đi học (0-100) trả về kèm theo từng member luôn (thay vì phải gọi riêng lẻ API policy).

---

## 3. Màn hình Cảnh báo Gian lận (`FraudTab.jsx`)

### 3.1 API Cập nhật: `GET /api/v1/groups/{groupId}/fraud-incidents`
Màn hình đã được thiết kế sẵn logic nhận dữ liệu nhưng dữ liệu thật trả về đang bị lỗi hoặc thiếu cấu trúc hiển thị cần thiết.
**Query Params cần hỗ trợ:**
- `page`, `size`, `sortDir`.
- `q` (String): Tìm kiếm theo ID hoặc Tên sinh viên vi phạm.
**Cấu trúc Response Item mong muốn (để khớp với UI hiển thị):**
```json
{
  "id": "incident-uuid",
  "type": "REPEATED_FAILED_QR_TOKEN", // Mã loại sự cố
  "title": "Quét QR thất bại liên tục",
  "description": "Phát hiện 3 lần quét trong 4 giây từ cùng một IP",
  "student": {
    "id": "STU-1198",
    "name": "Trần Đăng Khoa",
    "avatarUrl": "..."
  },
  "severity": "HIGH", // LOW, MEDIUM, HIGH, CRITICAL
  "confidence": 92, // % Độ tin cậy của thuật toán
  "status": "PENDING", // PENDING, RESOLVED
  "lastDetectedAt": "2023-10-24T10:08:00Z"
}
```

### 3.2 API Mới: Xử lý sự cố hàng loạt (Bulk Resolve)
Giảng viên có thể check chọn nhiều sự cố cùng lúc và nhấn nút **"Xử lý hàng loạt"**.
- **Endpoint:** `POST /api/v1/groups/{groupId}/fraud-incidents/bulk-resolve`
- **Request Body:**
  ```json
  {
    "incidentIds": ["id-1", "id-2"],
    "action": "RESOLVE",
    "note": "Đã xử lý hàng loạt từ hệ thống"
  }
  ```

---

## 4. Màn hình Đơn xin phép (`AbsenceTab.jsx`)

### 4.1 API Cập nhật: `GET /api/v1/groups/{groupId}/absence-requests`
API này hiện tại đang lấy dữ liệu khá tốt nhưng thiếu thông tin chi tiết về sinh viên (hiện Frontend đang phải tự gọi thêm API `members` để map tên sinh viên vào đơn xin phép).
**Yêu cầu cải tiến API:**
- **Join thông tin sinh viên vào Response:** Trả về đối tượng `student` (chứa `fullName`, `userCode`, `avatarUrl`) ngay bên trong mỗi request item.
- **Trạng thái nộp (Policy Status):** Trả về cờ kiểm tra nộp trễ hay đúng hạn (Ví dụ: `isLateSubmission: boolean` hoặc `policyStatus: "on_time" | "late"`). Frontend hiện phải hardcode là `on_time`.

---

**Tài liệu này được xuất bản để đội ngũ Backend tham chiếu và tiến hành cập nhật/phát triển bổ sung nhằm hoàn thiện tính năng.**
