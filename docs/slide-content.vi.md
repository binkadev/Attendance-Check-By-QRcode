# NỘI DUNG SLIDE THUYẾT TRÌNH
## Hệ thống Điểm Danh Bằng Mã QR Động — Kiến Trúc Hướng Dịch Vụ

> **Hướng dẫn sử dụng**: Mỗi phần bên dưới tương ứng với 1 slide. Sao chép nội dung vào PowerPoint / Google Slides theo từng mục.

---

---

## ── SLIDE 1: TRANG BÌA ──

### [TIÊU ĐỀ LỚN]
# HỆ THỐNG ĐIỂM DANH BẰNG MÃ QR ĐỘNG
### *(Attendance Check By QR Code)*
#### Thiết Kế Theo Kiến Trúc Hướng Dịch Vụ (SOA)

---

**Môn học:** Kiến Trúc Phần Mềm Hướng Dịch Vụ  
**Giảng viên hướng dẫn:** *(Điền tên giảng viên)*  
**Học kỳ:** HK2 — Năm học 2025–2026

---

| STT | Họ và Tên | MSSV | Vai trò / Công việc chính | Tỷ lệ đóng góp |
|:---:|:---|:---:|:---|:---:|
| 1 | *(Tên thành viên 1)* | *(MSSV)* | Trưởng nhóm · Backend Spring Boot · Auth Service · Fraud Service | 34% |
| 2 | *(Tên thành viên 2)* | *(MSSV)* | Backend · Attendance Service · QR Token Service · Database Schema | 33% |
| 3 | *(Tên thành viên 3)* | *(MSSV)* | Frontend Web Portal (React) · Android App (Java) · Docker Compose | 33% |

---

---

## ── SLIDE 2: PHÁT BIỂU BÀI TOÁN ──

### Bối cảnh & Vấn Đề Thực Tế

**Điểm danh thủ công truyền thống đang gặp nhiều bất cập:**

| Vấn đề | Hệ quả |
|:---|:---|
| 📋 Ký danh sách giấy cuối buổi | Sinh viên ký hộ nhau dễ dàng |
| 📷 Điểm danh bằng camera giám sát | Chi phí đầu tư hạ tầng lớn |
| 📊 Theo dõi tỉ lệ chuyên cần thủ công | Mất nhiều thời gian tổng hợp Excel |
| 📧 Xử lý đơn xin nghỉ phép bằng email | Không có luồng duyệt/từ chối chuẩn hóa |

---

### Giải Pháp Đề Xuất

**Hệ thống điểm danh 3 thành phần:**
- 🖥️ **Lecturer Web Portal** — Giảng viên mở buổi học, hiển thị QR xoay động, duyệt đơn nghỉ phép
- 📱 **Student Android App** — Sinh viên quét QR bằng camera điện thoại, xem lịch sử chuyên cần
- ⚙️ **Spring Boot Backend** — Trung tâm thực thi toàn bộ luật nghiệp vụ, chống gian lận điểm danh hộ

---

---

## ── SLIDE 3: LÝ DO CHỌN KIẾN TRÚC HƯỚNG DỊCH VỤ ──

### Tại Sao Cần SOA Cho Hệ Thống Điểm Danh?

**5 lý do kỹ thuật cốt lõi:**

**1. 🔧 Nhiều Client, Một Backend Duy Nhất**
> Web Portal và Android App là 2 ứng dụng hoàn toàn khác biệt về công nghệ (React vs Native Android). SOA cho phép cả hai chia sẻ cùng một tập hợp API mà không cần viết backend riêng.

**2. ⚡ Tách biệt module theo đúng ranh giới nghiệp vụ**
> Nghiệp vụ Xác thực, Điểm danh, Gian lận và Thông báo có logic độc lập hoàn toàn — nên phải thiết kế thành các dịch vụ tách biệt.

**3. 📈 Dễ mở rộng theo chiều ngang (Scale-out)**
> Sử dụng JWT stateless + Redis Cache → Backend có thể chạy nhiều instance song song phía sau Load Balancer mà không chia sẻ trạng thái RAM.

**4. 🔒 Hợp đồng API chuẩn hóa (OpenAPI Contract)**
> Đặc tả `openapi.yaml` là hợp đồng ràng buộc giữa Backend và tất cả Client — ngăn tình trạng Frontend và Backend "hiểu lệch nhau" về dữ liệu.

**5. 🏗️ Sẵn sàng chuyển đổi sang Microservices**
> Các gói dịch vụ được phân rã logic rõ ràng → Có thể tách từng module thành container Docker độc lập khi hệ thống tăng tải thực tế.

