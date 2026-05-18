const BASE_URL_ME = 'http://localhost:8081/api/v1/me/classes';
const BASE_URL_GROUPS = 'http://localhost:8081/api/v1/groups';
const BASE_URL_SESSIONS = 'http://localhost:8081/api/v1/sessions';

const BASE_URL_ME_SESSIONS = 'http://localhost:8081/api/v1/me/sessions';

// Danh sách sort field được backend hỗ trợ
const VALID_SORT_FIELDS = ['createdAt', 'updatedAt', 'groupName', 'courseCode', 'startTime'];

const getHeaders = () => {
  const token = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': `Bearer ${token}` } : {})
  };
};

// Helper xử lý response
const handleResponse = async (response) => {
  const contentType = response.headers.get('content-type');
  const isJson = contentType?.includes('application/json');
  const data = isJson ? await response.json() : null;
  
  if (!response.ok) {
    const error = new Error(data?.message || `HTTP ${response.status}: ${response.statusText}`);
    error.status = response.status;
    error.code = data?.code;
    throw error;
  }
  
  return data;
};

export const classApi = {
  // ==================== API CHO SINH VIÊN ====================
  
  /**
   * Lấy danh sách lớp sinh viên đã tham gia
   * @param {Object} params - { q, scope, status, memberStatus, semester, academicYear, page, size, sortBy, sortDir }
   */
  getStudentClasses: async (params = {}) => {
    try {
      const cleanParams = { ...params };
      
      // Lọc sortBy nếu không hợp lệ
      if (cleanParams.sortBy && !VALID_SORT_FIELDS.includes(cleanParams.sortBy)) {
        console.warn(`Sort field "${cleanParams.sortBy}" is not supported. Using default sort.`);
        delete cleanParams.sortBy;
        delete cleanParams.sortDir;
      }
      
      const query = new URLSearchParams();
      Object.entries(cleanParams).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          query.append(key, value);
        }
      });
      
      const url = `${BASE_URL_ME}${query.toString() ? `?${query.toString()}` : ''}`;
      console.log('Fetching student classes:', url);
      
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get student classes error:", error);
      throw error;
    }
  },

  // ==================== API CHO GIẢNG VIÊN ====================
  
  /**
   * Lấy danh sách lớp giảng viên đang dạy/tạo
   * @param {Object} params - { q, status, semester, academicYear, page, size, sortBy, sortDir }
   */
  getTeachingClasses: async (params = {}) => {
    try {
      const cleanParams = { ...params };
      
      // Lọc sortBy nếu không hợp lệ
      if (cleanParams.sortBy && !VALID_SORT_FIELDS.includes(cleanParams.sortBy)) {
        console.warn(`Sort field "${cleanParams.sortBy}" is not supported. Using default sort.`);
        delete cleanParams.sortBy;
        delete cleanParams.sortDir;
      }
      
      const query = new URLSearchParams();
      Object.entries(cleanParams).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          query.append(key, value);
        }
      });
      
      const url = `${BASE_URL_ME}/teaching${query.toString() ? `?${query.toString()}` : ''}`;
      console.log('Fetching teaching classes:', url);
      
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get teaching classes error:", error);
      throw error;
    }
  },

  // ==================== API LỊCH HỌC (TIMELINE) ====================
  
  /**
   * Lấy timeline lịch học (hôm nay, ngày mai, đang diễn ra)
   * Dùng cho cả sinh viên và giảng viên
   */
  getClassTimeline: async () => {
    try {
      const response = await fetch(`${BASE_URL_ME}/timeline`, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get class timeline error:", error);
      throw error;
    }
  },

  // ==================== API HỌC KỲ ====================
  
  /**
   * Lấy danh sách học kỳ (dùng cho cả 2 API)
   * Có thể dùng chung vì response giống nhau
   */
  getSemesters: async () => {
    try {
      const response = await fetch(`${BASE_URL_ME}/semesters`, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get semesters error:", error);
      throw error;
    }
  },

  // ==================== API CHI TIẾT LỚP ====================
  
  /**
   * Lấy chi tiết một lớp (theo groupId)
   */
  getClassDetail: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}`, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get class detail error:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách thành viên trong lớp
   */
  getClassMembers: async (groupId, params = {}) => {
    try {
      const query = new URLSearchParams(params).toString();
      const url = `${BASE_URL_GROUPS}/${groupId}/members${query ? `?${query}` : ''}`;
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get class members error:", error);
      throw error;
    }
  },

  // ==================== API TẠO LỚP ====================
  
  /**
   * Tạo lớp mới (chỉ giảng viên)
   */
  createGroup: async (payload) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(payload)
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Create group error:", error);
      throw error;
    }
  },

  /**
   * Cập nhật lớp học (chỉ giảng viên)
   */
  updateGroup: async (groupId, payload) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}`, {
        method: 'PUT',
        headers: getHeaders(),
        body: JSON.stringify(payload)
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Update group error:", error);
      throw error;
    }
  },

  /**
   * Xóa lớp học (chỉ giảng viên)
   */
  deleteGroup: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}`, {
        method: 'DELETE',
        headers: getHeaders()
      });
      if (response.status === 204) return true;
      return handleResponse(response);
    } catch (error) {
      console.error("Delete group error:", error);
      throw error;
    }
  },

  // ==================== API QUẢN LÝ PHIÊN ĐIỂM DANH ====================
  
  /**
   * Lấy phiên điểm danh đang mở của lớp
   */
  getOpenSession: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions/open`, { headers: getHeaders() });
      
      // Backend trả về 404 hoặc 204 khi không có phiên mở
      if (response.status === 404 || response.status === 204) {
        return null;
      }

      // Đọc body để kiểm tra trước khi xử lý
      const contentType = response.headers.get('content-type');
      const isJson = contentType?.includes('application/json');
      const data = isJson ? await response.json() : null;

      // Backend đôi khi trả 200 nhưng body có code lỗi SESSION_OPEN_NOT_FOUND
      if (data?.code === 'SESSION_OPEN_NOT_FOUND') {
        return null;
      }

      if (!response.ok) {
        const error = new Error(data?.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.code = data?.code;
        throw error;
      }

      return data;
    } catch (error) {
      // Bắt mọi dạng SESSION_OPEN_NOT_FOUND – trả null để không throw
      if (error.code === 'SESSION_OPEN_NOT_FOUND' || error.status === 404) {
        return null;
      }
      // Im lặng đối với lỗi này trên dashboard (không có phiên mở là bình thường)
      if (error.message?.includes('SESSION_OPEN_NOT_FOUND')) {
        return null;
      }
      // Chỉ log lỗi thực sự (không phải "không có phiên")
      console.warn('getOpenSession:', error.message);
      return null; // Trả null thay vì throw – tránh làm vỡ UI
    }
  },

  /**
   * Tạo phiên điểm danh mới
   */
  createSession: async (groupId, data) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Create session error:", error);
      throw error;
    }
  },

  /**
   * Lấy lịch sử các phiên điểm danh của lớp
   */
  getGroupSessions: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions`, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get group sessions error:", error);
      throw error;
    }
  },

  /**
   * Điểm danh (thủ công hoặc QR)
   */
  submitAttendance: async (sessionId, userId, data) => {
    try {
      const response = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance/${userId}`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Submit attendance error:", error);
      throw error;
    }
  },

  /**
   * Reset/Hủy điểm danh của một sinh viên
   */
  resetAttendance: async (sessionId, userId) => {
    try {
      const response = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance/${userId}/reset`, {
        method: 'POST',
        headers: getHeaders(),
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Reset attendance error:", error);
      throw error;
    }
  },

  /**
   * Lấy sự kiện điểm danh theo thời gian thực
   */
  getAttendanceEvents: async (sessionId, limit = 200) => {
    try {
      const response = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance-events?limit=${limit}`, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get attendance events error:", error);
      throw error;
    }
  },

  /**
   * Xoay vòng mã QR
   */
  rotateQR: async (sessionId) => {
    try {
      const response = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/qr/rotate`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify({ note: "Auto rotated" })
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Rotate QR error:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách sự cố gian lận của lớp
   */
  getFraudIncidents: async (groupId, params = {}) => {
    try {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          if (Array.isArray(value)) {
            value.forEach(v => query.append(key, v));
          } else {
            query.append(key, value);
          }
        }
      });
      
      const url = `${BASE_URL_GROUPS}/${groupId}/fraud-incidents${query.toString() ? `?${query.toString()}` : ''}`;
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get fraud incidents error:", error);
      throw error;
    }
  },

  /**
   * Lấy chi tiết một sự cố gian lận
   */
  getFraudIncidentDetail: async (groupId, incidentId) => {
    try {
      const url = `${BASE_URL_GROUPS}/${groupId}/fraud-incidents/${incidentId}`;
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get fraud incident detail error:", error);
      throw error;
    }
  },

  /**
   * Cập nhật trạng thái sự cố gian lận (Acknowledge/Resolve)
   */
  updateFraudIncident: async (groupId, incidentId, data) => {
    try {
      const url = `${BASE_URL_GROUPS}/${groupId}/fraud-incidents/${incidentId}`;
      const response = await fetch(url, {
        method: 'PATCH',
        headers: getHeaders(),
        body: JSON.stringify(data),
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Update fraud incident error:", error);
      throw error;
    }
  },

  // ==================== QUẢN LÝ ĐƠN XIN PHÉP (ABSENCE REQUESTS) ====================
  /**
   * Lấy danh sách đơn xin phép của lớp
   */
  getAbsenceRequests: async (groupId, params = {}) => {
    try {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== '') {
          query.append(key, value);
        }
      });
      const url = `${BASE_URL_GROUPS}/${groupId}/absence-requests${query.toString() ? `?${query.toString()}` : ''}`;
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get absence requests error:", error);
      throw error;
    }
  },

  /**
   * Duyệt hoặc từ chối đơn xin phép
   */
  reviewAbsenceRequest: async (requestId, data) => {
    try {
      const response = await fetch(`http://localhost:8081/api/v1/absence-requests/${requestId}/review`, {
        method: 'PATCH',
        headers: getHeaders(),
        body: JSON.stringify(data)
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Review absence request error:", error);
      throw error;
    }
  },

  /**
   * Revert (Hoàn tác) một đơn đã duyệt/từ chối
   */
  revertAbsenceRequest: async (requestId, data) => {
    try {
      const response = await fetch(`http://localhost:8081/api/v1/absence-requests/${requestId}/revert`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data)
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Revert absence request error:", error);
      throw error;
    }
  },

  /**
   * Hủy đơn (Sinh viên thực hiện)
   */
  cancelAbsenceRequest: async (requestId) => {
    try {
      const response = await fetch(`http://localhost:8081/api/v1/absence-requests/${requestId}/cancel`, {
        method: 'POST',
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Cancel absence request error:", error);
      throw error;
    }
  },

  /**
   * Lấy chi tiết đơn
   */
  getAbsenceRequestDetail: async (requestId) => {
    try {
      const response = await fetch(`http://localhost:8081/api/v1/absence-requests/${requestId}`, {
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Get absence request detail error:", error);
      throw error;
    }
  },

  /**
   * Gửi đơn mới (Sinh viên thực hiện)
   */
  createAbsenceRequest: async (groupId, data) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/absence-requests`, {
        method: 'POST',
        headers: getHeaders(),
        body: JSON.stringify(data)
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Create absence request error:", error);
      throw error;
    }
  },

  /**
   * Lấy danh sách đơn của bản thân
   */
  getMyAbsenceRequests: async (params = {}) => {
    try {
      const query = new URLSearchParams(params).toString();
      const response = await fetch(`http://localhost:8081/api/v1/me/absence-requests?${query}`, {
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Get my absence requests error:", error);
      throw error;
    }
  },

  // ==================== LỊCH HỌC SẮP TỚI (DASHBOARD) ====================
  /**
   * Lấy lịch học upcoming (hôm nay, ngày mai)
   */
  getUpcomingSessions: async (limit = 20) => {
    try {
      const url = `${BASE_URL_ME_SESSIONS}/upcoming?limit=${limit}`;
      const response = await fetch(url, { headers: getHeaders() });
      return handleResponse(response);
    } catch (error) {
      console.error("Get upcoming sessions error:", error);
      throw error;
    }
  },

  // ==================== API BỔ SUNG ====================
  /**
   * Đóng phiên điểm danh
   */
  closeSession: async (sessionId) => {
    try {
      const response = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/close`, {
        method: 'POST',
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Close session error:", error);
      throw error;
    }
  },

  /**
   * Xuất danh sách điểm danh
   */
  exportAttendance: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/attendance/export`, {
        headers: getHeaders()
      });
      // Đối với xuất file, trả về blob thay vì JSON
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      return await response.blob();
    } catch (error) {
      console.error("Export attendance error:", error);
      throw error;
    }
  },

  /**
   * Lấy thống kê tổng quan của lớp học
   */
  getAttendanceSummary: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/attendance/summary`, {
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Get attendance summary error:", error);
      throw error;
    }
  },

  /**
   * Lấy tỷ lệ điểm danh của danh sách sinh viên
   */
  getStudentsAttendancePolicy: async (groupId) => {
    try {
      const response = await fetch(`${BASE_URL_GROUPS}/${groupId}/attendance-policy/students`, {
        headers: getHeaders()
      });
      return handleResponse(response);
    } catch (error) {
      console.error("Get students attendance policy error:", error);
      throw error;
    }
  }
};



// const BASE_URL_ME = 'http://localhost:8081/api/v1/me/classes';
// const BASE_URL_GROUPS = 'http://localhost:8081/api/v1/groups';
// const BASE_URL_SESSIONS = 'http://localhost:8081/api/v1/sessions';

// const getHeaders = () => {
//   const token = localStorage.getItem('accessToken');
//   return {
//     'Content-Type': 'application/json',
//     ...(token ? { 'Authorization': `Bearer ${token}` } : {})
//   };
// };

// export const classApi = {
//   // Lấy danh sách lớp đang dạy
//   getTeachingClasses: async (params = {}) => {
//     const query = new URLSearchParams(params).toString();
//     const res = await fetch(`${BASE_URL_ME}/teaching?${query}`, { headers: getHeaders() });
//     if (!res.ok) throw new Error('Failed to fetch classes');
//     return res.json();
//   },

//   // Lấy danh sách học kỳ
//   getSemesters: async () => {
//     const res = await fetch(`${BASE_URL_ME}/semesters`, { headers: getHeaders() });
//     return res.json();
//   },

//   // Lấy chi tiết lớp
//   getClassDetail: async (groupId) => {
//     const res = await fetch(`${BASE_URL_GROUPS}/${groupId}`, { headers: getHeaders() });
//     return res.json();
//   },

//   // Lấy thành viên lớp
//   getClassMembers: async (groupId, params = {}) => {
//     const query = new URLSearchParams(params).toString();
//     const res = await fetch(`${BASE_URL_GROUPS}/${groupId}/members?${query}`, { headers: getHeaders() });
//     return res.json();
//   },

//   // --- CÁC HÀM PHIÊN ĐIỂM DANH (SỬ TRỰC TIẾP LỖI CỦA BẠN) ---

//   // Lấy phiên đang mở
//   getOpenSession: async (groupId) => {
//     const res = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions/open`, { headers: getHeaders() });
//     if (res.status === 404 || res.status === 204) return null;
//     if (!res.ok) throw new Error('Failed to fetch open session');
//     return res.json();
//   },

