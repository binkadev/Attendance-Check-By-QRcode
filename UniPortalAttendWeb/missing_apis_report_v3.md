# Báo cáo Yêu cầu Cập nhật & Bổ sung API (Phiên bản v3)
### Tài liệu kỹ thuật dành cho Đội ngũ Phát triển Backend (BE)

Dưới đây là danh sách chi tiết các API còn thiếu, các trường thông tin (Fields) bị khuyết trong Response, và các yêu cầu chuẩn hóa Schema dữ liệu được tổng hợp trực tiếp từ quá trình tích hợp giao diện Cổng Giảng viên (UniPortal). 

Đội ngũ Frontend đã xây dựng sẵn các cơ chế xử lý cục bộ (Client-side fallbacks) và giao diện trực quan cao cấp. Rất cần phía Backend cập nhật sớm để đồng bộ hoạt động với dữ liệu thật.

---

## 1. Màn hình Điểm danh & Lịch sử Phiên học (`SessionHistoryTab.jsx`, `HistoryAndManualEdit.jsx`)

### 1.1 Lưu trữ phương thức điểm danh khi Chỉnh sửa thủ công
* **Vấn đề**: Hiện tại, khi giảng viên điều chỉnh trạng thái điểm danh thủ công (từ Vắng sang Đi học/Muộn), phương thức điểm danh (`checkInMethod`) không phân biệt được rõ ràng hoặc bị reset về mặc định trên DB, khiến lịch sử nạp lại bị sai lệch.
* **Yêu cầu bổ sung**:
  * Các API ghi nhận/sửa đổi điểm danh thủ công (Manual Checkin):
    * `PATCH /api/v1/attendance-events/{id}`
    * `POST /api/v1/sessions/{sessionId}/manual`
  * Cần lưu trữ và trả về trường `checkInMethod` (Enum: `MANUAL`, `QR_DYNAMIC`, `FACE_ID`, `GPS`). 
  * Khi giảng viên điểm danh hộ, giá trị phải được lưu cố định là `MANUAL` để phân biệt với sinh viên tự quét `QR_DYNAMIC`.

### 1.2 Đồng bộ Mã sinh viên (MSV) thay vì ID tuần tự
* **Vấn đề**: Nhiều API điểm danh đang trả về hoặc yêu cầu `userId` (ID sequence tự sinh của DB) thay vì Mã số sinh viên chính thức (`userCode` / MSV). Điều này gây lỗi khớp dữ liệu khi hiển thị danh sách.
* **Yêu cầu bổ sung**:
  * Đảm bảo tất cả các Response trả về danh sách điểm danh, sự kiện điểm danh luôn chứa cả hai trường:
    * `userId` (String/Long): ID định danh tài khoản.
    * `userCode` (String): Mã sinh viên (MSV) dùng để tìm kiếm và đối chiếu.

---

## 2. Quản lý lớp học & Thành viên (`StudentsTab.jsx`, `ClassDetail.jsx`)

### 2.1 Xóa sinh viên khỏi lớp học (Remove Member)
* **Vấn đề**: Hiện tại Frontend đã dựng nút Xóa sinh viên khỏi lớp kèm Modal xác nhận an toàn tại Tab Sinh viên, tuy nhiên chưa có API cho phép Giảng viên xóa một thành viên bất kỳ.
* **Yêu cầu API mới**:
  * **Endpoint**: `DELETE /api/v1/groups/{groupId}/members/{memberId}`
  * **Hành vi**: Xóa bản ghi thành viên có `memberId` khỏi lớp `groupId`. Chỉ cho phép tài khoản có quyền `LECTURER` hoặc `OWNER` của lớp đó thực hiện. Giảng viên không được phép tự xóa chính mình.

### 2.2 Bổ sung thông tin metadata lớp học (`GET /api/v1/groups/{groupId}`)
* **Vấn đề**: Các trang Chi tiết lớp học, Quản lý lớp và máy chiếu QR hiện đang phải hiển thị dữ liệu giả định đối với các trường thông tin phòng học và học kỳ do API trả về thiếu.
* **Yêu cầu bổ sung vào Response**:
  * `room` (String): Phòng học (VD: "2E03", "Hội trường 1").
  * `semester` (String): Học kỳ hiện tại (VD: "HK1", "Kỳ Thu").
  * `academicYear` (String): Năm học (VD: "2025-2026").
  * `weeklySchedules` (Array): Lịch học cố định hàng tuần để tính toán lịch tự động.

---

## 3. Quản lý Sự cố Gian lận & Cảnh báo Bảo mật (`FraudTab.jsx`, Trang chủ Dashboard)

