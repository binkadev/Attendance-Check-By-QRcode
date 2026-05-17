import React, { useState, useEffect } from 'react';
import { Loader2, CheckCircle, XCircle, UserX, UserCheck, ArrowLeft, CalendarDays, Search } from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

export default function SessionHistoryTab({ classId }) {
  const [view, setView] = useState('list'); // 'list' hoặc 'detail'
  const [selectedSession, setSelectedSession] = useState(null);

  // States cho List
  const [sessions, setSessions] = useState([]);
  const [isLoadingSessions, setIsLoadingSessions] = useState(true);
  // Map: sessionId -> accurate checkIn count from events API
  const [sessionCheckInsMap, setSessionCheckInsMap] = useState({});

  // States cho Detail
  const [members, setMembers] = useState([]);
  const [totalStudents, setTotalStudents] = useState(0);
  const [events, setEvents] = useState([]);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [subTab, setSubTab] = useState('pending'); // 'pending' | 'checked-in'
  const [searchQuery, setSearchQuery] = useState('');



  // Tải danh sách thành viên và tính sự tổng số sinh viên
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

  // 1. TẢI DANH SÁCH CÁC PHIÊN CỦA LỚP + ĐẺM CHÍNH XÁC TỪ EVENTS
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

        // Fetch events cho từng phiên để tính tỷ lệ chính xác
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
        // Tải danh sách sinh viên của lớp (nếu chưa tải)
        let classMembers = members;
        if (members.length === 0) {
          const memberRes = await classApi.getClassMembers(classId);
          // Hỗ trợ cả trường hợp API trả về mảng trực tiếp hoặc trả về { items: [...] }
          classMembers = Array.isArray(memberRes) ? memberRes : (memberRes.items || []);
          setMembers(classMembers);
        }

        // Tải danh sách sự kiện điểm danh của phiên
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
        // GỌI ĐÚNG API RESET
        await classApi.resetAttendance(selectedSession.id, userId);
        toast.success(`Đã hủy kết quả điểm danh`, { id: loadingToast });

        // Vì RESET là xóa/hủy hoàn toàn, ta xóa hết các event cũ của SV này trong state
        setEvents(prev => prev.filter(e => String(e.userId) !== String(userId)));
      } else {
        // Ghi nhận PRESENT/LATE
        const payload = { status: status, note: "Ghi nhận thủ công" };
        await classApi.submitAttendance(selectedSession.id, userId, payload);
        toast.success(`Đã ghi nhận: ${statusLabels[status]}`, { id: loadingToast });

        const newEvent = { userId, newStatus: status, createdAt: new Date().toISOString() };
        setEvents(prev => [newEvent, ...prev.filter(e => String(e.userId) !== String(userId))]);
      }

      // Cập nhật con số tổng ở màn hình ngoài
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

  // Helper Formats
  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('vi-VN', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
  };
  const formatTime = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  // =============================================================
  // VIEW 1: MÀN HÌNH DANH SÁCH PHIÊN
  // =============================================================
  if (view === 'list') {
    return (
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden animate-in fade-in duration-300">
        <div className="p-5 border-b border-gray-200 flex justify-between items-center bg-gray-50/50">
          <h3 className="font-bold text-gray-900 flex items-center gap-2"><CalendarDays size={18} className="text-red-600" /> Lịch sử phiên điểm danh</h3>
        </div>

        <div className="overflow-x-auto min-h-[400px]">
          {isLoadingSessions ? (
            <div className="flex justify-center items-center h-48"><Loader2 className="animate-spin text-gray-400" /></div>
          ) : sessions.length === 0 ? (
            <div className="flex flex-col justify-center items-center h-48 text-gray-400">
              <CalendarDays size={32} className="mb-2 opacity-50" />
              <p>Chưa có phiên điểm danh nào.</p>
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 font-semibold border-b uppercase text-[11px] tracking-wider">
                <tr>
                  <th className="p-4">Ngày học</th>
                  <th className="p-4">Thời gian</th>
                  <th className="p-4">Trạng thái</th>
                  <th className="p-4">Tỷ lệ điểm danh</th>
                  <th className="p-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sessions.map((sess, idx) => {
                  const checkInCount = sessionCheckInsMap[sess.id] ?? (sess.checkIns || sess.checkinCount || 0);
                  const finalTotal = totalStudents > 0 ? totalStudents : (sess.total || 0);
                  const rate = finalTotal > 0 ? Math.round((checkInCount / finalTotal) * 100) : 0;

                  return (
                    <tr key={sess.id || idx} className="hover:bg-gray-50 transition-colors">
                      <td className="p-4 font-bold text-gray-900">{formatDate(sess.checkinOpenAt)}</td>
                      <td className="p-4 text-gray-500 font-medium">
                        {formatTime(sess.checkinOpenAt)} - {sess.checkinCloseAt ? formatTime(sess.checkinCloseAt) : 'Đang diễn ra'}
                      </td>
                      <td className="p-4">
                        {sess.status === 'OPEN' || sess.status === 'ACTIVE' ? (
                          <span className="px-2 py-1 bg-red-50 text-red-600 rounded text-[11px] font-bold border border-red-100 flex items-center w-max gap-1">
                            <span className="w-1.5 h-1.5 bg-red-600 rounded-full animate-pulse"></span> ĐANG MỞ
                          </span>
                        ) : (
                          <span className="px-2 py-1 bg-gray-100 text-gray-600 rounded text-[11px] font-bold border border-gray-200">ĐÃ ĐÓNG</span>
                        )}
                      </td>
                      {/* <td className="p-4">
                         <div className="flex items-center gap-2">
                           <span className="text-emerald-600 font-bold">{rate}%</span>
                           <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
           <div 
             className={`h-full ${rate >= 80 ? 'bg-emerald-500' : 'bg-amber-500'}`} 
             style={{width: `${rate > 100 ? 100 : rate}%`}} // Tránh tràn thanh bar nếu > 100%
           ></div>
         </div>
                         </div>
                      </td> */}
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          <span className="text-emerald-600 font-bold">{rate}%</span>
                          <div className="w-20 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                            <div
                              className={`h-full ${rate >= 80 ? 'bg-emerald-500' : 'bg-amber-500'}`}
                              style={{ width: `${rate > 100 ? 100 : rate}%` }}
                            ></div>
                          </div>
                          {/* Hiển thị số cụ thể để đối chiếu */}
                          <span className="text-[10px] text-gray-400">({checkInCount}/{finalTotal})</span>
                        </div>
                      </td>
                      <td className="p-4 text-right">
                        <button
                          onClick={() => { setSelectedSession(sess); setView('detail'); }}
                          className="text-indigo-600 font-bold hover:underline text-[13px] px-3 py-1.5 rounded-md hover:bg-indigo-50 transition-colors"
                        >
                          Xem chi tiết
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

  // // 1. Lấy danh sách ID đã điểm danh (Ép kiểu String để so sánh chuẩn xác)
  // const checkedInUserIds = new Set(
  //   events
  //     .filter(e => e.newStatus === 'PRESENT' || e.newStatus === 'LATE')
  //     .map(e => String(e.userId))
  // );

  // // 2. Map thông tin sinh viên ĐÃ điểm danh
  // const filteredCheckedIn = events
  //   .filter(e => e.newStatus === 'PRESENT' || e.newStatus === 'LATE')
  //   .map(event => {
  //     const memberInfo = members.find(m => String(m.userId || m.id) === String(event.userId));
  //     return {
  //       ...memberInfo,
  //       userId: event.userId,
  //       checkInTime: event.createdAt,
  //       attendanceStatus: event.newStatus,
  //       fullName: memberInfo?.fullName || 'Sinh viên ẩn danh',
  //       email: memberInfo?.email || 'N/A'
  //     };
  //   })
  //   .filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()))
  //   .sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime));

  // // 3. Lọc sinh viên CHƯA điểm danh (Pending)
  // const pendingUsers = members
  //   .filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER')
  //   .filter(m => !checkedInUserIds.has(String(m.userId || m.id)))
  //   .filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
  //     (m.email || "").toLowerCase().includes(searchQuery.toLowerCase()));

  // // In Console để Phú kiểm tra xem data vào đúng chưa
  // console.log("Tổng số Members:", members.length);
  // console.log("Số pending:", pendingUsers.length);
  // console.log("Số checked-in:", filteredCheckedIn.length);

  // --- LOGIC MỚI: CHỈ LẤY TRẠNG THÁI CUỐI CÙNG CỦA MỖI SINH VIÊN ---
const getLatestAttendanceMap = (allEvents) => {
  const map = new Map();
  // Sắp xếp events theo thời gian từ cũ đến mới
  const sortedEvents = [...allEvents].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
  
  sortedEvents.forEach(event => {
    map.set(String(event.userId), event.newStatus);
  });
  return map;
};

const latestStatusMap = getLatestAttendanceMap(events);

// 1. Lấy danh sách ID thực sự đang có mặt (Status cuối cùng là PRESENT hoặc LATE)
const checkedInUserIds = new Set();
latestStatusMap.forEach((status, userId) => {
  if (status === 'PRESENT' || status === 'LATE') {
    checkedInUserIds.add(userId);
  }
});

// // 2. Map thông tin để hiển thị tab "Đã điểm danh"
// const filteredCheckedIn = Array.from(checkedInUserIds).map(userId => {
//   const memberInfo = members.find(m => String(m.userId || m.id) === userId);
//   // Tìm event cuối cùng của user này để lấy thời gian
//   const userEvents = events.filter(e => String(e.userId) === userId);
//   const lastEvent = userEvents[userEvents.length - 1];

//   return {
//     ...memberInfo,
//     userId: userId,
//     checkInTime: lastEvent?.createdAt,
//     attendanceStatus: latestStatusMap.get(userId),
//     fullName: memberInfo?.fullName || 'Sinh viên ẩn danh',
//   };
// }).filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()));

// --- LOGIC MỚI: ĐẢM BẢO THỜI GIAN LUÔN LÀ MỚI NHẤT ---

// 2. Map thông tin để hiển thị tab "Đã điểm danh"
const filteredCheckedIn = Array.from(checkedInUserIds).map(userId => {
  const memberInfo = members.find(m => String(m.userId || m.id) === userId);
  
  // Lọc ra các event của user này và SẮP XẾP MỚI NHẤT LÊN ĐẦU
  const userEvents = events
    .filter(e => String(e.userId) === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  
  const latestEvent = userEvents[0]; // Cái đầu tiên chắc chắn là cái mới nhất

  return {
    ...memberInfo,
    userId: userId,
    checkInTime: latestEvent?.createdAt, // Đây sẽ là thời gian re-scan mới nhất
    attendanceStatus: latestStatusMap.get(userId),
    fullName: memberInfo?.fullName || 'Sinh viên ẩn danh',
  };
})
.filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()))
// Sắp xếp danh sách hiển thị: Ai vừa mới điểm danh (re-scan) thì nổi lên đầu bảng
.sort((a, b) => new Date(b.checkInTime) - new Date(a.checkInTime));

// 3. SV Chưa điểm danh (Những người không có trong checkedInUserIds)
const pendingUsers = members
  .filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER') 
  .filter(m => !checkedInUserIds.has(String(m.userId || m.id)))
  .filter(m => (m.fullName || "").toLowerCase().includes(searchQuery.toLowerCase()));

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden animate-in slide-in-from-right-4 duration-300">

      {/* Header của Detail */}
      <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50/80">
        <div className="flex items-center gap-4">
          <button
            onClick={() => { setView('list'); setSearchQuery(''); setSubTab('pending'); }}
            className="p-1.5 hover:bg-gray-200 rounded-md text-gray-500 transition-colors"
          >
            <ArrowLeft size={18} />
          </button>
          <div>
            <h3 className="font-bold text-gray-900 leading-tight">Chi tiết phiên điểm danh</h3>
            <p className="text-xs text-gray-500">Mở lúc: {formatTime(selectedSession.checkinOpenAt)} - {formatDate(selectedSession.checkinOpenAt)}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={14} />
            <input
              type="text"
              placeholder="Tìm sinh viên..."
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
              className="pl-8 pr-3 py-1.5 border border-gray-200 rounded-lg text-sm outline-none focus:border-indigo-300 bg-white w-64 transition-all"
            />
          </div>
        </div>
      </div>

      {/* Sub-Tabs: Pending vs Checked-in */}
      <div className="flex border-b border-gray-200">
        <button onClick={() => setSubTab('pending')} className={`flex-1 py-3 text-sm font-bold flex items-center justify-center gap-2 transition-colors ${subTab === 'pending' ? 'bg-red-50 text-red-600 border-b-2 border-red-600' : 'text-gray-500 hover:bg-gray-50'}`}>
          <UserX size={16} /> Chưa điểm danh ({pendingUsers.length})
        </button>
        <button onClick={() => setSubTab('checked-in')} className={`flex-1 py-3 text-sm font-bold flex items-center justify-center gap-2 transition-colors ${subTab === 'checked-in' ? 'bg-emerald-50 text-emerald-600 border-b-2 border-emerald-600' : 'text-gray-500 hover:bg-gray-50'}`}>
          <UserCheck size={16} /> Đã điểm danh ({filteredCheckedIn.length})
        </button>
      </div>

      {/* Data Table */}
      <div className="overflow-x-auto min-h-[400px]">
        {isLoadingDetail ? (
          <div className="flex justify-center items-center h-48"><Loader2 className="animate-spin text-gray-400" /></div>
        ) : subTab === 'pending' ? (
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-500 font-semibold border-b">
              <tr>
                <th className="p-4 w-2/5">Sinh viên</th>
                <th className="p-4 w-1/4">Mã SV / Email</th>
                <th className="p-4 text-right">Thao tác thủ công</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pendingUsers.map(user => (
                <tr key={user.userId || user.id} className="hover:bg-gray-50">
                  <td className="p-4 flex items-center gap-3">
                    <img src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=random`} className="w-8 h-8 rounded-full" alt="" />
                    <span className="font-bold text-gray-900">{user.fullName}</span>
                  </td>
                  <td className="p-4 text-gray-500 font-mono text-xs">{user.email?.split('@')[0]}</td>
                  <td className="p-4 text-right">
                    <div className="flex justify-end gap-2">
                      <button onClick={() => handleManualCheckIn(user.userId || user.id, 'PRESENT')} className="px-3 py-1.5 bg-emerald-50 text-emerald-600 hover:bg-emerald-100 rounded-md text-xs font-bold flex items-center gap-1 border border-emerald-200 shadow-sm transition-colors">
                        <CheckCircle size={14} /> Có mặt
                      </button>
                      <button onClick={() => handleManualCheckIn(user.userId || user.id, 'ABSENT')} className="px-3 py-1.5 bg-red-50 text-red-600 hover:bg-red-100 rounded-md text-xs font-bold flex items-center gap-1 border border-red-200 shadow-sm transition-colors">
                        <XCircle size={14} /> Vắng
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {pendingUsers.length === 0 && <tr><td colSpan="3" className="p-10 text-center text-emerald-500 font-bold bg-emerald-50/50">Tất cả sinh viên đã được điểm danh! 🎉</td></tr>}
            </tbody>
          </table>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="bg-gray-50 text-gray-500 font-semibold border-b">
              <tr>
                <th className="p-4 w-2/5">Sinh viên</th>
                <th className="p-4">Thời gian ghi nhận</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 text-right">Sửa đổi</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredCheckedIn.map((user, idx) => (
                <tr key={idx} className="hover:bg-gray-50">
                  <td className="p-4 flex items-center gap-3">
                    <img src={user.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.fullName)}&background=random`} className="w-8 h-8 rounded-full" alt="" />
                    <span className="font-bold text-gray-900">{user.fullName}</span>
                  </td>
                  <td className="p-4 text-gray-600 font-mono text-[13px]">{formatTime(user.checkInTime)}</td>
                  <td className="p-4">
                    <span className={`px-2.5 py-1 rounded-md text-[11px] font-bold ${user.attendanceStatus === 'PRESENT' ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-amber-50 text-amber-600 border border-amber-100'}`}>
                      {user.attendanceStatus === 'PRESENT' ? 'CÓ MẶT' : 'ĐI TRỄ'}
                    </span>
                  </td>
                  <td className="p-4 text-right">
                    <button onClick={() => handleManualCheckIn(user.userId || user.id, 'ABSENT')} className="text-gray-400 hover:text-red-600 text-xs font-semibold underline decoration-dotted transition-colors">
                      Hủy điểm danh
                    </button>
                  </td>
                </tr>
              ))}
              {filteredCheckedIn.length === 0 && <tr><td colSpan="4" className="p-10 text-center text-gray-400 font-medium">Chưa có dữ liệu điểm danh nào được tìm thấy.</td></tr>}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}