# Kế hoạch triển khai Hệ thống Đa ngôn ngữ (Việt - Anh) Toàn diện

Tài liệu này mô tả giải pháp thiết lập tính năng đa ngôn ngữ (Internationalization - i18n) cho hệ thống **UniAttend**, sử dụng cơ chế **React Context** tùy chỉnh. Giải pháp này giúp trang web có thể chuyển đổi linh hoạt qua lại giữa **Tiếng Việt (VI)** và **Tiếng Anh (EN)** tức thì mà không cần cài đặt thêm thư viện cồng kềnh, giảm thiểu rủi ro xung đột gói, lưu trạng thái người dùng vào LocalStorage và cập nhật đồng bộ trên toàn trang.

---

## User Review Required

> [!IMPORTANT]
> **Về Kiến trúc Đa ngôn ngữ:**
> - Chúng ta sẽ tạo một file `LanguageContext.jsx` chứa từ điển dịch (dictionaries) cho hai ngôn ngữ 'vi' và 'en'.
> - Từ điển sẽ được chia nhóm theo các màn hình: `common` (chân trang, liên kết chung), `login` (màn hình đăng nhập), `register` (màn hình đăng ký), và `about` (trang giới thiệu doanh nghiệp).
> - Để thuận tiện cho việc chuyển đổi ngôn ngữ, một nút bấm/dropdown chuyển đổi ngôn ngữ (Language Switcher Widget) thiết kế kính mờ (glassmorphism) đẹp mắt sẽ được tích hợp vào Header của trang Đăng nhập, Đăng ký và trang Giới thiệu.

---

## Proposed Changes

### 1. Xây dựng Lõi Đa ngôn ngữ (i18n Context)

#### [NEW] [LanguageContext.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/context/LanguageContext.jsx)
Tạo mới file quản lý trạng thái ngôn ngữ toàn cục:
- Lưu lựa chọn ngôn ngữ vào `localStorage` (mặc định là 'vi').
- Cung cấp hàm `t(key)` để dịch chuỗi ký tự theo cấu trúc dot-notation (Ví dụ: `t('login.email_placeholder')`).
- Cung cấp hàm `toggleLanguage()` hoặc `setLanguage(lang)`.
- Định nghĩa hook `useLanguage` để gọi nhanh ở mọi component.

### 2. Tích hợp Nhà cung cấp Toàn cục (Global Provider)

#### [MODIFY] [main.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/main.jsx)
Bọc ứng dụng `<App />` bên trong `<LanguageProvider>` để mọi component đều kế thừa tính năng dịch:
```jsx
import { LanguageProvider } from './context/LanguageContext'
// ...
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <LanguageProvider>
        <App />
      </LanguageProvider>
    </BrowserRouter>
  </StrictMode>,
)
```

### 3. Tích hợp Giao diện chọn Ngôn ngữ (Language Switcher UI)

#### [NEW] [LanguageSwitcher.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/components/layout/LanguageSwitcher.jsx)
Thiết kế một widget chuyển đổi ngôn ngữ nhỏ gọn, tinh tế:
- Hiển thị cờ và tên quốc gia tương ứng (VI / EN).
- Hiệu ứng hover mượt mà và nền Glassmorphism mờ sang trọng.
- Cho phép người dùng chuyển nhanh chỉ bằng 1 click.

### 4. Áp dụng Dịch cho các Trang Public cốt lõi

#### [MODIFY] [Login.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/auth/pages/Login.jsx)
Sử dụng `useLanguage` hook để dịch toàn bộ nhãn, văn bản, tiêu đề, và placeholder trong màn hình Đăng nhập (cả Tab mật khẩu & Tab mã QR). Tích hợp `LanguageSwitcher` vào Header.

#### [MODIFY] [RegisterPage.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/auth/pages/RegisterPage.jsx)
Sử dụng `useLanguage` để dịch toàn bộ màn hình Đăng ký tài khoản mới. Tích hợp `LanguageSwitcher` vào Header.

#### [MODIFY] [About.jsx](file:///home/phupv/mycode/UniPortalAttendWeb/src/features/about/About.jsx)
Dịch toàn bộ trang giới thiệu doanh nghiệp khổng lồ (Vision, Mission, Stats, 4 Core Values, Timeline các năm, và footer chi tiết của tập đoàn UniGroup). Tích hợp `LanguageSwitcher` vào Header.

---

## Verification Plan

### Kiểm thử tự động & Biên dịch
- Chạy `npm run build` để xác nhận việc tích hợp Context và các thay đổi hooks không làm gãy quá trình đóng gói React.

### Kiểm thử thủ công (Manual Verification)
1. **Kiểm tra chuyển đổi:** Click nút chuyển ngôn ngữ ở Header trang Login/Register/About và xác thực toàn bộ nội dung text lập tức thay đổi sang Tiếng Anh / Tiếng Việt mà không cần tải lại trang.
2. **Kiểm tra bộ nhớ tạm (Persistence):** Chuyển sang Tiếng Anh, tải lại trang (F5) và xác nhận ngôn ngữ vẫn giữ nguyên là Tiếng Anh.
