import React, { useState, useEffect } from 'react';
import { 
  Search, Download, Mail, ChevronLeft, ChevronRight, ShieldAlert, 
  Smartphone, Crosshair, Loader2, Trash2
} from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// --- MOCK DATA ---
const MOCK_STUDENTS = [
  { id: 'STU-8921', name: 'Lê Thị Hồng', email: 'hong.lt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704d', attendanceRate: 92, lastSeen: '12 Th10, 2023', joinedAt: '2023-10-12T08:30:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-7743', name: 'Trần Đăng Khoa', email: 'khoa.td@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704e', attendanceRate: 68, lastSeen: '05 Th10, 2023', joinedAt: '2023-10-05T09:15:00Z', attendanceStatus: 'At-Risk', memberStatus: 'ACTIVE' },
  { id: 'STU-6520', name: 'Nguyễn Văn Mạnh', email: 'manh.nv@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026704f', attendanceRate: 88, lastSeen: '12 Th10, 2023', joinedAt: '2023-10-10T10:00:00Z', attendanceStatus: 'Flagged', memberStatus: 'PENDING' },
  { id: 'STU-1122', name: 'Phạm Minh Tuấn', email: 'tuan.pm@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705a', attendanceRate: 95, lastSeen: '14 Th10, 2023', joinedAt: '2023-10-14T07:45:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-2233', name: 'Vũ Thanh Hằng', email: 'hang.vt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705b', attendanceRate: 100, lastSeen: '15 Th10, 2023', joinedAt: '2023-10-15T08:00:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-3344', name: 'Đỗ Tiến Đạt', email: 'dat.dt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705c', attendanceRate: 82, lastSeen: '10 Th10, 2023', joinedAt: '2023-10-10T09:20:00Z', attendanceStatus: 'Flagged', memberStatus: 'ACTIVE' },
  { id: 'STU-4455', name: 'Hoàng Thúy Quỳnh', email: 'quynh.ht@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705d', attendanceRate: 60, lastSeen: '01 Th10, 2023', joinedAt: '2023-10-01T10:15:00Z', attendanceStatus: 'At-Risk', memberStatus: 'INACTIVE' },
  { id: 'STU-5566', name: 'Ngô Tuấn Anh', email: 'anh.nt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705e', attendanceRate: 91, lastSeen: '15 Th10, 2023', joinedAt: '2023-10-15T13:30:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-6677', name: 'Lý Thảo Vy', email: 'vy.lt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026705f', attendanceRate: 74, lastSeen: '08 Th10, 2023', joinedAt: '2023-10-08T14:45:00Z', attendanceStatus: 'At-Risk', memberStatus: 'ACTIVE' },
  { id: 'STU-7788', name: 'Đặng Mai Phương', email: 'phuong.dm@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706a', attendanceRate: 85, lastSeen: '11 Th10, 2023', joinedAt: '2023-10-11T15:00:00Z', attendanceStatus: 'Flagged', memberStatus: 'PENDING' },
  { id: 'STU-8899', name: 'Bùi Gia Huy', email: 'huy.bg@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706b', attendanceRate: 98, lastSeen: '14 Th10, 2023', joinedAt: '2023-10-14T16:15:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-9900', name: 'Trương Mỹ Lan', email: 'lan.tm@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706c', attendanceRate: 77, lastSeen: '09 Th10, 2023', joinedAt: '2023-10-09T08:45:00Z', attendanceStatus: 'Flagged', memberStatus: 'ACTIVE' },
  { id: 'STU-1011', name: 'Chu Bảo Ngọc', email: 'ngoc.cb@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706d', attendanceRate: 93, lastSeen: '13 Th10, 2023', joinedAt: '2023-10-13T09:30:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-1213', name: 'Lâm Nhật Vượng', email: 'vuong.ln@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706e', attendanceRate: 65, lastSeen: '03 Th10, 2023', joinedAt: '2023-10-03T10:00:00Z', attendanceStatus: 'At-Risk', memberStatus: 'REJECTED' },
  { id: 'STU-1415', name: 'Kiều Trang Nhung', email: 'nhung.kt@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026706f', attendanceRate: 89, lastSeen: '12 Th10, 2023', joinedAt: '2023-10-12T11:20:00Z', attendanceStatus: 'Flagged', memberStatus: 'ACTIVE' },
  { id: 'STU-1617', name: 'Đoàn Hữu Trí', email: 'tri.dh@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707a', attendanceRate: 96, lastSeen: '15 Th10, 2023', joinedAt: '2023-10-15T12:10:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-1819', name: 'Hồ Tuấn Kiệt', email: 'kiet.ht@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707b', attendanceRate: 72, lastSeen: '06 Th10, 2023', joinedAt: '2023-10-06T13:40:00Z', attendanceStatus: 'At-Risk', memberStatus: 'ACTIVE' },
  { id: 'STU-2021', name: 'Phan Minh Anh', email: 'anh.pm@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707c', attendanceRate: 90, lastSeen: '14 Th10, 2023', joinedAt: '2023-10-14T14:50:00Z', attendanceStatus: 'Good', memberStatus: 'PENDING' },
  { id: 'STU-2223', name: 'Cao Gia Linh', email: 'linh.cg@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707d', attendanceRate: 86, lastSeen: '10 Th10, 2023', joinedAt: '2023-10-10T15:20:00Z', attendanceStatus: 'Flagged', memberStatus: 'ACTIVE' },
  { id: 'STU-2425', name: 'Thái Phương Nam', email: 'nam.tp@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707e', attendanceRate: 99, lastSeen: '15 Th10, 2023', joinedAt: '2023-10-15T16:30:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' },
  { id: 'STU-2627', name: 'Mai Diễm Hằng', email: 'hang.md@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026707f', attendanceRate: 79, lastSeen: '08 Th10, 2023', joinedAt: '2023-10-08T08:15:00Z', attendanceStatus: 'Flagged', memberStatus: 'ACTIVE' },
  { id: 'STU-2829', name: 'Tạ Minh Khang', email: 'khang.tm@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026708a', attendanceRate: 70, lastSeen: '04 Th10, 2023', joinedAt: '2023-10-04T09:45:00Z', attendanceStatus: 'At-Risk', memberStatus: 'PENDING' },
  { id: 'STU-3031', name: 'Tôn Nữ Diệu Anh', email: 'anh.tnd@university.edu', avatar: 'https://i.pravatar.cc/150?u=a042581f4e29026708b', attendanceRate: 94, lastSeen: '14 Th10, 2023', joinedAt: '2023-10-14T10:30:00Z', attendanceStatus: 'Good', memberStatus: 'ACTIVE' }
];

