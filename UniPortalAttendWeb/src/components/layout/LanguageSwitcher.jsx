import React from 'react';
import { useLanguage } from '../../context/LanguageContext';
import { Globe } from 'lucide-react';

export default function LanguageSwitcher() {
  const { language, toggleLanguage } = useLanguage();

  return (
    <button
      onClick={toggleLanguage}
      className="flex items-center gap-2.5 px-3 py-1.5 bg-slate-50/80 hover:bg-slate-100/90 border border-slate-200/60 hover:border-slate-300 rounded-xl text-slate-700 hover:text-slate-900 transition-all duration-300 active:scale-95 shadow-sm backdrop-blur-md cursor-pointer"
      title={language === 'vi' ? 'Switch to English' : 'Chuyển sang Tiếng Việt'}
    >
      <span className="text-base select-none shrink-0" role="img" aria-label="flag">
        {language === 'vi' ? '🇻🇳' : '🇬🇧'}
      </span>
      <span className="text-[11px] font-black tracking-wider select-none uppercase">
        {language === 'vi' ? 'VI' : 'EN'}
      </span>
    </button>
  );
}
