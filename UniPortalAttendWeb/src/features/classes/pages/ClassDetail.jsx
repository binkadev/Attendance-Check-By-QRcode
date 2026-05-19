import { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  Bell, Users, Clock, 
  CalendarX, MapPin, Calendar, AlertTriangle, ChevronRight, 
  Loader2, QrCode, CalendarDays, BookOpen
} from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// IMPORT CÁC COMPONENT TAB ĐÃ TÁCH
import StudentsTab from '../components/StudentsTab';
import DynamicQRTab from '../components/DynamicQRTab';
import SessionHistoryTab from '../components/SessionHistoryTab';
import JoinClassModal from '../components/JoinClassModal';
import FraudTab from '../components/FraudTab';
import AbsenceTab from '../components/AbsenceTab';

const generateScheduledSessionsList = (startDateStr, totalSessions, weeklySchedules) => {
  if (!startDateStr || !totalSessions || !weeklySchedules || weeklySchedules.length === 0) {
    return [];
  }
  
  const startDate = new Date(startDateStr);
  if (isNaN(startDate.getTime())) return [];

  const dayOrder = { 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 0 };
  
  const scheduleConfig = weeklySchedules.map(s => ({
    dayOfWeekNum: dayOrder[s.dayOfWeek],
    startTime: s.startTime,
    endTime: s.endTime
  }));

  let currentDate = new Date(startDate.getTime());
  let sessions = [];
  let sessionsRemaining = totalSessions;
  
  const startDayOfWeekNum = currentDate.getDay();
  const matchingConfig = scheduleConfig.find(c => c.dayOfWeekNum === startDayOfWeekNum);
  if (matchingConfig) {
    sessions.push({
      date: new Date(currentDate),
      startTime: matchingConfig.startTime,
      endTime: matchingConfig.endTime
    });
    sessionsRemaining--;
  }
  
  while (sessionsRemaining > 0) {
    currentDate.setDate(currentDate.getDate() + 1);
    const dayOfWeekNum = currentDate.getDay();
    const currentConfig = scheduleConfig.find(c => c.dayOfWeekNum === dayOfWeekNum);
    if (currentConfig) {
      sessions.push({
        date: new Date(currentDate),
        startTime: currentConfig.startTime,
        endTime: currentConfig.endTime
      });
      sessionsRemaining--;
    }
  }
  
  return sessions;
};

// --- MOCK DATA CHO THÔNG TIN LỚP ---
const MOCK_CLASSES_LIST = [
  { 
    groupId: 'mock-1', 
    courseCode: 'CS101', 
    classCode: 'Group 01', 
    name: 'Nhập môn lập trình', 
    room: 'Hội trường 1', 
    semester: 'Kỳ Thu', 
    academicYear: '2023' 
  },
  { 
    groupId: 'mock-2', 
    courseCode: 'CS204', 
    classCode: 'Group 02', 
    name: 'Cấu trúc dữ liệu & Giải thuật', 
    room: 'Phòng 3D2', semester: 'Kỳ Xuân', 
    academicYear: '2024' 
  }
];

