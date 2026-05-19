import React, { useState, useEffect } from 'react';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ShieldAlert, Bell, UploadCloud, Info, Plus, Trash2, 
  Calendar, Users, UserCheck, Clock, MapPin, BookOpen, AlertCircle, FileText, AlertTriangle
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

// Cấu hình danh sách phòng học cho từng cơ sở để tự động đề xuất
const CAMPUS_ROOMS = {
  'Cơ sở 1': ['A1.101', 'A1.102', 'A1.201', 'A1.202', 'B2.101', 'B2.102', 'B2.201', 'B2.202'],
  'Cơ sở 2': ['C3.101', 'C3.102', 'C3.201', 'C3.202', 'D4.101', 'D4.102', 'D4.201', 'D4.202'],
  'Cơ sở 3': ['E5.101', 'E5.102', 'E5.201', 'E5.202', 'F6.101', 'F6.102', 'F6.201', 'F6.202'],
  'default': ['A1.101', 'A1.102', 'B2.101', 'B2.102', 'C3.101', 'C3.102', 'D4.101', 'D4.102']
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
        className="w-full pl-9 pr-7 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50 font-semibold text-gray-700 cursor-pointer flex items-center justify-between transition-all"
      >
        <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-red-600">
          <Clock size={16} />
        </span>
        <span>{value}</span>
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
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [startDateError, setStartDateError] = useState('');
  const [absencesError, setAbsencesError] = useState('');
  const [hasDraft, setHasDraft] = useState(false);

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
    semester: 'HK1',
    academicYear: '2025-2026',
    campus: 'Cơ sở 1',
    room: '',
    startDate: '',
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

  // Kiểm tra trùng phòng học của giảng viên (Bắt buộc trùng cả Phòng học VÀ Lịch/Giờ học mới tính)
  const checkRoomConflict = (campusName, roomName, newSchedules) => {
    if (!campusName || !roomName || !newSchedules || newSchedules.length === 0) {
      return null;
    }

    const cleanCampus = campusName.trim().toLowerCase();
    const cleanRoom = roomName.trim().toLowerCase();

    for (const group of existingClasses) {
      if (!group.weeklySchedules) continue;

      const groupCampus = (group.campus || '').trim().toLowerCase();
      const groupRoom = (group.room || '').trim().toLowerCase();

      // Chỉ xét nếu trùng Cơ sở VÀ Phòng học
      if (groupCampus === cleanCampus && groupRoom === cleanRoom) {
        // Kiểm tra trùng lịch học tuần (trùng ngày học + chồng lấn thời gian)
        for (const newSched of newSchedules) {
          for (const extSched of group.weeklySchedules) {
            if (newSched.dayOfWeek === extSched.dayOfWeek) {
              if (isTimeOverlap(newSched.startTime, newSched.endTime, extSched.startTime, extSched.endTime)) {
                return {
                  class: {
                    ...group,
                    isOwnClass: true
                  },
                  isOwnClass: true,
                  conflictSchedule: `${DAYS_OF_WEEK.find(d => d.value === newSched.dayOfWeek)?.vi || newSched.dayOfWeek} ${newSched.startTime}-${newSched.endTime}`
                };
              }
            }
          }
        }
      }
    }
    return null;
  };

  // Tìm danh sách phòng học thay thế còn trống
  const getAlternativeRooms = (campusName, newSchedules) => {
    if (!campusName || !newSchedules || newSchedules.length === 0) return [];
    
    const cleanCampus = campusName.trim();
    const roomsList = CAMPUS_ROOMS[cleanCampus] || CAMPUS_ROOMS['default'];

    return roomsList.filter(roomCandidate => {
      // Kiểm tra xem ứng viên phòng học này có bị trùng với bất cứ lớp học nào không
      const hasConflict = existingClasses.some(group => {
        const groupCampus = (group.campus || '').trim().toLowerCase();
        const groupRoom = (group.room || '').trim().toLowerCase();

        if (groupCampus === cleanCampus.toLowerCase() && groupRoom === roomCandidate.toLowerCase() && group.weeklySchedules) {
          return newSchedules.some(newSched =>
            group.weeklySchedules.some(extSched =>
              newSched.dayOfWeek === extSched.dayOfWeek &&
              isTimeOverlap(newSched.startTime, newSched.endTime, extSched.startTime, extSched.endTime)
            )
          );
        }
        return false;
      });

      return !hasConflict;
    });
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    
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

    // Kiểm tra trùng phòng học lớp do chính mình quản lý trước khi submit (Bắt buộc trùng cả Phòng học VÀ Lịch/Giờ học)
    const roomConflict = checkRoomConflict(formData.campus, formData.room, formData.weeklySchedules);
    if (roomConflict) {
      const suggestions = getAlternativeRooms(formData.campus, formData.weeklySchedules);
      setConflictModal({
        isOpen: true,
        type: 'ROOM_CONFLICT',
        title: 'Trùng Phòng Lớp Của Bạn!',
        message: `Phòng học ${formData.room} và khung giờ này đã được sử dụng bởi lớp "${roomConflict.class.name}" (${roomConflict.class.code}) do chính bạn tạo và quản lý!`,
        conflictingClass: roomConflict.class,
        conflictingRoom: formData.room,
        conflictingSchedule: roomConflict.conflictSchedule,
        suggestedRooms: suggestions
      });
      toast.error('Trùng lịch & phòng học lớp của bạn!');
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
        const suggestions = getAlternativeRooms(formData.campus, formData.weeklySchedules);
        
        let serverClassName = 'Lớp học của giảng viên khác';
        let serverClassCode = 'TRÙNG PHÒNG';
        
        const errMsg = errorData?.message || '';
        if (errMsg) {
          // Regular expression to parse group/class details like: Room occupies by 'Java Programming' (JAVA101)
          const nameMatch = errMsg.match(/['"“](.+?)['"”]/);
          const codeMatch = errMsg.match(/\((.+?)\)/);
          if (nameMatch && nameMatch[1]) serverClassName = nameMatch[1];
          if (codeMatch && codeMatch[1]) serverClassCode = codeMatch[1];
        }

        setConflictModal({
          isOpen: true,
          type: 'ROOM_CONFLICT',
          title: 'Trùng Phòng Giảng Viên Khác!',
          message: errMsg || `Phòng học ${formData.room} tại ${formData.campus} đã được một giảng viên khác đăng ký sử dụng trong cùng khung giờ!`,
          conflictingClass: { 
            name: serverClassName, 
            code: serverClassCode,
            isOwnClass: false 
          },
          conflictingRoom: formData.room,
          conflictingSchedule: formData.weeklySchedules.map(s => `${DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.vi || s.dayOfWeek} ${s.startTime}-${s.endTime}`).join(', '),
          suggestedRooms: suggestions
        });
        toast.error(errMsg || "Trùng phòng học với lớp của giảng viên khác!", { id: loadingToastId, duration: 6000 });
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
            
            {/* 1. Thông tin cơ bản */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              <div className="p-6 border-b border-gray-100 flex items-center gap-3 bg-gradient-to-r from-white to-red-50/20">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-800 text-white flex items-center justify-center text-sm font-bold shadow-md">
                  1
                </div>
                <h2 className="text-lg font-bold text-gray-900">Thông tin cơ bản</h2>
              </div>
              <div className="p-6 space-y-5">
                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Tên lớp <span className="text-red-500">*</span>
                    </label>
                    <input 
                      type="text" 
                      name="name" 
                      value={formData.name} 
                      onChange={handleChange} 
                      placeholder="VD: Cấu trúc dữ liệu & Giải thuật" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Mã học phần <span className="text-red-500">*</span>
                    </label>
                    <input 
                      type="text" 
                      name="courseCode" 
                      value={formData.courseCode} 
                      onChange={handleChange} 
                      placeholder="VD: CS204" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Mã lớp/Groups
                    </label>
                    <input 
                      type="text" 
                      name="classCode" 
                      value={formData.classCode} 
                      onChange={handleChange} 
                      placeholder="VD: 01" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Mã tự động (Code)
                    </label>
                    <input 
                      type="text" 
                      name="code" 
                      value={formData.code} 
                      disabled
                      placeholder="Tự động tạo từ Mã HP & Lớp" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-gray-100 text-gray-500 cursor-not-allowed outline-none transition-all" 
                    />
                  </div>
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Học kỳ
                    </label>
                    <select 
                      name="semester" 
                      value={formData.semester} 
                      onChange={handleChange} 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-4 focus:ring-red-50"
                    >
                      {SEMESTERS.map(s => (
                        <option key={s.value} value={s.value}>{s.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Năm học
                    </label>
                    <select 
                      name="academicYear" 
                      value={formData.academicYear} 
                      onChange={handleChange} 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-4 focus:ring-red-50"
                    >
                      {ACADEMIC_YEARS.map(y => (
                        <option key={y.value} value={y.value}>{y.label}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Ngày bắt đầu <span className="text-red-500">*</span>
                    </label>
                    <input 
                      type="date" 
                      name="startDate" 
                      value={formData.startDate} 
                      onChange={handleStartDateChange} 
                      min={minDateStr}
                      max={maxDateStr}
                      className={`w-full px-4 py-2.5 border-2 rounded-xl text-sm focus:ring-4 outline-none transition-all ${
                        startDateError 
                          ? 'border-red-400 focus:border-red-400 focus:ring-red-50' 
                          : 'border-gray-200 focus:border-red-400 focus:ring-red-50'
                      }`}
                    />
                    {startDateError && (
                      <div className="mt-1 flex items-center gap-1 text-xs text-red-600">
                        <AlertCircle size={12} />
                        <span>{startDateError}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      <MapPin size={14} className="inline mr-1" /> Cơ sở
                    </label>
                    <input 
                      type="text" 
                      name="campus" 
                      value={formData.campus} 
                      onChange={handleChange} 
                      placeholder="VD: Cơ sở 1, Cơ sở 2" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Phòng học
                    </label>
                    <input 
                      type="text" 
                      name="room" 
                      value={formData.room} 
                      onChange={handleChange} 
                      placeholder="VD: A1.102" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                    Mô tả
                  </label>
                  <textarea 
                    name="description" 
                    value={formData.description} 
                    onChange={handleChange} 
                    rows="3" 
                    className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm outline-none resize-none focus:border-red-400 focus:ring-4 focus:ring-red-50 transition-all" 
                    placeholder="Nhập mô tả chi tiết về môn học..."
                  />
                </div>
              </div>
            </div>

            {/* 2. Lịch học */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow">
              <div className="p-6 border-b border-gray-100 flex items-center gap-3 bg-gradient-to-r from-white to-red-50/20">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-800 text-white flex items-center justify-center text-sm font-bold shadow-md">
                  2
                </div>
                <h2 className="text-lg font-bold text-gray-900">Lịch học</h2>
              </div>
              <div className="p-6 space-y-5">
                <div className="grid grid-cols-2 gap-5">
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      <Clock size={14} className="inline mr-1" /> Tổng số buổi
                    </label>
                    <input 
                      type="number" 
                      name="totalSessions" 
                      value={formData.totalSessions} 
                      onChange={handleChange} 
                      onBlur={handleTotalSessionsBlur}
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">
                      Số buổi được phép vắng
                    </label>
                    <input 
                      type="number" 
                      name="maxAllowedAbsences" 
                      value={formData.maxAllowedAbsences} 
                      onChange={handleChange} 
                      onBlur={handleAbsencesBlur}
                      className={`w-full px-4 py-2.5 border-2 rounded-xl text-sm focus:ring-4 outline-none transition-all ${
                        absencesError 
                          ? 'border-red-400 focus:border-red-400 focus:ring-red-50' 
                          : 'border-gray-200 focus:border-red-400 focus:ring-red-50'
                      }`}
                    />
                    {absencesError && (
                      <div className="mt-1 flex items-center gap-1 text-xs text-red-600">
                        <AlertCircle size={12} />
                        <span>{absencesError}</span>
                      </div>
                    )}
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-3">
                    Lịch học trong tuần
                  </label>
                  <div className="space-y-3">
                    {formData.weeklySchedules.map((schedule, idx) => (
                      <div key={idx} className="flex items-center gap-4 bg-gray-50 p-4 rounded-xl border border-gray-200 hover:border-red-200 transition-all flex-wrap sm:flex-nowrap">
                        {/* Thứ trong tuần */}
                        <div className="relative flex-1 min-w-[120px]">
                          <select 
                            value={schedule.dayOfWeek} 
                            onChange={(e) => handleScheduleChange(idx, 'dayOfWeek', e.target.value)} 
                            className="w-full pl-3 pr-8 py-2.5 border-2 border-gray-200 rounded-xl text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50 appearance-none font-semibold text-gray-700 cursor-pointer"
                          >
                            {DAYS_OF_WEEK.map(day => (
                              <option key={day.value} value={day.value}>{day.vi}</option>
                            ))}
                          </select>
                          <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-gray-400">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" /></svg>
                          </div>
                        </div>
                        
                        {/* Giờ bắt đầu */}
                        <div className="w-[130px]">
                          <CustomTimePicker 
                            value={schedule.startTime} 
                            onChange={(val) => handleScheduleChange(idx, 'startTime', val)} 
                          />
                        </div>
                        
                        <span className="text-gray-400 font-bold">→</span>
                        
                        {/* Giờ kết thúc */}
                        <div className="w-[130px]">
                          <CustomTimePicker 
                            value={schedule.endTime} 
                            onChange={(val) => handleScheduleChange(idx, 'endTime', val)} 
                          />
                        </div>
                        
                        {formData.weeklySchedules.length > 1 && (
                          <button 
                            onClick={() => removeSchedule(idx)} 
                            className="ml-auto p-2.5 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all shadow-sm border border-transparent hover:border-red-100"
                          >
                            <Trash2 size={16} />
                          </button>
                        )}
                      </div>
                    ))}
                    
                    <button 
                      onClick={handleAddFirstSchedule} 
                      className="text-sm font-semibold text-red-600 flex items-center gap-2 hover:gap-3 transition-all"
                    >
                      <Plus size={16} /> Thêm buổi học trong tuần
                    </button>
                    
                    {formData.weeklySchedules.length > 0 && formData.startDate && (
                      <div className="mt-3 p-3 bg-blue-50 rounded-lg border border-blue-100">
                        <p className="text-xs text-blue-700">
                          <Calendar size={12} className="inline mr-1" />
                          Gợi ý: Ngày bắt đầu nên là {formData.weeklySchedules.map(s => getVietnameseDayName(s.dayOfWeek)).join(' hoặc ')}
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>

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

            {/* 4. Danh sách sinh viên (giữ nguyên) */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow mb-8">
              <div className="p-6 border-b border-gray-100 flex items-center gap-3 bg-gradient-to-r from-white to-red-50/20">
                <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-red-600 to-red-800 text-white flex items-center justify-center text-sm font-bold shadow-md">
                  4
                </div>
                <h2 className="text-lg font-bold text-gray-900">Danh sách sinh viên</h2>
              </div>
              <div className="p-6">
                <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 flex flex-col items-center justify-center text-center hover:border-red-400 hover:bg-red-50/20 transition-all cursor-pointer group">
                  <div className="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mb-4 group-hover:bg-red-100 transition-all">
                    <UploadCloud size={24} className="text-gray-500 group-hover:text-red-600 transition-all" />
                  </div>
                  <p className="text-sm font-semibold text-gray-900 group-hover:text-red-700">Nhấp để tải lên hoặc kéo thả file</p>
                  <p className="text-xs text-gray-500 mt-2">
                    File CSV (Tối đa 5MB).{' '}
                    <span className="text-red-600 hover:underline cursor-pointer font-medium">Tải template mẫu</span>
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* CỘT PHẢI - Summary Sidebar */}
          <div className="lg:col-span-4">
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 sticky top-24">
              <h3 className="text-lg font-bold text-gray-900 mb-5 flex items-center gap-2">
                <Info size={18} className="text-red-600" />
                Tổng quan lớp học
              </h3>
              
              <div className="space-y-5">
                <div>
                  <p className="text-xs font-bold text-gray-400 tracking-wider mb-2">TRẠNG THÁI</p>
                  <span className="px-3 py-1.5 bg-amber-100 text-amber-700 text-xs font-bold rounded-lg">Bản nháp</span>
                </div>
                
                <div className="border-t border-gray-100 pt-4">
                  <p className="text-xs font-bold text-gray-400 tracking-wider mb-2">THÔNG SỐ</p>
                  <div className="space-y-2 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-500">Tổng số buổi:</span>
                      <span className="font-semibold text-gray-900">{formData.totalSessions}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Được vắng tối đa:</span>
                      <span className="font-semibold text-gray-900">{formData.maxAllowedAbsences}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Số buổi/tuần:</span>
                      <span className="font-semibold text-gray-900">{formData.weeklySchedules.length}</span>
                    </div>
                    {formData.startDate && (
                      <div className="flex justify-between">
                        <span className="text-gray-500">Ngày bắt đầu:</span>
                        <span className="font-semibold text-gray-900">
                          {(() => {
                            if (!formData.startDate) return '';
                            const [y, m, d] = formData.startDate.split('-');
                            return y && m && d ? `${d}/${m}/${y}` : formData.startDate;
                          })()}
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                <div className="bg-gradient-to-br from-red-50 to-orange-50 rounded-xl p-4 border border-red-100 mt-4">
                  <h4 className="flex items-center gap-2 font-bold text-gray-900 text-sm mb-3">
                    <BookOpen size={16} className="text-red-600" />
                    Lưu ý
                  </h4>
                  <ul className="text-xs text-gray-600 space-y-2">
                    <li className="flex items-start gap-2">
                      <span className="text-red-500">•</span>
                      Ngày bắt đầu phải trùng với một trong các ngày trong lịch học
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-red-500">•</span>
                      Sinh viên sẽ nhận email mời đăng ký thiết bị
                    </li>
                    <li className="flex items-start gap-2">
                      <span className="text-red-500">•</span>
                      Có thể tạo QR điểm danh ngay sau khi tạo lớp
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* BOTTOM ACTION BAR */}
        <div className={`fixed bottom-0 right-0 bg-white/80 backdrop-blur-md border-t border-gray-200 px-8 py-4 flex justify-between items-center z-10 shadow-lg transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'left-[80px]' : 'left-64'}`}>
          <button 
            onClick={() => navigate('/classes')} 
            className="px-6 py-2.5 text-sm font-semibold text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-xl transition-all"
          >
            Hủy bỏ
          </button>
          <div className="flex gap-3">
            <button 
              onClick={handleSaveDraftManual}
              type="button"
              className="px-6 py-2.5 border-2 border-gray-200 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm"
            >
              Lưu nháp
            </button>
            <button 
              onClick={handleSubmit} 
              disabled={isSubmitting || !!startDateError}
              className="px-8 py-2.5 bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white rounded-xl text-sm font-bold transition-all shadow-md hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                'Tạo lớp học →'
              )}
            </button>
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
                
                {/* 1. ROOM CONFLICT RENDER */}
                {conflictModal.type === 'ROOM_CONFLICT' && (
                  <>
                    <div className="w-full bg-red-50/80 border border-red-100 rounded-xl p-4 mb-4 shadow-sm text-left">
                      <div className="text-xs font-bold text-red-500 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-ping"></span>
                        Lớp học trùng lịch & phòng:
                      </div>
                      <div className="flex justify-between items-center text-sm font-bold text-red-950">
                        <span>{conflictModal.conflictingClass?.name || 'Lớp học khác'}</span>
                        <span className="text-xs text-red-700 bg-red-100 border border-red-200 px-2.5 py-0.5 rounded-lg font-mono font-bold">
                          {conflictModal.conflictingClass?.code || 'TRÙNG LỊCH'}
                        </span>
                      </div>
                    </div>

                    <p className="text-sm text-gray-600 text-center leading-relaxed mb-4">
                      Phòng học <strong className="text-gray-900">{conflictModal.conflictingRoom}</strong> tại <strong className="text-gray-900">{formData.campus}</strong> đã bị trùng vào khung giờ:
                      <br />
                      <span className="font-mono font-bold text-red-600 bg-red-50 px-2.5 py-0.5 rounded text-xs mt-2 inline-block">
                        {conflictModal.conflictingSchedule}
                      </span>
                    </p>

                    <div className="w-full text-left mb-6">
                      <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">GỢI Ý PHÒNG CÒN TRỐNG TẠI CƠ SỞ NÀY</label>
                      {conflictModal.suggestedRooms.length > 0 ? (
                        <div className="grid grid-cols-3 gap-2.5 max-h-36 overflow-y-auto pr-1">
                          {conflictModal.suggestedRooms.map((roomName) => (
                            <button
                              key={roomName}
                              onClick={() => {
                                setFormData(prev => ({ ...prev, room: roomName }));
                                setConflictModal(prev => ({ ...prev, isOpen: false }));
                                toast.success(`Đã tự động chọn phòng ${roomName}!`);
                              }}
                              className="px-3 py-2 bg-emerald-50 hover:bg-emerald-100 border border-emerald-100 hover:border-emerald-200 rounded-xl text-xs font-bold text-emerald-700 text-center transition-all cursor-pointer transform active:scale-95"
                            >
                              {roomName}
                            </button>
                          ))}
                        </div>
                      ) : (
                        <p className="text-xs text-amber-600 bg-amber-50 border border-amber-100 rounded-xl p-3 font-medium">
                          ⚠️ Không có phòng trống nào khác tại {formData.campus} vào khung giờ này. Vui lòng đổi cơ sở hoặc khung giờ học.
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




