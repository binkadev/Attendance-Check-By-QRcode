// src/api/authApi.js

// Khai báo BASE_URL chuẩn 1 lần duy nhất
const BASE_URL = 'http://localhost:8081/api/v1/auth';

export const authApi = {
  // ==========================================
  // 1. API ĐĂNG NHẬP
  // ==========================================
  login: async (credentials) => {
    try {
      const response = await fetch(`${BASE_URL}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        // credentials chứa: email, password, deviceId
        body: JSON.stringify(credentials),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || 'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.');
      }

      return data;
    } catch (error) {
      console.error("Login API Error:", error);
      throw error;
    }
  },

  // ==========================================
  // 2. API ĐĂNG KÝ MỚI
  // ==========================================
  register: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/register`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        // payload chứa: email, password, fullName, userCode, deviceId
        body: JSON.stringify(payload),
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.message || data.code || 'Lỗi đăng ký tài khoản');
      }

      return data;
    } catch (error) {
      console.error("Register API Error:", error);
      throw error;
    }
  },

  // // ==========================================
  // // 3. API ĐĂNG XUẤT
  // // ==========================================
  // logout: async (payload) => {
  //   try {
  //     const response = await fetch(`${BASE_URL}/logout`, {
  //       method: 'POST',
  //       headers: {
  //         'Content-Type': 'application/json',
  //         // Tùy vào Backend của bạn, nếu yêu cầu mang theo Access Token để gọi API Logout thì mở comment dòng dưới:
  //         // 'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
  //       },
  //       body: JSON.stringify(payload), // payload chứa: { refreshToken: "..." }
  //     });

  //     // BẮT MÃ 204 NO CONTENT (Thành công nhưng không có body)
  //     if (response.status === 204 || response.ok) {
  //       return true; 
  //     }

  //     // Nếu lỗi, cố gắng đọc JSON (nếu có)
  //     const data = await response.json().catch(() => ({}));
  //     throw new Error(data.message || 'Lỗi khi đăng xuất khỏi máy chủ');
      
  //   } catch (error) {
  //     console.error("Logout API Error:", error);
  //     throw error;
  //   }
  // },


    // ==========================================
  // LOGOUT API
  // ==========================================
  logout: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify(payload), // { refreshToken: "..." }
      });

      if (response.status === 204 || response.ok) {
        return true;
      }

      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Lỗi khi đăng xuất');
      
    } catch (error) {
      console.error("Logout API Error:", error);
      throw error;
    }
  },

  // ==========================================
  // 4. API ĐĂNG XUẤT TẤT CẢ THIẾT BỊ
  // ==========================================
  logoutAll: async () => {
    try {
      const response = await fetch(`${BASE_URL}/logout-all`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        }
      });

      if (response.status === 204 || response.ok) {
        return true;
      }

      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Lỗi khi đăng xuất tất cả');
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 5. API ĐỔI MẬT KHẨU
  // ==========================================
  changePassword: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/change-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify(payload), // { currentPassword, newPassword }
      });

      if (response.status === 204 || response.ok) {
        return true;
      }

      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Lỗi khi đổi mật khẩu');
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 5b. API ÉP ĐỔI MẬT KHẨU (GHOST ACCOUNT ĐĂNG NHẬP LẦN ĐẦU)
  // ==========================================
  forceChangePassword: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/force-change-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify(payload), // { currentPassword, newPassword }
      });

      if (response.status === 204 || response.ok) {
        return true;
      }

      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Lỗi khi đổi mật khẩu mặc định');
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 6. API QUÊN MẬT KHẨU
  // ==========================================
  forgotPassword: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/forgot-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload), // { email }
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Lỗi gửi yêu cầu quên mật khẩu');
      }
      return data;
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 7. API ĐẶT LẠI MẬT KHẨU
  // ==========================================
  resetPassword: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/reset-password`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload), // { token, newPassword }
      });

      if (response.status === 204 || response.ok) {
        return true;
      }

      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Lỗi khi đặt lại mật khẩu');
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 8. API REFRESH TOKEN
  // ==========================================
  refresh: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL}/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload), // { refreshToken }
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Lỗi refresh token');
      }
      return data;
    } catch (error) {
      throw error;
    }
  },

  // ==========================================
  // 9. API CẬP NHẬT THÔNG TIN CÁ NHÂN
  // ==========================================
  updateProfile: async (payload) => {
    try {
      // Giả định endpoint là /api/v1/users/me hoặc /api/v1/profile
      const response = await fetch(`http://localhost:8081/api/v1/users/me`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify(payload), // { fullName, avatarUrl, ... }
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.message || 'Lỗi khi cập nhật thông tin');
      }
      return data;
    } catch (error) {
      throw error;
    }
  },
};