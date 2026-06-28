import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, Download, Mail, ChevronLeft, ChevronRight, ShieldAlert, 
  Smartphone, Crosshair, Loader2, Trash2, Upload, AlertTriangle,
  FileText, CheckCircle2, XCircle, AlertCircle, Info, Sparkles,
  Ban, Bell, Users, UserX, BarChart3, TrendingDown, Send, RefreshCw, Eye, CalendarClock
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

  // --- STATE CHÍNH SÁCH ĐIỂM DANH ---
  const [attendancePolicy, setAttendancePolicy] = useState(null);

  // --- STATE TỔNG SỐ BUỔI HỌC CỦA LỚP ---
  const [totalClassSessions, setTotalClassSessions] = useState(null);

  // --- STATE SỐ BUỔI HỌC KẾ HOẠCH (thiết lập lúc tạo lớp) ---
  const [plannedSessions, setPlannedSessions] = useState(null);

  // --- STATE MODAL CHI TIẾT SINH VIÊN ---
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [selectedStudentForDetail, setSelectedStudentForDetail] = useState(null);
  const [studentDetailsLoading, setStudentDetailsLoading] = useState(false);
  const [studentAttendanceDetails, setStudentAttendanceDetails] = useState([]);

  const handleViewDetail = async (student) => {
    setSelectedStudentForDetail(student);
    setIsDetailModalOpen(true);
    setStudentDetailsLoading(true);
    try {
      const sessions = await classApi.getGroupSessions(classId);
      if (!sessions || sessions.length === 0) {
        setStudentAttendanceDetails([]);
        return;
      }
      
      const details = await Promise.all(
        sessions.map(async (sess) => {
          try {
            const events = await classApi.getAttendanceEvents(sess.id, 500);
            const studentEvent = events.find(e => e.userId === student.rawId);
            return {
              session: sess,
              event: studentEvent || null
            };
          } catch (err) {
            console.error("Lỗi lấy event của phiên:", sess.id, err);
            return { session: sess, event: null };
          }
        })
      );
      
      details.sort((a, b) => new Date(b.session.startTime || 0) - new Date(a.session.startTime || 0));
      setStudentAttendanceDetails(details);
    } catch (err) {
      console.error("Lỗi tải chi tiết điểm danh:", err);
      toast.error("Không thể tải chi tiết điểm danh.");
    } finally {
      setStudentDetailsLoading(false);
    }
  };
  
  // --- STATE CẢNH BÁO BẢO MẬT ---
  const [securityAlerts, setSecurityAlerts] = useState(ALERTS_DATA);

  // --- STATE XÓA THÀNH VIÊN LỚP HỌC ---
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [studentToDelete, setStudentToDelete] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // --- STATES & HANDLERS CHO IMPORT WIZARD (WIZARD 3 BƯỚC) ---
  const [showImportWizard, setShowImportWizard] = useState(false);
  const [importStep, setImportStep] = useState(1);
  const [selectedFile, setSelectedFile] = useState(null);
  const [validatedRows, setValidatedRows] = useState([]);
  const [isValidating, setIsValidating] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importSummary, setImportSummary] = useState(null);
  const [hasCriticalConflict, setHasCriticalConflict] = useState(false);

  const MOCK_IMPORT_PAYLOAD = [
    { rowIndex: 0, studentCode: "N22DCCN160", fullName: "Phạm Văn Phú", email: "phu.pvn22@ptit.edu.vn" },
    { rowIndex: 1, studentCode: "N22DCCN001", fullName: "Nguyễn Văn A", email: "a.nguyen@gmail.com" },
    { rowIndex: 2, studentCode: "N22DCCN002", fullName: "Trần Thị B", email: "b.tran@gmail.com" },
    { rowIndex: 3, studentCode: "STU-8921", fullName: "Lê Thị Hồng", email: "hong.lt@university.edu" }, // Đã là member -> SKIPPED (Rule 11)
    { rowIndex: 4, studentCode: "STU-1213", fullName: "Lâm Nhật Vượng", email: "vuong.ln@university.edu" }, // Từng bị REJECTED -> RESTORED (Rule 12)
    { rowIndex: 5, studentCode: "", fullName: "Trần Văn Thiếu MSV", email: "thieu.msv@gmail.com" }, // Lỗi: Thiếu MSV
    { rowIndex: 6, studentCode: "N22DCCN999", fullName: "Lâm Nhật Lỗi Email", email: "email_sai_dinh_dang" }, // Lỗi: Sai định dạng Email
    { rowIndex: 7, studentCode: "N22DCCN001", fullName: "Nguyễn Văn A (Dòng trùng)", email: "a.nguyen@gmail.com" }, // Lỗi: Trùng trong file (Rule 3)
    { rowIndex: 8, studentCode: "STU-7743", fullName: "Xung Đột Chéo", email: "manh.nv@university.edu" } // Lỗi: MSSV match Khoa, email match Mạnh -> Conflict (Rule 6)
  ];

  const simulateDryRun = (payload) => {
    const seenCodes = new Map();
    const seenEmails = new Map();
    
    // Tạo cơ sở dữ liệu giả lập hệ thống
    const mockSystemUsers = [
      { studentCode: "N22DCCN160", email: "phu.pvn22@ptit.edu.vn", fullName: "Phạm Văn Phú" },
      { studentCode: "N22DCCN001", email: "a.nguyen@gmail.com", fullName: "Nguyễn Văn A" },
      { studentCode: "N22DCCN002", email: "b.tran@gmail.com", fullName: "Trần Thị B" },
      { studentCode: "N22DCCN003", email: "c.le@gmail.com", fullName: "Lê Văn C" },
      ...students.map(s => ({
        studentCode: s.id,
        email: s.email,
        fullName: s.name,
        memberStatus: s.memberStatus
      }))
    ];

    const userByCodeMap = new Map();
    const userByEmailMap = new Map();
    mockSystemUsers.forEach(u => {
      if (u.studentCode) userByCodeMap.set(u.studentCode.trim().toUpperCase(), u);
      if (u.email) userByEmailMap.set(u.email.trim().toLowerCase(), u);
    });

    let localConflict = false;
    const fileDuplicates = new Set();

    // 1. Quét tìm phần tử trùng lặp trong file (Rule 3)
    payload.forEach((row, idx) => {
      const code = (row.studentCode || '').trim().toUpperCase();
      const email = (row.email || '').trim().toLowerCase();

      if (code) {
        if (seenCodes.has(code)) {
          fileDuplicates.add(idx);
          fileDuplicates.add(seenCodes.get(code));
        } else {
          seenCodes.set(code, idx);
        }
      }
      if (email) {
        if (seenEmails.has(email)) {
          fileDuplicates.add(idx);
          fileDuplicates.add(seenEmails.get(email));
        } else {
          seenEmails.set(email, idx);
        }
      }
    });

    // 2. Chạy logic đối chiếu chi tiết từng dòng
    const resultItems = payload.map((row, idx) => {
      const code = (row.studentCode || '').trim().toUpperCase();
      const email = (row.email || '').trim().toLowerCase();
      const fullName = (row.fullName || row.name || '').trim();

      if (fileDuplicates.has(idx)) {
        return {
          ...row,
          rowIndex: idx,
          studentCode: code,
          fullName: fullName,
          email: email,
          action: 'ERROR',
          errorMsg: 'Trùng lặp mã sinh viên hoặc email trong tệp tải lên'
        };
      }

      if (!code && !email) {
        return {
          ...row,
          rowIndex: idx,
          studentCode: code,
          fullName: fullName,
          email: email,
          action: 'ERROR',
          errorMsg: 'Mã sinh viên hoặc email không được trống'
        };
      }

      if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return {
          ...row,
          rowIndex: idx,
          studentCode: code,
          fullName: fullName,
          email: email,
          action: 'ERROR',
          errorMsg: 'Email không đúng định dạng'
        };
      }

      // Rule 4: Match theo studentCode trước
      let matchedUser = null;
      let matchedByCode = false;
      let matchedByEmail = false;

      if (code && userByCodeMap.has(code)) {
        matchedUser = userByCodeMap.get(code);
        matchedByCode = true;
      }

      // Rule 5: Match theo email sau (nếu không khớp MSSV)
      let userByEmailMatch = null;
      if (email && userByEmailMap.has(email)) {
        userByEmailMatch = userByEmailMap.get(email);
        matchedByEmail = true;
      }

      // Rule 6: Trùng chéo (Code matches User A, Email matches User B) => reject toàn bộ
      if (matchedByCode && matchedByEmail && matchedUser.studentCode !== userByEmailMatch.studentCode) {
        localConflict = true;
        return {
          ...row,
          rowIndex: idx,
          studentCode: code,
          fullName: fullName,
          email: email,
          action: 'ERROR',
          errorMsg: `Xung đột chéo: MSSV khớp với "${matchedUser.fullName}" nhưng Email khớp với "${userByEmailMatch.fullName}"`
        };
      }

      if (!matchedUser && matchedByEmail) {
        matchedUser = userByEmailMatch;
      }

      if (matchedUser) {
        // Kiểm tra xem đã là thành viên trong lớp chưa
        const inClass = students.find(s => s.id === matchedUser.studentCode || s.email === matchedUser.email);
        if (inClass) {
          const status = inClass.memberStatus;
          if (status === 'ACTIVE' || status === 'APPROVED') {
            // Rule 11: Đã là member => skip
            return {
              ...row,
              rowIndex: idx,
              studentCode: code || matchedUser.studentCode,
              fullName: fullName || matchedUser.fullName,
              email: email || matchedUser.email,
              userId: matchedUser.studentCode,
              memberStatus: status,
              accountStatus: 'ACTIVE',
              action: 'SKIPPED_EXISTING_MEMBER',
              errorMsg: null
            };
          } else {
            // Rule 12: Từng bị REMOVED/REJECTED => restore
            return {
              ...row,
              rowIndex: idx,
              studentCode: code || matchedUser.studentCode,
              fullName: fullName || matchedUser.fullName,
              email: email || matchedUser.email,
              userId: matchedUser.studentCode,
              memberStatus: 'APPROVED',
              accountStatus: 'ACTIVE',
              action: 'RESTORED_MEMBER',
              errorMsg: null
            };
          }
        }

        // Rule 7: Đã có tài khoản hệ thống nhưng chưa vào lớp => Thêm
        return {
          ...row,
          rowIndex: idx,
          studentCode: code || matchedUser.studentCode,
          fullName: fullName || matchedUser.fullName,
          email: email || matchedUser.email,
          userId: matchedUser.studentCode,
          memberStatus: 'APPROVED',
          accountStatus: 'ACTIVE',
          action: 'LINKED_EXISTING_USER_AND_ADDED',
          errorMsg: null
        };
      }

      // Rule 8 & 9 & 10: Chưa có tài khoản => tạo mới (Ghost Account)
      const cleanName = fullName.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\s+/g, '');
      const defaultPassword = `${code || 'GUEST'}${cleanName}`;
      return {
        ...row,
        rowIndex: idx,
        studentCode: code,
        fullName: fullName,
        email: email,
        userId: code || `NEW-${idx}`,
        memberStatus: 'APPROVED',
        accountStatus: 'ACTIVE',
        action: 'CREATED_USER_AND_ADDED',
        defaultPasswordRule: defaultPassword,
        errorMsg: null
      };
    });

    return {
      items: resultItems,
      hasCriticalConflict: localConflict,
      totalRows: payload.length,
      createdUsers: resultItems.filter(r => r.action === 'CREATED_USER_AND_ADDED').length,
      linkedExistingUsers: resultItems.filter(r => r.action === 'LINKED_EXISTING_USER_AND_ADDED').length,
      addedMembers: resultItems.filter(r => r.action === 'LINKED_EXISTING_USER_AND_ADDED' || r.action === 'CREATED_USER_AND_ADDED' || r.action === 'RESTORED_MEMBER').length,
      skippedExistingMembers: resultItems.filter(r => r.action === 'SKIPPED_EXISTING_MEMBER').length,
      restoredMembers: resultItems.filter(r => r.action === 'RESTORED_MEMBER').length,
      invitationEmailsQueued: resultItems.filter(r => r.action === 'CREATED_USER_AND_ADDED').length
    };
  };

  // Trình xử lý tải file Excel / CSV giả lập
  // Trình xử lý tải và phân tích file CSV thực tế
  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    setSelectedFile({
      name: file.name,
      size: `${(file.size / 1024).toFixed(1)} KB`
    });

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const text = event.target.result;
        const lines = text.split(/\r?\n/);
        if (lines.length < 2) {
          toast.error("File tải lên không có dữ liệu!");
          return;
        }

        // Tự động nhận diện phân tách (dấu phẩy, dấu chấm phẩy hoặc tab)
        const firstLine = lines[0];
        let separator = ',';
        if (firstLine.includes(';')) {
          separator = ';';
        } else if (firstLine.includes('\t')) {
          separator = '\t';
        }

        // Trích xuất tiêu đề và làm sạch khoảng trắng
        const headers = firstLine.split(separator).map(h => h.trim().replace(/^["']|["']$/g, ''));
        
        const parsedRows = [];
        let rIndex = 0;
        for (let i = 1; i < lines.length; i++) {
          const line = lines[i].trim();
          if (!line) continue;

          // Tách cột và tôn trọng dấu ngoặc kép
          const columns = [];
          let currentColumn = '';
          let inQuotes = false;
          for (let charIdx = 0; charIdx < line.length; charIdx++) {
            const char = line[charIdx];
            if (char === '"' || char === "'") {
              inQuotes = !inQuotes;
            } else if (char === separator && !inQuotes) {
              columns.push(currentColumn.trim().replace(/^["']|["']$/g, ''));
              currentColumn = '';
            } else {
              currentColumn += char;
            }
          }
          columns.push(currentColumn.trim().replace(/^["']|["']$/g, ''));

          // Ánh xạ cột tiêu đề thông minh
          const rowData = {};
          headers.forEach((header, colIdx) => {
            const val = columns[colIdx] || '';
            const normHeader = header.toLowerCase()
              .normalize("NFD")
              .replace(/[\u0300-\u036f]/g, "")
              .replace(/\s+/g, '')
              .replace(/[^a-z0-9]/g, '');

            if (normHeader.includes('code') || normHeader.includes('msv') || normHeader.includes('masinhvien') || normHeader.includes('masv')) {
              rowData.studentCode = val;
            } else if (normHeader.includes('name') || normHeader.includes('hoten') || normHeader.includes('hovaten') || normHeader.includes('tensinhvien') || normHeader.includes('ten')) {
              rowData.name = val;
            } else if (normHeader.includes('email')) {
              rowData.email = val;
            }
          });

          if (rowData.studentCode || rowData.name || rowData.email) {
            parsedRows.push({
              rowIndex: rIndex++,
              studentCode: rowData.studentCode || '',
              fullName: rowData.name || '',
              email: rowData.email || ''
            });
          }
        }

        if (parsedRows.length === 0) {
          toast.error("Không tìm thấy dữ liệu hợp lệ trong file!");
          return;
        }

        handleStartValidation(parsedRows);
      } catch (err) {
        console.error("Lỗi phân tích file:", err);
        toast.error("Không thể đọc tệp CSV này. Hãy lưu tệp Excel của bạn dưới dạng CSV UTF-8 để tải lên!");
      }
    };

    reader.onerror = () => {
      toast.error("Lỗi đọc file từ thiết bị!");
    };

    reader.readAsText(file, "UTF-8");
  };

  // Trình xử lý nạp danh sách mẫu nhanh để demo
  const handleLoadSampleData = () => {
    setSelectedFile({
      name: "Danh_sach_sinh_vien_mau.xlsx",
      size: "18.5 KB"
    });
    handleStartValidation(MOCK_IMPORT_PAYLOAD);
  };

  // Bước 2: Gọi API Chạy thử (Dry-Run / Validate-Import)
  const handleStartValidation = async (payload) => {
    setIsValidating(true);
    setImportStep(2);
    
    // Giả lập độ trễ mạng để tạo cảm giác thực tế và cao cấp cho người dùng
    await new Promise(resolve => setTimeout(resolve, 1500));
    
    try {
      if (classId && !classId.startsWith('mock-')) {
        try {
          const res = await classApi.validateImportMembers(classId, payload);
          const items = res?.items || (Array.isArray(res) ? res : null);
          if (items && items.length > 0) {
            // Kiểm tra xem backend đã phân loại (action hoặc status) cho các phần tử chưa
            const hasClassification = items.some(item => item.action || item.status);
            if (hasClassification) {
              setValidatedRows(items);
              setImportSummary(res?.items ? res : null);
              setHasCriticalConflict(!!res?.hasCriticalConflict || items.some(item => item.action === 'ERROR' && item.errorMsg?.includes('Xung đột')));
              setIsValidating(false);
              return;
            } else {
              console.warn("Backend trả về danh sách thô không có phân loại action/status. Tự động chuyển sang Client-side Simulator.");
            }
          }
        } catch (apiErr) {
          console.warn("API validate-import failed or not implemented, fallback to local simulator:", apiErr);
        }
      }
      
      // Fallback tự động sang Client-side Simulator
      const simulatedResult = simulateDryRun(payload);
      setValidatedRows(simulatedResult.items);
      setImportSummary(simulatedResult);
      setHasCriticalConflict(simulatedResult.hasCriticalConflict);
    } catch (err) {
      console.error("Lỗi validate import:", err);
      toast.error("Lỗi phân tích dữ liệu kiểm tra.");
    } finally {
      setIsValidating(false);
    }
  };

  // Bước 3: Xác nhận thực thi Import chính thức
  const handleConfirmImport = async () => {
    // Chỉ lấy dòng EXISTING + NEW + RESTORED để import, loại bỏ hoàn toàn các dòng ERROR
    const validRows = validatedRows.filter(row => {
      const action = row.action;
      const status = row.status;
      return (
        action === 'CREATED_USER_AND_ADDED' ||
        action === 'LINKED_EXISTING_USER_AND_ADDED' ||
        action === 'RESTORED_MEMBER' ||
        status === 'NEW' ||
        status === 'EXISTING'
      );
    });

    const validPayload = validRows.map(row => ({
      rowIndex: row.rowIndex !== undefined ? row.rowIndex : 0,
      studentCode: row.studentCode || '',
      fullName: row.fullName || row.name || '',
      email: row.email || ''
    }));

    if (validPayload.length === 0) {
      toast.error("Không có sinh viên hợp lệ để Import!");
      return;
    }

    setIsImporting(true);
    const loadingToast = toast.loading(`Đang tiến hành nhập ${validPayload.length} sinh viên vào lớp...`);

    // Giả lập độ trễ mạng để hiển thị tiến trình loading
    await new Promise(resolve => setTimeout(resolve, 1800));

    try {
      let isMockSuccess = true;
      if (classId && !classId.startsWith('mock-')) {
        try {
          const res = await classApi.importMembers(classId, validPayload);
          isMockSuccess = false;
          setImportSummary(res);
        } catch (apiErr) {
          console.warn("API import failed or not implemented, fallback to local mockup simulator:", apiErr);
        }
      }

      if (isMockSuccess) {
        // Fallback: Tự động thêm các sinh viên mới/cũ vào state để hiển thị ngay lập tức
        const newMembersMapped = validPayload.map(row => ({
          id: row.studentCode,
          rawId: `STU-${Math.floor(Math.random() * 10000)}`,
          name: row.fullName,
          email: row.email,
          avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(row.fullName)}&background=random`,
          joinedAt: new Date().toISOString(),
          attendanceRate: 100,
          memberStatus: 'ACTIVE',
          attendanceStatus: 'Good',
          role: 'STUDENT'
        }));

        setStudents(prev => {
          // Tránh bị trùng mã sinh viên
          const filteredPrev = prev.filter(s => !newMembersMapped.some(nm => nm.id === s.id));
          return [...newMembersMapped, ...filteredPrev];
        });
        
        setTotalElements(prev => prev + newMembersMapped.length);
      }

      toast.success(`Nhập thành công ${validPayload.length} sinh viên vào lớp!`, { id: loadingToast });
      setImportStep(3);
      fetchStudents(false);
    } catch (err) {
      console.error("Lỗi thực thi import:", err);
      toast.error(err.message || "Lỗi khi nhập thành viên vào lớp!", { id: loadingToast });
    } finally {
      setIsImporting(false);
    }
  };

  const handleCloseImportWizard = () => {
    setShowImportWizard(false);
    setImportStep(1);
    setSelectedFile(null);
    setValidatedRows([]);
    setImportSummary(null);
    setHasCriticalConflict(false);
  };

  // Tính toán số lượng dòng tổng kết
  const totalImport = validatedRows.length;
  const existingCount = validatedRows.filter(r => r.status === 'EXISTING' || r.action === 'LINKED_EXISTING_USER_AND_ADDED').length;
  const newCount = validatedRows.filter(r => r.status === 'NEW' || r.action === 'CREATED_USER_AND_ADDED').length;
  const skippedCount = validatedRows.filter(r => r.action === 'SKIPPED_EXISTING_MEMBER').length;
  const restoredCount = validatedRows.filter(r => r.action === 'RESTORED_MEMBER').length;
  const errorCount = validatedRows.filter(r => r.status === 'ERROR' || r.action === 'ERROR').length;
  const canConfirmImport = (existingCount + newCount + restoredCount) > 0 && !hasCriticalConflict;

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
        // Dùng API tổng hợp: trả về cả policy lẫn thống kê điểm danh từng sinh viên
        const res = await classApi.getStudentsAttendancePolicy(classId, {
          page,
          size,
          q: searchQuery || undefined,
        });

        // Lưu chính sách điểm danh chung của lớp
        if (res?.policy) {
          setAttendancePolicy(res.policy);
          // Nếu policy có tổng số buổi học theo kế hoạch
          const policyPlanned = res.policy?.totalSessions ?? res.policy?.plannedSessions ??
                                res.policy?.scheduledSessions ?? res.policy?.totalScheduledSessions;
          if (policyPlanned != null) setPlannedSessions(policyPlanned);
        }

        // Lưu số phên điểm danh đã mở (closedSessionCount từ API)
        if (res?.totalSessions != null) {
          setTotalClassSessions(res.totalSessions);
        } else if (res?.closedSessionCount != null) {
          setTotalClassSessions(res.closedSessionCount);
        } else if (res?.policy?.totalSessions != null) {
          // Policy totalSessions có thể là kế hoạch, không đưa vào opened
        }

        apiTotal = res?.totalElements || 0;
        apiTotalPages = res?.totalPages || 1;

        apiStudents = (res?.items || []).map(item => {
          const rate = item.attendanceRate != null ? Math.round(item.attendanceRate * 100) / 100 : undefined;

          // Map policyStatus từ API sang nhãn hiển thị
          const policyStatus = item.policyStatus || 'NO_DATA';
          let attendanceStatus;
          if (policyStatus === 'NO_DATA') attendanceStatus = 'Chưa cập nhật';
          else if (policyStatus === 'OK') attendanceStatus = 'Good';
          else if (policyStatus === 'WARNING') attendanceStatus = 'Flagged';
          else if (policyStatus === 'CRITICAL') attendanceStatus = 'At-Risk';
          else attendanceStatus = policyStatus;

          return {
            id: 'N/A',          // API này không trả userCode; giữ placeholder, sẽ bổ sung nếu cần
            rawId: item.userId,
            name: item.fullName || 'N/A',
            email: item.email || 'N/A',
            avatar: `https://ui-avatars.com/api/?name=${encodeURIComponent(item.fullName || 'U')}&background=random`,
            joinedAt: item.joinedAt,
            attendanceRate: rate,
            closedSessionCount: item.closedSessionCount ?? 0,
            eligibleSessionCount: item.eligibleSessionCount ?? 0,
            presentCount: item.presentCount ?? 0,
            lateCount: item.lateCount ?? 0,
            absentCount: item.absentCount ?? 0,
            excusedCount: item.excusedCount ?? 0,
            earnedAttendancePoints: item.earnedAttendancePoints,
            policyStatus,
            breachReasons: item.breachReasons || [],
            memberStatus: 'ACTIVE',   // API này không trả memberStatus; mặc định ACTIVE
            attendanceStatus,
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

      // Fallback: tính tổng buổi từ dữ liệu sinh viên (lấy max eligibleSessionCount)
      if (!isMockClass && combined.length > 0) {
        const maxEligible = Math.max(...combined.map(s => s.eligibleSessionCount ?? 0));
        if (maxEligible > 0) {
          setTotalClassSessions(prev => prev ?? maxEligible);
        }
      }
      
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

  // --- FETCH SỐ PHÊIEN ĐÃ MỠ TỪ ATTENDANCE SUMMARY + SỐ BUỔI HỌC TỪ CLASS DETAIL ---
  useEffect(() => {
    const fetchSessionData = async () => {
      if (!classId || classId.startsWith('mock-')) return;
      try {
        // 1. Lấy summary: số phiên điểm danh đã mở/đóng thực tế
        const summary = await classApi.getAttendanceSummary(classId);
        if (summary != null) {
          // closedSessionCount hoặc totalSessions trong summary = số phiên ĐD đã mở
          const opened = summary?.closedSessionCount ?? summary?.openedSessionCount ??
                         summary?.totalSessions ?? summary?.sessionCount;
          if (opened != null) setTotalClassSessions(opened);
          // Nếu summary trả cả totalPlannedSessions
          const planned = summary?.totalPlannedSessions ?? summary?.plannedSessions ??
                          summary?.scheduledSessions;
          if (planned != null) setPlannedSessions(planned);
        }
      } catch (err) {
        console.warn('fetchSummary fallback:', err?.message);
      }

      try {
        // 2. Lấy class detail: số buổi học theo kế hoạch khi tạo lớp
        const detail = await classApi.getClassDetail(classId);
        if (detail != null) {
          const planned = detail?.totalSessions ?? detail?.plannedSessions ??
                          detail?.scheduledSessions ?? detail?.totalScheduledSessions ??
                          detail?.maxSessions ?? detail?.sessionCount;
          if (planned != null) setPlannedSessions(planned);
          // Đôi khi class detail có nested policy
          const policyPlanned = detail?.policy?.totalSessions ?? detail?.policy?.plannedSessions;
          if (policyPlanned != null) setPlannedSessions(prev => prev ?? policyPlanned);
        }
      } catch (err) {
        console.warn('fetchClassDetail for plannedSessions:', err?.message);
      }
    };
    fetchSessionData();
  }, [classId]);

  const renderProgressBar = (rate) => {
    if (rate === undefined || rate === null) 
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

  // Badge dựa trên policyStatus thật từ API
  const renderStatusBadge = (student) => {
    const status = student?.attendanceStatus;
    const breachReasons = student?.breachReasons || [];
    if (!status || status === 'Chưa cập nhật' || status === 'NO_DATA')
      return <span className="text-xs text-gray-400 font-medium italic">Chưa cập nhật</span>;

    const styles = {
      'Good': 'bg-emerald-50 text-emerald-700 border border-emerald-200',
      'At-Risk': 'bg-red-50 text-red-700 border border-red-200',
      'Flagged': 'bg-amber-50 text-amber-700 border border-amber-200',
    };
    const labels = {
      'Good': '✓ Tốt',
      'At-Risk': '⚠ Nguy cơ',
      'Flagged': '⚡ Cảnh báo',
    };

    const breachLabel = {
      'RATE_BELOW_WARNING': 'TL thấp',
      'RATE_BELOW_CRITICAL': 'TL nguy hiểm',
      'ABSENT_ABOVE_WARNING': 'Vắng nhiều',
      'ABSENT_ABOVE_CRITICAL': 'Vắng nguy hiểm',
    };

    return (
      <div className="flex flex-col gap-1">
        <span className={`px-2 py-0.5 rounded-md text-[11px] font-semibold w-fit ${styles[status] || 'bg-gray-50 text-gray-600 border border-gray-200'}`}>
          {labels[status] || status}
        </span>
        {breachReasons.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {breachReasons.map(r => (
              <span key={r} className="text-[10px] bg-red-100 text-red-700 px-1.5 py-0.5 rounded font-medium">
                {breachLabel[r] || r}
              </span>
            ))}
          </div>
        )}
      </div>
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

  // --- COMPUTED STATS: Tính toán từ danh sách sinh viên hiện tại ---
  const maxAbsentAllowed = attendancePolicy?.maxAbsentSessions ?? attendancePolicy?.maxAbsentAllowed ?? 3;

  const bannedStudents = students.filter(s =>
    s.policyStatus === 'CRITICAL' ||
    s.breachReasons?.includes('ABSENT_ABOVE_CRITICAL') ||
    s.breachReasons?.includes('RATE_BELOW_CRITICAL')
  );

  const warningStudents = students.filter(s =>
    (s.policyStatus === 'WARNING' ||
    s.breachReasons?.includes('ABSENT_ABOVE_WARNING') ||
    s.breachReasons?.includes('RATE_BELOW_WARNING')) &&
    !bannedStudents.some(b => (b.rawId || b.id) === (s.rawId || s.id))
  );

  const nearBanStudents = students.filter(s => {
    const absentCount = s.absentCount ?? 0;
    const remaining = maxAbsentAllowed - absentCount;
    return remaining > 0 && remaining <= 2 &&
      s.policyStatus !== 'CRITICAL' &&
      !s.breachReasons?.includes('ABSENT_ABOVE_CRITICAL') &&
      !bannedStudents.some(b => (b.rawId || b.id) === (s.rawId || s.id));
  });

  const goodStudents = students.filter(s =>
    !bannedStudents.some(b => (b.rawId || b.id) === (s.rawId || s.id)) &&
    !warningStudents.some(w => (w.rawId || w.id) === (s.rawId || s.id)) &&
    !nearBanStudents.some(n => (n.rawId || n.id) === (s.rawId || s.id))
  );

  // Danh sách nguy cơ, deduplicated
  const atRiskIds = new Set();
  const atRiskList = [];
  [...bannedStudents, ...nearBanStudents, ...warningStudents].forEach(s => {
    const key = s.rawId || s.id;
    if (!atRiskIds.has(key)) {
      atRiskIds.add(key);
      atRiskList.push(s);
    }
  });

  const bannedOnlyList = atRiskList.filter(s => 
      s.policyStatus === 'CRITICAL' ||
      s.breachReasons?.includes('ABSENT_ABOVE_CRITICAL') ||
      s.breachReasons?.includes('RATE_BELOW_CRITICAL')
  );
  const warningOnlyList = atRiskList.filter(s => !bannedOnlyList.includes(s));

  const handleEmailWarning = async (student) => {
    const isBanned = student.policyStatus === 'CRITICAL' ||
                     student.breachReasons?.includes('ABSENT_ABOVE_CRITICAL') ||
                     student.breachReasons?.includes('RATE_BELOW_CRITICAL');
                     
    const loadingToast = toast.loading(`Đang tổng hợp dữ liệu vắng mặt của ${student.name}...`);
    try {
      let classCode = 'N/A';
      let className = 'Lớp học';
      let lecturerName = 'Giảng viên phụ trách';
      
      if (classId && !classId.startsWith('mock-')) {
         const detail = await classApi.getClassDetail(classId);
         if (detail) {
            classCode = detail.code || classId;
            className = detail.className || detail.name || 'Lớp học';
            lecturerName = detail.lecturerName || detail.instructorName || 'Giảng viên phụ trách';
         }
      }

      // Fetch sessions to find absent dates
      const absentSessionsList = [];
      if (classId && !classId.startsWith('mock-')) {
        const sessions = await classApi.getGroupSessions(classId);
        if (sessions && sessions.length > 0) {
          const details = await Promise.all(
            sessions.map(async (sess) => {
              try {
                const events = await classApi.getAttendanceEvents(sess.id, 500);
                const studentEvent = events.find(e => e.userId === student.rawId);
                return { session: sess, event: studentEvent || null };
              } catch (err) {
                return { session: sess, event: null };
              }
            })
          );
          
          details.sort((a, b) => new Date(a.session.startTime) - new Date(b.session.startTime));
          
          for (const detail of details) {
            const { session, event } = detail;
            const sessionDate = new Date(session.startTime);
            let isAbsent = false;
            let reason = '';
            
            if (event) {
              if (event.status === 'ABSENT') {
                isAbsent = true;
                reason = event.note || event.reason || '';
              }
            } else {
              const joinedAtDate = student.joinedAt ? new Date(student.joinedAt) : null;
              if (joinedAtDate && sessionDate < joinedAtDate) {
                isAbsent = true;
                reason = 'Chưa tham gia lớp học tại thời điểm này';
              } else if (session.status === 'CLOSED' || session.endTime < new Date().toISOString()) {
                 isAbsent = true;
              }
            }
            
            if (isAbsent) {
               const dateStr = sessionDate.toLocaleDateString('vi-VN');
               const startTimeStr = sessionDate.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
               const endTimeStr = new Date(session.endTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
               absentSessionsList.push(`- Ngày ${dateStr} (${startTimeStr} - ${endTimeStr}) ${reason ? `[Ghi chú: ${reason}]` : ''}`);
            }
          }
        }
      }

      let emailTitle = isBanned ? 'Thông báo: Vi phạm quy định điểm danh - CẤM THI' : 'Cảnh báo điểm danh - Nguy cơ vi phạm quy định';
      const subject = encodeURIComponent(`[${classCode}] ${emailTitle}`);
      
      let bodyText = `Kính gửi sinh viên ${student.name},\n\n`;
      if (isBanned) {
         bodyText += `Bạn đã vi phạm quy định điểm danh và hiện đang thuộc diện CẤM THI.\n\n`;
      } else {
         bodyText += `Bạn đang có nguy cơ vi phạm quy định điểm danh do số buổi vắng mặt vượt mức cho phép.\n\n`;
      }
      
      bodyText += `THÔNG TIN LỚP HỌC:\n`;
      bodyText += `- Lớp: ${className} (${classCode})\n`;
      bodyText += `- Giảng viên phụ trách: ${lecturerName}\n\n`;
      
      bodyText += `TỔNG KẾT ĐIỂM DANH:\n`;
      bodyText += `- Số buổi vắng: ${student.absentCount ?? absentSessionsList.length}/${maxAbsentAllowed} buổi\n`;
      
      if (absentSessionsList.length > 0) {
        bodyText += `- Chi tiết các buổi vắng mặt:\n`;
        bodyText += absentSessionsList.join('\n') + '\n';
      }
      
      bodyText += `\nVui lòng phản hồi email này hoặc liên hệ trực tiếp với giảng viên ngay lập tức để được hỗ trợ.\n\n`;
      bodyText += `Trân trọng,\n${lecturerName}`;
      
      const body = encodeURIComponent(bodyText);
      
      toast.dismiss(loadingToast);
      window.location.href = `mailto:${student.email}?subject=${subject}&body=${body}`;
      
    } catch (error) {
      console.error("Lỗi tạo email cảnh báo:", error);
      toast.error("Không thể tổng hợp dữ liệu, vui lòng thử lại!", { id: loadingToast });
    }
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
            {/* Nút Refresh thủ công: cập nhật ngay không cần chờ polling 10s */}
            <button
              onClick={() => fetchStudents(false)}
              disabled={isPolling || isInitialLoading}
              title="Cập nhật dữ liệu điểm danh mới nhất"
              className="flex items-center gap-1.5 px-3 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-600 hover:bg-gray-50 hover:border-gray-300 transition-colors disabled:opacity-50 shrink-0"
            >
              <RefreshCw size={14} className={isPolling ? 'animate-spin' : ''} />
              <span className="hidden sm:inline">Cập nhật</span>
            </button>
            <button 
              onClick={() => setShowImportWizard(true)} 
              className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-700 hover:to-red-600 text-white rounded-lg text-sm font-semibold transition-all shadow-sm active:scale-95 shrink-0"
            >
              <Upload size={16} /> Import Sinh viên
            </button>
            <button onClick={handleExportFile} className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors shrink-0">
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
                    <th className="p-4 whitespace-nowrap">Sinh viên</th>
                    <th className="p-4 whitespace-nowrap text-center">Có mặt</th>
                    <th className="p-4 whitespace-nowrap text-center">Trễ</th>
                    <th className="p-4 whitespace-nowrap text-center">Vắng</th>
                    <th className="p-4 whitespace-nowrap">Ngày tham gia</th>
                    <th className="p-4 whitespace-nowrap">Trạng thái</th>
                    <th className="p-4 whitespace-nowrap text-center">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {[...Array(5)].map((_, index) => (
                    <tr key={`sk-${index}`} className="hover:bg-gray-50/50">
                      <td className="p-4 text-center"><div className="w-5 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-gray-200 shimmer-loader shrink-0" />
                          <div className="space-y-2">
                            <div className="w-32 h-4 bg-gray-200 rounded shimmer-loader" />
                            <div className="w-24 h-3 bg-gray-200 rounded shimmer-loader" />
                          </div>
                        </div>
                      </td>
                      <td className="p-4"><div className="w-8 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4"><div className="w-8 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4"><div className="w-8 h-4 bg-gray-200 rounded shimmer-loader mx-auto" /></td>
                      <td className="p-4"><div className="w-24 h-4 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4"><div className="w-20 h-6 bg-gray-200 rounded shimmer-loader" /></td>
                      <td className="p-4 text-center"><div className="w-8 h-8 bg-gray-200 rounded-full shimmer-loader mx-auto" /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : students.length === 0 ? (
            <div className="flex justify-center items-center h-48 text-gray-500 font-medium">Không tìm thấy sinh viên nào.</div>
          ) : (
            <table className="min-w-[1050px] w-full text-left text-sm">
              <thead className="bg-gray-50 text-gray-500 font-semibold border-b border-gray-200">
                <tr>
                  <th className="p-4 w-10 text-center text-[10px] text-gray-400">STT</th>
                  <th className="p-4 whitespace-nowrap">Sinh viên</th>
                  <th className="p-4 whitespace-nowrap text-center">Có mặt</th>
                  
                  <th className="p-4 whitespace-nowrap text-center">Trễ</th>
                  <th className="p-4 whitespace-nowrap text-center" title="Số buổi vắng = vắng thực tế + buổi trước khi tham gia (mặc định VẮNG với SV join muộn, đánh dấu *)">
                    Vắng <span className="text-red-400 text-[10px] cursor-help" title="* bao gồm buổi trước khi tham gia (mặc định VẮNG)">*</span>
                  </th>
                  <th className="p-4 whitespace-nowrap">Ngày tham gia</th>
                  <th className="p-4 whitespace-nowrap">Trạng thái</th>
                  <th className="p-4 whitespace-nowrap text-center">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {students.map((student, idx) => {
                  const isBanned = student.policyStatus === 'CRITICAL' ||
                                   student.breachReasons?.includes('ABSENT_ABOVE_CRITICAL') ||
                                   student.breachReasons?.includes('RATE_BELOW_CRITICAL');
                  
                  const isWarning = !isBanned && (student.policyStatus === 'WARNING' ||
                                   student.breachReasons?.includes('ABSENT_ABOVE_WARNING') ||
                                   student.breachReasons?.includes('RATE_BELOW_WARNING'));
                                   
                  const absentCount = student.absentCount ?? 0;
                  const maxAbsentAllowed = attendancePolicy?.maxAbsentSessions ?? attendancePolicy?.maxAbsentAllowed ?? 3;
                  const remaining = maxAbsentAllowed - absentCount;
                  const isNearBan = !isBanned && remaining > 0 && remaining <= 2;
                  
                  const isAtRisk = isWarning || isNearBan;
                  
                  let rowBgClass = "hover:bg-gray-50 transition-colors";
                  if (isBanned) {
                    rowBgClass = "bg-rose-50 hover:bg-rose-100 transition-colors";
                  } else if (isAtRisk) {
                    rowBgClass = "bg-orange-50 hover:bg-orange-100 transition-colors";
                  }

                  return (
                  <tr key={idx} className={rowBgClass}>
                    <td className="p-4 text-center text-xs text-gray-400 font-semibold">{page * size + idx + 1}</td>
                    
                    <td className="p-4 whitespace-nowrap">
                      <div className="flex items-center gap-3">
                        <img src={student.avatar} alt={student.name} className="w-9 h-9 rounded-full bg-gray-200 object-cover border border-gray-200 shrink-0 relative z-10 shadow-sm" />
                        <div className="flex flex-col">
                          <span className="font-bold text-gray-900 whitespace-nowrap">{student.name}</span>
                          <span className="text-[12px] text-gray-500 font-medium mt-0.5 whitespace-nowrap">{student.email}</span>
                          {(student.eligibleSessionCount > 0 || totalClassSessions > 0) && (() => {
                            const openedTotal = totalClassSessions ?? student.eligibleSessionCount;
                            // Sinh viên tham gia trễ nếu số buổi hợp lệ < tổng buổi đã mở
                            const joinedLate = totalClassSessions != null && student.eligibleSessionCount < totalClassSessions;
                            const missedBeforeJoin = joinedLate ? (totalClassSessions - student.eligibleSessionCount) : 0;
                            return (
                              <span className="text-[10px] text-gray-400 mt-0.5 flex items-center gap-1 flex-wrap">
                                {/* presentCount = số buổi có mặt thực tế (PRESENT/LATE) từ backend */}
                                {student.presentCount ?? 0} có mặt / {student.eligibleSessionCount ?? openedTotal} buổi hợp lệ
                                {joinedLate && (
                                  <span
                                    className="inline-flex items-center gap-0.5 bg-red-100 text-red-700 border border-red-200 px-1.5 py-0.5 rounded font-bold cursor-help"
                                    title={`Tham gia lớp từ buổi thứ ${missedBeforeJoin + 1}. ${missedBeforeJoin} buổi trước đó mặc định tính là VẮNG (ABSENT) vì sinh viên chưa join. Giảng viên có thể điểm danh thủ công để điều chỉnh.`}
                                  >
                                    ⏰ Tham gia trễ ({missedBeforeJoin} buổi → VẮNG)
                                  </span>
                                )}
                              </span>
                            );
                          })()}
                          <div className="mt-1.5 flex items-center gap-2">
                            <span className="text-[10px] text-gray-400 font-medium">Tỷ lệ:</span>
                            {(() => {
                              const defaultSessions = plannedSessions && plannedSessions > 0 ? plannedSessions : (totalClassSessions || 1);
                              const presentCount = student.presentCount ?? 0;
                              const lateCount = student.lateCount ?? 0;
                              const rate = Math.round(((presentCount + lateCount) / defaultSessions) * 100);
                              return renderProgressBar(Math.min(rate, 100));
                            })()}
                          </div>
                        </div>
                      </div>
                    </td>

                    <td className="p-4 text-center whitespace-nowrap">
                      <span className="text-[13px] font-semibold text-emerald-600">
                        {student.presentCount ?? <span className="text-gray-400 text-xs italic">—</span>}
                      </span>
                    </td>
                    <td className="p-4 text-center whitespace-nowrap">
                      <span className={`text-[13px] font-semibold ${(student.lateCount ?? 0) > 0 ? 'text-amber-600' : 'text-gray-400'}`}>
                        {(student.lateCount ?? 0) > 0 ? student.lateCount : <span className="text-gray-400 text-xs italic">0</span>}
                      </span>
                    </td>
                    <td className="p-4 text-center whitespace-nowrap">
                      {(() => {
                        const joinedLate = totalClassSessions != null && student.eligibleSessionCount != null
                          && student.eligibleSessionCount < totalClassSessions;
                        const missedBeforeJoin = joinedLate ? (totalClassSessions - student.eligibleSessionCount) : 0;
                        const displayAbsent = (student.absentCount ?? 0) + missedBeforeJoin;
                        return (
                          <span className={`text-[13px] font-semibold ${displayAbsent > 0 ? 'text-red-600' : 'text-gray-400'}`}>
                            {displayAbsent > 0 ? (
                              <span title={missedBeforeJoin > 0 ? `${student.absentCount ?? 0} vắng + ${missedBeforeJoin} buổi trước khi tham gia (mặc định VẮNG)` : undefined}>
                                {displayAbsent}
                                {missedBeforeJoin > 0 && (
                                  <sup className="text-[9px] text-red-500 ml-0.5">*</sup>
                                )}
                              </span>
                            ) : <span className="text-gray-400 text-xs italic">—</span>}
                          </span>
                        );
                      })()}
                    </td>

                    <td className="p-4 text-gray-500 whitespace-nowrap">
                      {formatDate(student.joinedAt) || <span className="text-xs italic text-gray-400">Chưa cập nhật</span>}
                    </td>
                    <td className="p-4 whitespace-nowrap">{renderStatusBadge(student)}</td>
                    <td className="p-4 text-center whitespace-nowrap">
                      <div className="flex justify-center gap-1">
                        <button 
                          onClick={() => handleViewDetail(student)}
                          className="p-1.5 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          title="Xem chi tiết điểm danh"
                        >
                          <Eye size={15} />
                        </button>
                        <button 
                          onClick={() => handleDeleteStudent(student)}
                          className="p-1.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          title="Xóa sinh viên khỏi lớp"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </td>
                  </tr>
                )})}
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

      {/* =========== ATTENDANCE STATS & RISK PANEL =========== */}
      <div className="w-full xl:w-[340px] shrink-0 flex flex-col gap-4">

        {/* --- KPI CARDS --- */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
          <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
            <h3 className="font-bold text-gray-900 flex items-center gap-2 text-[13px]">
              <BarChart3 size={15} className="text-red-600" /> Tổng quan điểm danh
            </h3>
            {isPolling && <Loader2 size={12} className="animate-spin text-gray-400" />}
          </div>
          <div className="grid grid-cols-2 divide-x divide-y divide-gray-100">
            {/* Hàng 1: Phên đã mở ĐD vs Số buổi học kế hoạch */}
            <div className="p-3 text-center bg-blue-50/40">
              <div className="text-xl font-extrabold text-blue-700">
                {totalClassSessions != null ? totalClassSessions : <span className="text-gray-400 text-sm font-medium">—</span>}
              </div>
              <div className="text-[10px] text-blue-600 font-semibold mt-0.5">Đã mở điểm danh</div>
              <div className="text-[9px] text-blue-400 mt-0.5">(phiên)</div>
            </div>
            <div className="p-3 text-center bg-indigo-50/40">
              <div className="text-xl font-extrabold text-indigo-700">
                {plannedSessions != null ? plannedSessions : <span className="text-gray-400 text-sm font-medium">—</span>}
              </div>
              <div className="text-[10px] text-indigo-600 font-semibold mt-0.5">Số buổi học</div>
              <div className="text-[9px] text-indigo-400 mt-0.5">(kế hoạch)</div>
            </div>
            <div className="p-4 text-center">
              <div className="text-2xl font-extrabold text-gray-900">{totalElements}</div>
              <div className="text-[11px] text-gray-500 font-medium mt-0.5 flex items-center justify-center gap-1">
                <Users size={10} /> Tổng sinh viên
              </div>
            </div>
            <div className="p-4 text-center bg-emerald-50/40">
              <div className="text-2xl font-extrabold text-emerald-600">{goodStudents.length}</div>
              <div className="text-[11px] text-emerald-600 font-semibold mt-0.5">✓ Học tốt</div>
            </div>
            <div className="p-4 text-center bg-amber-50/40">
              <div className="text-2xl font-extrabold text-amber-600">{warningStudents.length + nearBanStudents.length}</div>
              <div className="text-[11px] text-amber-600 font-semibold mt-0.5">⚡ Cảnh báo</div>
            </div>
            <div className="p-4 text-center bg-red-50/40">
              <div className="text-2xl font-extrabold text-red-600">{bannedStudents.length}</div>
              <div className="text-[11px] text-red-600 font-semibold mt-0.5">🚫 Nguy cơ cấm thi</div>
            </div>
          </div>
          {/* Mini tỉ lệ phân bổ */}
          {students.length > 0 && (
            <div className="px-4 pb-3 pt-1">
              <div className="flex h-2 rounded-full overflow-hidden gap-0.5">
                <div className="bg-emerald-400 rounded-l-full transition-all" style={{ width: `${(goodStudents.length / students.length) * 100}%` }} title="Học tốt" />
                <div className="bg-amber-400 transition-all" style={{ width: `${((warningStudents.length + nearBanStudents.length) / students.length) * 100}%` }} title="Cảnh báo" />
                <div className="bg-red-500 rounded-r-full transition-all" style={{ width: `${(bannedStudents.length / students.length) * 100}%` }} title="Nguy cơ cấm thi" />
              </div>
              <div className="flex justify-between mt-1.5 text-[10px] text-gray-400 font-medium">
                <span className="text-emerald-500">Tốt: {goodStudents.length}</span>
                <span className="text-amber-500">CB: {warningStudents.length + nearBanStudents.length}</span>
                <span className="text-red-500">Cấm: {bannedStudents.length}</span>
              </div>
            </div>
          )}
        </div>

        {/* --- BANNER: Cấm thi --- */}
        {bannedStudents.length > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-xl p-3.5 flex gap-2.5 items-start shadow-sm animate-in fade-in">
            <Ban size={16} className="text-red-600 shrink-0 mt-0.5" />
            <div>
              <p className="text-xs font-bold text-red-800">
                {bannedStudents.length} sinh viên có nguy cơ bị cấm thi!
              </p>
              <p className="text-[11px] text-red-700 mt-0.5 leading-relaxed">
                Tỷ lệ vắng vượt ngưỡng quy định. Cần xem xét và xử lý ngay.
              </p>
            </div>
          </div>
        )}

        {/* --- BANNER: Sắp cấm thi (≤ 2 buổi còn lại) --- */}
        {nearBanStudents.length > 0 && (
          <div className="bg-amber-50 border border-amber-300 rounded-xl p-3.5 flex gap-2.5 items-start shadow-sm animate-in fade-in">
            <Bell size={16} className="text-amber-600 shrink-0 mt-0.5 animate-pulse" />
            <div>
              <p className="text-xs font-bold text-amber-800">
                {nearBanStudents.length} sinh viên sắp đến ngưỡng cấm thi!
              </p>
              <p className="text-[11px] text-amber-700 mt-0.5 leading-relaxed">
                Còn ≤ 2 buổi vắng nữa sẽ vi phạm. Nên thông báo ngay cho sinh viên.
              </p>
            </div>
          </div>
        )}

        {/* --- DANH SÁCH SINH VIÊN NGUY CƠ CAO --- */}
        {atRiskList.length > 0 ? (
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 flex items-center gap-2 text-[13px]">
                <UserX size={15} className="text-red-500" /> Sinh viên nguy cơ cao
              </h3>
              <span className="text-[11px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded-full border border-red-100">
                {atRiskList.length} SV
              </span>
            </div>

            <div className="divide-y divide-gray-50 max-h-[300px] overflow-y-auto">
              {atRiskList.slice(0, 10).map((student, idx) => {
                const absentCount = student.absentCount ?? 0;
                const remaining = maxAbsentAllowed - absentCount;
                const isBanned = bannedStudents.some(b => (b.rawId || b.id) === (student.rawId || student.id));
                const isNearBan = nearBanStudents.some(n => (n.rawId || n.id) === (student.rawId || student.id));

                return (
                  <div key={idx} className={`p-3 flex items-center gap-2.5 hover:bg-gray-50 transition-colors group ${
                    isBanned ? 'bg-red-50/30' : isNearBan ? 'bg-amber-50/30' : ''
                  }`}>
                    <img src={student.avatar} alt="" className="w-7 h-7 rounded-full shrink-0 border border-gray-200 object-cover" />
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-1">
                        <span className="text-[12px] font-bold text-gray-900 truncate">{student.name}</span>
                        {isBanned ? (
                          <span className="text-[9px] bg-red-100 text-red-700 px-1.5 py-0.5 rounded-full font-bold shrink-0 border border-red-200">CẤM THI</span>
                        ) : isNearBan ? (
                          <span className="text-[9px] bg-amber-100 text-amber-700 px-1.5 py-0.5 rounded-full font-bold shrink-0 border border-amber-200">SẮP CẤM</span>
                        ) : (
                          <span className="text-[9px] bg-orange-100 text-orange-700 px-1.5 py-0.5 rounded-full font-bold shrink-0 border border-orange-200">CẢNH BÁO</span>
                        )}
                      </div>
                      <div className="flex items-center gap-2 mt-0.5">
                        <span className={`text-[11px] font-semibold ${isBanned ? 'text-red-600' : 'text-amber-600'}`}>
                          Vắng {absentCount}/{maxAbsentAllowed} buổi
                        </span>
                        {!isBanned && remaining > 0 && (
                          <span className="text-[10px] text-gray-400">· còn {remaining} buổi</span>
                        )}
                      </div>
                      {student.attendanceRate !== undefined && (
                        <div className="mt-1.5 w-full bg-gray-100 h-1 rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all ${isBanned ? 'bg-red-500' : 'bg-amber-400'}`}
                            style={{ width: `${Math.min(100, student.attendanceRate ?? 0)}%` }}
                          />
                        </div>
                      )}
                    </div>
                    <button
                      onClick={() => handleEmailWarning(student)}
                      className="p-1.5 text-gray-300 group-hover:text-red-500 hover:!text-red-600 hover:bg-red-50 rounded-lg transition-colors shrink-0"
                      title={`Gửi email cảnh báo tới ${student.name}`}
                    >
                      <Send size={12} />
                    </button>
                  </div>
                );
              })}
            </div>

            {atRiskList.length > 10 && (
              <div className="p-2.5 border-t border-gray-100 text-center">
                <span className="text-[11px] text-gray-400 font-medium">
                  + {atRiskList.length - 10} sinh viên khác có nguy cơ
                </span>
              </div>
            )}

            {/* Nút gửi email hàng loạt */}
            {atRiskList.length > 0 && (
              <div className="p-3 border-t border-gray-100 bg-gray-50/50 flex flex-col gap-2">
                {bannedOnlyList.length > 0 && (
                  <button
                    onClick={async () => {
                      const toastId = toast.loading('Đang chuẩn bị email thông báo cấm thi...');
                      try {
                        let classCode = 'N/A';
                        let className = 'Lớp học';
                        let lecturerName = 'Giảng viên phụ trách';
                        
                        if (classId && !classId.startsWith('mock-')) {
                           const detail = await classApi.getClassDetail(classId);
                           if (detail) {
                              classCode = detail.code || classId;
                              className = detail.className || detail.name || 'Lớp học';
                              lecturerName = detail.lecturerName || detail.instructorName || 'Giảng viên phụ trách';
                           }
                        }
                        
                        const emails = bannedOnlyList.map(s => s.email).filter(Boolean).join(';');
                        const subject = encodeURIComponent(`[${classCode}] Thông báo: Vi phạm quy định điểm danh - CẤM THI`);
                        
                        let bodyText = `Kính gửi các bạn sinh viên,\n\n`;
                        bodyText += `Các bạn đã vi phạm quy định điểm danh và hiện đang thuộc diện CẤM THI.\n\n`;
                        bodyText += `THÔNG TIN LỚP HỌC:\n`;
                        bodyText += `- Lớp: ${className} (${classCode})\n`;
                        bodyText += `- Giảng viên phụ trách: ${lecturerName}\n\n`;
                        bodyText += `Vui lòng kiểm tra lại lịch sử điểm danh chi tiết của mình trên hệ thống và liên hệ giảng viên ngay lập tức để được hỗ trợ.\n\n`;
                        bodyText += `Trân trọng,\n${lecturerName}`;
                        
                        const body = encodeURIComponent(bodyText);
                        toast.dismiss(toastId);
                        window.location.href = `mailto:?bcc=${emails}&subject=${subject}&body=${body}`;
                      } catch (e) {
                        toast.error("Lỗi khi tạo email!", { id: toastId });
                      }
                    }}
                    className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-700 hover:to-red-600 text-white text-[12px] font-bold rounded-lg transition-all active:scale-[0.98] shadow-sm"
                  >
                    <Mail size={13} /> Thông báo Cấm thi ({bannedOnlyList.length} SV)
                  </button>
                )}

                {warningOnlyList.length > 0 && (
                  <button
                    onClick={async () => {
                      const toastId = toast.loading('Đang chuẩn bị email cảnh báo nguy cơ...');
                      try {
                        let classCode = 'N/A';
                        let className = 'Lớp học';
                        let lecturerName = 'Giảng viên phụ trách';
                        
                        if (classId && !classId.startsWith('mock-')) {
                           const detail = await classApi.getClassDetail(classId);
                           if (detail) {
                              classCode = detail.code || classId;
                              className = detail.className || detail.name || 'Lớp học';
                              lecturerName = detail.lecturerName || detail.instructorName || 'Giảng viên phụ trách';
                           }
                        }
                        
                        const emails = warningOnlyList.map(s => s.email).filter(Boolean).join(';');
                        const subject = encodeURIComponent(`[${classCode}] Cảnh báo điểm danh - Nguy cơ vi phạm quy định`);
                        
                        let bodyText = `Kính gửi các bạn sinh viên,\n\n`;
                        bodyText += `Các bạn đang có nguy cơ vi phạm quy định điểm danh do số buổi vắng mặt vượt mức cho phép.\n\n`;
                        bodyText += `THÔNG TIN LỚP HỌC:\n`;
                        bodyText += `- Lớp: ${className} (${classCode})\n`;
                        bodyText += `- Giảng viên phụ trách: ${lecturerName}\n\n`;
                        bodyText += `Vui lòng kiểm tra lại lịch sử điểm danh chi tiết của mình trên hệ thống và liên hệ giảng viên ngay lập tức để được hỗ trợ.\n\n`;
                        bodyText += `Trân trọng,\n${lecturerName}`;
                        
                        const body = encodeURIComponent(bodyText);
                        toast.dismiss(toastId);
                        window.location.href = `mailto:?bcc=${emails}&subject=${subject}&body=${body}`;
                      } catch (e) {
                        toast.error("Lỗi khi tạo email!", { id: toastId });
                      }
                    }}
                    className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-gradient-to-r from-orange-500 to-orange-400 hover:from-orange-600 hover:to-orange-500 text-white text-[12px] font-bold rounded-lg transition-all active:scale-[0.98] shadow-sm"
                  >
                    <Mail size={13} /> Cảnh báo nguy cơ ({warningOnlyList.length} SV)
                  </button>
                )}
              </div>
            )}
          </div>
        ) : (
          !isInitialLoading && students.length > 0 && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-center">
              <CheckCircle2 size={22} className="text-emerald-500 mx-auto mb-1.5" />
              <p className="text-xs font-bold text-emerald-800">Lớp học đang ổn định!</p>
              <p className="text-[11px] text-emerald-600 mt-0.5">Không có sinh viên nào có nguy cơ vi phạm quy định điểm danh.</p>
            </div>
          )
        )}

        {/* --- CHÍNH SÁCH ĐIỂM DANH --- */}
        {attendancePolicy && (
          <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-4 py-3 border-b border-gray-100">
              <h3 className="font-bold text-gray-900 flex items-center gap-2 text-[13px]">
                <Info size={14} className="text-blue-500" /> Chính sách điểm danh
              </h3>
            </div>
            <div className="p-3 space-y-2">
              <div className="flex justify-between items-center text-xs">
                <span className="text-gray-500 font-medium">Vắng tối đa được phép</span>
                <span className="font-bold text-gray-800 bg-gray-50 px-2 py-0.5 rounded border border-gray-200">{maxAbsentAllowed} buổi</span>
              </div>
              {attendancePolicy.minAttendanceRate != null && (
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-medium">Tỉ lệ điểm danh tối thiểu</span>
                  <span className="font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-100">{attendancePolicy.minAttendanceRate}%</span>
                </div>
              )}
              {attendancePolicy.warnAbsentSessions != null && (
                <div className="flex justify-between items-center text-xs">
                  <span className="text-gray-500 font-medium">Ngưỡng cảnh báo vắng</span>
                  <span className="font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded border border-amber-100">{attendancePolicy.warnAbsentSessions} buổi</span>
                </div>
              )}
              <div className="flex justify-between items-center text-xs">
                <span className="text-gray-500 font-medium">Còn lại không được vắng</span>
                <span className="font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded border border-red-100">≤ 2 buổi → Cảnh báo GV</span>
              </div>
            </div>
          </div>
        )}

        {/* --- SECURITY ALERTS (giữ nguyên) --- */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col">
          <div className="px-4 py-3 border-b border-gray-200 flex justify-between items-center">
            <h3 className="font-bold text-gray-900 flex items-center gap-2 text-[13px]">
              <ShieldAlert size={15} className="text-red-600" /> Cảnh báo bảo mật
            </h3>
            <span className="text-[11px] font-bold text-red-600 bg-red-50 px-2 py-0.5 rounded">{securityAlerts.length} Mới</span>
          </div>
          <div className="p-3 space-y-3 flex-1">
            {securityAlerts.map((alert) => (
              <div key={alert.id} className={`p-3 rounded-xl border ${alert.borderColor} ${alert.bgColor}`}>
                <div className="flex gap-2.5">
                  <div className="mt-0.5 shrink-0">{alert.icon}</div>
                  <div>
                    <h4 className="text-[13px] font-bold text-gray-900 leading-tight mb-1">{alert.title}</h4>
                    <p className="text-[12px] text-gray-600 mb-2 leading-snug">{alert.desc}</p>
                    <div className="flex gap-3 text-[12px] font-semibold">
                      <button className="text-red-600 hover:text-red-800 transition-colors">Xem xét</button>
                      <button className="text-gray-500 hover:text-gray-700 transition-colors">Bỏ qua</button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="p-3 border-t border-gray-200 text-center">
            <button className="text-[12px] font-bold text-red-600 hover:text-red-800 transition-colors flex items-center justify-center gap-1 w-full">
              Xem tất cả nhật ký bảo mật <ChevronRight size={14} />
            </button>
          </div>
        </div>

      </div>
      {/* =========== END STATS PANEL =========== */}

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

      {/* 3-STEP PREMIUM STUDENT IMPORT WIZARD MODAL */}
      {showImportWizard && (
        <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm z-[999] flex items-center justify-center animate-in fade-in duration-200 p-4">
          <div className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-gray-150 transform transition-all scale-in animate-in zoom-in-95 duration-200 overflow-hidden">
            
            {/* Modal Header */}
            <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50 shrink-0">
              <div>
                <h3 className="text-lg font-bold text-gray-900 flex items-center gap-2">
                  <Upload className="text-red-600" size={20} />
                  Nhập danh sách sinh viên hàng loạt
                </h3>
                <p className="text-xs text-gray-500 mt-1">Đăng ký thành viên lớp học nhanh chóng bằng file Excel hoặc CSV</p>
              </div>
              <button 
                onClick={handleCloseImportWizard}
                disabled={isImporting || isValidating}
                className="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <XCircle size={20} />
              </button>
            </div>

            {/* Step Wizard Bar */}
            <div className="px-6 py-4 bg-white border-b border-gray-100 flex items-center justify-center gap-4 md:gap-8 shrink-0">
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                  importStep >= 1 ? 'bg-red-600 text-white shadow-sm shadow-red-100' : 'bg-gray-100 text-gray-400'
                }`}>
                  {importStep > 1 ? <CheckCircle2 size={14} /> : '1'}
                </span>
                <span className={`text-xs md:text-sm font-bold ${importStep >= 1 ? 'text-gray-900' : 'text-gray-400'}`}>
                  Tải lên File
                </span>
              </div>
              <div className="w-8 md:w-16 h-[2px] bg-gray-100" />
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                  importStep >= 2 ? 'bg-red-600 text-white shadow-sm shadow-red-100' : 'bg-gray-100 text-gray-400'
                }`}>
                  {importStep > 2 ? <CheckCircle2 size={14} /> : '2'}
                </span>
                <span className={`text-xs md:text-sm font-bold ${importStep >= 2 ? 'text-gray-900' : 'text-gray-400'}`}>
                  Kiểm tra & Đối chiếu
                </span>
              </div>
              <div className="w-8 md:w-16 h-[2px] bg-gray-100" />
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                  importStep >= 3 ? 'bg-red-600 text-white shadow-sm shadow-red-100' : 'bg-gray-100 text-gray-400'
                }`}>
                  3
                </span>
                <span className={`text-xs md:text-sm font-bold ${importStep >= 3 ? 'text-gray-900' : 'text-gray-400'}`}>
                  Hoàn tất
                </span>
              </div>
            </div>

            {/* Modal Body / Steps View */}
            <div className="p-6 overflow-y-auto flex-1 bg-gray-50/30">
              
              {/* STEP 1: UPLOAD FILE PANEL */}
              {importStep === 1 && (
                <div className="flex flex-col items-center justify-center py-6">
                  {/* Vùng giao diện - Chứa input bên trong một cách hợp lệ */}
                  <label className="w-full max-w-lg border-2 border-dashed border-gray-300 hover:border-red-500 bg-white rounded-2xl p-8 flex flex-col items-center justify-center text-center cursor-pointer transition-all hover:bg-red-50/5 group shadow-sm animate-fade-in">
                    <input 
                      type="file" 
                      accept=".xlsx,.xls,.csv" 
                      onChange={handleFileUpload}
                      onClick={(e) => { e.target.value = null }} // Reset để chọn lại file cũ
                      className="hidden" // Input bị ẩn nhưng vẫn nhận sự kiện từ Label
                    />
                    <div className="w-14 h-14 rounded-full bg-red-50 text-red-600 flex items-center justify-center mb-4 transition-transform group-hover:scale-115 duration-300 shadow-inner">
                      <Upload size={24} />
                    </div>
                    <p className="text-sm font-bold text-gray-800 mb-1">
                      Kéo thả file Excel hoặc nhấp để chọn file
                    </p>
                    <p className="text-xs text-gray-400">
                      Chấp nhận file định dạng .xlsx, .xls, .csv (Tối đa 10MB)
                    </p>
                  </label>

                  <div className="my-6 flex items-center justify-center w-full max-w-md">
                    <div className="h-[1px] bg-gray-200 flex-1" />
                    <span className="px-4 text-xs font-bold text-gray-400 uppercase tracking-wider">Hoặc sử dụng dữ liệu mẫu</span>
                    <div className="h-[1px] bg-gray-200 flex-1" />
                  </div>

                  <div className="flex flex-col md:flex-row gap-4 justify-center items-center w-full max-w-lg shrink-0">
                    <button 
                      onClick={handleLoadSampleData}
                      className="flex items-center gap-2 px-5 py-3 border border-red-200 bg-red-50/50 hover:bg-red-50 text-red-600 hover:text-red-700 font-bold rounded-xl text-sm transition-all active:scale-[0.98] shadow-sm shadow-red-50/50"
                    >
                      <Sparkles size={16} className="animate-pulse" />
                      Nạp dữ liệu thử nghiệm ⚡
                    </button>
                    
                    <button 
                      onClick={() => {
                        // Xuất File Mẫu Excel
                        let csvContent = "\uFEFFstudentCode,name,email\nN22DCCN160,Phạm Văn Phú,phu.pvn22@ptit.edu.vn\nN22DCCN001,Nguyễn Văn A,a.nguyen@gmail.com\nN22DCCN002,Trần Thị B,b.tran@gmail.com\n";
                        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = "File_mau_import_sinh_vien.csv";
                        a.click();
                      }}
                      className="flex items-center gap-2 px-5 py-3 border border-gray-200 hover:border-gray-300 bg-white hover:bg-gray-50 text-gray-700 font-semibold rounded-xl text-sm transition-all active:scale-[0.98] shadow-sm"
                    >
                      <FileText size={16} className="text-gray-400" />
                      Tải mẫu file Excel (.csv)
                    </button>
                  </div>

                  {/* Format Notice */}
                  <div className="w-full max-w-lg mt-8 p-4 bg-blue-50/60 border border-blue-100 rounded-xl text-left flex gap-3">
                    <Info size={18} className="text-blue-500 shrink-0 mt-0.5" />
                    <div>
                      <h4 className="text-xs font-bold text-blue-950 uppercase tracking-wide">Yêu cầu cấu trúc cột trong file:</h4>
                      <p className="text-xs text-blue-900 leading-relaxed mt-1">
                        File Excel cần chứa đúng 3 cột tiêu đề sau tại dòng đầu tiên:
                        <br />
                        • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">studentCode</strong> : Mã sinh viên (Bắt buộc, không được để trống).
                        <br />
                        • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">name</strong> : Họ và tên sinh viên.
                        <br />
                        • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">email</strong> : Địa chỉ Email (Dùng để gửi thông tin tài khoản và đăng nhập).
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* STEP 2: DRY-RUN VALIDATION & PREVIEW PANEL */}
              {importStep === 2 && (
                <div className="flex flex-col h-full min-h-[300px]">
                  
                  {isValidating ? (
                    <div className="flex flex-col items-center justify-center flex-1 py-12">
                      <Loader2 className="animate-spin text-red-600 mb-4" size={40} />
                      <h4 className="text-sm font-bold text-gray-800">Đang kiểm tra đối chiếu dữ liệu...</h4>
                      <p className="text-xs text-gray-400 mt-1">Hệ thống đang đối chiếu mã sinh viên, email với cơ sở dữ liệu hiện tại</p>
                    </div>
                  ) : (
                    <>
                      {/* Rule 6 Critical Conflict Red Alert */}
                      {hasCriticalConflict && (
                        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-xl text-left flex gap-3 shadow-sm animate-pulse">
                          <XCircle className="text-red-500 shrink-0 mt-0.5" size={20} />
                          <div>
                            <h4 className="text-xs font-bold text-red-950 uppercase tracking-wide">
                              PHÁT HIỆN XUNG ĐỘT TÀI KHOẢN CHÉO NGHIÊM TRỌNG (RULE 6):
                            </h4>
                            <p className="text-xs text-red-900 leading-relaxed mt-1">
                              Một học sinh trong tệp khớp với **User A** qua Mã Sinh Viên nhưng lại khớp với **User B** qua địa chỉ Email. Theo chính sách an ninh bảo mật hệ thống, **toàn bộ tiến trình nhập danh sách này sẽ bị từ chối** để bảo đảm an toàn dữ liệu. Vui lòng kiểm tra và hiệu chỉnh lại tệp tin trước khi thử lại!
                            </p>
                          </div>
                        </div>
                      )}

                      {/* Summary Statistics Dashboard */}
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-5">
                        <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm text-left">
                          <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider block">Tổng số dòng đã nhận</span>
                          <span className="text-2xl font-extrabold text-gray-900 block mt-1">{totalImport}</span>
                        </div>
                        <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-4 shadow-sm text-left">
                          <span className="text-[11px] font-bold text-emerald-500 uppercase tracking-wider block">Liên kết & Khôi phục</span>
                          <span className="text-2xl font-extrabold text-emerald-600 block mt-1">{existingCount + restoredCount}</span>
                        </div>
                        <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-4 shadow-sm text-left">
                          <span className="text-[11px] font-bold text-blue-500 uppercase tracking-wider block">Sẽ tạo mới (NEW)</span>
                          <span className="text-2xl font-extrabold text-blue-600 block mt-1">{newCount}</span>
                        </div>
                        <div className="bg-red-50/50 border border-red-100 rounded-xl p-4 shadow-sm text-left">
                          <span className="text-[11px] font-bold text-red-500 uppercase tracking-wider block">Dữ liệu lỗi (ERROR)</span>
                          <span className="text-2xl font-extrabold text-red-600 block mt-1">{errorCount}</span>
                        </div>
                      </div>

                      {/* Info Alert Box */}
                      {errorCount > 0 && !hasCriticalConflict && (
                        <div className="mb-4 p-3 bg-amber-50 border border-amber-100 rounded-xl text-left flex gap-2.5 items-start">
                          <AlertTriangle className="text-amber-500 shrink-0 mt-0.5" size={16} />
                          <p className="text-xs text-amber-800 leading-relaxed">
                            Có <strong className="text-amber-950">{errorCount} dòng dữ liệu bị lỗi</strong> trong file của bạn. Hệ thống sẽ **tự động bỏ qua các dòng lỗi** này và chỉ thực hiện nhập các sinh viên hợp lệ.
                          </p>
                        </div>
                      )}

                      {/* Preview Table */}
                      <div className="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm flex-1 max-h-[350px] overflow-y-auto">
                        <table className="w-full text-left text-xs border-collapse">
                          <thead className="bg-gray-50 border-b border-gray-200 font-bold text-gray-500 uppercase tracking-wider sticky top-0 z-20 font-sans">
                            <tr>
                              <th className="p-3 w-10 text-center">STT</th>
                              <th className="p-3">Mã Sinh Viên</th>
                              <th className="p-3">Họ và Tên</th>
                              <th className="p-3">Email</th>
                              <th className="p-3 w-40">Phân Loại Trạng Thế</th>
                              <th className="p-3">Chi tiết lỗi / Chú thích</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-gray-100 font-sans">
                            {validatedRows.map((row, index) => {
                              let rowClass = 'hover:bg-gray-50';
                              let badgeClass = '';
                              let badgeText = '';
                              let note = '';

                              const action = row.action;
                              const status = row.status;

                              if (action === 'CREATED_USER_AND_ADDED' || status === 'NEW') {
                                rowClass = 'bg-blue-50/15 hover:bg-blue-50/30';
                                badgeClass = 'bg-blue-100 text-blue-850 border border-blue-200 font-bold';
                                badgeText = 'Tạo tài khoản mới';
                                note = `🔑 Ghost Account. Mật khẩu mặc định: ${row.defaultPasswordRule || 'MSSV+tên viết liền'}. Bắt buộc đổi mật khẩu ở lần đăng nhập đầu tiên.`;
                              } else if (action === 'LINKED_EXISTING_USER_AND_ADDED' || status === 'EXISTING') {
                                rowClass = 'bg-emerald-50/15 hover:bg-emerald-50/30';
                                badgeClass = 'bg-emerald-100 text-emerald-850 border border-emerald-200 font-bold';
                                badgeText = 'Liên kết tài khoản';
                                note = 'Khớp tài khoản hệ thống. Sẽ tự động liên kết trực tiếp vào lớp học.';
                              } else if (action === 'RESTORED_MEMBER') {
                                rowClass = 'bg-teal-50/15 hover:bg-teal-50/30';
                                badgeClass = 'bg-teal-100 text-teal-850 border border-teal-200 font-bold';
                                badgeText = 'Khôi phục thành viên';
                                note = 'Sinh viên từng bị xóa/từ chối khỏi lớp, nay được khôi phục về Approved.';
                              } else if (action === 'SKIPPED_EXISTING_MEMBER') {
                                rowClass = 'bg-gray-50 hover:bg-gray-100/80';
                                badgeClass = 'bg-gray-150 text-gray-700 border border-gray-200 font-semibold';
                                badgeText = 'Bỏ qua (Đã tham gia)';
                                note = 'Sinh viên đã là thành viên đang hoạt động, hệ thống tự động bỏ qua.';
                              } else if (action === 'ERROR' || status === 'ERROR') {
                                rowClass = 'bg-red-50/20 hover:bg-red-50/40';
                                badgeClass = 'bg-red-100 text-red-800 border border-red-200 font-bold';
                                badgeText = 'Lỗi dữ liệu';
                                note = row.errorMsg || 'Sai định dạng dữ liệu';
                              }

                              return (
                                <tr key={index} className={`transition-colors ${rowClass}`}>
                                  <td className="p-3 text-center text-gray-400 font-semibold">{index + 1}</td>
                                  <td className="p-3 font-mono font-bold text-gray-900">{row.studentCode || <span className="text-red-550 italic font-bold">Rỗng</span>}</td>
                                  <td className="p-3 font-semibold text-gray-800">{row.fullName || row.name || <span className="text-gray-400 italic">Chưa cập nhật</span>}</td>
                                  <td className="p-3 text-gray-600 font-medium">{row.email || <span className="text-gray-400 italic">Chưa cập nhật</span>}</td>
                                  <td className="p-3">
                                    <span className={`px-2 py-0.5 rounded text-[10px] uppercase font-bold inline-block tracking-wide ${badgeClass}`}>
                                      {badgeText}
                                    </span>
                                  </td>
                                  <td className={`p-3 text-[11px] font-medium ${badgeText === 'Lỗi dữ liệu' ? 'text-red-600 font-bold' : 'text-gray-500'}`}>
                                    {badgeText === 'Lỗi dữ liệu' ? (
                                      <span className="flex items-center gap-1">
                                        <AlertCircle size={12} className="shrink-0 animate-pulse text-red-500" />
                                        {note}
                                      </span>
                                    ) : note}
                                  </td>
                                </tr>
                              );
                            })}
                          </tbody>
                        </table>
                      </div>

                      {/* Locked State Warning */}
                      {!canConfirmImport && (
                        <div className="mt-4 p-4 bg-red-50 border border-red-100 rounded-xl text-left flex gap-3">
                          <XCircle className="text-red-500 shrink-0 mt-0.5" size={18} />
                          <div>
                            <h4 className="text-xs font-bold text-red-950 uppercase tracking-wide">Không thể thực thi Import:</h4>
                            <p className="text-xs text-red-900 leading-relaxed mt-1">
                              {hasCriticalConflict ? (
                                "Tệp tin chứa xung đột tài khoản chéo nghiêm trọng. Bạn bắt buộc phải sửa đổi tệp tin để đảm bảo an ninh thông tin."
                              ) : (
                                "File của bạn không chứa bất kỳ dòng dữ liệu hợp lệ nào có thể nhập vào lớp. Vui lòng kiểm tra lại cấu trúc dữ liệu!"
                              )}
                            </p>
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}

              {/* STEP 3: SUCCESS & REPORT PANEL */}
              {importStep === 3 && (
                <div className="flex flex-col items-center justify-center py-6 text-center animate-fade-in">
                  <div className="w-16 h-16 rounded-full bg-emerald-50 border border-emerald-100 text-emerald-600 flex items-center justify-center mb-4 shadow-md shadow-emerald-50/50 animate-bounce">
                    <CheckCircle2 size={36} />
                  </div>
                  
                  <h3 className="text-xl font-bold text-gray-900 mb-2">Nhập danh sách sinh viên thành công!</h3>
                  <p className="text-sm text-gray-600 max-w-md mb-6 leading-relaxed">
                    Hệ thống đã thực thi lưu trữ hoàn tất! Đã cập nhật sĩ số lớp học tăng thêm <strong className="text-emerald-600 font-extrabold">{existingCount + newCount + restoredCount} sinh viên</strong>.
                  </p>

                  {/* Detailed statistics breakdown */}
                  <div className="w-full max-w-lg bg-white rounded-2xl p-5 border border-gray-200 shadow-sm mb-6 text-left">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">Kết quả thống kê thực thi chi tiết:</h4>
                    <div className="grid grid-cols-2 gap-3 text-xs">
                      <div className="p-3 bg-gray-50 rounded-xl border border-gray-100">
                        <span className="text-gray-400 block font-bold">Tổng số dòng xử lý</span>
                        <span className="text-lg font-extrabold text-gray-900 block mt-0.5">
                          {importSummary?.totalRows ?? totalImport}
                        </span>
                      </div>
                      <div className="p-3 bg-blue-50/40 rounded-xl border border-blue-100/50">
                        <span className="text-blue-500 block font-bold">Tài khoản tạo mới</span>
                        <span className="text-lg font-extrabold text-blue-600 block mt-0.5">
                          {importSummary?.createdUsers ?? newCount}
                        </span>
                      </div>
                      <div className="p-3 bg-emerald-50/40 rounded-xl border border-emerald-100/50">
                        <span className="text-emerald-500 block font-bold">Tài khoản liên kết</span>
                        <span className="text-lg font-extrabold text-emerald-600 block mt-0.5">
                          {importSummary?.linkedExistingUsers ?? existingCount}
                        </span>
                      </div>
                      <div className="p-3 bg-teal-50/40 rounded-xl border border-teal-100/50">
                        <span className="text-teal-500 block font-bold">Khôi phục quyền</span>
                        <span className="text-lg font-extrabold text-teal-600 block mt-0.5">
                          {importSummary?.restoredMembers ?? restoredCount}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="w-full max-w-lg bg-gray-50 rounded-2xl p-5 border border-gray-200 text-left space-y-4 shadow-inner">
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Thông tin bảo mật quan trọng (Default Password Strategy):</h4>
                    
                    <div className="flex gap-3 items-start">
                      <span className="w-6 h-6 rounded-full bg-blue-50 border border-blue-100 text-blue-650 flex items-center justify-center text-xs shrink-0 font-bold">🔑</span>
                      <div>
                        <h5 className="text-xs font-bold text-gray-800">Cơ chế thiết lập mật khẩu mặc định (Ghost Accounts):</h5>
                        <p className="text-xs text-gray-500 leading-relaxed mt-1">
                          Các tài khoản tạo mới được gán mật khẩu tự động theo công thức: <strong className="text-blue-600 font-extrabold">Mã Sinh Viên + Họ Tên</strong> viết liền không dấu, chữ thường.
                          <br />
                          <span className="text-[11px] text-gray-400 italic">Ví dụ: MSV 'N22DCCN160' + Tên 'Phạm Văn Phú' ➡️ Mật khẩu mặc định: 'n22dccn160phamvanphu'.</span>
                        </p>
                      </div>
                    </div>

                    <div className="flex gap-3 items-start">
                      <span className="w-6 h-6 rounded-full bg-amber-50 border border-amber-100 text-amber-650 flex items-center justify-center text-xs shrink-0 font-bold">🔒</span>
                      <div>
                        <h5 className="text-xs font-bold text-gray-800">Ép buộc thay đổi mật khẩu lần đăng nhập đầu tiên:</h5>
                        <p className="text-xs text-gray-500 leading-relaxed mt-1">
                          Cờ bắt buộc đổi mật khẩu <strong className="font-mono text-amber-600 font-bold">requirePasswordChange = true</strong> đã được bật. Sinh viên bắt buộc phải đổi mật khẩu ngay trong lần đầu tiên đăng nhập trước khi được quyền truy cập vào các chức năng của cổng thông tin.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              )}

            </div>

            {/* Modal Footer */}
            <div className="p-6 border-t border-gray-100 bg-gray-50/50 flex justify-end gap-3 shrink-0">
              {importStep === 1 && (
                <button 
                  onClick={handleCloseImportWizard}
                  className="px-5 py-2.5 bg-gray-150 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl text-sm transition-all active:scale-[0.98]"
                >
                  Hủy bỏ
                </button>
              )}

              {importStep === 2 && (
                <>
                  <button 
                    onClick={() => setImportStep(1)}
                    disabled={isImporting}
                    className="px-5 py-2.5 bg-gray-100 hover:bg-gray-200 disabled:opacity-50 text-gray-700 font-semibold rounded-xl text-sm transition-all active:scale-[0.98]"
                  >
                    Quay lại
                  </button>
                  
                  <button 
                    onClick={handleConfirmImport}
                    disabled={!canConfirmImport || isImporting}
                    className="px-5 py-2.5 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-700 hover:to-red-600 disabled:opacity-50 text-white font-semibold rounded-xl text-sm transition-all shadow-md active:scale-[0.98] flex items-center justify-center gap-1.5 font-bold"
                  >
                    {isImporting ? (
                      <>
                        <Loader2 className="animate-spin" size={16} /> Đang lưu thật...
                      </>
                    ) : (
                      <>Nhập danh sách ({existingCount + newCount + restoredCount} SV)</>
                    )}
                  </button>
                </>
              )}

              {importStep === 3 && (
                <button 
                  onClick={handleCloseImportWizard}
                  className="px-6 py-2.5 bg-gradient-to-r from-red-600 to-red-500 hover:from-red-700 hover:to-red-600 text-white font-bold rounded-xl text-sm transition-all shadow-md active:scale-[0.98]"
                >
                  Đóng & Quay lại Lớp học
                </button>
              )}
            </div>

          </div>
        </div>
      )}

      {/* ===================== MODAL CHI TIẾT ĐIỂM DANH ===================== */}
      {isDetailModalOpen && selectedStudentForDetail && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-gray-900/40 backdrop-blur-sm transition-opacity" onClick={() => setIsDetailModalOpen(false)}></div>
          <div className="bg-white rounded-3xl w-full max-w-4xl max-h-[85vh] overflow-hidden shadow-2xl relative z-10 flex flex-col animate-in zoom-in-95 duration-200">
            {/* Header */}
            <div className="px-6 py-5 border-b border-gray-100 flex items-center justify-between bg-gradient-to-r from-blue-50/50 to-white">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center shrink-0 border border-blue-200 shadow-inner overflow-hidden">
                  <img src={selectedStudentForDetail.avatar} alt={selectedStudentForDetail.name} className="w-full h-full object-cover" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-gray-900">{selectedStudentForDetail.name}</h2>
                  <div className="text-sm text-gray-500 font-medium flex items-center gap-2">
                    {selectedStudentForDetail.email}
                  </div>
                </div>
              </div>
              <button 
                onClick={() => setIsDetailModalOpen(false)}
                className="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition-colors"
              >
                <XCircle size={24} />
              </button>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto p-6 bg-gray-50/50">
              {studentDetailsLoading ? (
                <div className="flex flex-col items-center justify-center h-48 space-y-3">
                  <Loader2 className="animate-spin text-blue-600" size={32} />
                  <span className="text-gray-500 font-medium">Đang tải lịch sử điểm danh...</span>
                </div>
              ) : studentAttendanceDetails.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 text-gray-500">
                  <CalendarClock size={48} className="text-gray-300 mb-3" />
                  <span className="font-medium">Chưa có dữ liệu điểm danh nào.</span>
                </div>
              ) : (
                <div className="space-y-4">
                  {studentAttendanceDetails.map((detail, idx) => {
                    const session = detail.session;
                    const evt = detail.event;
                    
                    let statusLabel = "VẮNG";
                    let statusColor = "bg-red-100 text-red-700 border-red-200";
                    let statusIcon = <XCircle size={14} className="mr-1" />;
                    let reason = "";
                    let recordedAt = "";

                    if (evt) {
                      if (evt.status === 'PRESENT') {
                        statusLabel = "CÓ MẶT";
                        statusColor = "bg-emerald-100 text-emerald-700 border-emerald-200";
                        statusIcon = <CheckCircle2 size={14} className="mr-1" />;
                      } else if (evt.status === 'LATE') {
                        statusLabel = "TRỄ";
                        statusColor = "bg-amber-100 text-amber-700 border-amber-200";
                        statusIcon = <AlertCircle size={14} className="mr-1" />;
                      } else if (evt.status === 'EXCUSED') {
                        statusLabel = "CÓ PHÉP";
                        statusColor = "bg-blue-100 text-blue-700 border-blue-200";
                        statusIcon = <Info size={14} className="mr-1" />;
                      }
                      reason = evt.note || evt.reason || "";
                      if (evt.timestamp) {
                        const t = new Date(evt.timestamp);
                        recordedAt = t.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) + ' ' + t.toLocaleDateString('vi-VN');
                      }
                    }

                    // Xử lý mặc định VẮNG nếu session đã mở trước khi join
                    if (!evt) {
                      const joinedAtDate = selectedStudentForDetail.joinedAt ? new Date(selectedStudentForDetail.joinedAt) : null;
                      const sessionDate = new Date(session.startTime);
                      if (joinedAtDate && sessionDate < joinedAtDate) {
                        reason = "Chưa tham gia lớp học tại thời điểm này (Mặc định VẮNG)";
                      }
                    }

                    return (
                      <div key={idx} className="bg-white border border-gray-200 rounded-2xl p-4 shadow-sm hover:shadow-md transition-shadow flex flex-col md:flex-row md:items-center justify-between gap-4">
                        <div className="flex items-start gap-4">
                          <div className="w-10 h-10 rounded-xl bg-gray-100 text-gray-500 flex items-center justify-center shrink-0 font-black text-sm">
                            #{studentAttendanceDetails.length - idx}
                          </div>
                          <div>
                            <h4 className="font-bold text-gray-900 text-[15px]">Phiên {new Date(session.startTime).toLocaleDateString('vi-VN')}</h4>
                            <div className="flex items-center gap-2 mt-1">
                              <span className="text-xs text-gray-500 font-medium">
                                {new Date(session.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })} - {new Date(session.endTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}
                              </span>
                              {session.room && (
                                <>
                                  <span className="w-1 h-1 bg-gray-300 rounded-full"></span>
                                  <span className="text-xs text-gray-500 font-medium">P. {session.room}</span>
                                </>
                              )}
                            </div>
                          </div>
                        </div>

                        <div className="flex flex-col md:items-end gap-2">
                          <span className={`inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold border ${statusColor}`}>
                            {statusIcon} {statusLabel}
                          </span>
                          {recordedAt && <span className="text-[11px] text-gray-400 font-medium flex items-center gap-1">🕒 {recordedAt}</span>}
                          {reason && <span className="text-xs text-gray-600 bg-gray-50 px-2 py-1 rounded border border-gray-100 italic max-w-xs truncate" title={reason}>{reason}</span>}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
