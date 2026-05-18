import React, { useState, useEffect, useRef } from 'react';
import { QrCode, Maximize, Pause, Plus, ShieldCheck, BarChart2, AlertTriangle, Play, Settings, X, BookOpen, CalendarDays, MapPin, Clock, Timer, Smartphone } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react'; 
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// Cấu hình hiển thị theo loại sự cố
const FRAUD_TYPE_CONFIG = {
  REPEATED_FAILED_QR_TOKEN: { label: 'Quét QR thất bại liên tục', icon: <Timer size={16} className="text-red-500" /> },
  WRONG_SESSION_QR_TOKEN: { label: 'QR sai phiên học', icon: <Smartphone size={16} className="text-amber-500" /> },
  EXPIRED_QR_TOKEN: { label: 'QR hết hạn', icon: <Timer size={16} className="text-red-500" /> },
  REPEATED_OUT_OF_RANGE: { label: 'Quét ngoài phạm vi', icon: <MapPin size={16} className="text-amber-500" /> },
  IP_BURST_MULTI_ATTEMPT: { label: 'Nhiều yêu cầu từ 1 IP', icon: <Smartphone size={16} className="text-red-500" /> },
  SHARED_DEVICE_MULTI_ACCOUNT: { label: 'Trùng thiết bị điểm danh', icon: <Smartphone size={16} className="text-red-500" /> },
};

