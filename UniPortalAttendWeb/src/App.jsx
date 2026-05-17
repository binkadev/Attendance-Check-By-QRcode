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
import Profile from './features/auth/pages/Profile';
import ForgotPassword from './features/auth/pages/ForgotPassword';
import ResetPassword from './features/auth/pages/ResetPassword';

// Hàm kiểm tra Token
const isTokenValid = (token) => {
  if (!token) return false;
  try {
    const payloadBase64 = token.split('.')[1];
    const decodedJson = atob(payloadBase64);
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
  
  if (!token || !isTokenValid(token)) {
    // Xóa rác nếu token hết hạn
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    return <Navigate to="/login" replace />;
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

      {/* Main Routes */}
      <Routes location={background || location}>
        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        
        {/* Public Routes */}
        <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
        <Route path="/register" element={<PublicRoute><RegisterPage /></PublicRoute>} /> 
        <Route path="/forgot-password" element={<PublicRoute><ForgotPassword /></PublicRoute>} />
        <Route path="/reset-password" element={<PublicRoute><ResetPassword /></PublicRoute>} />

        {/* Các trang phụ trợ */}
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
    </>
  );
}

export default App;