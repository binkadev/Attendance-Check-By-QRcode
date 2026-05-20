import React from 'react';
import { Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { Toaster } from 'react-hot-toast'; 

import Login from './features/auth/pages/Login';
import RegisterPage from './features/auth/pages/RegisterPage';
import Dashboard from './features/dashboard/pages/Dashboard';
import ClassManagementLayout from './features/classes/pages/ClassManagement';
import CreateClass from './features/classes/pages/CreateClass';
import ClassDetail from './features/classes/pages/ClassDetail';
import HistoryAndManualEdit from './features/attendance-history/pages/HistoryAndManualEdit';
import TermsOfService from './features/legal/TermsOfService';
import PrivacyPolicy from './features/legal/PrivacyPolicy';
import HelpCenter from './features/support/HelpCenter'; 
import About from './features/about/About';
import Profile from './features/auth/pages/Profile';
import ForgotPassword from './features/auth/pages/ForgotPassword';
import ResetPassword from './features/auth/pages/ResetPassword';
import TokenCountdownWidget from './features/auth/components/TokenCountdownWidget';
import ForceChangePassword from './features/auth/pages/ForceChangePassword';

// Hàm kiểm tra Token
const isTokenValid = (token) => {
  if (!token) return false;
  try {
    const payloadBase64 = token.split('.')[1];
    // 1. Đổi ký tự Base64Url sang Base64 tiêu chuẩn
    const base64 = payloadBase64.replace(/-/g, '+').replace(/_/g, '/');
    // 2. Thêm dấu '=' bù độ dài chia hết cho 4 để tránh lỗi ở một số trình duyệt
    const paddedBase64 = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
    
    const decodedJson = atob(paddedBase64);
    const decoded = JSON.parse(decodedJson);
    const currentTime = Date.now() / 1000;
    return decoded.exp > currentTime;
  } catch (error) {
    console.error("Lỗi giải mã token:", error);
    return false;
  }
};

// Component bảo vệ các trang yêu cầu đăng nhập (Chặn khách)
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem('accessToken');
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  
  if (!token || !isTokenValid(token)) {
    // Xóa rác nếu token hết hạn
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    return <Navigate to="/login" replace />;
  }
  
  // Nếu là Ghost Account chưa đổi mật khẩu -> Bắt buộc đẩy vào trang đổi mật khẩu
  if (user.isFirstLogin || user.requirePasswordChange) {
    return <Navigate to="/force-change-password" replace />;
  }
  
  return children;
};

// Component bảo vệ trang Ép Đổi Mật Khẩu
const ForceChangePasswordRoute = ({ children }) => {
  const token = localStorage.getItem('accessToken');
  const user = JSON.parse(localStorage.getItem('user') || '{}');
  
  if (!token || !isTokenValid(token)) {
    return <Navigate to="/login" replace />;
  }
  
  // Nếu đã đổi mật khẩu thành công rồi -> Trả về dashboard
  if (!user.isFirstLogin && !user.requirePasswordChange) {
    return <Navigate to="/dashboard" replace />;
  }
  
  return children;
};

// Component bảo vệ các trang Public (Chặn người dùng ĐÃ đăng nhập)
const PublicRoute = ({ children }) => {
  const token = localStorage.getItem('accessToken');
  
  // Nếu đã đăng nhập hợp lệ mà vào Login/Register -> Đẩy thẳng vào Dashboard
  if (token && isTokenValid(token)) {
    return <Navigate to="/dashboard" replace />;
  }
  
  return children;
};