export default function ClassDetail() {
  const { classId } = useParams(); 
  const location = useLocation();
  const [activeTab, setActiveTab] = useState(location.state?.activeTab || 'students');

  useEffect(() => {
    if (location.state?.activeTab) {
      Promise.resolve().then(() => {
        setActiveTab(location.state.activeTab);
      });
    }
  }, [location.state?.activeTab]);

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });
  
  const [classDetail, setClassDetail] = useState(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(true);

  const [showJoinModal, setShowJoinModal] = useState(false);

  // States cho thống kê động
  const [totalStudents, setTotalStudents] = useState(0);
  const [latestAttendance, setLatestAttendance] = useState(0);

  const [fraudCount, setFraudCount] = useState(0);
  const [absenceCount, setAbsenceCount] = useState(0);
  const [sessionsCount, setSessionsCount] = useState(0);

  // States cho đồng hồ đếm ngược
  const [openSession, setOpenSession] = useState(null);
  const [timeRemaining, setTimeRemaining] = useState('--:--');
  const [nextSessionDate, setNextSessionDate] = useState('');

  useEffect(() => {
    console.log("Dynamic attendance count:", latestAttendance);
  }, [latestAttendance]);

  // TÍNH TOÁN NGÀY HỌC TIẾP THEO THEO LỊCH TUẦN THỰC TẾ
  useEffect(() => {
    if (!classDetail || !classDetail.weeklySchedules || classDetail.weeklySchedules.length === 0) {
      setNextSessionDate('---');
      return;
    }

    try {
      const now = new Date();
      const dayOrder = { 0: 'SUNDAY', 1: 'MONDAY', 2: 'TUESDAY', 3: 'WEDNESDAY', 4: 'THURSDAY', 5: 'FRIDAY', 6: 'SATURDAY' };
      const daysOfWeekVi = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
      
      let foundNextDate = null;
      let minDiff = Infinity;
      let nextSchedInfo = null;

      // Quét 14 ngày tới để tìm mốc thời gian khớp lịch học gần nhất
      for (let offset = 0; offset <= 14; offset++) {
        const d = new Date(now);
        d.setDate(now.getDate() + offset);
        
        const dayOfWeekName = dayOrder[d.getDay()];
        const sched = classDetail.weeklySchedules.find(s => s.dayOfWeek === dayOfWeekName);

        if (sched) {
          const [startH, startM] = sched.startTime.split(':').map(Number);
          const schedDateTime = new Date(d);
          schedDateTime.setHours(startH, startM, 0, 0);

          const diff = schedDateTime.getTime() - now.getTime();
          if (diff > 0 && diff < minDiff) {
            minDiff = diff;
            foundNextDate = schedDateTime;
            nextSchedInfo = sched;
          }
        }
      }

      if (foundNextDate && nextSchedInfo) {
        const dayNameVi = daysOfWeekVi[foundNextDate.getDay()];
        const formattedDate = foundNextDate.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
        setNextSessionDate(`${dayNameVi}, ${formattedDate} (${nextSchedInfo.startTime} - ${nextSchedInfo.endTime})`);
      } else {
        setNextSessionDate('---');
      }
    } catch (err) {
      console.error("Lỗi tính lịch học tiếp theo:", err);
      setNextSessionDate('---');
    }
  }, [classDetail]);

  // GỌI API LẤY THÔNG TIN TỔNG QUAN LỚP HỌC
  useEffect(() => {
    const fetchClassStats = async () => {
      if (!classId || classId.startsWith('mock-')) {
        setFraudCount(0);
        setAbsenceCount(0);
        setSessionsCount(0);
        return;
      }
      try {
        const [membersRes, sessionsRes, summaryRes, fraudRes, absenceRes] = await Promise.all([
          classApi.getClassMembers(classId),
          classApi.getGroupSessions(classId),
          classApi.getAttendanceSummary(classId).catch(() => null),
          classApi.getFraudIncidents(classId).catch(() => null),
          classApi.getAbsenceRequests(classId).catch(() => null)
        ]);

        const allMembers = Array.isArray(membersRes) ? membersRes : (membersRes.items || []);
        const students = allMembers.filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER');
        
        // Nếu API summary trả về totalStudents thì dùng, không thì đếm tay
        setTotalStudents(summaryRes?.totalStudents || students.length);

        // Lấy số lượng sự cố gian lận và đơn xin phép
        const realFraud = fraudRes?.totalElements ?? fraudRes?.items?.length ?? fraudRes?.length ?? 0;
        const realAbsence = absenceRes?.totalElements ?? absenceRes?.items?.length ?? absenceRes?.length ?? 0;
        
        setFraudCount(realFraud);
        setAbsenceCount(realAbsence);

        const allSessions = sessionsRes.items || [];
        setSessionsCount(sessionsRes?.totalElements ?? sessionsRes?.items?.length ?? allSessions.length);
        
        if (allSessions.length > 0) {
          const sortedSessions = allSessions.sort((a, b) => new Date(b.checkinOpenAt) - new Date(a.checkinOpenAt));
          const latestSession = sortedSessions[0];
          
          // Lấy chính xác số lượng từ events để luôn đồng bộ
          try {
            const eventRes = await classApi.getAttendanceEvents(latestSession.id, 200);
            const events = Array.isArray(eventRes) ? eventRes : (eventRes.items || []);
            const latestStatusMap = new Map();
            const sortedEvents = [...events].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
            sortedEvents.forEach(e => latestStatusMap.set(String(e.userId), e.newStatus));
            let validCount = 0;
            latestStatusMap.forEach(status => {
              if (status === 'PRESENT' || status === 'LATE') validCount++;
            });
            setLatestAttendance(validCount);
          } catch (e) {
            console.error("Lỗi lấy sự kiện điểm danh:", e);
            setLatestAttendance(latestSession.checkIns || latestSession.checkinCount || 0);
          }
        }
      } catch (error) {
        console.error("Lỗi lấy thông tin thống kê:", error);
      }
    };

    const loadClassDetail = async () => {
      setIsLoadingDetail(true);
      
      // Nếu là ID giả lập
      if (classId?.startsWith('mock-')) {
        setClassDetail(MOCK_CLASSES_LIST.find(c => c.groupId === classId) || null);
        setIsLoadingDetail(false);
        return;
      }

      // Nếu là ID thật
      try {
        const res = await classApi.getClassDetail(classId);
        setClassDetail(res);
      } catch (error) {
        console.error("Lỗi lấy chi tiết lớp", error);
        toast.error("Không thể tải thông tin lớp học.");
      } finally {
        setIsLoadingDetail(false);
      }
    };

    if (classId) {
      loadClassDetail();
      fetchClassStats();

      // Polling liên tục mỗi 5s để cập nhật sĩ số điểm danh thực tế
      const pollStats = setInterval(async () => {
        if (classId.startsWith('mock-')) return;
        try {
          const sessionsRes = await classApi.getGroupSessions(classId);
          const allSessions = sessionsRes.items || [];
          if (allSessions.length > 0) {
            const sortedSessions = allSessions.sort((a, b) => new Date(b.checkinOpenAt) - new Date(a.checkinOpenAt));
            const latestSession = sortedSessions[0];
            
            try {
              const eventRes = await classApi.getAttendanceEvents(latestSession.id, 200);
              const events = Array.isArray(eventRes) ? eventRes : (eventRes.items || []);
              const latestStatusMap = new Map();
              const sortedEvents = [...events].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
              sortedEvents.forEach(e => latestStatusMap.set(String(e.userId), e.newStatus));
              let validCount = 0;
              latestStatusMap.forEach(status => {
                if (status === 'PRESENT' || status === 'LATE') validCount++;
              });
              setLatestAttendance(validCount);
            } catch (e) {
              console.error("Lỗi lấy sự kiện điểm danh polling:", e);
              setLatestAttendance(latestSession.checkIns || latestSession.checkinCount || 0);
            }
          }
        } catch (error) {
          console.warn("Lỗi polling stats:", error);
        }
      }, 5000);

      return () => clearInterval(pollStats);
    }
  }, [classId]);

  // LẤY OPEN SESSION - CHẠY LUÔN, KHÔNG PHỤ THUỘC VÀO TAB
  useEffect(() => {
    if (!classId || classId.startsWith('mock-')) {
      Promise.resolve().then(() => {
        setTimeRemaining(prev => prev === '--:--' ? prev : '--:--');
      });
      return;
    }

    const fetchSession = async () => {
      try {
        const session = await classApi.getOpenSession(classId);
        setOpenSession(session);
      } catch (error) {
        console.warn("Lỗi getOpenSession:", error);
      }
    };

    // Fetch ngay lập tức và poll mỗi 15s để bắt phiên mới mở
    fetchSession();
    const pollSession = setInterval(fetchSession, 15000);
    return () => clearInterval(pollSession);
  }, [classId]);

  // ĐỒNG HỒ ĐẾM NGƯỢC - CHẠY LUÔN DỰA TRÊN openSession
  useEffect(() => {
    if (!openSession?.checkinCloseAt) {
      Promise.resolve().then(() => {
        setTimeRemaining(prev => prev === '--:--' ? prev : '--:--');
      });
      return;
    }

    const closeTime = new Date(openSession.checkinCloseAt).getTime();

    const updateTimer = () => {
      const distance = closeTime - Date.now();
      if (distance <= 0) {
        setTimeRemaining('00:00');
        return;
      }
      const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((distance % (1000 * 60)) / 1000);
      setTimeRemaining(`${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`);
    };

    updateTimer(); // Cập nhật ngay
    const timer = setInterval(updateTimer, 1000);
    return () => clearInterval(timer);
  }, [openSession]);

  return (
    <div className="flex min-h-screen bg-[#F8FAFC] font-sans">
      {/* SIDEBAR DÙNG CHUNG */}
      <Sidebar 
        onCollapseChange={setIsSidebarCollapsed} 
        activeTab={activeTab} 
        onTabChange={setActiveTab} 
      />
      {/* SIDEBAR DÙNG CHUNG */}
      {/* <aside className="w-64 bg-[#111827] min-h-screen flex flex-col text-gray-300 shrink-0">
        <div className="p-6 flex items-center gap-3 border-b border-gray-800">
          <div className="w-8 h-8 bg-red-700 rounded flex items-center justify-center text-white font-bold"><ShieldAlert size={20} /></div>
          <div>
            <h1 className="text-white font-bold text-lg leading-tight">EduGuard</h1>
            <p className="text-xs text-gray-400">Lecturer Portal</p>
          </div>
        </div>
        <div className="flex-1 py-6 space-y-1">
            <button onClick={() => setActiveTab('students')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'students' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <Users size={18} className={activeTab === 'students' ? 'text-red-500' : ''} /> Students
            </button>
            <button onClick={() => setActiveTab('dynamic-qr')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'dynamic-qr' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <QrCode size={18} className={activeTab === 'dynamic-qr' ? 'text-red-500' : ''}/> Dynamic QR Session
            </button>
            <button onClick={() => setActiveTab('fraud')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'fraud' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <AlertTriangle size={18} className={activeTab === 'fraud' ? 'text-red-500' : ''}/> Fraud Incidents
            </button>
            <button onClick={() => setActiveTab('absence')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'absence' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <CalendarX size={18} className={activeTab === 'absence' ? 'text-red-500' : ''}/> Absence Requests
            </button>
        </div>
      </aside> */}

      
      {/* NỘI DUNG CHÍNH */}
      <main className={`flex-1 flex flex-col h-screen overflow-y-auto transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        {/* HEADER DÙNG CHUNG */}
        {/* <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-50">
          <div>
            <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
              <Link to="/classes" className="hover:text-red-600 transition-colors">Class Management</Link>
              <ChevronRight size={14} />
              {isLoadingDetail ? <div className="h-4 w-20 bg-gray-100 animate-pulse rounded"></div> : (
                // <span className="font-medium text-gray-900">{classDetail?.courseCode || '---'}</span>
                <span className="font-medium text-gray-900">
                  {classDetail?.courseCode ? `${classDetail.courseCode}: ${classDetail.name}` : 'Chưa cập nhật'}
                </span>
              )}
            </div>
            <h1 className="text-2xl font-bold text-[#111827]">Class Details</h1>
          </div>
          <button className="p-2.5 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm"><Bell size={18} /></button>
        </header>

        <div className="p-8 max-w-7xl w-full mx-auto flex-1 flex flex-col"> */}

        {/* HEADER DÙNG CHUNG */}
        <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-50">
          <div>
            <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
              <Link to="/classes" className="hover:text-red-600 transition-colors">Quản lý lớp học</Link>
              <ChevronRight size={14} />
              {isLoadingDetail ? <div className="h-4 w-20 bg-gray-100 animate-pulse rounded"></div> : (
                <span className="font-medium text-gray-900">
                  {classDetail?.courseCode ? `${classDetail.courseCode}: ${classDetail.name}` : 'Chưa cập nhật'}
                </span>
              )}
            </div>
            <h1 className="text-2xl font-bold text-[#111827]">Chi tiết lớp học</h1>
          </div>

          {/* <button className="p-2.5 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm"><Bell size={18} /></button> */}

          {/* Khu vực các nút bấm bên phải */}
          <div className="flex items-center gap-3">
            {/* NÚT MỞ MÃ QR THAM GIA LỚP */}
            <button 
              onClick={() => setShowJoinModal(true)}
              className="bg-white border border-gray-200 text-gray-700 hover:bg-gray-50 px-4 py-2 rounded-lg text-sm font-bold shadow-sm transition-colors flex items-center gap-2"
            >
              <QrCode size={16} className="text-gray-500" />
              Mã tham gia lớp
            </button>

            <button className="p-2.5 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm">
              <Bell size={18} />
            </button>
          </div>
        </header>

        <div className="p-8 max-w-[1600px] 2xl:max-w-[1800px] w-full mx-auto flex-1 flex flex-col">
          
          {/* OVERVIEW CARD (Thay đổi theo Tab) */}
          {activeTab === 'dynamic-qr' ? (
             <div className="bg-white rounded-xl border border-red-200 p-4 lg:p-6 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 mb-8 shadow-sm relative overflow-hidden">
               <div className="absolute top-0 left-0 w-1.5 h-full bg-red-600"></div>
               <div className="flex items-start lg:items-center gap-4 lg:gap-5 pl-2 w-full lg:w-auto">
                 <div className="w-12 h-12 lg:w-14 lg:h-14 bg-[#111827] rounded-xl flex items-center justify-center text-white font-bold text-lg lg:text-xl uppercase shrink-0">
                   {classDetail?.courseCode?.substring(0,2) || 'CL'}
                 </div>
                 <div>
                   <div className="flex items-center gap-3 mb-2">
                     <h2 className="text-xl font-bold text-gray-900">{classDetail?.name || 'Đang tải...'}</h2>
                     <span className="bg-red-50 text-red-600 text-[11px] font-bold px-2.5 py-1 rounded-md flex items-center gap-1.5 border border-red-100">
                       <span className="w-1.5 h-1.5 bg-red-600 rounded-full animate-pulse"></span> PHIÊN TRỰC TIẾP
                     </span>
                   </div>
                   <div className="flex flex-wrap items-center gap-y-2 gap-x-5 text-[13px] text-gray-500 mt-2">
                     <span className="flex items-center gap-1.5">
                       <BookOpen size={15} className="text-blue-500"/>
                       <span className="font-bold text-gray-800">{classDetail?.courseCode || 'BAS1234'} <span className="text-gray-300 mx-1">|</span> {classDetail?.classCode || 'D22PM2'}</span>
                     </span>
                     <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>
                     
                     <span className="flex items-center gap-1.5">
                       <CalendarDays size={15} className="text-indigo-500"/>
                       <span className="font-bold text-gray-800">{classDetail?.semester?.includes('HK') ? classDetail.semester : `HK${classDetail?.semester || '1'}`} <span className="text-gray-300 mx-1">|</span> {classDetail?.academicYear || '2025-2026'}</span>
                     </span>
                     <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                     <span className="flex items-center gap-1.5">
                       <MapPin size={15} className="text-purple-500"/>
                       <span className="font-bold text-gray-800">{classDetail?.room || '2E03'}</span>
                     </span>
                     <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                     <span className="flex items-center gap-1.5">
                       <Clock size={15} className="text-orange-500"/>
                       <span className="font-bold text-gray-800">
                         {classDetail?.weeklySchedules?.length > 0 
                           ? classDetail.weeklySchedules.map(s => `${s.dayOfWeek} ${s.startTime}-${s.endTime}`).join(', ') 
                           : classDetail?.schedule || 'THỨ HAI 17:00-22:30'}
                       </span>
                     </span>
                     {classDetail?.startDate && (
                       <>
                         <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>
                         <span className="flex items-center gap-1.5">
                           <Calendar size={15} className="text-teal-500"/>
                           <span className="font-bold text-gray-800">
                             Dự kiến: {(() => {
                               const start = new Date(classDetail.startDate);
                               const total = classDetail.totalSessions || 15;
                               const end = new Date(start.getTime() + (total - 1) * 7 * 24 * 60 * 60 * 1000);
                               const f = (d) => d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
                               return `${f(start)} - ${f(end)}`;
                             })()}
                           </span>
                         </span>
                       </>
                     )}
                     <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                      <span className="flex items-center gap-1.5">
                        <CalendarDays size={15} className="text-emerald-500"/>
                        <span className="font-bold text-gray-800">
                          Tiếp theo: {nextSessionDate || '---'}
                        </span>
                      </span>
                   </div>
                 </div>
               </div>
               <div className="flex items-center justify-between lg:justify-end gap-3 lg:gap-5 w-full lg:w-auto border-t lg:border-t-0 pt-4 lg:pt-0 border-gray-100 pl-2 lg:pl-0 mt-2 lg:mt-0">
                 <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 lg:px-5 py-2 text-left lg:text-right flex-1 lg:flex-none">
                   <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">THỜI GIAN CÒN LẠI</p>
                   <p className="text-2xl font-bold text-gray-900 tabular-nums leading-none tracking-tight">{timeRemaining}</p>
                 </div>
                 <button onClick={async () => {
                    if (openSession?.id) {
                      try {
                        await classApi.closeSession(openSession.id);
                        toast.success("Đã đóng phiên điểm danh");
                      } catch (err) {
                        console.error("Lỗi đóng phiên:", err);
                        toast.success("Đã đóng phiên (Tính năng Mock do lỗi API)");
                      }
                    }
                    setOpenSession(null);
                    setActiveTab('students');
                  }} className="bg-white border border-red-200 text-red-600 hover:bg-red-50 px-5 py-3.5 rounded-lg text-sm font-bold shadow-sm flex items-center gap-2 transition-colors">
                   <div className="w-3.5 h-3.5 bg-red-600 rounded-[3px]"></div> Kết thúc phiên
                 </button>
               </div>
             </div>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-4 lg:p-6 flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 mb-8 shadow-sm">
              {isLoadingDetail ? (
                <div className="flex items-center gap-4 py-4"><Loader2 className="animate-spin text-red-600" /> Đang tải thông tin lớp...</div>
              ) : (
                <>
                  <div className="flex items-start lg:items-center gap-4 lg:gap-5 w-full lg:w-auto">
                    <div className="w-12 h-12 lg:w-14 lg:h-14 bg-[#111827] rounded-xl flex items-center justify-center text-white font-bold text-lg lg:text-xl uppercase shrink-0">
                      {classDetail?.courseCode?.substring(0,2) || 'CS'}
                    </div>
                    <div>
                      <div className="flex items-center gap-3 mb-2">
                        <h2 className="text-xl font-bold text-gray-900">{classDetail?.name || 'Cấu trúc dữ liệu & Giải thuật'}</h2>
                        <span className="bg-gray-100 text-gray-600 text-[11px] font-bold px-2.5 py-1 rounded-md uppercase tracking-wider">PHIÊN ĐÃ KẾT THÚC</span>
                      </div>
                      <div className="flex flex-wrap items-center gap-y-2 gap-x-5 text-[13px] text-gray-500 mt-2">
                        <span className="flex items-center gap-1.5">
                          <BookOpen size={15} className="text-blue-500"/>
                          <span className="font-bold text-gray-800">{classDetail?.courseCode || 'BAS1234'} <span className="text-gray-300 mx-1">|</span> {classDetail?.classCode || 'D22PM2'}</span>
                        </span>
                        <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>
                        
                        <span className="flex items-center gap-1.5">
                          <CalendarDays size={15} className="text-indigo-500"/>
                          <span className="font-bold text-gray-800">{classDetail?.semester?.includes('HK') ? classDetail.semester : `HK${classDetail?.semester || '1'}`} <span className="text-gray-300 mx-1">|</span> {classDetail?.academicYear || '2025-2026'}</span>
                        </span>
                        <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                        <span className="flex items-center gap-1.5">
                          <MapPin size={15} className="text-purple-500"/>
                          <span className="font-bold text-gray-800">{classDetail?.room || '2E03'}</span>
                        </span>
                        <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                        <span className="flex items-center gap-1.5">
                          <Clock size={15} className="text-orange-500"/>
                          <span className="font-bold text-gray-800">
                            {classDetail?.weeklySchedules?.length > 0 
                              ? classDetail.weeklySchedules.map(s => `${s.dayOfWeek} ${s.startTime}-${s.endTime}`).join(', ') 
                              : classDetail?.schedule || 'THỨ HAI 17:00-22:30'}
                          </span>
                        </span>
                        {classDetail?.startDate && (
                          <>
                            <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>
                            <span className="flex items-center gap-1.5">
                              <Calendar size={15} className="text-teal-500"/>
                              <span className="font-bold text-gray-800">
                                Dự kiến: {(() => {
                                  const start = new Date(classDetail.startDate);
                                  const total = classDetail.totalSessions || 15;
                                  const end = new Date(start.getTime() + (total - 1) * 7 * 24 * 60 * 60 * 1000);
                                  const f = (d) => d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
                                  return `${f(start)} - ${f(end)}`;
                                })()}
                              </span>
                            </span>
                          </>
                        )}
                        <span className="hidden sm:block w-px h-3.5 bg-gray-300"></span>

                         <span className="flex items-center gap-1.5">
                           <CalendarDays size={15} className="text-emerald-500"/>
                           <span className="font-bold text-gray-800">
                             Tiếp theo: {nextSessionDate || '---'}
                           </span>
                         </span>
                      </div>
                    </div>
                  </div>
                </>
              )}
            </div>
          )}

          {/* TABS NAVIGATION */}
          <div className="flex gap-8 border-b border-gray-200 mb-6 flex-wrap">
            <button onClick={() => setActiveTab('students')} className={`pb-4 text-sm font-semibold flex items-center gap-2 relative transition-colors ${activeTab === 'students' ? 'text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <Users size={18} /> Sinh viên <span className={`px-1.5 py-0.5 rounded text-[11px] font-bold ${activeTab === 'students' ? 'bg-red-50 text-red-600' : 'bg-gray-100 text-gray-600'}`}>{totalStudents}</span>
            </button>
            <button onClick={() => setActiveTab('dynamic-qr')} className={`pb-4 text-sm font-semibold flex items-center gap-2 relative transition-colors ${activeTab === 'dynamic-qr' ? 'text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <QrCode size={18} /> QR Động <span className={`px-1.5 py-0.5 rounded text-[11px] font-bold flex items-center gap-1 ${openSession?.id ? 'bg-red-50 text-red-600 border border-red-100' : 'bg-gray-100 text-gray-500 border border-gray-200'}`}>
                {openSession?.id ? (
                  <><span className="w-1.5 h-1.5 bg-red-600 rounded-full animate-pulse"></span> Đang mở</>
                ) : 'Chưa mở'}
              </span>
            </button>

            <button onClick={() => setActiveTab('sessions')} className={`pb-4 text-sm font-semibold flex items-center gap-2 relative transition-colors ${activeTab === 'sessions' ? 'text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <CalendarDays size={18} /> Lịch sử phiên học <span className={`px-1.5 py-0.5 rounded text-[11px] font-bold ${activeTab === 'sessions' ? 'bg-red-50 text-red-600' : 'bg-gray-100 text-gray-600'}`}>{sessionsCount}</span>
            </button>

            <button onClick={() => setActiveTab('fraud')} className={`pb-4 text-sm font-semibold flex items-center gap-2 relative transition-colors ${activeTab === 'fraud' ? 'text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <AlertTriangle size={18} /> Gian lận {fraudCount > 0 && <span className={`px-1.5 py-0.5 rounded text-[11px] font-bold ${activeTab === 'fraud' ? 'bg-red-50 text-red-600' : 'bg-gray-100 text-gray-600'}`}>{fraudCount}</span>}
            </button>
            <button onClick={() => setActiveTab('absence')} className={`pb-4 text-sm font-semibold flex items-center gap-2 relative transition-colors ${activeTab === 'absence' ? 'text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <CalendarX size={18} /> Vắng mặt {absenceCount > 0 && <span className={`px-1.5 py-0.5 rounded text-[11px] font-bold ${activeTab === 'absence' ? 'bg-red-50 text-red-600' : 'bg-gray-100 text-gray-600'}`}>{absenceCount}</span>}
            </button>
          </div>

          {/* RENDER CONTENT THEO TAB */}
          <div className="flex-1">
            {activeTab === 'students' && <StudentsTab classId={classId} />}
            {activeTab === 'dynamic-qr' && (
              <DynamicQRTab 
                classDetail={classDetail} 
                onCheckInsUpdate={(count) => setLatestAttendance(count)}
              />
            )}
            {activeTab === 'fraud' && <FraudTab classId={classId} />}
            {activeTab === 'absence' && <AbsenceTab classId={classId} />}
          
            {activeTab === 'sessions' && <SessionHistoryTab classId={classId} />}
          </div>

        </div>

        {/* NHÚNG MODAL VÀO ĐÂY */}
        <JoinClassModal 
          isOpen={showJoinModal} 
          onClose={() => setShowJoinModal(false)} 
          classDetail={classDetail} 
        />
      </main>
    </div>
  );
}