---

---

## ── SLIDE 4: KIẾN TRÚC TỔNG THỂ (Architecture Diagram) ──

### Sơ Đồ Kiến Trúc Hệ Thống

```
┌────────────────────┐      ┌─────────────────────┐
│  Lecturer Web      │      │  Student Android     │
│  Portal (React)    │      │  App (Java MVVM)     │
│  Vite · Tailwind   │      │  CameraX · ML Kit    │
└────────┬───────────┘      └──────────┬───────────┘
         │                             │
         │   HTTPS · JSON · Bearer JWT │
         ▼                             ▼
┌─────────────────────────────────────────────────┐
│            Spring Boot REST API Server           │
│              Java 17 · Port 8081                 │
│      Spring Security · JWT Filter Chain         │
│         OpenAPI 3.0 Contract (Swagger)           │
└────────────────────┬────────────────────────────┘
                     │
         ┌───────────┼───────────────────┐
         ▼           ▼                   ▼
┌──────────────┐  ┌──────────┐  ┌──────────────────┐
│ Auth Service │  │  Group   │  │ Attendance &     │
│ (JWT+Session)│  │ Service  │  │ QR Token Service │
└──────────────┘  └──────────┘  └──────────────────┘
         │           │                   │
         ▼           ▼                   ▼
┌──────────────┐  ┌──────────┐  ┌──────────────────┐
│ Fraud &      │  │ Absence  │  │ Notification &   │
│ Monitoring   │  │ Service  │  │ Email Outbox     │
└──────────────┘  └──────────┘  └──────────────────┘
         │                                │
         ▼                                ▼
┌────────────────────┐      ┌─────────────────────┐
│ MySQL 8.x Database │      │  Redis 7.x Cache    │
│ (Flyway Migration) │      │  (QR Token · JWT)   │
└────────────────────┘      └─────────────────────┘
```

**Đặc điểm nổi bật:**
- ✅ Backend là **"Source of Truth"** duy nhất cho toàn bộ luật nghiệp vụ
- ✅ Clients giao tiếp qua **chuẩn RESTful JSON** với xác thực JWT
- ✅ Redis xử lý tải cao QR Check-in bảo vệ MySQL khỏi nghẽn cổ chai

---

---

## ── SLIDE 5: DANH SÁCH CÁC DỊCH VỤ (Services Definition) ──

### 7 Dịch Vụ Nghiệp Vụ Độc Lập Trong Backend

| # | Dịch vụ | Gói Java | Chức năng chính | Endpoints đại diện |
|:---:|:---|:---|:---|:---|
| 1 | **Auth Service** | `auth` | Đăng ký · Đăng nhập · JWT · Refresh Token · Reset mật khẩu · Force Change Password | `POST /auth/login` `POST /auth/register` `POST /auth/refresh` |
| 2 | **Group Service** | `group` | Quản lý lớp học · Thành viên · Quyền Owner/Co-host · Import danh sách sinh viên | `POST /groups` `POST /groups/join` `POST /groups/{id}/members/import` |
| 3 | **Attendance & QR Service** | `attendance` | Mở/đóng phiên học · Xoay QR động · Check-in QR · Chỉnh sửa thủ công | `POST /sessions/{id}/qr/rotate` `POST /sessions/{id}/checkin/qr` |
| 4 | **Fraud & Monitoring Service** | `fraud` | Ghi log check-in · Phát hiện dùng chung thiết bị · Cảnh báo IP bất thường · Quản lý vụ việc gian lận | `GET /groups/{id}/fraud-incidents` `PATCH /groups/{id}/fraud-incidents/{fid}` |
| 5 | **Absence Service** | `absence` | Nộp đơn xin nghỉ · Duyệt/Từ chối · Tự động cập nhật EXCUSED | `POST /groups/{id}/absence-requests` `POST /absence-requests/{id}/review` |
| 6 | **Notification & Mail Service** | `notification` `mail` | Thông báo thời gian thực · Email Outbox Pattern · Đếm thông báo chưa đọc | `GET /me/notifications` `POST /me/notifications/read-all` |
| 7 | **Stats Service** | `stats` | Tổng hợp tỉ lệ chuyên cần · Báo cáo cảnh báo vắng mặt · Xuất dữ liệu | `GET /me/attendance/summary` `GET /groups/{id}/attendance/export` |

---

---

## ── SLIDE 6: CƠ SỞ DỮ LIỆU (Database Design) ──

