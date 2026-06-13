import React, { useState, useEffect } from 'react';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ShieldAlert, Bell, UploadCloud, Info, Plus, Trash2, 
  Calendar, Users, UserCheck, Clock, MapPin, BookOpen, AlertCircle, FileText, AlertTriangle,
  Upload, Sparkles, CheckCircle2, Loader2, GraduationCap
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { classApi } from '../../../api/classApi';

// --- HÀM GEN MÃ BẢO MẬT ---
export const generateSecureJoinCode = (subjectCode) => {
  let prefix = subjectCode.trim().toUpperCase().replace(/\s+/g, '');
  if (prefix.length > 2) prefix = prefix.substring(0, 2);
  const randomPart = Math.random().toString(36).substring(2, 6).toUpperCase();
  const timeSalt = (Date.now() % 100000).toString().padStart(5, '0');
  return `${prefix}_${randomPart}_${timeSalt}`;
};

// Danh sách ngày trong tuần (ánh xạ số -> tên tiếng Anh)
const DAYS_OF_WEEK_MAP = {
  0: 'SUNDAY',
  1: 'MONDAY',
  2: 'TUESDAY',
  3: 'WEDNESDAY',
  4: 'THURSDAY',
  5: 'FRIDAY',
  6: 'SATURDAY'
};

const DAYS_OF_WEEK = [
  { value: 'MONDAY', label: 'Thứ 2', vi: 'Thứ Hai' },
  { value: 'TUESDAY', label: 'Thứ 3', vi: 'Thứ Ba' },
  { value: 'WEDNESDAY', label: 'Thứ 4', vi: 'Thứ Tư' },
  { value: 'THURSDAY', label: 'Thứ 5', vi: 'Thứ Năm' },
  { value: 'FRIDAY', label: 'Thứ 6', vi: 'Thứ Sáu' },
  { value: 'SATURDAY', label: 'Thứ 7', vi: 'Thứ Bảy' },
  { value: 'SUNDAY', label: 'CN', vi: 'Chủ Nhật' }
];

const SEMESTERS = [
  { value: 'HK1', label: 'Học kỳ 1' },
  { value: 'HK2', label: 'Học kỳ 2' },
  { value: 'HK3', label: 'Học kỳ 3 (Hè)' }
];

const ACADEMIC_YEARS = [
  { value: '2024-2025', label: '2024-2025' },
  { value: '2025-2026', label: '2025-2026' },
  { value: '2026-2027', label: '2026-2027' }
];

const CREDITS = [
  { value: '1', label: '1 Tín chỉ' },
  { value: '2', label: '2 Tín chỉ' },
  { value: '3', label: '3 Tín chỉ' },
  { value: '4', label: '4 Tín chỉ' },
  { value: '5', label: '5 Tín chỉ' }
];

// Helper kiểm tra trùng khung giờ học (overlapping time intervals)
const isTimeOverlap = (start1, end1, start2, end2) => {
  const parseTimeToMinutes = (timeStr) => {
    if (!timeStr) return 0;
    const [h, m] = timeStr.split(':').map(Number);
    return h * 60 + m;
  };
  const s1 = parseTimeToMinutes(start1);
  const e1 = parseTimeToMinutes(end1);
  const s2 = parseTimeToMinutes(start2);
  const e2 = parseTimeToMinutes(end2);
  return s1 < e2 && s2 < e1;
};



const formatTimeForDisplay = (timeStr) => {
  if (!timeStr) return '';
  const [hourStr, minuteStr] = timeStr.split(':');
  const hourVal = parseInt(hourStr);
  const ampm = hourVal >= 12 ? 'PM' : 'AM';
  const displayHour = hourVal % 12 === 0 ? 12 : hourVal % 12;
  const formattedHour = displayHour.toString().padStart(2, '0');
  return `${formattedHour}:${minuteStr} ${ampm}`;
};

