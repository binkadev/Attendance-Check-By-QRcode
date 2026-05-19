import { useState, useEffect } from 'react';
import { 
  Search, Bell, Plus, UserCheck, Monitor, Inbox, AlertTriangle, 
  Calendar as CalendarIcon, MapPin, ArrowRight, Smartphone, FileText, Clock, Timer, ShieldAlert
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import Sidebar from '../../../components/layout/Sidebar';
import { 
  ComposedChart, Line, Bar, Area, XAxis, YAxis, CartesianGrid, 
  Tooltip, ResponsiveContainer, Legend 
} from 'recharts';
import { classApi } from '../../../api/classApi';
import { parseDeviceName } from '../../../utils/formatters';

const SUBJECT_COLORS = [
  '#3B82F6', // Blue (Giải tích / CSDL)
  '#10B981', // Emerald (Lập trình / Web)
  '#F59E0B', // Amber (Mạng / IoT)
  '#EF4444', // Red (Bảo mật / AI)
  '#8B5CF6', // Purple
  '#EC4899', // Pink
  '#06B6D4', // Cyan
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
  fraud: { label: 'Phát hiện gian lận', icon: <ShieldAlert size={20} className="text-red-500" /> },
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
  const [chartPeriod, setChartPeriod] = useState('day');
  const [classes, setClasses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [selectedSemester, setSelectedSemester] = useState('all');
  const [isLoadingChart, setIsLoadingChart] = useState(false);
  const [classSessions, setClassSessions] = useState([]);
  const [chartData, setChartData] = useState([]);
  const [chartKeys, setChartKeys] = useState([]);

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

  // KPI states
  const [attendanceRate, setAttendanceRate] = useState(null);
  const [activeClassCount, setActiveClassCount] = useState(0);
  const [topFraudClassId, setTopFraudClassId] = useState(null);

  // Thêm state trigger để ép component re-render mỗi phút cập nhật trạng thái "Now"
  // eslint-disable-next-line no-unused-vars
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

        const activeCount = todayList.filter(cls => {
          const status = getSessionStatus(cls.startAt, cls.endAt);
          return status === 'now' || status === 'future';
        }).length;
        setActiveClassCount(activeCount);
      } catch (error) {
        console.error("Failed to fetch schedule:", error);
        setTodayClasses([]);
        setTomorrowClasses([]);
        setActiveClassCount(0);
      } finally {
        setIsLoadingSchedule(false);
      }
    };

    const fetchAlertsAndStats = async () => {
      try {
        setIsLoadingAlerts(true);
        const classesData = await classApi.getTeachingClasses();
        const classesList = classesData.items || [];
        setClasses(classesList);
        
        let allAlerts = [];
        // Theo dõi class có nhiều sự cố gian lận nhất để điều hướng "Xem chi tiết"
        const fraudCountPerClass = {};

        // Tính tỷ lệ điểm danh hôm nay từ summary các lớp
        let totalPresent = 0;
        let totalExpected = 0;

        await Promise.all(classesList.map(async (cls) => {
          try {
            const classId = cls.groupId || cls.id;
            const [fraudRes, absenceRes, summaryRes] = await Promise.all([
              classApi.getFraudIncidents(classId).catch(() => null),
              classApi.getAbsenceRequests(classId).catch(() => null),
              classApi.getAttendanceSummary(classId).catch(() => null)
            ]);

            // Tỷ lệ điểm danh
            if (summaryRes) {
              const present = summaryRes.totalPresent ?? summaryRes.presentCount ?? 0;
              const total = summaryRes.totalStudents ?? summaryRes.totalEnrolled ?? 0;
              if (total > 0) {
                totalPresent += present;
                totalExpected += total;
              }
            }

            const frauds = fraudRes?.items || fraudRes || [];
            const absences = absenceRes?.items || absenceRes || [];

            const openFrauds = frauds.filter(f => f.status === 'OPEN' || f.status === 'PENDING');

            // Ghi nhận class có nhiều sự cố nhất
            if (openFrauds.length > 0) {
              fraudCountPerClass[classId] = (fraudCountPerClass[classId] || 0) + openFrauds.length;
            }

            allAlerts.push(...frauds.map(f => ({ ...f, type: 'fraud', classInfo: { ...cls, id: classId } })));
            allAlerts.push(...absences.map(a => ({ ...a, type: 'absence', classInfo: { ...cls, id: classId } })));
          } catch (e) {
            console.error(`Failed to fetch alerts for class`, e);
          }
        }));

        // Sắp xếp các cảnh báo theo thời gian mới nhất lên trước
        allAlerts.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

        // Tính lại số lượng dựa trên mảng thực tế từ API
        const calculatedFraudCount = allAlerts.filter(f => f.type === 'fraud' && (f.status === 'OPEN' || f.status === 'PENDING')).length;
        const calculatedAbsenceCount = allAlerts.filter(a => a.type === 'absence' && a.status === 'PENDING').length;

        // Trích xuất bằng chứng thiết bị cho 5 sự cố đầu tiên (nếu là fraud và chưa có)
        const top5 = allAlerts.slice(0, 5);
        await Promise.all(top5.map(async (alert) => {
          if (alert.type === 'fraud' && !alert.evidenceSummary) {
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

        setAlerts(allAlerts);
        setDashboardStats({ fraudCount: calculatedFraudCount, absenceCount: calculatedAbsenceCount });

        // Cập nhật các chỉ số KPI từ dữ liệu thực tế
        if (totalExpected > 0) {
          setAttendanceRate(((totalPresent / totalExpected) * 100).toFixed(1));
        } else {
          setAttendanceRate(null);
        }

        // Lớp có nhiều sự cố nhất
        const topClassEntry = Object.entries(fraudCountPerClass).sort((a, b) => b[1] - a[1])[0];
        if (topClassEntry) {
          setTopFraudClassId(topClassEntry[0]);
        } else {
          setTopFraudClassId(null);
        }
      } catch (error) {
        console.error("Failed to fetch teaching classes for alerts:", error);
        setAlerts([]);
        setDashboardStats({ fraudCount: 0, absenceCount: 0 });
        setAttendanceRate(null);
      } finally {
        setIsLoadingAlerts(false);
      }
    };

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
        start = new Date(year1, 7, 1);
        end = new Date(year1, 11, 31, 23, 59, 59);
      } else if (semester === 'HK2') {
        // Học kỳ 2: Tháng 1 đến Tháng 5 năm học sau
        start = new Date(year2, 0, 1);
        end = new Date(year2, 4, 31, 23, 59, 59);
      } else if (semester === 'HK3' || semester === 'SUMMER') {
        // Học kỳ Hè: Tháng 6 đến Tháng 8 năm học sau
        start = new Date(year2, 5, 1);
        end = new Date(year2, 7, 31, 23, 59, 59);
      }
      
      return { start, end };
    };

    const findSemesterForDate = (date, semList) => {
      for (const sem of semList) {
        const range = getSemesterDateRange(sem.semester, sem.academicYear);
        if (range && date >= range.start && date <= range.end) {
          return sem;
        }
      }
      return null;
    };

    const fetchSemesters = async () => {
      try {
        const res = await classApi.getSemesters();
        const semList = Array.isArray(res) ? res : [];
        setSemesters(semList);
        
        // Chọn mặc định học kỳ tương ứng với thời gian hiện tại
        if (semList.length > 0) {
          const now = new Date();
          const matchedSem = findSemesterForDate(now, semList);
          
          if (matchedSem) {
            setSelectedSemester(`${matchedSem.semester}|${matchedSem.academicYear}`);
          } else {
            // Fallback: chọn học kỳ mới nhất theo thời gian nếu không khớp học kỳ nào
            const sorted = [...semList].sort((a, b) => {
              if (a.academicYear !== b.academicYear) {
                return b.academicYear.localeCompare(a.academicYear);
              }
              const semOrder = { 'HK3': 3, 'HK2': 2, 'HK1': 1 };
              return (semOrder[b.semester] || 0) - (semOrder[a.semester] || 0);
            });
            setSelectedSemester(`${sorted[0].semester}|${sorted[0].academicYear}`);
          }
        }
      } catch (error) {
        console.error("Failed to fetch semesters:", error);
      }
    };

    fetchSchedule();
    fetchAlertsAndStats();
    fetchSemesters();
    
    // Cập nhật lại UI mỗi 60 giây để status chuyển từ Future -> Now -> Past mượt mà
    const interval = setInterval(() => setTimeTrigger(prev => prev + 1), 60000);
    return () => clearInterval(interval);
  }, []);

  // Effect để tự động tải dữ liệu các phiên học cho biểu đồ khi classes hoặc selectedSemester thay đổi
  useEffect(() => {
    if (!classes || classes.length === 0) {
      setClassSessions([]);
      return;
    }

    const loadChartSessions = async () => {
      try {
        setIsLoadingChart(true);
        let filteredClasses = classes;
        
        if (selectedSemester && selectedSemester !== 'all') {
          const [sem, year] = selectedSemester.split('|');
          filteredClasses = classes.filter(cls => cls.semester === sem && cls.academicYear === year);
        }

        // Hiện tối đa 10 môn học trong kỳ của giảng viên thay vì cứng 5 môn
        const targetClasses = filteredClasses.slice(0, 10);

        const classSessionsPromises = targetClasses.map(async (cls) => {
          try {
            const classId = cls.groupId || cls.id;
            const res = await classApi.getGroupSessions(classId);
            const realSessions = res?.items || [];
            
            const getSessionDateStr = (sess) => {
              const dateObj = new Date(sess.checkinOpenAt || sess.createdAt);
              return `${dateObj.getFullYear()}-${String(dateObj.getMonth() + 1).padStart(2, '0')}-${String(dateObj.getDate()).padStart(2, '0')}`;
            };

            // Nhóm các phiên thực tế theo ngày
            const sessionsByDate = {};
            realSessions.forEach(sess => {
              const dateStr = getSessionDateStr(sess);
              if (!sessionsByDate[dateStr]) {
                sessionsByDate[dateStr] = [];
              }
              sessionsByDate[dateStr].push(sess);
            });

            // Chọn phiên cuối cùng của mỗi ngày làm đại diện
            const uniqueRealSessionsMap = new Map();
            Object.keys(sessionsByDate).forEach(dateStr => {
              const daySessions = sessionsByDate[dateStr];
              // Sắp xếp tăng dần theo thời gian để lấy phiên cuối cùng trong ngày
              daySessions.sort((a, b) => new Date(a.checkinOpenAt || a.createdAt) - new Date(b.checkinOpenAt || b.createdAt));
              uniqueRealSessionsMap.set(dateStr, daySessions[daySessions.length - 1]);
            });

            // Lấy danh sách ngày dự kiến đã trôi qua dựa trên lịch học hàng tuần và tổng số buổi học
            const scheduledDates = [];
            const now = new Date();
            const todayEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate(), 23, 59, 59);
            const startStr = cls.startDate || cls.createdAt || new Date().toISOString();
            const startDate = new Date(startStr);
            startDate.setHours(0, 0, 0, 0);
            
            const totalSessions = cls.totalSessions || 15;
            const weeklySchedules = cls.weeklySchedules || [];

            if (weeklySchedules.length > 0) {
              let current = new Date(startDate);
              let sessionCount = 0;
              const dayOfWeekMap = {
                'SUNDAY': 0, 'MONDAY': 1, 'TUESDAY': 2, 'WEDNESDAY': 3,
                'THURSDAY': 4, 'FRIDAY': 5, 'SATURDAY': 6
              };
              const activeDays = weeklySchedules.map(s => dayOfWeekMap[s.dayOfWeek]);

              while (current <= todayEnd && sessionCount < totalSessions) {
                if (activeDays.includes(current.getDay())) {
                  const ymd = `${current.getFullYear()}-${String(current.getMonth() + 1).padStart(2, '0')}-${String(current.getDate()).padStart(2, '0')}`;
                  scheduledDates.push(ymd);
                  sessionCount++;
                }
                current.setDate(current.getDate() + 1);
              }
            } else {
              // Fallback nếu không có weeklySchedules
              for (let j = 0; j < totalSessions; j++) {
                const expectedDate = new Date(startDate.getTime() + j * 7 * 24 * 60 * 60 * 1000);
                if (expectedDate <= todayEnd) {
                  const ymd = `${expectedDate.getFullYear()}-${String(expectedDate.getMonth() + 1).padStart(2, '0')}-${String(expectedDate.getDate()).padStart(2, '0')}`;
                  scheduledDates.push(ymd);
                }
              }
            }

            // Gộp tất cả các ngày và sắp xếp theo thứ tự thời gian tăng dần
            const allDatesSet = new Set([
              ...scheduledDates,
              ...uniqueRealSessionsMap.keys()
            ]);
            const sortedAllDates = Array.from(allDatesSet).sort(
              (a, b) => new Date(a) - new Date(b)
            );

            // Xây dựng danh sách phiên hoàn chỉnh có đếm sự kiện thực tế
            const finalSessions = [];
            for (const dateStr of sortedAllDates) {
              const realSess = uniqueRealSessionsMap.get(dateStr);
              if (realSess) {
                try {
                  const eventRes = await classApi.getAttendanceEvents(realSess.id, 200);
                  const allEvents = Array.isArray(eventRes) ? eventRes : (eventRes.items || []);
                  const latestStatusMap = new Map();
                  const sorted = [...allEvents].sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
                  sorted.forEach(e => latestStatusMap.set(String(e.userId), e.newStatus));
                  let count = 0;
                  latestStatusMap.forEach(status => {
                    if (status === 'PRESENT' || status === 'LATE') count++;
                  });
                  finalSessions.push({
                    ...realSess,
                    actualCheckIns: count,
                    dateStr
                  });
                } catch (err) {
                  finalSessions.push({
                    ...realSess,
                    actualCheckIns: realSess.checkIns || realSess.checkinCount || 0,
                    dateStr
                  });
                }
              } else {
                finalSessions.push({
                  id: `missed-${classId}-${dateStr}`,
                  actualCheckIns: 0,
                  checkinOpenAt: new Date(dateStr).toISOString(),
                  dateStr,
                  isMissed: true
                });
              }
            }

            return {
              classId,
              groupName: cls.groupName || cls.name || 'Không rõ lớp',
              courseCode: cls.courseCode || '',
              classCode: cls.classCode || '',
              semester: cls.semester || cls.term || 'Kỳ 1 2025',
              sessions: finalSessions,
              totalEnrolled: cls.approvedStudentCount || 30
            };
          } catch (e) {
            console.error("Failed to load sessions for chart:", e);
            return null;
          }
        });

        const classSessionsResults = (await Promise.all(classSessionsPromises)).filter(Boolean);
        setClassSessions(classSessionsResults);
      } catch (error) {
        console.error("Failed to load sessions for chart:", error);
        setClassSessions([]);
      } finally {
        setIsLoadingChart(false);
      }
    };

    loadChartSessions();
  }, [classes, selectedSemester]);

  useEffect(() => {
    if (!classSessions || classSessions.length === 0) {
      setChartData([]);
      setChartKeys([]);
      return;
    }

    let newChartData = [];
    let newChartKeys = [];

    classSessions.forEach(c => {
      const key = `${c.courseCode}${c.classCode ? ` - ${c.classCode}` : ''} - ${c.groupName}`;
      if (key && !newChartKeys.includes(key)) {
        newChartKeys.push(key);
      }
    });

    if (chartPeriod === 'day') {
      const allDatesAcrossClasses = new Set();
      classSessions.forEach(c => {
        c.sessions.forEach(sess => {
          if (sess.dateStr) {
            allDatesAcrossClasses.add(sess.dateStr);
          }
        });
      });

      const sortedGlobalDates = Array.from(allDatesAcrossClasses).sort(
        (a, b) => new Date(a) - new Date(b)
      );

      const finalGlobalDates = sortedGlobalDates;

      if (finalGlobalDates.length > 0) {
        finalGlobalDates.forEach(dateStr => {
          const dateObj = new Date(dateStr);
          const formattedLabel = `${String(dateObj.getDate()).padStart(2, '0')}/${String(dateObj.getMonth() + 1).padStart(2, '0')}`;
          
          const chartItem = { name: formattedLabel, rawDate: dateStr };
          let sumRates = 0;
          let validClassesCount = 0;
          
          classSessions.forEach(c => {
            const sessionIndex = c.sessions.findIndex(s => s.dateStr === dateStr);
            if (sessionIndex !== -1) {
              const sessionOnDate = c.sessions[sessionIndex];
              const checkIns = sessionOnDate.actualCheckIns ?? (sessionOnDate.checkIns || sessionOnDate.checkinCount || 0);
              const total = c.totalEnrolled || 30;
              const rate = total > 0 ? Math.round((checkIns / total) * 100) : 0;
              
              const key = `${c.courseCode}${c.classCode ? ` - ${c.classCode}` : ''} - ${c.groupName}`;
              chartItem[key] = rate;
              chartItem[`${key}_sessionLabel`] = `Buổi ${sessionIndex + 1}`;
              
              sumRates += rate;
              validClassesCount++;
            }
          });
          
          if (validClassesCount > 0) {
            chartItem['Trung bình'] = Math.round(sumRates / validClassesCount);
          }
          newChartData.push(chartItem);
        });
      }
    } else if (chartPeriod === 'week') {
      const getMondayStr = (dStr) => {
        const d = new Date(dStr);
        const day = d.getDay();
        const diff = d.getDate() - day + (day === 0 ? -6 : 1);
        const monday = new Date(d.setDate(diff));
        return `${monday.getFullYear()}-${String(monday.getMonth() + 1).padStart(2, '0')}-${String(monday.getDate()).padStart(2, '0')}`;
      };

      const allWeeksAcrossClasses = new Set();
      classSessions.forEach(c => {
        c.sessions.forEach(sess => {
          if (sess.dateStr) {
            allWeeksAcrossClasses.add(getMondayStr(sess.dateStr));
          }
        });
      });

      const sortedGlobalWeeks = Array.from(allWeeksAcrossClasses).sort(
        (a, b) => new Date(a) - new Date(b)
      );

      const finalGlobalWeeks = sortedGlobalWeeks;

      if (finalGlobalWeeks.length > 0) {
        finalGlobalWeeks.forEach(mondayStr => {
          const mondayDate = new Date(mondayStr);
          const formattedLabel = `T. ${String(mondayDate.getDate()).padStart(2, '0')}/${String(mondayDate.getMonth() + 1).padStart(2, '0')}`;
          
          const chartItem = { name: formattedLabel, rawDate: mondayStr };
          let sumRates = 0;
          let validClassesCount = 0;
          
          classSessions.forEach(c => {
            const sessionsInWeek = [];
            c.sessions.forEach((s, idx) => {
              if (getMondayStr(s.dateStr) === mondayStr) {
                sessionsInWeek.push({ session: s, index: idx });
              }
            });

            if (sessionsInWeek.length > 0) {
              let sumVal = 0;
              sessionsInWeek.forEach(item => {
                const s = item.session;
                const checkIns = s.actualCheckIns ?? (s.checkIns || s.checkinCount || 0);
                const total = c.totalEnrolled || 30;
                sumVal += total > 0 ? Math.round((checkIns / total) * 100) : 0;
              });
              const rate = Math.round(sumVal / sessionsInWeek.length);
              
              const key = `${c.courseCode}${c.classCode ? ` - ${c.classCode}` : ''} - ${c.groupName}`;
              chartItem[key] = rate;
              
              const labelIndices = sessionsInWeek.map(item => item.index + 1);
              chartItem[`${key}_sessionLabel`] = `Buổi ${labelIndices.join('-')}`;
              
              sumRates += rate;
              validClassesCount++;
            }
          });
          
          if (validClassesCount > 0) {
            chartItem['Trung bình'] = Math.round(sumRates / validClassesCount);
          }
          newChartData.push(chartItem);
        });
      }
    } else if (chartPeriod === 'month') {
      const allMonthsAcrossClasses = new Set();
      classSessions.forEach(c => {
        c.sessions.forEach(sess => {
          if (sess.dateStr) {
            allMonthsAcrossClasses.add(sess.dateStr.substring(0, 7));
          }
        });
      });

      const sortedGlobalMonths = Array.from(allMonthsAcrossClasses).sort(
        (a, b) => new Date(a + "-01") - new Date(b + "-01")
      );

      const finalGlobalMonths = sortedGlobalMonths.slice(-10);

      if (finalGlobalMonths.length > 0) {
        finalGlobalMonths.forEach(monthStr => {
          const parts = monthStr.split('-');
          const formattedLabel = `T. ${parts[1]}`;
          
          const chartItem = { name: formattedLabel, rawDate: monthStr };
          let sumRates = 0;
          let validClassesCount = 0;
          
          classSessions.forEach(c => {
            const sessionsInMonth = [];
            c.sessions.forEach((s, idx) => {
              if (s.dateStr.substring(0, 7) === monthStr) {
                sessionsInMonth.push({ session: s, index: idx });
              }
            });

            if (sessionsInMonth.length > 0) {
              let sumVal = 0;
              sessionsInMonth.forEach(item => {
                const s = item.session;
                const checkIns = s.actualCheckIns ?? (s.checkIns || s.checkinCount || 0);
                const total = c.totalEnrolled || 30;
                sumVal += total > 0 ? Math.round((checkIns / total) * 100) : 0;
              });
              const rate = Math.round(sumVal / sessionsInMonth.length);
              
              const key = `${c.courseCode}${c.classCode ? ` - ${c.classCode}` : ''} - ${c.groupName}`;
              chartItem[key] = rate;
              
              const labelIndices = sessionsInMonth.map(item => item.index + 1);
              const labelText = labelIndices.length > 2 
                ? `Buổi ${labelIndices[0]}-${labelIndices[labelIndices.length - 1]}`
                : `Buổi ${labelIndices.join('-')}`;
              chartItem[`${key}_sessionLabel`] = labelText;
              
              sumRates += rate;
              validClassesCount++;
            }
          });
          
          if (validClassesCount > 0) {
            chartItem['Trung bình'] = Math.round(sumRates / validClassesCount);
          }
          newChartData.push(chartItem);
        });
      }
    } else if (chartPeriod === 'semester') {
      const allSemestersAcrossClasses = new Set();
      classSessions.forEach(c => {
        if (c.semester) {
          allSemestersAcrossClasses.add(c.semester);
        }
      });

      const sortedGlobalSemesters = Array.from(allSemestersAcrossClasses).sort();

      if (sortedGlobalSemesters.length > 0) {
        sortedGlobalSemesters.forEach(semStr => {
          const chartItem = { name: semStr };
          let sumRates = 0;
          let validClassesCount = 0;
          
          classSessions.forEach(c => {
            if (c.semester === semStr && c.sessions.length > 0) {
              let sumVal = 0;
              c.sessions.forEach(s => {
                const checkIns = s.actualCheckIns ?? (s.checkIns || s.checkinCount || 0);
                const total = c.totalEnrolled || 30;
                sumVal += total > 0 ? Math.round((checkIns / total) * 100) : 0;
              });
              const rate = Math.round(sumVal / c.sessions.length);
              
              const key = `${c.courseCode}${c.classCode ? ` - ${c.classCode}` : ''} - ${c.groupName}`;
              chartItem[key] = rate;
              chartItem[`${key}_sessionLabel`] = `Cả kỳ (1-${c.sessions.length})`;
              
              sumRates += rate;
              validClassesCount++;
            }
          });
          
          if (validClassesCount > 0) {
            chartItem['Trung bình'] = Math.round(sumRates / validClassesCount);
          }
          newChartData.push(chartItem);
        });
      }
    }

    setChartData(newChartData);
    setChartKeys(newChartKeys);
  }, [classSessions, chartPeriod]);

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
            {/* Card 1: Tỷ lệ đi học hôm nay */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-emerald-50 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-gray-500 mb-1">Tỷ lệ đi học hôm nay</p>
                  <h3 className="text-3xl font-bold text-gray-900">
                    {isLoadingAlerts ? '...' : (attendanceRate !== null ? `${attendanceRate}%` : '--')}
                  </h3>
                </div>
                <div className="w-10 h-10 bg-emerald-50 rounded-full flex items-center justify-center text-emerald-600">
                  <UserCheck size={20} />
                </div>
              </div>
              <div className="flex items-center gap-2 text-sm">
                <span className="text-emerald-600 font-bold bg-emerald-50 px-2 py-0.5 rounded flex items-center gap-1">
                  Hôm nay
                </span>
                <span className="text-gray-400 font-medium">tổng hợp tất cả lớp</span>
              </div>
            </div>

            {/* Card 2: Lớp học đang hoạt động */}
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-24 h-24 bg-blue-50 rounded-bl-full -mr-4 -mt-4 opacity-50"></div>
              <div className="flex justify-between items-start mb-4">
                <div>
                  <p className="text-sm font-semibold text-gray-500 mb-1">Lớp học đang hoạt động</p>
                  <h3 className="text-3xl font-bold text-gray-900">
                    {isLoadingSchedule ? '...' : activeClassCount}
                  </h3>
                </div>
                <div className="w-10 h-10 bg-blue-50 rounded-full flex items-center justify-center text-blue-600">
                  <Monitor size={20} />
                </div>
              </div>
              <div className="flex items-center gap-2 text-sm">
                {activeClassCount > 0 ? (
                  <span className="text-blue-600 font-bold bg-blue-50 px-2 py-0.5 rounded flex items-center gap-1.5">
                    <span className="w-1.5 h-1.5 rounded-full bg-blue-600 animate-pulse"></span> Đang diễn ra
                  </span>
                ) : (
                  <span className="text-gray-400 font-medium">Không có lớp đang diễn ra</span>
                )}
              </div>
            </div>

            {/* Card 3: Đơn xin nghỉ chờ duyệt */}
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

            {/* Card 4: Sự cố gian lận – có điều hướng đến tab fraud */}
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
                  {dashboardStats.fraudCount > 0 ? `${dashboardStats.fraudCount} cần xử lý` : 'Không có sự cố'}
                </span>
                {topFraudClassId ? (
                  <button
                    onClick={() => navigate(`/classes/${topFraudClassId}`, { state: { activeTab: 'fraud' } })}
                    className="text-red-600 font-bold hover:underline flex items-center gap-1 transition-all"
                  >
                    Xem chi tiết <ArrowRight size={14} />
                  </button>
                ) : (
                  <span className="text-red-400 flex items-center gap-1">
                    Xem chi tiết <ArrowRight size={14} />
                  </span>
                )}
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            
            {/* LEFT COLUMN */}
            <div className="lg:col-span-8 flex flex-col gap-6">
              <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
                <div className="flex justify-between items-center flex-wrap gap-4 mb-6">
                  <div>
                    <h3 className="text-lg font-bold text-gray-900">Xu hướng điểm danh</h3>
                    <p className="text-sm text-gray-500">So sánh hàng tuần giữa các khóa học</p>
                  </div>
                  <div className="flex items-center gap-3">
                    {/* HỘP CHỌN HỌC KỲ DƯỚI DẠNG SELECT DROPDOWN */}
                    {semesters.length > 0 && (
                      <select
                        value={selectedSemester}
                        onChange={(e) => setSelectedSemester(e.target.value)}
                        className="bg-gray-50 border border-gray-200 text-gray-700 text-sm font-semibold rounded-lg px-3 py-1.5 focus:bg-white focus:border-red-300 focus:ring-2 focus:ring-red-100 outline-none transition-all cursor-pointer shadow-sm"
                      >
                        <option value="all">Tất cả học kỳ</option>
                        {semesters.map((sem) => {
                          const label = `${sem.semester === 'HK1' ? 'Kỳ 1' : sem.semester === 'HK2' ? 'Kỳ 2' : 'Kỳ Hè'} (${sem.academicYear})`;
                          return (
                            <option key={`${sem.semester}|${sem.academicYear}`} value={`${sem.semester}|${sem.academicYear}`}>
                              {label}
                            </option>
                          );
                        })}
                      </select>
                    )}

                    <div className="flex bg-gray-50 border border-gray-200 rounded-lg p-1">
                      <button 
                        onClick={() => setChartPeriod('day')}
                        className={`px-3 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'day' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                      >
                        Theo ngày
                      </button>
                      <button 
                        onClick={() => setChartPeriod('week')}
                        className={`px-3 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'week' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                      >
                        Theo tuần
                      </button>
                      <button 
                        onClick={() => setChartPeriod('month')}
                        className={`px-3 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'month' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                      >
                        Theo tháng
                      </button>
                      <button 
                        onClick={() => setChartPeriod('semester')}
                        className={`px-3 py-1.5 text-sm font-semibold rounded-md transition-colors ${chartPeriod === 'semester' ? 'bg-white text-gray-800 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`}
                      >
                        Theo học kỳ
                      </button>
                    </div>
                  </div>
                </div>

                <div className="h-72 w-full relative" style={{ minHeight: '288px' }}>
                  {isLoadingChart && (
                    <div className="absolute inset-0 bg-white/70 backdrop-blur-[1px] flex justify-center items-center z-10 rounded-xl">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-red-600"></div>
                    </div>
                  )}
                  <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                      <defs>
                        <linearGradient id="colorAvg" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#94A3B8" stopOpacity={0.2}/>
                          <stop offset="95%" stopColor="#94A3B8" stopOpacity={0}/>
                        </linearGradient>
                        {SUBJECT_COLORS.map((color, idx) => (
                          <linearGradient key={idx} id={`colorSubj-${idx}`} x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor={color} stopOpacity={0.35}/>
                            <stop offset="95%" stopColor={color} stopOpacity={0}/>
                          </linearGradient>
                        ))}
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                      <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{fill: '#6B7280', fontSize: 12}} dy={10} />
                      <YAxis axisLine={false} tickLine={false} tick={{fill: '#6B7280', fontSize: 12}} domain={[0, 100]} />
                      <Tooltip 
                        contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 8px 30px rgb(0 0 0 / 0.12)', padding: '12px 16px' }}
                        cursor={{ fill: '#F3F4F6' }}
                        formatter={(value, name, props) => {
                          const payload = props.payload;
                          const sessionLabel = payload[`${name}_sessionLabel`];
                          if (sessionLabel) {
                            return [`${value}% (${sessionLabel})`, name];
                          }
                          return [`${value}%`, name];
                        }}
                      />
                      <Legend iconType="circle" wrapperStyle={{ paddingTop: '20px', fontSize: '12px', fontWeight: 600 }} />
                      
                      {/* VÙNG TRUNG BÌNH CHUNG ĐƯỜNG NỀN CHO CÁC PHIÊN CHU KỲ NGẮN */}
                      {chartPeriod === 'day' && (
                        <Area 
                          type="monotone" 
                          dataKey="Trung bình" 
                          name="Trung bình chung" 
                          stroke="#94A3B8" 
                          fill="url(#colorAvg)" 
                          strokeWidth={2}
                          strokeDasharray="4 4"
                        />
                      )}

                      {chartPeriod === 'week' && (
                        <Area 
                          type="monotone" 
                          dataKey="Trung bình" 
                          name="Trung bình chung" 
                          stroke="#475569" 
                          fill="url(#colorAvg)" 
                          strokeWidth={2}
                        />
                      )}
                      
                      {/* ĐƯỜNG BIỂU ĐỒ ĐỘNG THEO TỪNG MÔN HỌC & CHU KỲ */}
                      {chartKeys.map((key, index) => {
                        const color = SUBJECT_COLORS[index % SUBJECT_COLORS.length];
                        
                        if (chartPeriod === 'day') {
                          // Daily View: Dynamic lines with active dots
                          return (
                            <Line
                              key={key}
                              type="monotone"
                              dataKey={key}
                              name={key}
                              connectNulls
                              stroke={color}
                              strokeWidth={3}
                              dot={{ r: 4, strokeWidth: 2, fill: '#fff' }}
                              activeDot={{ r: 6 }}
                            />
                          );
                        } else if (chartPeriod === 'week') {
                          // Weekly View: Layered smooth glowing Area charts
                          return (
                            <Area
                              key={key}
                              type="monotone"
                              dataKey={key}
                              name={key}
                              stroke={color}
                              fill={`url(#colorSubj-${index % SUBJECT_COLORS.length})`}
                              strokeWidth={3}
                              dot={{ r: 4, strokeWidth: 2, fill: '#fff' }}
                              activeDot={{ r: 6 }}
                            />
                          );
                        } else {
                          // Monthly & Semester Views: Clean discrete Column (Bar) charts side-by-side
                          return (
                            <Bar
                              key={key}
                              dataKey={key}
                              name={key}
                              fill={color}
                              barSize={chartPeriod === 'semester' ? 40 : 20}
                              radius={[4, 4, 0, 0]}
                            />
                          );
                        }
                      })}

                      {/* ĐƯỜNG TRUNG BÌNH CHUNG ĐÈ LÊN TRÊN CHO CÁC PHIÊN CHU KỲ DÀI */}
                      {(chartPeriod === 'month' || chartPeriod === 'semester') && (
                        <Line
                          type="monotone"
                          dataKey="Trung bình"
                          name="Trung bình chung"
                          stroke="#1E293B"
                          strokeWidth={3}
                          strokeDasharray="6 4"
                          dot={{ r: 5, strokeWidth: 3, fill: '#1E293B' }}
                        />
                      )}
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
                    <div className="space-y-4 animate-fade-in-up">
                      {[...Array(3)].map((_, idx) => (
                        <div key={`sk-alert-${idx}`} className="flex gap-4 p-4 rounded-xl border border-gray-100 bg-gray-50/30">
                          <div className="w-10 h-10 rounded-full bg-gray-200 shimmer-loader shrink-0" />
                          <div className="flex-1 space-y-3">
                            <div className="flex justify-between items-center">
                              <div className="w-32 h-4 bg-gray-200 rounded shimmer-loader" />
                              <div className="w-12 h-3 bg-gray-200 rounded shimmer-loader" />
                            </div>
                            <div className="space-y-2">
                              <div className="w-full h-3.5 bg-gray-200 rounded shimmer-loader" />
                              <div className="w-3/4 h-3.5 bg-gray-200 rounded shimmer-loader" />
                            </div>
                            <div className="w-24 h-8 bg-gray-200 rounded-lg shimmer-loader" />
                          </div>
                        </div>
                      ))}
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
                              {alert.studentName && alert.studentName !== 'Không rõ' ? (
                                <><span className="font-bold text-gray-900">{alert.studentName}</span>: </>
                              ) : (
                                <span className="font-bold text-red-600">Cảnh báo: </span>
                              )}
                              {alert.description || 'Có hành vi điểm danh bất thường'} <br/>
                              Tại lớp: <span className="font-bold text-gray-900">
                                {alert.classInfo?.courseCode ? `${alert.classInfo.courseCode} – ` : ''}
                                {alert.classInfo?.classCode ? `${alert.classInfo.classCode} – ` : ''}
                                {alert.classInfo?.groupName || alert.classInfo?.name || 'Không rõ lớp'}
                              </span>.

                              {/* THÔNG TIN SINH VIÊN LIÊN QUAN (TÊN & MSV) */}
                              {(alert.studentName || alert.student?.name) && (
                                <p className="text-xs text-gray-500 mt-1.5 flex items-center gap-1.5 bg-red-50/40 border border-red-100/50 p-2 rounded-lg">
                                  <span>👤 Sinh viên liên quan:</span>
                                  <strong className="text-gray-900 font-bold">{alert.studentName || alert.student?.name}</strong>
                                  {(alert.studentCode || alert.student?.studentCode || alert.student?.id || alert.userId) && (
                                    <> - MSV: <span className="font-mono font-bold text-red-600 bg-white border border-red-100 px-1 py-0.5 rounded">{alert.studentCode || alert.student?.studentCode || alert.student?.id || alert.userId}</span></>
                                  )}
                                </p>
                              )}
                              
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
                              <span className="font-bold text-gray-900">{alert.studentName || 'Không rõ'}</span>
                              {(alert.studentCode || alert.student?.studentCode || alert.student?.id || alert.userId) && (
                                <> (MSV: <span className="font-mono text-red-600 font-bold">{alert.studentCode || alert.student?.studentCode || alert.student?.id || alert.userId}</span>)</>
                              )} đã gửi yêu cầu cho môn <span className="font-bold text-gray-900">
                                {alert.classInfo?.courseCode ? `${alert.classInfo.courseCode} – ` : ''}
                                {alert.classInfo?.classCode ? `${alert.classInfo.classCode} – ` : ''}
                                {alert.classInfo?.groupName || alert.classInfo?.name || 'không rõ'}
                              </span>. Lý do: {alert.reason}
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