### Thiết Kế Cơ Sở Dữ Liệu Theo Ranh Giới Nghiệp Vụ

**Schema MySQL (Flyway Migration) — 20 bảng chính:**

| Nhóm nghiệp vụ | Các bảng thuộc nhóm |
|:---|:---|
| 🔐 **Auth & Security** | `users` · `user_sessions` · `password_reset_tokens` · `login_attempts` · `password_reset_attempts` |
| 🏫 **Class & Membership** | `class_groups` · `group_members` · `group_weekly_schedules` |
| 📋 **Sessions & QR** | `attendance_sessions` · `qr_tokens` |
| ✅ **Attendance** | `session_attendance` · `attendance_events` · `attendance_policies` |
| 📝 **Absence** | `absence_requests` |
| 🔔 **Notification** | `notifications` · `notification_deliveries` · `notification_rule_configs` · `email_outbox` |
| 🚨 **Fraud** | `checkin_attempt_logs` · `fraud_incidents` |

**Kỹ thuật đảm bảo tính toàn vẹn:**
- 🔑 **Foreign Key Constraints** — Ràng buộc quan hệ giữa các bảng
- 📌 **Unique Constraints** — Ngăn trùng lặp dữ liệu nghiệp vụ (1 user ↔ 1 session ↔ 1 attendance record)
- ⚙️ **Check Constraints** — Chặn giá trị enum không hợp lệ ở tầng DB
- 🗄️ **Indexes** — Tối ưu hiệu năng truy vấn các bảng lớn (`session_attendance`, `checkin_attempt_logs`)
- 🔄 **Trigger-based Hardening** — Cứng hóa luật chuyển trạng thái quan trọng ở mức Database

---

---

## ── SLIDE 7: GIAO TIẾP ĐỒNG BỘ (REST/HTTP) ──

### Phương Thức Giao Tiếp Synchronous — REST API over HTTP

**Tất cả giao tiếp Client ↔ Backend được thực hiện qua REST/JSON:**

```
Client (Web / Android)
      │
      │  HTTPS + Bearer JWT Token
      ▼
Spring Security Filter Chain
      │  Kiểm tra JWT · Resolve UserPrincipal
      ▼
REST Controller (API Layer)
      │  Validate DTO (@Valid)
      ▼
Service Layer (Business Logic)
      │  @Transactional
      ▼
JPA Repository → MySQL / Redis
```

**Thiết kế API nổi bật:**
- **Idempotent Check-in**: Quét QR trùng lặp (do camera bắn nhiều lần) → Trả về kết quả cũ `200 OK`, không tạo record điểm danh thứ hai
- **Mã lỗi có ngữ nghĩa**: `QR_TOKEN_EXPIRED` · `CHECKIN_OUT_OF_RANGE` · `SHARED_DEVICE_DETECTED` thay vì chỉ `400 Bad Request`
- **Phân trang chuẩn**: Tất cả API danh sách hỗ trợ `page`, `size`, `sortBy`, `sortDir`
- **OpenAPI Contract**: Tài liệu `openapi.yaml` là bản hợp đồng ràng buộc giữa Backend và tất cả Clients

---

---

## ── SLIDE 8: GIAO TIẾP BẤT ĐỒNG BỘ & EMAIL OUTBOX PATTERN ──

### Phương Thức Giao Tiếp Asynchronous — Event-Driven & Email Outbox

**Email Outbox Pattern — Tách biệt nghiệp vụ chính và gửi email:**

```
Nghiệp vụ chính (Check-in, Duyệt nghỉ phép...)
         │  @Transactional (Atomic Write)
         ▼
  INSERT email_outbox (trạng thái PENDING)
  ────────────────────────────────────────
         │  COMMIT Transaction thành công
         ▼
  Worker quét hàng đợi email (background)
         │  Gửi qua Mailpit/SMTP
         ▼
  UPDATE email_outbox (trạng thái SENT)
```

**Lý do áp dụng Email Outbox Pattern:**
> Nếu gửi email trực tiếp trong Transaction điểm danh — khi SMTP server gặp lỗi mạng, toàn bộ Transaction check-in sẽ bị rollback, sinh viên mất dữ liệu điểm danh. Pattern này tách biệt hoàn toàn rủi ro email khỏi nghiệp vụ cốt lõi.

**Fraud Detection — Transaction độc lập:**
```java
// Ghi log gian lận LUÔN thành công, kể cả khi check-in bị rollback
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void handleFailedAttempt(CheckinAttemptContext context) { ... }
```