//   // Tạo phiên mới
//   createSession: async (groupId, data) => {
//     const res = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions`, {
//       method: 'POST',
//       headers: getHeaders(),
//       body: JSON.stringify(data),
//     });
//     if (!res.ok) throw new Error('Failed to create session');
//     return res.json();
//   },

//   // Lấy lịch sử phiên của lớp
//   getGroupSessions: async (groupId) => {
//     const res = await fetch(`${BASE_URL_GROUPS}/${groupId}/sessions`, { headers: getHeaders() });
//     return res.json();
//   },

//   // Điểm danh (Manual/QR)
//   submitAttendance: async (sessionId, userId, data) => {
//     const res = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance/${userId}`, {
//       method: 'POST',
//       headers: getHeaders(),
//       body: JSON.stringify(data),
//     });
//     return res.json();
//   },

//   // Reset/Hủy điểm danh (Dùng API Chốt)
//   resetAttendance: async (sessionId, userId) => {
//     const res = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance/${userId}/reset`, {
//       method: 'POST',
//       headers: getHeaders(),
//     });
//     if (!res.ok) throw new Error('Failed to reset attendance');
//     return res.json();
//   },

//   // Lấy sự kiện điểm danh (Real-time feed)
//   getAttendanceEvents: async (sessionId, limit = 200) => {
//     const res = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/attendance-events?limit=${limit}`, { headers: getHeaders() });
//     return res.json();
//   },

//   // Xoay vòng QR
//   rotateQR: async (sessionId) => {
//     const res = await fetch(`${BASE_URL_SESSIONS}/${sessionId}/qr/rotate`, {
//       method: 'POST',
//       headers: getHeaders(),
//       body: JSON.stringify({ note: "Auto rotated" })
//     });
//     return res.json();
//   },

//   // Tạo lớp mới
//   createGroup: async (payload) => {
//     const res = await fetch(`${BASE_URL_GROUPS}`, {
//       method: 'POST',
//       headers: getHeaders(),
//       body: JSON.stringify(payload)
//     });
//     return res.json();
//   },
// };
