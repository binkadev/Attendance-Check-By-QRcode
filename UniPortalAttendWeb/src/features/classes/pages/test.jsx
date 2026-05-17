import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { 
  Search, Bell, LayoutDashboard, Users, PlusCircle, Clock, 
  ShieldAlert, CalendarX, LogOut, MapPin, Calendar, 
  Download, Mail, AlertTriangle, Smartphone, Crosshair, ChevronLeft, ChevronRight, Loader2
} from 'lucide-react';
import { classApi } from '../api/classApi';
import toast from 'react-hot-toast';

// --- MOCK DATA DỰ PHÒNG ---
const MOCK_CLASS_INFO = {
  courseCode: 'CS204',
  classCode: 'Group 02',
  name: 'Data Structures & Algorithms',
  room: 'Lecture Hall 1',
  schedule: 'Tue, Thu 10:00 AM',
  nextSession: 'Today, 10:00 AM'
};

const MOCK_STUDENTS = [
  { id: 'STU-8921', name: 'Sarah Jenkins', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d', attendanceRate: 92, lastSeen: 'Oct 12, 2023', status: 'Good' },
  { id: 'STU-7743', name: 'Marcus Chen', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704e', attendanceRate: 68, lastSeen: 'Oct 05, 2023', status: 'At-Risk' },
  { id: 'STU-6520', name: 'David Rodriguez', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704f', attendanceRate: 88, lastSeen: 'Oct 12, 2023', status: 'Flagged' },
  // Thêm dữ liệu tùy ý...
];

const STUDENTS_DATA = [
  { id: 'STU-8921', name: 'Sarah Jenkins', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d', attendanceRate: 92, lastSeen: 'Oct 12, 2023', status: 'Good' },
  { id: 'STU-7743', name: 'Marcus Chen', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704e', attendanceRate: 68, lastSeen: 'Oct 05, 2023', status: 'At-Risk' },
  { id: 'STU-6520', name: 'David Rodriguez', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704f', attendanceRate: 88, lastSeen: 'Oct 12, 2023', status: 'Flagged' },
  // Thêm dữ liệu tùy ý...
];

const ALERTS_DATA = [
  {
    id: 1, type: 'duplicate_device', title: 'Duplicate Device ID',
    desc: "Device 'iPhone 13' (ID: A8F2) used by STU-6520 and STU-1198.",
    icon: <Smartphone size={18} className="text-red-500" />,
    bgColor: 'bg-red-50', borderColor: 'border-red-200'
  },
  {
    id: 2, type: 'suspicious_location', title: 'Suspicious Location',
    desc: "STU-4421 checked in 2.4 miles away from Campus Geofence.",
    icon: <Crosshair size={18} className="text-amber-500" />,
    bgColor: 'bg-amber-50', borderColor: 'border-amber-200'
  }
];

export default function ClassDetail() {

  // Hàm helper format ngày tháng (Oct 12, 2023)
  const formatDate = (dateString) => {
    if (!dateString) return null;
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: '2-digit', year: 'numeric' });
  };


  const { classId } = useParams();
  const [activeTab, setActiveTab] = useState('students');

  // States cho API Dữ liệu
  const [classDetail, setClassDetail] = useState(null);
  const [students, setStudents] = useState([]);
  const [isLoadingDetail, setIsLoadingDetail] = useState(true);
  const [isLoadingStudents, setIsLoadingStudents] = useState(true);

  // Pagination cho danh sách sinh viên
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // GỌI API LẤY CHI TIẾT LỚP
  useEffect(() => {
    const loadClassDetail = async () => {
      try {
        const res = await classApi.getClassDetail(classId);
        setClassDetail(res);
      } catch (error) {
        console.error("Lỗi lấy chi tiết lớp");
      } finally {
        setIsLoadingDetail(false);
      }
    };
    if (classId) loadClassDetail();
  }, [classId]);

  // GỌI API LẤY DANH SÁCH SINH VIÊN
  useEffect(() => {
    const loadStudents = async () => {
      setIsLoadingStudents(true);
      try {
        const res = await classApi.getClassMembers(classId, { page, size });

        // Chuẩn hóa dữ liệu API sinh viên về format chung để dễ hiển thị
        const apiStudents = (res?.items || []).map(member => ({
          id: member.email ? member.email.split('@')[0].toUpperCase() : member.userId.substring(0, 8),
          name: member.fullName || 'Chưa cập nhật',
          avatar: member.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(member.fullName || 'U')}&background=random`,
          
          joinedAt: member.joinedAt,

          attendanceRate: undefined, // API chưa trả về
          lastSeen: undefined,       // API chưa trả về
          
          status: member.memberStatus || 'Chưa cập nhật'
        }));

        // Trộn Mock Data vào API Data
        setStudents([...apiStudents, ...MOCK_STUDENTS]);
        setTotalPages(res?.totalPages || 1);
        setTotalElements((res?.totalElements || 0) + MOCK_STUDENTS.length);
      } catch (error) {
        console.error("Lỗi lấy danh sách sinh viên");
        setStudents(MOCK_STUDENTS); // Fallback mock
        setTotalElements(MOCK_STUDENTS.length);
      } finally {
        setIsLoadingStudents(false);
      }
    };
    if (classId) loadStudents();
  }, [classId, page, size]);

  // HÀM HELPER HIỂN THỊ
  const displayDetail = {
    courseCode: classDetail?.courseCode || MOCK_CLASS_INFO.courseCode,
    classCode: classDetail?.classCode || MOCK_CLASS_INFO.classCode,
    name: classDetail?.name || MOCK_CLASS_INFO.name,
    room: classDetail?.room || MOCK_CLASS_INFO.room,
    // Format lịch học từ API (ví dụ: "MONDAY 07:00-09:30")
    schedule: classDetail?.weeklySchedules?.length > 0
      ? classDetail.weeklySchedules.map(s => `${s.dayOfWeek} ${s.startTime}-${s.endTime}`).join(' | ')
      : MOCK_CLASS_INFO.schedule,
    nextSession: MOCK_CLASS_INFO.nextSession // API hiện tại chưa có trường này
  };

  const renderProgressBar = (rate) => {
    if (rate === undefined) return <span className="text-xs text-gray-400 font-medium italic">Chưa cập nhật</span>;
    let colorClass = 'bg-emerald-500';
    if (rate < 75) colorClass = 'bg-red-500';
    else if (rate < 90) colorClass = 'bg-amber-500';

    return (
      <div className="flex items-center gap-3">
        <span className={`text-[13px] font-bold ${colorClass.replace('bg-', 'text-')}`}>{rate}%</span>
        <div className="w-24 h-2 bg-gray-100 rounded-full overflow-hidden">
          <div className={`h-full ${colorClass} rounded-full`} style={{ width: `${rate}%` }}></div>
        </div>
      </div>
    );
  };

  const renderStatusBadge = (status) => {
    if (status === 'Chưa cập nhật') return <span className="text-xs text-gray-400 font-medium italic">Chưa cập nhật</span>;
    const styles = {
      'Good': 'bg-emerald-50 text-emerald-600',
      'At-Risk': 'bg-red-50 text-red-600',
      'Flagged': 'bg-amber-50 text-amber-600',
      'ACTIVE': 'bg-blue-50 text-blue-600', // Status từ API
    };
    return (
      <span className={`px-2.5 py-1 rounded-md text-[12px] font-semibold ${styles[status] || 'bg-gray-50 text-gray-600'}`}>
        {status}
      </span>
    );
  };

  return (
    <div className="flex min-h-screen bg-[#F8FAFC] font-sans">

      {/* SIDEBAR */}
      <aside className="w-64 bg-[#111827] min-h-screen flex flex-col text-gray-300">
        <div className="p-6 flex items-center gap-3 border-b border-gray-800">
          <div className="w-8 h-8 bg-red-700 rounded flex items-center justify-center text-white font-bold">
            <ShieldAlert size={20} />
          </div>
          <div>
            <h1 className="text-white font-bold text-lg leading-tight">EduGuard</h1>
            <p className="text-xs text-gray-400">Lecturer Portal</p>
          </div>
        </div>

        <div className="flex-1 py-6">
          <div className="px-6 mb-4 text-xs font-semibold text-gray-500 tracking-wider">ACTIVE CLASS CONTEXT</div>
          <nav className="space-y-1">
            <button onClick={() => setActiveTab('students')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'students' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <Users size={18} className={activeTab === 'students' ? 'text-red-500' : ''} /> Students
            </button>
            <button onClick={() => setActiveTab('dynamic-qr')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'dynamic-qr' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <div className="w-4 h-4 border-2 border-current rounded-sm"></div> Dynamic QR Session
            </button>
            <button onClick={() => setActiveTab('fraud')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'fraud' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <AlertTriangle size={18} className={activeTab === 'fraud' ? 'text-red-500' : ''} /> Fraud Incidents
            </button>
            <button onClick={() => setActiveTab('absence')} className={`w-full flex items-center gap-3 px-6 py-2.5 transition-colors ${activeTab === 'absence' ? 'bg-gray-800 text-white border-l-4 border-red-600' : 'hover:bg-gray-800'}`}>
              <CalendarX size={18} className={activeTab === 'absence' ? 'text-red-500' : ''} /> Absence Requests
            </button>
          </nav>
        </div>

      </aside>

      <main className="flex-1 flex flex-col h-screen overflow-y-auto">
        {/* HEADER */}
        <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-10">
          <div>
            <div className="flex items-center gap-2 text-sm text-gray-500 mb-1">
              <Link to="/classes" className="hover:text-red-600 transition-colors">Class Management</Link>
              <ChevronRight size={14} />
              {isLoadingDetail ? (
                <div className="h-4 w-24 bg-gray-200 rounded animate-pulse"></div>
              ) : (
                <span className="font-medium text-gray-900">{displayDetail.courseCode}: {displayDetail.name}</span>
              )}
            </div>
            <h1 className="text-2xl font-bold text-[#111827]">Class Details</h1>
          </div>
          <button className="p-2.5 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm">
            <Bell size={18} />
          </button>
        </header>

        <div className="p-8 max-w-7xl w-full mx-auto flex-1 flex flex-col">

          {/* CLASS OVERVIEW CARD */}
          {isLoadingDetail ? (
            <div className="bg-white rounded-xl border border-gray-200 p-6 flex items-center justify-center mb-8 h-32">
              <Loader2 className="animate-spin text-gray-400" size={24} />
            </div>
          ) : (
            <div className="bg-white rounded-xl border border-gray-200 p-6 flex justify-between items-center mb-8 shadow-sm">
              <div className="flex items-center gap-5">
                <div className="w-14 h-14 bg-[#111827] rounded-xl flex items-center justify-center text-white font-bold text-xl">
                  {displayDetail.courseCode.substring(0, 2)}
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-900 mb-2">{displayDetail.name}</h2>
                  <div className="flex items-center gap-4 text-sm text-gray-500 font-medium">
                    {/* KHU VỰC HIỂN THỊ MÃ MÔN VÀ NHÓM LỚP */}
                    <span className="flex items-center gap-1.5 border border-gray-200 px-2 py-0.5 rounded bg-gray-50">
                      <LayoutDashboard size={14} className="text-gray-400" />
                      <span className="font-bold text-red-600">{displayDetail.courseCode}</span>
                      <span className="text-gray-300">|</span>
                      <span className="font-semibold text-gray-600">{displayDetail.classCode}</span>
                    </span>

                    {/* MỤC HỌC KỲ */}
                    <span className="flex items-center gap-1.5">
                      <Calendar size={16} className="text-gray-400" />
                      {classDetail?.semester || classDetail?.academicYear ? (
                        <span>
                          {classDetail?.semester || '---'} - {classDetail?.academicYear || '---'}
                        </span>
                      ) : (
                        <span className="italic text-gray-400">Học kỳ: Chưa cập nhật</span>
                      )}
                    </span>
                    
                    <span className="flex items-center gap-1.5"><MapPin size={16} /> {displayDetail.room || 'Chưa cập nhật'}</span>
                    <span className="flex items-center gap-1.5"><Calendar size={16} /> {displayDetail.schedule || 'Chưa cập nhật'}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-4">
                <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-2 text-right">
                  <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-0.5">NEXT SESSION</p>
                  <p className="text-sm font-bold text-gray-900">{displayDetail.nextSession}</p>
                </div>
                <button className="bg-[#C81E1E] hover:bg-red-800 text-white px-5 py-3 rounded-lg text-sm font-semibold transition-colors shadow-sm flex items-center gap-2">
                  <div className="w-4 h-4 border-2 border-current rounded-sm"></div> Start QR Session
                </button>
              </div>
            </div>
          )}

          {/* TABS NAVIGATION */}
          <div className="flex gap-8 border-b border-gray-200 mb-6">
            <button onClick={() => setActiveTab('students')} className={`pb-4 text-sm font-semibold flex items-center gap-2 transition-colors relative ${activeTab === 'students' ? 'text-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <Users size={18} /> Students ({totalElements})
              {activeTab === 'students' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600 rounded-t-full"></div>}
            </button>
            <button onClick={() => setActiveTab('dynamic-qr')} className={`pb-4 text-sm font-semibold flex items-center gap-2 transition-colors relative ${activeTab === 'dynamic-qr' ? 'text-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <div className="w-4 h-4 border-2 border-current rounded-sm"></div> Dynamic QR
              {activeTab === 'dynamic-qr' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600 rounded-t-full"></div>}
            </button>
            <button onClick={() => setActiveTab('fraud')} className={`pb-4 text-sm font-semibold flex items-center gap-2 transition-colors relative ${activeTab === 'fraud' ? 'text-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <AlertTriangle size={18} /> Fraud <span className="bg-red-100 text-red-600 px-1.5 py-0.5 rounded text-[11px]">3</span>
              {activeTab === 'fraud' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600 rounded-t-full"></div>}
            </button>
            <button onClick={() => setActiveTab('absence')} className={`pb-4 text-sm font-semibold flex items-center gap-2 transition-colors relative ${activeTab === 'absence' ? 'text-red-600' : 'text-gray-500 hover:text-gray-700'}`}>
              <CalendarX size={18} /> Absence <span className="bg-gray-100 text-gray-600 px-1.5 py-0.5 rounded text-[11px]">1</span>
              {activeTab === 'absence' && <div className="absolute bottom-0 left-0 right-0 h-0.5 bg-red-600 rounded-t-full"></div>}
            </button>
          </div>

          {/* TAB CONTENT: STUDENTS */}
          {activeTab === 'students' && (
            <div className="flex gap-6 items-start">

              {/* MAIN CONTENT (Table) */}
              <div className="flex-1 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
                <div className="p-4 border-b border-gray-200 flex justify-between items-center">
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                    <input type="text" placeholder="Search student name or ID..." className="pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm w-64 focus:ring-2 focus:ring-red-100 focus:border-red-300 outline-none transition-all" />
                  </div>
                  <div className="flex gap-3">
                    <select className="border border-gray-200 rounded-lg text-sm px-3 py-2 outline-none focus:border-red-300 text-gray-700 font-medium cursor-pointer bg-white">
                      <option>All Status</option><option>Good</option><option>Flagged</option>
                    </select>
                    <button className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors">
                      <Mail size={16} /> Message
                    </button>
                    <button className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors">
                      <Download size={16} /> Export
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto min-h-[300px]">
                  {isLoadingStudents ? (
                    <div className="flex justify-center items-center h-48 text-gray-400">
                      <Loader2 className="animate-spin" size={24} />
                    </div>
                  ) : students.length === 0 ? (
                    <div className="flex justify-center items-center h-48 text-gray-500 font-medium">Chưa có sinh viên nào trong lớp.</div>
                  ) : (
                    <table className="w-full text-left text-sm">
                      <thead className="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200">
                        <tr>
                          <th className="p-4 w-12"><input type="checkbox" className="rounded border-gray-300 text-red-600 focus:ring-red-500" /></th>
                          <th className="p-4">Student</th>
                          <th className="p-4">Student ID</th>
                          <th className="p-4">Attendance Rate</th>
                          <th className="p-4">Joined At</th>
                          <th className="p-4">Last Seen</th>
                          <th className="p-4">Status</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                        {students.map((student, idx) => (
                          <tr key={idx} className="hover:bg-gray-50 transition-colors">
                            <td className="p-4"><input type="checkbox" className="rounded border-gray-300 text-red-600 focus:ring-red-500" /></td>
                            <td className="p-4 flex items-center gap-3">
                              <img src={student.avatar} alt={student.name} className="w-8 h-8 rounded-full bg-gray-200" />
                              <span className="font-bold text-gray-900">{student.name}</span>
                            </td>
                            <td className="p-4 text-gray-500 font-medium">{student.id}</td>
                            <td className="p-4">{renderProgressBar(student.attendanceRate)}</td>
                            {/* HIỂN THỊ THỜI GIAN THAM GIA */}
                            <td className="p-4 text-gray-500">
                              {formatDate(student.joinedAt) || <span className="text-gray-400 italic text-xs">Chưa cập nhật</span>}
                            </td>
                            
                            <td className="p-4 text-gray-500">
                              {student.lastSeen || <span className="text-xs italic text-gray-400">Chưa cập nhật</span>}
                            </td>
                            <td className="p-4">{renderStatusBadge(student.status)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>

                {!isLoadingStudents && students.length > 0 && (
                  <div className="p-4 border-t border-gray-200 flex justify-between items-center text-sm text-gray-500">
                    <span>Showing {Math.min(page * size + 1, totalElements)} to {Math.min((page + 1) * size, totalElements)} of {totalElements} entries</span>
                    <div className="flex gap-1">
                      <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0} className="p-1.5 rounded-md border border-gray-200 hover:bg-gray-50 disabled:opacity-50"><ChevronLeft size={16} /></button>

                      {[...Array(Math.max(1, totalPages))].map((_, i) => (
                        <button key={i} onClick={() => setPage(i)} className={`w-8 h-8 flex items-center justify-center rounded-md font-medium border ${page === i ? 'bg-red-50 text-red-600 font-bold border-red-100' : 'border-gray-200 hover:bg-gray-50'}`}>
                          {i + 1}
                        </button>
                      ))}

                      <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1} className="p-1.5 rounded-md border border-gray-200 hover:bg-gray-50 disabled:opacity-50"><ChevronRight size={16} /></button>
                    </div>
                  </div>
                )}
              </div>

              {/* SECURITY ALERTS SIDEBAR */}
              <div className="w-[320px] shrink-0 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
                <div className="p-5 border-b border-gray-200 flex justify-between items-center">
                  <h3 className="font-bold text-gray-900 flex items-center gap-2">
                    <ShieldAlert size={18} className="text-red-600" /> Security Alerts
                  </h3>
                  <span className="text-[11px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded">3 New</span>
                </div>

                <div className="p-4 space-y-4 flex-1">
                  {ALERTS_DATA.map((alert) => (
                    <div key={alert.id} className={`p-4 rounded-xl border ${alert.borderColor} ${alert.bgColor}`}>
                      <div className="flex gap-3">
                        <div className="mt-0.5 shrink-0">{alert.icon}</div>
                        <div>
                          <h4 className="text-[14px] font-bold text-gray-900 leading-tight mb-1">{alert.title}</h4>
                          <p className="text-[13px] text-gray-600 mb-3 leading-snug">{alert.desc}</p>
                          <div className="flex gap-3 text-[13px] font-semibold">
                            <button className="text-red-600 hover:text-red-800 transition-colors">Review</button>
                            <button className="text-gray-500 hover:text-gray-700 transition-colors">Dismiss</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                <div className="p-4 border-t border-gray-200 text-center">
                  <button className="text-sm font-bold text-red-600 hover:text-red-800 transition-colors flex items-center justify-center gap-1 w-full">
                    View All Security Logs <ChevronRight size={16} />
                  </button>
                </div>
              </div>

            </div>
          )}


          

        </div>
      </main>
    </div>
  );
}