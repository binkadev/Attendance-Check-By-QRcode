import React, { useState, useEffect } from 'react';
import { 
  Search, Bell, Plus, UserCheck, Monitor, Inbox, AlertTriangle, 
  Calendar as CalendarIcon, MapPin, ArrowRight, Smartphone, FileText, Clock, Timer
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ComposedChart, Line, Bar, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer, Legend 
} from 'recharts';
import { classApi } from '../../../api/classApi';
import { parseDeviceName } from '../../../utils/formatters';

// --- MOCK DATA ---
const chartData = [
  { name: 'Mon', present: 88, absences: 2 },
  { name: 'Tue', present: 85, absences: 4 },
  { name: 'Wed', present: 90, absences: 1 },
  { name: 'Thu', present: 83, absences: 5 },
  { name: 'Fri', present: 87, absences: 3 },
];

// Helper formats
const formatTime = (isoString) => {
  if (!isoString) return "--:--";
  return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

// Hàm kiểm tra trạng thái lớp học
const getSessionStatus = (startAt, endAt) => {
  if (!startAt || !endAt) return 'future';
  const now = new Date();
  const start = new Date(startAt);
  const end = new Date(endAt);

  if (now > end) return 'past';
  if (now >= start && now <= end) return 'now';
  return 'future';
};

// Hàm tính thời gian trôi qua
const timeAgo = (dateString) => {
  if (!dateString) return "";
  const now = new Date();
  const past = new Date(dateString);
  const diffMs = now - past;
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return `Vừa xong`;
  if (diffMins < 60) return `${diffMins} phút trước`;
  if (diffHours < 24) return `${diffHours} giờ trước`;
  return `${diffDays} ngày trước`;
};

// Giả lập Avatar sinh viên ngẫu nhiên
const generateMockParticipants = (index, modifier = 0) => {
  const totalStudents = ((index + 1 + modifier) * 27) % 45 + 20; 
  const displayAvatarsCount = Math.min(3, totalStudents);
  const extraCount = totalStudents - displayAvatarsCount;

  const avatars = [];
  for (let i = 0; i < displayAvatarsCount; i++) {
    avatars.push(`https://i.pravatar.cc/150?u=${index * 10 + i + modifier}`);
  }

  return { avatars, extraCount };
};

// Cấu hình hiển thị theo loại sự cố
const FRAUD_TYPE_CONFIG = {
  REPEATED_FAILED_QR_TOKEN: { label: 'Quét QR thất bại liên tục', icon: <Timer size={20} className="text-red-500" /> },
  WRONG_SESSION_QR_TOKEN: { label: 'QR sai phiên học', icon: <Smartphone size={20} className="text-amber-500" /> },
  EXPIRED_QR_TOKEN: { label: 'QR hết hạn', icon: <Timer size={20} className="text-red-500" /> },
  REPEATED_OUT_OF_RANGE: { label: 'Quét ngoài phạm vi', icon: <MapPin size={20} className="text-amber-500" /> },
  IP_BURST_MULTI_ATTEMPT: { label: 'Nhiều yêu cầu từ 1 IP', icon: <Smartphone size={20} className="text-red-500" /> },
  SHARED_DEVICE_MULTI_ACCOUNT: { label: 'Trùng thiết bị điểm danh', icon: <Smartphone size={20} className="text-red-500" /> },
};

// Dữ liệu mẫu (Mock data) cho Cảnh báo để tránh bị trống
const MOCK_ALERTS = [
  {
    id: 'mock-f1',
    type: 'fraud',
    fraudType: 'SHARED_DEVICE_MULTI_ACCOUNT',
    title: 'Trùng thiết bị điểm danh',
    description: 'Sinh viên cố tình điểm danh trên cùng một thiết bị (trùng địa chỉ MAC). Những sinh viên liên quan: Lê Văn Bình.',
    createdAt: new Date(Date.now() - 10 * 60000).toISOString(),
    studentName: 'Nguyễn Văn Mạnh',
    classInfo: { courseCode: 'CS101', name: 'Nhập môn lập trình', id: 'mock-1' },
    evidenceSummary: {
      userAgent: "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
      deviceId: "A8F2-99B1-C34X"
    }
  },
  {
    id: 'mock-f2',
    type: 'fraud',
    fraudType: 'REPEATED_OUT_OF_RANGE',
    title: 'Quét QR ngoài phạm vi',
    description: 'Phát hiện tọa độ GPS cách xa vị trí phòng học 2.5km.',
    createdAt: new Date(Date.now() - 45 * 60000).toISOString(),
    studentName: 'Trần Đăng Khoa',
    classInfo: { courseCode: 'CS204', name: 'Cấu trúc dữ liệu', id: 'mock-2' }
  },
  {
    id: 'mock-f3',
    type: 'fraud',
    fraudType: 'IP_BURST_MULTI_ATTEMPT',
    title: 'Quét QR quá nhanh',
    description: 'Phát hiện 3 lần quét trong 4 giây từ cùng một địa chỉ IP.',
    createdAt: new Date(Date.now() - 90 * 60000).toISOString(),
    studentName: 'Phạm Thị Mai',
    classInfo: { courseCode: 'CS101', name: 'Nhập môn lập trình', id: 'mock-1' }
  },
  {
    id: 'mock-a1',
    type: 'absence',
    createdAt: new Date(Date.now() - 120 * 60000).toISOString(),
    studentName: 'Lê Thị Hồng',
    reason: 'Lý do y tế (Có kèm giấy khám bệnh)',
    classInfo: { name: 'Hệ quản trị cơ sở dữ liệu nâng cao', id: 'mock-1' }
  },
  {
    id: 'mock-a2',
    type: 'absence',
    createdAt: new Date(Date.now() - 360 * 60000).toISOString(),
    studentName: 'Hoàng Văn Thái',
    reason: 'Xin nghỉ việc gia đình',
    classInfo: { name: 'Toán rời rạc', id: 'mock-3' }
  }
];

export default function Dashboard() {
  const navigate = useNavigate();
  const [chartPeriod, setChartPeriod] = useState('This Week');

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });

  const [todayClasses, setTodayClasses] = useState([]);
  const [tomorrowClasses, setTomorrowClasses] = useState([]);
  const [isLoadingSchedule, setIsLoadingSchedule] = useState(true);

  // Thêm state cho Cảnh báo & Thống kê
  const [alerts, setAlerts] = useState([]);
  const [isLoadingAlerts, setIsLoadingAlerts] = useState(false);
  const [dashboardStats, setDashboardStats] = useState({ fraudCount: 0, absenceCount: 0 });

  // Thêm state trigger để ép component re-render mỗi phút cập nhật trạng thái "Now"
  const [timeTrigger, setTimeTrigger] = useState(0);

  useEffect(() => {
    const fetchSchedule = async () => {
      try {
        setIsLoadingSchedule(true);
        const data = await classApi.getUpcomingSessions(20);
        
        const today = new Date();
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);

        const getLocalYMD = (d) => {
          return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
        };

        const localTodayStr = getLocalYMD(today);
        const localTomorrowStr = getLocalYMD(tomorrow);

        let todayList = [];
        let tomorrowList = [];

        if (data && data.sections) {
          data.sections.forEach(section => {
            if (section.date === localTodayStr) {
              todayList = [...todayList, ...section.items];
            } else if (section.date === localTomorrowStr) {
              tomorrowList = [...tomorrowList, ...section.items];
            }
          });
        }

        setTodayClasses(todayList);
        setTomorrowClasses(tomorrowList);
      } catch (error) {
        console.error("Failed to fetch schedule:", error);
      } finally {
        setIsLoadingSchedule(false);
      }
    };

    const fetchAlertsAndStats = async () => {
      try {
        setIsLoadingAlerts(true);
        const classesData = await classApi.getTeachingClasses();
        const classes = classesData.items || [];
        
        let allAlerts = [];
        let fraudCount = 0;
        let absenceCount = 0;

        await Promise.all(classes.map(async (cls) => {
          try {
            const classId = cls.groupId || cls.id;
            const [fraudRes, absenceRes] = await Promise.all([
              classApi.getFraudIncidents(classId).catch(() => null),
              classApi.getAbsenceRequests(classId).catch(() => null)
            ]);

            const frauds = fraudRes?.items || fraudRes || [];
            const absences = absenceRes?.items || absenceRes || [];

            const openFrauds = frauds.filter(f => f.status === 'OPEN' || f.status === 'PENDING');
            const pendingAbsences = absences.filter(a => a.status === 'PENDING');

            fraudCount += openFrauds.length;
            absenceCount += pendingAbsences.length;

            allAlerts.push(...frauds.map(f => ({ ...f, type: 'fraud', classInfo: { ...cls, id: classId } })));
            allAlerts.push(...absences.map(a => ({ ...a, type: 'absence', classInfo: { ...cls, id: classId } })));
          } catch (e) {
            console.error(`Failed to fetch alerts for class`, e);
          }
        }));

        allAlerts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        // Fallback to mock data if no alerts from API
        if (allAlerts.length === 0) {
          allAlerts = [...MOCK_ALERTS];
          if (fraudCount === 0 && absenceCount === 0) {
            fraudCount = 3; // mock stats
            absenceCount = 7; // mock stats
          }
        } else {
          // Lấy chi tiết cho 5 alert đầu tiên (nếu là fraud) để có evidenceSummary hiển thị thiết bị
          const top5 = allAlerts.slice(0, 5);
          await Promise.all(top5.map(async (alert) => {
            if (alert.type === 'fraud' && !alert.evidenceSummary && !String(alert.id).startsWith('mock-')) {
              try {
                const detail = await classApi.getFraudIncidentDetail(alert.classInfo.id, alert.id);
                if (detail && detail.evidenceSummary) {
                  alert.evidenceSummary = detail.evidenceSummary;
                }
              } catch (e) {
                console.error("Failed to fetch alert detail for evidenceSummary", e);
              }
            }
          }));
        }
        
        setAlerts(allAlerts);
        setDashboardStats({ fraudCount, absenceCount });
      } catch (error) {
        console.error("Failed to fetch teaching classes for alerts:", error);
      } finally {
        setIsLoadingAlerts(false);
      }
    };

    fetchSchedule();
    fetchAlertsAndStats();
    
    // Cập nhật lại UI mỗi 60 giây để status chuyển từ Future -> Now -> Past mượt mà
    const interval = setInterval(() => setTimeTrigger(prev => prev + 1), 60000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div className="flex min-h-screen bg-[#f8fafc] font-sans">
      <Sidebar onCollapseChange={setIsSidebarCollapsed} />
      
      <main className={`flex-1 flex flex-col h-screen overflow-y-auto transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        {/* HEADER */}
        <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-10">
          <div>
            <h1 className="text-2xl font-bold text-[#111827]">Tổng quan Dashboard</h1>
            <p className="text-sm text-gray-500 mt-1">Chào mừng trở lại, đây là tóm tắt giảng dạy của bạn hôm nay.</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input
                type="text"
                placeholder="Tìm lớp học, sinh viên..."
                className="pl-10 pr-4 py-2.5 bg-[#F8FAFC] border border-gray-200 rounded-lg text-sm w-72 focus:bg-white focus:border-red-300 focus:ring-4 focus:ring-red-50 outline-none transition-all font-medium placeholder-gray-400"
              />
            </div>
            <button className="p-2.5 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm">
              <Bell size={18} />
            </button>
            <button 
              onClick={() => navigate('/classes/create')}
              className="flex items-center gap-2 bg-[#C81E1E] text-white px-5 py-2.5 rounded-lg text-sm font-semibold hover:bg-red-800 transition-colors shadow-sm"
            >
              <Plus size={18} /> Lớp học mới
            </button>
          </div>
        </header>

        <div className="p-8 max-w-[1600px] w-full mx-auto flex-1">
          
          {/* KPI CARDS */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-50 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-gray-500 mb-1">Tỷ lệ đi học hôm nay</p>
                  <h3 className="text-3xl font-bold text-gray-900">92.4%</h3>
                </div>
                <div className="w-10 h-10 bg-emerald-50 rounded-full flex items-center justify-center text-emerald-600">
                  <UserCheck size={20} />
                </div>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-emerald-600 font-bold bg-emerald-50 px-2 py-0.5 rounded flex items-center gap-1">
                  ↗ +2.1%
                </span>
                <span className="text-gray-400 font-medium">so với tuần trước</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-blue-50 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-gray-500 mb-1">Lớp học đang hoạt động</p>
                  <h3 className="text-3xl font-bold text-gray-900">4</h3>
                </div>
                <div className="w-10 h-10 bg-blue-50 rounded-full flex items-center justify-center text-blue-600">
                  <Monitor size={20} />
                </div>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-gray-700 font-bold">120</span>
                <span className="text-gray-400 font-medium">tổng số sinh viên dự kiến</span>
              </div>
            </div>

            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden">
               <div className="absolute top-0 right-0 w-24 h-24 bg-amber-50 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-gray-500 mb-1">Đơn xin nghỉ chờ duyệt</p>
                  <h3 className="text-3xl font-bold text-gray-900">{isLoadingAlerts ? '...' : dashboardStats.absenceCount}</h3>
                </div>
                <div className="w-10 h-10 bg-amber-50 rounded-full flex items-center justify-center text-amber-600">
                  <Inbox size={20} />
                </div>
              </div>
              <div className="flex items-center text-sm">
                <span className="text-amber-600 font-bold bg-amber-50 px-2 py-0.5 rounded flex items-center gap-1.5">
                  <Clock size={14} /> Cần xem xét
                </span>
              </div>
            </div>

            <div className="bg-red-50/30 rounded-2xl border border-red-100 shadow-sm p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-red-100 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-red-800 mb-1">Sự cố gian lận đang mở</p>
                  <h3 className="text-3xl font-bold text-red-600">{isLoadingAlerts ? '...' : dashboardStats.fraudCount}</h3>
                </div>
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center text-red-600">
                  <AlertTriangle size={20} />
                </div>
              </div>
              <div className="flex items-center justify-between text-sm">
                <span className="text-red-600 font-bold bg-red-100 px-2 py-0.5 rounded flex items-center gap-1">
                  ↗ +1 hôm nay
                </span>
                <span className="text-red-600 font-bold cursor-pointer hover:underline flex items-center gap-1">
                  Xem chi tiết <ArrowRight size={14} />
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            
            {/* LEFT COLUMN */}
            <div className="lg:col-span-8 flex flex-col gap-6">
              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <div className="flex justify-between items-center mb-6">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">Xu hướng điểm danh</h3>
                    <p className="text-sm text-gray-500">So sánh hàng tuần giữa các khóa học</p>
                  </div>
                  <div className="flex bg-gray-50 border border-gray-200 rounded-lg p-1">
                    <button 
                      onClick={() => setChartPeriod('This Week')}
                      className={`px-4 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'This Week' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                      Tuần này
                    </button>
                    <button 
                      onClick={() => setChartPeriod('Last Week')}
                      className={`px-4 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'Last Week' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                    >
                      Tuần trước
                    </button>
                  </div>
                </div>

                {/* Đã thêm minHeight để đảm bảo không bị lỗi -1 width/height của Recharts */}
                <div className="h-72 w-full" style={{ minHeight: '288px' }}>
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                      <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#6B7280', fontSize: 12}} dy={10} />
                      <YAxis yAxisId="left" axisLine={false} tickLine={false} tick={{fill: '#6B7280', fontSize: 12}} />
                      <YAxis yAxisId="right" orientation="right" hide={true} />
                      <Tooltip 
                        contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                        cursor={{ fill: '#F3F4F6' }}
                      />
                       <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px', fontSize: '13px', fontWeight: 500 }} />
                      <Bar yAxisId="right" dataKey="absences" name="Vắng mặt" fill="#F59E0B" barSize={40} radius={[4, 4, 0, 0]} />
                      <Line yAxisId="left" type="monotone" dataKey="present" name="% Có mặt" stroke="#10B981" strokeWidth={3} dot={{ r: 5, strokeWidth: 2, fill: '#fff' }} activeDot={{ r: 7 }} />
                    </ComposedChart>
                  </ResponsiveContainer>
                </div>
              </div>

              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-gray-900">Hoạt động gần đây & Cảnh báo</h3>
                  <button className="text-sm font-bold text-red-600 hover:underline">Xem tất cả</button>
                </div>

                <div className="space-y-4">
                  {isLoadingAlerts ? (
                    <div className="flex justify-center items-center h-20 text-red-600">
                      <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-600"></div>
                    </div>
                  ) : alerts.length > 0 ? (
                    alerts.slice(0, 5).map((alert, idx) => (
                      alert.type === 'fraud' ? (
                        <div key={`fraud-${idx}`} className="flex gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                          <div className="w-10 h-10 rounded-full bg-red-100 text-red-600 flex items-center justify-center shrink-0 mt-1">
                            {FRAUD_TYPE_CONFIG[alert.fraudType || alert.type]?.icon || <Smartphone size={20} />}
                          </div>
                          <div className="flex-1">
                            <div className="flex justify-between items-start mb-1">
                              <h4 className="font-bold text-gray-900">{alert.title || FRAUD_TYPE_CONFIG[alert.fraudType || alert.type]?.label || 'Phát hiện gian lận điểm danh'}</h4>
                              <span className="text-xs font-medium text-gray-400">{timeAgo(alert.createdAt)}</span>
                            </div>
                            <div className="text-sm text-gray-600 mb-3">
                              <span className="font-bold text-gray-900">{alert.studentName || 'Không rõ'}</span>: {alert.description || 'Có hành vi điểm danh bất thường'} <br/>
                              Tại lớp: <span className="font-bold text-gray-900">{alert.classInfo?.courseCode ? `${alert.classInfo.courseCode} - ` : ''}{alert.classInfo?.name || 'Không rõ lớp'}</span>.
                              
                              {alert.evidenceSummary?.userAgent && (
                                <p className="text-xs text-gray-500 mt-2 leading-relaxed bg-gray-50 p-2 rounded-lg border border-gray-100">
                                  Hệ thống phát hiện thiết bị {' '}
                                  <span className="font-mono bg-white border border-gray-200 px-1 py-0.5 rounded text-gray-700 font-bold">
                                    {parseDeviceName(alert.evidenceSummary.userAgent)}
                                  </span> {' '}
                                  {alert.evidenceSummary.deviceId && `(Mã ID: ${alert.evidenceSummary.deviceId.substring(0,6)}) `}
                                  đã quét điểm danh cho nhiều sinh viên.
                                </p>
                              )}
                            </div>
                            <div className="flex gap-2">
                              <button onClick={() => navigate(`/classes/${alert.classInfo?.id}`, { state: { activeTab: 'fraud' } })} className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-lg transition-colors">Xem xét sự cố</button>
                            </div>
                          </div>
                        </div>
                      ) : (
                        <div key={`abs-${idx}`} className="flex gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                          <div className="w-10 h-10 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center shrink-0 mt-1">
                            <FileText size={20} />
                          </div>
                          <div className="flex-1">
                            <div className="flex justify-between items-start mb-1">
                              <h4 className="font-bold text-gray-900">Yêu cầu xin nghỉ</h4>
                              <span className="text-xs font-medium text-gray-400">{timeAgo(alert.createdAt)}</span>
                            </div>
                            <p className="text-sm text-gray-600 mb-3">
                              <span className="font-bold text-gray-900">{alert.studentName || 'Không rõ'}</span> đã gửi yêu cầu cho môn {alert.classInfo?.name}. Lý do: {alert.reason}
                            </p>
                            <div>
                              <button onClick={() => navigate(`/classes/${alert.classInfo?.id}`, { state: { activeTab: 'absence' } })} className="px-4 py-2 bg-[#111827] hover:bg-gray-800 text-white text-xs font-bold rounded-lg transition-colors">Xem tài liệu</button>
                            </div>
                          </div>
                        </div>
                      )
                    ))
                  ) : (
                    <div className="text-sm text-gray-500 text-center py-4">Không có hoạt động nào gần đây.</div>
                  )}
                </div>
              </div>
            </div>

            {/* RIGHT COLUMN */}
            <div className="lg:col-span-4">
              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 sticky top-24">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-gray-900">Lịch trình</h3>
                  <CalendarIcon size={20} className="text-gray-400" />
                </div>

                {isLoadingSchedule ? (
                  <div className="flex justify-center items-center h-40">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
                  </div>
                ) : (
                  <div className="relative border-l-2 border-gray-100 ml-3 space-y-8 pb-4">
                    
                    {/* KHỐI HÔM NAY */}
                    {todayClasses.length > 0 && <div className="pl-6 -ml-3 mt-2 text-xs font-bold text-gray-400 uppercase tracking-wider">Hôm nay</div>}
                    
                    {todayClasses.map((cls, idx) => {
                      const status = getSessionStatus(cls.startAt, cls.endAt);
                      const mockData = generateMockParticipants(idx); 
                      
                      if (status === 'past') {
                        return (
                          <div key={cls.groupId || idx} className="relative pl-6">
                            <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-emerald-500 rounded-full ring-4 ring-white"></div>
                            <p className="text-xs font-bold text-gray-500 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
                            <div className="bg-gray-50 rounded-xl p-4 border border-gray-100 cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
                              <div className="flex justify-between items-start mb-2">
                                <h4 className="font-bold text-gray-900 text-sm leading-tight pr-2">{cls.groupName || cls.sessionName}</h4>
                                <span className="bg-emerald-100 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded">Đã xong</span>
                              </div>
                              <p className="text-xs text-gray-500 flex items-center gap-1.5 mb-3">
                                <MapPin size={12} /> {cls.room || 'Chưa xác định'}
                              </p>
                              
                              <div className="flex items-center gap-2 mt-2">
                                <div className="flex -space-x-2">
                                  {mockData.avatars.map((url, i) => (
                                    <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-200 object-cover" />
                                  ))}
                                </div>
                                {mockData.extraCount > 0 && (
                                  <span className="text-[10px] font-bold text-gray-600 bg-gray-200 px-1.5 py-0.5 rounded-full shadow-sm">
                                    +{mockData.extraCount}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        );
                      }

                      if (status === 'now') {
                        return (
                          <div key={cls.groupId || idx} className="relative pl-6">
                            <div className="absolute -left-[7px] top-1.5 w-3 h-3 bg-red-600 rounded-full ring-4 ring-red-50">
                              <div className="absolute inset-0 rounded-full bg-red-600 animate-ping opacity-75"></div>
                            </div>
                            <p className="text-xs font-bold text-red-600 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)} (Hiện tại)</p>
                            <div className="bg-red-50/50 rounded-xl p-4 border border-red-100">
                              <div className="flex justify-between items-start mb-2">
                                <h4 className="font-bold text-red-900 text-sm leading-tight pr-2">{cls.groupName || cls.sessionName}</h4>
                                <span className="bg-red-100 text-red-600 text-[10px] font-bold px-2 py-0.5 rounded flex items-center gap-1">
                                  <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span> Trực tiếp
                                </span>
                              </div>
                              <p className="text-xs text-red-700/70 flex items-center gap-1.5 mb-3">
                                <MapPin size={12} /> {cls.room || 'Chưa xác định'}
                              </p>

                              <div className="flex items-center gap-2 mb-4">
                                <div className="flex -space-x-2">
                                  {mockData.avatars.map((url, i) => (
                                    <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-red-100 object-cover" />
                                  ))}
                                </div>
                                {mockData.extraCount > 0 && (
                                  <span className="text-[10px] font-bold text-red-800 bg-red-100 px-1.5 py-0.5 rounded-full shadow-sm">
                                    +{mockData.extraCount}
                                  </span>
                                )}
                              </div>

                              <button 
                                onClick={() => navigate(`/classes/${cls.groupId}`)}
                                className="w-full bg-red-600 hover:bg-red-700 text-white text-xs font-bold py-2.5 rounded-lg transition-colors shadow-sm"
                              >
                                Quản lý phiên
                              </button>
                            </div>
                          </div>
                        );
                      }

                      // Future Today
                      return (
                        <div key={cls.groupId || idx} className="relative pl-6">
                          <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-blue-500 rounded-full ring-4 ring-white"></div>
                          <p className="text-xs font-bold text-blue-500 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
                          <div className="bg-white rounded-xl p-4 border border-blue-100 shadow-sm cursor-pointer hover:border-blue-300 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
                            <div className="flex justify-between items-start mb-2">
                              <h4 className="font-bold text-gray-900 text-sm leading-tight">{cls.groupName || cls.sessionName}</h4>
                            </div>
                            <p className="text-xs text-gray-500 flex items-center gap-1.5 mb-3">
                              <MapPin size={12} /> {cls.room || 'TBA'}
                            </p>

                            <div className="flex items-center gap-2 mt-2">
                              <div className="flex -space-x-2">
                                {mockData.avatars.map((url, i) => (
                                  <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-100 object-cover" />
                                ))}
                              </div>
                              {mockData.extraCount > 0 && (
                                <span className="text-[10px] font-bold text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded-full shadow-sm">
                                  +{mockData.extraCount}
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                      );
                    })}

                    {todayClasses.length === 0 && (
                      <div className="pl-6 text-sm text-gray-400 italic mb-6">Không còn lớp học nào hôm nay.</div>
                    )}

                    {/* KHỐI NGÀY MAI */}
                    {tomorrowClasses.length > 0 && (
                      <>
                        <div className="pl-6 -ml-3 mt-8 text-xs font-bold text-gray-400 uppercase tracking-wider">Ngày mai</div>
                        {tomorrowClasses.map((cls, idx) => {
                          const mockData = generateMockParticipants(idx, 100); 
                          
                          return (
                            <div key={`tmr-${cls.groupId || idx}`} className="relative pl-6 mt-4">
                              <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-gray-300 rounded-full ring-4 ring-white"></div>
                              <p className="text-xs font-bold text-gray-400 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
                              <div className="bg-white rounded-xl p-4 border border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
                                <div className="flex justify-between items-start mb-2">
                                  <h4 className="font-bold text-gray-700 text-sm leading-tight">{cls.groupName || cls.sessionName}</h4>
                                </div>
                                <p className="text-xs text-gray-400 flex items-center gap-1.5 mb-3">
                                  <MapPin size={12} /> {cls.room || 'Chưa xác định'}
                                </p>

                                <div className="flex items-center gap-2 mt-2 opacity-70">
                                  <div className="flex -space-x-2">
                                    {mockData.avatars.map((url, i) => (
                                      <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-100 object-cover" />
                                    ))}
                                  </div>
                                  {mockData.extraCount > 0 && (
                                    <span className="text-[10px] font-bold text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded-full shadow-sm">
                                      +{mockData.extraCount}
                                    </span>
                                  )}
                                </div>
                              </div>
                            </div>
                          );
                        })}
                      </>
                    )}

                  </div>
                )}
              </div>
            </div>

          </div>
        </div>
      </main>
    </div>
  );
}



// (cleaned up)

//                 {isLoadingSchedule ? (
//                   <div className="flex justify-center items-center h-40">
//                     <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
//                   </div>
//                 ) : (
//                   <div className="relative border-l-2 border-gray-100 ml-3 space-y-8 pb-4">
                    
//                     {/* KHỐI HÔM NAY */}
//                     {todayClasses.length > 0 && <div className="pl-6 -ml-3 mt-2 text-xs font-bold text-gray-400 uppercase tracking-wider">Today</div>}
                    
//                     {todayClasses.map((cls, idx) => {
//                       const status = getSessionStatus(cls.startAt, cls.endAt);
//                       const mockData = generateMockParticipants(idx); // Lấy mock avatar
                      
//                       if (status === 'past') {
//                         return (
//                           <div key={idx} className="relative pl-6">
//                             <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-emerald-500 rounded-full ring-4 ring-white"></div>
//                             <p className="text-xs font-bold text-gray-500 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
//                             <div className="bg-gray-50 rounded-xl p-4 border border-gray-100 cursor-pointer hover:bg-gray-100 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
//                               <div className="flex justify-between items-start mb-2">
//                                 <h4 className="font-bold text-gray-900 text-sm leading-tight pr-2">{cls.groupName || cls.sessionName}</h4>
//                                 <span className="bg-emerald-100 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded">Completed</span>
//                               </div>
//                               <p className="text-xs text-gray-500 flex items-center gap-1.5 mb-3">
//                                 <MapPin size={12} /> {cls.room || 'TBA'}
//                               </p>
                              
//                               {/* THÊM KHỐI AVATAR */}
//                               <div className="flex items-center gap-2 mt-2">
//                                 <div className="flex -space-x-2">
//                                   {mockData.avatars.map((url, i) => (
//                                     <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-200 object-cover" />
//                                   ))}
//                                 </div>
//                                 {mockData.extraCount > 0 && (
//                                   <span className="text-[10px] font-bold text-gray-600 bg-gray-200 px-1.5 py-0.5 rounded-full shadow-sm">
//                                     +{mockData.extraCount}
//                                   </span>
//                                 )}
//                               </div>
//                             </div>
//                           </div>
//                         );
//                       }

//                       if (status === 'now') {
//                         return (
//                           <div key={idx} className="relative pl-6">
//                             <div className="absolute -left-[7px] top-1.5 w-3 h-3 bg-red-600 rounded-full ring-4 ring-red-50">
//                               <div className="absolute inset-0 rounded-full bg-red-600 animate-ping opacity-75"></div>
//                             </div>
//                             <p className="text-xs font-bold text-red-600 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)} (Now)</p>
//                             <div className="bg-red-50/50 rounded-xl p-4 border border-red-100">
//                               <div className="flex justify-between items-start mb-2">
//                                 <h4 className="font-bold text-red-900 text-sm leading-tight pr-2">{cls.groupName || cls.sessionName}</h4>
//                                 <span className="bg-red-100 text-red-600 text-[10px] font-bold px-2 py-0.5 rounded flex items-center gap-1">
//                                   <span className="w-1.5 h-1.5 rounded-full bg-red-600"></span> Live
//                                 </span>
//                               </div>
//                               <p className="text-xs text-red-700/70 flex items-center gap-1.5 mb-3">
//                                 <MapPin size={12} /> {cls.room || 'TBA'}
//                               </p>

//                               {/* THÊM KHỐI AVATAR */}
//                               <div className="flex items-center gap-2 mb-4">
//                                 <div className="flex -space-x-2">
//                                   {mockData.avatars.map((url, i) => (
//                                     <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-red-100 object-cover" />
//                                   ))}
//                                 </div>
//                                 {mockData.extraCount > 0 && (
//                                   <span className="text-[10px] font-bold text-red-800 bg-red-100 px-1.5 py-0.5 rounded-full shadow-sm">
//                                     +{mockData.extraCount}
//                                   </span>
//                                 )}
//                               </div>

//                               <button 
//                                 onClick={() => navigate(`/classes/${cls.groupId}`)}
//                                 className="w-full bg-red-600 hover:bg-red-700 text-white text-xs font-bold py-2.5 rounded-lg transition-colors shadow-sm"
//                               >
//                                 Manage Session
//                               </button>
//                             </div>
//                           </div>
//                         );
//                       }

//                       // Future Today
//                       return (
//                         <div key={idx} className="relative pl-6">
//                           <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-blue-500 rounded-full ring-4 ring-white"></div>
//                           <p className="text-xs font-bold text-blue-500 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
//                           <div className="bg-white rounded-xl p-4 border border-blue-100 shadow-sm cursor-pointer hover:border-blue-300 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
//                             <div className="flex justify-between items-start mb-2">
//                               <h4 className="font-bold text-gray-900 text-sm leading-tight">{cls.groupName || cls.sessionName}</h4>
//                             </div>
//                             <p className="text-xs text-gray-500 flex items-center gap-1.5 mb-3">
//                               <MapPin size={12} /> {cls.room || 'TBA'}
//                             </p>

//                             {/* THÊM KHỐI AVATAR */}
//                             <div className="flex items-center gap-2 mt-2">
//                               <div className="flex -space-x-2">
//                                 {mockData.avatars.map((url, i) => (
//                                   <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-100 object-cover" />
//                                 ))}
//                               </div>
//                               {mockData.extraCount > 0 && (
//                                 <span className="text-[10px] font-bold text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded-full shadow-sm">
//                                   +{mockData.extraCount}
//                                 </span>
//                               )}
//                             </div>
//                           </div>
//                         </div>
//                       );
//                     })}

//                     {todayClasses.length === 0 && (
//                       <div className="pl-6 text-sm text-gray-400 italic mb-6">No more classes today.</div>
//                     )}

//                     {/* KHỐI NGÀY MAI */}
//                     {tomorrowClasses.length > 0 && (
//                       <>
//                         <div className="pl-6 -ml-3 mt-8 text-xs font-bold text-gray-400 uppercase tracking-wider">Tomorrow</div>
//                         {tomorrowClasses.map((cls, idx) => {
//                           // Lấy mock avatar, truyền thêm 100 vào modifier để ảnh không bị trùng với "Hôm nay"
//                           const mockData = generateMockParticipants(idx, 100); 
                          
//                           return (
//                             <div key={`tmr-${idx}`} className="relative pl-6 mt-4">
//                               <div className="absolute -left-[5px] top-1.5 w-2 h-2 bg-gray-300 rounded-full ring-4 ring-white"></div>
//                               <p className="text-xs font-bold text-gray-400 mb-2">{formatTime(cls.startAt)} - {formatTime(cls.endAt)}</p>
//                               <div className="bg-white rounded-xl p-4 border border-gray-100 cursor-pointer hover:bg-gray-50 transition-colors" onClick={() => navigate(`/classes/${cls.groupId}`)}>
//                                 <div className="flex justify-between items-start mb-2">
//                                   <h4 className="font-bold text-gray-700 text-sm leading-tight">{cls.groupName || cls.sessionName}</h4>
//                                 </div>
//                                 <p className="text-xs text-gray-400 flex items-center gap-1.5 mb-3">
//                                   <MapPin size={12} /> {cls.room || 'TBA'}
//                                 </p>

//                                 {/* THÊM KHỐI AVATAR */}
//                                 <div className="flex items-center gap-2 mt-2 opacity-70">
//                                   <div className="flex -space-x-2">
//                                     {mockData.avatars.map((url, i) => (
//                                       <img key={i} src={url} alt="student" className="w-6 h-6 rounded-full border-2 border-white bg-gray-100 object-cover" />
//                                     ))}
//                                   </div>
//                                   {mockData.extraCount > 0 && (
//                                     <span className="text-[10px] font-bold text-gray-500 bg-gray-100 px-1.5 py-0.5 rounded-full shadow-sm">
//                                       +{mockData.extraCount}
//                                     </span>
//                                   )}
//                                 </div>
//                               </div>
//                             </div>
//                           );
//                         })}
//                       </>
//                     )}

//                   </div>
//                 )}
//               </div>
//             </div>

//           </div>
//         </div>
//       </main>
//     </div>
//   );
// }


