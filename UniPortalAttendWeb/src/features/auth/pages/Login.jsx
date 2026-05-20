// Login.jsx - Phiên bản Clean Enterprise (Phong cách Taobao/Alipay)
import React, { useState, useEffect } from 'react';
import { 
  Mail, Lock, Loader2, Eye, EyeOff, 
  QrCode, Smartphone, Shield, 
  ArrowRight, MonitorPlay, Zap
} from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';
import { useLanguage } from '../../../context/LanguageContext';
import LanguageSwitcher from '../../../components/layout/LanguageSwitcher';

export default function Login() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [activeTab, setActiveTab] = useState('password');
  const [focusedField, setFocusedField] = useState(null);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const getDeviceId = () => {
    let deviceId = localStorage.getItem('web_device_id');
    if (!deviceId) {
      deviceId = 'web-' + (crypto.randomUUID?.() || Math.random().toString(36).substring(2, 15));
      localStorage.setItem('web_device_id', deviceId);
    }
    return deviceId;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    const loadingToastId = toast.loading(t('login.toast_loading'));

    try {
      const payload = {
        email: formData.email,
        password: formData.password,
        deviceId: getDeviceId()
      };

      const response = await authApi.login(payload);

      if (rememberMe) {
        localStorage.setItem('rememberedEmail', formData.email);
      } else {
        localStorage.removeItem('rememberedEmail');
      }

      localStorage.setItem('accessToken', response.accessToken);
      localStorage.setItem('refreshToken', response.refreshToken);
      localStorage.setItem('user', JSON.stringify(response.user));

      toast.success(t('login.toast_success'), { id: loadingToastId });
      
      setTimeout(() => {
        if (response.user?.isFirstLogin || response.user?.requirePasswordChange) {
          toast.success("Tài khoản của bạn cần đổi mật khẩu mặc định trước khi sử dụng!", { duration: 4000 });
          navigate('/force-change-password');
        } else {
          navigate('/dashboard');
        }
      }, 500);
      
    } catch (err) {
      toast.error(err.message || t('login.toast_error'), { id: loadingToastId });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    const rememberedEmail = localStorage.getItem('rememberedEmail');
    if (rememberedEmail) {
      setFormData(prev => ({ ...prev, email: rememberedEmail }));
      setRememberMe(true);
    }
  }, []);

  return (
    <div className="min-h-screen bg-white font-sans flex flex-col">
      
      {/* 1. Header Navigation - Tối giản */}
      <header className="w-full h-20 bg-white flex items-center px-8 lg:px-24 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-red-600 rounded-xl flex items-center justify-center shadow-sm">
            <Shield className="text-white" size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900 tracking-tight">{t('common.brand')}</h1>
          </div>
        </div>
        <div className="ml-auto flex items-center gap-6 text-sm text-gray-500">
          <Link to="/" className="hidden md:block hover:text-red-600 transition-colors">{t('common.home')}</Link>
          <Link to="/help" className="hidden md:block hover:text-red-600 transition-colors">{t('common.help')}</Link>
          <LanguageSwitcher />
        </div>
      </header>

      {/* 2. Main Banner & Login Area */}
      <main className="flex-1 relative bg-gradient-to-br from-red-50 via-white to-orange-50 flex items-center">
        {/* Background Decorative Pattern */}
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
           <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-red-100 rounded-full blur-3xl opacity-30 transform translate-x-1/3 -translate-y-1/4"></div>
           <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-orange-100 rounded-full blur-3xl opacity-30 transform -translate-x-1/3 translate-y-1/4"></div>
        </div>

        <div className="container mx-auto px-4 lg:px-24 flex justify-between items-center relative z-10 py-12">
          
          {/* Left Side - Promo Content (Hidden on small screens) */}
          <div className="hidden lg:block w-1/2 pr-12">
            <h2 className="text-4xl font-extrabold text-gray-900 leading-tight mb-4">
              {t('login.subtitle')} <br />
              <span className="text-red-600">{t('login.highlight')}</span>
            </h2>
            <p className="text-gray-600 text-lg mb-8 max-w-md">
              {t('login.desc')}
            </p>
            <div className="flex gap-6">
              <div className="flex items-center gap-2 text-gray-700">
                <Zap className="text-orange-500" size={20} />
                <span className="font-medium">{t('login.realtime')}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700">
                <MonitorPlay className="text-blue-500" size={20} />
                <span className="font-medium">{t('login.multiplatform')}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700">
                <Shield className="text-green-500" size={20} />
                <span className="font-medium">{t('login.antifraud')}</span>
              </div>
            </div>
          </div>

          {/* Right Side - Floating Login Card (Taobao Style) */}
          <div className="w-full max-w-[400px] mx-auto lg:mx-0">
            <div className="bg-white/90 backdrop-blur-md rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.08)] border border-gray-100 overflow-hidden relative">
              
              {/* Corner Toggle (QR/Password) */}
              <button 
                onClick={() => setActiveTab(activeTab === 'password' ? 'qrcode' : 'password')}
                className="absolute top-0 right-0 w-16 h-16 overflow-hidden group z-20"
              >
                <div className="absolute top-[-32px] right-[-32px] w-16 h-16 bg-red-50 transform rotate-45 group-hover:bg-red-100 transition-colors"></div>
                <div className="absolute top-2 right-2 text-red-600 transition-transform group-hover:scale-110">
                  {activeTab === 'password' ? <QrCode size={20} /> : <MonitorPlay size={20} />}
                </div>
              </button>

              <div className="p-8">
                {/* Tab Headers */}
                <div className="flex items-center gap-6 mb-8 text-lg font-bold border-b border-gray-100 pb-3">
                  <button 
                    onClick={() => setActiveTab('password')}
                    className={`relative transition-colors ${activeTab === 'password' ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'}`}
                  >
                    {t('login.title')}
                    {activeTab === 'password' && <span className="absolute -bottom-[13px] left-0 right-0 h-0.5 bg-red-600 rounded-full"></span>}
                  </button>
                  <button 
                    onClick={() => setActiveTab('qrcode')}
                    className={`relative transition-colors ${activeTab === 'qrcode' ? 'text-gray-900' : 'text-gray-400 hover:text-gray-600'}`}
                  >
                    {t('login.qr_tab')}
                    {activeTab === 'qrcode' && <span className="absolute -bottom-[13px] left-0 right-0 h-0.5 bg-red-600 rounded-full"></span>}
                  </button>
                </div>

                {/* Password Login Form */}
                {activeTab === 'password' && (
                  <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Minimalist Input - Email */}
                    <div>
                      <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'email' ? 'border-red-600' : 'border-gray-200'}`}>
                        <Mail size={20} className={focusedField === 'email' ? 'text-red-600' : 'text-gray-400'} />
                        <input
                          type="email"
                          name="email"
                          value={formData.email}
                          onChange={handleChange}
                          onFocus={() => setFocusedField('email')}
                          onBlur={() => setFocusedField(null)}
                          required
                          className="w-full px-3 py-3 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400"
                          placeholder={t('login.email_placeholder')}
                        />
                      </div>
                    </div>

                    {/* Minimalist Input - Password */}
                    <div>
                      <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'password' ? 'border-red-600' : 'border-gray-200'}`}>
                        <Lock size={20} className={focusedField === 'password' ? 'text-red-600' : 'text-gray-400'} />
                        <input
                          type={showPassword ? "text" : "password"}
                          name="password"
                          value={formData.password}
                          onChange={handleChange}
                          onFocus={() => setFocusedField('password')}
                          onBlur={() => setFocusedField(null)}
                          required
                          className="w-full px-3 py-3 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400"
                          placeholder={t('login.password_placeholder')}
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="p-1 focus:outline-none shrink-0"
                        >
                          {showPassword ? 
                            <EyeOff size={20} className="text-gray-400 hover:text-gray-600" /> : 
                            <Eye size={20} className="text-gray-400 hover:text-gray-600" />
                          }
                        </button>
                      </div>
                    </div>

                    {/* Options */}
                    <div className="flex items-center justify-between text-sm">
                      <label className="flex items-center gap-2 cursor-pointer group">
                        <input 
                          type="checkbox" 
                          checked={rememberMe}
                          onChange={(e) => setRememberMe(e.target.checked)}
                          className="w-4 h-4 rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer" 
                        />
                        <span className="text-gray-600 group-hover:text-gray-900">{t('login.remember_me')}</span>
                      </label>
                      <Link to="/forgot-password" className="text-gray-500 hover:text-red-600 transition-colors">
                        {t('login.forgot_password')}
                      </Link>
                    </div>

                    {/* Submit Button */}
                    <button
                      type="submit"
                      disabled={isLoading}
                      className="w-full bg-gradient-to-r from-red-600 to-orange-500 hover:from-red-700 hover:to-orange-600 text-white font-bold py-3.5 rounded-lg transition-all duration-300 disabled:opacity-70 disabled:cursor-not-allowed shadow-md shadow-red-500/30 flex items-center justify-center gap-2"
                    >
                      {isLoading ? (
                        <>
                          <Loader2 size={20} className="animate-spin" /> 
                          {t('login.authenticating')}
                        </>
                      ) : (
                        t('login.login_btn')
                      )}
                    </button>

                    {/* Other Logins */}
                    <div className="mt-6 flex items-center justify-between gap-4 text-sm text-gray-500">
                      <div className="flex items-center gap-4">
                        <span className="cursor-pointer hover:text-red-600 transition-colors flex items-center gap-1"><Smartphone size={16}/> {t('login.sms_login')}</span>
                        <span className="w-px h-3 bg-gray-300"></span>
                        <span className="cursor-pointer hover:text-red-600 transition-colors flex items-center gap-1"><MonitorPlay size={16}/> {t('login.app_login')}</span>
                      </div>
                      <Link to="/register" className="text-red-600 hover:text-red-700 font-medium">{t('login.register_now')}</Link>
                    </div>
                  </form>
                )}

                {/* QR Code Login Tab */}
                {activeTab === 'qrcode' && (
                  <div className="text-center py-6">
                    <h3 className="text-gray-800 font-medium mb-6">{t('login.qr_instruction')}</h3>
                    <div className="w-48 h-48 mx-auto bg-white p-2 rounded-xl shadow-sm border border-gray-100 flex items-center justify-center mb-6 relative group">
                      <QrCode size={160} className="text-gray-800" />
                      {/* Overlay on hover for refreshing */}
                      <div className="absolute inset-0 bg-black/40 rounded-xl flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer">
                        <span className="text-white text-sm font-medium">{t('login.qr_refresh')}</span>
                      </div>
                    </div>
                    <div className="flex items-center justify-center gap-2 text-sm text-gray-600">
                      <Scan size={16} />
                      <p>{t('login.qr_safer')}</p>
                    </div>
                    
                    <div className="mt-8 pt-6 border-t border-gray-100 text-sm">
                      <Link to="/register" className="text-gray-500 hover:text-red-600">{t('login.register_free')}</Link>
                    </div>
                  </div>
                )}

              </div>
            </div>
          </div>
          
        </div>
      </main>

      {/* 3. Footer - Enterprise Style */}
      <footer className="py-6 bg-white border-t border-gray-100 shrink-0">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center gap-4 text-sm text-gray-500 mb-2">
            <Link to="/about" className="hover:text-gray-900">{t('common.about')}</Link>
            <span>|</span>
            <Link to="/terms" className="hover:text-gray-900">{t('common.terms')}</Link>
            <span>|</span>
            <Link to="/privacy" className="hover:text-gray-900">{t('common.privacy')}</Link>
            <span>|</span>
            <Link to="#" className="hover:text-gray-900">{t('common.support')}</Link>
          </div>
          <p className="text-xs text-gray-400">
            {t('common.copyright')}
          </p>
        </div>
      </footer>
    </div>
  );
}

// Icon component
function Scan({ size = 24, className = "" }) {
  return (
    <svg 
      xmlns="http://www.w3.org/2000/svg" 
      width={size} 
      height={size} 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="2" 
      strokeLinecap="round" 
      strokeLinejoin="round" 
      className={className}
    >
      <path d="M3 7V5a2 2 0 0 1 2-2h2"></path>
      <path d="M17 3h2a2 2 0 0 1 2 2v2"></path>
      <path d="M21 17v2a2 2 0 0 1-2 2h-2"></path>
      <path d="M7 21H5a2 2 0 0 1-2-2v-2"></path>
    </svg>
  );
}