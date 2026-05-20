import React, { useState } from 'react';
import { 
  ArrowLeft, Shield, Target, Award, Users, 
  Zap, Globe, Database, Smartphone, Laptop, 
  MapPin, Phone, Mail, ChevronRight, Activity, 
  CheckCircle2, Sparkles, Building2, Flame
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useLanguage } from '../../context/LanguageContext';
import LanguageSwitcher from '../../components/layout/LanguageSwitcher';

export default function About() {
  const navigate = useNavigate();
  const { t, language } = useLanguage();
  const [activeTimelineYear, setActiveTimelineYear] = useState(2026);

  const timelineData = [
    {
      year: 2023,
      title: t('about.timeline_y2023_title'),
      description: t('about.timeline_y2023_desc')
    },
    {
      year: 2024,
      title: t('about.timeline_y2024_title'),
      description: t('about.timeline_y2024_desc')
    },
    {
      year: 2025,
      title: t('about.timeline_y2025_title'),
      description: t('about.timeline_y2025_desc')
    },
    {
      year: 2026,
      title: t('about.timeline_y2026_title'),
      description: t('about.timeline_y2026_desc')
    }
  ];

  const stats = [
    { value: "10M+", label: t('about.stats_attendance'), sub: t('about.stats_attendance_sub') },
    { value: "200+", label: t('about.stats_universities'), sub: t('about.stats_universities_sub') },
    { value: "500K+", label: t('about.stats_users'), sub: t('about.stats_users_sub') },
    { value: "99.99%", label: t('about.stats_accuracy'), sub: t('about.stats_accuracy_sub') }
  ];

  const coreValues = [
    {
      icon: <Flame className="w-8 h-8 text-red-600" />,
      title: t('about.val_1_title'),
      desc: t('about.val_1_desc')
    },
    {
      icon: <Sparkles className="w-8 h-8 text-orange-500" />,
      title: t('about.val_2_title'),
      desc: t('about.val_2_desc')
    },
    {
      icon: <Zap className="w-8 h-8 text-amber-500" />,
      title: t('about.val_3_title'),
      desc: t('about.val_3_desc')
    },
    {
      icon: <Users className="w-8 h-8 text-blue-500" />,
      title: t('about.val_4_title'),
      desc: t('about.val_4_desc')
    }
  ];

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800 flex flex-col selection:bg-red-500 selection:text-white">
      
      {/* 1. Header (Sticky Glassmorphic) */}
      <header className="w-full h-20 bg-white/80 backdrop-blur-md border-b border-slate-100 flex items-center px-6 lg:px-24 sticky top-0 z-50 transition-all duration-300">
        <button 
          onClick={() => navigate('/login')} 
          className="flex items-center gap-2 text-slate-500 hover:text-red-600 font-medium transition-all group active:scale-95 cursor-pointer"
        >
          <ArrowLeft size={20} className="transform group-hover:-translate-x-1 transition-transform" />
          <span className="text-sm">{t('about.back_to_login')}</span>
        </button>
        <div className="mx-auto flex items-center gap-3">
          <div className="w-9 h-9 bg-red-600 rounded-lg flex items-center justify-center shadow-md shadow-red-500/20">
            <Shield className="text-white" size={20} />
          </div>
          <span className="text-xl font-black text-slate-900 tracking-tight">{t('common.brand')}</span>
        </div>
        <div className="hidden md:flex items-center gap-6">
          <Link to="/help" className="text-sm text-slate-500 hover:text-red-600 transition-colors font-medium">{t('common.support')}</Link>
          <LanguageSwitcher />
        </div>
      </header>

      {/* 2. Hero Section (Khát vọng chuyển đổi số) */}
      <section className="relative overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-red-950 text-white py-20 lg:py-32 px-6 lg:px-24">
        {/* Họa tiết công nghệ nền động */}
        <div className="absolute inset-0 pointer-events-none opacity-30 overflow-hidden">
          <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-red-600 rounded-full blur-[150px] transform translate-x-1/4 -translate-y-1/4 animate-pulse"></div>
          <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-orange-600 rounded-full blur-[130px] transform -translate-x-1/4 translate-y-1/4"></div>
          <div className="absolute inset-0 bg-[radial-gradient(#ffffff0a_1px,transparent_1px)] [background-size:20px_20px]"></div>
        </div>

        <div className="max-w-5xl mx-auto text-center relative z-10">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 bg-white/10 backdrop-blur-md rounded-full border border-white/10 text-red-400 text-sm font-semibold mb-6 tracking-wide uppercase">
            <Sparkles size={16} />
            {t('about.sparkles_label')}
          </div>
          <h1 className="text-4xl md:text-6xl font-black tracking-tight leading-none mb-6">
            {t('about.hero_title')} <br />
            <span className="bg-gradient-to-r from-red-500 via-orange-400 to-amber-300 bg-clip-text text-transparent">
              {t('about.hero_highlight')}
            </span>
          </h1>
          <p className="text-lg md:text-xl text-slate-300 max-w-3xl mx-auto leading-relaxed mb-10">
            {t('about.hero_desc')}
          </p>
          <div className="flex flex-col sm:flex-row justify-center items-center gap-4">
            <button 
              onClick={() => {
                const element = document.getElementById("mission-section");
                element?.scrollIntoView({ behavior: "smooth" });
              }}
              className="w-full sm:w-auto px-8 py-4 bg-white text-slate-900 hover:bg-slate-100 font-bold rounded-2xl transition-all shadow-lg active:scale-98 flex items-center justify-center gap-2 group cursor-pointer"
            >
              {t('about.discover_mission')}
              <ChevronRight size={18} className="transform group-hover:translate-x-1 transition-transform" />
            </button>
            <button 
              onClick={() => navigate('/login')}
              className="w-full sm:w-auto px-8 py-4 bg-gradient-to-r from-red-600 to-orange-500 hover:from-red-700 hover:to-orange-600 font-bold rounded-2xl transition-all shadow-lg shadow-red-500/20 active:scale-98 cursor-pointer"
            >
              {t('about.get_started_today')}
            </button>
          </div>
        </div>
      </section>

      {/* 3. Stats Section (Những con số ấn tượng) */}
      <section className="relative -mt-10 z-20 px-6 lg:px-24">
        <div className="max-w-6xl mx-auto bg-white rounded-3xl shadow-[0_20px_50px_rgba(0,0,0,0.04)] border border-slate-100/80 p-8 md:p-12 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          {stats.map((stat, idx) => (
            <div key={idx} className="flex flex-col items-center md:items-start text-center md:text-left border-b md:border-b-0 lg:border-r last:border-0 border-slate-100 pb-6 md:pb-0 lg:pr-6 last:pr-0">
              <span className="text-4xl md:text-5xl font-black bg-gradient-to-r from-red-600 to-orange-500 bg-clip-text text-transparent tracking-tight mb-2">
                {stat.value}
              </span>
              <span className="text-sm font-bold text-slate-800 mb-1 leading-snug">{stat.label}</span>
              <span className="text-xs text-slate-400 leading-normal">{stat.sub}</span>
            </div>
          ))}
        </div>
      </section>

      {/* 4. Vision & Mission Section (Tầm nhìn - Sứ mệnh) */}
      <section id="mission-section" className="py-20 lg:py-28 px-6 lg:px-24 max-w-6xl mx-auto w-full">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <div>
            <div className="flex items-center gap-2 text-red-600 font-bold text-sm tracking-widest uppercase mb-3">
              <Activity size={18} />
              {t('about.position_title')}
            </div>
            <h2 className="text-3xl md:text-4xl font-extrabold text-slate-900 leading-tight mb-6">
              {t('about.position_heading')}
            </h2>
            <p className="text-slate-600 leading-relaxed mb-8">
              {t('about.position_desc')}
            </p>
            <div className="space-y-4">
              <div className="flex gap-4 items-start">
                <div className="p-1.5 bg-green-50 rounded-lg text-green-600 shrink-0 mt-1">
                  <CheckCircle2 size={18} />
                </div>
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{t('about.feature_paperless_title')}</h4>
                  <p className="text-xs text-slate-500 mt-0.5">{t('about.feature_paperless_desc')}</p>
                </div>
              </div>
              <div className="flex gap-4 items-start">
                <div className="p-1.5 bg-green-50 rounded-lg text-green-600 shrink-0 mt-1">
                  <CheckCircle2 size={18} />
                </div>
                <div>
                  <h4 className="font-bold text-slate-900 text-sm">{t('about.feature_fairness_title')}</h4>
                  <p className="text-xs text-slate-500 mt-0.5">{t('about.feature_fairness_desc')}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-6">
            {/* Card Tầm nhìn */}
            <div className="bg-white rounded-2xl p-8 border border-slate-100 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group">
              <div className="absolute top-0 right-0 w-24 h-24 bg-red-500/5 rounded-full transform translate-x-8 -translate-y-8 group-hover:scale-125 transition-transform"></div>
              <div className="w-12 h-12 bg-red-50 rounded-xl flex items-center justify-center text-red-600 mb-6">
                <Target size={24} />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-3">{t('about.vision_title')}</h3>
              <p className="text-sm text-slate-500 leading-relaxed">
                {t('about.vision_desc')}
              </p>
            </div>

            {/* Card Sứ mệnh */}
            <div className="bg-white rounded-2xl p-8 border border-slate-100 shadow-sm hover:shadow-md transition-shadow relative overflow-hidden group">
              <div className="absolute top-0 right-0 w-24 h-24 bg-orange-500/5 rounded-full transform translate-x-8 -translate-y-8 group-hover:scale-125 transition-transform"></div>
              <div className="w-12 h-12 bg-orange-50 rounded-xl flex items-center justify-center text-orange-600 mb-6">
                <Award size={24} />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-3">{t('about.mission_title')}</h3>
              <p className="text-sm text-slate-500 leading-relaxed">
                {t('about.mission_desc')}
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 5. Core Values Section (Giá trị cốt lõi) */}
      <section className="py-20 bg-slate-900 text-white px-6 lg:px-24">
        <div className="max-w-6xl mx-auto w-full">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <span className="text-red-500 font-bold text-sm tracking-widest uppercase block mb-3">{t('about.philosophy_title')}</span>
            <h2 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-4">
              {t('about.philosophy_heading')}
            </h2>
            <p className="text-sm text-slate-400 leading-relaxed">
              {t('about.philosophy_desc')}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {coreValues.map((value, idx) => (
              <div 
                key={idx} 
                className="bg-slate-800/40 backdrop-blur-md rounded-2xl p-8 border border-slate-700/50 hover:border-slate-600 transition-all duration-300 hover:-translate-y-1 group"
              >
                <div className="w-14 h-14 bg-slate-800 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform shadow-inner">
                  {value.icon}
                </div>
                <h3 className="text-lg font-bold text-white mb-3 group-hover:text-red-400 transition-colors">
                  {value.title}
                </h3>
                <p className="text-sm text-slate-400 leading-relaxed">
                  {value.desc}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 6. Dynamic Development Timeline (Hành trình phát triển) */}
      <section className="py-20 lg:py-28 px-6 lg:px-24 max-w-6xl mx-auto w-full">
        <div className="text-center max-w-2xl mx-auto mb-16">
          <span className="text-red-600 font-bold text-sm tracking-widest uppercase block mb-3">{t('about.timeline_subtitle')}</span>
          <h2 className="text-3xl md:text-4xl font-extrabold text-slate-900 tracking-tight mb-4">
            {t('about.timeline_heading')}
          </h2>
          <p className="text-sm text-slate-500">
            {t('about.timeline_desc')}
          </p>
        </div>

        {/* Timeline Component */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          
          {/* Trái: Thanh điều hướng năm */}
          <div className="lg:col-span-3 flex lg:flex-col gap-2 overflow-x-auto lg:overflow-x-visible pb-4 lg:pb-0 border-b lg:border-b-0 lg:border-r border-slate-200 pr-0 lg:pr-6">
            {timelineData.map((item) => (
              <button
                key={item.year}
                onClick={() => setActiveTimelineYear(item.year)}
                className={`px-5 py-3 rounded-xl font-bold text-sm text-left transition-all shrink-0 flex items-center justify-between group cursor-pointer ${
                  activeTimelineYear === item.year 
                    ? 'bg-red-600 text-white shadow-md shadow-red-500/20' 
                    : 'bg-white text-slate-500 hover:bg-slate-100 hover:text-slate-800 border border-slate-100'
                }`}
              >
                <span>{language === 'vi' ? 'Năm ' + item.year : 'Year ' + item.year}</span>
                <ChevronRight size={16} className={`hidden lg:block transform transition-transform ${
                  activeTimelineYear === item.year ? 'translate-x-1 opacity-100' : 'opacity-0 group-hover:opacity-100'
                }`} />
              </button>
            ))}
          </div>

          {/* Phải: Hiển thị chi tiết dấu mốc */}
          <div className="lg:col-span-9 bg-white rounded-3xl p-8 md:p-12 border border-slate-100 shadow-sm min-h-[250px] flex flex-col justify-center animate-fade-in-up">
            <span className="text-5xl font-black text-slate-200 mb-4 block">
              {activeTimelineYear}
            </span>
            {timelineData.map((item) => {
              if (item.year !== activeTimelineYear) return null;
              return (
                <div key={item.year} className="transition-all duration-300">
                  <h3 className="text-2xl font-bold text-slate-900 mb-3">
                    {item.title}
                  </h3>
                  <p className="text-slate-600 leading-relaxed text-sm">
                    {item.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* 7. Key Technology Pillars (Hệ sinh thái cốt lõi) */}
      <section className="py-20 bg-gradient-to-b from-white to-slate-50 px-6 lg:px-24">
        <div className="max-w-6xl mx-auto w-full">
          <div className="text-center max-w-2xl mx-auto mb-16">
            <span className="text-red-600 font-bold text-sm tracking-widest uppercase block mb-3">{t('about.tech_subtitle')}</span>
            <h2 className="text-3xl md:text-4xl font-extrabold text-slate-900 tracking-tight mb-4">
              {t('about.tech_heading')}
            </h2>
            <p className="text-sm text-slate-500">
              {t('about.tech_desc')}
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            
            {/* Pillar 1 */}
            <div className="bg-white rounded-3xl p-8 border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
              <div className="w-12 h-12 bg-red-50 text-red-600 rounded-2xl flex items-center justify-center mb-6">
                <Laptop size={24} />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-3">{t('about.tech_p1_title')}</h3>
              <p className="text-xs text-slate-500 leading-relaxed mb-4">
                {t('about.tech_p1_desc')}
              </p>
              <ul className="text-xs text-slate-500 space-y-2 mt-4 pt-4 border-t border-slate-100">
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p1_bullet1')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p1_bullet2')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p1_bullet3')}</li>
              </ul>
            </div>

            {/* Pillar 2 */}
            <div className="bg-white rounded-3xl p-8 border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
              <div className="w-12 h-12 bg-orange-50 text-orange-600 rounded-2xl flex items-center justify-center mb-6">
                <Smartphone size={24} />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-3">{t('about.tech_p2_title')}</h3>
              <p className="text-xs text-slate-500 leading-relaxed mb-4">
                {t('about.tech_p2_desc')}
              </p>
              <ul className="text-xs text-slate-500 space-y-2 mt-4 pt-4 border-t border-slate-100">
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p2_bullet1')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p2_bullet2')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p2_bullet3')}</li>
              </ul>
            </div>

            {/* Pillar 3 */}
            <div className="bg-white rounded-3xl p-8 border border-slate-100 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1">
              <div className="w-12 h-12 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center mb-6">
                <Database size={24} />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-3">{t('about.tech_p3_title')}</h3>
              <p className="text-xs text-slate-500 leading-relaxed mb-4">
                {t('about.tech_p3_desc')}
              </p>
              <ul className="text-xs text-slate-500 space-y-2 mt-4 pt-4 border-t border-slate-100">
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p3_bullet1')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p3_bullet2')}</li>
                <li className="flex items-center gap-2"><CheckCircle2 className="text-green-500 shrink-0" size={14} /> {t('about.tech_p3_bullet3')}</li>
              </ul>
            </div>

          </div>
        </div>
      </section>

      {/* 8. Call To Action (Kiến tạo giảng đường số) */}
      <section className="bg-gradient-to-br from-red-700 via-red-600 to-orange-600 py-16 lg:py-24 px-6 lg:px-24 text-white text-center relative overflow-hidden">
        {/* Decorative circle */}
        <div className="absolute top-0 left-1/2 w-[700px] h-[700px] bg-white/5 rounded-full blur-2xl transform -translate-x-1/2 -translate-y-1/2 pointer-events-none"></div>

        <div className="max-w-4xl mx-auto relative z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-white/10 rounded-full text-xs font-semibold mb-6 uppercase tracking-widest border border-white/10">
            <Building2 size={14} />
            {t('about.cta_subtitle')}
          </div>
          <h2 className="text-3xl md:text-5xl font-black mb-6 tracking-tight">
            {t('about.cta_heading')}
          </h2>
          <p className="text-sm md:text-base text-red-55 max-w-2xl mx-auto leading-relaxed mb-8">
            {t('about.cta_desc')}
          </p>
          <div className="flex flex-col sm:flex-row justify-center gap-4">
            <button 
              onClick={() => navigate('/login')}
              className="w-full sm:w-auto px-8 py-4 bg-white hover:bg-slate-50 text-red-600 font-bold rounded-2xl shadow-lg shadow-black/10 active:scale-95 transition-all text-sm cursor-pointer"
            >
              {t('about.cta_login_btn')}
            </button>
            <button 
              onClick={() => navigate('/register')}
              className="w-full sm:w-auto px-8 py-4 bg-red-950/20 hover:bg-red-950/30 text-white font-bold rounded-2xl border border-white/20 active:scale-95 transition-all text-sm cursor-pointer"
            >
              {t('about.cta_register_btn')}
            </button>
          </div>
        </div>
      </section>

      {/* 9. Footer (Tập đoàn UniGroup) */}
      <footer className="bg-slate-950 text-slate-400 py-16 px-6 lg:px-24 border-t border-slate-900">
        <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12 mb-12">
          
          <div className="lg:col-span-1">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-8 h-8 bg-red-600 rounded-lg flex items-center justify-center shadow-md shadow-red-500/10">
                <Shield className="text-white" size={18} />
              </div>
              <span className="text-lg font-black text-white tracking-tight">{t('common.brand')}</span>
            </div>
            <p className="text-xs leading-relaxed text-slate-500">
              {t('about.footer_unigroup_desc')}
            </p>
          </div>

          <div>
            <h4 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">{t('about.footer_section_legal')}</h4>
            <ul className="space-y-3 text-xs">
              <li><Link to="/terms" className="hover:text-white transition-colors">{t('common.terms')}</Link></li>
              <li><Link to="/privacy" className="hover:text-white transition-colors">{t('common.privacy')}</Link></li>
              <li><Link to="/help" className="hover:text-white transition-colors">{t('common.help')}</Link></li>
              <li><a href="#" className="hover:text-white transition-colors">{t('about.footer_profile_doc')}</a></li>
            </ul>
          </div>

          <div>
            <h4 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">{t('about.footer_section_products')}</h4>
            <ul className="space-y-3 text-xs">
              <li><span className="hover:text-white cursor-pointer transition-colors">{t('about.footer_p_qr')}</span></li>
              <li><span className="hover:text-white cursor-pointer transition-colors">{t('about.footer_p_biometric')}</span></li>
              <li><span className="hover:text-white cursor-pointer transition-colors">{t('about.footer_p_smart')}</span></li>
              <li><span className="hover:text-white cursor-pointer transition-colors">{t('about.footer_p_api')}</span></li>
            </ul>
          </div>

          <div>
            <h4 className="text-sm font-bold text-white mb-6 uppercase tracking-wider">{t('about.footer_section_contact')}</h4>
            <ul className="space-y-3 text-xs text-slate-500">
              <li className="flex items-start gap-2">
                <MapPin size={16} className="text-slate-400 shrink-0 mt-0.5" />
                <span>{t('about.footer_c_addr')}</span>
              </li>
              <li className="flex items-center gap-2">
                <Phone size={16} className="text-slate-400 shrink-0" />
                <span>{t('about.footer_c_phone')}</span>
              </li>
              <li className="flex items-center gap-2">
                <Mail size={16} className="text-slate-400 shrink-0" />
                <span>{t('about.footer_c_email')}</span>
              </li>
            </ul>
          </div>

        </div>

        <div className="max-w-6xl mx-auto border-t border-slate-900 pt-8 flex flex-col md:flex-row items-center justify-between text-xs text-slate-600 gap-4">
          <p>{t('common.copyright')}</p>
          <div className="flex gap-4">
            <span className="hover:text-slate-500 cursor-pointer">{t('common.vietnamese')}</span>
            <span>•</span>
            <span className="hover:text-slate-500 cursor-pointer">{t('common.english')}</span>
          </div>
        </div>
      </footer>

    </div>
  );
}
