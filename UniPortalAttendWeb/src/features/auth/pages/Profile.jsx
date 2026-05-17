import React, { useState, useEffect, useRef } from 'react';
import { 
  User, Mail, Shield, Lock, KeyRound, 
  Save, LogOut, ChevronLeft, Loader2,
  Camera, BadgeCheck, Smartphone, X
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';

export default function Profile({ isModal = false }) {
  const navigate = useNavigate();
  const fileInputRef = useRef(null);
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
  const [locationDetails, setLocationDetails] = useState(''); // State mới cho chi tiết địa chỉ

  useEffect(() => {
    if ("geolocation" in navigator) {
      // Cấu hình ép trình duyệt lấy vị trí chính xác nhất có thể
      const geoOptions = {
        enableHighAccuracy: true, // Bật độ chính xác cao (ưu tiên GPS/Wi-Fi tầm gần thay vì IP)
        timeout: 10000,           // Thời gian chờ tối đa là 10 giây
        maximumAge: 0             // Không sử dụng lại vị trí cũ trong bộ nhớ cache
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
        geoOptions // Truyền cấu hình vào đây
      );
    } else {
      setCurrentLocation('Trình duyệt không hỗ trợ GPS');
    }
  }, []);

  const handleSaveProfile = async () => {
    setIsLoading(true);
    try {
      // Gọi API cập nhật (Giả định)
      await authApi.updateProfile({ fullName: editedUser.fullName });
      
      // Cập nhật local storage
      const updatedUser = { ...user, fullName: editedUser.fullName };
      localStorage.setItem('user', JSON.stringify(updatedUser));
      setUser(updatedUser);
      
      toast.success('Cập nhật thông tin thành công!');
      setIsEditingInfo(false);
    } catch (err) {
      // Nếu API thật chưa có, vẫn cho phép cập nhật local để test UI
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
        
        // Lưu vào object user hiện tại
        const updatedUser = { ...user, avatar: base64String };
        localStorage.setItem('user', JSON.stringify(updatedUser));
        
        // Lưu vào một kho riêng không bị xóa khi logout (gắn với ID hoặc Email)
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

  const content = (
    <div className={`flex flex-col ${isModal ? 'w-full max-w-4xl bg-white rounded-3xl shadow-2xl overflow-hidden max-h-[90vh]' : 'min-h-screen bg-gray-50'}`}>
      {/* Header */}
      <header className="h-16 bg-white border-b border-gray-200 flex items-center px-6 sticky top-0 z-10">
        <button 
          onClick={() => navigate(-1)}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors mr-4"
        >
          {isModal ? <X size={20} className="text-gray-600" /> : <ChevronLeft size={20} className="text-gray-600" />}
        </button>
        <h1 className="text-lg font-bold text-gray-900">Thông tin cá nhân</h1>
      </header>

      <main className={`flex-1 overflow-y-auto ${isModal ? 'p-6' : 'max-w-4xl mx-auto w-full p-6'} space-y-6`}>
        
        {/* Profile Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="h-32 bg-gradient-to-r from-red-600 to-orange-500 relative">
             <div className="absolute -bottom-12 left-8">
                <div className="relative group">
                  <div className="w-24 h-24 rounded-2xl bg-white p-1 shadow-lg overflow-hidden">
                    <div className="w-full h-full rounded-xl bg-gray-100 flex items-center justify-center text-red-600 font-bold text-3xl overflow-hidden">
                      {user.avatar ? (
                        <img src={user.avatar} alt="avatar" className="w-full h-full object-cover" />
                      ) : (
                        user.fullName?.charAt(0).toUpperCase() || 'U'
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
                    className="absolute bottom-0 right-0 p-1.5 bg-white rounded-lg shadow-md border border-gray-100 text-gray-500 hover:text-red-600 transition-colors"
                  >
                    <Camera size={14} />
                  </button>
                </div>
             </div>
          </div>
          
          <div className="pt-16 pb-8 px-8 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
            <div>
              <div className="flex items-center gap-2">
                {isEditingInfo ? (
                  <input 
                    type="text"
                    value={editedUser.fullName}
                    onChange={(e) => setEditedUser({ ...editedUser, fullName: e.target.value })}
                    className="text-2xl font-bold text-gray-900 border-b-2 border-red-500 outline-none bg-transparent"
                    autoFocus
                  />
                ) : (
                  <h2 className="text-2xl font-bold text-gray-900">{user.fullName || 'Chưa cập nhật'}</h2>
                )}
                <BadgeCheck size={20} className="text-blue-500 fill-blue-50" />
              </div>
              <p className="text-gray-500 font-medium">Giảng viên hệ thống</p>
            </div>
            <div className="flex gap-3 w-full md:w-auto">
               {isEditingInfo ? (
                 <>
                   <button 
                    onClick={() => { setIsEditingInfo(false); setEditedUser({ fullName: user.fullName }); }}
                    className="flex-1 md:flex-none px-6 py-2.5 bg-gray-100 border border-gray-200 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-200 transition-colors"
                   >
                     Hủy
                   </button>
                   <button 
                    onClick={handleSaveProfile}
                    disabled={isLoading}
                    className="flex-1 md:flex-none px-6 py-2.5 bg-red-600 text-white rounded-xl text-sm font-bold hover:bg-red-700 shadow-md shadow-red-200 transition-all flex items-center justify-center gap-2"
                   >
                     {isLoading ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                     Lưu lại
                   </button>
                 </>
               ) : (
                 <button 
                  onClick={() => setIsEditingInfo(true)}
                  className="flex-1 md:flex-none px-6 py-2.5 bg-white border border-gray-200 rounded-xl text-sm font-bold text-gray-700 hover:bg-gray-50 transition-colors flex items-center justify-center gap-2"
                 >
                   Chỉnh sửa hồ sơ
                 </button>
               )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
              <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-6">Thông tin liên hệ</h3>
              <div className="space-y-4">
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-gray-400">
                    <Mail size={18} />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold text-gray-400 uppercase">Email</p>
                    <p className="text-sm font-semibold text-gray-900">{user.email || 'N/A'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-gray-400">
                    <KeyRound size={18} />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold text-gray-400 uppercase">Mã giảng viên</p>
                    <p className="text-sm font-semibold text-gray-900">{user.userCode || 'N/A'}</p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="w-10 h-10 rounded-xl bg-gray-50 flex items-center justify-center text-gray-400">
                    <Shield size={18} />
                  </div>
                  <div>
                    <p className="text-[11px] font-bold text-gray-400 uppercase">Vai trò</p>
                    <span className="inline-block px-2 py-0.5 bg-red-50 text-red-600 rounded text-[10px] font-bold border border-red-100 uppercase mt-1">
                      {user.platformRole || 'LECTURER'}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 overflow-hidden relative">
              <div className="flex items-center justify-between mb-6">
                <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider">Đổi mật khẩu</h3>
                {!isChangingPass && (
                  <button 
                    onClick={() => setIsChangingPass(true)}
                    className="text-sm font-bold text-red-600 hover:text-red-700"
                  >
                    Thay đổi
                  </button>
                )}
              </div>

              {isChangingPass ? (
                <form onSubmit={submitChangePassword} className="space-y-4 animate-in slide-in-from-top-2 duration-300">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="md:col-span-2">
                      <label className="block text-xs font-bold text-gray-500 mb-1.5 ml-1">MẬT KHẨU HIỆN TẠI</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input 
                          type="password"
                          name="currentPassword"
                          value={passData.currentPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 outline-none transition-all"
                          placeholder="••••••••"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-gray-500 mb-1.5 ml-1">MẬT KHẨU MỚI</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input 
                          type="password"
                          name="newPassword"
                          value={passData.newPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 outline-none transition-all"
                          placeholder="Tối thiểu 8 ký tự"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-gray-500 mb-1.5 ml-1">XÁC NHẬN MẬT KHẨU</label>
                      <div className="relative">
                        <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                        <input 
                          type="password"
                          name="confirmPassword"
                          value={passData.confirmPassword}
                          onChange={handlePassChange}
                          required
                          className="w-full bg-gray-50 border border-gray-200 rounded-xl pl-11 pr-4 py-3 text-sm focus:bg-white focus:border-red-400 outline-none transition-all"
                          placeholder="Nhập lại mật khẩu mới"
                        />
                      </div>
                    </div>
                  </div>
                  <div className="flex gap-3 mt-6">
                    <button 
                      type="button"
                      onClick={() => setIsChangingPass(false)}
                      className="flex-1 px-6 py-3 border border-gray-200 rounded-xl text-sm font-bold text-gray-600 hover:bg-gray-50 transition-colors"
                    >
                      Hủy bỏ
                    </button>
                    <button 
                      type="submit"
                      disabled={isLoading}
                      className="flex-1 px-6 py-3 bg-red-600 text-white rounded-xl text-sm font-bold hover:bg-red-700 shadow-md shadow-red-200 transition-all flex items-center justify-center gap-2"
                    >
                      {isLoading ? <Loader2 size={18} className="animate-spin" /> : <Save size={18} />}
                      Lưu mật khẩu
                    </button>
                  </div>
                </form>
              ) : (
                <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-xl border border-dashed border-gray-200">
                  <div className="p-2 bg-white rounded-lg shadow-sm">
                    <Lock size={16} className="text-gray-400" />
                  </div>
                  <p className="text-xs text-gray-500 font-medium">Bạn nên thay đổi mật khẩu định kỳ để bảo vệ tài khoản tốt hơn.</p>
                </div>
              )}
            </div>
          </div>

          <div className="space-y-6">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6">
               <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">Trình duyệt & Thiết bị</h3>
               
               <div className="flex items-start gap-3 p-3 bg-blue-50 border border-blue-100 rounded-xl mb-4">
                  <Smartphone size={18} className="text-blue-500 mt-0.5" />
                  <div>
                    <p className="text-xs font-bold text-blue-900">Thiết bị hiện tại (Phiên này)</p>
                    <p className="text-[10px] text-blue-700 mt-1">
                      Mã ID: {localStorage.getItem('web_device_id') || 'Unknown'}
                    </p>
                    <p className="text-[10px] text-blue-700 mt-0.5">
                      Vị trí: {currentLocation}
                    </p>
                    {/* Hiển thị chi tiết địa chỉ */}
                    {locationDetails && (
                      <p className="text-[10px] text-blue-800 font-medium mt-0.5 italic">
                        {locationDetails}
                      </p>
                    )}
                  </div>
               </div>

               <div className="border-t border-gray-100 pt-4">
                 <p className="text-xs text-gray-500 mb-3">Bạn có thể đăng xuất khỏi tất cả các thiết bị khác (bao gồm cả trình duyệt này) nếu phát hiện hoạt động đáng ngờ.</p>
                 <button 
                  onClick={() => {
                    if(window.confirm('Bạn có chắc chắn muốn đăng xuất khỏi TOÀN BỘ thiết bị?')) {
                      authApi.logoutAll().then(() => { 
                        toast.success('Đã đăng xuất toàn bộ thiết bị'); 
                        navigate('/login'); 
                      }).catch(err => {
                        toast.error('Lỗi khi đăng xuất toàn bộ');
                      })
                    }
                  }}
                  className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-red-50 text-red-600 border border-red-100 rounded-xl text-sm font-bold hover:bg-red-100 transition-colors"
                 >
                   <LogOut size={16} />
                   Đăng xuất toàn bộ
                 </button>
               </div>
            </div>

            <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-2">
               <button 
                onClick={() => navigate('/dashboard')}
                className="w-full flex items-center gap-3 p-4 rounded-xl hover:bg-gray-50 transition-colors group"
               >
                 <div className="p-2 bg-gray-50 rounded-lg group-hover:bg-white transition-colors">
                   <User size={18} className="text-gray-400 group-hover:text-red-500" />
                 </div>
                 <span className="text-sm font-bold text-gray-700">Trở về Dashboard</span>
               </button>
            </div>
          </div>
        </div>

      </main>
    </div>
  );

  if (isModal) {
    return (
      <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
        {/* Backdrop */}
        <div 
          className="absolute inset-0 bg-black/40 backdrop-blur-[6px] animate-in fade-in duration-300"
          onClick={() => navigate(-1)}
        ></div>
        {/* Modal Container */}
        <div className="relative w-full max-w-4xl animate-in zoom-in-95 fade-in duration-300">
          {content}
        </div>
      </div>
    );
  }

  return content;
}
