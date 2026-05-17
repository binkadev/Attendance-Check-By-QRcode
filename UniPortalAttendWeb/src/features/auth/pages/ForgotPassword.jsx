import React, { useState } from 'react';
import { 
  Mail, ArrowLeft, Loader2, Shield, 
  Send, CheckCircle2, AlertCircle
} from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSuccess, setIsSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!email) return toast.error('Vui lòng nhập email');

    setIsLoading(true);
    try {
      await authApi.forgotPassword({ email });
      setIsSuccess(true);
      toast.success('Yêu cầu đã được gửi!');
    } catch (err) {
      toast.error(err.message || 'Có lỗi xảy ra, vui lòng thử lại sau.');
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
          
          <button 
            onClick={() => navigate('/login')}
            className="flex items-center gap-2 text-sm text-gray-500 hover:text-red-600 transition-colors mb-8 group"
          >
            <ArrowLeft size={16} className="group-hover:-translate-x-1 transition-transform" />
            Trở lại đăng nhập
          </button>

          {!isSuccess ? (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h2 className="text-2xl font-bold text-gray-900 mb-2">Quên mật khẩu?</h2>
              <p className="text-gray-500 text-sm mb-8">
                Nhập email của bạn để nhận liên kết đặt lại mật khẩu.
              </p>

              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-xs font-bold text-gray-400 uppercase mb-2 ml-1">Địa chỉ Email</label>
                  <div className="relative">
                    <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                    <input 
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      className="w-full bg-gray-50 border border-gray-100 rounded-xl pl-12 pr-4 py-3.5 text-sm outline-none focus:bg-white focus:border-red-400 focus:ring-4 focus:ring-red-500/5 transition-all"
                      placeholder="name@example.com"
                    />
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="w-full bg-red-600 hover:bg-red-700 text-white font-bold py-3.5 rounded-xl transition-all shadow-lg shadow-red-200 flex items-center justify-center gap-2"
                >
                  {isLoading ? <Loader2 size={20} className="animate-spin" /> : <Send size={20} />}
                  Gửi yêu cầu
                </button>
              </form>

              <div className="mt-8 p-4 bg-orange-50 rounded-2xl border border-orange-100 flex items-start gap-3">
                <AlertCircle className="text-orange-500 shrink-0 mt-0.5" size={18} />
                <p className="text-xs text-orange-700 leading-relaxed">
                  Lưu ý: Nếu bạn không nhận được email trong vài phút, vui lòng kiểm tra thư mục Spam hoặc thử lại.
                </p>
              </div>
            </div>
          ) : (
            <div className="text-center animate-in zoom-in-95 duration-500">
              <div className="w-20 h-20 bg-green-50 rounded-full flex items-center justify-center mx-auto mb-6">
                <CheckCircle2 className="text-green-500" size={40} />
              </div>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">Kiểm tra Email</h2>
              <p className="text-gray-500 text-sm mb-8">
                Chúng tôi đã gửi hướng dẫn đặt lại mật khẩu đến <span className="font-bold text-gray-900">{email}</span>.
              </p>
              <button
                onClick={() => navigate('/login')}
                className="w-full bg-gray-900 hover:bg-black text-white font-bold py-3.5 rounded-xl transition-all"
              >
                Trở lại Đăng nhập
              </button>
              <button 
                onClick={() => setIsSuccess(false)}
                className="mt-6 text-sm font-bold text-red-600 hover:underline"
              >
                Gửi lại email khác
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
