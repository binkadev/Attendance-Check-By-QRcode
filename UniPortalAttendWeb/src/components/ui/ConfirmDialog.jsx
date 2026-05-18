import React from 'react';
import { LogOut, X } from 'lucide-react';

export default function ConfirmDialog({ isOpen, onClose, onConfirm, title, message }) {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[110] flex items-center justify-center p-4">
      {/* Backdrop with rich blur */}
      <div 
        className="absolute inset-0 bg-gray-950/60 backdrop-blur-md transition-opacity duration-300 animate-in fade-in"
        onClick={onClose}
      ></div>

      {/* Modern Dialog Box */}
      <div className="relative bg-white rounded-3xl shadow-2xl max-w-sm w-full overflow-hidden p-6 text-center border border-gray-100/80 animate-in zoom-in-95 duration-200">
        {/* Close Button */}
        <button 
          onClick={onClose} 
          className="absolute right-4 top-4 text-gray-400 hover:text-gray-600 p-1.5 hover:bg-gray-50 rounded-full transition-all"
        >
          <X size={16} />
        </button>

        {/* Ambient Glowing Circular Logout Icon */}
        <div className="mx-auto w-16 h-16 rounded-full bg-red-50 border border-red-100 flex items-center justify-center text-red-600 mb-5 relative">
          <div className="absolute inset-0 rounded-full bg-red-400/20 animate-ping opacity-75 duration-1000 scale-75"></div>
          <LogOut size={26} className="relative z-10" />
        </div>

        {/* Dialog Header */}
        <h3 className="text-xl font-bold text-gray-900 mb-2">
          {title || 'Xác nhận'}
        </h3>

        {/* Dialog Message */}
        <p className="text-sm text-gray-500 mb-6 leading-relaxed px-2">
          {message || 'Bạn có chắc chắn muốn thực hiện hành động này?'}
        </p>

        {/* Button Actions with Premium Hover Scales */}
        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 px-4 py-3 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-2xl text-gray-700 font-bold text-sm transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            Hủy bỏ
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 px-4 py-3 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white rounded-2xl font-bold text-sm shadow-lg shadow-red-200 transition-all hover:scale-[1.02] active:scale-[0.98]"
          >
            Đăng xuất
          </button>
        </div>
      </div>
    </div>
  );
}