// --- BỘ CHỌN THỜI GIAN CUSTOM CAO CẤP DẠNG ĐỒNG HỒ ---
const CustomTimePicker = ({ value, onChange }) => {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = React.useRef(null);
  const hourListRef = React.useRef(null);
  const minuteListRef = React.useRef(null);

  const [hour, minute] = value.split(':');

  const hours = Array.from({ length: 17 }, (_, i) => (i + 6).toString().padStart(2, '0')); // 06 to 22
  const minutes = Array.from({ length: 12 }, (_, i) => (i * 5).toString().padStart(2, '0')); // 00 to 55 in 5m steps

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    if (isOpen) {
      setTimeout(() => {
        const activeHourEl = hourListRef.current?.querySelector('[data-active="true"]');
        const activeMinuteEl = minuteListRef.current?.querySelector('[data-active="true"]');
        if (activeHourEl) {
          activeHourEl.scrollIntoView({ block: 'center', behavior: 'auto' });
        }
        if (activeMinuteEl) {
          activeMinuteEl.scrollIntoView({ block: 'center', behavior: 'auto' });
        }
      }, 50);
    }
  }, [isOpen]);

  const handleSelectHour = (h) => {
    onChange(`${h}:${minute}`);
  };

  const handleSelectMinute = (m) => {
    onChange(`${hour}:${m}`);
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full pl-9 pr-7 py-2.5 border border-gray-250 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50 font-semibold text-gray-700 cursor-pointer flex items-center justify-between transition-all"
      >
        <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-red-600">
          <Clock size={16} />
        </span>
        <span>{formatTimeForDisplay(value)}</span>
        <svg className="w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="absolute z-50 mt-2 p-3 bg-white border border-gray-150 rounded-2xl shadow-xl flex gap-3 animate-in fade-in slide-in-from-top-1 duration-200">
          {/* Cột chọn Giờ */}
          <div className="flex flex-col items-center">
            <span className="text-xs font-bold text-gray-400 mb-1">Giờ</span>
            <div 
              ref={hourListRef} 
              className="h-40 overflow-y-auto w-12 flex flex-col gap-1 pr-1 scrollbar-none"
            >
              {hours.map(h => (
                <button
                  key={h}
                  type="button"
                  data-active={h === hour}
                  onClick={() => handleSelectHour(h)}
                  className={`w-full py-1 text-xs font-bold rounded-lg transition-all ${
                    h === hour 
                      ? 'bg-red-600 text-white shadow-md' 
                      : 'text-gray-600 hover:bg-red-50 hover:text-red-600'
                  }`}
                >
                  {h}
                </button>
              ))}
            </div>
          </div>

          <div className="w-[1px] bg-gray-100 self-stretch my-2" />

          {/* Cột chọn Phút */}
          <div className="flex flex-col items-center">
            <span className="text-xs font-bold text-gray-400 mb-1">Phút</span>
            <div 
              ref={minuteListRef} 
              className="h-40 overflow-y-auto w-12 flex flex-col gap-1 pr-1 scrollbar-none"
            >
              {minutes.map(m => (
                <button
                  key={m}
                  type="button"
                  data-active={m === minute}
                  onClick={() => handleSelectMinute(m)}
                  className={`w-full py-1 text-xs font-bold rounded-lg transition-all ${
                    m === minute 
                      ? 'bg-red-600 text-white shadow-md' 
                      : 'text-gray-600 hover:bg-red-50 hover:text-red-600'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};


export default function CreateClass() {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(1);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [startDateError, setStartDateError] = useState('');
  const [absencesError, setAbsencesError] = useState('');
  const [hasDraft, setHasDraft] = useState(false);
  const [scheduleTab, setScheduleTab] = useState('config'); // 'config' | 'free-slots'
  const [livePersonalConflict, setLivePersonalConflict] = useState(null);

  // --- STATES & HANDLERS CHO IMPORT SINH VIÊN ---
  const [importStep, setImportStep] = useState(1);
  const [selectedFile, setSelectedFile] = useState(null);
  const [validatedRows, setValidatedRows] = useState([]);
  const [isValidating, setIsValidating] = useState(false);

  const totalImport = validatedRows.length;
  const existingCount = validatedRows.filter(r => r.status === 'EXISTING').length;
  const newCount = validatedRows.filter(r => r.status === 'NEW').length;
  const errorCount = validatedRows.filter(r => r.status === 'ERROR').length;
  const canConfirmImport = (existingCount + newCount) > 0;

  const MOCK_IMPORT_PAYLOAD = [
    { studentCode: "N22DCCN160", name: "Phạm Văn Phú", email: "phu.pvn22@ptit.edu.vn" },
    { studentCode: "N22DCCN001", name: "Nguyễn Văn A", email: "a.nguyen@gmail.com" },
    { studentCode: "N22DCCN002", name: "Trần Thị B", email: "b.tran@gmail.com" },
    { studentCode: "N22DCCN003", name: "Lê Văn C", email: "c.le@gmail.com" },
    { studentCode: "N22DCCN004", name: "", email: "d.nguyen@gmail.com" }, // Lỗi thiếu tên
    { studentCode: "", name: "Phạm Văn E", email: "e.pham@gmail.com" }, // Lỗi thiếu MSSV
    { studentCode: "N22DCCN006", name: "Nguyễn Văn F", email: "invalid-email" }, // Lỗi định dạng email
    { studentCode: "N22DCCN001", name: "Trùng lặp mã", email: "dup.code@gmail.com" } // Lỗi lặp MSSV trong file
  ];

  const simulateDryRun = (rows) => {
    const seenCodes = new Set();
    return rows.map(row => {
      const code = (row.studentCode || '').trim();
      const name = (row.name || '').trim();
      const email = (row.email || '').trim();

      // 1. Kiểm tra ERROR
      if (!code) {
        return { ...row, status: 'ERROR', errorMsg: 'Thiếu Mã sinh viên (studentCode)' };
      }
      if (!name) {
        return { ...row, status: 'ERROR', errorMsg: 'Thiếu Họ tên sinh viên (name)' };
      }
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!email || !emailRegex.test(email)) {
        return { ...row, status: 'ERROR', errorMsg: 'Định dạng Email không hợp lệ' };
      }
      if (seenCodes.has(code)) {
        return { ...row, status: 'ERROR', errorMsg: 'Mã sinh viên bị lặp lại trong file' };
      }
      seenCodes.add(code);

      // 2. NEW (Tạo mới tài khoản ma)
      return { ...row, status: 'NEW', errorMsg: null };
    });
  };

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
              studentCode: rowData.studentCode || '',
              name: rowData.name || '',
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

  const handleLoadSampleData = () => {
    setSelectedFile({
      name: "Danh_sach_sinh_vien_mau.xlsx",
      size: "18.5 KB"
    });
    handleStartValidation(MOCK_IMPORT_PAYLOAD);
  };

  const handleStartValidation = async (payload) => {
    setIsValidating(true);
    setImportStep(2);
    
    await new Promise(resolve => setTimeout(resolve, 1200));
    
    try {
      const simulated = simulateDryRun(payload);
      setValidatedRows(simulated);
    } catch (err) {
      console.error("Lỗi validate import:", err);
      toast.error("Lỗi phân tích dữ liệu kiểm tra.");
    } finally {
      setIsValidating(false);
    }
  };

  const handleRemoveFile = () => {
    setSelectedFile(null);
    setValidatedRows([]);
    setImportStep(1);
  };

  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });

  const [formData, setFormData] = useState({
    name: '',
    code: '',
    courseCode: '',
    classCode: '',
    joinCode: '',
    description: '',
    semester: 'HK2',
    academicYear: '2024-2025',
    campus: 'Cơ sở 1',
    room: '',
    startDate: '',
    credits: '3',
    approvalMode: 'AUTO',
    allowAutoJoinOnCheckin: true,
    totalSessions: 15,
    maxAllowedAbsences: 3,
    weeklySchedules: [
      { dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }
    ]
  });

  const [uiState, setUiState] = useState({
    strictDevicePolicy: true,
    qrSessionLength: 15,
    lateWindow: 10,
    flagRapid: true,
    flagLocation: true
  });

  const [existingClasses, setExistingClasses] = useState([]);
  const [conflictModal, setConflictModal] = useState({
    isOpen: false,
    type: '', // 'ROOM_CONFLICT', 'START_DATE_CONFLICT', 'END_DATE_EXCEEDED'
    title: '',
    message: '',
    conflictingClass: null,
    conflictingRoom: '',
    conflictingSchedule: '',
    suggestedRooms: [],
    suggestedDates: [],
    maxSessionsSuggested: 15
  });

  // Tìm kiếm các ngày bắt đầu hợp lệ khớp với lịch học
  const getSuggestedStartDates = (schedules, semester, academicYear) => {
    if (!schedules || schedules.length === 0) return [];
    const range = getSemesterDateRange(semester, academicYear);
    if (!range) return [];

    const suggested = [];
    const now = new Date();
    // Bắt đầu tìm kiếm từ ngày bắt đầu học kỳ hoặc hôm nay (tùy mốc nào lớn hơn)
    const startDateCursor = now > range.start ? new Date(now) : new Date(range.start);
    
    // Tìm kiếm trong vòng 30 ngày tiếp theo
    for (let offset = 0; offset < 30; offset++) {
      const d = new Date(startDateCursor);
      d.setDate(startDateCursor.getDate() + offset);
      if (d > range.end) break;

      const dayOfWeek = getDayOfWeek(d.toISOString().split('T')[0]);
      const isMatching = schedules.some(s => s.dayOfWeek === dayOfWeek);

      if (isMatching) {
        suggested.push(new Date(d));
      }
      if (suggested.length >= 6) break; // Chỉ gợi ý tối đa 6 ngày gần nhất
    }
    return suggested;
  };

  // Tải danh sách lớp hiện có của giảng viên khi load trang để phục vụ việc kiểm tra trùng phòng
  useEffect(() => {
    const fetchExistingClasses = async () => {
      try {
        const res = await classApi.getTeachingClasses({ page: 0, size: 200 });
        if (res && res.items) {
          setExistingClasses(res.items);
        }
      } catch (err) {
        console.error("Lỗi lấy danh sách lớp học hiện có:", err);
      }
    };
    fetchExistingClasses();
  }, []);

  // Real-time dynamic conflict checker for personal schedules
  useEffect(() => {
    if (formData.weeklySchedules && formData.weeklySchedules.length > 0) {
      const personalConflict = checkPersonalScheduleConflict(formData.weeklySchedules);
      if (personalConflict) {
        setLivePersonalConflict(personalConflict);
      } else {
        setLivePersonalConflict(null);
      }
    } else {
      setLivePersonalConflict(null);
    }
  }, [formData.weeklySchedules, existingClasses]);

  // Đề xuất khung giờ rảnh trong ngày bị trùng lịch
  const getAvailablePersonalTimeSlots = (dayOfWeek) => {
    const STANDARD_TIME_SLOTS = [
      { start: '07:00', end: '09:30', label: 'Ca 1 (07:00 - 09:30)' },
      { start: '09:30', end: '12:00', label: 'Ca 2 (09:30 - 12:00)' },
      { start: '13:00', end: '15:30', label: 'Ca 3 (13:00 - 15:30)' },
      { start: '15:30', end: '18:00', label: 'Ca 4 (15:30 - 18:00)' },
      { start: '18:00', end: '20:30', label: 'Ca tối (18:00 - 20:30)' }
    ];

    const busySchedules = [];
    for (const group of existingClasses) {
      if (!group.weeklySchedules) continue;
      for (const s of group.weeklySchedules) {
        if (s.dayOfWeek === dayOfWeek) {
          busySchedules.push(s);
        }
      }
    }

    return STANDARD_TIME_SLOTS.filter(slot => {
      // Slot trống nếu KHÔNG trùng với bất kỳ lịch bận nào
      return !busySchedules.some(busy => isTimeOverlap(slot.start, slot.end, busy.startTime, busy.endTime));
    });
  };

  // Kiểm tra trùng lịch dạy cá nhân
  const checkPersonalScheduleConflict = (newSchedules) => {
    if (!newSchedules || newSchedules.length === 0) return null;

    for (const group of existingClasses) {
      if (!group.weeklySchedules) continue;

      for (const newSched of newSchedules) {
        for (const extSched of group.weeklySchedules) {
          if (newSched.dayOfWeek === extSched.dayOfWeek) {
            if (isTimeOverlap(newSched.startTime, newSched.endTime, extSched.startTime, extSched.endTime)) {
              return {
                class: group,
                conflictSchedule: `${DAYS_OF_WEEK.find(d => d.value === newSched.dayOfWeek)?.vi || newSched.dayOfWeek} ${newSched.startTime}-${newSched.endTime}`,
                dayOfWeek: newSched.dayOfWeek,
                newSchedule: newSched
              };
            }
          }
        }
      }
    }
    return null;
  };


  // Lấy phạm vi ngày của học kỳ theo năm học
  const getSemesterDateRange = (semester, academicYear) => {
    if (!academicYear || !semester) return null;
    const parts = academicYear.split('-');
    if (parts.length !== 2) return null;
    
    const year1 = parseInt(parts[0]);
    const year2 = parseInt(parts[1]);
    
    if (isNaN(year1) || isNaN(year2)) return null;
    
    let start = null;
    let end = null;
    
    if (semester === 'HK1') {
      // Học kỳ 1: Tháng 8 đến Tháng 12 năm học đầu
      start = new Date(year1, 7, 1); // 7 = Tháng 8
      end = new Date(year1, 11, 31); // 11 = Tháng 12
    } else if (semester === 'HK2') {
      // Học kỳ 2: Tháng 1 đến Tháng 5 năm học sau
      start = new Date(year2, 0, 1); // 0 = Tháng 1
      end = new Date(year2, 4, 31); // 4 = Tháng 5
    } else if (semester === 'HK3' || semester === 'SUMMER') {
      // Học kỳ Hè: Tháng 6 đến Tháng 8 năm học sau
      start = new Date(year2, 5, 1); // 5 = Tháng 6
      end = new Date(year2, 7, 31); // 7 = Tháng 8
    }
    
    return { start, end };
  };

  // Tính ngày kết thúc dựa trên số buổi học và lịch học tuần
  const calculateEndDate = (startDateStr, totalSessions, weeklySchedules) => {
    if (!startDateStr || !totalSessions || !weeklySchedules || weeklySchedules.length === 0) {
      return null;
    }
    
    const startDate = new Date(startDateStr);
    if (isNaN(startDate.getTime())) return null;

    const dayOrder = { 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 0 };
    
    const scheduleDays = weeklySchedules.map(s => dayOrder[s.dayOfWeek]);
    
    let currentDate = new Date(startDate.getTime());
    let sessionsRemaining = totalSessions;
    
    // Buổi học đầu tiên rơi vào ngày bắt đầu
    sessionsRemaining--;
    
    while (sessionsRemaining > 0) {
      currentDate.setDate(currentDate.getDate() + 1);
      const dayOfWeekNum = currentDate.getDay();
      if (scheduleDays.includes(dayOfWeekNum)) {
        sessionsRemaining--;
      }
    }
    
    return currentDate;
  };

  // Tính tổng số buổi tối đa có thể xếp từ ngày bắt đầu đến hết học kỳ
  const calculateMaxSessions = (startDateStr, rangeEnd, weeklySchedules) => {
    if (!startDateStr || !rangeEnd || !weeklySchedules || weeklySchedules.length === 0) return 0;
    
    const startDate = new Date(startDateStr);
    const endDate = new Date(rangeEnd);
    if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) return 0;
    
    const dayOrder = { 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 0 };
    const scheduleDays = weeklySchedules.map(s => dayOrder[s.dayOfWeek]);
    
    let currentDate = new Date(startDate.getTime());
    let count = 0;
    
    while (currentDate <= endDate) {
      const dayOfWeekNum = currentDate.getDay();
      if (scheduleDays.includes(dayOfWeekNum)) {
        count++;
      }
      currentDate.setDate(currentDate.getDate() + 1);
    }
    
    return count;
  };

  const generateScheduledSessionsList = (startDateStr, totalSessions, weeklySchedules) => {
    if (!startDateStr || !totalSessions || !weeklySchedules || weeklySchedules.length === 0) {
      return [];
    }
    
    const startDate = new Date(startDateStr);
    if (isNaN(startDate.getTime())) return [];

    const dayOrder = { 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3, 'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6, 'SUNDAY': 0 };
    
    // Map day to schedule config { dayOfWeekNum, startTime, endTime }
    const scheduleConfig = weeklySchedules.map(s => ({
      dayOfWeekNum: dayOrder[s.dayOfWeek],
      startTime: s.startTime,
      endTime: s.endTime
    }));

    let currentDate = new Date(startDate.getTime());
    let sessions = [];
    let sessionsRemaining = totalSessions;
    
    // Check if the startDate itself matches any scheduled day of week
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
    
    // Iterate day-by-day to find subsequent sessions
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

  // Hàm lấy thứ trong tuần từ ngày
  const getDayOfWeek = (dateString) => {
    if (!dateString) return null;
    const date = new Date(dateString);
    const dayIndex = date.getDay(); // 0 = Chủ Nhật, 1 = Thứ 2, ...
    return DAYS_OF_WEEK_MAP[dayIndex];
  };

  // Hàm validate toàn bộ ràng buộc ngày bắt đầu và ngày kết thúc
  const validateDates = (startDate, schedules, totalSessions, semester, academicYear) => {
    if (!startDate) return { isValid: false, message: 'Vui lòng chọn ngày bắt đầu' };
    if (!schedules || schedules.length === 0) return { isValid: false, message: 'Vui lòng thêm ít nhất một lịch học' };
    
    // 1. Kiểm tra ngày bắt đầu có nằm ngoài học kỳ không
    const range = getSemesterDateRange(semester, academicYear);
    if (range) {
      const start = new Date(startDate);
      if (start < range.start || start > range.end) {
        const startStr = range.start.toLocaleDateString('vi-VN');
        const endStr = range.end.toLocaleDateString('vi-VN');
        const semLabel = SEMESTERS.find(s => s.value === semester)?.label || semester;
        return {
          isValid: false,
          message: `Ngày bắt đầu phải nằm trong thời gian của ${semLabel} (${startStr} - ${endStr})`
        };
      }
    }

    // 2. Kiểm tra ngày bắt đầu khớp với lịch học
    const dayOfWeek = getDayOfWeek(startDate);
    const matchingSchedule = schedules.find(s => s.dayOfWeek === dayOfWeek);
    const dayName = DAYS_OF_WEEK.find(d => d.value === dayOfWeek)?.vi || dayOfWeek;
    
    if (!matchingSchedule) {
      const scheduleDays = schedules.map(s => DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.vi || s.dayOfWeek).join(', ');
      return { 
        isValid: false, 
        message: `Ngày ${startDate} là ${dayName}, nhưng lịch học chỉ có các ngày: ${scheduleDays}. Vui lòng chọn ngày khác hoặc thêm lịch học cho ${dayName}.` 
      };
    }

    // 3. Tính toán ngày kết thúc và kiểm tra xem có vượt quá học kỳ không
    const endDate = calculateEndDate(startDate, totalSessions, schedules);
    if (endDate && range && endDate > range.end) {
      const endDateStr = endDate.toLocaleDateString('vi-VN');
      const semEndStr = range.end.toLocaleDateString('vi-VN');
      const semLabel = SEMESTERS.find(s => s.value === semester)?.label || semester;
      
      // Tính gợi ý số buổi tối đa khả dụng trong thời gian học kỳ từ ngày bắt đầu
      const maxSessions = calculateMaxSessions(startDate, range.end, schedules);
      
      return {
        isValid: false,
        message: `Cảnh báo: Với ${totalSessions} buổi học, ngày kết thúc dự kiến là ${endDateStr}, vượt quá ngày kết thúc của ${semLabel} (${semEndStr}). Dựa trên số buổi học trong tuần, tổng số buổi không nên vượt quá ${maxSessions} buổi.`
      };
    }
    
    return { isValid: true, message: '' };
  };

  // Tự động điều chỉnh startDate khi thay đổi lịch học
  const autoAdjustStartDate = (schedules) => {
    if (schedules.length === 0) return;
    
    const firstScheduleDay = schedules[0].dayOfWeek;
    const today = new Date();
    const currentDayIndex = today.getDay();
    const targetDayIndex = DAYS_OF_WEEK.findIndex(d => d.value === firstScheduleDay);
    
    let daysToAdd = targetDayIndex - currentDayIndex;
    if (daysToAdd < 0) daysToAdd += 7;
    if (daysToAdd === 0 && today.getHours() >= 12) daysToAdd = 7;
    
    const nextDate = new Date(today);
    nextDate.setDate(today.getDate() + daysToAdd);
    const suggestedDate = nextDate.toISOString().split('T')[0];
    
    setFormData(prev => ({ ...prev, startDate: suggestedDate }));
    setStartDateError('');
  };

  // Kiểm tra bản nháp khi tải trang
  useEffect(() => {
    const saved = localStorage.getItem('create_class_draft');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed.name || parsed.courseCode || parsed.description || parsed.room || parsed.startDate) {
          setHasDraft(true);
        }
      } catch (e) {
        console.error(e);
      }
    }
  }, []);

  const handleRestoreDraft = () => {
    const saved = localStorage.getItem('create_class_draft');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setFormData(parsed);
        toast.success('Đã khôi phục bản nháp thành công!');
      } catch (e) {
        console.error(e);
        toast.error('Không thể khôi phục bản nháp!');
      }
    }
    setHasDraft(false);
  };

  const handleDiscardDraft = () => {
    localStorage.removeItem('create_class_draft');
    setHasDraft(false);
    toast.success('Đã bỏ qua bản nháp.');
  };

  const handleSaveDraftManual = () => {
    localStorage.setItem('create_class_draft', JSON.stringify(formData));
    toast.success('Đã lưu bản nháp thành công!');
  };

  // Tự động lưu bản nháp khi dữ liệu thay đổi
  useEffect(() => {
    const hasData = formData.name || formData.courseCode || formData.description || formData.room || formData.startDate;
    if (hasData) {
      localStorage.setItem('create_class_draft', JSON.stringify(formData));
    }
  }, [formData]);

  // Auto-generate code when courseCode or classCode changes
  useEffect(() => {
    const generatedCode = formData.courseCode 
      ? `${formData.courseCode}-${formData.classCode || '01'}`
      : '';
    setFormData(prev => ({ ...prev, code: generatedCode }));
  }, [formData.courseCode, formData.classCode]);

  // Reactive validation và tự động sửa sai (auto-correction & reset) cho các trường ngày, số buổi, học kỳ
  useEffect(() => {
    // 1. Tự động sửa giới hạn tổng số buổi học nếu vượt quá thời hạn học kỳ
    if (formData.startDate) {
      const range = getSemesterDateRange(formData.semester, formData.academicYear);
      if (range) {
        const maxSessions = calculateMaxSessions(formData.startDate, range.end, formData.weeklySchedules);
        const currentTotal = parseInt(formData.totalSessions) || 0;
        
        if (currentTotal > maxSessions) {
          toast.error(`Tổng số buổi học vượt quá quỹ thời gian của kỳ. Tự động điều chỉnh về tối đa ${maxSessions} buổi.`);
          setFormData(prev => ({
            ...prev,
            totalSessions: maxSessions
          }));
          return;
        }
      }
    }

    // 2. Tự động sửa số buổi vắng cho phép (< 0 hoặc > 30% tổng số buổi)
    const total = parseInt(formData.totalSessions) || 0;
    const absences = parseInt(formData.maxAllowedAbsences);
    
    if (!isNaN(absences)) {
      if (absences < 0) {
        toast.error('Số buổi vắng không được nhỏ hơn 0. Tự động đặt về 0.');
        setFormData(prev => ({
          ...prev,
          maxAllowedAbsences: 0
        }));
        return;
      }
      
      const maxAllowed = Math.floor(total * 0.3);
      if (total > 0 && absences > maxAllowed) {
        toast.error(`Số buổi vắng vượt quá 30% tổng số buổi học. Tự động điều chỉnh về tối đa ${maxAllowed} buổi.`);
        setFormData(prev => ({
          ...prev,
          maxAllowedAbsences: maxAllowed
        }));
        return;
      }
    }

    // 3. Thực hiện kiểm tra lỗi trực quan để hiển thị thông báo dưới ô nhập
    if (formData.startDate) {
      const validation = validateDates(
        formData.startDate,
        formData.weeklySchedules,
        formData.totalSessions,
        formData.semester,
        formData.academicYear
      );
      if (!validation.isValid) {
        setStartDateError(validation.message);
      } else {
        setStartDateError('');
      }
    } else {
      setStartDateError('');
    }

    // Kiểm tra số buổi vắng cho phép để hiện thông tin lỗi dưới trường nhập
    const currentAbsences = parseInt(formData.maxAllowedAbsences);
    const currentTotal = parseInt(formData.totalSessions) || 0;
    const currentMaxAllowed = Math.floor(currentTotal * 0.3);
    
    if (isNaN(currentAbsences) || currentAbsences < 0) {
      setAbsencesError('Số buổi vắng cho phép không được âm (< 0)');
    } else if (currentTotal > 0 && currentAbsences > currentMaxAllowed) {
      setAbsencesError(`Số buổi vắng cho phép không được vượt quá 30% tổng số buổi (${currentMaxAllowed} buổi)`);
    } else {
      setAbsencesError('');
    }
  }, [formData.startDate, formData.weeklySchedules, formData.totalSessions, formData.maxAllowedAbsences, formData.semester, formData.academicYear]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleStartDateChange = (e) => {
    const newDate = e.target.value;
    setFormData(prev => ({ ...prev, startDate: newDate }));
    
    if (newDate) {
      const validation = validateDates(
        newDate,
        formData.weeklySchedules,
        formData.totalSessions,
        formData.semester,
        formData.academicYear
      );
      if (!validation.isValid) {
        if (validation.message.includes("không khớp") || validation.message.includes("lịch học chỉ có")) {
          const suggestions = getSuggestedStartDates(formData.weeklySchedules, formData.semester, formData.academicYear);
          setConflictModal({
            isOpen: true,
            type: 'START_DATE_CONFLICT',
            title: 'Ngày Bắt Đầu Không Khớp Lịch Học!',
            message: validation.message,
            suggestedDates: suggestions
          });
        } else if (validation.message.includes("vượt quá ngày kết thúc")) {
          const range = getSemesterDateRange(formData.semester, formData.academicYear);
          const maxS = range ? calculateMaxSessions(newDate, range.end, formData.weeklySchedules) : 15;
          setConflictModal({
            isOpen: true,
            type: 'END_DATE_EXCEEDED',
            title: 'Vượt Quá Quỹ Thời Gian Học Kỳ!',
            message: validation.message,
            maxSessionsSuggested: maxS
          });
        }
      }
    }
  };

  const handleTotalSessionsBlur = (e) => {
    const value = e.target.value;
    let num = parseInt(value) || 0;
    
    if (num < 1) {
      toast.error('Tổng số buổi học phải lớn hơn 0! Đặt lại về mặc định 15.');
      setFormData(prev => ({ ...prev, totalSessions: 15 }));
      return;
    }
    
    if (formData.startDate) {
      const range = getSemesterDateRange(formData.semester, formData.academicYear);
      if (range) {
        const maxSessions = calculateMaxSessions(formData.startDate, range.end, formData.weeklySchedules);
        if (num > maxSessions) {
          toast.error(`Tổng số buổi học vượt quá quỹ thời gian của học kỳ! Đặt lại về tối đa ${maxSessions} buổi.`);
          setFormData(prev => ({ ...prev, totalSessions: maxSessions }));
          return;
        }
      }
    }
  };

  const handleAbsencesBlur = (e) => {
    const value = e.target.value;
    let num = parseInt(value);
    
    if (isNaN(num) || num < 0) {
      toast.error('Số buổi vắng không được nhỏ hơn 0! Đặt lại về 0.');
      setFormData(prev => ({ ...prev, maxAllowedAbsences: 0 }));
      return;
    }
    
    const total = parseInt(formData.totalSessions) || 0;
    const maxAllowed = Math.floor(total * 0.3);
    if (total > 0 && num > maxAllowed) {
      toast.error(`Số buổi vắng vượt quá 30% tổng số buổi học! Đặt lại về tối đa ${maxAllowed} buổi.`);
      setFormData(prev => ({ ...prev, maxAllowedAbsences: maxAllowed }));
      return;
    }
  };

  const addSchedule = () => {
    setFormData(prev => {
      const newSchedules = [...prev.weeklySchedules, { dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }];
      return { ...prev, weeklySchedules: newSchedules };
    });
  };

  const removeSchedule = (index) => {
    if (formData.weeklySchedules.length > 1) {
      setFormData(prev => {
        const newSchedules = prev.weeklySchedules.filter((_, i) => i !== index);
        return { ...prev, weeklySchedules: newSchedules };
      });
    } else {
      toast.error('Phải có ít nhất một lịch học');
    }
  };

  const handleScheduleChange = (index, field, value) => {
    const newSchedules = [...formData.weeklySchedules];
    newSchedules[index][field] = value;
    setFormData({ ...formData, weeklySchedules: newSchedules });
  };

  // Tự động gợi ý startDate khi thêm lịch học đầu tiên
  const handleAddFirstSchedule = () => {
    if (formData.weeklySchedules.length === 0) {
      addSchedule();
      autoAdjustStartDate([{ dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }]);
    } else {
      addSchedule();
    }
  };


  const handleNextStep = () => {
    if (currentStep === 1) {
      if (!formData.name || !formData.courseCode || !formData.classCode) {
        toast.error("Vui lòng nhập đầy đủ Tên môn học, Mã môn học và Mã nhóm lớp!");
        return;
      }
    } else if (currentStep === 2) {
      if (!formData.startDate) {
        toast.error("Vui lòng chọn ngày bắt đầu!");
        return;
      }
      if (startDateError) {
        toast.error("Vui lòng kiểm tra lại ngày bắt đầu!");
        return;
      }
      if (!formData.weeklySchedules || formData.weeklySchedules.length === 0) {
        toast.error("Vui lòng thêm ít nhất một lịch giảng dạy!");
        return;
      }
      if (absencesError) {
        toast.error("Vui lòng kiểm tra lại số buổi vắng cho phép!");
        return;
      }
      // Kiểm tra trùng lịch dạy cá nhân trước
      const personalConflict = checkPersonalScheduleConflict(formData.weeklySchedules);
      if (personalConflict) {
        const availableSlots = getAvailablePersonalTimeSlots(personalConflict.dayOfWeek);
        setConflictModal({
          isOpen: true,
          type: 'PERSONAL_CONFLICT',
          title: 'Trùng Lịch Dạy Của Bạn!',
          message: `Bạn đã có lịch dạy lớp "${personalConflict.class.name}" (${personalConflict.class.code}) vào khung giờ này. Vui lòng chọn khung giờ khác!`,
          conflictingClass: personalConflict.class,
          conflictingSchedule: personalConflict.conflictSchedule,
          suggestedSlots: availableSlots,
          dayOfWeek: personalConflict.dayOfWeek
        });
        toast.error('Trùng lịch dạy cá nhân!');
        return;
      }
    }
    setCurrentStep(prev => Math.min(prev + 1, 4));
  };

  const handlePrevStep = () => {
    setCurrentStep(prev => Math.max(prev - 1, 1));
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    
    // Validate cơ bản
    if (!formData.name || !formData.courseCode) {
      toast.error("Vui lòng nhập Tên lớp và Mã học phần!");
      return;
    }

    // Validate các ràng buộc ngày, học kỳ, và lịch học
    if (!formData.startDate) {
      toast.error("Vui lòng chọn ngày bắt đầu!");
      return;
    }

    const validation = validateDates(
      formData.startDate, 
      formData.weeklySchedules, 
      formData.totalSessions, 
      formData.semester, 
      formData.academicYear
    );
    if (!validation.isValid) {
      toast.error(validation.message);
      setStartDateError(validation.message);
      return;
    }

    // Validate số buổi vắng cho phép (< 0 hoặc > 30% tổng buổi)
    const total = parseInt(formData.totalSessions) || 0;
    const absences = parseInt(formData.maxAllowedAbsences);
    if (isNaN(absences) || absences < 0) {
      toast.error('Số buổi vắng cho phép không được âm (< 0)!');
      setAbsencesError('Số buổi vắng cho phép không được âm (< 0)');
      return;
    }
    if (total > 0 && absences > total * 0.3) {
      const maxAllowed = Math.floor(total * 0.3);
      toast.error(`Số buổi vắng cho phép không được vượt quá 30% tổng số buổi (${maxAllowed} buổi)!`);
      setAbsencesError(`Số buổi vắng cho phép không được vượt quá 30% tổng số buổi (${maxAllowed} buổi)`);
      return;
    }



    setIsSubmitting(true);
    const loadingToastId = toast.loading('Đang khởi tạo lớp học...');

    try {
      const baseCode = formData.classCode || formData.courseCode || "CLASS";
      const uniqueJoinCode = generateSecureJoinCode(baseCode);
      
      const requestPayload = {
        name: formData.name,
        code: formData.code || `${formData.courseCode}-${formData.classCode || '01'}`,
        courseCode: formData.courseCode,
        classCode: formData.classCode,
        joinCode: uniqueJoinCode,
        description: formData.description,
        semester: formData.semester,
        academicYear: formData.academicYear,
        campus: formData.campus,
        room: formData.room,
        startDate: formData.startDate,
        credits: parseInt(formData.credits) || 3,
        totalSessions: parseInt(formData.totalSessions),
        maxAllowedAbsences: parseInt(formData.maxAllowedAbsences),
        weeklySchedules: formData.weeklySchedules.map(schedule => ({
          dayOfWeek: schedule.dayOfWeek,
          startTime: schedule.startTime,
          endTime: schedule.endTime
        })),
        approvalMode: formData.approvalMode,
        allowAutoJoinOnCheckin: formData.allowAutoJoinOnCheckin
      };

      const createdGroup = await classApi.createGroup(requestPayload);
      const groupId = createdGroup?.id || createdGroup?.groupId;

      // Tự động import danh sách sinh viên đã chuẩn bị
      const validPayload = validatedRows
        .filter(row => row.status === 'NEW' || row.status === 'EXISTING')
        .map(row => ({
          studentCode: row.studentCode,
          name: row.name,
          email: row.email
        }));

      if (groupId && validPayload.length > 0) {
        try {
          await classApi.importMembers(groupId, validPayload);
          toast.success(`Đã tự động nhập ${validPayload.length} sinh viên vào lớp!`);
        } catch (importErr) {
          console.warn("Lỗi tự động import sinh viên sau khi tạo nhóm:", importErr);
        }
      }

      localStorage.removeItem('create_class_draft');
      toast.success(`Tạo lớp thành công! Mã tham gia: ${uniqueJoinCode}`, { id: loadingToastId, duration: 6000 });
      
      setTimeout(() => {
        navigate('/classes');
      }, 2000);

    } catch (error) {
      console.error('Lỗi khi tạo lớp:', error);
      
      if (error.response?.status === 422) {
        const errorData = error.response?.data;
        if (errorData?.code === 'START_DATE_NOT_MATCH_SCHEDULE') {
          const suggestions = getSuggestedStartDates(formData.weeklySchedules, formData.semester, formData.academicYear);
          setConflictModal({
            isOpen: true,
            type: 'START_DATE_CONFLICT',
            title: 'Ngày Bắt Đầu Không Khớp Lịch Học!',
            message: `Lỗi: Ngày bắt đầu không khớp với lịch học. Vui lòng chọn lại ngày khớp lịch học.`,
            suggestedDates: suggestions
          });
          toast.error(`Ngày bắt đầu không khớp với lịch học!`, { id: loadingToastId });
        } else {
          toast.error(errorData?.message || 'Dữ liệu không hợp lệ!', { id: loadingToastId });
        }
      } else if (error.response?.status === 400) {
        toast.error("Dữ liệu gửi lên không hợp lệ!", { id: loadingToastId });
      } else if (error.response?.status === 409 || error.status === 409 || error.code === 'SCHEDULE_CONFLICT' || error.message?.toLowerCase().includes("conflict")) {
        const errorData = error.response?.data;
        const errMsg = errorData?.message || "Dữ liệu bị trùng lặp trên hệ thống!";
        setConflictModal({
          isOpen: true,
          type: 'SERVER_CONFLICT',
          title: 'Không Thể Tạo Lớp Học!',
          message: errMsg
        });
        toast.error("Vui lòng kiểm tra chi tiết lỗi!", { id: loadingToastId });
      } else {
        const errorMsg = error.response?.data?.message || 'Có lỗi xảy ra khi giao tiếp với máy chủ.';
        toast.error(`Lỗi tạo lớp: ${errorMsg}`, { id: loadingToastId });
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Helper lya ngay chuan vietnam
  const getVietnameseDayName = (dayValue) => {
    return DAYS_OF_WEEK.find(d => d.value === dayValue)?.vi || dayValue;
  };

  const range = getSemesterDateRange(formData.semester, formData.academicYear);
  const minDateStr = range ? range.start.toISOString().split('T')[0] : '';
  const maxDateStr = range ? range.end.toISOString().split('T')[0] : '';

  return (
    <div className="flex min-h-screen bg-gradient-to-br from-gray-50 to-red-50/30 font-sans">
      <Sidebar onCollapseChange={setIsSidebarCollapsed} />
      
      <main className={`flex-1 flex flex-col min-h-screen relative pb-24 transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        <header className="bg-white/80 backdrop-blur-md border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-10 shadow-sm">
          <div>
            <div className="flex items-center text-sm text-gray-500 mb-1 gap-2">
              <span className="hover:underline cursor-pointer" onClick={() => navigate('/classes')}>Quản lý lớp học</span>
              <span className="text-gray-300">/</span>
              <span className="text-gray-900 font-medium">Tạo lớp mới</span>
            </div>
            <h1 className="text-2xl font-bold bg-gradient-to-r from-gray-900 to-red-900 bg-clip-text text-transparent">
              Tạo lớp học mới
            </h1>
          </div>
          <button className="p-2 border border-gray-200 rounded-xl text-gray-500 hover:bg-gray-50 transition-all shadow-sm">
            <Bell size={18} />
          </button>
        </header>

        <div className="p-8 max-w-[1600px] 2xl:max-w-[1800px] w-full mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* CỘT TRÁI - Form chính */}
          <div className="lg:col-span-8 space-y-6">
            
            {/* STEPPER */}
            <div className="mb-8 animate-in fade-in slide-in-from-top-4 duration-500">
              <h2 className="text-3xl font-extrabold text-[#8B1D1D] mb-1.5 tracking-tight">Tạo Lớp Học Mới</h2>
              <p className="text-sm text-gray-500 mb-10 leading-relaxed">Thiết lập thông tin học thuật để bắt đầu quản lý điểm danh.</p>
              
              <div className="flex items-center justify-between relative max-w-2xl mx-auto px-4">
                <div className="absolute left-4 right-4 top-1/2 -translate-y-1/2 h-[3px] bg-gray-200 rounded-full z-0"></div>
                <div 
                  className="absolute left-4 top-1/2 -translate-y-1/2 h-[3px] bg-red-65 rounded-full z-0 transition-all duration-500 ease-out"
                  style={{ width: `calc(${((currentStep - 1) / 3) * 100}% - ${currentStep === 1 ? 0 : 2}rem)` }}
                ></div>
                
                {[
                  { step: 1, label: 'THÔNG TIN' },
                  { step: 2, label: 'LỊCH GIẢNG' },
                  { step: 3, label: 'SINH VIÊN' },
                  { step: 4, label: 'CÀI ĐẶT' }
                ].map((s) => (
                  <div key={s.step} className="relative z-10 flex flex-col items-center gap-2">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-extrabold transition-all duration-300 ${
                      currentStep === s.step 
                        ? 'bg-[#8B1D1D] text-white shadow-lg shadow-red-700/30 scale-110' 
                        : currentStep > s.step 
                          ? 'bg-[#8B1D1D]/90 text-white' 
                          : 'bg-gray-100 text-gray-400 border-2 border-white shadow-inner'
                    }`}>
                      {currentStep > s.step ? <CheckCircle2 size={20} className="animate-in zoom-in duration-300"/> : s.step}
                    </div>
                    <span className={`text-[10px] font-black tracking-widest whitespace-nowrap transition-colors duration-300 ${currentStep >= s.step ? 'text-[#8B1D1D]' : 'text-gray-400'}`}>
                      {s.label}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {hasDraft && (
              <div className="bg-gradient-to-r from-red-50 to-red-100/30 border-2 border-red-200/60 p-4 rounded-2xl flex items-center justify-between gap-4 shadow-sm animate-fade-in">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center shadow-inner">
                    <FileText size={20} />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-gray-800">Khôi phục bản nháp?</h4>
                    <p className="text-xs text-gray-500 mt-0.5">Hệ thống tìm thấy một bản nháp chưa hoàn thành của bạn.</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button 
                    onClick={handleRestoreDraft}
                    type="button"
                    className="px-3.5 py-1.5 bg-red-600 hover:bg-red-700 text-white rounded-xl text-xs font-semibold shadow-md transition-all hover:scale-[1.02] active:scale-[0.98]"
                  >
                    Khôi phục
                  </button>
                  <button 
                    onClick={handleDiscardDraft}
                    type="button"
                    className="px-3 py-1.5 bg-white hover:bg-gray-50 text-gray-700 border border-gray-200 rounded-xl text-xs font-semibold shadow-sm transition-all"
                  >
                    Bỏ qua
                  </button>
                </div>
              </div>
            )}
            
            {currentStep === 1 && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* 1. Thông tin cơ bản */}
            <div className="bg-white rounded-3xl border border-gray-150 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              {/* Card Header with circular badge */}
              <div className="p-6 border-b border-gray-100 flex items-center gap-3 bg-gradient-to-r from-white to-red-50/10">
                <div className="w-8 h-8 rounded-full bg-[#FCE8E6] text-[#8B1D1D] flex items-center justify-center text-xs font-black border border-[#FAD2CD]">
                  1
                </div>
                <h2 className="text-base font-extrabold text-gray-900">Thông tin cơ bản</h2>
              </div>

              {/* Card Body */}
              <div className="p-6 space-y-6 text-left">
                {/* Tên môn học */}
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                    Tên môn học <span className="text-red-500">*</span>
                  </label>
                  <input 
                    type="text" 
                    name="name" 
                    value={formData.name} 
                    onChange={handleChange} 
                    placeholder="VD: Lập trình hướng đối tượng" 
                    className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-semibold focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 outline-none transition-all bg-white text-gray-800 shadow-sm placeholder:text-gray-400 placeholder:font-normal" 
                  />
                </div>

                {/* Mã môn học & Mã nhóm lớp */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Mã môn học <span className="text-red-500">*</span>
                    </label>
                    <input 
                      type="text" 
                      name="courseCode" 
                      value={formData.courseCode} 
                      onChange={handleChange} 
                      placeholder="VD: INT12345" 
                      className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-semibold focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 outline-none transition-all bg-white text-gray-800 shadow-sm placeholder:text-gray-400 placeholder:font-normal" 
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Mã nhóm lớp <span className="text-red-500">*</span>
                    </label>
                    <input 
                      type="text" 
                      name="classCode" 
                      value={formData.classCode} 
                      onChange={handleChange} 
                      placeholder="VD: D22PM3" 
                      className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-semibold focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 outline-none transition-all bg-white text-gray-800 shadow-sm placeholder:text-gray-400 placeholder:font-normal" 
                    />
                  </div>
                </div>

                {/* Học kỳ & Năm học */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Học kỳ
                    </label>
                    <select 
                      name="semester" 
                      value={formData.semester} 
                      onChange={handleChange} 
                      className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-bold bg-white text-gray-700 outline-none focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 shadow-sm cursor-pointer"
                    >
                      {SEMESTERS.map(s => (
                        <option key={s.value} value={s.value}>{s.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Năm học
                    </label>
                    <select 
                      name="academicYear" 
                      value={formData.academicYear} 
                      onChange={handleChange} 
                      className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-bold bg-white text-gray-700 outline-none focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 shadow-sm cursor-pointer"
                    >
                      {ACADEMIC_YEARS.map(y => (
                        <option key={y.value} value={y.value}>{y.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                {/* Số tín chỉ */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                      Số tín chỉ
                    </label>
                    <select 
                      name="credits" 
                      value={formData.credits} 
                      onChange={handleChange} 
                      className="w-full px-4.5 py-3 border border-gray-250 rounded-2xl text-sm font-bold bg-white text-gray-700 outline-none focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 shadow-sm cursor-pointer"
                    >
                      {CREDITS.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </div>
                  <div className="hidden md:block"></div>
                </div>
              </div>
            </div>
            </div>
            )}
            {currentStep === 2 && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500 space-y-6">
              {/* Tiêu đề & Subtitle của Thiết lập Lịch Giảng Dạy */}
              <div className="flex flex-col md:flex-row md:items-start justify-between gap-4 pt-2">
                <div className="space-y-1.5">
                  <h2 className="text-2xl lg:text-3xl font-extrabold text-gray-900 tracking-tight">
                    Thiết lập Lịch Giảng Dạy
                  </h2>
                  <p className="text-sm text-gray-500 max-w-2xl leading-relaxed">
                    Định cấu hình thời gian và địa điểm cho các buổi học. Sử dụng <span className="font-bold text-gray-800">Trợ lý lịch biểu</span> để tối ưu thời gian.
                  </p>
                </div>

                {/* Sub-tabs toggle */}
                <div className="flex bg-gray-100 p-1.5 rounded-full border border-gray-200 self-start md:self-center shadow-inner shrink-0">
                  <button
                    type="button"
                    onClick={() => setScheduleTab('config')}
                    className={`px-5 py-2.5 text-xs font-bold rounded-full transition-all cursor-pointer active:scale-95 ${
                      scheduleTab === 'config'
                        ? 'bg-white text-gray-900 shadow-sm border border-gray-100'
                        : 'text-gray-500 hover:text-gray-800'
                    }`}
                  >
                    Thiết lập lịch
                  </button>
                  <button
                    type="button"
                    onClick={() => setScheduleTab('free-slots')}
                    className={`px-5 py-2.5 text-xs font-bold rounded-full transition-all cursor-pointer active:scale-95 ${
                      scheduleTab === 'free-slots'
                        ? 'bg-white text-gray-900 shadow-sm border border-gray-100'
                        : 'text-gray-500 hover:text-gray-800'
                    }`}
                  >
                    Thời gian biểu cá nhân
                  </button>
                </div>
              </div>

              {/* Hộp thông báo cảnh báo trùng lịch dạy thời gian thực */}
              {livePersonalConflict && (
                <div className="bg-[#FFF5F5] border border-red-200 rounded-3xl p-5 text-left relative overflow-hidden animate-in fade-in slide-in-from-top-3 duration-300 shadow-sm">
                  <div className="absolute -top-6 -right-6 w-20 h-20 bg-red-100/50 rounded-full blur-xl"></div>
                  <div className="flex gap-4">
                    <div className="w-10 h-10 rounded-2xl bg-red-100 text-[#8B1D1D] flex items-center justify-center shrink-0">
                      <ShieldAlert size={20} className="stroke-[2.5]" />
                    </div>
                    <div className="space-y-2 flex-1">
                      <h4 className="text-sm font-extrabold text-red-900 uppercase tracking-wider flex items-center gap-1.5">
                        Trùng Lịch Dạy Của Chính Bạn!
                      </h4>
                      <p className="text-xs text-red-750 leading-relaxed font-semibold">
                        Bạn đang có lịch giảng dạy trùng với lớp học của chính bạn: <span className="font-extrabold text-red-950 underline">{livePersonalConflict.class.name}</span> (Mã: {livePersonalConflict.class.courseCode}, Nhóm {livePersonalConflict.class.classCode || '01'}).
                      </p>
                      <div className="flex flex-wrap gap-2 pt-1 text-[11px] font-bold">
                        <span className="bg-red-200/50 text-[#8B1D1D] px-2.5 py-1 rounded-lg border border-red-200/80">
                          Khung giờ trùng: {livePersonalConflict.conflictSchedule.startTime} - {livePersonalConflict.conflictSchedule.endTime}
                        </span>
                        <span className="bg-red-200/50 text-[#8B1D1D] px-2.5 py-1 rounded-lg border border-red-200/80">
                          Thứ trong tuần: {DAYS_OF_WEEK.find(d => d.value === livePersonalConflict.dayOfWeek)?.label}
                        </span>
                      </div>
                      <div className="text-xs text-red-800 font-bold bg-white/70 border border-red-100 p-3 rounded-2xl mt-2 leading-relaxed">
                        <p className="mb-1 text-gray-500 font-bold text-[10px] uppercase tracking-wider">💡 ĐỀ XUẤT GIỜ TRỐNG CHO KHUNG GIỜ KHÁC:</p>
                        <span>{getAvailablePersonalTimeSlots(livePersonalConflict.dayOfWeek).map(s => s.label).join(', ')}</span>
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* VIEW 1: THIẾT LẬP LỊCH (MANUAL CONFIG) */}
              {scheduleTab === 'config' && (
                <div className="space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-300">
                  {/* Thông tin chung: Ngày bắt đầu, Tổng số buổi, Số buổi vắng */}
                  <div className="bg-white rounded-3xl p-6 border border-gray-150 shadow-sm grid grid-cols-1 md:grid-cols-3 gap-5 text-left">
                    <div>
                      <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                        Ngày bắt đầu <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-gray-400">
                          <Calendar size={16} />
                        </span>
                        <input 
                          type="date" 
                          name="startDate" 
                          value={formData.startDate} 
                          onChange={handleStartDateChange} 
                          min={minDateStr}
                          max={maxDateStr}
                          className={`w-full pl-9 pr-4 py-2.5 border rounded-xl text-sm font-semibold outline-none focus:border-red-400 focus:ring-4 focus:ring-red-50 transition-all shadow-sm ${
                            startDateError 
                              ? 'border-red-400 focus:border-red-400 focus:ring-red-50' 
                              : 'border-gray-200 focus:border-[#8B1D1D] focus:ring-red-50'
                          }`}
                        />
                      </div>
                      {startDateError && (
                        <div className="mt-1.5 flex items-center gap-1 text-xs text-red-600">
                          <AlertCircle size={12} />
                          <span>{startDateError}</span>
                        </div>
                      )}
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                        Tổng số buổi <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-gray-400">
                          <FileText size={16} />
                        </span>
                        <input 
                          type="number" 
                          name="totalSessions" 
                          value={formData.totalSessions} 
                          onChange={handleChange} 
                          onBlur={handleTotalSessionsBlur}
                          className="w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm font-semibold focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 outline-none transition-all shadow-sm bg-white" 
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">
                        Số buổi vắng tối đa <span className="text-red-500">*</span>
                      </label>
                      <div className="relative">
                        <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-gray-400">
                          <AlertCircle size={16} />
                        </span>
                        <input 
                          type="number" 
                          name="maxAllowedAbsences" 
                          value={formData.maxAllowedAbsences} 
                          onChange={handleChange} 
                          onBlur={handleAbsencesBlur}
                          className={`w-full pl-9 pr-4 py-2.5 border rounded-xl text-sm font-semibold focus:ring-4 outline-none transition-all shadow-sm bg-white ${
                            absencesError 
                              ? 'border-red-400 focus:border-red-400 focus:ring-red-50' 
                              : 'border-gray-200 focus:border-[#8B1D1D] focus:ring-red-50'
                          }`}
                        />
                      </div>
                      {absencesError && (
                        <div className="mt-1.5 flex items-center gap-1 text-xs text-red-600">
                          <AlertCircle size={12} />
                          <span>{absencesError}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Danh sách các buổi học (Session Cards) */}
                  <div className="space-y-4">
                    {formData.weeklySchedules.map((schedule, idx) => (
                      <div 
                        key={idx} 
                        className="bg-white rounded-3xl p-6 border border-gray-150 shadow-sm hover:shadow-md transition-shadow relative space-y-6 animate-in fade-in slide-in-from-bottom-2 duration-300 text-left"
                      >
                        {/* Header của Buổi học */}
                        <div className="flex items-center justify-between pb-3 border-b border-gray-100">
                          <div className="flex items-center gap-3">
                            <div className="w-11 h-11 rounded-2xl bg-red-55 text-[#8B1D1D] flex items-center justify-center border border-red-100 shadow-sm">
                              <Calendar size={18} className="stroke-[2.5]" />
                            </div>
                            <div>
                              <span className="text-base font-extrabold text-gray-900 block">Buổi học {idx + 1}</span>
                              <span className="text-[9px] text-gray-400 font-extrabold tracking-widest block uppercase mt-0.5">
                                {idx === 0 ? 'CƠ BẢN' : 'BỔ SUNG'}
                              </span>
                            </div>
                          </div>

                          {formData.weeklySchedules.length > 1 && (
                            <button
                              type="button"
                              onClick={() => removeSchedule(idx)}
                              className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all cursor-pointer active:scale-95"
                            >
                              <Trash2 size={16} />
                            </button>
                          )}
                        </div>

                        {/* Custom Weekday Selector (Monday to Saturday as shown in the design) */}
                        <div>
                          <label className="block text-xs font-bold text-gray-400 uppercase tracking-widest mb-3">Ngày học trong tuần</label>
                          <div className="flex flex-wrap gap-2.5">
                            {DAYS_OF_WEEK.slice(0, 6).map((day) => {
                              const isActive = schedule.dayOfWeek === day.value;
                              return (
                                <button
                                  key={day.value}
                                  type="button"
                                  onClick={() => handleScheduleChange(idx, 'dayOfWeek', day.value)}
                                  className={`px-5 py-2.5 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer active:scale-95 border ${
                                    isActive
                                      ? 'bg-[#8B1D1D] text-white border-[#8B1D1D] shadow-sm'
                                      : 'bg-white text-gray-700 border-gray-200 hover:bg-gray-50'
                                  }`}
                                >
                                  {day.label}
                                </button>
                              );
                            })}
                          </div>
                        </div>

                        {/* Thời gian & Địa điểm (Time Pickers & Room Input) */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
                          <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase tracking-widest mb-2">Giờ bắt đầu</label>
                            <CustomTimePicker
                              value={schedule.startTime}
                              onChange={(val) => handleScheduleChange(idx, 'startTime', val)}
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase tracking-widest mb-2">Giờ kết thúc</label>
                            <CustomTimePicker
                              value={schedule.endTime}
                              onChange={(val) => handleScheduleChange(idx, 'endTime', val)}
                            />
                          </div>

                          <div>
                            <label className="block text-xs font-bold text-gray-400 uppercase tracking-widest mb-2">Phòng học</label>
                            <div className="relative">
                              <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-gray-400">
                                <MapPin size={16} />
                              </span>
                              <input
                                type="text"
                                value={schedule.room || ''}
                                onChange={(e) => handleScheduleChange(idx, 'room', e.target.value)}
                                placeholder="VD: P.302-A2"
                                className="w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-xl text-sm font-semibold outline-none focus:border-[#8B1D1D] focus:ring-4 focus:ring-red-50 text-gray-700 shadow-sm hover:border-gray-300 transition-all bg-white"
                              />
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}

                    {/* Dotted Schedule Adder */}
                    <button
                      type="button"
                      onClick={handleAddFirstSchedule}
                      className="w-full py-7 border-2 border-dashed border-red-200/60 hover:border-[#8B1D1D] rounded-3xl text-gray-500 hover:text-[#8B1D1D] bg-white flex flex-col items-center justify-center gap-3 cursor-pointer transition-all hover:bg-red-50/10 active:scale-98 group shadow-sm"
                    >
                      <div className="w-12 h-12 rounded-full border border-gray-300 group-hover:border-[#8B1D1D] text-gray-400 group-hover:text-[#8B1D1D] flex items-center justify-center transition-all bg-gray-50/50">
                        <Plus size={22} className="stroke-[2] group-hover:rotate-90 transition-transform duration-300" />
                      </div>
                      <span className="text-xs font-black tracking-widest text-gray-600 group-hover:text-[#8B1D1D] uppercase">THÊM BUỔI HỌC KHÁC</span>
                    </button>
                  </div>
                </div>
              )}

              {/* VIEW 2: THỜI GIAN BIỂU CÁ NHÂN (FREE SLOTS / BUSY SLOTS TIMETABLE) */}
              {scheduleTab === 'free-slots' && (
                <div className="space-y-4 animate-in fade-in slide-in-from-bottom-2 duration-300">
                  <div className="bg-[#EBF5FF] border border-[#BFDBFE] rounded-3xl p-5 text-left flex gap-3.5 relative overflow-hidden shadow-sm">
                    <div className="w-10 h-10 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center shrink-0">
                      <Info size={20} className="stroke-[2.5]" />
                    </div>
                    <div className="space-y-1">
                      <p className="text-xs font-black text-blue-900 uppercase tracking-widest">Trợ lý lịch biểu của bạn</p>
                      <p className="text-xs text-blue-750 font-semibold leading-relaxed">
                        Danh sách các ca dạy trong tuần hiện có của bạn. Hãy thiết lập các buổi học vào khung giờ trống (màu xanh lá) để tránh bị cảnh báo trùng lịch.
                      </p>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 gap-4">
                    {DAYS_OF_WEEK.slice(0, 6).map((day) => {
                      // Lookup classes of teacher on this dayOfWeek
                      const busySlots = [];
                      existingClasses.forEach((c) => {
                        if (c.weeklySchedules) {
                          c.weeklySchedules.forEach((s) => {
                            if (s.dayOfWeek === day.value) {
                              busySlots.push({
                                className: c.name,
                                courseCode: c.courseCode,
                                room: s.room || 'P.302-A2',
                                time: `${s.startTime} - ${s.endTime}`
                              });
                            }
                          });
                        }
                      });

                      return (
                        <div 
                          key={day.value} 
                          className="bg-white rounded-3xl border border-gray-150 p-5 shadow-sm space-y-3.5 text-left hover:shadow-md transition-shadow"
                        >
                          <div className="flex justify-between items-center pb-2.5 border-b border-gray-100">
                            <span className="text-sm font-extrabold text-gray-900">{day.label}</span>
                            <span className={`text-[10px] font-black px-2.5 py-0.5 rounded-lg border uppercase tracking-wider ${
                              busySlots.length > 0
                                ? 'bg-amber-50 text-amber-700 border-amber-200'
                                : 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            }`}>
                              {busySlots.length > 0 ? `${busySlots.length} ca dạy bận` : 'Cả ngày trống ✨'}
                            </span>
                          </div>

                          {busySlots.length > 0 ? (
                            <div className="space-y-2.5">
                              {busySlots.map((slot, i) => (
                                <div 
                                  key={i} 
                                  className="flex justify-between items-center bg-[#FDF2F2]/60 border border-red-50 p-3 rounded-2xl text-xs font-semibold hover:bg-[#FDF2F2] transition-colors"
                                >
                                  <div className="space-y-1">
                                    <p className="text-gray-900 font-extrabold leading-snug">{slot.className} <span className="text-gray-400 font-bold text-[10px]">({slot.courseCode})</span></p>
                                    <p className="text-gray-400 font-bold text-[10px] flex items-center gap-1 mt-0.5"><MapPin size={11} /> {slot.room}</p>
                                  </div>
                                  <span className="text-xs font-black text-[#8B1D1D] bg-white border border-red-100 px-3 py-1 rounded-xl shadow-sm tracking-tight font-mono">{slot.time}</span>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <p className="text-xs text-gray-400 italic pl-1">Không có lịch giảng dạy nào vào ngày này. Bạn có thể tự do lên lịch mới!</p>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
            )}

            {currentStep === 4 && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* 3. Cài đặt điểm danh (giữ nguyên) */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              <div className="p-6 border-b border-gray-100 flex items-center gap-3 bg-gradient-to-r from-white to-red-50/20">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-800 text-white flex items-center justify-center text-sm font-bold shadow-md">
                  3
                </div>
                <h2 className="text-lg font-bold text-gray-900">Cài đặt điểm danh</h2>
              </div>
              <div className="p-6 space-y-6">
                {/* ... giữ nguyên phần cài đặt điểm danh như cũ ... */}
                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Chế độ duyệt sinh viên
                    </label>
                    <select 
                      name="approvalMode" 
                      value={formData.approvalMode} 
                      onChange={handleChange} 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-4 focus:ring-red-50"
                    >
                      <option value="AUTO">Tự động (Vào lớp ngay)</option>
                      <option value="MANUAL">Thủ công (Cần phê duyệt)</option>
                    </select>
                  </div>
                  <div className="flex items-center mt-6">
                    <label className="flex items-center gap-3 cursor-pointer">
                      <input 
                        type="checkbox" 
                        name="allowAutoJoinOnCheckin" 
                        checked={formData.allowAutoJoinOnCheckin} 
                        onChange={handleChange} 
                        className="w-5 h-5 text-red-600 rounded-lg border-gray-300 focus:ring-red-500 focus:ring-offset-0" 
                      />
                      <span className="text-sm font-semibold text-gray-700">
                        Tự động tham gia khi điểm danh lần đầu
                      </span>
                    </label>
                  </div>
                </div>
              </div>
            </div>

            </div>
            )}
            {currentStep === 3 && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            {/* 4. Danh sách sinh viên */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow mb-8">
              <div className="p-6 border-b border-gray-100 flex items-center justify-between bg-gradient-to-r from-white to-red-50/20">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-800 text-white flex items-center justify-center text-sm font-bold shadow-md">
                    4
                  </div>
                  <h2 className="text-lg font-bold text-gray-900">Danh sách sinh viên</h2>
                </div>
                {importStep === 2 && !isValidating && (
                  <button
                    onClick={handleRemoveFile}
                    className="flex items-center gap-1.5 px-3.5 py-1.5 border border-red-200 hover:border-red-300 bg-red-50 hover:bg-red-100 text-red-600 hover:text-red-700 font-bold rounded-lg text-xs transition-all"
                  >
                    <Trash2 size={13} />
                    Xóa file & Chọn lại
                  </button>
                )}
              </div>
              <div className="p-6">
                
                {/* STEP 1: UPLOAD FILE PANEL */}
                {importStep === 1 && (
                  <div className="flex flex-col items-center justify-center py-4">
                    {/* Vùng giao diện - Chứa input bên trong một cách hợp lệ */}
                    <label className="w-full max-w-lg border-2 border-dashed border-gray-300 hover:border-red-500 bg-white rounded-2xl p-8 flex flex-col items-center justify-center text-center cursor-pointer transition-all hover:bg-red-50/5 group shadow-sm">
                      <input 
                        type="file" 
                        accept=".xlsx,.xls,.csv" 
                        onChange={handleFileUpload}
                        onClick={(e) => { e.target.value = null }} // Reset để chọn lại file cũ
                        className="hidden" // Input bị ẩn nhưng vẫn nhận sự kiện từ Label
                      />
                      <div className="w-14 h-14 rounded-full bg-red-50 text-red-600 flex items-center justify-center mb-4 transition-transform group-hover:scale-110 duration-300 shadow-inner">
                        <Upload size={24} />
                      </div>
                      <p className="text-sm font-bold text-gray-800 mb-1">
                        Kéo thả file Excel hoặc nhấp để chọn file
                      </p>
                      <p className="text-xs text-gray-400">
                        Chấp nhận file định dạng .xlsx, .xls, .csv (Tối đa 10MB)
                      </p>
                    </label>

                    <div className="my-5 flex items-center justify-center w-full max-w-md">
                      <div className="h-[1px] bg-gray-200 flex-1" />
                      <span className="px-4 text-xs font-bold text-gray-400 uppercase tracking-wider">Hoặc sử dụng dữ liệu mẫu</span>
                      <div className="h-[1px] bg-gray-200 flex-1" />
                    </div>

                    <div className="flex flex-col md:flex-row gap-4 justify-center items-center w-full max-w-lg shrink-0">
                      <button 
                        type="button"
                        onClick={handleLoadSampleData}
                        className="flex items-center gap-2 px-5 py-3 border border-red-200 bg-red-50/50 hover:bg-red-50 text-red-600 hover:text-red-700 font-bold rounded-xl text-sm transition-all active:scale-[0.98] shadow-sm shadow-red-50/50"
                      >
                        <Sparkles size={16} className="animate-pulse" />
                        Nạp dữ liệu thử nghiệm ⚡
                      </button>
                      
                      <button 
                        type="button"
                        onClick={() => {
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
                    <div className="w-full max-w-lg mt-6 p-4 bg-blue-50/60 border border-blue-100 rounded-xl text-left flex gap-3">
                      <Info size={18} className="text-blue-500 shrink-0 mt-0.5" />
                      <div>
                        <h4 className="text-xs font-bold text-blue-950 uppercase tracking-wide">Yêu cầu cấu trúc cột trong file:</h4>
                        <p className="text-xs text-blue-900 leading-relaxed mt-1">
                          File Excel cần chứa đúng 3 cột tiêu đề sau tại dòng đầu tiên:
                          <br />
                          • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">studentCode</strong> : Mã sinh viên (Bắt buộc).
                          <br />
                          • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">name</strong> : Họ và tên sinh viên.
                          <br />
                          • <strong className="font-mono text-[11px] text-blue-950 bg-blue-100 px-1 py-0.5 rounded">email</strong> : Địa chỉ Email.
                        </p>
                      </div>
                    </div>
                  </div>
                )}

                {/* STEP 2: VALIDATION & PREVIEW PANEL */}
                {importStep === 2 && (
                  <div className="flex flex-col h-full min-h-[250px]">
                    
                    {isValidating ? (
                      <div className="flex flex-col items-center justify-center flex-1 py-10">
                        <Loader2 className="animate-spin text-red-600 mb-4 animate-duration-1000" size={36} />
                        <h4 className="text-sm font-bold text-gray-800">Đang đối chiếu dữ liệu sinh viên...</h4>
                        <p className="text-xs text-gray-400 mt-1">Hệ thống đang mô phỏng chạy thử và kiểm tra định dạng dòng</p>
                      </div>
                    ) : (
                      <>
                        {/* File details Header */}
                        {selectedFile && (
                          <div className="mb-4 px-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl flex justify-between items-center text-xs">
                            <span className="text-gray-500 font-medium flex items-center gap-1.5">
                              <FileText size={14} className="text-gray-400" />
                              Tệp đã tải lên: <strong className="text-gray-800">{selectedFile.name}</strong> ({selectedFile.size})
                            </span>
                            <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 font-bold rounded-md">Đã nạp</span>
                          </div>
                        )}

                        {/* Summary Statistics Dashboard */}
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
                          <div className="bg-white border border-gray-150 rounded-xl p-3 shadow-sm text-left">
                            <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">Tổng số dòng</span>
                            <span className="text-xl font-black text-gray-900 block mt-0.5">{totalImport}</span>
                          </div>
                          <div className="bg-emerald-50/50 border border-emerald-100 rounded-xl p-3 shadow-sm text-left">
                            <span className="text-[10px] font-bold text-emerald-500 uppercase tracking-wider block">Khớp học viên</span>
                            <span className="text-xl font-black text-emerald-600 block mt-0.5">{existingCount}</span>
                          </div>
                          <div className="bg-blue-50/50 border border-blue-100 rounded-xl p-3 shadow-sm text-left">
                            <span className="text-[10px] font-bold text-blue-500 uppercase tracking-wider block">Sẽ tạo mới</span>
                            <span className="text-xl font-black text-blue-600 block mt-0.5">{newCount}</span>
                          </div>
                          <div className="bg-red-50/50 border border-red-100 rounded-xl p-3 shadow-sm text-left">
                            <span className="text-[10px] font-bold text-red-500 uppercase tracking-wider block">Dòng lỗi</span>
                            <span className="text-xl font-black text-red-600 block mt-0.5">{errorCount}</span>
                          </div>
                        </div>

                        {/* Error Notice Alert Box */}
                        {errorCount > 0 && (
                          <div className="mb-4 p-3 bg-amber-50 border border-amber-100 rounded-xl text-left flex gap-2 items-start">
                            <AlertTriangle className="text-amber-500 shrink-0 mt-0.5" size={14} />
                            <p className="text-xs text-amber-800 leading-relaxed">
                              Phát hiện <strong className="text-amber-950">{errorCount} dòng lỗi</strong>. Khi bấm "Tạo lớp học", hệ thống sẽ **tự động bỏ qua các dòng đỏ** và chỉ nhập các sinh viên hợp lệ.
                            </p>
                          </div>
                        )}

                        {/* Preview Table */}
                        <div className="bg-white border border-gray-150 rounded-xl overflow-hidden shadow-sm max-h-[220px] overflow-y-auto mb-4">
                          <table className="w-full text-left text-xs border-collapse">
                            <thead className="bg-gray-50 border-b border-gray-150 font-bold text-gray-500 uppercase tracking-wider sticky top-0 z-20">
                              <tr>
                                <th className="p-2.5 w-10 text-center">STT</th>
                                <th className="p-2.5">Mã Sinh Viên</th>
                                <th className="p-2.5">Họ và Tên</th>
                                <th className="p-2.5">Email</th>
                                <th className="p-2.5 w-32">Phân Loại</th>
                                <th className="p-2.5">Ghi chú / Lỗi</th>
                              </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                              {validatedRows.map((row, index) => {
                                let rowClass = 'hover:bg-gray-50';
                                let badgeClass = '';
                                let badgeText = '';
                                let note = '';

                                if (row.status === 'EXISTING') {
                                  rowClass = 'bg-emerald-50/10 hover:bg-emerald-50/20';
                                  badgeClass = 'bg-emerald-100 text-emerald-800 border border-emerald-150 font-bold';
                                  badgeText = 'Khớp sẵn';
                                  note = 'Đã có tài khoản. Sẽ tự động ghi danh vào lớp học này.';
                                } else if (row.status === 'NEW') {
                                  rowClass = 'bg-blue-50/10 hover:bg-blue-50/25';
                                  badgeClass = 'bg-blue-100 text-blue-800 border border-blue-150 font-bold';
                                  badgeText = 'Tạo mới';
                                  note = 'Tài khoản ma. Mật khẩu mặc định: MSV + tên (không dấu).';
                                } else if (row.status === 'ERROR') {
                                  rowClass = 'bg-red-50/15 hover:bg-red-50/30';
                                  badgeClass = 'bg-red-100 text-red-800 border border-red-150 font-bold';
                                  badgeText = 'Lỗi dòng';
                                  note = row.errorMsg || 'Sai định dạng dữ liệu';
                                }

                                return (
                                  <tr key={index} className={`transition-colors ${rowClass}`}>
                                    <td className="p-2.5 text-center text-gray-400 font-semibold">{index + 1}</td>
                                    <td className="p-2.5 font-mono font-bold text-gray-900">{row.studentCode || <span className="text-red-500 italic font-bold">Rỗng</span>}</td>
                                    <td className="p-2.5 font-semibold text-gray-800">{row.name || <span className="text-gray-400 italic">Rỗng</span>}</td>
                                    <td className="p-2.5 text-gray-600 font-medium">{row.email || <span className="text-gray-400 italic">Rỗng</span>}</td>
                                    <td className="p-2.5">
                                      <span className={`px-2 py-0.5 rounded text-[9px] uppercase font-bold inline-block tracking-wide ${badgeClass}`}>
                                        {badgeText}
                                      </span>
                                    </td>
                                    <td className={`p-2.5 text-[11px] font-medium ${row.status === 'ERROR' ? 'text-red-600 font-bold' : 'text-gray-500'}`}>
                                      {row.status === 'ERROR' ? (
                                        <span className="flex items-center gap-1">
                                          <AlertCircle size={11} className="shrink-0 animate-pulse text-red-500" />
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

                        {/* Security default password strategy notice box */}
                        {newCount > 0 && (
                          <div className="p-4 bg-blue-50/60 border border-blue-100 rounded-xl text-left flex gap-3 text-xs leading-relaxed text-blue-900 mb-2">
                            <Info size={16} className="text-blue-500 shrink-0 mt-0.5" />
                            <div>
                              <strong className="text-blue-950 block mb-1">Chính sách bảo mật tài khoản tự tạo:</strong>
                              Các sinh viên chưa có tài khoản trên hệ thống sẽ được tự động đăng ký dạng tài khoản ma. Mật khẩu mặc định là: <code className="font-bold text-red-600 bg-red-50 px-1 py-0.5 rounded">Mã sinh viên + Tên không dấu</code> (Ví dụ: `N22DCCN160phamvanphu`). Sinh viên bắt buộc phải thực hiện cập nhật lại mật khẩu cá nhân mới ngay trong lần đăng nhập đầu tiên.
                            </div>
                          </div>
                        )}

                        {/* Completely blocked empty state warning */}
                        {!canConfirmImport && (
                          <div className="p-3 bg-red-50 border border-red-100 rounded-xl text-left flex gap-2 items-start">
                            <AlertCircle className="text-red-500 shrink-0 mt-0.5" size={14} />
                            <p className="text-xs text-red-800">
                              <strong>Lỗi dữ liệu:</strong> Không tìm thấy học viên hợp lệ nào trong tệp này để có thể import. Vui lòng bấm nút chọn tệp khác hoặc điều chỉnh lại!
                            </p>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                )}

              </div>
            </div>
            </div>
            )}
          </div>

          {/* CỘT PHẢI - Summary Sidebar */}
          <div className="lg:col-span-4">
            <div className="bg-transparent rounded-2xl shadow-none p-0 sticky top-24">
              
              {currentStep === 1 && (
                <div className="animate-in fade-in zoom-in-95 duration-300">
                  {/* Live Preview capsule indicator */}
                  <div className="flex items-center justify-center mb-5">
                    <div className="flex items-center gap-1.5 bg-[#FDF2F2] px-3.5 py-1.5 rounded-full border border-red-150 text-[10px] font-black text-[#8B1D1D] uppercase tracking-widest shadow-sm">
                      <span className="w-1.5 h-1.5 rounded-full bg-red-600 animate-ping"></span>
                      ✦ LIVE PREVIEW
                    </div>
                  </div>

                  {/* Card Container */}
                  <div className="rounded-3xl overflow-hidden shadow-2xl shadow-red-100/40 border border-gray-200/50 bg-white">
                    {/* Solid Maroon Header */}
                    <div className="bg-[#8B1D1D] p-6 text-white text-left relative overflow-hidden">
                      <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/5 rounded-full blur-2xl"></div>
                      <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-red-600/20 rounded-full blur-2xl"></div>
                      
                      <div className="relative z-10">
                        <div className="flex justify-between items-center mb-4">
                          <span className="text-[10px] font-bold text-white/80 uppercase tracking-widest">
                            {(formData.courseCode || 'INT12345') + ' • ' + (formData.classCode || 'D22PM3')}
                          </span>
                          <span className="bg-white/15 backdrop-blur-md px-3 py-1 rounded-lg text-[9px] font-black uppercase tracking-wider text-white border border-white/10 flex items-center gap-1">
                            ✦ DỰ THẢO
                          </span>
                        </div>
                        <h3 className="text-xl font-extrabold mb-3 leading-tight tracking-tight line-clamp-2">
                          {formData.name || 'Lập trình hướng đối tượng'}
                        </h3>
                        <p className="text-white/60 text-[11px] font-bold tracking-wide">
                          {SEMESTERS.find(s => s.value === formData.semester)?.label || 'Học kỳ 2'} • {formData.academicYear || '2024 - 2025'}
                        </p>
                      </div>
                    </div>

                    {/* Card Body */}
                    <div className="p-5 space-y-4 text-left bg-white">
                      {/* Lịch học dự kiến */}
                      <div className="flex gap-3.5 items-center p-3 rounded-2xl hover:bg-gray-50 transition-colors">
                        <div className="w-10 h-10 rounded-full bg-red-50 text-[#8B1D1D] flex items-center justify-center border border-red-100 shrink-0">
                          <Clock size={18} className="stroke-[2.5]" />
                        </div>
                        <div>
                          <p className="text-xs font-bold text-gray-500 uppercase tracking-wider">Lịch học dự kiến</p>
                          <p className="text-xs font-black text-gray-900 mt-0.5">
                            {formData.weeklySchedules[0] 
                              ? `${DAYS_OF_WEEK.find(d => d.value === formData.weeklySchedules[0].dayOfWeek)?.label}, ${formData.weeklySchedules[0].startTime} - ${formData.weeklySchedules[0].endTime}` 
                              : 'Thứ 2, Kip 1'}
                            {` • ${formData.room || 'Phòng 402-A2'}`}
                          </p>
                        </div>
                      </div>

                      {/* Sĩ số lớp */}
                      <div className="flex gap-3.5 items-center p-3 rounded-2xl hover:bg-gray-50 transition-colors">
                        <div className="w-10 h-10 rounded-full bg-red-50 text-[#8B1D1D] flex items-center justify-center border border-red-100 shrink-0">
                          <Users size={18} className="stroke-[2.5]" />
                        </div>
                        <div>
                          <p className="text-xs font-bold text-gray-500 uppercase tracking-wider">Sĩ số lớp</p>
                          <p className="text-xs font-black text-gray-400 mt-0.5 flex items-center gap-1">
                            Chưa có sinh viên nào <span className="text-[10px]">👁️‍🗨️</span>
                          </p>
                        </div>
                      </div>

                      {/* Footer Badge Lineup */}
                      <div className="flex items-center justify-between pt-4 border-t border-gray-100">
                        <div className="flex items-center gap-2">
                          <span className="bg-[#8B1D1D] text-white text-[10px] font-black px-3 py-1.5 rounded-xl">
                            {CREDITS.find(c => c.value === formData.credits)?.label || '3 Tín chỉ'}
                          </span>
                          <span className="bg-gray-50 text-gray-600 text-[10px] font-bold px-3 py-1.5 rounded-xl border border-gray-200">
                            {formData.campus || 'PTIT - Cơ sở 1'}
                          </span>
                        </div>
                        
                        {/* Student avatar indicators */}
                        <div className="flex items-center">
                          <div className="flex -space-x-2">
                            <div className="w-6 h-6 rounded-full bg-gray-250 border-2 border-white shadow-sm"></div>
                            <div className="w-6 h-6 rounded-full bg-gray-300 border-2 border-white shadow-sm"></div>
                          </div>
                          <span className="text-[10px] text-gray-400 font-bold ml-1.5">+0</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Footnote instruction */}
                  <p className="text-center text-[10px] text-gray-400 font-semibold mt-5 leading-relaxed bg-[#F8F9FA]/80 py-2.5 rounded-2xl border border-gray-150 px-4">
                    Đây là giao diện thẻ lớp học sẽ hiển thị trên <span className="text-[#8B1D1D] font-bold">UniAttend Dashboard</span> của bạn ✨
                  </p>
                </div>
              )}

              {currentStep === 2 && (
                <div className="animate-in fade-in zoom-in-95 duration-300 rounded-3xl border border-gray-200/60 shadow-xl overflow-hidden bg-white">
                  {/* Banner header BẢN XEM TRƯỚC */}
                  <div className="bg-[#8B1D1D] p-6 text-white relative overflow-hidden">
                    <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/5 rounded-full blur-2xl"></div>
                    <div className="absolute -bottom-8 -left-8 w-32 h-32 bg-red-600/20 rounded-full blur-2xl"></div>
                    <div className="relative z-10">
                      <div className="text-[10px] font-bold text-white/70 uppercase tracking-widest mb-3 flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-white/80 animate-ping"></span>
                        ✦ BÀN XEM TRƯỚC
                      </div>
                      <h3 className="text-xl font-extrabold mb-4 leading-tight tracking-tight line-clamp-2">
                        {formData.name || 'Kiến trúc Máy tính'}
                      </h3>
                      <div className="flex flex-wrap gap-2">
                        <span className="bg-white/15 backdrop-blur-md px-3 py-1 rounded-lg text-[10px] font-bold border border-white/10 tracking-wide text-white">
                          Nhóm: {formData.classCode || '01'}
                        </span>
                        <span className="bg-white/15 backdrop-blur-md px-3 py-1 rounded-lg text-[10px] font-bold border border-white/10 tracking-wide text-white">
                          {formData.courseCode || 'INT1340_01'}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="p-5 space-y-4 bg-[#F8F9FA]/40 text-left">
                    {/* Giảng viên block */}
                    <div className="flex items-center gap-3 p-4 bg-white border border-gray-100 rounded-2xl shadow-sm">
                      <div className="w-11 h-11 rounded-full bg-red-50 text-[#8B1D1D] flex items-center justify-center border border-red-100">
                        <GraduationCap size={22} className="stroke-[2]" />
                      </div>
                      <div className="space-y-0.5">
                        <p className="text-[9px] text-gray-400 font-extrabold uppercase tracking-widest">GIẢNG VIÊN</p>
                        <p className="text-sm font-bold text-gray-900">TS. Nguyễn Văn A</p>
                      </div>
                    </div>

                    {/* Parameter badges */}
                    <div className="flex flex-wrap gap-2.5">
                      <div className="flex items-center gap-1.5 px-3 py-2 bg-[#FCE8E6] border border-[#FAD2CD] rounded-xl text-xs font-bold text-[#A8201A]">
                        <Calendar size={13} />
                        <span>{formData.startDate ? new Date(formData.startDate).toLocaleDateString('vi-VN') : '15/01/2024'}</span>
                      </div>
                      <div className="flex items-center gap-1.5 px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-bold text-gray-700">
                        <span className="text-gray-400 font-mono font-bold">#</span>
                        <span>{formData.totalSessions || 15} buổi</span>
                      </div>
                      <div className="flex items-center gap-1.5 px-3 py-2 bg-amber-50 border border-amber-200 rounded-xl text-xs font-bold text-amber-800">
                        <AlertCircle size={13} className="text-amber-600" />
                        <span>Vắng tối đa: {formData.maxAllowedAbsences || 3}</span>
                      </div>
                    </div>

                    {/* Lịch dự kiến list */}
                    <div className="pt-4 border-t border-gray-200/70 space-y-3">
                      <div className="flex justify-between items-center mb-1">
                        <h4 className="font-bold text-gray-900 flex items-center gap-2 text-sm">
                          <Calendar size={16} className="text-[#8B1D1D]" />
                          <span>Lịch dự kiến</span>
                        </h4>
                        <span className="text-[9px] text-[#8B1D1D] bg-[#FDF2F2] border border-[#FDE8E8] px-2.5 py-0.5 rounded-lg font-bold">
                          {SEMESTERS.find(s => s.value === formData.semester)?.label || 'HỌC KỲ 1'}
                        </span>
                      </div>

                      {formData.weeklySchedules && formData.weeklySchedules.length > 0 ? (
                        formData.weeklySchedules.map((s, idx) => {
                          const dayLabel = DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.label || 'Thứ 2';
                          // Day color coding (Monday = Red, Wednesday = Blue, Friday = Purple, etc.)
                          let dayBg = 'bg-[#FDF2F2] text-[#8B1D1D] border-[#FDE8E8]';
                          if (s.dayOfWeek === 'WEDNESDAY') dayBg = 'bg-[#EFF6FF] text-[#1E40AF] border-[#DBEAFE]';
                          if (s.dayOfWeek === 'FRIDAY') dayBg = 'bg-[#F5F3FF] text-[#5B21B6] border-[#EDE9FE]';
                          if (s.dayOfWeek === 'TUESDAY') dayBg = 'bg-[#ECFDF5] text-[#065F46] border-[#D1FAE5]';

                          return (
                            <div 
                              key={idx} 
                              className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm hover:shadow-md transition-shadow flex justify-between items-center"
                            >
                              <div className="space-y-2 text-left">
                                <span className={`px-2.5 py-1 rounded-lg text-xs font-bold border ${dayBg}`}>
                                  {dayLabel}
                                </span>
                                <div className="text-[10px] font-bold text-gray-400 flex items-center gap-1 mt-1">
                                  <MapPin size={11} />
                                  <span>{s.room || formData.room || 'P.302-A2'}</span>
                                </div>
                              </div>
                              <div className="flex items-center gap-2">
                                <div className="w-8 h-8 rounded-full bg-gray-50 flex items-center justify-center text-gray-400">
                                  <Clock size={14} />
                                </div>
                                <span className="text-base font-extrabold text-gray-800 tracking-tight font-mono">
                                  {s.startTime} - {s.endTime}
                                </span>
                              </div>
                            </div>
                          );
                        })
                      ) : (
                        <p className="text-xs text-gray-400 text-center py-4 italic">Chưa cấu hình lịch giảng dạy nào.</p>
                      )}
                    </div>
                  </div>
                  <div className="bg-gray-50 p-4 border-t border-gray-100">
                    <p className="text-center text-[10px] text-gray-400 font-semibold italic">
                      "Lịch này sẽ được áp dụng cho toàn bộ học kỳ."
                    </p>
                  </div>
                </div>
              )}

              {currentStep === 3 && (
                <div className="animate-in fade-in zoom-in-95 duration-300">
                  <div className="rounded-3xl overflow-hidden shadow-xl shadow-red-100/30 border border-white/60">
                  <div className="bg-gradient-to-br from-red-800 via-red-700 to-red-600 p-5 text-white relative overflow-hidden">
                    <div className="absolute -top-6 -right-6 w-28 h-28 bg-white/5 rounded-full blur-2xl"></div>
                    <span className="bg-white/15 backdrop-blur-sm px-2.5 py-1 rounded-full text-[10px] font-bold border border-white/20 inline-flex items-center gap-1 mb-3">👥 Tổng quan</span>
                    <h3 className="text-base font-extrabold">{formData.name || 'Chưa có tên ✏️'}</h3>
                    <p className="text-white/50 text-[11px] font-semibold mt-1">{formData.code || 'Mã HP'} • 📍 {formData.room || 'Chưa xếp'}</p>
                  </div>
                  <div className="bg-white p-5 space-y-4">
                     <div className="p-3 bg-gradient-to-r from-red-50/80 to-transparent rounded-2xl">
                       <span className="text-[10px] font-bold text-red-600 uppercase tracking-wider block mb-1">🗓️ Lịch học</span>
                       <p className="text-xs font-bold text-gray-900 flex items-center gap-1.5">
                          {formData.weeklySchedules.map(s => DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.label).join(', ')} 
                          <span className="text-gray-400 font-medium">({formData.weeklySchedules[0]?.startTime} → {formData.weeklySchedules[0]?.endTime})</span>
                       </p>
                     </div>
                     <div className="pt-4 border-t border-gray-100/80">
                        <span className="text-[10px] font-bold text-red-700 uppercase tracking-wider block mb-3">🎓 Sinh viên đã import</span>
                        <div className="bg-gradient-to-br from-red-50 to-red-100/50 border border-red-100/60 rounded-2xl p-5 text-center relative overflow-hidden">
                           <div className="absolute -right-3 -bottom-3 w-16 h-16 bg-red-200/30 rounded-full blur-xl"></div>
                           <div className="text-4xl font-black bg-gradient-to-r from-red-800 to-red-600 bg-clip-text text-transparent mb-1 relative z-10">{totalImport || 0}</div>
                           <div className="text-xs font-bold text-red-800 relative z-10">Sinh viên 🧑‍🎓</div>
                           {(existingCount + newCount) > 0 && (
                             <div className="text-[10px] text-red-700 flex items-center justify-center gap-1 mt-3 font-bold bg-white/80 py-1.5 px-3 rounded-full relative z-10 shadow-sm border border-red-100 backdrop-blur-sm">
                               <CheckCircle2 size={12}/> Sẵn sàng nhập ✅
                             </div>
                           )}
                        </div>
                     </div>
                  </div>
                  </div>
                  <p className="text-[10px] text-gray-400 font-semibold mt-4 text-center italic">Tổng hợp từ các bước trước 💡</p>
                </div>
              )}

              {currentStep === 4 && (
                <div className="animate-in fade-in zoom-in-95 duration-300">
                  <div className="rounded-3xl overflow-hidden shadow-xl shadow-red-100/30 border border-white/60">
                  <div className="bg-gradient-to-br from-red-950 via-red-900 to-red-800 p-5 text-white relative overflow-hidden">
                    <div className="absolute -top-6 -right-6 w-28 h-28 bg-white/5 rounded-full blur-2xl"></div>
                    <span className="bg-white/15 backdrop-blur-sm px-2.5 py-1 rounded-full text-[10px] font-bold border border-white/20 inline-flex items-center gap-1 mb-3">🏁 Tổng kết</span>
                    <h3 className="text-lg font-extrabold leading-tight">{formData.name}</h3>
                    <p className="text-white/50 text-[11px] font-semibold mt-1">#{formData.courseCode} • {formData.classCode ? `Nhóm ${formData.classCode}` : ''}</p>
                    <p className="text-white/40 text-[11px] font-semibold mt-0.5">📍 {formData.room}, {formData.campus}</p>
                  </div>
                  <div className="bg-white p-5 space-y-4">
                     <div className="grid grid-cols-3 gap-2.5">
                        <div className="bg-gradient-to-br from-red-50 to-red-100/50 rounded-2xl p-3 text-center border border-red-100/60 hover:shadow-md transition-all">
                           <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-red-700 to-red-900 mx-auto flex items-center justify-center mb-2 text-white shadow-md shadow-red-200">
                             <Users size={15} />
                           </div>
                           <p className="font-extrabold text-gray-900 text-base">{totalImport || 0}</p>
                           <p className="text-[9px] uppercase font-bold text-red-600 tracking-wider">Sĩ số</p>
                        </div>
                        <div className="bg-gradient-to-br from-red-50 to-red-100/50 rounded-2xl p-3 text-center border border-red-100/60 hover:shadow-md transition-all">
                           <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-red-800 to-red-950 mx-auto flex items-center justify-center mb-2 text-white shadow-md shadow-red-200">
                             <Calendar size={15} />
                           </div>
                           <p className="font-extrabold text-gray-900 text-base">{formData.totalSessions}</p>
                           <p className="text-[9px] uppercase font-bold text-red-600 tracking-wider">Buổi</p>
                        </div>
                        <div className="bg-gradient-to-br from-red-50 to-red-100/50 rounded-2xl p-3 text-center border border-red-100/60 hover:shadow-md transition-all">
                           <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-red-600 to-red-800 mx-auto flex items-center justify-center mb-2 text-white shadow-md shadow-red-200">
                             <Clock size={15} />
                           </div>
                           <p className="font-extrabold text-gray-900 text-sm">{formData.weeklySchedules[0]?.dayOfWeek ? DAYS_OF_WEEK.find(d => d.value === formData.weeklySchedules[0].dayOfWeek)?.label : '-'}</p>
                           <p className="text-[9px] uppercase font-bold text-red-600 tracking-wider">Lịch</p>
                        </div>
                     </div>

                     <div className="pt-4 border-t border-gray-100/80">
                        <span className="text-[10px] font-bold bg-gradient-to-r from-red-800 to-red-600 bg-clip-text text-transparent uppercase tracking-wider block mb-3">⚡ Quy chế &amp; Cài đặt</span>
                        <ul className="text-[11px] text-gray-600 space-y-2.5 font-medium">
                           <li className="flex items-center gap-2.5 p-2 bg-gradient-to-r from-red-50/60 to-transparent rounded-xl"><div className="w-5 h-5 rounded-lg bg-gradient-to-br from-red-700 to-red-900 flex items-center justify-center shrink-0"><CheckCircle2 size={11} className="text-white"/></div> Vắng &gt; 20% cấm thi</li>
                           <li className="flex items-center gap-2.5 p-2 bg-gradient-to-r from-red-50/60 to-transparent rounded-xl"><div className="w-5 h-5 rounded-lg bg-gradient-to-br from-red-700 to-red-900 flex items-center justify-center shrink-0"><CheckCircle2 size={11} className="text-white"/></div> QR động (15s) 📱</li>
                           <li className="flex items-center gap-2.5 p-2 bg-gradient-to-r from-red-50/60 to-transparent rounded-xl"><div className="w-5 h-5 rounded-lg bg-gradient-to-br from-red-700 to-red-900 flex items-center justify-center shrink-0"><CheckCircle2 size={11} className="text-white"/></div> 1 thiết bị/SV 🔒</li>
                           <li className="flex items-center gap-2.5 p-2 bg-gradient-to-r from-red-50/60 to-transparent rounded-xl"><div className="w-5 h-5 rounded-lg bg-gradient-to-br from-red-700 to-red-900 flex items-center justify-center shrink-0"><CheckCircle2 size={11} className="text-white"/></div> Báo cáo tự động 📊</li>
                           <li className="flex items-center gap-2.5 p-2 bg-gradient-to-r from-red-50/60 to-transparent rounded-xl"><div className="w-5 h-5 rounded-lg bg-gradient-to-br from-red-700 to-red-900 flex items-center justify-center shrink-0"><CheckCircle2 size={11} className="text-white"/></div> SV xem tiến độ 👁️</li>
                        </ul>
                     </div>
                  </div>
                  </div>
                </div>
              )}
              
            </div>
          </div>
        </div>

        {/* BOTTOM ACTION BAR */}
        <div className={`fixed bottom-0 right-0 bg-gray-50/90 backdrop-blur-md border-t border-gray-200 px-8 py-4 flex justify-between items-center z-10 shadow-[0_-4px_20px_-10px_rgba(0,0,0,0.1)] transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'left-[80px]' : 'left-64'}`}>
          <button 
            onClick={() => currentStep === 1 ? navigate('/classes') : handlePrevStep()} 
            className="px-6 py-2.5 text-sm font-semibold text-gray-600 hover:text-gray-900 bg-white hover:bg-gray-100 border border-gray-200 rounded-full transition-all shadow-sm flex items-center gap-2"
          >
            {currentStep === 1 ? 'Hủy & Đóng' : '← Quay lại'}
          </button>
          
          <div className="flex gap-4 items-center">
            <button 
              onClick={handleSaveDraftManual}
              type="button"
              className="text-sm font-bold text-red-700 hover:text-red-800 hover:underline px-4 transition-all"
            >
              Lưu bản nháp
            </button>
            {currentStep < 4 ? (
              <button 
                onClick={handleNextStep} 
                className="px-8 py-2.5 bg-red-800 hover:bg-red-900 text-white rounded-full text-sm font-bold transition-all shadow-md hover:shadow-lg flex items-center gap-2 transform active:scale-95"
              >
                Tiếp tục →
              </button>
            ) : (
              <button 
                onClick={handleSubmit} 
                disabled={isSubmitting}
                className="px-8 py-2.5 bg-red-900 hover:bg-red-950 text-white rounded-full text-sm font-bold transition-all shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2 transform active:scale-95"
              >
                {isSubmitting ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                    Đang xử lý...
                  </>
                ) : (
                  <>
                    <Sparkles size={16} />
                    Hoàn tất & Kích hoạt lớp
                  </>
                )}
              </button>
            )}
          </div>
        </div>
        {/* UNIFIED CONFLICT RESOLUTION & RECOMMENDATIONS MODAL */}
        {conflictModal.isOpen && (
          <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-sm z-[999] flex items-center justify-center animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl border border-gray-100 transform transition-all scale-in animate-in zoom-in-95 duration-200">
              <div className="flex flex-col items-center">
                <div className="w-14 h-14 rounded-full bg-amber-50 border border-amber-100 flex items-center justify-center text-amber-600 mb-4 animate-pulse">
                  <AlertTriangle size={28} />
                </div>
                <h3 className="text-xl font-bold text-gray-900 mb-2">{conflictModal.title}</h3>
                
                {/* 0. PERSONAL CONFLICT RENDER */}
                {conflictModal.type === 'PERSONAL_CONFLICT' && (
                  <>
                    <div className="w-full bg-orange-50/80 border border-orange-100 rounded-xl p-4 mb-4 shadow-sm text-left">
                      <div className="text-xs font-bold text-orange-600 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-orange-500 animate-ping"></span>
                        Trùng lịch dạy của chính bạn:
                      </div>
                      <div className="flex justify-between items-center text-sm font-bold text-orange-950">
                        <span>{conflictModal.conflictingClass?.name || 'Lớp học khác'}</span>
                        <span className="text-xs text-orange-700 bg-orange-100 border border-orange-200 px-2.5 py-0.5 rounded-lg font-mono font-bold">
                          {conflictModal.conflictingClass?.code || 'TRÙNG LỊCH'}
                        </span>
                      </div>
                    </div>

                    <p className="text-sm text-gray-600 text-center leading-relaxed mb-4">
                      Bạn đã có lịch dạy vào khung giờ này:
                      <br />
                      <span className="font-mono font-bold text-orange-600 bg-orange-50 px-2.5 py-0.5 rounded text-xs mt-2 inline-block">
                        {conflictModal.conflictingSchedule}
                      </span>
                    </p>

                    <div className="w-full text-left mb-6">
                      <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">GỢI Ý KHUNG GIỜ RẢNH TRONG NGÀY</label>
                      {conflictModal.suggestedSlots && conflictModal.suggestedSlots.length > 0 ? (
                        <div className="grid grid-cols-2 gap-2.5 max-h-36 overflow-y-auto pr-1">
                          {conflictModal.suggestedSlots.map((slot, idx) => (
                            <button
                              key={idx}
                              onClick={() => {
                                const newSchedules = formData.weeklySchedules.map(s => {
                                  if (s.dayOfWeek === conflictModal.dayOfWeek) {
                                    return { ...s, startTime: slot.start, endTime: slot.end };
                                  }
                                  return s;
                                });
                                setFormData(prev => ({ ...prev, weeklySchedules: newSchedules }));
                                setConflictModal(prev => ({ ...prev, isOpen: false }));
                                toast.success(`Đã tự động đổi sang ${slot.label}!`);
                              }}
                              className="px-3 py-2 bg-emerald-50 hover:bg-emerald-100 border border-emerald-100 hover:border-emerald-200 rounded-xl text-xs font-bold text-emerald-700 text-center transition-all cursor-pointer transform active:scale-95"
                            >
                              {slot.label}
                            </button>
                          ))}
                        </div>
                      ) : (
                        <p className="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-xl p-3 font-medium">
                          ⚠️ Bạn không còn khung giờ trống nào trong ngày này. Vui lòng chọn ngày dạy khác.
                        </p>
                      )}
                    </div>
                  </>
                )}



                {/* 2. START DATE CONFLICT RENDER */}
                {conflictModal.type === 'START_DATE_CONFLICT' && (
                  <>
                    <p className="text-sm text-gray-600 text-center leading-relaxed mb-5">
                      {conflictModal.message}
                    </p>
                    
                    <div className="w-full text-left mb-6">
                      <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-3">GỢI Ý CÁC NGÀY BẮT ĐẦU PHÙ HỢP GẦN NHẤT</label>
                      {conflictModal.suggestedDates.length > 0 ? (
                        <div className="grid grid-cols-2 gap-3 max-h-48 overflow-y-auto pr-1">
                          {conflictModal.suggestedDates.map((dateObj) => {
                            const dateValue = dateObj.toISOString().split('T')[0];
                            const daysOfWeekVi = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];
                            const dayNameVi = daysOfWeekVi[dateObj.getDay()];
                            const formattedDate = dateObj.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
                            return (
                              <button
                                key={dateValue}
                                onClick={() => {
                                  setFormData(prev => ({ ...prev, startDate: dateValue }));
                                  setConflictModal(prev => ({ ...prev, isOpen: false }));
                                  toast.success(`Đã chọn ngày bắt đầu: ${formattedDate}!`);
                                }}
                                className="px-3 py-2.5 bg-blue-50 hover:bg-blue-100 border border-blue-100 hover:border-blue-200 rounded-xl text-xs font-bold text-blue-700 text-center transition-all cursor-pointer transform active:scale-95"
                              >
                                <div>{dayNameVi}</div>
                                <div className="text-[10px] text-blue-500 font-normal mt-0.5">{formattedDate}</div>
                              </button>
                            );
                          })}
                        </div>
                      ) : (
                        <p className="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-xl p-3 font-medium">
                          ⚠️ Không tìm thấy ngày bắt đầu phù hợp trong phạm vi học kỳ. Vui lòng đổi lịch học hoặc kiểm tra lại ngày học kỳ.
                        </p>
                      )}
                    </div>
                  </>
                )}

                {/* SERVER CONFLICT RENDER */}
                {conflictModal.type === 'SERVER_CONFLICT' && (
                  <>
                    <div className="w-full bg-red-50/80 border border-red-100 rounded-xl p-4 mb-6 shadow-sm text-center">
                      <div className="text-xs font-bold text-red-600 uppercase tracking-wider mb-2 flex items-center justify-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping"></span>
                        Hệ thống từ chối do trùng lặp dữ liệu:
                      </div>
                      <p className="text-sm font-semibold text-red-900 leading-relaxed">
                        {conflictModal.message}
                      </p>
                    </div>

                    <div className="w-full text-center mb-6">
                      <p className="text-xs text-gray-500">
                        Vui lòng quay lại các bước trước để điều chỉnh thông tin lịch giảng, cơ sở hoặc phòng học trước khi thử lại.
                      </p>
                    </div>
                  </>
                )}

                {/* 3. END DATE EXCEEDED RENDER */}
                {conflictModal.type === 'END_DATE_EXCEEDED' && (
                  <>
                    <p className="text-sm text-gray-600 text-center leading-relaxed mb-5">
                      {conflictModal.message}
                    </p>
                    
                    <div className="w-full text-left mb-6 space-y-3">
                      <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider">HÀNH ĐỘNG KHẮC PHỤC NHANH</label>
                      <button
                        onClick={() => {
                          setFormData(prev => ({ ...prev, totalSessions: conflictModal.maxSessionsSuggested }));
                          setConflictModal(prev => ({ ...prev, isOpen: false }));
                          toast.success(`Đã giới hạn tổng số buổi về ${conflictModal.maxSessionsSuggested} buổi!`);
                        }}
                        className="w-full px-4 py-3 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-xs text-center transition-all flex items-center justify-center gap-2 transform active:scale-95 shadow-md cursor-pointer"
                      >
                        <span>Giới hạn lại tổng số buổi về {conflictModal.maxSessionsSuggested} buổi</span>
                      </button>
                    </div>
                  </>
                )}

                <button
                  onClick={() => setConflictModal(prev => ({ ...prev, isOpen: false }))}
                  className="w-full px-5 py-2.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-semibold rounded-xl text-sm transition-all"
                >
                  Hủy bỏ
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}