---

---

## ── SLIDE 9: LUỒNG CHECK-IN QR — NGHIỆP VỤ TRỌNG TÂM ──

### Luồng Giao Tiếp QR Check-in Giữa 3 Dịch Vụ

```
[Giảng viên] → Web Portal → POST /sessions/{id}/qr/rotate
                                    │
                          Backend sinh plaintext token
                          Lưu SHA-256 hash vào Redis (TTL=30s)
                          Trả plaintext token về Web 1 lần duy nhất
                                    │
                          Web Portal render mã QR từ token
                                    ↓
[Sinh viên] → Android Camera → ML Kit đọc token
              Thu thập: deviceId · GPS (lat, lng)
              POST /sessions/{sessionId}/checkin/qr
                                    │
              Backend kiểm tra tuần tự:
              ① Thành viên lớp học hợp lệ?
              ② Session đang OPEN?
              ③ Hash token khớp Redis & chưa hết TTL?
              ④ Thời gian trong cửa sổ check-in?
              ⑤ GPS khoảng cách ≤ bán kính (Haversine)?
              ⑥ Device ID trùng tài khoản khác (gian lận)?
                                    │
              Ghi kết quả: PRESENT / LATE
              Ghi log: checkin_attempt_logs
              (nếu có) Tạo fraud_incidents
                                    │
              Trả kết quả 200 OK cho Android App
```

**3 lớp chống gian lận:**
- ⏱️ **QR xoay 15–30 giây** — Không thể chụp ảnh gửi cho bạn ở ngoài giảng đường
- 📱 **Device ID vật lý** — 1 điện thoại không thể điểm danh cho nhiều tài khoản
- 📍 **GPS Haversine Backend** — Client không thể tự giả mạo khoảng cách

---

---

## ── SLIDE 10: DEMO SẢN PHẨM ──

### Demo Thực Tế — Luồng Nghiệp Vụ Chính

**Thứ tự demo đề xuất (5–10 phút):**

**🎬 Cảnh 1: Giảng viên đăng nhập & mở phiên điểm danh (1 phút)**
- Giảng viên đăng nhập vào Web Portal
- Chọn lớp học → Nhấn "Mở điểm danh"
- Màn hình hiển thị mã QR đang đếm ngược xoay vòng

**🎬 Cảnh 2: Sinh viên quét QR điểm danh (2 phút)**
- Sinh viên mở ứng dụng Android → Đăng nhập
- Vào tính năng Quét QR
- Hướng camera vào màn hình giảng viên → Kết quả điểm danh hiển thị ngay

**🎬 Cảnh 3: Phát hiện gian lận điểm danh hộ (2 phút)**
- Demo thử dùng 2 tài khoản sinh viên khác nhau trên cùng 1 điện thoại để quét QR
- Quay lại Web Portal của giảng viên → Cảnh báo gian lận `SHARED_DEVICE_MULTI_ACCOUNT` xuất hiện
- Giảng viên xem chi tiết vụ việc → Xử lý

**🎬 Cảnh 4: Giảng viên xem báo cáo (1 phút)**
- Xem danh sách điểm danh theo buổi
- Xem tỉ lệ chuyên cần tổng hợp

---

---

## ── SLIDE 11: KẾT LUẬN — ĐÃ LÀM ĐƯỢC ──

### Những Gì Nhóm Đã Thực Hiện Được

**Công nghệ sử dụng:**

| Thành phần | Công nghệ |
|:---|:---|
| **Backend** | Java 17 · Spring Boot 3.5 · Spring Security · JJWT · Spring Data JPA · Flyway · Maven |
| **API** | RESTful · OpenAPI 3.0 · springdoc-openapi · Swagger UI |
| **Database** | MySQL 8.x · Redis 7.x |
| **Frontend** | React 19 · Vite · React Router v7 · Tailwind CSS · Recharts · qrcode.react |
| **Mobile** | Android Native · Java · CameraX · Google ML Kit · Retrofit 2 · Gson |
| **DevOps** | Docker · Docker Compose · GitHub Actions CI/CD |
| **Testing** | JUnit 5 · Mockito · MockMvc · Testcontainers · Spring Security Test |

