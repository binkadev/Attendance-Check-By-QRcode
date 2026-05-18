import React, { useState, useEffect } from 'react';
import { 
  Loader2, CheckCircle, XCircle, UserX, UserCheck, 
  ArrowLeft, CalendarDays, Search, Clock, ArrowRight, BookOpen
} from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

export default function SessionHistoryTab({ classId }) {
  const [view, setView] = useState('list'); // 'list' hoặc 'detail'
  const [selectedSession, setSelectedSession] = useState(null);

  // States cho List
  const [sessions, setSessions] = useState([]);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  const [sessionCheckInsMap, setSessionCheckInsMap] = useState({});

  // States cho Detail
  const [members, setMembers] = useState([]);
  const [totalStudents, setTotalStudents] = useState(0);
  const [events, setEvents] = useState([]);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [subTab, setSubTab] = useState('pending'); // 'pending' | 'checked-in'
  const [searchQuery, setSearchQuery] = useState('');

  // Tải danh sách thành viên và tính tổng số sinh viên
  useEffect(() => {
    const loadClassMembers = async () => {
      if (!classId || classId.startsWith('mock-')) return;
      try {
        const memberRes = await classApi.getClassMembers(classId);
        const allMembers = Array.isArray(memberRes) ? memberRes : (memberRes.items || []);
        setMembers(allMembers);
        const students = allMembers.filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER');
        setTotalStudents(students.length);
      } catch (error) {
        console.error("Lỗi tải thành viên lớp:", error);
      }
    };
    loadClassMembers();
  }, [classId]);

  // 1. TẢI DANH SÁCH CÁC PHIÊN CỦA LỚP + ĐẾM CHÍNH XÁC TỪ EVENTS
  useEffect(() => {
    const loadSessions = async () => {
      if (!classId || classId.startsWith('mock-')) {
        setIsLoadingSessions(false);
        return;
      }

      setIsLoadingSessions(true);
      try {
        const res = await classApi.getGroupSessions(classId);
        const loadedSessions = res.items || [];
        setSessions(loadedSessions);

        const checkInsMap = {};
        await Promise.all(
          loadedSessions.map(async (sess) => {
            try {
              const eventRes = await classApi.getAttendanceEvents(sess.id, 200);
              const allEvents = Array.isArray(eventRes) ? eventRes : (eventRes.items || []);
              const latestStatusMap = new Map();
              const sorted = [...allEvents].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
              sorted.forEach(e => latestStatusMap.set(String(e.userId), e.newStatus));
              let count = 0;
              latestStatusMap.forEach(status => {
                if (status === 'PRESENT' || status === 'LATE') count++;
              });
              checkInsMap[sess.id] = count;
            } catch {
              checkInsMap[sess.id] = sess.checkIns || sess.checkinCount || 0;
            }
          })
        );
        setSessionCheckInsMap(checkInsMap);
      } catch (error) {
        console.error("Lỗi lấy danh sách phiên:", error);
        toast.error("Không thể tải lịch sử điểm danh.");
        setSessions([]);
      } finally {
        setIsLoadingSessions(false);
      }
    };

    if (view === 'list') loadSessions();
  }, [classId, view]);

  // 2. TẢI CHI TIẾT KHI CHỌN 1 PHIÊN
  useEffect(() => {
    const loadSessionDetail = async () => {
      if (view !== 'detail' || !selectedSession || !classId || classId.startsWith('mock-')) return;

      setIsLoadingDetail(true);
      try {
        let classMembers = members;
        if (members.length === 0) {
          const memberRes = await classApi.getClassMembers(classId);
          classMembers = Array.isArray(memberRes) ? memberRes : (memberRes.items || []);
          setMembers(classMembers);
        }

        const eventRes = await classApi.getAttendanceEvents(selectedSession.id, 200);
        const rawEvents = Array.isArray(eventRes) ? eventRes : (eventRes.items || []);
        setEvents(rawEvents);
      } catch (error) {
        console.error("Lỗi tải chi tiết phiên:", error);
        toast.error("Không thể tải chi tiết điểm danh.");
        setEvents([]);
      } finally {
        setIsLoadingDetail(false);
      }
    };

    loadSessionDetail();
  }, [view, selectedSession, classId]);

  const handleManualCheckIn = async (userId, status) => {
    if (!selectedSession || !selectedSession.id) return;

    const statusLabels = {
      'PRESENT': 'Có mặt',
      'LATE': 'Đi trễ',
      'ABSENT': 'Vắng mặt'
    };

    const loadingToast = toast.loading("Đang xử lý...");
    try {
      if (status === 'ABSENT') {
        await classApi.resetAttendance(selectedSession.id, userId);
        toast.success(`Đã hủy kết quả điểm danh`, { id: loadingToast });
        setEvents(prev => prev.filter(e => String(e.userId) !== String(userId)));
      } else {
        const payload = { 
          status: status, 
          note: "Ghi nhận thủ công",
          method: "MANUAL"
        };
        await classApi.submitAttendance(selectedSession.id, userId, payload);
        toast.success(`Đã ghi nhận: ${statusLabels[status]}`, { id: loadingToast });

        const newEvent = { 
          userId, 
          newStatus: status, 
          createdAt: new Date().toISOString(),
          method: "MANUAL"
        };
        setEvents(prev => [newEvent, ...prev.filter(e => String(e.userId) !== String(userId))]);
      }

      setSessions(prevSessions => prevSessions.map(s => {
        if (s.id === selectedSession.id) {
          let currentCount = s.checkIns || s.checkinCount || 0;
          if ((status === 'PRESENT' || status === 'LATE') && !checkedInUserIds.has(String(userId))) {
            currentCount += 1;
          } else if (status === 'ABSENT' && checkedInUserIds.has(String(userId))) {
            currentCount = Math.max(0, currentCount - 1);
          }
          return { ...s, checkIns: currentCount, checkinCount: currentCount };
        }
        return s;
      }));

    } catch (error) {
      console.error("Lỗi điểm danh tại Component:", error);
      toast.error("Thao tác thất bại. Vui lòng kiểm tra kết nối!", { id: loadingToast });
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('vi-VN', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
  };
  
  const formatTime = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const getLatestAttendanceMap = (allEvents) => {
    const map = new Map();
    const sortedEvents = [...allEvents].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
    sortedEvents.forEach(event => {
      map.set(String(event.userId), event.newStatus);
    });
    return map;
  };

  const latestStatusMap = getLatestAttendanceMap(events);

  const checkedInUserIds = new Set();
  latestStatusMap.forEach((status, userId) => {
    if (status === 'PRESENT' || status === 'LATE') {
      checkedInUserIds.add(userId);
    }
  });

  const filteredCheckedIn = Array.from(checkedInUserIds).map(userId => {
    const memberInfo = members.find(m => String(m.studentCode || m.code || m.userId || m.id) === userId);
    const userEvents = events
      .filter(e => String(e.userId) === userId)
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    const latestEvent = userEvents[0];

    return {
      ...memberInfo,
      userId: userId,
      checkInTime: latestEvent?.createdAt,
      attendanceStatus: latestStatusMap.get(userId),
      fullName: memberInfo?.fullName || 'Sinh viên ẩn danh',
      studentCode: memberInfo?.studentCode || memberInfo?.code || userId.slice(0, 8).toUpperCase(),
    };
  })
  .filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()))
  .sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime));

  const pendingUsers = members
    .filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER') 
    .filter(m => !checkedInUserIds.has(String(m.studentCode || m.code || m.userId || m.id)))
    .filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()) || 
                 (m.studentCode || m.code || "").toLowerCase().includes(searchQuery.toLowerCase()));

  // =============================================================
  // VIEW 1: MÀN HÌNH DANH SÁCH PHIÊN
  // =============================================================
  if (view === 'list') {
    return (
      <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gradient-to-r from-gray-50 to-white">
          <h3 className="font-bold text-gray-900 flex items-center gap-2.5 text-base">
            <div className="w-8 h-8 rounded-lg bg-red-50 flex items-center justify-center text-red-600">
              <CalendarDays size={18} />
            </div>
            Lịch sử phiên điểm danh
          </h3>
          <span className="text-xs font-semibold text-gray-500 bg-gray-100 px-3 py-1 rounded-full">
            Tổng số: {sessions.length} phiên
          </span>
        </div>

        <div className="overflow-x-auto min-h-[300px]">
          {isLoadingSessions ? (
            <div className="animate-fade-in-up">
              <table className="w-full text-left text-sm border-collapse">
                <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100 uppercase text-[10px] tracking-wider">
                  <tr>
                    <th className="p-4 pl-6">Ngày học</th>
                    <th className="p-4">Thời gian mở</th>
                    <th className="p-4">Trạng thái</th>
                    <th className="p-4">Phương thức</th>
                    <th className="p-4">Hiện diện</th>
                    <th className="p-4 pr-6 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {[...Array(4)].map((_, index) => (
                    <tr key={`sk-sess-${index}`} className="hover:bg-gray-50/50">
                      <td className="p-4 pl-6">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg bg-gray-200 shimmer-loader shrink-0" />
                          <div className="space-y-1.5">
                            <div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" />
                            <div className="w-16 h-3 bg-gray-200 rounded shimmer-loader" />
                          </div>
                        </div>
                      </td>
                      <td className="p-4">
                        <div className="space-y-1.5">
                          <div className="w-28 h-4 bg-gray-200 rounded shimmer-loader" />
                          <div className="w-20 h-3 bg-gray-200 rounded shimmer-loader" />
                        </div>
                      </td>
                      <td className="p-4"><div className="w-16 h-6 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-20 h-6 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4">
                        <div className="space-y-1.5">
                          <div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" />
                          <div className="w-32 h-2.5 bg-gray-100 rounded-full overflow-hidden">
                            <div className="w-1/2 h-full bg-gray-200 shimmer-loader rounded-full" />
                          </div>
                        </div>
                      </td>
                      <td className="p-4 pr-6 text-right"><div className="w-24 h-8 bg-gray-200 rounded-lg shimmer-loader ml-auto" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : sessions.length === 0 ? (
            <div className="flex flex-col justify-center items-center h-56 text-gray-400">
              <div className="w-12 h-12 rounded-full bg-gray-50 flex items-center justify-center mb-3">
                <CalendarDays size={24} className="text-gray-300" />
              </div>
              <p className="text-sm font-medium">Chưa có phiên điểm danh nào được ghi nhận</p>
            </div>
          ) : (
            <table className="w-full text-left text-sm border-collapse">
              <thead className="bg-gray-50 text-gray-500 font-bold border-b border-gray-100 uppercase text-[10px] tracking-wider">
                <tr>
                  <th className="p-4 pl-6">Ngày học</th>
                  <th className="p-4">Thời gian mở</th>
                  <th className="p-4">Trạng thái</th>
                  <th className="p-4">Tỷ lệ đi học</th>
                  <th className="p-4 pr-6 text-right">Chi tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sessions.map((sess, idx) => {
                  const checkInCount = sessionCheckInsMap[sess.id] ?? (sess.checkIns || sess.checkinCount || 0);
                  const finalTotal = totalStudents > 0 ? totalStudents : (sess.total || 0);
                  const rate = finalTotal > 0 ? Math.round((checkInCount / finalTotal) * 100) : 0;
                  const isOpen = sess.status === 'OPEN' || sess.status === 'ACTIVE';

                  return (
                    <tr key={sess.id || idx} className="hover:bg-gray-50/50 transition-colors group">
                      <td className="p-4 pl-6 font-bold text-gray-900">{formatDate(sess.checkinOpenAt)}</td>
                      <td className="p-4 text-gray-500 font-medium">
                        <div className="flex items-center gap-1.5">
                          <Clock size={13} className="text-gray-400" />
                          <span>{formatTime(sess.checkinOpenAt)} - {sess.checkinCloseAt ? formatTime(sess.checkinCloseAt) : 'Đang diễn ra'}</span>
                        </div>
                      </td>
                      <td className="p-4">
                        {isOpen ? (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-red-50 text-red-600 rounded-md text-[10px] font-bold border border-red-100">
                            <span className="w-1.5 h-1.5 bg-red-600 rounded-full animate-ping"></span> ĐANG MỞ
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-gray-50 text-gray-500 rounded-md text-[10px] font-bold border border-gray-200">
                            ĐÃ ĐÓNG
                          </span>
                        )}
                      </td>
                      <td className="p-4">
                        <div className="flex items-center gap-3">
                          <span className={`font-bold text-xs ${rate >= 80 ? 'text-emerald-600' : rate >= 50 ? 'text-amber-600' : 'text-red-500'}`}>
                            {rate}%
                          </span>
                          <div className="w-24 h-2 bg-gray-100 rounded-full overflow-hidden shadow-inner">
                            <div
                              className={`h-full rounded-full transition-all duration-500 ${
                                rate >= 80 ? 'bg-gradient-to-r from-emerald-400 to-emerald-500' : 
                                rate >= 50 ? 'bg-gradient-to-r from-amber-400 to-amber-500' : 
                                'bg-gradient-to-r from-red-400 to-red-500'
                              }`}
                              style={{ width: `${rate > 100 ? 100 : rate}%` }}
                            ></div>
                          </div>
                          <span className="text-[10px] text-gray-400 font-semibold bg-gray-50 px-1.5 py-0.5 rounded border border-gray-100">
                            {checkInCount}/{finalTotal}
                          </span>
                        </div>
                      </td>
                      <td className="p-4 pr-6 text-right">
                        <button
                          onClick={() => { setSelectedSession(sess); setView('detail'); }}
                          className="inline-flex items-center gap-1 text-red-600 font-bold hover:text-red-700 text-[13px] bg-red-50/50 hover:bg-red-50 px-3 py-1.5 rounded-lg border border-red-100/50 hover:border-red-200 transition-all shadow-sm"
                        >
                          Xem chi tiết <ArrowRight size={13} className="transition-transform group-hover:translate-x-0.5" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    );
  }

  // =============================================================
  // VIEW 2: MÀN HÌNH CHI TIẾT 1 PHIÊN ĐIỂM DANH THỦ CÔNG
  // =============================================================
  return (
    <div className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden animate-in slide-in-from-right-4 duration-300">
      
      {/* Header của Detail */}
      <div className="p-5 border-b border-gray-100 flex flex-col sm:flex-row justify-between sm:items-center gap-4 bg-gradient-to-r from-gray-50 to-white">
        <div className="flex items-center gap-3">
          <button
            onClick={() => { setView('list'); setSearchQuery(''); setSubTab('pending'); }}
            className="p-2 hover:bg-gray-200 rounded-lg text-gray-500 hover:text-gray-900 border border-gray-200 bg-white transition-all shadow-sm shrink-0"
          >
            <ArrowLeft size={16} />
          </button>
          <div>
            <h3 className="font-extrabold text-gray-900 leading-tight text-[15px] uppercase tracking-wide">Chi tiết phiên điểm danh</h3>
            <p className="text-xs text-gray-400 mt-1 flex items-center gap-1.5 font-medium">
              <Clock size={12} />
              Mở lúc: {formatTime(selectedSession.checkinOpenAt)} - {formatDate(selectedSession.checkinOpenAt)}
            </p>
          </div>
        </div>
        
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
          <input
            type="text"
            placeholder="Tìm tên hoặc mã sinh viên..."
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
            className="pl-9 pr-4 py-2 border border-gray-200 rounded-xl text-xs font-semibold outline-none focus:border-red-300 focus:ring-4 focus:ring-red-50 bg-white w-full sm:w-64 transition-all"
          />
        </div>
      </div>

      {/* Sub-Tabs: Pending vs Checked-in */}
      <div className="flex border-b border-gray-100 bg-gray-50/50">
        <button 
          onClick={() => setSubTab('pending')} 
          className={`flex-1 py-3.5 text-xs font-extrabold flex items-center justify-center gap-2 transition-all border-b-2 uppercase tracking-wider ${
            subTab === 'pending' 
              ? 'text-red-600 border-red-600 bg-white shadow-sm' 
              : 'text-gray-500 border-transparent hover:bg-gray-100/50 hover:text-gray-700'
          }`}
        >
          <UserX size={15} /> Chưa điểm danh 
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${subTab === 'pending' ? 'bg-red-100 text-red-700' : 'bg-gray-200 text-gray-600'}`}>
            {pendingUsers.length}
          </span>
        </button>
        <button 
          onClick={() => setSubTab('checked-in')} 
          className={`flex-1 py-3.5 text-xs font-extrabold flex items-center justify-center gap-2 transition-all border-b-2 uppercase tracking-wider ${
            subTab === 'checked-in' 
              ? 'text-emerald-600 border-emerald-600 bg-white shadow-sm' 
              : 'text-gray-500 border-transparent hover:bg-gray-100/50 hover:text-gray-700'
          }`}
        >
          <UserCheck size={15} /> Đã điểm danh 
          <span className={`px-2 py-0.5 rounded-full text-[10px] font-black ${subTab === 'checked-in' ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-200 text-gray-600'}`}>
            {filteredCheckedIn.length}
          </span>
        </button>
      </div>

      {/* Data Table */}
      <div className="overflow-x-auto min-h-[300px]">
        {isLoadingDetail ? (
          <div className="animate-fade-in-up">
            <table className="w-full text-left text-sm border-collapse">
              <thead className="bg-gray-50/70 text-gray-500 font-bold border-b border-gray-100 uppercase text-[10px] tracking-wider">
                <tr>
                  <th className="p-4 pl-6 w-2/5">Sinh viên</th>
                  <th className="p-4 w-1/4">Mã sinh viên</th>
                  <th className="p-4 pr-6 text-right">Ghi nhận thủ công</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {[...Array(3)].map((_, index) => (
                  <tr key={`sk-det-${index}`} className="hover:bg-gray-50/50">
                    <td className="p-4 pl-6">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gray-200 shimmer-loader shrink-0" />
                        <div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" />
                      </div>
                    </td>
                    <td className="p-4"><div className="w-16 h-4 bg-gray-200 rounded shimmer-loader" /></td>
                    <td className="p-4 pr-6 text-right"><div className="w-32 h-8 bg-gray-200 rounded-lg shimmer-loader ml-auto" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : subTab === 'pending' ? (
          <table className="w-full text-left text-sm border-collapse">
            <thead className="bg-gray-50/70 text-gray-500 font-bold border-b border-gray-100 uppercase text-[10px] tracking-wider">
              <tr>
                <th className="p-4 pl-6 w-2/5">Sinh viên</th>
                <th className="p-4 w-1/4">Mã sinh viên</th>
                <th className="p-4 pr-6 text-right">Ghi nhận thủ công</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pendingUsers.map(user => (
                <tr key={user.userId || user.id} className="hover:bg-gray-50/40 transition-colors">
                  <td className="p-4 pl-6 flex items-center gap-3">
                    <img 
                      src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=fca5a5&color=b91c1c&bold=true`} 
                      className="w-9 h-9 rounded-full object-cover border-2 border-white ring-2 ring-gray-100" 
                      alt="" 
                    />
                    <span className="font-bold text-gray-900">{user.fullName}</span>
                  </td>
                  <td className="p-4 text-gray-500 font-bold font-mono text-xs">
                    {user.studentCode || user.code || 'N/A'}
                  </td>
                  <td className="p-4 pr-6 text-right">
                    <div className="flex justify-end gap-2">
                      <button 
                        onClick={() => handleManualCheckIn(user.studentCode || user.code || user.userId || user.id, 'PRESENT')} 
                        className="px-3.5 py-1.5 bg-emerald-50 text-emerald-600 hover:bg-emerald-600 hover:text-white rounded-lg text-xs font-bold flex items-center gap-1 border border-emerald-200 transition-all hover:scale-[1.02] shadow-sm"
                      >
                        <CheckCircle size={13} /> Có mặt
                      </button>
                      <button 
                        onClick={() => handleManualCheckIn(user.studentCode || user.code || user.userId || user.id, 'ABSENT')} 
                        className="px-3.5 py-1.5 bg-red-50 text-red-600 hover:bg-red-600 hover:text-white rounded-lg text-xs font-bold flex items-center gap-1 border border-red-200 transition-all hover:scale-[1.02] shadow-sm"
                      >
                        <XCircle size={13} /> Vắng
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {pendingUsers.length === 0 && (
                <tr>
                  <td colSpan="3" className="p-12 text-center bg-emerald-50/30">
                    <div className="flex flex-col items-center justify-center">
                      <div className="w-12 h-12 rounded-full bg-emerald-50 border border-emerald-100 flex items-center justify-center mb-3">
                        <CheckCircle className="text-emerald-500" size={24} />
                      </div>
                      <p className="text-sm font-extrabold text-emerald-800">Tất cả sinh viên đã được điểm danh! 🎉</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        ) : (
          <table className="w-full text-left text-sm border-collapse">
            <thead className="bg-gray-50/70 text-gray-500 font-bold border-b border-gray-100 uppercase text-[10px] tracking-wider">
              <tr>
                <th className="p-4 pl-6 w-2/5">Sinh viên</th>
                <th className="p-4">Mã sinh viên</th>
                <th className="p-4">Thời gian ghi nhận</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 pr-6 text-right">Hủy bỏ</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredCheckedIn.map((user, idx) => (
                <tr key={idx} className="hover:bg-gray-50/40 transition-colors">
                  <td className="p-4 pl-6 flex items-center gap-3">
                    <img 
                      src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=86efac&color=15803d&bold=true`} 
                      className="w-9 h-9 rounded-full object-cover border-2 border-white ring-2 ring-gray-100" 
                      alt="" 
                    />
                    <span className="font-bold text-gray-900">{user.fullName}</span>
                  </td>
                  <td className="p-4 text-gray-500 font-bold font-mono text-xs">
                    {user.studentCode || 'N/A'}
                  </td>
                  <td className="p-4 text-gray-600 font-bold font-mono text-xs">
                    <div className="flex items-center gap-1">
                      <Clock size={12} className="text-gray-400" />
                      <span>{formatTime(user.checkInTime)}</span>
                    </div>
                  </td>
                  <td className="p-4">
                    <span className={`inline-flex px-2.5 py-1 rounded-md text-[10px] font-bold border uppercase tracking-wider ${
                      user.attendanceStatus === 'PRESENT' 
                        ? 'bg-emerald-50 text-emerald-600 border-emerald-100' 
                        : 'bg-amber-50 text-amber-600 border-amber-100'
                    }`}>
                      {user.attendanceStatus === 'PRESENT' ? 'CÓ MẶT' : 'ĐI TRỄ'}
                    </span>
                  </td>
                  <td className="p-4 pr-6 text-right">
                    <button 
                      onClick={() => handleManualCheckIn(user.studentCode || user.code || user.userId || user.id, 'ABSENT')} 
                      className="text-gray-400 hover:text-red-600 text-xs font-bold hover:underline transition-all"
                    >
                      Hủy điểm danh
                    </button>
                  </td>
                </tr>
              ))}
              {filteredCheckedIn.length === 0 && (
                <tr>
                  <td colSpan="5" className="p-12 text-center text-gray-400 font-medium">
                    Chưa tìm thấy dữ liệu điểm danh hợp lệ
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}