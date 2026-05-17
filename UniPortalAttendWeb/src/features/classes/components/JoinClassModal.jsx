import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, Copy, Check, Users, ShieldCheck } from 'lucide-react';

export default function JoinClassModal({ isOpen, onClose, classDetail }) {
  const [copied, setCopied] = useState(false);

  if (!isOpen || !classDetail) return null;

  // Lấy mã tham gia, nếu không có thì để trống
  const joinCode = classDetail.joinCode || 'CHƯA_CÓ_MÃ';
  
  // Chuỗi mã hóa vào QR (App Android sẽ quét và đọc tiền tố JOIN_CLASS:)
  const qrData = `JOIN:${joinCode}`;

  const handleCopy = () => {
    navigator.clipboard.writeText(joinCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/60 backdrop-blur-sm p-4">
      {/* Box Modal */}
      <div 
        className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in-95 duration-200"
      >
        {/* Header Modal */}
        <div className="bg-gray-50 border-b border-gray-100 px-6 py-4 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-red-100 text-red-600 rounded-lg flex items-center justify-center">
              <Users size={16} />
            </div>
            <h3 className="font-bold text-gray-900">Mời sinh viên vào lớp</h3>
          </div>
          <button 
            onClick={onClose}
            className="text-gray-400 hover:text-gray-700 hover:bg-gray-200 p-1.5 rounded-lg transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        {/* Nội dung chính */}
        <div className="p-8 flex flex-col items-center">
          <h2 className="text-xl font-bold text-gray-900 mb-1 text-center">
            {classDetail.name}
          </h2>
          <p className="text-sm text-gray-500 mb-8 flex items-center gap-1.5">
            <ShieldCheck size={14} className="text-emerald-500" /> 
            Yêu cầu sinh viên dùng app EduGuard để quét
          </p>

          {/* MÃ QR */}
          <div className="bg-white p-4 rounded-2xl shadow-[0_4px_20px_rgba(0,0,0,0.08)] border border-gray-100 mb-8 relative">
            <QRCodeSVG 
              value={qrData} 
              size={200} 
              level="M" 
              includeMargin={true}
            />
          </div>

          {/* HIỂN THỊ MÃ TEXT & NÚT COPY */}
          <div className="w-full">
            <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 text-center">
              HOẶC NHẬP MÃ THAM GIA
            </p>
            <div className="flex items-center bg-gray-50 border border-gray-200 rounded-xl p-1.5">
              <div className="flex-1 text-center font-mono font-bold text-lg text-gray-800 tracking-[0.2em]">
                {joinCode}
              </div>
              <button 
                onClick={handleCopy}
                className={`flex items-center gap-1.5 px-4 py-2.5 rounded-lg text-sm font-bold transition-all ${
                  copied ? 'bg-emerald-100 text-emerald-700' : 'bg-white border border-gray-200 text-gray-700 hover:bg-gray-100 shadow-sm'
                }`}
              >
                {copied ? <Check size={16} /> : <Copy size={16} />}
                {copied ? 'Đã chép' : 'Copy'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}