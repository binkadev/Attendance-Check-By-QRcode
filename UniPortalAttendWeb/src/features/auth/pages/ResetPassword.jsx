import React, { useState, useEffect } from 'react';
import { 
  Lock, ArrowLeft, Loader2, Shield, 
  CheckCircle2, AlertCircle, Eye, EyeOff
} from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';

export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [formData, setFormData] = useState({ newPassword: '', confirmPassword: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);

  const calculatePasswordStrength = (password) => {
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.length >= 10) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    setPasswordStrength(Math.min(strength, 4));
  };

  const getStrengthColor = () => {
    const colors = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-green-500'];
    return colors[passwordStrength - 1] || 'bg-gray-200';
  };

  const getStrengthText = () => {
    const texts = ['Rất yếu', 'Yếu', 'Trung bình', 'Mạnh'];
    return texts[passwordStrength - 1] || '';
  };

  useEffect(() => {
    if (!token) {
      toast.error('Mã xác thực không hợp lệ hoặc đã hết hạn.');
      setTimeout(() => navigate('/login'), 3000);
    }
  }, [token, navigate]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.newPassword !== formData.confirmPassword) {
      return toast.error('Mật khẩu không khớp!');
    }
    if (formData.newPassword.length < 8) {
      return toast.error('Mật khẩu phải từ 8 ký tự trở lên.');
    }
    if (passwordStrength < 2) {
      return toast.error('Mật khẩu quá yếu! Vui lòng thêm số hoặc chữ hoa.');
    }

    setIsLoading(true);
    try {
      await authApi.resetPassword({
        token: token,
        newPassword: formData.newPassword
      });
      setIsSuccess(true);
      toast.success('Đổi mật khẩu thành công!');
    } catch (err) {
      toast.error(err.message || 'Lỗi khi đặt lại mật khẩu.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
      <header className="w-full h-20 bg-white flex items-center px-8 lg:px-24 border-b border-gray-100">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-red-600 rounded-xl flex items-center justify-center shadow-sm">
            <Shield className="text-white" size={24} />
          </div>
          <h1 className="text-2xl font-bold text-gray-900 tracking-tight">UniAttend</h1>
        </div>
      </header>

      <main className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md bg-white rounded-3xl shadow-xl shadow-gray-200/50 p-8 border border-gray-100">
          
          {!isSuccess ? (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">Đặt lại mật khẩu</h2>
              <p className="text-gray-500 text-sm mb-8">
                Vui lòng nhập mật khẩu mới cho tài khoản của bạn.
              </p>

              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Mật khẩu mới</label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                    <input 
                      type={showPassword ? "text" : "password"}
                      value={formData.newPassword}
                      onChange={(e) => {
                        setFormData({ ...formData, newPassword: e.target.value });
                        calculatePasswordStrength(e.target.value);
                      }}
                      required
                      className="w-full bg-gray-50 border border-gray-100 rounded-xl pl-12 pr-12 py-3.5 text-sm outline-none focus:bg-white focus:border-red-400 transition-all"
                      placeholder="••••••••"
                    />
                    <button 
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                    </button>
                  </div>

                  {/* Password Strength Meter */}
                  {formData.newPassword && (
                    <div className="mt-3 animate-in fade-in slide-in-from-top-1 duration-300">
                      <div className="flex justify-between items-center mb-1.5">
                        <span className="text-[10px] font-bold text-gray-400 uppercase">Độ mạnh mật khẩu</span>
                        <span className={`text-[10px] font-bold uppercase ${getStrengthColor().replace('bg-', 'text-')}`}>
                          {getStrengthText()}
                        </span>
                      </div>
                      <div className="flex gap-1 h-1.5">
                        {[1, 2, 3, 4].map((step) => (
                          <div 
                            key={step}
                            className={`flex-1 rounded-full transition-all duration-500 ${
                              step <= passwordStrength ? getStrengthColor() : 'bg-gray-100'
                            }`}
                          />
                        ))}
                      </div>
                      <p className="mt-2 text-[10px] text-gray-400 leading-relaxed">
                        Gợi ý: Sử dụng ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và chữ số.
                      </p>
                    </div>
                  )}
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Xác nhận mật khẩu</label>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                    <input 
                      type={showPassword ? "text" : "password"}
                      value={formData.confirmPassword}
                      onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                      required
                      className="w-full bg-gray-50 border border-gray-100 rounded-xl pl-12 pr-12 py-3.5 text-sm outline-none focus:bg-white focus:border-red-400 transition-all"
                      placeholder="••••••••"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isLoading || !token}
                  className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3.5 rounded-xl transition-all shadow-lg shadow-red-200 flex items-center justify-center gap-2"
                >
                  {isLoading ? <Loader2 size={20} className="animate-spin" /> : "Cập nhật mật khẩu"}
                </button>
              </form>
            </div>
          ) : (
            <div className="text-center animate-in zoom-in-95 duration-500">
              <div className="w-20 h-20 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="text-green-500" size={40} />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">Thành công!</h2>
              <p className="text-gray-500 text-sm mb-8">
                Mật khẩu của bạn đã được thay đổi thành công. Bạn có thể sử dụng mật khẩu mới để đăng nhập ngay bây giờ.
              </p>
              <button
                onClick={() => navigate('/login')}
                className="w-full bg-gray-900 hover:bg-black text-white font-bold py-3.5 rounded-xl transition-all"
              >
                Đăng nhập ngay
              </button>
            </div>
          )}
        </div>
      </main>

      <footer className="w-full h-16 bg-white flex items-center justify-center px-8 text-xs text-gray-400 border-t border-gray-100">
        &copy; 2026 UniAttend. Mọi quyền được bảo lưu.
      </footer>
    </div>
  );
}
