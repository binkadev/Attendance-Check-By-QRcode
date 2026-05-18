# Yêu Cầu Cập Nhật API Quản Lý Gian Lận (Fraud Incidents)

Tài liệu này mô tả chi tiết các trường dữ liệu cần thiết từ phía Backend API (các endpoint liên quan đến `Fraud Incident`) nhằm hiển thị đầy đủ, chính xác và trực quan nhất trên giao diện Dashboard quản lý gian lận của Frontend.

## 1. Vấn Đề Hiện Tại

Hiện tại, dữ liệu thực tế từ API trả về đang bị thiếu một số thông tin quan trọng để hiển thị trên UI, dẫn đến việc Frontend phải xử lý fallback hoặc hiển thị không đầy đủ (Ví dụ: chỉ có `userId` thay vì thông tin đầy đủ của sinh viên, thiếu tiêu đề và mô tả sự cố rõ ràng, dữ liệu log thô chưa được bóc tách). 

Dưới đây là một mẫu UI hiển thị lý tưởng (dựa trên Mock Data) mà Frontend đang mong đợi:

> **Trùng ID thiết bị**
> Đang chờ
> Thiết bị ID A8F2 được dùng để điểm danh cho 2 sinh viên khác nhau
> 
> **Đối tượng vi phạm:** Nguyễn Văn Mạnh (STU-6520) - [Có Avatar]
> **Thời gian phát hiện:** 10:08 SA - 24 Th10, 2023
> **Mức độ rủi ro:** Rủi ro cao (Độ tin cậy 98%)
> **Dữ liệu bằng chứng (evidenceSummary):** IP, Trình duyệt, Tọa độ, Mã thiết bị...

## 2. Đề Xuất Cập Nhật Cấu Trúc JSON (Response)

Để Frontend có thể render chính xác như trên, BE vui lòng cập nhật/bổ sung cấu trúc Response của 2 APIs:
- `GET /api/v1/classes/{classId}/fraud-incidents` (Danh sách)
- `GET /api/v1/classes/{classId}/fraud-incidents/{id}` (Chi tiết)

### Cấu Trúc JSON Đề Xuất

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "type": "SHARED_DEVICE_MULTI_ACCOUNT", 
  
  // [MỚI] Bổ sung object student thay vì chỉ trả về userId string
  "student": {
    "id": "STU-6520",
    "name": "Nguyễn Văn Mạnh",
    "avatar": "https://url-to-avatar.jpg" // Có thể null nếu chưa có
  },
  
  // [MỚI] Bổ sung tiêu đề và mô tả chi tiết sự cố do AI/Logic sinh ra
  "title": "Trùng ID thiết bị", 
  "description": "Thiết bị ID A8F2 được dùng để điểm danh cho 2 sinh viên khác nhau",
  
  "status": "PENDING", // PENDING, RESOLVED
  "severity": "HIGH", // LOW, MEDIUM, HIGH, CRITICAL
  "confidence": 98, // % Độ tin cậy (int)
  "lastDetectedAt": "2023-10-24T10:08:00Z",
  
  // [CẬP NHẬT] Đưa toàn bộ log thô vào evidenceSummary dưới dạng Key-Value chuẩn hóa
  "evidenceSummary": {
    "ipAddress": "116.102.13.44",
    "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X)",
    "deviceId": "A8F2-99B1-C34X",
    "location": "21.037, 105.783", // Gộp lat, lng thành chuỗi để FE dễ hiển thị
    "distance": 2400, // (Tùy chọn) Tính bằng mét
    "occurrenceCount": 2 // Số lần vi phạm lặp lại
  }
}
```

## 3. Chi Tiết Các Trường Thay Đổi

| Trường Dữ Liệu | Loại | Mô Tả & Yêu Cầu Thay Đổi |
| :--- | :--- | :--- |
| `student` | Object | Trả về object chứa `id`, `name`, `avatar` của sinh viên thay vì chỉ trả về một chuỗi `userId`. Frontend cần `name` và `avatar` để hiển thị thẻ Đối tượng vi phạm. |
| `title` | String | Tiêu đề ngắn gọn của sự cố (Ví dụ: "Trùng ID thiết bị", "Quá thời gian quét"). Giúp FE không phải tự map `type` ra Text. |
| `description` | String | Mô tả rõ ràng nội dung vi phạm dựa vào logic hệ thống (Ví dụ: "Tọa độ quét cách phòng học 2.4km"). |
| `confidence` | Integer | (Bổ sung nếu thiếu) Thang điểm 0-100% thể hiện mức độ tin cậy của AI/Hệ thống khi bắt lỗi này. |
| `evidenceSummary` | Object | **KHÔNG** nên gom chung tất cả thành chuỗi JSON thô (Log). BE nên parse log thô thành các keys chuẩn như `ipAddress`, `deviceId`, `location`, `userAgent`... Frontend đã thiết lập bộ dịch tự động để map các key này sang Tiếng Việt hiển thị dạng danh sách (List). |

## 4. Lợi Ích Của Việc Cập Nhật
1. **Đồng Bộ Dữ Luệu:** Frontend không cần phải gọi thêm API User/Student Profile để lấy `name` và `avatar` dựa vào `userId`, giúp tăng tốc độ load và tối ưu performance.
2. **Dynamic Data:** Tiêu đề và mô tả (`title`, `description`) được sinh ra từ BE sẽ linh hoạt hơn, chứa được các biến số cụ thể (VD: khoảng cách chính xác, số lần lặp lại chính xác).
3. **Giao Diện Sạch (Clean UI):** Việc sử dụng `evidenceSummary` dưới dạng Key-Value giúp hiển thị danh sách các thông số bằng chứng đẹp mắt, thay vì hiển thị nguyên một cục Raw JSON làm rối mắt người dùng.