const ALERTS_DATA = [
  {
    id: 1, type: 'duplicate_device', title: 'Trùng ID thiết bị',
    desc: "Thiết bị 'iPhone 13' (ID: A8F2) được dùng bởi STU-6520 và STU-1198.",
    icon: <Smartphone size={18} className="text-red-500" />,
    bgColor: 'bg-red-50', borderColor: 'border-red-200'
  },
  {
    id: 2, type: 'suspicious_location', title: 'Vị trí khả nghi',
    desc: "STU-4421 điểm danh cách ranh giới cơ sở 2.4 dặm.",
    icon: <Crosshair size={18} className="text-amber-500" />,
    bgColor: 'bg-amber-50', borderColor: 'border-amber-200'
  }
];

export default function StudentsTab({ classId }) {
  const [students, setStudents] = useState([]);

  // States quản lý loading
  const [isInitialLoading, setIsInitialLoading] = useState(true);
  const [isPolling, setIsPolling] = useState(false);
  
  // States quản lý phân trang
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // --- THÊM STATE TÌM KIẾM & LỌC ---
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [filterAttendance, setFilterAttendance] = useState('ALL');
  const [filterMember, setFilterMember] = useState('ALL');
  
  // --- STATE CẢNH BÁO BẢO MẬT ---
  const [securityAlerts, setSecurityAlerts] = useState(ALERTS_DATA);

  // --- STATE XÓA THÀNH VIÊN LỚP HỌC ---
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [studentToDelete, setStudentToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDeleteStudent = (student) => {
    // Không thể xóa giảng viên/người tạo lớp khỏi lớp học
    if (student.role === 'LECTURER' || student.role === 'OWNER') {
      toast.error("Không thể xóa giảng viên/người tạo lớp khỏi lớp học!");
      return;
    }

    const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
    const currentUserId = currentUser.userId || currentUser.id;
    if (String(student.rawId || student.id) === String(currentUserId)) {
      toast.error("Bạn không thể tự xóa chính mình khỏi lớp học!");
      return;
    }

    setStudentToDelete(student);
    setShowDeleteModal(true);
  };

  const handleConfirmDelete = async () => {
    if (!studentToDelete) return;
    setIsDeleting(true);
    const loadingToast = toast.loading(`Đang xóa ${studentToDelete.name} khỏi lớp...`);

    try {
      if (!classId || classId.startsWith('mock-')) {
        setStudents(prev => prev.filter(s => s.id !== studentToDelete.id));
        toast.success(`Đã xóa sinh viên ${studentToDelete.name} (Mock)`, { id: loadingToast });
        setShowDeleteModal(false);
        setStudentToDelete(null);
        return;
      }

      await classApi.removeMember(classId, studentToDelete.rawId || studentToDelete.id);
      toast.success(`Đã xóa sinh viên ${studentToDelete.name} khỏi lớp thành công!`, { id: loadingToast });
      
      setShowDeleteModal(false);
      setStudentToDelete(null);
      // Tải lại danh sách
      fetchStudents(false);
    } catch (error) {
      console.error("Lỗi xóa sinh viên:", error);
      toast.error(error.message || "Xóa sinh viên thất bại!", { id: loadingToast });
    } finally {
      setIsDeleting(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return null;
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', { month: 'short', day: '2-digit', year: 'numeric' });
  };

  // --- HÀM XUẤT FILE ---
  const exportClientSideCSV = () => {
    // Tiêu đề cột rõ ràng, đầy đủ các thông tin cần thiết trong bảng
    let csvContent = "\uFEFF"; // UTF-8 Byte Order Mark (BOM) để hiển thị tiếng Việt trong Excel không bị lỗi font!
    csvContent += "Mã Sinh Viên (MSV),Họ và Tên,Email,Tỷ lệ Điểm danh (%),Trạng thái Học tập,Trạng thái Thành viên\n";
    
    students.forEach(student => {
      const rate = student.attendanceRate !== undefined ? `${student.attendanceRate}%` : 'Chưa cập nhật';
      const statusTrans = student.attendanceStatus === 'Good' ? 'Tốt' : 
                          student.attendanceStatus === 'Flagged' ? 'Nghi vấn' : 
                          student.attendanceStatus === 'At-Risk' ? 'Nguy cơ' : 'Chưa cập nhật';
      
      const memberStatusTrans = student.memberStatus === 'ACTIVE' ? 'Hoạt động' : 
                                student.memberStatus === 'PENDING' ? 'Chờ duyệt' : 'Không hoạt động';

      // Loại bỏ dấu phẩy trong các trường để tránh làm hỏng cấu trúc CSV
      const cleanName = (student.name || '').replace(/,/g, ' ');
      const cleanEmail = (student.email || '').replace(/,/g, ' ');
      
      csvContent += `${student.id},${cleanName},${cleanEmail},${rate},${statusTrans},${memberStatusTrans}\n`;
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Danh_sach_lop_${classId || 'export'}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  };

  const handleExportFile = async () => {
    const loadingToast = toast.loading("Đang chuẩn bị dữ liệu xuất file...");
    try {
      // 1. Nếu là mock class hoặc API chưa sẵn sàng
      if (!classId || classId.startsWith('mock-')) {
        exportClientSideCSV();
        toast.success("Đã xuất danh sách lớp học (Mẫu Local)", { id: loadingToast });
        return;
      }

      // 2. Gọi API thực tế
      try {
        const blob = await classApi.exportAttendance(classId);
        
        // Kiểm tra content type hoặc kích thước blob
        if (blob && blob.size > 100) {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          
          // Kiểm tra loại file trả về từ Backend để gán extension chuẩn
          const isExcel = blob.type.includes('spreadsheet') || blob.type.includes('excel') || blob.type.includes('officedocument');
          const extension = isExcel ? 'xlsx' : 'csv';
          
          a.download = `Bao_cao_diem_danh_${classId}.${extension}`;
          document.body.appendChild(a);
          a.click();
          a.remove();
          window.URL.revokeObjectURL(url);
          toast.success(`Đã xuất báo cáo điểm danh (.${extension})`, { id: loadingToast });
        } else {
          // Fallback sang Client-side CSV nếu API rỗng
          exportClientSideCSV();
          toast.success("Đã xuất dữ liệu hiển thị (Bản Local)", { id: loadingToast });
        }
      } catch (apiError) {
        console.warn("API Export failed, falling back to client-side CSV:", apiError);
        exportClientSideCSV();
        toast.success("Đã xuất dữ liệu hiển thị (Bản Local)", { id: loadingToast });
      }
    } catch (error) {
      console.error("Lỗi xuất file:", error);
      toast.error("Lỗi khi xuất dữ liệu", { id: loadingToast });
    }
  };

  // --- EFFECT DEBOUNCE TÌM KIẾM ---
  useEffect(() => {
    const handler = setTimeout(() => {
      setSearchQuery(searchInput);
      setPage(0); 
    }, 500);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // --- FETCH SECURITY ALERTS ---
  useEffect(() => {
    const fetchAlerts = async () => {
      if (!classId || classId.startsWith('mock-')) return;
      try {
         const res = await classApi.getFraudIncidents(classId, { size: 3, sortDir: 'DESC' });
         const realAlerts = (res.items || []).map((incident, index) => ({
             id: incident.id,
             title: incident.title || 'Cảnh báo hệ thống',
             desc: incident.description || 'Phát hiện dấu hiệu bất thường',
             icon: incident.type?.includes('LOCATION') ? <Crosshair size={18} className="text-amber-500" /> : <Smartphone size={18} className="text-red-500" />,
             bgColor: incident.severity === 'HIGH' ? 'bg-red-50' : 'bg-amber-50',
             borderColor: incident.severity === 'HIGH' ? 'border-red-200' : 'border-amber-200'
         }));
         if (realAlerts.length > 0) {
            setSecurityAlerts(realAlerts);
         }
      } catch (err) {
         console.error("Lỗi lấy cảnh báo bảo mật:", err);
      }
    };
    fetchAlerts();
  }, [classId]);

  // --- HÀM LOAD DỮ LIỆU ---
  const fetchStudents = async (showLoadingUI = false) => {
    if (showLoadingUI) setIsInitialLoading(true);
    else setIsPolling(true);

    try {
      let apiStudents = [];
      let apiTotal = 0;
      let apiTotalPages = 1;
      
      const isMockClass = String(classId).startsWith('mock-');
      if (!isMockClass) {
        const [res, policyRes] = await Promise.all([
          classApi.getClassMembers(classId, { 
            page, 
            size, 
            q: searchQuery
          }),
          classApi.getStudentsAttendancePolicy(classId).catch(() => null)
        ]);
        
        apiTotal = res?.totalElements || 0;
        apiTotalPages = res?.totalPages || 1;
        
        // Map policy data
        const policyItems = policyRes?.items || [];
        const policyMap = new Map();
        policyItems.forEach(p => {
           policyMap.set(p.userId, p);
        });

        apiStudents = (res?.items || [])
          .filter(member => member.role !== 'LECTURER' && member.role !== 'OWNER') // Lọc giảng viên / chủ phòng ra khỏi danh sách
          .map(member => {
          const policy = policyMap.get(member.userId || member.id);
          const rate = policy ? Math.round(policy.attendanceRate) : undefined;
          
          let attendanceStatus = 'Chưa cập nhật';
          if (rate !== undefined) {
             if (rate >= 90) attendanceStatus = 'Good';
             else if (rate >= 75) attendanceStatus = 'Flagged';
             else attendanceStatus = 'At-Risk';
          }
          
          return {
            id: member.userCode || member.studentCode || 'N/A',
            rawId: member.userId || member.id,
            name: member.fullName || 'N/A',
            email: member.email || 'N/A',
            avatar: member.avatarUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(member.fullName || 'U')}&background=random`,
            joinedAt: member.joinedAt,
            attendanceRate: rate,
            lastSeen: undefined,      
            memberStatus: member.memberStatus || 'N/A',
            attendanceStatus: attendanceStatus,
            role: member.role
          };
        });
      }

      let finalMock = isMockClass ? MOCK_STUDENTS : [];
      if (isMockClass && searchQuery) {
        const query = searchQuery.toLowerCase();
        finalMock = MOCK_STUDENTS.filter(s => 
          s.name.toLowerCase().includes(query) || 
          s.id.toLowerCase().includes(query) ||
          (s.email && s.email.toLowerCase().includes(query))
        );
      }

      let combined = isMockClass ? finalMock : apiStudents;
      
      if (filterAttendance !== 'ALL') {
         combined = combined.filter(s => s.attendanceStatus === filterAttendance);
      }
      if (filterMember !== 'ALL') {
         combined = combined.filter(s => s.memberStatus === filterMember);
      }

      setStudents(combined);
      setTotalPages(isMockClass ? (Math.ceil(combined.length / size) || 1) : apiTotalPages);
      setTotalElements(isMockClass ? combined.length : apiTotal);
      
    } catch (error) {
      console.error("Lỗi lấy danh sách sinh viên:", error);
      setStudents([]);
      setTotalElements(0);
      setTotalPages(1);
    } finally {
      setIsInitialLoading(false);
      setIsPolling(false);
    }
  };

  useEffect(() => {
    if (classId) fetchStudents(true); 
  }, [classId, page, size, searchQuery, filterAttendance, filterMember]);

  useEffect(() => {
    if (!classId || classId.startsWith('mock-')) return;
    const pollingInterval = setInterval(() => fetchStudents(false), 10000);
    return () => clearInterval(pollingInterval);
  }, [classId, page, size, searchQuery, filterAttendance, filterMember]);

  const renderProgressBar = (rate) => {
    if (rate === undefined) 
      return <span className="text-xs text-gray-400 font-medium italic">Chưa cập nhật</span>;
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
    if (status === 'Chưa cập nhật' || status === 'N/A' || !status) return <span className="text-xs text-gray-400 font-medium italic">Chưa cập nhật</span>;
    const styles = {
      'Good': 'bg-emerald-50 text-emerald-600',
      'At-Risk': 'bg-red-50 text-red-600',
      'Flagged': 'bg-amber-50 text-amber-600',
    };
    const labels = {
      'Good': 'Tốt',
      'At-Risk': 'Nguy cơ',
      'Flagged': 'Nghi vấn',
    };
    return (
      <span className={`px-2.5 py-1 rounded-md text-[12px] font-semibold ${styles[status] || 'bg-gray-50 text-gray-600'}`}>
        {labels[status] || status}
      </span>
    );
  };

  const renderMemberStatusBadge = (status) => {
    if (!status || status === 'N/A') return <span className="text-xs text-gray-400 font-medium italic">N/A</span>;
    const styles = {
      'ACTIVE': 'bg-blue-50 text-blue-600',
      'PENDING': 'bg-amber-50 text-amber-600',
      'INACTIVE': 'bg-gray-50 text-gray-600',
      'REJECTED': 'bg-red-50 text-red-600',
    };
    const labels = {
      'ACTIVE': 'Đã tham gia',
      'PENDING': 'Chờ duyệt',
      'INACTIVE': 'Vô hiệu hóa',
      'REJECTED': 'Từ chối',
    };
    return (
      <span className={`px-2.5 py-1 rounded-md text-[12px] font-semibold ${styles[status] || 'bg-gray-50 text-gray-600'}`}>
        {labels[status] || status}
      </span>
    );
  };

  return (
    <div className="flex flex-col xl:flex-row gap-6 items-start animate-in fade-in duration-500 w-full">
      <div className="w-full xl:flex-1 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden relative">
        
        {isPolling && (
          <div className="absolute top-0 left-0 w-full h-1 bg-gray-100 overflow-hidden z-10">
            <div className="w-full h-full bg-red-500 origin-left animate-pulse"></div>
          </div>
        )}

        <div className="p-4 border-b border-gray-200 flex justify-between items-center mt-1">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
            <input 
              type="text" 
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Tìm tên hoặc MSSV..." 
              className="pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm w-64 focus:ring-2 focus:ring-red-100 focus:border-red-300 outline-none transition-all"
            />
          </div>
          <div className="flex items-center gap-3">
            <span className="text-xs text-gray-500 font-bold bg-gray-100 px-2.5 py-1.5 rounded-lg border border-gray-200">
              Sĩ số: {totalElements} SV
            </span>
            <select 
              value={filterMember} 
              onChange={(e) => setFilterMember(e.target.value)}
              className="border border-gray-200 rounded-lg text-sm px-3 py-2 outline-none focus:border-red-300 text-gray-700 font-medium cursor-pointer bg-white">
              <option value="ALL">Tham gia lớp</option>
              <option value="ACTIVE">Đã tham gia</option>
              <option value="PENDING">Chờ duyệt</option>
            </select>
            <select 
              value={filterAttendance} 
              onChange={(e) => setFilterAttendance(e.target.value)}
              className="border border-gray-200 rounded-lg text-sm px-3 py-2 outline-none focus:border-red-300 text-gray-700 font-medium cursor-pointer bg-white">
              <option value="ALL">Điểm danh</option>
              <option value="Good">Tốt</option>
              <option value="Flagged">Nghi vấn</option>
              <option value="At-Risk">Nguy cơ</option>
            </select>
            <button onClick={handleExportFile} className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors">
              <Download size={16} /> Xuất file
            </button>
          </div>
        </div>

        <div className="overflow-x-auto min-h-[300px]">
          {isInitialLoading ? (
            <div className="animate-fade-in-up">
              <table className="min-w-[850px] w-full text-left text-sm">
                <thead className="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200">
                  <tr>
                    <th className="p-4 w-10 text-center text-[10px] text-gray-400">STT</th>
                    <th className="p-4 w-12 text-center"><input type="checkbox" disabled className="rounded border-gray-300 opacity-50" /></th>
                    <th className="p-4 whitespace-nowrap">Sinh viên</th>
                    <th className="p-4 whitespace-nowrap">Tỷ lệ điểm danh</th>
                    <th className="p-4 whitespace-nowrap">Ngày tham gia</th>
                    <th className="p-4 whitespace-nowrap">Lần cuối thấy</th>
                    <th className="p-4 whitespace-nowrap">Tham gia lớp</th>
                    <th className="p-4 whitespace-nowrap">Điểm danh</th>
                    <th className="p-4 whitespace-nowrap text-center">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {[...Array(5)].map((_, index) => (
                    <tr key={`sk-${index}`} className="hover:bg-gray-50/50">
                      <td className="p-4 text-center"><div className="w-5 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4 text-center"><div className="w-4 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-gray-200 shimmer-loader shrink-0" />
                          <div className="space-y-2">
                            <div className="w-32 h-4 bg-gray-200 rounded shimmer-loader" />
                            <div className="w-24 h-3 bg-gray-200 rounded shimmer-loader" />
                          </div>
                        </div>
                      </td>
                      <td className="p-4"><div className="w-16 h-4 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-20 h-6 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-16 h-6 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4 text-center"><div className="w-8 h-8 bg-gray-200 rounded-full shimmer-loader mx-auto" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : students.length === 0 ? (
            <div className="flex justify-center items-center h-48 text-gray-500 font-medium">Không tìm thấy sinh viên nào.</div>
          ) : (
            <table className="min-w-[850px] w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200">
                <tr>
                  <th className="p-4 w-10 text-center text-[10px] text-gray-400">STT</th>
                  <th className="p-4 w-12 text-center"><input type="checkbox" className="rounded border-gray-300 text-red-600 focus:ring-red-500" /></th>
                  <th className="p-4 whitespace-nowrap">Sinh viên</th>
                  <th className="p-4 whitespace-nowrap">Tỷ lệ điểm danh</th>
                  <th className="p-4 whitespace-nowrap">Ngày tham gia</th>
                  <th className="p-4 whitespace-nowrap">Lần cuối thấy</th>
                  <th className="p-4 whitespace-nowrap">Tham gia lớp</th>
                  <th className="p-4 whitespace-nowrap">Điểm danh</th>
                  <th className="p-4 whitespace-nowrap text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {students.map((student, idx) => (
                  <tr key={idx} className="hover:bg-gray-50 transition-colors">
                    <td className="p-4 text-center text-xs text-gray-400 font-semibold">{page * size + idx + 1}</td>
                    <td className="p-4 text-center"><input type="checkbox" className="rounded border-gray-300 text-red-600 focus:ring-red-500" /></td>
                    
                    <td className="p-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <img src={student.avatar} alt={student.name} className="w-9 h-9 rounded-full bg-gray-200 object-cover border border-gray-200 shrink-0 relative z-10 shadow-sm" />
                        <div className="flex flex-col">
                          <span className="font-bold text-gray-900 whitespace-nowrap">{student.name}</span>
                          <span className="text-[12px] text-gray-500 font-medium mt-0.5 whitespace-nowrap">{student.email}</span>
                          <span className="text-[11px] text-[#dc2626] font-bold mt-0.5 whitespace-nowrap">MSSV: {student.id}</span>
                        </div>
                      </div>
                    </td>

                    <td className="p-4 whitespace-nowrap">{renderProgressBar(student.attendanceRate)}</td>
                    <td className="p-4 text-gray-500 whitespace-nowrap">
                      {formatDate(student.joinedAt) || <span className="text-xs italic text-gray-400">Chưa cập nhật</span>}
                    </td>
                    <td className="p-4 text-gray-500 whitespace-nowrap">
                      {student.lastSeen || <span className="text-xs italic text-gray-400">Chưa cập nhật</span>}
                    </td>
                    <td className="p-4 whitespace-nowrap">{renderMemberStatusBadge(student.memberStatus)}</td>
                    <td className="p-4 whitespace-nowrap">{renderStatusBadge(student.attendanceStatus)}</td>
                    <td className="p-4 text-center whitespace-nowrap">
                      <button 
                        onClick={() => handleDeleteStudent(student)}
                        className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                        title="Xóa sinh viên khỏi lớp"
                      >
                        <Trash2 size={15} />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {!isInitialLoading && students.length > 0 && (
          <div className="p-4 border-t border-gray-200 flex justify-between items-center text-sm text-gray-500">
            <span>Hiển thị {Math.min(page * size + 1, totalElements)} đến {Math.min((page + 1) * size, totalElements)} trong tổng số {totalElements} mục</span>
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
      <div className="w-full xl:w-[320px] shrink-0 bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
        <div className="p-5 border-b border-gray-200 flex justify-between items-center">
          <h3 className="font-bold text-gray-900 flex items-center gap-2">
            <ShieldAlert size={18} className="text-red-600" /> Cảnh báo bảo mật
          </h3>
          <span className="text-[11px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded">{securityAlerts.length} Mới</span>
        </div>
        
        <div className="p-4 space-y-4 flex-1">
          {securityAlerts.map((alert) => (
            <div key={alert.id} className={`p-4 rounded-xl border ${alert.borderColor} ${alert.bgColor}`}>
              <div className="flex gap-3">
                <div className="mt-0.5 shrink-0">{alert.icon}</div>
                <div>
                  <h4 className="text-[14px] font-bold text-gray-900 leading-tight mb-1">{alert.title}</h4>
                  <p className="text-[13px] text-gray-600 mb-3 leading-snug">{alert.desc}</p>
                  <div className="flex gap-3 text-[13px] font-semibold">
                    <button className="text-red-600 hover:text-red-800 transition-colors">Xem xét</button>
                    <button className="text-gray-500 hover:text-gray-700 transition-colors">Bỏ qua</button>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="p-4 border-t border-gray-200 text-center">
          <button className="text-sm font-bold text-red-600 hover:text-red-800 transition-colors flex items-center justify-center gap-1 w-full">
            Xem tất cả nhật ký bảo mật <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {/* DETAILED DELETE CONFIRMATION MODAL WITH PREMIUM UI */}
      {showDeleteModal && studentToDelete && (
        <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm z-[999] flex items-center justify-center animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-gray-100 transform transition-all scale-in animate-in zoom-in-95 duration-200">
            <div className="flex flex-col items-center text-center">
              <div className="w-14 h-14 rounded-full bg-red-50 border border-red-100 flex items-center justify-center text-red-600 mb-4 animate-bounce">
                <Trash2 size={24} />
              </div>
              <h3 className="text-lg font-bold text-gray-900 mb-2">Xác nhận xóa sinh viên</h3>
              <p className="text-sm text-gray-600 leading-relaxed mb-6">
                Bạn có chắc chắn muốn xóa sinh viên <strong className="text-gray-900">{studentToDelete.name}</strong> (MSSV: <span className="font-mono font-bold text-red-600">{studentToDelete.id}</span>) ra khỏi lớp học này? 
                <br />
                <span className="text-[12px] text-red-500 font-medium mt-2 block">⚠️ Lưu ý: Thao tác này không thể hoàn tác và mọi dữ liệu liên quan sẽ bị xóa!</span>
              </p>
              
              <div className="flex gap-3 w-full">
                <button
                  onClick={() => {
                    setShowDeleteModal(false);
                    setStudentToDelete(null);
                  }}
                  disabled={isDeleting}
                  className="flex-1 px-4 py-2.5 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 text-gray-700 font-semibold rounded-xl text-sm transition-all active:scale-[0.98]"
                >
                  Hủy bỏ
                </button>
                <button
                  onClick={handleConfirmDelete}
                  disabled={isDeleting}
                  className="flex-1 px-4 py-2.5 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-700 hover:to-red-600 disabled:opacity-50 text-white font-semibold rounded-xl text-sm shadow-md shadow-red-200 transition-all active:scale-[0.98] flex items-center justify-center gap-1.5"
                >
                  {isDeleting ? (
                    <>
                      <Loader2 className="animate-spin" size={16} /> Đang xóa...
                    </>
                  ) : (
                    'Xác nhận xóa'
                  )}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}


