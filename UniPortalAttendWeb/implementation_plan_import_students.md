# Kế hoạch tích hợp API Import Sinh viên & Đồng bộ Logic Đối chiếu 12 Quy tắc

Tài liệu này mô tả chi tiết giải pháp tích hợp chức năng **Nhập danh sách sinh viên hàng loạt (Bulk Import Students)** giữa Frontend React (`UniPortalAttendWeb`) và API Backend mới (`POST /api/v1/groups/{groupId}/members/import`). 

Đồng thời, kế hoạch này thiết lập bộ **Giả lập đối chiếu Client-side (Dry-Run Simulator)** chính xác 100% theo **12 Quy tắc nghiệp vụ** của Backend, giúp hỗ trợ môi trường chạy thử (mock/local) mượt mà và nâng cao trải nghiệm người dùng với các cảnh báo trực quan trước khi thực hiện lưu dữ liệu chính thức.

---

## User Review Required

> [!IMPORTANT]
> **Điểm mấu chốt trong thiết kế API và Giao diện:**
> - **Cấu trúc API thống nhất:** Thay vì 2 endpoint riêng biệt như trước, Backend nay chỉ cung cấp một endpoint duy nhất là `POST /api/v1/groups/{groupId}/members/import`. Việc phân biệt giữa Chạy thử (Validation) và Lưu chính thức (Commit) sẽ được cấu hình qua tham số `"importMode": "VALIDATE_ONLY"` hoặc `"importMode": "COMMIT"`.
> - **Đồng bộ Định dạng Dữ liệu (Row Schema Mismatch):** Cần ánh xạ lại cấu trúc dữ liệu từ tệp CSV từ `{ studentCode, name, email }` thành `{ rowIndex, studentCode, fullName, email }` để khớp hoàn toàn với Dữ liệu Đầu vào của Backend.
> - **Ngăn chặn Xung đột Nghiêm trọng (Rule 6 - Reject Toàn bộ):** Trong trường hợp xảy ra xung đột chéo (Mã sinh viên trùng với User A nhưng Email lại trùng với User B), hệ thống giả lập và giao diện sẽ đánh dấu trạng thái lỗi nghiêm trọng, hiển thị hộp cảnh báo màu đỏ chi tiết và vô hiệu hóa (disable) hoàn toàn nút "Nhập danh sách" để tuân thủ quy tắc từ chối toàn bộ.

---

## Proposed Changes

### 1. Refactor API Services

#### [MODIFY] [classApi.js](file:///home/phupv/mycode/UniPortalAttendWeb/src/api/classApi.js)
Cập nhật hai phương thức `validateImportMembers` và `importMembers` để cùng gọi tới endpoint duy nhất `POST /api/v1/groups/{groupId}/members/import` với cấu trúc body chuẩn:
- **`validateImportMembers(groupId, members)`**: Truyền `importMode: "VALIDATE_ONLY"`, `syncMode: "APPEND_ONLY"`, `accountProvisioningMode: "CREATE_REQUIRE_PASSWORD_CHANGE"`, `notifyStudents: true` và mảng `members`.
- **`importMembers(groupId, members)`**: Truyền `importMode: "COMMIT"` cùng các cấu hình tương tự.
- Cả hai phương thức sẽ gửi mảng `members` có cấu trúc các phần tử là `{ rowIndex, studentCode, fullName, email }`.

---

### 2. Tái cấu trúc cấu trúc dữ liệu tệp tải lên (CSV Parsing)

#### [MODIFY] [StudentsTab.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/classes/components/StudentsTab.jsx)
Cập nhật luồng phân tích tệp trong `handleFileUpload`:
- Đổi tên trường từ `name` thành `fullName` khi trích xuất thông tin hàng dữ liệu.
- Bổ sung trường `rowIndex` tự động tăng (0-based index) cho từng phần tử khi nạp dữ liệu từ tệp CSV.

---

### 3. Đồng bộ Logic giả lập đối chiếu 12 Quy tắc Nghiệp vụ

#### [MODIFY] [StudentsTab.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/classes/components/StudentsTab.jsx)
Viết lại hoàn toàn hàm giả lập chạy thử `simulateDryRun(payload)` để tuân thủ chính xác 12 quy tắc:
1. **Kiểm tra trùng lặp trong file (Rule 3):** Quét toàn bộ danh sách import, nếu phát hiện `studentCode` hoặc `email` bị lặp lại trong chính file đó, đánh dấu lỗi `action: 'ERROR'` và `errorMsg: 'Trùng lặp mã sinh viên hoặc email trong tệp tải lên'`.
2. **Khớp mã sinh viên trước (Rule 4):** So khớp `studentCode` với cơ sở dữ liệu hệ thống (ở môi trường mock, là danh sách `MOCK_SYSTEM_USERS` kết hợp học sinh hiện tại).
3. **Khớp email tiếp theo (Rule 5):** Nếu không khớp MSSV, tiến hành đối chiếu qua `email`.
4. **Phát hiện xung đột chéo (Rule 6 - Reject Toàn bộ):** Nếu MSSV khớp với User A nhưng Email khớp với User B, thiết lập cờ cảnh báo `hasCriticalConflict = true`, ghi nhận lỗi xung đột rõ ràng trên dòng dữ liệu.
5. **Xác định hành động cho User có sẵn (Rule 7, 11, 12):**
   - Nếu User đã ở trong lớp với trạng thái `ACTIVE` hoặc `APPROVED` => Bỏ qua (`SKIPPED_EXISTING_MEMBER`).
   - Nếu User từng tham gia lớp nhưng bị `REMOVED` hoặc `REJECTED` => Khôi phục về APPROVED (`RESTORED_MEMBER`).
   - Nếu User tồn tại trên hệ thống nhưng chưa vào lớp => Liên kết và thêm mới vào lớp (`LINKED_EXISTING_USER_AND_ADDED`).
