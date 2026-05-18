import React, { useState, useEffect, useRef } from 'react';
import { 
  User, Mail, Shield, Lock, KeyRound, 
  Save, LogOut, ChevronLeft, Loader2,
  Camera, BadgeCheck, Smartphone, X, MapPin, Laptop
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';
import ConfirmDialog from '../../../components/ui/ConfirmDialog';

export default function Profile({ isModal = false }) {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
  
  // State quản lý thông tin User
  const [user, setUser] = useState(() => {
    const rawUser = JSON.parse(localStorage.getItem('user') || '{}');
    const persistentAvatar = localStorage.getItem(`persistent_avatar_${rawUser.id || rawUser.email}`);
    if (persistentAvatar && !rawUser.avatar) {
      return { ...rawUser, avatar: persistentAvatar };
    }
    return rawUser;
  });
  
  const [isEditingInfo, setIsEditingInfo] = useState(false);
  const [editedUser, setEditedUser] = useState({ fullName: user.fullName || '' });
  
  const [isChangingPass, setIsChangingPass] = useState(false);
  const [passData, setPassData] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [isLoading, setIsLoading] = useState(false);
  const [currentLocation, setCurrentLocation] = useState('Đang lấy vị trí...');
  const [locationDetails, setLocationDetails] = useState('');
  
  // State quản lý Modal xác nhận đăng xuất toàn bộ
  const [showLogoutAllConfirm, setShowLogoutAllConfirm] = useState(false);

  // Lấy vị trí GPS định vị thời gian thực
  useEffect(() => {
    if ("geolocation" in navigator) {
      const geoOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      };

      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const lat = position.coords.latitude.toFixed(4);
          const lon = position.coords.longitude.toFixed(4);
          setCurrentLocation(`Lat: ${lat}, Lon: ${lon}`);

          try {
            const response = await fetch(
              `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&accept-language=vi`
            );
            const data = await response.json();
            
            if (data && data.address) {
              const addr = data.address;
              const ward = addr.suburb || addr.village || addr.neighbourhood || addr.quarter || addr.hamlet;
              const district = addr.city_district || addr.county || addr.district || addr.town;
              const province = addr.city || addr.state || addr.province;

              const addressParts = [ward, district, province].filter(Boolean);
              const uniqueAddress = [...new Set(addressParts)].join(', ');
              
              setLocationDetails(uniqueAddress);
            }
          } catch (error) {
            console.error("Lỗi khi lấy thông tin địa chỉ:", error);
          }
        },
        (error) => {
          console.error("Lỗi định vị:", error);
          if (error.code === 1) {
            setCurrentLocation('Bị từ chối quyền truy cập vị trí');
          } else if (error.code === 2) {
            setCurrentLocation('Không thể xác định vị trí (Sóng yếu/Lỗi mạng)');
          } else {
            setCurrentLocation('Hết thời gian tìm vị trí');
          }
        },
        geoOptions
      );
    } else {
      setCurrentLocation('Trình duyệt không hỗ trợ GPS');
    }
  }, []);

  const handleSaveProfile = async () => {
    setIsLoading(true);
    try {
      await authApi.updateProfile({ fullName: editedUser.fullName });
      const updatedUser = { ...user, fullName: editedUser.fullName };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      toast.success('Cập nhật thông tin thành công!');
      setIsEditingInfo(false);
    } catch (err) {
      const updatedUser = { ...user, fullName: editedUser.fullName };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      toast.success('Đã cập nhật cục bộ (API chưa phản hồi)');
      setIsEditingInfo(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAvatarChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        return toast.error('Kích thước ảnh không được vượt quá 2MB');
      }
      
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64String = reader.result;
        const updatedUser = { ...user, avatar: base64String };
        localStorage.setItem('user', JSON.stringify(updatedUser));
        
        const storageKey = `persistent_avatar_${user.id || user.email}`;
        localStorage.setItem(storageKey, base64String);
        
        setUser(updatedUser);
        toast.success('Đã cập nhật ảnh đại diện');
      };
      reader.readAsDataURL(file);
    }
  };

  const handlePassChange = (e) => {
    setPassData({ ...passData, [e.target.name]: e.target.value });
  };

  const submitChangePassword = async (e) => {
    e.preventDefault();
    if (passData.newPassword !== passData.confirmPassword) {
      return toast.error('Mật khẩu mới không khớp!');
    }
    if (passData.newPassword.length < 8) {
      return toast.error('Mật khẩu mới phải có ít nhất 8 ký tự!');
    }

    setIsLoading(true);
    try {
      await authApi.changePassword({
        currentPassword: passData.currentPassword,
        newPassword: passData.newPassword
      });
      toast.success('Đổi mật khẩu thành công!');
      setIsChangingPass(false);
      setPassData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      toast.error(err.message || 'Lỗi khi đổi mật khẩu');
    } finally {
      setIsLoading(false);
    }
  };

  // Hàm xử lý Đăng xuất toàn bộ thiết bị
  const handleLogoutAllDevices = async () => {
    const loadingToast = toast.loading('Đang đăng xuất toàn bộ thiết bị...');
    try {
      await authApi.logoutAll();
      toast.success('Đã đăng xuất khỏi toàn bộ các thiết bị', { id: loadingToast });
      setShowLogoutAllConfirm(false);
      localStorage.clear();
      setTimeout(() => navigate('/login', { replace: true }), 500);
    } catch (error) {
      console.error(error);
      toast.success('Đã xóa phiên cục bộ (Không thể đồng bộ với server)', { id: loadingToast });
      setShowLogoutAllConfirm(false);
      localStorage.clear();
      setTimeout(() => navigate('/login', { replace: true }), 500);
    }
  };

  const content = (
    <div className={`flex flex-col ${isModal ? 'w-full max-w-4xl bg-white rounded-3xl shadow-2xl overflow-hidden max-h-[90vh]' : 'min-h-screen bg-slate-50/50'}`}>
      {/* Header với thiết kế Glassmorphism */}
      <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-100 flex items-center px-6 sticky top-0 z-10 justify-between">
        <div className="flex items-center">
          <button 
            onClick={() => navigate(-1)}
            className="p-2 hover:bg-slate-100 rounded-full transition-all mr-4 hover:scale-105 active:scale-95"
          >
            {isModal ? <X size={18} className="text-slate-600" /> : <ChevronLeft size={18} className="text-slate-600" />}
          </button>
          <h1 className="text-base font-bold text-slate-800 tracking-tight">Hồ sơ cá nhân</h1>
        </div>
        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-50 border border-slate-100 text-xs font-semibold text-slate-500">
          <Laptop size={13} className="text-slate-400" />
          Giảng viên
        </div>
      </header>

      <main className={`flex-1 overflow-y-auto ${isModal ? 'p-6' : 'max-w-4xl mx-auto w-full p-6'} space-y-6 animate-fade-in-up`}>
        
        {/* Banner Profile & Avatar */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden relative">
          {/* Gradient Banner cao cấp */}
          <div className="h-36 bg-gradient-to-r from-red-600 via-rose-500 to-orange-500 relative overflow-hidden">
            {/* Vòng tròn phản chiếu nghệ thuật */}
            <div className="absolute top-[-50px] right-[-30px] w-48 h-48 rounded-full bg-white/10 blur-xl"></div>
            <div className="absolute bottom-[-20px] left-1/3 w-36 h-36 rounded-full bg-white/10 blur-xl"></div>
            
            <div className="absolute -bottom-14 left-8">
              <div className="relative group">
                <div className="w-28 h-28 rounded-3xl bg-white p-1.5 shadow-xl overflow-hidden transition-all duration-300 group-hover:scale-105">
                  <div className="w-full h-full rounded-2xl bg-slate-50 flex items-center justify-center text-red-600 font-extrabold text-4xl overflow-hidden border border-slate-100">
                    {user.avatar ? (
                      <img src={user.avatar} alt="avatar" className="w-full h-full object-cover" />
                    ) : (
                      user.fullName?.charAt(0).toUpperCase() || 'G'
                    )}
                  </div>
                </div>
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  className="hidden" 
                  accept="image/*"
                  onChange={handleAvatarChange}
                />
                <button 
                  onClick={() => fileInputRef.current.click()}
                  className="absolute bottom-1.5 right-1.5 p-2 bg-red-600 text-white rounded-xl shadow-lg border-2 border-white hover:bg-red-700 hover:scale-110 active:scale-95 transition-all"
                  title="Thay ảnh đại diện"
                >
                  <Camera size={14} />
                </button>
              </div>
            </div>
          </div>
          
          <div className="pt-16 pb-6 px-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
            <div>
              <div className="flex items-center gap-2">
                {isEditingInfo ? (
                  <input 
                    type="text"
                    value={editedUser.fullName}
                    onChange={(e) => setEditedUser({ ...editedUser, fullName: e.target.value })}
                    className="text-2xl font-bold text-slate-800 border-b-2 border-red-500 outline-none bg-transparent py-1 px-1 focus:bg-slate-50 rounded"
                    autoFocus
                  />
                ) : (
                  <h2 className="text-2xl font-black text-slate-800 tracking-tight">{user.fullName || 'Chưa cập nhật'}</h2>
                )}
                <div className="flex items-center justify-center w-5 h-5 rounded-full bg-blue-500 text-white shadow-sm" title="Tài khoản đã xác minh">
                  <BadgeCheck size={14} />
                </div>
              </div>
              <p className="text-slate-400 text-xs font-semibold mt-1">Giảng viên bộ môn • UniPortal System</p>
            </div>
            <div className="flex gap-3 w-full md:w-auto">
               {isEditingInfo ? (
                 <>
                   <button 
                    onClick={() => { setIsEditingInfo(false); setEditedUser({ fullName: user.fullName }); }}
                    className="flex-1 md:flex-none px-5 py-2.5 bg-slate-100 border border-slate-200 rounded-2xl text-xs font-bold text-slate-600 hover:bg-slate-200 transition-colors"
                   >
                     Hủy
                   </button>
                   <button 
                    onClick={handleSaveProfile}
                    disabled={isLoading}
                    className="flex-1 md:flex-none px-5 py-2.5 bg-red-600 text-white rounded-2xl text-xs font-bold hover:bg-red-700 shadow-lg shadow-red-200 transition-all flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-[0.98]"
                   >
                     {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                     Lưu thay đổi
                   </button>
                 </>
               ) : (
                 <button 
                  onClick={() => setIsEditingInfo(true)}
                  className="flex-1 md:flex-none px-5 py-2.5 bg-white border border-slate-200 hover:border-slate-300 rounded-2xl text-xs font-bold text-slate-700 hover:bg-slate-50 transition-all flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-[0.98]"
                 >
                   Chỉnh sửa thông tin
                 </button>
               )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Cột trái: Thông tin & Đổi mật khẩu */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Thông tin liên hệ */}
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6">
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-6">Thông tin tài khoản</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex items-center gap-4 p-3 bg-slate-50 rounded-2xl border border-slate-100/50">
                  <div className="w-10 h-10 rounded-xl bg-white border border-slate-100 flex items-center justify-center text-slate-500 shadow-sm shrink-0">
                    <Mail size={18} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Hòm thư Email</p>
                    <p className="text-sm font-bold text-slate-800 truncate">{user.email || 'N/A'}</p>
                  </div>
                </div>

                <div className="flex items-center gap-4 p-3 bg-slate-50 rounded-2xl border border-slate-100/50">
                  <div className="w-10 h-10 rounded-xl bg-white border border-slate-100 flex items-center justify-center text-slate-500 shadow-sm shrink-0">
                    <KeyRound size={18} />
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Mã số giảng viên (MSV)</p>
                    <p className="text-sm font-bold text-slate-800 font-mono">{user.userCode || 'N/A'}</p>
                  </div>
                </div>

                <div className="flex items-center gap-4 p-3 bg-slate-50 rounded-2xl border border-slate-100/50 md:col-span-2">
                  <div className="w-10 h-10 rounded-xl bg-white border border-slate-100 flex items-center justify-center text-slate-500 shadow-sm shrink-0">
                    <Shield size={18} />
                  </div>
                  <div>
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">Quyền hạn hệ thống</p>
                    <span className="inline-block px-2.5 py-0.5 bg-red-50 border border-red-100 text-red-600 rounded-lg text-[10px] font-black uppercase mt-1">
                      {user.platformRole || 'LECTURER'}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Đổi mật khẩu */}
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 overflow-hidden relative">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Bảo mật mật khẩu</h3>
                {!isChangingPass && (
                  <button 
                    onClick={() => setIsChangingPass(true)}
                    className="text-xs font-extrabold text-red-600 hover:text-red-700 transition-colors"
                  >
                    Đổi mật khẩu
                  </button>
                )}
              </div>

              {isChangingPass ? (
                <form onSubmit={submitChangePassword} className="space-y-4 animate-in slide-in-from-top-2 duration-300">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                      <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1.5 ml-1">MẬT KHẨU HIỆN TẠI</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={15} />
                        <input 
                          type="password"
                          name="currentPassword"
                          value={passData.currentPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 focus:ring-1 focus:ring-red-400 outline-none transition-all"
                          placeholder="••••••••"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1.5 ml-1">MẬT KHẨU MỚI</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={15} />
                        <input 
                          type="password"
                          name="newPassword"
                          value={passData.newPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 focus:ring-1 focus:ring-red-400 outline-none transition-all"
                          placeholder="Tối thiểu 8 ký tự"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-[10px] font-bold text-slate-400 uppercase mb-1.5 ml-1">XÁC NHẬN MẬT KHẨU MỚI</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400" size={15} />
                        <input 
                          type="password"
                          name="confirmPassword"
                          value={passData.confirmPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 focus:ring-1 focus:ring-red-400 outline-none transition-all"
                          placeholder="Nhập lại mật khẩu mới"
                        />
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-3 mt-6">
                    <button 
                      type="button"
                      onClick={() => setIsChangingPass(false)}
                      className="flex-1 px-5 py-3 border border-slate-200 rounded-2xl text-xs font-bold text-slate-600 hover:bg-slate-50 transition-colors"
                    >
                      Hủy bỏ
                    </button>
                    <button 
                      type="submit"
                      disabled={isLoading}
                      className="flex-1 px-5 py-3 bg-red-600 hover:bg-red-700 text-white rounded-2xl text-xs font-bold transition-all flex items-center justify-center gap-2 hover:scale-[1.02] active:scale-[0.98]"
                    >
                      {isLoading ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
                      Lưu mật khẩu mới
                    </button>
                  </div>
                </form>
              ) : (
                <div className="flex items-center gap-3.5 p-4 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                  <div className="p-2 bg-white rounded-xl shadow-sm shrink-0 border border-slate-100">
                    <Lock size={16} className="text-slate-400" />
                  </div>
                  <p className="text-xs text-slate-400 font-medium">Bạn nên thay đổi mật khẩu định kỳ để nâng cao tính bảo mật cho cổng thông tin điểm danh.</p>
                </div>
              )}
            </div>
          </div>

          {/* Cột phải: Live Geolocation & Logout All Devices */}
          <div className="space-y-6">
            
            {/* Geolocation & Current Device Terminal */}
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 relative overflow-hidden">
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-5">Định vị & Thiết bị</h3>
               
              <div className="space-y-4">
                {/* Thiết bị hiện tại */}
                <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl flex items-start gap-3.5">
                  <div className="p-2.5 bg-white border border-slate-100 rounded-xl text-blue-500 shadow-sm shrink-0">
                    <Smartphone size={18} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-800">Trình duyệt hiện tại</p>
                    <p className="text-[10px] text-slate-400 font-mono mt-1 select-all truncate" title={localStorage.getItem('web_device_id')}>
                      ID: {localStorage.getItem('web_device_id')?.substring(0,16) || 'Unknown'}...
                    </p>
                  </div>
                </div>

                {/* Định vị GPS thời gian thực */}
                <div className="p-4 bg-red-50/30 border border-red-100/50 rounded-2xl flex items-start gap-3.5 relative overflow-hidden">
                  {/* Live Pulse Dot */}
                  <div className="absolute top-4 right-4 w-2 h-2 rounded-full bg-red-500">
                    <div className="absolute inset-0 rounded-full bg-red-400 animate-ping opacity-75"></div>
                  </div>
                  <div className="p-2.5 bg-white border border-slate-100 rounded-xl text-red-500 shadow-sm shrink-0">
                    <MapPin size={18} />
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-bold text-slate-800">Tọa độ GPS hiện tại</p>
                    <p className="text-[10px] text-red-600 font-bold font-mono mt-1">
                      {currentLocation}
                    </p>
                    {locationDetails && (
                      <p className="text-[10px] text-slate-500 font-semibold mt-1 italic leading-relaxed">
                        📍 {locationDetails}
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* Đăng xuất toàn bộ */}
              <div className="border-t border-slate-100 pt-5 mt-5">
                <p className="text-[11px] text-slate-400 leading-relaxed mb-4">Bạn có thể đăng xuất khỏi toàn bộ các thiết bị khác (bao gồm cả trình duyệt này) để đảm bảo an toàn tuyệt đối nếu phát hiện bất thường.</p>
                <button 
                  onClick={() => setShowLogoutAllConfirm(true)}
                  className="w-full flex items-center justify-center gap-2 px-4 py-3 bg-red-50 hover:bg-red-100 border border-red-100 text-red-600 rounded-2xl text-xs font-bold hover:scale-[1.02] active:scale-[0.98] transition-all"
                >
                  <LogOut size={14} />
                  Đăng xuất toàn bộ thiết bị
                </button>
              </div>
            </div>

            {/* Quay về Dashboard */}
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-2">
               <button 
                onClick={() => navigate('/dashboard')}
                className="w-full flex items-center justify-between p-3.5 rounded-2xl hover:bg-slate-50 transition-all group"
               >
                 <div className="flex items-center gap-3">
                   <div className="p-2 bg-slate-50 border border-slate-100 rounded-xl text-slate-400 group-hover:text-red-500 transition-colors">
                     <User size={18} />
                   </div>
                   <span className="text-xs font-extrabold text-slate-700">Trở lại Dashboard</span>
                 </div>
                 <ChevronLeft size={16} className="text-slate-300 rotate-180 group-hover:translate-x-0.5 transition-transform" />
               </button>
            </div>
          </div>
        </div>

      </main>

      {/* Dialog xác nhận Đăng xuất toàn bộ */}
      <ConfirmDialog
        isOpen={showLogoutAllConfirm}
        onClose={() => setShowLogoutAllConfirm(false)}
        onConfirm={handleLogoutAllDevices}
        title="Đăng xuất toàn bộ"
        message="Hành động này sẽ đăng xuất tài khoản giảng viên khỏi TOÀN BỘ các trình duyệt và thiết bị di động đang đăng nhập. Bạn có chắc chắn muốn tiếp tục?"
      />
    </div>
  );

  if (isModal) {
    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
        {/* Backdrop che mờ sang trọng */}
        <div 
          className="absolute inset-0 bg-slate-950/40 backdrop-blur-[8px] animate-in fade-in duration-300"
          onClick={() => navigate(-1)}
        ></div>
        {/* Container Modal căn giữa với hiệu ứng scale-in */}
        <div className="relative w-full max-w-4xl animate-in zoom-in-95 duration-300">
          {content}
        </div>
      </div>
    );
  }

  return content;
}
