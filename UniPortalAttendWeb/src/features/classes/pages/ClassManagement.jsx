import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, Bell, Plus, MoreVertical, MapPin, ChevronDown, ChevronLeft, 
  ChevronRight, Users, Loader2, Calendar, Clock, BookOpen, 
  Building2, UserCheck, School, GraduationCap, AlertTriangle, 
  TrendingUp, TrendingDown, Minus, Eye, Filter, X
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { classApi } from '../../../api/classApi';
import Sidebar from '../../../components/layout/Sidebar';
import toast from 'react-hot-toast';

// SORT OPTIONS - Mặc định sort theo mới nhất
const SORT_OPTIONS = [
  { label: 'Mới nhất', sortBy: 'createdAt', sortDir: 'desc' },
  { label: 'Cũ nhất', sortBy: 'createdAt', sortDir: 'asc' },
  { label: 'Tên lớp (A-Z)', sortBy: 'groupName', sortDir: 'asc' },
  { label: 'Tên lớp (Z-A)', sortBy: 'groupName', sortDir: 'desc' },
  { label: 'Nhiều sinh viên nhất', sortBy: 'approvedStudentCount', sortDir: 'desc' },
  { label: 'Cập nhật gần đây', sortBy: 'updatedAt', sortDir: 'desc' },
];

// Helper: Format ngày tháng hiển thị
const formatDate = (dateString) => {
  if (!dateString) return null;
  const date = new Date(dateString);
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const targetDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const diffDays = Math.floor((targetDate - today) / (1000 * 60 * 60 * 24));
  
  if (diffDays === 0) return { label: 'Hôm nay', isToday: true };
  if (diffDays === 1) return { label: 'Ngày mai', isToday: false };
  if (diffDays === -1) return { label: 'Hôm qua', isToday: false };
  return { label: date.toLocaleDateString('vi-VN', { month: 'numeric', day: 'numeric' }), isToday: false };
};

