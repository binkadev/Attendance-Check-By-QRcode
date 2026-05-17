// Sidebar.jsx
import { useLocation, useNavigate } from 'react-router-dom';
import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';

import { 
  ShieldAlert, LayoutDashboard, Users, PlusCircle, Clock, 
  AlertTriangle, CalendarX, LogOut, Loader2, ChevronLeft, ChevronRight 
} from 'lucide-react';

import { authApi } from '../../api/authApi';
import ConfirmDialog from '../../components/ui/ConfirmDialog';

export default function Sidebar({ onCollapseChange, activeTab, onTabChange }) {
  const navigate = useNavigate();
  const location = useLocation();
  const currentPath = location.pathname;
  
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [showConfirmDialog, setShowConfirmDialog] = useState(false);
  
  // State quản lý việc thu phóng Sidebar
  // Lấy trạng thái lưu trong localStorage (nếu có) để khi F5 trang không bị giật
  const [isCollapsed, setIsCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });

  // Gọi hàm callback lên component cha (Layout) mỗi khi state thay đổi
  useEffect(() => {
    localStorage.setItem('sidebar_collapsed', isCollapsed);
    if (onCollapseChange) {
      onCollapseChange(isCollapsed);
    }
  }, [isCollapsed, onCollapseChange]);

  const rawUser = localStorage.getItem('user') || '{}';
  const user = JSON.parse(rawUser);
  
  // Kiểm tra xem có ảnh đại diện bền vững không (không bị xóa khi logout)
  if (user && (user.id || user.email)) {
    const persistentAvatar = localStorage.getItem(`persistent_avatar_${user.id || user.email}`);
    if (persistentAvatar && !user.avatar) {
      user.avatar = persistentAvatar;
    }
  }

  const getNavClass = (path, exact = false) => {
    const isActive = exact 
      ? currentPath === path 
      : currentPath.startsWith(path) && (path !== '/classes' || !currentPath.includes('/create'));

    return `flex items-center transition-colors cursor-pointer relative group ${
      isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'
    } ${
      isActive 
        ? `bg-gray-800 text-white font-medium ${isCollapsed ? '' : 'border-l-4 border-red-600'}` 
        : 'hover:bg-gray-800 text-gray-400 hover:text-gray-200'
    }`;
  };

  const performLogout = async () => {
    setIsLoggingOut(true);
    setShowConfirmDialog(false);
    const loadingToast = toast.loading('Đang đăng xuất...');

    try {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) await authApi.logout({ refreshToken });
      
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
      localStorage.removeItem('web_device_id');
      
      toast.success('Đăng xuất thành công!', { id: loadingToast });
      setTimeout(() => navigate('/login', { replace: true }), 500);
    } catch (error) {
      console.error('Logout error:', error);
      localStorage.clear();
      toast.error(error.message || 'Đăng xuất thất bại', { id: loadingToast });
      setTimeout(() => navigate('/login', { replace: true }), 1000);
    } finally {
      setIsLoggingOut(false);
    }
  };

  const getInitials = () => {
    if (user.fullName) {
      const names = user.fullName.split(' ');
      if (names.length >= 2) return `${names[0].charAt(0)}${names[names.length - 1].charAt(0)}`.toUpperCase();
      return user.fullName.charAt(0).toUpperCase();
    }
    return 'GV';
  };

  return (
    <>
      <aside 
        className={`bg-gradient-to-b from-gray-900 to-gray-800 min-h-screen flex flex-col text-gray-300 fixed left-0 top-0 bottom-0 z-20 shrink-0 shadow-xl transition-all duration-300 ease-in-out ${
          isCollapsed ? 'w-[80px]' : 'w-64'
        }`}
      >
        {/* Nút Toggle nằm ở cạnh phải */}
        <button 
          onClick={() => setIsCollapsed(!isCollapsed)}
          className="absolute -right-3 top-8 bg-gray-800 border border-gray-600 text-white p-1 rounded-full hover:bg-gray-700 hover:text-red-400 transition-colors z-50 shadow-md"
        >
          {isCollapsed ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
        </button>

        {/* Logo Section */}
        <div className={`p-6 flex items-center border-b border-gray-700/50 ${isCollapsed ? 'justify-center px-0' : 'gap-3'}`}>
          <div className="w-10 h-10 shrink-0 bg-gradient-to-br from-red-600 to-red-800 rounded-xl flex items-center justify-center text-white font-bold shadow-lg">
            <ShieldAlert size={22} />
          </div>
          {!isCollapsed && (
            <div className="overflow-hidden whitespace-nowrap opacity-100 transition-opacity duration-300">
              <h1 className="text-white font-bold text-xl leading-tight tracking-tight">UniAttend</h1>
              <p className="text-xs text-gray-400">Cổng Giảng viên</p>
            </div>
          )}
        </div>

        {/* Navigation Menu */}
        <div className="flex-1 py-6 overflow-y-auto overflow-x-hidden">
          {!isCollapsed && (
            <div className="px-6 mb-4 text-[11px] font-bold text-gray-500 tracking-wider uppercase whitespace-nowrap">
              Menu chính
            </div>
          )}
          
          <nav className="space-y-1">
            <div onClick={() => navigate('/dashboard')} className={getNavClass('/dashboard')}>
              <LayoutDashboard size={isCollapsed ? 22 : 18} className={currentPath === '/dashboard' ? 'text-red-500' : ''} /> 
              {!isCollapsed && <span className="whitespace-nowrap">Tổng quan Dashboard</span>}
              {/* Tooltip khi thu nhỏ */}
              {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Tổng quan</div>}
            </div>
            
            <div onClick={() => navigate('/classes')} className={getNavClass('/classes', true)}>
              <Users size={isCollapsed ? 22 : 18} className={currentPath === '/classes' ? 'text-red-500' : ''} /> 
              {!isCollapsed && <span className="whitespace-nowrap">Quản lý lớp học</span>}
              {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Lớp học</div>}
            </div>
            
            <div onClick={() => navigate('/classes/create')} className={getNavClass('/classes/create')}>
              <PlusCircle size={isCollapsed ? 22 : 18} className={currentPath.includes('/create') ? 'text-red-500' : ''} /> 
              {!isCollapsed && <span className="whitespace-nowrap">Tạo lớp học mới</span>}
              {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Tạo lớp</div>}
            </div>
            
            <div onClick={() => navigate('/attendance-history')} className={getNavClass('/attendance-history')}>
              <Clock size={isCollapsed ? 22 : 18} className={currentPath === '/attendance-history' ? 'text-red-500' : ''} /> 
              {!isCollapsed && <span className="whitespace-nowrap">Lịch sử & Chỉnh sửa</span>}
              {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Lịch sử</div>}
            </div>
          </nav>

          {(activeTab && onTabChange) && !isCollapsed && (
            <div className="px-6 mt-8 mb-4 text-[11px] font-bold text-gray-500 tracking-wider uppercase whitespace-nowrap">
              Lớp đang chọn
            </div>
          )}
          
          {(activeTab && onTabChange) && (
            <nav className="space-y-1 mt-2">
              <div 
                onClick={() => onTabChange('students')}
                className={`flex items-center cursor-pointer transition-colors relative group ${isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'} ${activeTab === 'students' ? `bg-gray-800 text-white ${isCollapsed ? '' : 'border-l-4 border-red-600'}` : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`}
              >
                <Users size={isCollapsed ? 22 : 18} className={activeTab === 'students' ? 'text-red-500' : ''} /> 
                {!isCollapsed && <span className={activeTab === 'students' ? 'font-medium' : ''}>Danh sách sinh viên</span>}
                {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Sinh viên</div>}
              </div>

              <div 
                onClick={() => onTabChange('dynamic-qr')}
                className={`flex items-center cursor-pointer transition-colors relative group ${isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'} ${activeTab === 'dynamic-qr' ? `bg-gray-800 text-white ${isCollapsed ? '' : 'border-l-4 border-red-600'}` : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`}
              >
                <div className={`w-4 h-4 border-2 rounded-sm ${activeTab === 'dynamic-qr' ? 'border-red-500' : 'border-current'}`}></div> 
                {!isCollapsed && <span className={activeTab === 'dynamic-qr' ? 'font-medium' : ''}>Điểm danh QR động</span>}
                {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">QR Động</div>}
              </div>

              <div 
                onClick={() => onTabChange('sessions')}
                className={`flex items-center cursor-pointer transition-colors relative group ${isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'} ${activeTab === 'sessions' ? `bg-gray-800 text-white ${isCollapsed ? '' : 'border-l-4 border-red-600'}` : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`}
              >
                <Clock size={isCollapsed ? 22 : 18} className={activeTab === 'sessions' ? 'text-red-500' : ''} /> 
                {!isCollapsed && <span className={activeTab === 'sessions' ? 'font-medium' : ''}>Lịch sử phiên học</span>}
                {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Phiên học</div>}
              </div>

              <div 
                onClick={() => onTabChange('fraud')}
                className={`flex items-center cursor-pointer transition-colors relative group ${isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'} ${activeTab === 'fraud' ? `bg-gray-800 text-white ${isCollapsed ? '' : 'border-l-4 border-red-600'}` : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`}
              >
                <AlertTriangle size={isCollapsed ? 22 : 18} className={activeTab === 'fraud' ? 'text-red-500' : ''} /> 
                {!isCollapsed && <span className={activeTab === 'fraud' ? 'font-medium' : ''}>Cảnh báo gian lận</span>}
                {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Gian lận</div>}
              </div>

              <div 
                onClick={() => onTabChange('absence')}
                className={`flex items-center cursor-pointer transition-colors relative group ${isCollapsed ? 'justify-center py-3' : 'gap-3 px-6 py-2.5'} ${activeTab === 'absence' ? `bg-gray-800 text-white ${isCollapsed ? '' : 'border-l-4 border-red-600'}` : 'text-gray-400 hover:bg-gray-800 hover:text-gray-200'}`}
              >
                <CalendarX size={isCollapsed ? 22 : 18} className={activeTab === 'absence' ? 'text-red-500' : ''} /> 
                {!isCollapsed && <span className={activeTab === 'absence' ? 'font-medium' : ''}>Yêu cầu vắng mặt</span>}
                {isCollapsed && <div className="absolute left-full ml-4 bg-gray-900 text-white text-xs px-2 py-1 rounded opacity-0 group-hover:opacity-100 pointer-events-none transition-opacity z-50 whitespace-nowrap">Vắng mặt</div>}
              </div>
            </nav>
          )}
        </div>

        {/* User Profile & Logout Section */}
        <div className={`border-t border-gray-700/50 bg-gray-800/30 ${isCollapsed ? 'p-3' : 'p-4'}`}>
          <div 
            className="mb-3 cursor-pointer group/profile"
            onClick={() => navigate('/profile', { state: { background: location } })}
          >
            <div className={`flex items-center ${isCollapsed ? 'justify-center' : 'gap-3 px-2'}`}>
              <div className="w-10 h-10 shrink-0 bg-gradient-to-br from-red-600 to-red-800 rounded-xl flex items-center justify-center text-white font-bold text-sm shadow-md group-hover/profile:scale-110 transition-transform">
                {getInitials()}
              </div>
              {!isCollapsed && (
                <div className="flex-1 overflow-hidden">
                  <p className="text-sm text-white font-semibold truncate group-hover/profile:text-red-400 transition-colors">{user.fullName || 'Giảng viên'}</p>
                  <p className="text-xs text-gray-400 truncate">{user.userCode || user.email || 'Đại học'}</p>
                </div>
              )}
            </div>
          </div>
          
          <button
            onClick={() => setShowConfirmDialog(true)}
            disabled={isLoggingOut}
            className={`w-full flex items-center transition-all duration-300 group disabled:opacity-50 ${
              isCollapsed 
                ? 'justify-center p-3 rounded-xl hover:bg-gray-700' 
                : 'justify-between gap-3 px-3 py-2.5 rounded-xl bg-red-600/10 hover:bg-red-600/20'
            }`}
          >
            <div className={`flex items-center ${isCollapsed ? '' : 'gap-3'}`}>
              {isLoggingOut ? (
                <Loader2 size={18} className="animate-spin text-red-400" />
              ) : (
                <LogOut size={isCollapsed ? 20 : 18} className="text-red-400 group-hover:text-red-300 transition-colors" />
              )}
              {!isCollapsed && (
                <span className="text-sm font-medium text-red-400 group-hover:text-red-300 transition-colors">
                  {isLoggingOut ? 'Đang xuất...' : 'Đăng xuất'}
                </span>
              )}
            </div>
          </button>
        </div>
      </aside>

      <ConfirmDialog
        isOpen={showConfirmDialog}
        onClose={() => setShowConfirmDialog(false)}
        onConfirm={performLogout}
        title="Xác nhận đăng xuất"
        message="Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?"
      />
    </>
  );
}