function App() {
  const location = useLocation();
  // Kiểm tra xem có location state 'background' không để hiển thị modal
  const background = location.state && location.state.background;

  // State và Effect quản lý phiên làm việc hết hạn toàn cục
  const [sessionExpiredMsg, setSessionExpiredMsg] = React.useState(null);

  React.useEffect(() => {
    const handleUnauthorized = (event) => {
      if (!sessionExpiredMsg) {
        setSessionExpiredMsg(event.detail?.message || 'Phiên làm việc đã hết hạn.');
      }
    };

    window.addEventListener('unauthorized-api-error', handleUnauthorized);
    return () => {
      window.removeEventListener('unauthorized-api-error', handleUnauthorized);
    };
  }, [sessionExpiredMsg]);

  const handleLogoutAndRedirect = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setSessionExpiredMsg(null);
    window.location.href = '/login';
  };

  return (
    <>
      <Toaster 
        position="top-right" 
        reverseOrder={false}
        toastOptions={{
          style: {
            background: '#1a1a1a',
            color: '#fff',
            borderRadius: '12px',
          },
          success: {
            iconTheme: {
              primary: '#10b981',
              secondary: '#fff',
            },
          },
        }}
      />

      {/* Bộ đếm ngược thời gian sống của token (kéo thả tự do) */}
      <TokenCountdownWidget />

      {/* Main Routes */}
      <Routes location={background || location}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        
        {/* Public Routes */}
        <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} /> 
        <Route path="/forgot-password" element={<PublicRoute><ForgotPassword /></PublicRoute>} />
        <Route path="/reset-password" element={<PublicRoute><ResetPassword /></PublicRoute>} />
        <Route 
          path="/force-change-password" 
          element={
            <ForceChangePasswordRoute>
              <ForceChangePassword />
            </ForceChangePasswordRoute>
          } 
        />

        {/* Các trang phụ trợ */}
        <Route path="/about" element={<About />} />
        <Route path="/terms" element={<TermsOfService />} />
        <Route path="/privacy" element={<PrivacyPolicy />} />
        <Route path="/help" element={<HelpCenter />} />

        {/* Protected Routes */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/classes"
          element={
            <ProtectedRoute>
              <ClassManagementLayout />
            </ProtectedRoute>
          }
        />
        <Route
          path="/classes/create"
          element={
            <ProtectedRoute>
              <CreateClass />
            </ProtectedRoute>
          }
        />
        <Route
          path="/classes/:classId"
          element={
            <ProtectedRoute>
              <ClassDetail />
            </ProtectedRoute>
          } 
        />
        
        <Route
          path="/attendance-history"
          element={
            <ProtectedRoute>
              <HistoryAndManualEdit />
            </ProtectedRoute>
          } 
        />

        {/* Route Profile khi truy cập trực tiếp (không phải modal) */}
        {!background && (
          <Route
            path="/profile"
            element={
              <ProtectedRoute>
                <Profile isModal={false} />
              </ProtectedRoute>
            } 
          />
        )}
        
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>

      {/* Modal Route cho Profile */}
      {background && (
        <Routes>
          <Route 
            path="/profile" 
            element={
              <ProtectedRoute>
                <Profile isModal={true} />
              </ProtectedRoute>
            } 
          />
        </Routes>
      )}

      {/* Centralized Session Expiration Modal Overlay */}
      {sessionExpiredMsg && (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-slate-900/75 backdrop-blur-md animate-in fade-in duration-300">
          <div className="bg-white rounded-3xl max-w-md w-full p-8 shadow-2xl border border-slate-100 flex flex-col items-center text-center animate-in zoom-in-95 slide-in-from-bottom-8 duration-300">
            {/* Cảnh báo Icon với vòng tròn sóng động */}
            <div className="w-16 h-16 rounded-full bg-red-50 border border-red-100 flex items-center justify-center mb-6 relative">
              <div className="absolute inset-0 rounded-full bg-red-500/10 animate-ping opacity-75"></div>
              <svg className="w-8 h-8 text-red-600 relative z-10" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
              </svg>
            </div>

            {/* Chi tiết nội dung thông báo */}
            <h3 className="text-xl font-bold text-slate-800 mb-3">Phiên đăng nhập hết hạn</h3>
            <p className="text-sm text-slate-500 leading-relaxed mb-6">
              {sessionExpiredMsg}
            </p>
            <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100/80 mb-6 text-left w-full">
              <div className="flex gap-2.5 items-start">
                <svg className="w-5 h-5 text-red-500 shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <div>
                  <span className="text-[12px] font-bold text-slate-700 block">Lý do bảo mật</span>
                  <span className="text-[11px] text-slate-500 leading-normal block mt-0.5">
                    Mã xác thực của phiên làm việc đã quá hạn hoặc không còn hiệu lực trên thiết bị này. Vui lòng đăng xuất để làm mới lại kết nối bảo mật.
                  </span>
                </div>
              </div>
            </div>

            {/* Nút hành động */}
            <button
              onClick={handleLogoutAndRedirect}
              className="w-full py-3.5 px-6 bg-red-600 hover:bg-red-700 active:scale-[0.98] text-white font-bold rounded-2xl shadow-lg shadow-red-600/10 hover:shadow-red-600/20 transition-all flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path>
              </svg>
              Xác nhận Đăng xuất
            </button>
          </div>
        </div>
      )}
    </>
  );
}

export default App;