### 3.1 Bổ sung đầy đủ thông tin Sinh viên liên quan trực tiếp vào sự cố
* **Vấn đề**: API sự cố gian lận (`GET /api/v1/groups/{groupId}/fraud-incidents`) trả về thông tin thiết bị nhưng thiếu thông tin chi tiết của sinh viên liên đới, dẫn đến hiển thị nhãn "Không rõ" hoặc "Thiết bị không xác định" trên bảng tin.
* **Yêu cầu bổ sung vào Response**:
  * Trả về đối tượng sinh viên vi phạm đầy đủ trong từng sự cố:
    ```json
    {
      "id": "incident-uuid",
      "type": "DUPLICATE_DEVICE_ID", 
      "student": {
        "userId": "user-uuid",
        "fullName": "Nguyễn Văn A",
        "userCode": "B22DCCN123"
      },
      "device": {
        "deviceId": "88e0baaa45ba",
        "deviceName": "iPhone 13 Pro"
      }
    }
    ```

### 3.2 Chuẩn hóa ghi nhận thiết bị điểm danh (Device Audit Log)
* **Vấn đề**: Khi thiết bị gửi quét QR, hệ thống trả về thông tin máy trống/null, hoặc chuỗi UserAgent thô chưa qua phân tách, làm giảm hiệu quả giám sát gian lận.
* **Yêu cầu chuẩn hóa**:
  * Backend cần tích hợp bộ thư viện phân tích UserAgent (như `uap-core`) để bóc tách và trả về thông tin máy sạch:
    * `deviceBrand` (Hãng máy: Apple, Samsung...)
    * `deviceModel` (Dòng máy: iPhone 13, Galaxy S22...)
    * `osName` / `osVersion` (Hệ điều hành: iOS 17.2, Android 14...)
  * Nếu không có thông tin hoặc không parse được, trả về giá trị null cụ thể thay vì chuỗi rác để Frontend hiển thị nhãn chuẩn *"Thiết bị không xác định"*.

---

## 4. Quản lý Đơn xin phép & Minh chứng ngoại tuyến (`AbsenceTab.jsx`)

### 4.1 Bổ sung thông tin Minh chứng PDF / Ảnh trực tuyến
* **Vấn đề**: Frontend đã tích hợp bộ xem tài liệu trực tuyến **FilePreviewModal (hỗ trợ Google Docs PDF Viewer và Image Viewer)**. Tuy nhiên, API hiện tại trả về liên kết chứa tiêu đề tải về ép buộc (Content-Disposition: attachment) hoặc thiếu cấu trúc phân loại tệp.
* **Yêu cầu bổ sung**:
  * **Trường dữ liệu trong `GET /api/v1/groups/{groupId}/absence-requests`**:
    * `evidenceUrl` (String): Đường dẫn trực tiếp đến tệp tin lưu trữ trên CDN/S3.
    * `evidenceName` (String): Tên tệp tin (VD: "giay_ra_vien.pdf").
    * `evidenceType` (Enum/String): Phân loại tệp để Frontend render đúng khung (`pdf` hoặc `img`).
  * **Cấu hình Headers phía S3/CDN**:
    * Cần cấu hình `Content-Type` chuẩn (`application/pdf`, `image/jpeg`, `image/png`).
    * Tránh cấu hình header `Content-Disposition: attachment`, thay vào đó hãy để trình duyệt tự do đọc nội dung trực tiếp qua iframe bằng cách dùng `Content-Disposition: inline`.

### 4.2 Lấy thông tin sinh viên đi kèm đơn xin nghỉ
* **Vấn đề**: Hiện tại API chỉ trả về `requesterUserId`. Frontend đang phải thực hiện gọi thêm API danh sách thành viên rồi tự đối chiếu để tìm tên và MSV của người viết đơn.
* **Yêu cầu bổ sung**:
  * Trả về kèm thông tin chi tiết sinh viên gửi đơn trực tiếp trong mỗi Item của danh sách:
    ```json
    {
      "id": "request-id",
      "requester": {
        "userId": "user-uuid",
        "fullName": "Trần Thị Bình",
        "userCode": "B22DCCN456"
      }
    }
    ```

---

## 5. Tổng kết Trạng thái Frontend (Đã hoàn tất 100%)
Các trang màn hình Frontend đã được tối ưu hóa toàn diện:
* Giao diện co giãn màn hình rộng thông minh (`max-w-[1600px] 2xl:max-w-[1800px]`).
* Bộ nạp dữ liệu mờ shimmer skeletons hiện đại, mượt mà.
* Các modal xem trước PDF/Hình ảnh trực tiếp dùng Google Docs Viewer an toàn, tiện lợi.
* Bộ lọc trạng thái đơn xin nghỉ học, tìm kiếm sinh viên, cảnh báo bảo mật thời gian thực.

*Kính đề nghị đội ngũ Backend ưu tiên cập nhật sớm các trường thông tin trên để nâng cấp hệ thống đạt chất lượng cao nhất!*
