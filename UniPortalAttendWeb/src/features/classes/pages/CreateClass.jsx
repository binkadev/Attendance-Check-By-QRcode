import React, { useState } from 'react';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ShieldAlert, Bell, UploadCloud, Info, Plus, Trash2, 
  Calendar, Users, UserCheck, Clock, MapPin, BookOpen, AlertCircle
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

export default function CreateClass() {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [startDateError, setStartDateError] = useState('');

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

  // Hàm lấy thứ trong tuần từ ngày
  const getDayOfWeek = (dateString) => {
    if (!dateString) return null;
    const date = new Date(dateString);
    const dayIndex = date.getDay(); // 0 = Chủ Nhật, 1 = Thứ 2, ...
    return DAYS_OF_WEEK_MAP[dayIndex];
  };

  // Kiểm tra startDate có khớp với lịch học không
  const validateStartDate = (startDate, schedules) => {
    if (!startDate) return { isValid: false, message: 'Vui lòng chọn ngày bắt đầu' };
    if (!schedules || schedules.length === 0) return { isValid: false, message: 'Vui lòng thêm ít nhất một lịch học' };
    
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
    
    return { isValid: true, message: '' };
  };

  // Tự động điều chỉnh startDate khi thay đổi lịch học
  const autoAdjustStartDate = (schedules) => {
    if (schedules.length === 0) return;
    
    const firstScheduleDay = schedules[0].dayOfWeek;
    // Tìm ngày gần nhất trong tuần (tuần tới)
    const today = new Date();
    const currentDayIndex = today.getDay();
    const targetDayIndex = DAYS_OF_WEEK.findIndex(d => d.value === firstScheduleDay);
    
    let daysToAdd = targetDayIndex - currentDayIndex;
    if (daysToAdd < 0) daysToAdd += 7;
    if (daysToAdd === 0 && today.getHours() >= 12) daysToAdd = 7; // Nếu đã qua trưa thì sang tuần sau
    
    const nextDate = new Date(today);
    nextDate.setDate(today.getDate() + daysToAdd);
    const suggestedDate = nextDate.toISOString().split('T')[0];
    
    setFormData(prev => ({ ...prev, startDate: suggestedDate }));
    setStartDateError('');
  };

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
    
    // Validate ngay lập tức
    const validation = validateStartDate(newDate, formData.weeklySchedules);
    if (!validation.isValid) {
      setStartDateError(validation.message);
    } else {
      setStartDateError('');
    }
  };

  const addSchedule = () => {
    setFormData(prev => {
      const newSchedules = [...prev.weeklySchedules, { dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }];
      
      // Nếu có startDate, re-validate
      if (prev.startDate) {
        const validation = validateStartDate(prev.startDate, newSchedules);
        if (!validation.isValid) {
          setStartDateError(validation.message);
        } else {
          setStartDateError('');
        }
      }
      
      return { ...prev, weeklySchedules: newSchedules };
    });
  };

  const removeSchedule = (index) => {
    if (formData.weeklySchedules.length > 1) {
      setFormData(prev => {
        const newSchedules = prev.weeklySchedules.filter((_, i) => i !== index);
        
        // Re-validate sau khi xóa
        if (prev.startDate) {
          const validation = validateStartDate(prev.startDate, newSchedules);
          if (!validation.isValid) {
            setStartDateError(validation.message);
          } else {
            setStartDateError('');
          }
        }
        
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
    
    // Re-validate startDate sau khi thay đổi lịch
    if (formData.startDate) {
      const validation = validateStartDate(formData.startDate, newSchedules);
      if (!validation.isValid) {
        setStartDateError(validation.message);
      } else {
        setStartDateError('');
      }
    }
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

    // Validate startDate với weeklySchedules
    if (!formData.startDate) {
      toast.error("Vui lòng chọn ngày bắt đầu!");
      return;
    }

    const validation = validateStartDate(formData.startDate, formData.weeklySchedules);
    if (!validation.isValid) {
      toast.error(validation.message);
      setStartDateError(validation.message);
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

      await classApi.createGroup(requestPayload);
      
      toast.success(`Tạo lớp thành công! Mã tham gia: ${uniqueJoinCode}`, { id: loadingToastId, duration: 6000 });
      
      setTimeout(() => {
        navigate('/classes');
      }, 2000);

    } catch (error) {
      console.error('Lỗi khi tạo lớp:', error);
      
      if (error.response?.status === 422) {
        const errorData = error.response?.data;
        if (errorData?.code === 'START_DATE_NOT_MATCH_SCHEDULE') {
          toast.error(`Lỗi: Ngày bắt đầu không khớp với lịch học. Vui lòng chọn ngày ${formData.weeklySchedules.map(s => DAYS_OF_WEEK.find(d => d.value === s.dayOfWeek)?.vi || s.dayOfWeek).join(' hoặc ')}.`, { id: loadingToastId, duration: 6000 });
        } else {
          toast.error(errorData?.message || 'Dữ liệu không hợp lệ!', { id: loadingToastId });
        }
      } else if (error.response?.status === 400) {
        toast.error("Dữ liệu gửi lên không hợp lệ!", { id: loadingToastId });
      } else if (error.response?.status === 409) {
        toast.error("Mã học phần này đã tồn tại trên hệ thống!", { id: loadingToastId, duration: 4000 });
      }
      else if (error.message.includes("schedule conflict")) {
        toast.error("Lịch học bị trùng lặp!", { id: loadingToastId, duration: 6000 });
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

        <div className="p-8 max-w-7xl w-full mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* CỘT TRÁI - Form chính */}
          <div className="lg:col-span-8 space-y-6">
            
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
                      onChange={handleChange} 
                      placeholder="Để trống để tự động tạo" 
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all bg-gray-50" 
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
                      className="w-full px-4 py-2.5 border-2 border-gray-200 rounded-xl text-sm focus:border-red-400 focus:ring-4 focus:ring-red-50 outline-none transition-all" 
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-gray-700 mb-3">
                    Lịch học trong tuần
                  </label>
                  <div className="space-y-3">
                    {formData.weeklySchedules.map((schedule, idx) => (
                      <div key={idx} className="flex items-center gap-3 bg-gray-50 p-4 rounded-xl border border-gray-200 hover:border-red-200 transition-all">
                        <select 
                          value={schedule.dayOfWeek} 
                          onChange={(e) => handleScheduleChange(idx, 'dayOfWeek', e.target.value)} 
                          className="px-3 py-2 border-2 border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50"
                        >
                          {DAYS_OF_WEEK.map(day => (
                            <option key={day.value} value={day.value}>{day.vi}</option>
                          ))}
                        </select>
                        
                        <input 
                          type="time" 
                          value={schedule.startTime} 
                          onChange={(e) => handleScheduleChange(idx, 'startTime', e.target.value)} 
                          className="px-3 py-2 border-2 border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" 
                        />
                        
                        <span className="text-gray-400">→</span>
                        
                        <input 
                          type="time" 
                          value={schedule.endTime} 
                          onChange={(e) => handleScheduleChange(idx, 'endTime', e.target.value)} 
                          className="px-3 py-2 border-2 border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" 
                        />
                        
                        {formData.weeklySchedules.length > 1 && (
                          <button 
                            onClick={() => removeSchedule(idx)} 
                            className="ml-auto p-2 text-red-500 hover:bg-red-50 rounded-lg transition-all"
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
                        <span className="font-semibold text-gray-900">{formData.startDate}</span>
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
            <button className="px-6 py-2.5 border-2 border-gray-200 rounded-xl text-sm font-semibold text-gray-700 hover:bg-gray-50 hover:border-gray-300 transition-all shadow-sm">
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
      </main>
    </div>
  );
}




// import React, { useState } from 'react';
// import Sidebar from '../../../components/layout/Sidebar';
// import { 
//   ShieldAlert, Bell, UploadCloud, Info, Plus, Trash2 
// } from 'lucide-react';
// import { useNavigate } from 'react-router-dom';
// import toast from 'react-hot-toast';
// import { classApi } from '../../../api/classApi'; 

// // --- HÀM GEN MÃ BẢO MẬT (ĐỂ NGOÀI COMPONENT) ---
// export const generateSecureJoinCode = (subjectCode) => {
//   let prefix = subjectCode.trim().toUpperCase().replace(/\s+/g, '');
//   if (prefix.length > 2) prefix = prefix.substring(0, 2);
//   const randomPart = Math.random().toString(36).substring(2, 6).toUpperCase();
//   const timeSalt = (Date.now() % 100000).toString().padStart(5, '0');
//   return `${prefix}_${randomPart}_${timeSalt}`;
// };

// export default function CreateClass() {
//   const navigate = useNavigate();
//   const [isSubmitting, setIsSubmitting] = useState(false);

//   // Payload khớp 100% với API Swagger (Bỏ joinCode khởi tạo)
//   const [formData, setFormData] = useState({
//     name: '',
//     courseCode: '',
//     classCode: '',
//     description: '',
//     semester: 'HK1',
//     academicYear: '2025-2026',
//     campus: 'Ho Chi Minh',
//     room: '',
//     approvalMode: 'AUTO',
//     allowAutoJoinOnCheckin: true,
//     totalSessions: 15,
//     maxAllowedAbsences: 3,
//     weeklySchedules: [
//       { dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }
//     ]
//   });

//   const [uiState, setUiState] = useState({
//     strictDevicePolicy: true,
//     qrSessionLength: 15,
//     lateWindow: 10,
//     flagRapid: true,
//     flagLocation: true
//   });

//   const handleChange = (e) => {
//     const { name, value, type, checked } = e.target;
//     setFormData(prev => ({
//       ...prev,
//       [name]: type === 'checkbox' ? checked : value
//     }));
//   };

//   const addSchedule = () => {
//     setFormData(prev => ({
//       ...prev,
//       weeklySchedules: [...prev.weeklySchedules, { dayOfWeek: 'MONDAY', startTime: '07:00', endTime: '09:30' }]
//     }));
//   };

//   const removeSchedule = (index) => {
//     setFormData(prev => ({
//       ...prev,
//       weeklySchedules: prev.weeklySchedules.filter((_, i) => i !== index)
//     }));
//   };

//   const handleScheduleChange = (index, field, value) => {
//     const newSchedules = [...formData.weeklySchedules];
//     newSchedules[index][field] = value;
//     setFormData({ ...formData, weeklySchedules: newSchedules });
//   };

//   // --- LOGIC SUBMIT ĐÃ ĐƯỢC TỐI ƯU THEO YÊU CẦU ---
//   const handleSubmit = async (e) => {
//     e.preventDefault();
    
//     if (!formData.name || !formData.courseCode) {
//       toast.error("Vui lòng nhập Class Title và Course Code!");
//       return;
//     }

//     setIsSubmitting(true);
//     const loadingToastId = toast.loading('Đang khởi tạo lớp học...');

//     try {
//       // Logic sinh mã tham gia tự động ngầm
//       const baseCode = formData.classCode || formData.courseCode || "CLASS";
//       const uniqueJoinCode = generateSecureJoinCode(baseCode);
      
//       const requestPayload = {
//         name: formData.name, 
//         code: `${formData.courseCode}-${formData.classCode || 'ALL'}`, 
//         courseCode: formData.courseCode,
//         classCode: formData.classCode,
//         joinCode: uniqueJoinCode, // Gắn mã ẩn vừa gen vào Payload
//         description: formData.description,
//         semester: formData.semester, 
//         academicYear: formData.academicYear,
//         campus: formData.campus,
//         room: formData.room, 
//         totalSessions: parseInt(formData.totalSessions),
//         maxAllowedAbsences: parseInt(formData.maxAllowedAbsences),
//         weeklySchedules: formData.weeklySchedules,
//         approvalMode: formData.approvalMode, 
//         allowAutoJoinOnCheckin: formData.allowAutoJoinOnCheckin
//       };

//       await classApi.createGroup(requestPayload);
      
//       toast.success(`Tạo lớp thành công! Mã tham gia: ${uniqueJoinCode}`, { id: loadingToastId, duration: 6000 });
      
//       setTimeout(() => {
//         navigate('/classes');
//       }, 2000);

//     } catch (error) {
//       console.error('Lỗi khi tạo lớp:', error);

//       if (error.response?.status === 400) {
//         toast.error("Lỗi: Dữ liệu gửi lên không hợp lệ!", { id: loadingToastId });
//       } else if (error.response?.status === 409) {
//         toast.error("Lỗi: Mã môn học và Mã nhóm này ĐÃ TỒN TẠI trên hệ thống!", { id: loadingToastId, duration: 4000 });
//       } else {
//         const errorMsg = error.response?.data?.message || 'Có lỗi xảy ra khi giao tiếp với máy chủ.';
//         toast.error(`Lỗi tạo lớp: ${errorMsg}`, { id: loadingToastId });
//       }
//     } finally {
//       setIsSubmitting(false);
//     }
//   };

//   return (
//     <div className="flex min-h-screen bg-[#f8fafc] font-sans">
//       <Sidebar />
      
//       <main className="flex-1 ml-64 flex flex-col min-h-screen relative pb-24">
//         <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-10">
//           <div>
//             <div className="flex items-center text-sm text-gray-500 mb-1 gap-2">
//               <span className="hover:underline cursor-pointer" onClick={() => navigate('/classes')}>Class Management</span>
//               <span className="text-gray-300">/</span>
//               <span className="text-gray-900 font-medium">Create New Class</span>
//             </div>
//             <h1 className="text-2xl font-bold text-[#111827]">Create New Class</h1>
//           </div>
//           <button className="p-2 border border-gray-200 rounded-lg text-gray-500 hover:bg-gray-50 transition-colors shadow-sm">
//             <Bell size={18} />
//           </button>
//         </header>

//         <div className="p-8 max-w-6xl w-full mx-auto grid grid-cols-1 lg:grid-cols-12 gap-8">
          
//           {/* CỘT TRÁI */}
//           <div className="lg:col-span-8 space-y-6">
            
//             {/* 1. Class Information */}
//             <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
//               <div className="p-6 border-b border-gray-100 flex items-center gap-3">
//                 <div className="w-6 h-6 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xs font-bold">1</div>
//                 <h2 className="text-lg font-bold text-gray-900">Class Information</h2>
//               </div>
//               <div className="p-6 space-y-5">
//                 <div className="grid grid-cols-2 gap-5">
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Class Title <span className="text-red-500">*</span></label>
//                     <input type="text" name="name" value={formData.name} onChange={handleChange} placeholder="e.g. Data Structures & Algorithms" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm focus:border-red-400 focus:ring-2 focus:ring-red-50 outline-none" />
//                   </div>
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Course Code <span className="text-red-500">*</span></label>
//                     <input type="text" name="courseCode" value={formData.courseCode} onChange={handleChange} placeholder="e.g. CS204" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm focus:border-red-400 focus:ring-2 focus:ring-red-50 outline-none" />
//                   </div>
//                 </div>

//                 {/* Đã gỡ bỏ Join Code và sửa thành grid-cols-2 */}
//                 <div className="grid grid-cols-2 gap-5">
//                    <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Class/Group Code</label>
//                     <input type="text" name="classCode" value={formData.classCode} onChange={handleChange} placeholder="e.g. Group 01" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm focus:border-red-400 focus:ring-2 focus:ring-red-50 outline-none transition-all" />
//                   </div>
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Total Sessions</label>
//                     <input type="number" name="totalSessions" value={formData.totalSessions} onChange={handleChange} className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm focus:border-red-400 focus:ring-2 focus:ring-red-50 outline-none transition-all" />
//                   </div>
//                 </div>

//                 <div className="grid grid-cols-2 gap-5">
//                   <div className="grid grid-cols-2 gap-3">
//                     <div>
//                       <label className="block text-sm font-semibold text-gray-700 mb-1.5">Term <span className="text-red-500">*</span></label>
//                       <select name="semester" value={formData.semester} onChange={handleChange} className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50">
//                         <option value="HK1">HK1</option><option value="HK2">HK2</option><option value="HK3">HK3 (Hè)</option>
//                       </select>
//                     </div>
//                     <div>
//                       <label className="block text-sm font-semibold text-gray-700 mb-1.5">Year <span className="text-red-500">*</span></label>
//                       <select name="academicYear" value={formData.academicYear} onChange={handleChange} className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50">
//                         <option value="2025-2026">2025-2026</option><option value="2026-2027">2026-2027</option>
//                       </select>
//                     </div>
//                   </div>
//                   <div className="grid grid-cols-2 gap-3">
//                      <div>
//                       <label className="block text-sm font-semibold text-gray-700 mb-1.5">Campus</label>
//                       <input type="text" name="campus" value={formData.campus} onChange={handleChange} placeholder="e.g. HCM" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" />
//                     </div>
//                     <div>
//                       <label className="block text-sm font-semibold text-gray-700 mb-1.5">Room</label>
//                       <input type="text" name="room" value={formData.room} onChange={handleChange} placeholder="e.g. A1.102" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" />
//                     </div>
//                   </div>
//                 </div>

//                 <div>
//                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">Meeting Pattern (Schedules)</label>
//                    <div className="space-y-3">
//                      {formData.weeklySchedules.map((schedule, idx) => (
//                        <div key={idx} className="flex items-center gap-3 bg-gray-50 p-3 rounded-lg border border-gray-200">
//                           <select value={schedule.dayOfWeek} onChange={(e) => handleScheduleChange(idx, 'dayOfWeek', e.target.value)} className="px-3 py-1.5 border border-gray-300 rounded text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50">
//                             <option value="MONDAY">Monday</option><option value="TUESDAY">Tuesday</option><option value="WEDNESDAY">Wednesday</option>
//                             <option value="THURSDAY">Thursday</option><option value="FRIDAY">Friday</option><option value="SATURDAY">Saturday</option><option value="SUNDAY">Sunday</option>
//                           </select>
//                           <input type="time" value={schedule.startTime} onChange={(e) => handleScheduleChange(idx, 'startTime', e.target.value)} className="px-3 py-1.5 border border-gray-300 rounded text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" />
//                           <span className="text-gray-400">to</span>
//                           <input type="time" value={schedule.endTime} onChange={(e) => handleScheduleChange(idx, 'endTime', e.target.value)} className="px-3 py-1.5 border border-gray-300 rounded text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50" />
                          
//                           {formData.weeklySchedules.length > 1 && (
//                             <button onClick={() => removeSchedule(idx)} className="ml-auto p-1.5 text-red-500 hover:bg-red-50 rounded"><Trash2 size={16}/></button>
//                           )}
//                        </div>
//                      ))}
//                      <button onClick={addSchedule} className="text-sm font-semibold text-indigo-600 flex items-center gap-1 hover:underline"><Plus size={14}/> Add another session</button>
//                    </div>
//                 </div>

//                 <div>
//                    <label className="block text-sm font-semibold text-gray-700 mb-1.5">Description</label>
//                    <textarea name="description" value={formData.description} onChange={handleChange} rows="2" className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm outline-none resize-none focus:border-red-400 focus:ring-2 focus:ring-red-50"></textarea>
//                 </div>
//               </div>
//             </div>

//             {/* 2. Roster & Device Setup */}
//             <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
//               <div className="p-6 border-b border-gray-100 flex items-center gap-3">
//                 <div className="w-6 h-6 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center text-xs font-bold">2</div>
//                 <h2 className="text-lg font-bold text-gray-900">Roster & Device Setup</h2>
//               </div>
//               <div className="p-6 space-y-6">
//                 <div>
//                   <label className="block text-sm font-semibold text-gray-700 mb-2">Import Student Roster (CSV)</label>
//                   <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 flex flex-col items-center justify-center text-center hover:bg-gray-50 transition-colors cursor-pointer">
//                     <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center mb-3">
//                       <UploadCloud size={20} className="text-gray-500" />
//                     </div>
//                     <p className="text-sm font-semibold text-gray-900">Click to upload or drag and drop</p>
//                     <p className="text-xs text-gray-500 mt-1">CSV files only (Max 5MB). <span className="text-red-600 hover:underline cursor-pointer">Download template</span></p>
//                   </div>
//                 </div>

//                 <div className="grid grid-cols-2 gap-5">
//                    <div>
//                       <label className="block text-sm font-semibold text-gray-700 mb-1.5">Student Approval Mode</label>
//                       <select name="approvalMode" value={formData.approvalMode} onChange={handleChange} className="w-full px-4 py-2 border border-gray-200 rounded-lg text-sm bg-white outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50">
//                         <option value="AUTO">Auto (Join immediately)</option>
//                         <option value="MANUAL">Manual (Requires approval)</option>
//                       </select>
//                    </div>
//                    <div className="flex items-center mt-6">
//                       <label className="flex items-center gap-3 cursor-pointer">
//                         <input type="checkbox" name="allowAutoJoinOnCheckin" checked={formData.allowAutoJoinOnCheckin} onChange={handleChange} className="w-4 h-4 text-red-600 rounded border-gray-300 focus:ring-red-500" />
//                         <span className="text-sm font-semibold text-gray-700">Allow Auto-Join on first Check-in scan</span>
//                       </label>
//                    </div>
//                 </div>

//                 <div className="flex items-start gap-4 p-4 border border-gray-200 rounded-xl bg-gray-50/50">
//                   <div className="mt-0.5"><div className="w-8 h-8 bg-white border border-gray-200 rounded-lg flex items-center justify-center"><ShieldAlert size={16} className="text-red-500"/></div></div>
//                   <div className="flex-1">
//                     <div className="flex justify-between items-center mb-1">
//                       <h4 className="font-semibold text-gray-900 text-sm">Strict Device Policy</h4>
//                       <div className="w-10 h-6 bg-red-600 rounded-full relative cursor-pointer"><div className="w-4 h-4 bg-white rounded-full absolute right-1 top-1"></div></div>
//                     </div>
//                     <p className="text-xs text-gray-500 leading-relaxed">Lock attendance to a single registered device per student. Prevents sharing credentials or remote check-ins from unauthorized devices.</p>
//                   </div>
//                 </div>
//               </div>
//             </div>

//             {/* 3. Attendance & Fraud Rules */}
//             <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden mb-8">
//               <div className="p-6 border-b border-gray-100 flex items-center gap-3">
//                 <div className="w-6 h-6 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center text-xs font-bold">3</div>
//                 <h2 className="text-lg font-bold text-gray-900">Attendance & Fraud Rules</h2>
//               </div>
//               <div className="p-6">
//                 <div className="grid grid-cols-3 gap-6 mb-6">
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Max Absences</label>
//                     <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden focus-within:border-red-400 focus-within:ring-2 focus-within:ring-red-50">
//                       <input type="number" name="maxAllowedAbsences" value={formData.maxAllowedAbsences} onChange={handleChange} className="w-full px-4 py-2 text-sm outline-none" />
//                       <span className="px-3 bg-gray-50 text-gray-500 text-sm border-l border-gray-200">sessions</span>
//                     </div>
//                   </div>
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">QR Rotation</label>
//                     <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden focus-within:border-red-400 focus-within:ring-2 focus-within:ring-red-50">
//                       <input type="number" value={uiState.qrSessionLength} onChange={(e) => setUiState({...uiState, qrSessionLength: e.target.value})} className="w-full px-4 py-2 text-sm outline-none" />
//                       <span className="px-3 bg-gray-50 text-gray-500 text-sm border-l border-gray-200">seconds</span>
//                     </div>
//                   </div>
//                   <div>
//                     <label className="block text-sm font-semibold text-gray-700 mb-1.5">Late Window</label>
//                     <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden focus-within:border-red-400 focus-within:ring-2 focus-within:ring-red-50">
//                       <input type="number" value={uiState.lateWindow} onChange={(e) => setUiState({...uiState, lateWindow: e.target.value})} className="w-full px-4 py-2 text-sm outline-none" />
//                       <span className="px-3 bg-gray-50 text-gray-500 text-sm border-l border-gray-200">minutes</span>
//                     </div>
//                   </div>
//                 </div>

//                 <div className="mb-2"><label className="block text-sm font-semibold text-gray-900">Auto-Flag Fraud Thresholds</label></div>
//                 <div className="space-y-3">
//                   <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
//                     <div className="flex items-center gap-3">
//                       <input type="checkbox" checked={uiState.flagRapid} onChange={(e) => setUiState({...uiState, flagRapid: e.target.checked})} className="w-4 h-4 text-red-600 rounded border-gray-300 focus:ring-red-500" />
//                       <span className="text-sm font-semibold text-gray-800">Rapid succession check-ins</span>
//                     </div>
//                     <span className="text-xs text-gray-400">Multiple scans {'<'} 2s</span>
//                   </label>
//                   <label className="flex items-center justify-between p-3 border border-gray-200 rounded-lg cursor-pointer hover:bg-gray-50">
//                     <div className="flex items-center gap-3">
//                       <input type="checkbox" checked={uiState.flagLocation} onChange={(e) => setUiState({...uiState, flagLocation: e.target.checked})} className="w-4 h-4 text-red-600 rounded border-gray-300 focus:ring-red-500" />
//                       <span className="text-sm font-semibold text-gray-800">Location mismatch (GPS)</span>
//                     </div>
//                     <span className="text-xs text-gray-400">Outside campus geofence</span>
//                   </label>
//                 </div>
//               </div>
//             </div>

//           </div>

//           {/* CỘT PHẢI: SUMMARY SIDEBAR */}
//           <div className="lg:col-span-4">
//             <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-6 sticky top-24">
//               <h3 className="text-lg font-bold text-gray-900 mb-5">Class Summary</h3>
              
//               <div className="space-y-5">
//                 <div>
//                   <p className="text-xs font-bold text-gray-400 tracking-wider mb-1.5">STATUS</p>
//                   <span className="px-3 py-1 bg-amber-100 text-amber-700 text-xs font-bold rounded">Draft</span>
//                 </div>
                
//                 <div className="border-t border-gray-100 pt-5">
//                   <p className="text-xs font-bold text-gray-400 tracking-wider mb-2">SECURITY LEVEL</p>
//                   <div className="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
//                     <ShieldAlert size={16} className="text-emerald-500" /> High (Device Lock On)
//                   </div>
//                 </div>

//                 <div className="bg-gray-50 rounded-xl p-4 border border-gray-100 mt-6">
//                   <h4 className="flex items-center gap-2 font-bold text-gray-900 text-sm mb-2">
//                     <Info size={16} className="text-red-500"/> What happens next?
//                   </h4>
//                   <p className="text-xs text-gray-500 leading-relaxed">
//                     Once created, students will receive an email invitation to register their devices. You can start generating QR sessions immediately if Auto-Join is enabled.
//                   </p>
//                 </div>
//               </div>
//             </div>
//           </div>

//         </div>

//         {/* BOTTOM ACTION BAR */}
//         <div className="fixed bottom-0 right-0 left-64 bg-white border-t border-gray-200 px-8 py-4 flex justify-between items-center z-10">
//           <button onClick={() => navigate('/classes')} className="text-sm font-semibold text-gray-500 hover:text-gray-900 transition-colors">
//             Cancel
//           </button>
//           <div className="flex gap-3">
//             <button className="px-6 py-2.5 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors shadow-sm">
//               Save Draft
//             </button>
//             <button 
//               onClick={handleSubmit} 
//               disabled={isSubmitting}
//               className="px-6 py-2.5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-bold transition-colors shadow-sm disabled:opacity-50 flex items-center gap-2"
//             >
//               {isSubmitting ? <span className="animate-spin text-white">O</span> : null}
//               Create Class →
//             </button>
//           </div>
//         </div>

//       </main>
//     </div>
//   );
// }