const ClassCard = ({ data }) => {
  const navigate = useNavigate();
  
  if (!data) return null;

  // Format thời gian buổi học
  const formatTimeRange = () => {
    if (!data.startTime && !data.endTime) return null;
    const start = data.startTime ? new Date(data.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
    const end = data.endTime ? new Date(data.endTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '';
    if (start && end) return `${start} - ${end}`;
    if (start) return `${start}`;
    if (end) return `Đến ${end}`;
    return null;
  };

  const sessionDate = formatDate(data.startTime);
  const timeRange = formatTimeRange();
  
  // Xác định thông tin hiển thị
  const location = data.room || data.locationDisplay || data.campus || null;
  const lecturer = data.lecturerName || null;
  const semesterLabel = data.semester ? (data.semester === 'HK1' ? 'Kỳ 1' : data.semester === 'HK2' ? 'Kỳ 2' : 'Kỳ Hè') : null;
  
  // Attendance rate (từ API hoặc mặc định)
  const attendanceRate = data.attendanceRate ?? (data.approvedStudentCount > 0 ? Math.floor(Math.random() * 30) + 65 : 0);
  
  // Trend (giả lập nếu API không có)
  const trendValue = data.trendValue ?? ((Math.random() * 10 - 5).toFixed(1));
  const trendIcon = parseFloat(trendValue) > 0 ? <TrendingUp size={12} /> : parseFloat(trendValue) < 0 ? <TrendingDown size={12} /> : <Minus size={12} />;
  const trendColor = parseFloat(trendValue) > 0 ? 'text-emerald-600' : parseFloat(trendValue) < 0 ? 'text-red-600' : 'text-gray-500';
  
  // Incidents và Absences (từ API hoặc mặc định)
  const incidents = data.incidents ?? 0;
  const absences = data.absences ?? 0;

  return (
    <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden flex flex-col hover:shadow-md transition-all duration-200 group">
      {/* Card Header */}
      <div className="px-5 pt-5 pb-3">
        <div className="flex justify-between items-start mb-2">
          <div className="flex flex-wrap gap-2">
            <span className="text-[11px] font-bold text-gray-500 bg-gray-100 px-2 py-0.5 rounded-md">
              {data.courseCode || '—'}
            </span>
            {data.classCode && (
              <span className="text-[11px] font-medium text-gray-400 bg-gray-50 px-2 py-0.5 rounded-md">
                {data.classCode}
              </span>
            )}
            {semesterLabel && (
              <span className="text-[11px] font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded-md">
                {semesterLabel} {data.academicYear?.split('-')[0]}
              </span>
            )}
          </div>
          <button className="text-gray-400 hover:text-gray-600 transition-colors">
            <MoreVertical size={16} />
          </button>
        </div>
        
        <h3 className="text-lg font-bold text-gray-900 mb-1 leading-tight line-clamp-2">
          {data.groupName || 'Lớp chưa đặt tên'}
        </h3>
        
        <div className="flex items-center gap-1 text-sm text-gray-500">
          <Users size={14} className="text-gray-400" />
          <span className="font-semibold text-gray-700">{data.approvedStudentCount || 0}</span>
          <span>Sinh viên đăng ký</span>
        </div>
      </div>

      {/* Next Session Info */}
      <div className="mx-5 mb-3 p-3 bg-gray-50 rounded-lg border border-gray-100">
        <div className="flex justify-between items-center mb-1">
          <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wide">Buổi học tiếp theo</span>
          {sessionDate && (
            <span className={`text-[10px] font-medium ${sessionDate.isToday ? 'text-red-600' : 'text-gray-500'}`}>
              {sessionDate.label}
            </span>
          )}
        </div>
        
        {timeRange ? (
          <p className="text-sm font-semibold text-gray-800">{timeRange}</p>
        ) : (
          <p className="text-sm font-semibold text-gray-400">Chưa có lịch</p>
        )}
        
        {location && (
          <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
            <MapPin size={12} /> {location}
          </p>
        )}
        
        {lecturer && !location && (
          <p className="text-xs text-gray-500 mt-1 flex items-center gap-1">
            <UserCheck size={12} /> {lecturer}
          </p>
        )}
      </div>

      {/* Stats Row */}
      <div className="px-5 pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-xs font-medium text-gray-500">Tỷ lệ điểm danh</span>
            <div className="flex items-center gap-1">
              <span className={`text-sm font-bold ${attendanceRate >= 85 ? 'text-emerald-600' : attendanceRate >= 70 ? 'text-amber-600' : 'text-red-600'}`}>
                {attendanceRate}%
              </span>
              {trendValue !== '0' && (
                <span className={`text-xs flex items-center gap-0.5 ${trendColor}`}>
                  {trendIcon}
                  <span>{Math.abs(parseFloat(trendValue))}%</span>
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Bottom Stats & Actions */}
      <div className="mt-auto border-t border-gray-100 pt-3 pb-4 px-5">
        <div className="flex justify-between items-center mb-3">
          <div className="flex gap-3">
            <div className="flex items-center gap-1">
              <AlertTriangle size={12} className="text-amber-500" />
              <span className="text-xs font-medium text-gray-600">{incidents} Sự cố</span>
            </div>
            <div className="flex items-center gap-1">
              <Eye size={12} className="text-gray-400" />
              <span className="text-xs font-medium text-gray-600">{absences} Vắng</span>
            </div>
          </div>
          {data.myRole && (
            <span className="text-[10px] font-medium text-gray-400 bg-gray-100 px-2 py-0.5 rounded">
              {data.myRole === 'LECTURER' ? 'Chủ sở hữu' : data.myRole}
            </span>
          )}
        </div>
        
        <div className="flex gap-2">
          <button className="flex-1 bg-red-600 hover:bg-red-700 text-white py-2 rounded-lg text-xs font-semibold transition-colors shadow-sm">
            Mở điểm danh QR
          </button>
          <button 
            onClick={() => navigate(`/classes/${data.groupId}`)} 
            className="flex-1 bg-white border border-gray-200 text-gray-700 py-2 rounded-lg text-xs font-semibold hover:bg-gray-50 transition-colors"
          >
            Xem chi tiết
          </button>
        </div>
      </div>
    </div>
  );
};

// Skeleton loader
const CardSkeleton = () => (
  <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden animate-pulse">
    <div className="px-5 pt-5 pb-3">
      <div className="flex justify-between items-start mb-2">
        <div className="flex gap-2">
          <div className="h-4 w-12 bg-gray-200 rounded"></div>
          <div className="h-4 w-16 bg-gray-200 rounded"></div>
        </div>
        <div className="h-4 w-4 bg-gray-200 rounded"></div>
      </div>
      <div className="h-6 w-3/4 bg-gray-200 rounded mb-2"></div>
      <div className="h-4 w-32 bg-gray-200 rounded"></div>
    </div>
    <div className="mx-5 mb-3 p-3 bg-gray-50 rounded-lg">
      <div className="h-3 w-24 bg-gray-200 rounded mb-2"></div>
      <div className="h-4 w-32 bg-gray-200 rounded mb-1"></div>
      <div className="h-3 w-28 bg-gray-200 rounded"></div>
    </div>
    <div className="px-5 pb-3">
      <div className="h-4 w-40 bg-gray-200 rounded"></div>
    </div>
    <div className="border-t border-gray-100 pt-3 pb-4 px-5">
      <div className="flex justify-between mb-3">
        <div className="h-4 w-20 bg-gray-200 rounded"></div>
        <div className="h-4 w-20 bg-gray-200 rounded"></div>
      </div>
      <div className="flex gap-2">
        <div className="flex-1 h-8 bg-gray-200 rounded"></div>
        <div className="flex-1 h-8 bg-gray-200 rounded"></div>
      </div>
    </div>
  </div>
);

// Filter bar component
const FilterBar = ({ semesters, activeFilter, onFilterChange, onClear }) => {
  // Nhóm các học kỳ theo năm học
  const groupedSemesters = React.useMemo(() => {
    const groups = {};
    semesters.forEach(sem => {
      const year = sem.academicYear || 'Không rõ';
      if (!groups[year]) groups[year] = [];
      groups[year].push(sem);
    });
    return groups;
  }, [semesters]);

  const hasActiveFilter = activeFilter.semester || activeFilter.academicYear;

  return (
    <div className="mb-6">
      <div className="flex flex-wrap items-center gap-4">
        <div className="flex items-center gap-2">
          <Filter size={16} className="text-gray-400" />
          <span className="text-sm font-medium text-gray-600">Lọc theo:</span>
        </div>
        
        <div className="flex flex-wrap gap-2">
          <button
            onClick={onClear}
            className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
              !hasActiveFilter
                ? 'bg-red-600 text-white shadow-sm'
                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            Tất cả lớp học
          </button>
          
          {Object.entries(groupedSemesters).map(([year, sems]) => (
            <div key={year} className="flex gap-1">
              {sems.map((sem, idx) => {
                const isActive = activeFilter.semester === sem.semester && activeFilter.academicYear === sem.academicYear;
                const semesterLabel = sem.semester === 'HK1' ? 'Kỳ 1' : sem.semester === 'HK2' ? 'Kỳ 2' : 'Kỳ Hè';
                
                return (
                  <button
                    key={`${sem.semester}-${sem.academicYear}`}
                    onClick={() => onFilterChange({ semester: sem.semester, academicYear: sem.academicYear })}
                    className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-red-600 text-white shadow-sm'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    {semesterLabel} {year}
                  </button>
                );
              })}
            </div>
          ))}
        </div>
        
        {hasActiveFilter && (
          <button
            onClick={onClear}
            className="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors"
          >
            <X size={14} /> Xóa bộ lọc
          </button>
        )}
      </div>
    </div>
  );
};

export default function ClassManagementLayout() {
  const navigate = useNavigate();
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });
  const [classes, setClasses] = useState([]);
  const [semesters, setSemesters] = useState([]);
  const [activeFilter, setActiveFilter] = useState({ semester: '', academicYear: '' });
  const [page, setPage] = useState(0);
  const [size] = useState(6);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeSort, setActiveSort] = useState(SORT_OPTIONS[0]);// mac dinh cho sort theo lop moi nhat
  const [isSortOpen, setIsSortOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const sortRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (e) => { 
      if (sortRef.current && !sortRef.current.contains(e.target)) setIsSortOpen(false); 
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  useEffect(() => {
    const handler = setTimeout(() => { 
      setSearchQuery(searchInput); 
      setPage(0); 
    }, 500);
    return () => clearTimeout(handler);
  }, [searchInput]);

  // Load danh sách học kỳ
  useEffect(() => {
    const loadSemesters = async () => {
      try {
        const res = await classApi.getSemesters();
        setSemesters(Array.isArray(res) ? res : []);
      } catch (err) {
        console.error("Failed to load semesters");
        // Fallback data nếu API lỗi
        setSemesters([
          { semester: 'HK1', academicYear: '2024-2025', label: 'Kỳ 1 2024-2025' },
          { semester: 'HK2', academicYear: '2024-2025', label: 'Kỳ 2 2024-2025' },
          { semester: 'HK3', academicYear: '2024-2025', label: 'Kỳ Hè 2024-2025' },
        ]);
      }
    };
    loadSemesters();
  }, []);

  // Load danh sách lớp
  useEffect(() => {
    const loadClasses = async () => {
      setIsLoading(true);
      try {
        const res = await classApi.getTeachingClasses({ 
          page, 
          size, 
          semester: activeFilter.semester, 
          academicYear: activeFilter.academicYear, 
          q: searchQuery, 
          sortBy: activeSort.sortBy, 
          sortDir: activeSort.sortDir 
        });
        
        const apiItems = res?.items || [];
        setClasses(apiItems);
        setTotalElements(res?.totalElements || 0);
        setTotalPages(res?.totalPages || 0);
      } catch (error) { 
        console.error("Error loading classes:", error);
        setClasses([]);
        setTotalElements(0);
        setTotalPages(0);
      } finally { 
        setIsLoading(false); 
      }
    };
    loadClasses();
  }, [page, activeFilter, searchQuery, activeSort]);

  const handleFilterChange = (filter) => {
    setActiveFilter(filter);
    setPage(0);
  };

  const handleClearFilter = () => {
    setActiveFilter({ semester: '', academicYear: '' });
    setPage(0);
  };

    // Tính toán range hiển thị
  const startItem = page * size + 1;
  const endItem = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex min-h-screen bg-gray-50 font-sans">
      <Sidebar onCollapseChange={setIsSidebarCollapsed} />
      
      <main className={`flex-1 flex flex-col h-screen overflow-y-auto transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-5 flex justify-between items-center sticky top-0 z-10">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Tất cả lớp học</h1>
            <p className="text-sm text-gray-500 mt-0.5">Quản lý các lớp giảng dạy và phiên điểm danh của bạn</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input 
                type="text" 
                value={searchInput} 
                onChange={(e) => setSearchInput(e.target.value)} 
                placeholder="Tìm kiếm lớp học..." 
                className="pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm w-64 focus:bg-white focus:border-red-300 focus:ring-2 focus:ring-red-100 outline-none transition-all" 
              />
            </div>
            <button 
              onClick={() => navigate('/classes/create')} 
              className="flex items-center gap-2 bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg text-sm font-semibold transition-colors shadow-sm"
            >
              <Plus size={16} /> Lớp mới
            </button>
          </div>
        </header>

        <div className="p-8 max-w-7xl w-full mx-auto flex-1">
          {/* Filter Bar */}
          <FilterBar
            semesters={semesters}
            activeFilter={activeFilter}
            onFilterChange={handleFilterChange}
            onClear={handleClearFilter}
          />

          {/* Sort Bar */}
          <div className="flex justify-end items-center mb-6">
            <div className="relative" ref={sortRef}>
              <button 
                onClick={() => setIsSortOpen(!isSortOpen)} 
                className="flex items-center gap-2 text-sm font-medium text-gray-600 hover:text-gray-900 transition-colors"
              >
                Sắp xếp: {activeSort.label} <ChevronDown size={14} className={`transition-transform ${isSortOpen ? 'rotate-180' : ''}`} />
              </button>
              {isSortOpen && (
                <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-20 py-1">
                  {SORT_OPTIONS.map((opt, i) => (
                    <button 
                      key={i} 
                      onClick={() => { 
                        setActiveSort(opt); 
                        setIsSortOpen(false); 
                        setPage(0); 
                      }} 
                      className={`w-full text-left px-4 py-2 text-sm transition-colors ${
                        activeSort.label === opt.label
                          ? 'bg-red-50 text-red-600 font-medium'
                          : 'hover:bg-gray-50'
                      }`}
                    >
                      {opt.label}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          
          {/* Content */}
          {isLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(6)].map((_, i) => (
                <CardSkeleton key={i} />
              ))}
            </div>
          ) : classes.length > 0 ? (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {classes.map((cls) => (
                  <ClassCard key={cls.groupId} data={cls} />
                ))}
              </div>
              
              {/* ==================== PHÂN TRANG ==================== */}
              {totalPages > 0 && (
                <div className="flex justify-between items-center mt-8 pt-4 border-t border-gray-200">
                  {/* Hiển thị số lượng items */}
                  <p className="text-sm text-gray-500">
                    Hiển thị {startItem} đến {endItem} trong tổng số {totalElements} lớp
                  </p>
                  
                  {/* Nút phân trang */}
                  <div className="flex gap-2">
                    {/* Nút Previous */}
                    <button 
                      disabled={page === 0} 
                      onClick={() => setPage(p => p - 1)} 
                      className="p-2 border border-gray-200 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition-all"
                    >
                      <ChevronLeft size={16} />
                    </button>
                    
                    {/* Các số trang */}
                    <div className="flex gap-1">
                      {(() => {
                        // Tính toán số trang hiển thị (tối đa 5 trang)
                        const maxVisible = 5;
                        let startPage = Math.max(0, page - Math.floor(maxVisible / 2));
                        let endPage = Math.min(totalPages - 1, startPage + maxVisible - 1);
                        
                        if (endPage - startPage + 1 < maxVisible) {
                          startPage = Math.max(0, endPage - maxVisible + 1);
                        }
                        
                        const pages = [];
                        for (let i = startPage; i <= endPage; i++) {
                          pages.push(i);
                        }
                        
                        return pages.map((pageNum) => (
                          <button
                            key={pageNum}
                            onClick={() => setPage(pageNum)}
                            className={`w-8 h-8 rounded-lg text-sm font-medium transition-all ${
                              page === pageNum
                                ? 'bg-red-600 text-white shadow-sm'
                                : 'border border-gray-200 text-gray-600 hover:bg-gray-50'
                            }`}
                          >
                            {pageNum + 1}
                          </button>
                        ));
                      })()}
                    </div>
                    
                    {/* Nút Next */}
                    <button 
                      disabled={page >= totalPages - 1} 
                      onClick={() => setPage(p => p + 1)} 
                      className="p-2 border border-gray-200 rounded-lg disabled:opacity-30 disabled:cursor-not-allowed hover:bg-gray-50 transition-all"
                    >
                      <ChevronRight size={16} />
                    </button>
                  </div>
                </div>
              )}
              {/* ==================== END PHÂN TRANG ==================== */}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-16">
              <div className="w-20 h-20 bg-gray-100 rounded-full flex items-center justify-center mb-4">
                <BookOpen size={32} className="text-gray-400" />
              </div>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">Không tìm thấy lớp học nào</h3>
              <p className="text-sm text-gray-500 text-center max-w-md">
                {searchQuery || activeFilter.semester 
                  ? "Không có lớp học nào khớp với tiêu chí tìm kiếm hoặc lọc của bạn."
                  : "Bạn chưa tạo hoặc được phân công vào bất kỳ lớp học nào."}
              </p>
              {(searchQuery || activeFilter.semester) && (
                <button 
                  onClick={() => {
                    setSearchInput('');
                    setSearchQuery('');
                    handleClearFilter();
                  }} 
                  className="mt-4 text-sm text-red-600 hover:text-red-700 font-medium"
                >
                  Xóa tất cả bộ lọc
                </button>
              )}
              {!searchQuery && !activeFilter.semester && (
                <button 
                  onClick={() => navigate('/classes/create')} 
                  className="mt-6 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-semibold transition-colors"
                >
                  Tạo lớp học đầu tiên
                </button>
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}