**Các tính năng hoàn thiện (~90% phạm vi):**
- ✅ Xác thực JWT đầy đủ (Login · Register · Refresh · Logout · Reset Password · Force Change)
- ✅ Quản lý lớp học, thành viên, lịch học, phiên điểm danh với đầy đủ vòng đời trạng thái
- ✅ QR xoay động, check-in thời gian thực, tính toán GPS bằng Haversine
- ✅ Phát hiện gian lận 3 chiều (Device · IP Burst · GPS)
- ✅ Luồng xin nghỉ phép và tự động đồng bộ trạng thái EXCUSED
- ✅ Thông báo hệ thống và Email Outbox Pattern
- ✅ Cơ sở dữ liệu bảo vệ bằng 20 bảng + Foreign key · Unique constraint · Trigger hardening
- ✅ CI/CD GitHub Actions với MySQL service container

---

---

## ── SLIDE 12: KẾT LUẬN — HẠN CHẾ & HƯỚNG PHÁT TRIỂN ──

### Hạn Chế Hiện Tại & Hướng Phát Triển Tiếp Theo

**Các hạn chế cần thừa nhận:**

| Hạn chế | Giải thích |
|:---|:---|
| 🔑 Viết cứng địa chỉ API | `BASE_URL` đang hardcoded trong mã nguồn Web và Android, nên chuyển ra `.env` và `build.gradle` |
| 🔐 SSL Bypass trên Android | `getUnsafeOkHttpClient()` chỉ phù hợp môi trường debug — cần tắt khi release production |
| 🗄️ Database chung | Các module chia sẻ 1 schema MySQL — Vi phạm nguyên tắc Database-per-Service của SOA thuần túy |
| 🔄 Gọi đồng bộ nội bộ | `AttendanceService` gọi trực tiếp `FraudService` → nên chuyển sang bất đồng bộ `@Async EventListener` |
| 📊 Dashboard chưa live | Một số số liệu trên Web Portal chưa kết nối hoàn toàn với dữ liệu thực từ Backend |

---

**Hướng phát triển tiếp theo:**

| Ưu tiên | Hướng phát triển |
|:---:|:---|
| 🥇 **Cao** | Triển khai CI/CD tự động deploy lên server cloud (AWS/GCP/VPS) |
| 🥇 **Cao** | Bổ sung Network Security Config chuẩn trên Android (thay thế SSL bypass) |
| 🥈 **Trung bình** | Chuyển luồng thông báo sang bất đồng bộ hoàn toàn bằng Spring `@Async` + Message Queue |
| 🥈 **Trung bình** | Tích hợp hệ thống quan sát (Observability): Structured logs · Prometheus metrics · Grafana dashboard |
| 🥉 **Thấp** | Nâng cấp lên Email Activation flow thực sự (thay thế default password cho sinh viên import) |
| 🥉 **Thấp** | Bóc tách Fraud Service thành container Docker độc lập — bước đầu chuyển sang Microservices thực sự |

---

---

## ── SLIDE PHỤ: Q&A — CÂU HỎI THƯỜNG GẶP ──

### Chuẩn Bị Trả Lời Câu Hỏi Phản Biện

**❓ "Tại sao gọi là SOA nhưng backend lại là 1 file Monolith?"**
> Đây là **Modular Monolith** — triển khai trên 1 process nhưng thiết kế logic hoàn toàn theo SOA: ranh giới nghiệp vụ độc lập, hợp đồng API chuẩn OpenAPI, stateless JWT. Khi cần mở rộng, có thể tách từng package thành Microservice riêng mà không cần viết lại logic.

**❓ "QR xoay 30 giây thì database chịu tải thế nào?"**
> Không ghi QR Token vào MySQL. Token hash được lưu trên **Redis RAM** với TTL tự hủy = thời gian xoay vòng. Đối khớp diễn ra trên RAM trong microseconds, MySQL không bị đụng đến.

**❓ "Làm sao chống sinh viên chụp ảnh mã QR gửi cho bạn ở nhà điểm danh hộ?"**
> 3 lớp bảo vệ: QR hết hạn sau 15–30 giây *(chụp ảnh gửi đi là hết hạn)* + Device ID vật lý *(1 máy điểm cho 2 người → cảnh báo SHARED_DEVICE)* + GPS Haversine Backend tự tính *(client giả mạo tọa độ cũng bị phát hiện)*.

**❓ "Database dùng chung, vậy các service có độc lập không?"**
> Về mặt triển khai, các service chia sẻ 1 MySQL schema — đây là điểm hạn chế được nhóm nhận thức rõ. Về mặt thiết kế code, mỗi service chỉ thao tác với bảng thuộc domain của mình, không viết JOIN chéo qua ranh giới nghiệp vụ. Đây là bước trung gian hợp lý trước khi bóc tách sang Database-per-Service.