// =========================================================================
// COMPONENT CHÍNH
// =========================================================================
export default function DynamicQRTab({ classDetail, onCheckInsUpdate }) {

  const groupId = classDetail?.id || classDetail?.groupId;

  // --- STATES CƠ BẢN ---
  const [sessionStatus, setSessionStatus] = useState('paused');
  const [countdown, setCountdown] = useState(11);
  const [qrCodeData, setQrCodeData] = useState('INITIAL_CODE'); 
  const [activeSessionId, setActiveSessionId] = useState(null);
  const [rotateInterval, setRotateInterval] = useState(11); 
  
  // STATE MỚI: Quản lý trạng thái bật/tắt cửa sổ máy chiếu
  const [showProjector, setShowProjector] = useState(false);

  // --- STATES CẤU HÌNH THÔNG SỐ ---
  const [config, setConfig] = useState({
    title: '',
    timeWindowMinutes: '',
    lateAfterMinutes: '',
    qrRotateSeconds: ''
  });

  // --- STATES THỐNG KÊ ---
  const [totalStudents, setTotalStudents] = useState(0); 
  const [checkIns, setCheckIns] = useState(0);
  const attendanceRate = totalStudents > 0 ? Math.round((checkIns / totalStudents) * 100) : 0;

  // --- TẢI TỔNG SỐ SINH VIÊN KHI LOAD COMPONENT ---
  useEffect(() => {
    const fetchClassMembers = async () => {
      if (!groupId || groupId.startsWith('mock-')) {
        setTotalStudents(142); 
        return;
      }
      try {
        const memberRes = await classApi.getClassMembers(groupId);
        const allMembers = Array.isArray(memberRes) ? memberRes : (memberRes.items || []);
        const studentMembers = allMembers.filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER');
        setTotalStudents(studentMembers.length);
      } catch (error) {
        console.error("Lỗi lấy danh sách sinh viên:", error);
        setTotalStudents(classDetail?.totalMembers || 0); 
      }
    };
    fetchClassMembers();
  }, [groupId, classDetail]);

  // --- TỰ ĐỘNG KIỂM TRA PHIÊN MỞ ---
  useEffect(() => {
    const autoResumeSession = async () => {
      if (!groupId || groupId.startsWith('mock-')) return;
      try {
        const openSession = await classApi.getOpenSession(groupId);
        if (openSession && openSession.id) {
           const now = new Date();
           const endTime = new Date(openSession.checkinCloseAt || openSession.endAt);
           
           if (endTime > now) {
               setActiveSessionId(openSession.id);
               setSessionStatus('active');
               const secs = openSession.qrRotateSeconds || 11;
               setRotateInterval(secs);
               setCountdown(secs);
               await handleRotateQR(openSession.id);
           }
        }
      } catch (err) {}
    };
    autoResumeSession();
  }, [groupId]);

  const [securityAlerts, setSecurityAlerts] = useState([]);

  // --- FETCH SECURITY ALERTS (POLLING) ---
  useEffect(() => {
    if (!groupId || groupId.startsWith('mock-')) return;
    
    const fetchAlerts = async () => {
      try {
        const res = await classApi.getFraudIncidents(groupId, { size: 3, sortDir: 'DESC' });
        const realAlerts = (res.items || []).map((incident) => {
             const detectedTime = incident.createdAt || incident.lastDetectedAt;
             return {
               id: incident.id,
               fraudType: incident.type,
               title: incident.title || FRAUD_TYPE_CONFIG[incident.type]?.label || incident.type || 'Hoạt động bất thường',
               time: new Date(detectedTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
               desc: incident.description || (incident.student?.name ? `Sinh viên <span class="font-bold">${incident.student.name}</span>: Hệ thống tự động phát hiện rủi ro` : 'Hệ thống tự động phát hiện rủi ro'),
               severity: incident.severity || 'HIGH'
             };
        });
        setSecurityAlerts(realAlerts);
      } catch (error) {
        setSecurityAlerts([]);
      }
    };
    
    fetchAlerts();
    const alertTimer = setInterval(fetchAlerts, 10000); // Polling mỗi 10s
    return () => clearInterval(alertTimer);
  }, [groupId]);

  // --- LOGIC: XOAY MÃ QR ---
  const handleRotateQR = async (sessionIdToRotate) => {
    const idToUse = sessionIdToRotate || activeSessionId;
    if (!idToUse) return; 

    try {
      const res = await classApi.rotateQR(idToUse);
      if (res && res.token) setQrCodeData(res.token); 
      else setQrCodeData('NO_CODE_RETURNED');
    } catch (error) {
      console.error("Lỗi xoay QR:", error);
    }
  };

  // --- LOGIC 1 & 2: ĐẾM NGƯỢC VÀ GỌI API ---
  useEffect(() => {
    let timer;
    if (sessionStatus === 'active' && activeSessionId) {
      timer = setInterval(() => setCountdown((prev) => prev - 1), 1000);
    }
    return () => clearInterval(timer);
  }, [sessionStatus, activeSessionId]);

  useEffect(() => {
    if (sessionStatus === 'active' && countdown <= 0) {
      handleRotateQR(activeSessionId); 
      setCountdown(rotateInterval); 
    }
  }, [countdown, sessionStatus, activeSessionId, rotateInterval]); 

  // --- LOGIC POLLING ---
  useEffect(() => {
    if (sessionStatus !== 'active' || !activeSessionId || groupId?.startsWith('mock-')) return;

    const fetchCurrentCheckIns = async () => {
      try {
        const res = await classApi.getAttendanceEvents(activeSessionId, 200); 
        const allEvents = Array.isArray(res) ? res : (res.items || []);

        const latestStatusMap = new Map();
        const sortedEvents = [...allEvents].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
        
        sortedEvents.forEach(event => latestStatusMap.set(String(event.userId), event.newStatus));

        let validCheckInCount = 0;
        latestStatusMap.forEach((status) => {
          if (status === 'PRESENT' || status === 'LATE') validCheckInCount++;
        });
        
        setCheckIns(validCheckInCount);
        if (onCheckInsUpdate) onCheckInsUpdate(validCheckInCount);
      } catch (error) {}
    };

    fetchCurrentCheckIns();
    const pollingTimer = setInterval(fetchCurrentCheckIns, 5000); 

    return () => clearInterval(pollingTimer);
  }, [sessionStatus, activeSessionId, groupId]);

  // --- ACTIONS ---
  const handleStartSession = async () => {
    if (activeSessionId) {
      setSessionStatus('active');
      toast.success("Đã tiếp tục quét mã QR!");
      return;
    }

    if (!groupId || groupId.startsWith('mock-')) {
      const rotateSecs = config.qrRotateSeconds ? parseInt(config.qrRotateSeconds) : 11;
      setSessionStatus('active');
      setActiveSessionId('mock-session-123'); 
      setRotateInterval(rotateSecs);
      setCountdown(rotateSecs);
      toast.success("Đã mở phiên điểm danh (Mock)!");
      return;
    }

    try {
      const timeWindow = config.timeWindowMinutes ? parseInt(config.timeWindowMinutes) : 30;
      const lateAfter = config.lateAfterMinutes ? parseInt(config.lateAfterMinutes) : 15;
      const rotateSecs = config.qrRotateSeconds ? parseInt(config.qrRotateSeconds) : 11;
      const titleStr = config.title.trim() !== '' ? config.title : `Phiên điểm danh ${new Date().toLocaleDateString('vi-VN')}`;

      let currentSessionId = null;
      let finalRotateSecs = rotateSecs;

      try {
        const openSession = await classApi.getOpenSession(groupId);
        if (openSession && openSession.id) {
           const now = new Date();
           const endTime = new Date(openSession.checkinCloseAt || openSession.endAt);
           if (endTime > now) {
               currentSessionId = openSession.id;
               finalRotateSecs = openSession.qrRotateSeconds || rotateSecs;
               toast.success("Đang tiếp tục phiên điểm danh đã mở trước đó!");
           }
        }
      } catch (err) {}

      if (!currentSessionId) {
        const now = new Date();
        const endTime = new Date(now.getTime() + timeWindow * 60000); 
        
        const payload = {
          title: titleStr,
          startAt: now.toISOString(),
          endAt: endTime.toISOString(),
          timeWindowMinutes: timeWindow,
          lateAfterMinutes: lateAfter,
          qrRotateSeconds: rotateSecs,
          checkinOpenAt: now.toISOString(),
          checkinCloseAt: endTime.toISOString(),
          allowManualOverride: true,
          note: "Mở tự động từ Web Admin"
        };
        
        const newSession = await classApi.createSession(groupId, payload);
        currentSessionId = newSession.id;
        toast.success("Đã tạo phiên điểm danh mới!");
      }

      setActiveSessionId(currentSessionId);
      setRotateInterval(finalRotateSecs);
      setCountdown(finalRotateSecs);
      setSessionStatus('active');
      
      await handleRotateQR(currentSessionId);

    } catch (error) {
      toast.error("Lỗi khi mở/tạo phiên điểm danh.");
    }
  };

  const handlePauseSession = () => {
    setSessionStatus('paused');
    toast("Đã tạm dừng phiên điểm danh.");
  };

  const handleSimulateScan = async () => {
    if (sessionStatus !== 'active') return toast.error("Vui lòng Start Session trước!");
    setCheckIns(prev => prev + 1);
    toast.success(`Mô phỏng quét mã thành công!`);
  };

  const handleOpenProjector = () => {
    if (!activeSessionId) {
       toast.error("Vui lòng Bắt đầu phiên điểm danh trước khi chiếu lên màn hình lớn!");
       return;
    }
    setShowProjector(true);
  };

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-500">

      {/* ================= KHU VỰC CỘT TRÁI & PHẢI ================= */}
      <div className="flex flex-col lg:flex-row gap-6 items-start w-full">
      
        {/* ================= CỘT TRÁI: ĐIỀU KHIỂN & CẤU HÌNH ================= */}
        <div className="flex-1 bg-white rounded-xl border border-gray-200 shadow-sm flex flex-col min-h-[640px] w-full">
        <div className="p-4 sm:p-5 border-b border-gray-200 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-gray-50/50">
          <h3 className="font-bold text-gray-900 flex items-center gap-2 text-[15px]">
            <QrCode size={18} className={sessionStatus === 'active' ? 'text-red-600' : 'text-gray-400'}/> Mã QR điểm danh
          </h3>
          <div className="flex flex-wrap sm:flex-nowrap gap-2 w-full sm:w-auto">
            <button 
              onClick={handleSimulateScan}
              className="flex-1 sm:flex-none justify-center border border-indigo-200 bg-indigo-50 text-indigo-600 hover:bg-indigo-100 px-4 py-2 rounded-lg text-sm font-semibold flex items-center gap-2 transition-colors">
              Giả lập quét mã
            </button>
            <button 
              onClick={handleOpenProjector}
              disabled={showProjector}
              className={`flex-1 sm:flex-none justify-center border border-gray-200 px-4 py-2 rounded-lg text-sm font-semibold flex items-center gap-2 transition-colors ${showProjector ? 'bg-gray-100 text-gray-400 cursor-not-allowed' : 'text-gray-600 hover:bg-gray-50'}`}>
              <Maximize size={16}/> {showProjector ? 'Đang chiếu...' : 'Chế độ máy chiếu'}
            </button>
          </div>
        </div>

        <div className="flex-1 flex flex-col items-center justify-center p-10 relative bg-gray-50/30">
          
          {!activeSessionId ? (
            /* --- FORM CẤU HÌNH KHI CHƯA MỞ PHIÊN --- */
            <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 w-full max-w-sm z-10 relative animate-in zoom-in-95 duration-300">
              <div className="flex items-center gap-2 mb-5 text-indigo-600 border-b border-gray-100 pb-3">
                <Settings size={20} />
                <h4 className="font-bold text-gray-800 text-sm uppercase tracking-wider">Thông số phiên mới</h4>
              </div>
              <div className="space-y-4">
                <div>
                  <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-1.5">Tiêu đề (Tùy chọn)</label>
                  <input type="text" placeholder={`VD: Điểm danh ngày ${new Date().toLocaleDateString('vi-VN')}`} className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-50 transition-all text-gray-700" value={config.title} onChange={e => setConfig({...config, title: e.target.value})} />
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-1.5">Mở trong (Phút)</label>
                    <input type="number" placeholder="Mặc định: 30" min="1" className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-50 transition-all text-gray-700" value={config.timeWindowMinutes} onChange={e => setConfig({...config, timeWindowMinutes: e.target.value})} />
                  </div>
                  <div className="flex-1">
                    <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-1.5">Tính trễ sau (Phút)</label>
                    <input type="number" placeholder="Mặc định: 15" min="1" className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-50 transition-all text-gray-700" value={config.lateAfterMinutes} onChange={e => setConfig({...config, lateAfterMinutes: e.target.value})} />
                  </div>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-1.5">Thời gian đổi mã QR (Giây)</label>
                  <input type="number" placeholder="Mặc định: 11" min="5" className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm outline-none focus:border-indigo-400 focus:ring-2 focus:ring-indigo-50 transition-all text-gray-700" value={config.qrRotateSeconds} onChange={e => setConfig({...config, qrRotateSeconds: e.target.value})} />
                </div>
              </div>
            </div>
          ) : (
            /* --- KHUNG QUÉT QR KHI ĐANG HOẠT ĐỘNG --- */
            <>
              <div className={`relative w-72 h-72 mb-10 flex items-center justify-center transition-opacity ${sessionStatus === 'paused' ? 'opacity-50 blur-[2px]' : 'opacity-100'}`}>
                <div className="absolute top-0 left-0 w-8 h-8 border-t-4 border-l-4 border-gray-300 rounded-tl-xl"></div>
                <div className="absolute top-0 right-0 w-8 h-8 border-t-4 border-r-4 border-gray-300 rounded-tr-xl"></div>
                <div className="absolute bottom-0 left-0 w-8 h-8 border-b-4 border-l-4 border-gray-300 rounded-bl-xl"></div>
                <div className="absolute bottom-0 right-0 w-8 h-8 border-b-4 border-r-4 border-gray-300 rounded-br-xl"></div>
                
                {sessionStatus === 'active' && (
                  <div className="absolute top-8 left-4 right-4 h-0.5 bg-red-500 shadow-[0_0_12px_rgba(239,68,68,0.8)] z-10 animate-[scan_2s_ease-in-out_infinite]"></div>
                )}

                <div className="bg-white p-4 rounded-xl shadow-sm border border-gray-100 flex flex-col items-center justify-center w-56 h-56">
                  {sessionStatus === 'active' && activeSessionId && qrCodeData !== 'NO_CODE_RETURNED' ? (
                    <QRCodeSVG 
                       value={`ATTEND:${activeSessionId}:${qrCodeData}`} 
                       size={200} 
                       level="M" 
                       includeMargin={true}
                    />
                  ) : (
                    <QrCode size={180} strokeWidth={1} className="text-gray-300" />
                  )}
                </div>
                
                {sessionStatus === 'paused' && (
                  <div className="absolute inset-0 flex items-center justify-center">
                     <span className="bg-[#111827] text-white px-4 py-2 rounded-lg font-bold shadow-lg tracking-wider">PHIÊN ĐANG TẠM DỪNG</span>
                  </div>
                )}
              </div>

              <div className="flex flex-col items-center">
                <div className="relative w-14 h-14 flex items-center justify-center mb-3">
                  <svg className="w-full h-full transform -rotate-90" viewBox="0 0 36 36">
                    <path className="text-gray-200" strokeWidth="3" fill="none" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
                    <path 
                      className={sessionStatus === 'active' ? "text-[#111827] transition-all duration-1000 ease-linear" : "text-gray-400"} 
                      strokeDasharray={`${(countdown / rotateInterval) * 100}, 100`} 
                      strokeWidth="3" fill="none" 
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" 
                    />
                  </svg>
                  <span className="absolute text-xl font-bold text-gray-900">{sessionStatus === 'active' ? countdown : '--'}</span>
                </div>
                <span className="text-gray-400 font-medium text-sm">Giây cho đến lần làm mới tiếp theo</span>
              </div>
            </>
          )}
        </div>

        <div className="p-4 sm:p-5 border-t border-gray-200 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white">
          <div className="flex flex-wrap sm:flex-nowrap gap-3 w-full sm:w-auto">
            {sessionStatus === 'active' ? (
              <button onClick={handlePauseSession} className="flex-1 sm:flex-none justify-center bg-[#111827] hover:bg-gray-800 text-white px-5 py-2.5 rounded-lg text-sm font-semibold flex items-center gap-2 transition-colors">
                <Pause size={16}/> Tạm dừng quét
              </button>
            ) : (
              <button onClick={handleStartSession} className="flex-1 sm:flex-none justify-center bg-red-600 hover:bg-red-700 text-white px-5 py-2.5 rounded-lg text-sm font-semibold flex items-center gap-2 transition-colors">
                <Play size={16}/> {activeSessionId ? 'Tiếp tục phiên' : 'Bắt đầu phiên'}
              </button>
            )}
            <button className="flex-1 sm:flex-none justify-center bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 px-5 py-2.5 rounded-lg text-sm font-semibold flex items-center gap-2 transition-colors">
              <Plus size={16} className="text-gray-400"/> Gia hạn thêm (+10p)
            </button>
          </div>
          <div className="flex w-full sm:w-auto justify-center items-center gap-2 text-emerald-500 text-sm font-bold bg-emerald-50 px-4 py-2.5 rounded-lg border border-emerald-100">
            <ShieldCheck size={18}/> Xác thực thiết bị đang bật
          </div>
        </div>
      </div>

      {/* ================= CỘT PHẢI: THÔNG SỐ VÀ CẢNH BÁO ================= */}
      <div className="w-full lg:w-[340px] shrink-0 flex flex-col gap-6">
        
        {/* Panel 1: Live Attendance */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6">
          <h3 className="text-[11px] font-bold text-gray-500 tracking-wider mb-5 flex items-center gap-2 uppercase">
            <BarChart2 size={16}/> SỐ LƯỢNG ĐIỂM DANH
          </h3>
          <div className="flex justify-between items-end mb-2">
            <div className="text-4xl font-bold text-[#111827] tracking-tight">{checkIns} <span className="text-xl text-gray-400 font-medium tracking-normal">/ {totalStudents}</span></div>
            <div className="text-emerald-500 font-bold text-lg">{attendanceRate}%</div>
          </div>
          <div className="text-xs font-semibold text-gray-500 mb-2 flex justify-between">
            <span>Lượt quét hợp lệ</span><span className="text-gray-900">{checkIns}</span>
          </div>
          <div className="w-full h-2.5 bg-gray-100 rounded-full mb-6 overflow-hidden">
            <div className="h-full bg-[#111827] rounded-full transition-all duration-500" style={{width: `${attendanceRate}%`}}></div>
          </div>
        </div>

        {/* Panel 2 & 3: Cảnh báo bảo mật */}
        <div className="bg-white rounded-xl border border-red-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-4 border-b border-red-100 bg-red-50/50 flex justify-between items-center">
            <h3 className="font-bold text-red-700 flex items-center gap-2 text-[13px] uppercase tracking-wider"><AlertTriangle size={16} /> HOẠT ĐỘNG NGHI VẤN</h3>
            <span className="bg-red-600 text-white text-[11px] font-bold px-2 py-0.5 rounded">{securityAlerts.length} Mới</span>
          </div>
          <div className="p-5 space-y-4 flex-1">
            {securityAlerts.length === 0 ? (
               <div className="text-center text-gray-400 font-medium py-4 text-sm">Không có hoạt động nghi vấn nào</div>
            ) : securityAlerts.map(alert => (
               <div key={alert.id} className="pb-4 border-b border-gray-100 last:border-0 last:pb-0">
                 <div className="flex justify-between items-start mb-1.5">
                   <div className="flex gap-2 items-center">
                      <div className="w-6 h-6 rounded-full bg-red-100 flex items-center justify-center shrink-0">
                         {FRAUD_TYPE_CONFIG[alert.fraudType]?.icon || <AlertTriangle size={14} className="text-red-500" />}
                      </div>
                      <h4 className="text-[14px] font-bold text-gray-900">{alert.title}</h4>
                   </div>
                   <span className="text-[10px] text-gray-400 font-medium">{alert.time}</span>
                 </div>
                 <p className="text-xs text-gray-500 mb-2.5 leading-relaxed" dangerouslySetInnerHTML={{ __html: alert.desc }}></p>
                 {alert.severity === 'HIGH' || alert.severity === 'CRITICAL' ? (
                   <span className="text-[10px] font-bold text-red-600 bg-red-50 border border-red-100 px-2 py-0.5 rounded">RỦI RO CAO</span>
                 ) : (
                   <span className="text-[10px] font-bold text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded">RỦI RO VỪA</span>
                 )}
               </div>
            ))}
          </div>
        </div>
      </div>
      </div>

      <style>{`
        @keyframes scan { 0% { top: 0%; opacity: 0; } 10% { opacity: 1; } 90% { opacity: 1; } 100% { top: 100%; opacity: 0; } }
      `}</style>

      {/* ========================================================================= */}
      {/* RENDER MÀN HÌNH MÁY CHIẾU (FULLSCREEN MODAL) */}
      {/* ========================================================================= */}
      {showProjector && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center bg-gray-900/60 backdrop-blur-sm p-6 lg:p-10 font-sans animate-in fade-in duration-300">
          {/* Main Floating Box */}
          <div className="bg-white w-full h-full max-w-7xl max-h-[900px] rounded-[32px] shadow-2xl overflow-hidden flex flex-col animate-in zoom-in-95 duration-300 relative border border-gray-100">
              
              {/* Close Button Floating Top Right */}
              <button 
                onClick={() => setShowProjector(false)}
                className="absolute top-6 right-6 bg-gray-100 hover:bg-gray-200 text-gray-500 hover:text-gray-900 p-3 rounded-full shadow-sm transition-colors z-10"
              >
                <X size={24} />
              </button>

              {/* Header */}
              <div className="p-6 lg:p-8 pb-0 text-center">
                  <h1 className="text-3xl md:text-4xl lg:text-5xl text-[#111827] font-extrabold uppercase tracking-tight">
                     {config.title || 'QUÉT MÃ ĐỂ ĐIỂM DANH'}
                  </h1>
                  
                  {/* Detailed Class Info Bar */}
                  <div className="flex flex-wrap items-center justify-center gap-4 lg:gap-8 bg-gray-50 border border-gray-200 rounded-2xl p-4 lg:p-5 mt-6 mx-auto max-w-6xl shadow-sm">
                      
                      {/* Mã Lớp */}
                      <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 shrink-0">
                             <BookOpen size={20} />
                          </div>
                          <div className="text-left">
                              <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-0.5">Mã Lớp</div>
                              <div className="text-sm font-bold text-gray-900">{classDetail?.courseCode || 'BAS1234'} <span className="text-gray-300 mx-1">|</span> {classDetail?.classCode || 'D22PM2'}</div>
                          </div>
                      </div>
                      <div className="hidden lg:block w-px h-10 bg-gray-200"></div>
                      
                      {/* Học Kỳ */}
                      <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 shrink-0">
                             <CalendarDays size={20} />
                          </div>
                          <div className="text-left">
                              <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-0.5">Học Kỳ</div>
                              <div className="text-sm font-bold text-gray-900">{classDetail?.semester?.includes('HK') ? classDetail.semester : `HK${classDetail?.semester || '1'}`} <span className="text-gray-300 mx-1">|</span> {classDetail?.academicYear || '2025-2026'}</div>
                          </div>
                      </div>
                      <div className="hidden lg:block w-px h-10 bg-gray-200"></div>

                      {/* Phòng Học */}
                      <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-purple-100 flex items-center justify-center text-purple-600 shrink-0">
                             <MapPin size={20} />
                          </div>
                          <div className="text-left">
                              <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-0.5">Phòng học</div>
                              <div className="text-sm font-bold text-gray-900">{classDetail?.room || '2E03'}</div>
                          </div>
                      </div>
                      <div className="hidden lg:block w-px h-10 bg-gray-200"></div>
                      
                      {/* Lịch Học */}
                      <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center text-orange-600 shrink-0">
                             <Clock size={20} />
                          </div>
                          <div className="text-left">
                              <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-0.5">Lịch Học</div>
                              <div className="text-sm font-bold text-gray-900">MONDAY 17:00-22:30</div>
                          </div>
                      </div>
                      <div className="hidden lg:block w-px h-10 bg-gray-200"></div>

                      {/* Buổi tiếp theo */}
                      <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-600 shrink-0">
                             <CalendarDays size={20} />
                          </div>
                          <div className="text-left">
                              <div className="text-[10px] uppercase font-bold text-gray-400 tracking-wider mb-0.5">Buổi tiếp theo</div>
                              <div className="text-sm font-bold text-gray-900">{new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toLocaleDateString('vi-VN')}</div>
                          </div>
                      </div>
                  </div>
              </div>
              
              {/* Main Content Area */}
              <div className="flex-1 flex flex-col md:flex-row items-stretch justify-center gap-6 lg:gap-12 xl:gap-20 p-6 lg:p-10 overflow-hidden">
                  
                  {/* Left: QR Code */}
                  <div className="p-4 lg:p-8 bg-white rounded-[24px] lg:rounded-[32px] shadow-[0_10px_40px_-10px_rgba(0,0,0,0.15)] border border-gray-100 flex items-center justify-center shrink-0">
                     {sessionStatus === 'active' && activeSessionId && qrCodeData !== 'NO_CODE_RETURNED' ? (
                          <QRCodeSVG 
                             value={`ATTEND:${activeSessionId}:${qrCodeData}`} 
                             size={Math.min(window.innerHeight - 350, window.innerWidth * 0.4, 600)} 
                             level="M" 
                             includeMargin={true}
                             style={{ width: '100%', height: '100%', maxWidth: '600px', maxHeight: '600px' }}
                          />
                        ) : (
                          <div style={{ width: Math.min(window.innerHeight - 350, window.innerWidth * 0.4, 600), height: Math.min(window.innerHeight - 350, window.innerWidth * 0.4, 600) }} className="flex items-center justify-center bg-gray-50 rounded-2xl border-2 border-dashed border-gray-200 text-gray-400 text-xl md:text-2xl font-bold p-4 text-center">
                             {sessionStatus === 'paused' ? 'PHIÊN ĐANG TẠM DỪNG' : 'ĐANG TẢI DỮ LIỆU...'}
                          </div>
                        )}
                  </div>
                  
                  {/* Right: Info & Countdown */}
                  <div className="flex flex-col justify-center flex-1 min-w-[280px] max-w-xl w-full">
                       <div className="bg-red-50 p-6 lg:p-10 rounded-[24px] lg:rounded-[32px] border border-red-100 flex flex-col items-center text-center shadow-inner flex-1 justify-center min-h-[200px]">
                           <div className="text-red-600 uppercase tracking-widest font-bold text-xs lg:text-sm mb-2">
                               Mã sẽ tự làm mới sau
                           </div>
                           <div className={`text-[clamp(5rem,12vw,11rem)] font-black leading-none tabular-nums tracking-tighter ${sessionStatus === 'active' ? 'text-red-600' : 'text-red-300'}`}>
                               {sessionStatus === 'active' ? countdown : '--'}
                           </div>
                           <div className="text-red-500 font-bold uppercase tracking-widest mt-2 lg:mt-4 text-xs lg:text-base">
                               giây
                           </div>
                       </div>
                       
                       <div className="mt-4 lg:mt-8 bg-[#111827] text-white p-6 lg:p-8 rounded-[24px] lg:rounded-[32px] shadow-2xl flex items-center justify-between shrink-0">
                           <div>
                               <div className="text-gray-400 font-bold uppercase tracking-wider text-[10px] lg:text-xs mb-1 lg:mb-2">Tiến độ điểm danh</div>
                               <div className="text-3xl lg:text-5xl font-black text-white leading-tight">
                                   {checkIns} <span className="text-lg lg:text-2xl text-gray-500 font-medium">/ {totalStudents}</span>
                               </div>
                           </div>
                           <div className="h-12 w-12 lg:h-16 lg:w-16 rounded-full bg-emerald-500/20 flex items-center justify-center border border-emerald-500/30 shrink-0">
                               <BarChart2 className="w-6 h-6 lg:w-8 lg:h-8 text-emerald-400" />
                           </div>
                       </div>
                  </div>

              </div>
          </div>
        </div>
      )}

    </div>
  );
}