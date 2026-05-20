import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Lock, Key, Eye, EyeOff, Loader2, 
  ShieldAlert, ShieldCheck, AlertCircle, 
  CheckCircle2, ArrowRight, Shield
} from 'lucide-react';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';
import { useLanguage } from '../../../context/LanguageContext';
import LanguageSwitcher from '../../../components/layout/LanguageSwitcher';

export default function ForceChangePassword() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  
  const [isLoading, setIsLoading] = useState(false);
  const [strength, setStrength] = useState({ score: 0, label: 'Rỗng', color: 'bg-gray-200' });
  const [focusedField, setFocusedField] = useState(null);

  // Lấy thông tin user đăng nhập
  const currentUser = JSON.parse(localStorage.getItem('user') || '{}');

  // Đánh giá độ mạnh mật khẩu thời gian thực
  const evalPasswordStrength = (pwd) => {
    if (!pwd) return { score: 0, label: 'Rỗng', color: 'bg-gray-200 text-gray-400' };
    
    let score = 0;
    if (pwd.length >= 8) score++;
    if (/[A-Z]/.test(pwd)) score++;
    if (/[a-z]/.test(pwd)) score++;
    if (/[0-9]/.test(pwd)) score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;

    switch (score) {
      case 1:
      case 2:
        return { score: 1, label: 'Yếu', color: 'bg-red-500 text-red-500' };
      case 3:
      case 4:
        return { score: 2, label: 'Trung bình', color: 'bg-amber-500 text-amber-500' };
      case 5:
        return { score: 3, label: 'Mạnh 🔥', color: 'bg-emerald-500 text-emerald-500' };
      default:
        return { score: 1, label: 'Yếu', color: 'bg-red-500 text-red-500' };
    }
  };

  useEffect(() => {
    setStrength(evalPasswordStrength(formData.newPassword));
  }, [formData.newPassword]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleForceChange = async (e) => {
    e.preventDefault();
    
    // Kiểm tra tính hợp lệ dữ liệu
    if (!formData.currentPassword) {
      toast.error("Vui lòng nhập mật khẩu hiện tại!");
      return;
    }
    
    if (formData.newPassword.length < 8) {
      toast.error("Mật khẩu mới phải chứa ít nhất 8 ký tự!");
      return;
    }

    if (formData.newPassword === formData.currentPassword) {
      toast.error("Mật khẩu mới không được trùng với mật khẩu mặc định cũ!");
      return;
    }
    
    if (formData.newPassword !== formData.confirmPassword) {
      toast.error("Xác nhận mật khẩu mới không khớp!");
      return;
    }

    setIsLoading(true);
    const loadingToastId = toast.loading("Đang thiết lập mật khẩu mới...");

    try {
      const payload = {
        currentPassword: formData.currentPassword,
        newPassword: formData.newPassword
      };

      await authApi.forceChangePassword(payload);

      // Cập nhật lại thông tin user trong LocalStorage (xóa bỏ cờ đăng nhập lần đầu)
      const updatedUser = {
        ...currentUser,
        isFirstLogin: false,
        requirePasswordChange: false
      };
      localStorage.setItem('user', JSON.stringify(updatedUser));

      toast.success("Thay đổi mật khẩu thành công! Chào mừng bạn đến với UniPortal.", { id: loadingToastId });
      
      setTimeout(() => {
        navigate('/dashboard');
      }, 800);
      
    } catch (err) {
      toast.error(err.message || "Thay đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu cũ.", { id: loadingToastId });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white font-sans flex flex-col">
      {/* Header */}
      <header className="w-full h-20 bg-white flex items-center px-8 lg:px-24 border-b border-gray-100 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-red-600 rounded-xl flex items-center justify-center shadow-sm">
            <Shield className="text-white" size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 tracking-tight">{t('common.brand') || 'UniPortal'}</h1>
          </div>
        </div>
        <div className="ml-auto flex items-center gap-6 text-sm text-gray-500">
          <LanguageSwitcher />
        </div>
      </header>

      {/* Main Body */}
      <main className="flex-1 relative bg-gradient-to-br from-red-50 via-white to-orange-50 flex items-center justify-center py-12 px-4">
        {/* Background Patterns */}
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
           <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-red-100 rounded-full blur-3xl opacity-20 transform translate-x-1/3 -translate-y-1/4"></div>
           <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-orange-100 rounded-full blur-3xl opacity-20 transform -translate-x-1/3 translate-y-1/4"></div>
        </div>

        <div className="w-full max-w-[480px] relative z-10">
          <div className="bg-white/95 backdrop-blur-md rounded-2xl shadow-[0_12px_40px_rgba(0,0,0,0.06)] border border-gray-100 overflow-hidden">
            
            {/* Top Indicator */}
            <div className="h-1.5 bg-gradient-to-r from-red-600 to-orange-500 w-full" />
            
            <div className="p-8">
              {/* Header Title */}
              <div className="text-center mb-8">
                <div className="w-14 h-14 rounded-full bg-red-50 border border-red-100 text-red-600 flex items-center justify-center mx-auto mb-4 shadow-sm animate-pulse">
                  <ShieldAlert size={26} />
                </div>
                <h2 className="text-xl font-bold text-gray-900 leading-tight">Yêu cầu đổi mật khẩu mặc định</h2>
                <p className="text-sm text-gray-500 mt-2">
                  Chào mừng học viên <strong className="text-gray-800">{currentUser.fullName || 'mới'}</strong>. Vì lý do bảo mật an toàn thông tin, hệ thống bắt buộc bạn phải thay đổi mật khẩu mặc định cũ để tiếp tục.
                </p>
              </div>

              {/* Security Hint */}
              <div className="mb-6 p-4 bg-blue-50/60 border border-blue-100 rounded-xl flex gap-3 text-left">
                <ShieldCheck size={20} className="text-blue-600 shrink-0 mt-0.5" />
                <div className="text-xs text-blue-900 leading-relaxed">
                  <strong className="font-bold text-blue-950 block mb-0.5">Quy tắc đặt mật khẩu bảo mật:</strong>
                  • Chứa ít nhất **8 ký tự** trở lên.<br />
                  • Gồm chữ hoa, chữ thường và chữ số.<br />
                  • Không được trùng lại với mật khẩu cũ (MSV + Họ tên).
                </div>
              </div>

              {/* Form */}
              <form onSubmit={handleForceChange} className="space-y-5">
                {/* 1. Current Password */}
                <div>
                  <label className="text-[13px] font-bold text-gray-700 block mb-1">Mật khẩu mặc định hiện tại</label>
                  <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'currentPassword' ? 'border-red-600' : 'border-gray-200'}`}>
                    <Key size={18} className={focusedField === 'currentPassword' ? 'text-red-600' : 'text-gray-400'} />
                    <input
                      type={showCurrent ? "text" : "password"}
                      name="currentPassword"
                      value={formData.currentPassword}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('currentPassword')}
                      onBlur={() => setFocusedField(null)}
                      required
                      placeholder="Mã sinh viên + tên viết liền"
                      className="w-full px-3 py-2.5 bg-transparent text-sm text-gray-950 focus:outline-none placeholder-gray-400 font-medium"
                    />
                    <button
                      type="button"
                      onClick={() => setShowCurrent(!showCurrent)}
                      className="p-1 focus:outline-none shrink-0"
                    >
                      {showCurrent ? <EyeOff size={18} className="text-gray-400" /> : <Eye size={18} className="text-gray-400" />}
                    </button>
                  </div>
                </div>

                {/* 2. New Password */}
                <div>
                  <label className="text-[13px] font-bold text-gray-700 block mb-1">Mật khẩu mới</label>
                  <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'newPassword' ? 'border-red-600' : 'border-gray-200'}`}>
                    <Lock size={18} className={focusedField === 'newPassword' ? 'text-red-600' : 'text-gray-400'} />
                    <input
                      type={showNew ? "text" : "password"}
                      name="newPassword"
                      value={formData.newPassword}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('newPassword')}
                      onBlur={() => setFocusedField(null)}
                      required
                      placeholder="Thiết lập mật khẩu bảo mật mới"
                      className="w-full px-3 py-2.5 bg-transparent text-sm text-gray-950 focus:outline-none placeholder-gray-400 font-medium"
                    />
                    <button
                      type="button"
                      onClick={() => setShowNew(!showNew)}
                      className="p-1 focus:outline-none shrink-0"
                    >
                      {showNew ? <EyeOff size={18} className="text-gray-400" /> : <Eye size={18} className="text-gray-400" />}
                    </button>
                  </div>

                  {/* Strength Bar */}
                  {formData.newPassword && (
                    <div className="mt-2.5 animate-in fade-in slide-in-from-top-1 duration-250">
                      <div className="flex justify-between items-center text-xs mb-1">
                        <span className="text-gray-500">Mức độ bảo mật:</span>
                        <span className={`font-bold ${strength.color.replace('bg-', 'text-')}`}>{strength.label}</span>
                      </div>
                      <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden flex gap-0.5">
                        <div className={`h-full rounded-full transition-all duration-300 ${strength.score >= 1 ? strength.color : 'bg-gray-100'}`} style={{ width: strength.score >= 1 ? '33.33%' : '0%' }} />
                        <div className={`h-full rounded-full transition-all duration-300 ${strength.score >= 2 ? strength.color : 'bg-gray-100'}`} style={{ width: strength.score >= 2 ? '33.33%' : '0%' }} />
                        <div className={`h-full rounded-full transition-all duration-300 ${strength.score >= 3 ? strength.color : 'bg-gray-100'}`} style={{ width: strength.score >= 3 ? '33.34%' : '0%' }} />
                      </div>
                    </div>
                  )}
                </div>

                {/* 3. Confirm Password */}
                <div>
                  <label className="text-[13px] font-bold text-gray-700 block mb-1">Xác nhận mật khẩu mới</label>
                  <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'confirmPassword' ? 'border-red-600' : 'border-gray-200'}`}>
                    <Lock size={18} className={focusedField === 'confirmPassword' ? 'text-red-600' : 'text-gray-400'} />
                    <input
                      type={showConfirm ? "text" : "password"}
                      name="confirmPassword"
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('confirmPassword')}
                      onBlur={() => setFocusedField(null)}
                      required
                      placeholder="Nhập lại mật khẩu mới để đối chiếu"
                      className="w-full px-3 py-2.5 bg-transparent text-sm text-gray-950 focus:outline-none placeholder-gray-400 font-medium"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirm(!showConfirm)}
                      className="p-1 focus:outline-none shrink-0"
                    >
                      {showConfirm ? <EyeOff size={18} className="text-gray-400" /> : <Eye size={18} className="text-gray-400" />}
                    </button>
                  </div>
                  
                  {/* Warning mismatched */}
                  {formData.confirmPassword && formData.newPassword !== formData.confirmPassword && (
                    <div className="mt-2 text-red-500 text-xs font-semibold flex items-center gap-1 animate-in fade-in duration-200">
                      <AlertCircle size={13} />
                      Mật khẩu xác nhận không trùng khớp!
                    </div>
                  )}
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  disabled={isLoading || !formData.newPassword || formData.newPassword !== formData.confirmPassword}
                  className="w-full mt-2 bg-gradient-to-r from-red-600 to-orange-500 hover:from-red-700 hover:to-orange-600 text-white font-bold py-3.5 rounded-xl transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed shadow-md shadow-red-200 flex items-center justify-center gap-2 active:scale-[0.98]"
                >
                  {isLoading ? (
                    <>
                      <Loader2 size={18} className="animate-spin" />
                      Đang xử lý đổi mật khẩu...
                    </>
                  ) : (
                    <>
                      Xác nhận đổi mật khẩu & Vào lớp
                      <ArrowRight size={16} />
                    </>
                  )}
                </button>
              </form>
            </div>

          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="py-6 bg-white border-t border-gray-100 shrink-0 text-center text-xs text-gray-400">
        <p>© {new Date().getFullYear()} UniPortal. Bảo mật dữ liệu & Thông tin cá nhân được bảo vệ tối đa.</p>
      </footer>
    </div>
  );
}
