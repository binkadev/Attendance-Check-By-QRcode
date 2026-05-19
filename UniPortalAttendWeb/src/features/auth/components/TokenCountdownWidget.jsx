import React from 'react';
import { useLocation } from 'react-router-dom';
import { KeyRound, Maximize2, Minimize2 } from 'lucide-react';

export default function TokenCountdownWidget() {
  const location = useLocation();
  const [timeLeft, setTimeLeft] = React.useState(null);
  const [isDragging, setIsDragging] = React.useState(false);
  const [minimized, setMinimized] = React.useState(false);
  const [totalDuration, setTotalDuration] = React.useState(3600); // Mặc định 1h (3600s)
  const [userEmail, setUserEmail] = React.useState('');
  
  const [position, setPosition] = React.useState(() => {
    const width = typeof window !== 'undefined' ? window.innerWidth : 1024;
    return {
      x: width - 230,
      y: 90
    };
  });
  const [dragStart, setDragStart] = React.useState({ x: 0, y: 0 });

  // 1. Hook: Effect tính giờ và đọc token
  React.useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || typeof token !== 'string') {
      setTimeLeft(null);
      return;
    }

    try {
      const parts = token.split('.');
      if (parts.length < 3) {
        throw new Error('Định dạng JWT không đúng');
      }

      // Đổi ký tự Base64Url sang Base64 tiêu chuẩn & thêm padding
      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedBase64 = base64.padEnd(base64.length + (4 - base64.length % 4) % 4, '=');
      
      const decoded = JSON.parse(atob(paddedBase64));
      const exp = decoded.exp;
      const iat = decoded.iat;
      
      if (typeof exp !== 'number' || isNaN(exp)) {
        throw new Error('Trường exp của token không hợp lệ');
      }

      // Tính tổng thời lượng token nếu có trường iat
      if (iat && typeof iat === 'number') {
        setTotalDuration(Math.max(60, exp - iat));
      } else {
        setTotalDuration(3600); // 1 giờ mặc định
      }

      // Đọc thông tin email người dùng từ localStorage
      const userStr = localStorage.getItem('user');
      if (userStr) {
        try {
          const userObj = JSON.parse(userStr);
          setUserEmail(userObj.email || userObj.username || 'UniPortal User');
        } catch (_) {}
      }

      let interval;
      const calculateDiff = () => {
        const now = Math.floor(Date.now() / 1000);
        const diff = exp - now;
        if (diff <= 0) {
          setTimeLeft(0);
          // Phát sự kiện báo hết phiên làm việc đến App.jsx để hiển thị modal báo lỗi
          const event = new CustomEvent('unauthorized-api-error', {
            detail: { message: 'Phiên làm việc đã hết hạn do thời gian sống của Token đã kết thúc.' }
          });
          window.dispatchEvent(event);
          if (interval) clearInterval(interval);
        } else {
          setTimeLeft(diff);
        }
      };

      calculateDiff();
      const now = Math.floor(Date.now() / 1000);
      if (exp - now > 0) {
        interval = setInterval(calculateDiff, 1000);
      }
      return () => {
        if (interval) clearInterval(interval);
      };
    } catch (error) {
      console.warn('Lỗi phân tích token trong Widget:', error);
      setTimeLeft(null);
    }
  }, [location]);

  // 2. Hook: Effect resize để bám viền màn hình
  React.useEffect(() => {
    const handleResize = () => {
      setPosition(prev => {
        const maxX = window.innerWidth - (minimized ? 110 : 220);
        const maxY = window.innerHeight - 180;
        return {
          x: Math.max(10, Math.min(maxX, prev.x)),
          y: Math.max(10, Math.min(maxY, prev.y))
        };
      });
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [minimized]);

  // 3. Hooks: Các Callback kéo thả
  const handleMouseMove = React.useCallback((e) => {
    if (!isDragging) return;
    const maxX = window.innerWidth - (minimized ? 110 : 220);
    const maxY = window.innerHeight - 180;
    
    const x = Math.max(10, Math.min(maxX, e.clientX - dragStart.x));
    const y = Math.max(10, Math.min(maxY, e.clientY - dragStart.y));
    
    setPosition({ x, y });
  }, [isDragging, dragStart, minimized]);

  const handleMouseUp = React.useCallback(() => {
    setIsDragging(false);
  }, []);

  const handleTouchMove = React.useCallback((e) => {
    if (!isDragging) return;
    const touch = e.touches[0];
    const maxX = window.innerWidth - (minimized ? 110 : 220);
    const maxY = window.innerHeight - 180;
    
    const x = Math.max(10, Math.min(maxX, touch.clientX - dragStart.x));
    const y = Math.max(10, Math.min(maxY, touch.clientY - dragStart.y));
    
    setPosition({ x, y });
  }, [isDragging, dragStart, minimized]);

  const handleTouchEnd = React.useCallback(() => {
    setIsDragging(false);
  }, []);

  // 4. Hook: Effect gắn event kéo thả
  React.useEffect(() => {
    if (isDragging) {
      window.addEventListener('mousemove', handleMouseMove);
      window.addEventListener('mouseup', handleMouseUp);
      window.addEventListener('touchmove', handleTouchMove, { passive: false });
      window.addEventListener('touchend', handleTouchEnd);
    }
    return () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
      window.removeEventListener('touchmove', handleTouchMove);
      window.removeEventListener('touchend', handleTouchEnd);
    };
  }, [isDragging, handleMouseMove, handleMouseUp, handleTouchMove, handleTouchEnd]);

  // Các hàm tương tác kéo thả thông thường
  const handleMouseDown = (e) => {
    if (e.button !== 0) return; 
    if (e.target.closest('button')) return; 
    setIsDragging(true);
    setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    e.preventDefault();
  };

  const handleTouchStart = (e) => {
    if (e.target.closest('button')) return;
    const touch = e.touches[0];
    setIsDragging(true);
    setDragStart({ x: touch.clientX - position.x, y: touch.clientY - position.y });
  };

  // =========================================================================
  // ĐĂY LÀ ĐIỂM QUAN TRỌNG: LỆNH RETURN SỚM PHẢI NẰM SAU TẤT CẢ CÁC HOOKS!
  // =========================================================================
  if (timeLeft === null || isNaN(timeLeft) || timeLeft <= 0) return null;

  // Render logic và định dạng MM:SS
  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const timeString = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

  // Tính phần trăm thời gian còn lại
  const percentage = Math.max(0, Math.min(100, (timeLeft / totalDuration) * 100));

  let statusColor = 'text-emerald-400 bg-emerald-500/10 border-emerald-500/20';
  let pulseColor = 'bg-emerald-500';
  let ringColor = '#10b981'; // Emerald
  if (timeLeft < 120) { 
    statusColor = 'text-rose-400 bg-rose-500/10 border-rose-500/20 animate-pulse';
    pulseColor = 'bg-rose-500';
    ringColor = '#f43f5e'; // Rose
  } else if (timeLeft < 600) { 
    statusColor = 'text-amber-400 bg-amber-500/10 border-amber-500/20';
    pulseColor = 'bg-amber-500';
    ringColor = '#f59e0b'; // Amber
  }

  return (
    <div
      style={{ position: 'fixed', left: `${position.x}px`, top: `${position.y}px`, touchAction: 'none' }}
      className={`z-[999] transition-shadow duration-200 select-none ${
        isDragging ? 'shadow-2xl scale-[1.01] cursor-grabbing' : 'shadow-xl cursor-grab hover:shadow-2xl'
      }`}
      onMouseDown={handleMouseDown}
      onTouchStart={handleTouchStart}
    >
      {minimized ? (
        // ==================== GIAO DIỆN CAPSULE COLLAPSED ====================
        <div className={`flex items-center gap-2 px-3 py-1.5 rounded-full border bg-slate-950/90 text-white backdrop-blur-xl transition-all duration-300 border-slate-800/80 shadow-md ${statusColor}`}>
          <div className="relative flex h-2 w-2 shrink-0">
            <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${pulseColor}`}></span>
            <span className={`relative inline-flex rounded-full h-2 w-2 ${pulseColor}`}></span>
          </div>
          <span className="text-[11px] font-bold font-mono tracking-wider">{timeString}</span>
          <button
            onClick={() => setMinimized(false)}
            className="p-0.5 hover:bg-white/10 rounded transition-colors text-inherit"
            title="Mở rộng"
          >
            <Maximize2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ) : (
        // ==================== GIAO DIỆNexpanded - ĐẶC TRƯNG CHẤT PTIT/UNIATTEND ====================
        <div className="w-[210px] rounded-3xl border border-slate-800/70 bg-slate-950/85 text-white p-4 backdrop-blur-xl flex flex-col gap-3 shadow-[0_20px_50px_rgba(0,0,0,0.5)] relative overflow-hidden">
          {/* Vết sáng hologram trang trí */}
          <div className="absolute top-[-30px] left-[-30px] w-24 h-24 rounded-full bg-red-600/5 blur-2xl pointer-events-none" />
          
          {/* Header Widget */}
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <div className="flex items-center gap-1.5">
              <div className={`p-1 rounded-lg ${timeLeft < 120 ? 'bg-rose-500/10' : 'bg-red-500/10'} border border-red-500/20`}>
                <KeyRound className={`w-3.5 h-3.5 ${timeLeft < 120 ? 'text-rose-400 animate-pulse' : 'text-red-500'}`} />
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] font-extrabold tracking-wider text-slate-100 uppercase font-sans">UniPortal</span>
                <span className="text-[8px] font-semibold text-slate-500 tracking-widest uppercase">Security Key</span>
              </div>
            </div>
            <button
              onClick={() => setMinimized(true)}
              className="p-1 hover:bg-slate-800/60 rounded-lg transition-colors text-slate-400 hover:text-white"
              title="Thu nhỏ"
            >
              <Minimize2 className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Vòng tròn Radar quét SVG cực kỳ xịn sò */}
          <div className="flex justify-center items-center py-2 relative">
            <svg className="w-[100px] h-[100px]" viewBox="0 0 88 88">
              <defs>
                <filter id="svgGlow" x="-20%" y="-20%" width="140%" height="140%">
                  <feGaussianBlur stdDeviation="2.5" result="blur" />
                  <feMerge>
                    <feMergeNode in="blur" />
                    <feMergeNode in="SourceGraphic" />
                  </feMerge>
                </filter>
              </defs>
              {/* Vòng nền xám tối */}
              <circle
                cx="44"
                cy="44"
                r="34"
                className="stroke-slate-800"
                strokeWidth="5"
                fill="transparent"
              />
              {/* Vòng phát sáng phần trăm thời gian thực */}
              <circle
                cx="44"
                cy="44"
                r="34"
                stroke={ringColor}
                strokeWidth="5"
                fill="transparent"
                strokeDasharray={2 * Math.PI * 34}
                strokeDashoffset={(2 * Math.PI * 34) * (1 - percentage / 100)}
                strokeLinecap="round"
                filter="url(#svgGlow)"
                transform="rotate(-90 44 44)"
                className="transition-all duration-1000 ease-out"
              />
            </svg>
            
            {/* Bộ đếm ngược hiển thị ở giữa tâm */}
            <div className="absolute flex flex-col items-center justify-center">
              <span className={`text-[17px] font-black font-mono tracking-wider ${timeLeft < 120 ? 'text-rose-400 animate-pulse' : 'text-slate-100'}`}>
                {timeString}
              </span>
              <span className="text-[7px] text-slate-500 font-bold uppercase tracking-wider mt-0.5">Lifespan</span>
            </div>
          </div>

          {/* Thông tin Chi tiết Phiên hoạt động */}
          <div className="flex flex-col gap-1.5 border-t border-slate-900 pt-2.5">
            <div className="flex justify-between text-[9px] font-semibold text-slate-400">
              <span>Trạng thái:</span>
              <span className={`font-bold ${timeLeft < 120 ? 'text-rose-400 animate-pulse' : timeLeft < 600 ? 'text-amber-400' : 'text-emerald-400'}`}>
                {timeLeft < 120 ? 'NGUY CẤP' : timeLeft < 600 ? 'CẦN LÀM MỚI' : 'AN TOÀN'}
              </span>
            </div>
            <div className="flex justify-between text-[9px] text-slate-500 font-medium">
              <span>Tài khoản:</span>
              <span className="truncate max-w-[110px] text-slate-400" title={userEmail}>{userEmail || 'UniPortal User'}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}