6. **Xác định hành động cho User chưa tồn tại (Rule 8, 9, 10):**
   - Đánh dấu tạo mới tài khoản và thêm vào lớp (`CREATED_USER_AND_ADDED`).
   - Ghi chú quy tắc mật khẩu mặc định (Ghost Account: MSSV + Họ tên viết liền không dấu, chữ thường).
   - Thiết lập trạng thái tài khoản là `ACTIVE` và yêu cầu đổi mật khẩu ở lần đăng nhập đầu tiên (`requirePasswordChange = true`).

---

### 4. Nâng cấp Giao diện Wizard 3 bước (Premium UI)

#### [MODIFY] [StudentsTab.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/classes/components/StudentsTab.jsx)
Cải thiện giao diện bước đối chiếu dữ liệu (Step 2) và hoàn thành (Step 3):
- **Xử lý Response dạng Object:** Hỗ trợ hiển thị kết quả từ API thực tế trả về dạng đối tượng chứa tổng số liệu thống kê (`createdUsers`, `linkedExistingUsers`, `addedMembers`, `skippedExistingMembers`, `restoredMembers`) cùng danh sách `items` chi tiết, thay vì mảng phẳng như trước đây.
- **Hộp thoại Cảnh báo Xung đột Nghiêm trọng (Rule 6 Warning):** Khi phát hiện cờ `hasCriticalConflict` (trong cả kết quả API thực hoặc kết quả giả lập), hiển thị banner đỏ nổi bật cảnh báo người dùng: *"Phát hiện xung đột tài khoản chéo. Theo chính sách bảo mật, toàn bộ yêu cầu nhập tệp này sẽ bị từ chối."*
- **Kiểm soát nút bấm hành động:** Vô hiệu hóa (disable) nút "Nhập danh sách" khi có lỗi xung đột nghiêm trọng hoặc khi không có bất kỳ hàng nào hợp lệ.
- **Hiển thị Bảng đối chiếu Thông minh:** Ánh xạ các trạng thái `action` của Backend thành nhãn màu sắc bắt mắt:
  - `CREATED_USER_AND_ADDED` ➡️ Màu xanh dương "Tạo tài khoản mới"
  - `LINKED_EXISTING_USER_AND_ADDED` ➡️ Màu xanh lá "Liên kết tài khoản"
  - `RESTORED_MEMBER` ➡️ Màu ngọc bích "Khôi phục thành viên"
  - `SKIPPED_EXISTING_MEMBER` ➡️ Màu xám "Đã tham gia (Bỏ qua)"
  - `ERROR` ➡️ Màu đỏ kèm chi tiết lý do lỗi.
- **Bảng Tổng kết hoàn thành chi tiết:** Ở Bước 3, hiển thị biểu đồ đếm số lượng tài khoản mới tạo, tài khoản liên kết cũ, số lượng sinh viên đã được khôi phục quyền học tập thành công và hướng dẫn mật khẩu mặc định cực kỳ trực quan.

---

## Verification Plan

### Kiểm thử Tự động & Build
- Chạy lệnh build để đảm bảo việc thay đổi cú pháp, import, và logic trong `StudentsTab.jsx` không làm phát sinh lỗi biên dịch.

### Kiểm thử Thủ công (Manual Verification Flow)
1. **Kiểm tra Tải file CSV mẫu chuẩn:** Tải lên tệp hợp lệ, kiểm tra xem hệ thống có nhận diện đúng số lượng và phân loại chính xác các sinh viên chưa có tài khoản (NEW/CREATED) và sinh viên đã có tài khoản (EXISTING/LINKED) hay không.
2. **Kiểm tra Phát hiện Trùng lặp trong File:** Nhập một file có 2 dòng trùng nhau về email hoặc studentCode. Xác nhận hệ thống báo lỗi đỏ *"Trùng lặp mã sinh viên hoặc email trong tệp"* tại bước 2.
3. **Kiểm tra Phát hiện Xung đột Chéo (Rule 6):** Sử dụng dữ liệu thử nghiệm có chứa sinh viên có MSSV trùng với User A nhưng Email lại trùng với User B. Xác nhận hệ thống bật cảnh báo đỏ toàn cục và chặn hoàn toàn thao tác Lưu (Commit).
4. **Kiểm tra Khôi phục Thành viên cũ (Rule 12):** Nạp một sinh viên trước đây có trạng thái trong lớp là `REJECTED` hoặc `REMOVED`. Xác nhận ở bước 2 hệ thống hiển thị phân loại "Khôi phục thành viên" (`RESTORED_MEMBER`).
