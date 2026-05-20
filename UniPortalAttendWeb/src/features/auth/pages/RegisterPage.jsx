// RegisterPage.jsx - Phiên bản Clean Enterprise (Phong cách Taobao/Alipay)
import React, { useState } from 'react';
import { 
  Mail, Lock, User, KeyRound, Loader2, 
  Eye, EyeOff, CheckCircle, Shield, 
  Zap, MonitorPlay
} from 'lucide-react';
import { authApi } from '../../../api/authApi';
import toast from 'react-hot-toast';
import { useNavigate, Link } from 'react-router-dom';
import { useLanguage } from '../../../context/LanguageContext';
import LanguageSwitcher from '../../../components/layout/LanguageSwitcher';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { t } = useLanguage();
  
  const [formData, setFormData] = useState({
    fullName: '',
    userCode: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const [focusedField, setFocusedField] = useState(null);
  const [agreeTerms, setAgreeTerms] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (e.target.name === 'password') {
      calculatePasswordStrength(e.target.value);
    }
  };

  const calculatePasswordStrength = (password) => {
    let strength = 0;
    if (password.length >= 8) strength++;
    if (password.length >= 10) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    setPasswordStrength(Math.min(strength, 4));
  };

  const getOrCreateDeviceId = () => {
    let deviceId = localStorage.getItem('web_device_id');
    if (!deviceId) {
      deviceId = crypto.randomUUID ? crypto.randomUUID() : 'web-' + new Date().getTime();
      localStorage.setItem('web_device_id', deviceId);
    }
    return deviceId;
  };

  const getStrengthColor = () => {
    const colors = ['bg-red-500', 'bg-orange-500', 'bg-yellow-500', 'bg-green-500'];
    return colors[passwordStrength - 1] || 'bg-gray-200';
  };

  const getStrengthText = () => {
    const texts = [
      t('register.strength_very_weak'), 
      t('register.strength_weak'), 
      t('register.strength_medium'), 
      t('register.strength_strong')
    ];
    return texts[passwordStrength - 1] || '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!agreeTerms) {
      return toast.error(t('register.toast_agree_terms'));
    }
    if (formData.password !== formData.confirmPassword) {
      return toast.error(t('register.toast_mismatch'));
    }
    if (formData.password.length < 8) {
      return toast.error(t('register.toast_min_len'));
    }

    setIsLoading(true);
    const loadingToast = toast.loading(t('register.toast_loading'));

    try {
      const payload = {
        fullName: formData.fullName,
        userCode: formData.userCode,
        email: formData.email,
        password: formData.password,
        deviceId: getOrCreateDeviceId()
      };

      await authApi.register(payload);

      toast.success(t('register.toast_success'), { 
        id: loadingToast, 
        duration: 3000 
      });
      setTimeout(() => {
        navigate('/login');
      }, 1500);

    } catch (error) {
      console.error("Register Error:", error);
      toast.error(error.message || t('register.toast_error'), { id: loadingToast });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-white font-sans flex flex-col">
      
      {/* 1. Header Navigation - Tối giản */}
      <header className="w-full h-20 bg-white flex items-center px-8 lg:px-24 shrink-0 border-b border-gray-50">
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

      {/* 2. Main Area */}
      <main className="flex-1 relative bg-gradient-to-br from-red-50 via-white to-orange-50 flex items-center py-8">
        {/* Background Decorative Pattern */}
        <div className="absolute inset-0 pointer-events-none overflow-hidden">
           <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-red-100 rounded-full blur-3xl opacity-30 transform translate-x-1/3 -translate-y-1/4"></div>
           <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-orange-100 rounded-full blur-3xl opacity-30 transform -translate-x-1/3 translate-y-1/4"></div>
        </div>

        <div className="container mx-auto px-4 lg:px-24 flex justify-between items-center relative z-10">
          
          {/* Left Side - Promo Content */}
          <div className="hidden lg:block w-1/2 pr-12">
            <h2 className="text-4xl font-extrabold text-gray-900 leading-tight mb-4">
              {t('register.promo_title')} <br />
              <span className="text-red-600">{t('register.promo_highlight')}</span>
            </h2>
            <p className="text-gray-600 text-lg mb-8 max-w-md">
              {t('register.promo_desc')}
            </p>
            <div className="flex gap-6">
              <div className="flex items-center gap-2 text-gray-700">
                <Zap className="text-orange-500" size={20} />
                <span className="font-medium">{t('register.fast')}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700">
                <Shield className="text-green-500" size={20} />
                <span className="font-medium">{t('register.secure')}</span>
              </div>
              <div className="flex items-center gap-2 text-gray-700">
                <MonitorPlay className="text-blue-500" size={20} />
                <span className="font-medium">{t('register.easy')}</span>
              </div>
            </div>
          </div>

          {/* Right Side - Floating Register Card */}
          <div className="w-full max-w-[420px] mx-auto lg:mx-0">
            <div className="bg-white/90 backdrop-blur-md rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.08)] border border-gray-100 overflow-hidden">
              <div className="p-8">
                
                <div className="mb-6">
                  <h2 className="text-2xl font-bold text-gray-900 mb-1">{t('register.title')}</h2>
                  <p className="text-sm text-gray-500">{t('register.subtitle')}</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                  
                  {/* Full Name */}
                  <div>
                    <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'fullName' ? 'border-red-600' : 'border-gray-200'}`}>
                      <User size={20} className={focusedField === 'fullName' ? 'text-red-600' : 'text-gray-400'} />
                      <input
                        type="text"
                        name="fullName"
                        value={formData.fullName}
                        onChange={handleChange}
                        onFocus={() => setFocusedField('fullName')}
                        onBlur={() => setFocusedField(null)}
                        required
                        className="w-full px-3 py-2.5 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400 text-sm"
                        placeholder={t('register.fullName')}
                      />
                    </div>
                  </div>

                  {/* User Code */}
                  <div>
                    <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'userCode' ? 'border-red-600' : 'border-gray-200'}`}>
                      <KeyRound size={20} className={focusedField === 'userCode' ? 'text-red-600' : 'text-gray-400'} />
                      <input
                        type="text"
                        name="userCode"
                        value={formData.userCode}
                        onChange={handleChange}
                        onFocus={() => setFocusedField('userCode')}
                        onBlur={() => setFocusedField(null)}
                        required
                        className="w-full px-3 py-2.5 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400 text-sm"
                        placeholder={t('register.userCode')}
                      />
                    </div>
                  </div>

                  {/* Email */}
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
                        className="w-full px-3 py-2.5 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400 text-sm"
                        placeholder={t('register.email')}
                      />
                    </div>
                  </div>

                  {/* Password */}
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
                        className="w-full px-3 py-2.5 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400 text-sm"
                        placeholder={t('register.password')}
                      />
                      <button
                        type="button"
                        onClick={() => setShowPassword(!showPassword)}
                        className="p-1 focus:outline-none shrink-0"
                      >
                        {showPassword ? 
                          <EyeOff size={18} className="text-gray-400 hover:text-gray-600" /> : 
                          <Eye size={18} className="text-gray-400 hover:text-gray-600" />
                        }
                      </button>
                    </div>
                    {/* Minimalist Password Strength */}
                    {formData.password && (
                      <div className="mt-2 flex items-center justify-between">
                        <div className="flex gap-1 w-24">
                          {[...Array(4)].map((_, i) => (
                            <div 
                              key={i} 
                              className={`h-1 flex-1 rounded-full transition-all duration-300 ${i < passwordStrength ? getStrengthColor() : 'bg-gray-200'}`}
                            />
                          ))}
                        </div>
                        <span className="text-[10px] text-gray-400 font-medium uppercase tracking-wider">{getStrengthText()}</span>
                      </div>
                    )}
                  </div>

                  {/* Confirm Password */}
                  <div>
                    <div className={`flex items-center border-b-2 transition-colors duration-300 ${focusedField === 'confirmPassword' ? 'border-red-600' : 'border-gray-200'}`}>
                      <CheckCircle size={20} className={focusedField === 'confirmPassword' ? 'text-red-600' : 'text-gray-400'} />
                      <input
                        type={showConfirmPassword ? "text" : "password"}
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        onFocus={() => setFocusedField('confirmPassword')}
                        onBlur={() => setFocusedField(null)}
                        required
                        className="w-full px-3 py-2.5 bg-transparent text-gray-900 focus:outline-none placeholder-gray-400 text-sm"
                        placeholder={t('register.confirmPassword')}
                      />
                      <button
                        type="button"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                        className="p-1 focus:outline-none shrink-0"
                      >
                        {showConfirmPassword ? 
                          <EyeOff size={18} className="text-gray-400 hover:text-gray-600" /> : 
                          <Eye size={18} className="text-gray-400 hover:text-gray-600" />
                        }
                      </button>
                    </div>
                  </div>

                  {/* Terms */}
                  <label className="flex items-start gap-2 cursor-pointer group mt-2">
                    <input 
                      type="checkbox" 
                      checked={agreeTerms}
                      onChange={(e) => setAgreeTerms(e.target.checked)}
                      className="mt-1 w-4 h-4 rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer shrink-0" 
                    />
                    <span className="text-sm text-gray-600 leading-tight">
                      {t('register.agree_text')}{' '}
                      <Link to="/terms" target="_blank" className="text-red-600 hover:text-red-700 font-medium transition-colors">{t('register.terms_link')}</Link>
                      {' '}{t('register.and')}{' '}
                      <Link to="/privacy" target="_blank" className="text-red-600 hover:text-red-700 font-medium transition-colors">{t('register.privacy_link')}</Link>
                    </span>
                  </label>

                  {/* Submit Button */}
                  <button
                    type="submit"
                    disabled={isLoading}
                    className="w-full mt-2 bg-gradient-to-r from-red-600 to-orange-500 hover:from-red-700 hover:to-orange-600 text-white font-bold py-3.5 rounded-lg transition-all duration-300 disabled:opacity-70 disabled:cursor-not-allowed shadow-md shadow-red-500/30 flex items-center justify-center gap-2"
                  >
                    {isLoading ? (
                      <>
                        <Loader2 size={20} className="animate-spin" /> 
                        {t('register.creating_account')}
                      </>
                    ) : (
                      t('register.register_btn')
                    )}
                  </button>

                </form>

                <div className="mt-6 text-center text-sm text-gray-500 border-t border-gray-100 pt-6">
                  {t('register.have_account')}{' '}
                  <Link to="/login" className="text-red-600 font-medium hover:text-red-700 transition-colors">
                    {t('register.login_here')}
                  </Link>
                </div>

              </div>
            </div>
          </div>
          
        </div>
      </main>

      {/* 3. Footer - Enterprise Style */}
      <footer className="py-6 bg-white border-t border-gray-100 shrink-0">
        <div className="container mx-auto px-4 text-center">
          <div className="flex items-center justify-center gap-4 text-sm text-gray-500 mb-2">
            <Link to="/about" className="hover:text-gray-900 transition-colors">{t('common.about')}</Link>
            <span>|</span>
            <Link to="/terms" className="hover:text-gray-900 transition-colors">{t('common.terms')}</Link>
            <span>|</span>
            <Link to="/privacy" className="hover:text-gray-900 transition-colors">{t('common.privacy')}</Link>
            <span>|</span>
            <Link to="#" className="hover:text-gray-900 transition-colors">{t('common.contact')}</Link>
          </div>
          <p className="text-xs text-gray-400">
            {t('common.copyright')}
          </p>
        </div>
      </footer>
    </div>
  );
}