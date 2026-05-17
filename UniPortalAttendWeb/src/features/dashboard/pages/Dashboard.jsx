import React, { useState, useEffect } from 'react';
import { 
  Search, Bell, Plus, UserCheck, Monitor, Inbox, AlertTriangle, 
  Calendar as CalendarIcon, MapPin, ArrowRight, Smartphone, FileText, Clock
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ComposedChart, Line, Bar, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer, Legend 
} from 'recharts';
import { classApi } from '../../../api/classApi';

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

export default function Dashboard() {
  const navigate = useNavigate();
  const [chartPeriod, setChartPeriod] = useState('This Week');

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });

  const [todayClasses, setTodayClasses] = useState([]);
  const [tomorrowClasses, setTomorrowClasses] = useState([]);
  const [isLoadingSchedule, setIsLoadingSchedule] = useState(true);

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

    fetchSchedule();
    
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
                  <h3 className="text-3xl font-bold text-gray-900">7</h3>
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
                  <h3 className="text-3xl font-bold text-red-600">3</h3>
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
                  <div className="flex gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                    <div className="w-10 h-10 rounded-full bg-red-100 text-red-600 flex items-center justify-center shrink-0 mt-1">
                      <Smartphone size={20} />
                    </div>
                    <div className="flex-1">
                      <div className="flex justify-between items-start mb-1">
                        <h4 className="font-bold text-gray-900">Phát hiện sai lệch thiết bị</h4>
                        <span className="text-xs font-medium text-gray-400">10 phút trước</span>
                      </div>
                      <p className="text-sm text-gray-600 mb-3">
                        Sinh viên <span className="font-bold text-gray-900">Nguyễn Văn Mạnh</span> đã cố gắng quét từ thiết bị chưa đăng ký trong lớp CS101.
                      </p>
                      <div className="flex gap-2">
                        <button className="px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-xs font-bold rounded-lg transition-colors">Xem xét sự cố</button>
                        <button className="px-4 py-2 bg-white border border-gray-200 text-gray-600 hover:bg-gray-50 text-xs font-bold rounded-lg transition-colors">Bỏ qua</button>
                      </div>
                    </div>
                  </div>

                  <div className="flex gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/50">
                    <div className="w-10 h-10 rounded-full bg-amber-100 text-amber-600 flex items-center justify-center shrink-0 mt-1">
                      <FileText size={20} />
                    </div>
                    <div className="flex-1">
                      <div className="flex justify-between items-start mb-1">
                        <h4 className="font-bold text-gray-900">Yêu cầu xin nghỉ vì lý do y tế</h4>
                        <span className="text-xs font-medium text-gray-400">2 giờ trước</span>
                      </div>
                      <p className="text-sm text-gray-600 mb-3">
                        <span className="font-bold text-gray-900">Lê Thị Hồng</span> đã gửi yêu cầu cho môn Hệ quản trị cơ sở dữ liệu nâng cao.
                      </p>
                      <div>
                        <button className="px-4 py-2 bg-[#111827] hover:bg-gray-800 text-white text-xs font-bold rounded-lg transition-colors">Xem tài liệu</button>
                      </div>
                    </div>
                  </div>
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


