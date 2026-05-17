import React, { useState, useEffect, useCallback } from 'react';
import { 
  Search, AlertTriangle, Flag, CheckCircle2, 
  Smartphone, MapPin, MoreVertical, Check, Timer,
  Loader2, RefreshCcw, Info, X, ShieldAlert,
  Server, Calendar, FileText
} from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// Cấu hình hiển thị theo loại sự cố
const TYPE_CONFIG = {
  REPEATED_FAILED_QR_TOKEN: { label: 'Quét QR thất bại liên tục', icon: <Timer size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
  WRONG_SESSION_QR_TOKEN: { label: 'QR sai phiên học', icon: <Smartphone size={20} className="text-amber-500" />, bgColor: 'bg-amber-50', borderColor: 'border-amber-100' },
  EXPIRED_QR_TOKEN: { label: 'QR hết hạn', icon: <Timer size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
  REPEATED_OUT_OF_RANGE: { label: 'Quét ngoài phạm vi', icon: <MapPin size={20} className="text-amber-500" />, bgColor: 'bg-amber-50', borderColor: 'border-amber-100' },
  IP_BURST_MULTI_ATTEMPT: { label: 'Nhiều yêu cầu từ 1 IP', icon: <Smartphone size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
  SHARED_DEVICE_MULTI_ACCOUNT: { label: 'Dùng chung thiết bị', icon: <Smartphone size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
  // Loại sự cố cũ cho Mock Data
  device: { label: 'Trùng thiết bị', icon: <Smartphone size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
  location: { label: 'Vị trí bất thường', icon: <MapPin size={20} className="text-amber-500" />, bgColor: 'bg-amber-50', borderColor: 'border-amber-100' },
  time: { label: 'Quét quá nhanh', icon: <Timer size={20} className="text-red-500" />, bgColor: 'bg-red-50', borderColor: 'border-red-100' },
};

const SEVERITY_COLORS = {
  LOW: { label: 'Thấp', bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', dot: 'bg-gray-500' },
  MEDIUM: { label: 'Trung bình', bg: 'bg-amber-50', border: 'border-amber-200', text: 'text-amber-700', dot: 'bg-amber-500' },
  HIGH: { label: 'Cao', bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700', dot: 'bg-red-500' },
  CRITICAL: { label: 'Nghiêm trọng', bg: 'bg-red-100', border: 'border-red-300', text: 'text-red-800', dot: 'bg-red-600' },
  // Mức độ cũ cho Mock Data
  'Low Risk': { label: 'Rủi ro thấp', bg: 'bg-gray-50', border: 'border-gray-200', text: 'text-gray-700', dot: 'bg-gray-500' },
  'Medium Risk': { label: 'Rủi ro vừa', bg: 'bg-amber-50', border: 'border-amber-200', text: 'text-amber-700', dot: 'bg-amber-500' },
  'High Risk': { label: 'Rủi ro cao', bg: 'bg-red-50', border: 'border-red-200', text: 'text-red-700', dot: 'bg-red-500' },
};

const MOCK_INCIDENTS = [
  {
    id: 'MOCK-001',
    type: 'device',
    title: 'Trùng ID thiết bị',
    description: 'Thiết bị ID A8F2 được dùng để điểm danh cho 2 sinh viên khác nhau',
    student: {
      name: 'Nguyễn Văn Mạnh',
      id: 'STU-6520',
      avatar: 'https://i.pravatar.cc/150?u=mich'
    },
    timestamp: '10:08 SA',
    date: '24 Th10, 2023',
    severity: 'High Risk',
    confidence: 98,
    status: 'pending'
  },
  {
    id: 'MOCK-002',
    type: 'location',
    title: 'Vị trí không hợp lệ',
    description: 'Tọa độ quét cách phòng học 2.4km',
    student: {
      name: 'Lê Thị Hồng',
      id: 'STU-4421',
      avatar: 'https://i.pravatar.cc/150?u=sarah'
    },
    timestamp: '10:12 SA',
    date: '24 Th10, 2023',
    severity: 'Medium Risk',
    confidence: 75,
    status: 'pending'
  },
  {
    id: 'MOCK-003',
    type: 'time',
    title: 'Quét QR quá nhanh',
    description: 'Phát hiện 3 lần quét trong 4 giây từ cùng một IP',
    student: {
      name: 'Trần Đăng Khoa',
      id: 'STU-1198',
      avatar: 'https://i.pravatar.cc/150?u=david'
    },
    timestamp: '10:08 SA',
    date: '24 Th10, 2023',
    severity: 'High Risk',
    confidence: 92,
    status: 'pending'
  }
];

export default function FraudTab({ classId }) {
  const [incidents, setIncidents] = useState(MOCK_INCIDENTS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedIncidents, setSelectedIncidents] = useState([]);
  const [stats, setStats] = useState({ total: 0, highRisk: 0, resolved: 0 });
  
  // State quản lý chi tiết sự cố
  const [selectedDetail, setSelectedDetail] = useState(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);

  // Lấy danh sách sự cố từ API
  const fetchIncidents = useCallback(async () => {
    if (!classId) return;
    
    setLoading(true);
    try {
      const data = await classApi.getFraudIncidents(classId, {
        size: 50,
        sortDir: 'DESC'
      });
      
      const realItems = data.items || [];
      // Trộn dữ liệu thật với dữ liệu giả để kiểm thử
      const combinedItems = [...realItems, ...MOCK_INCIDENTS];
      
      setIncidents(combinedItems);
      
      // Tính toán thống kê dựa trên dữ liệu tổng hợp
      const total = combinedItems.length;
      const highRisk = combinedItems.filter(i => 
        i.severity === 'HIGH' || i.severity === 'CRITICAL' || i.severity === 'High Risk'
      ).length;
      const resolved = combinedItems.filter(i => 
        i.status === 'RESOLVED' || i.status === 'resolved'
      ).length;
      
      setStats({ total, highRisk, resolved });
      setError(null);
    } catch (err) {
      console.error("Lỗi khi tải sự cố gian lận:", err);
      // Nếu lỗi API vẫn giữ lại dữ liệu giả để kiểm thử giao diện
      setIncidents(MOCK_INCIDENTS);
      setError("Lỗi kết nối API: Đang hiển thị dữ liệu mẫu để thử nghiệm");
      toast.error("Không thể tải dữ liệu thực, đang hiển thị dữ liệu mẫu");
    } finally {
      setLoading(false);
    }
  }, [classId]);

  useEffect(() => {
    fetchIncidents();
  }, [fetchIncidents]);

  // Xử lý giải quyết từng sự cố
  const handleResolve = async (incidentId) => {
    // Nếu là dữ liệu giả thì chỉ cập nhật trạng thái cục bộ
    if (incidentId.startsWith('MOCK-')) {
      setIncidents(prev => prev.map(inc => 
        inc.id === incidentId ? { ...inc, status: 'resolved' } : inc
      ));
      toast.success("Đã xác nhận xử lý (Dữ liệu mẫu)");
      return;
    }

    try {
      await classApi.updateFraudIncident(classId, incidentId, {
        action: 'RESOLVE',
        note: 'Đã xử lý từ Dashboard giảng viên'
      });
      toast.success("Đã xử lý sự cố thành công");
      fetchIncidents(); // Làm mới dữ liệu
    } catch (err) {
      toast.error("Không thể cập nhật trạng thái sự cố");
    }
  };

  // Xem chi tiết sự cố
  const handleViewDetail = async (incident) => {
    setIsDetailLoading(true);
    setSelectedDetail(incident); // Set tạm thời bằng data row
    
    // Nếu là mock data thì dừng lại, dùng data của row luôn
    if (String(incident.id).startsWith('MOCK-')) {
      setIsDetailLoading(false);
      return;
    }

    try {
      const res = await classApi.getFraudIncidentDetail(classId, incident.id);
      if (res) {
        setSelectedDetail(res); // Nạp lại detail đầy đủ từ API
      }
    } catch (error) {
      console.error("Lỗi lấy chi tiết sự cố:", error);
      toast.error("Không thể tải chi tiết, hiển thị dữ liệu rút gọn.");
    } finally {
      setIsDetailLoading(false);
    }
  };

  // Hiển thị icon theo loại sự cố
  const renderIncidentIcon = (type) => {
    const config = TYPE_CONFIG[type] || { icon: <Flag size={20} className="text-gray-500" />, bgColor: 'bg-gray-50', borderColor: 'border-gray-200' };
    return (
      <div className={`w-10 h-10 rounded-lg flex items-center justify-center shrink-0 border ${config.bgColor} ${config.borderColor}`}>
        {config.icon}
      </div>
    );
  };

  // Hiển thị nhãn mức độ nghiêm trọng
  const renderSeverityBadge = (severity, confidence) => {
    const style = SEVERITY_COLORS[severity] || SEVERITY_COLORS.LOW;
    
    return (
      <div>
        <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded border ${style.bg} ${style.border} ${style.text}`}>
          <div className={`w-1.5 h-1.5 rounded-full ${style.dot}`}></div>
          <span className="text-xs font-bold uppercase">{style.label || severity}</span>
        </div>
        {confidence !== undefined && (
          <p className="text-[10px] text-gray-500 font-medium mt-1 pl-1">Độ tin cậy {confidence}%</p>
        )}
      </div>
    );
  };

  const formatDate = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  };

  const formatDay = (dateString) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  // Xử lý chọn checkbox
  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedIncidents(incidents.map(inc => inc.id));
    } else {
      setSelectedIncidents([]);
    }
  };

  const handleSelectOne = (e, id) => {
    if (e.target.checked) {
      setSelectedIncidents(prev => [...prev, id]);
    } else {
      setSelectedIncidents(prev => prev.filter(item => item !== id));
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      
      {/* 1. THẺ TỔNG HỢP */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-gray-100 flex items-center justify-center">
            <Flag size={24} className="text-gray-400" />
          </div>
          <div>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Tổng cảnh báo</p>
            <h3 className="text-2xl font-black text-gray-900">{stats.total}</h3>
          </div>
        </div>

        {/* Thẻ rủi ro cao */}
        <div className="bg-red-50/50 rounded-xl border border-red-200 shadow-sm p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-red-100 flex items-center justify-center">
            <AlertTriangle size={24} className="text-red-500" />
          </div>
          <div>
            <p className="text-xs font-bold text-red-600 uppercase tracking-wider mb-1">Rủi ro cao</p>
            <h3 className="text-2xl font-black text-red-700">{stats.highRisk}</h3>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5 flex items-center gap-4">
          <div className="w-12 h-12 rounded-full bg-emerald-50 flex items-center justify-center">
            <CheckCircle2 size={24} className="text-emerald-500" />
          </div>
          <div>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Đã xử lý</p>
            <h3 className="text-2xl font-black text-gray-900">{stats.resolved}</h3>
          </div>
        </div>
      </div>

      {/* 2. BẢNG DANH SÁCH CHÍNH */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden min-h-[400px] flex flex-col">
        
        {/* Thanh công cụ */}
        <div className="p-4 border-b border-gray-200 flex flex-wrap justify-between items-center gap-4 bg-white">
          <div className="flex flex-wrap items-center gap-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
              <input 
                type="text" 
                placeholder="Tìm theo MSSV hoặc tên..." 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 pr-4 py-2 border border-gray-200 rounded-lg text-sm w-64 focus:ring-2 focus:ring-red-100 focus:border-red-300 outline-none transition-all"
              />
            </div>
            <button 
              onClick={fetchIncidents}
              className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
              title="Làm mới dữ liệu"
            >
              <RefreshCcw size={18} className={loading ? 'animate-spin' : ''} />
            </button>
          </div>

          <div className="flex items-center gap-3">
            <button 
              disabled={selectedIncidents.length === 0 || loading}
              className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <Check size={16} /> Xử lý hàng loạt
            </button>
          </div>
        </div>

        {/* Nội dung bảng */}
        <div className="overflow-x-auto flex-1">
          {loading && incidents.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center gap-3 text-gray-400">
              <Loader2 size={32} className="animate-spin text-red-500" />
              <p className="text-sm font-medium">Đang quét các trường hợp nghi vấn...</p>
            </div>
          ) : error && incidents.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center gap-3 text-gray-400">
              <AlertTriangle size={32} className="text-amber-500" />
              <p className="text-sm font-medium">{error}</p>
              <button onClick={fetchIncidents} className="text-red-600 text-sm font-bold hover:underline">Thử lại</button>
            </div>
          ) : incidents.length === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center gap-3 text-gray-400">
              <CheckCircle2 size={32} className="text-emerald-500" />
              <p className="text-sm font-medium">Không phát hiện dấu hiệu gian lận nào trong lớp này.</p>
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-white text-gray-500 font-bold border-b border-gray-200 text-[11px] uppercase tracking-wider">
                <tr>
                  <th className="p-4 w-12 text-center">
                    <input 
                      type="checkbox" 
                      onChange={handleSelectAll}
                      checked={selectedIncidents.length === incidents.length && incidents.length > 0}
                      className="rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer w-4 h-4" 
                    />
                  </th>
                  <th className="p-4">Loại sự cố & Bằng chứng</th>
                  <th className="p-4">Sinh viên</th>
                  <th className="p-4">Thời gian</th>
                  <th className="p-4">Mức độ</th>
                  <th className="p-4 text-right pr-6">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {incidents
                  .filter(inc => {
                    const searchStr = searchQuery.toLowerCase();
                    const userId = inc.userId || inc.student?.id || '';
                    const name = inc.student?.name || '';
                    return userId.toLowerCase().includes(searchStr) || name.toLowerCase().includes(searchStr);
                  })
                  .map((incident) => (
                  <tr key={incident.id} className={`hover:bg-gray-50 transition-colors group ${incident.status === 'RESOLVED' || incident.status === 'resolved' ? 'opacity-60 bg-gray-50/30' : ''}`}>
                    <td className="p-4 text-center">
                      <input 
                        type="checkbox" 
                        onChange={(e) => handleSelectOne(e, incident.id)}
                        checked={selectedIncidents.includes(incident.id)}
                        className="rounded border-gray-300 text-red-600 focus:ring-red-500 cursor-pointer w-4 h-4" 
                      />
                    </td>
                    
                    {/* Loại sự cố & Bằng chứng */}
                    <td className="p-4 flex items-start gap-3">
                      {renderIncidentIcon(incident.type)}
                      <div>
                        <h4 className="font-bold text-gray-900 leading-tight mb-1">
                          {incident.title || TYPE_CONFIG[incident.type]?.label || incident.type}
                        </h4>
                        <p className="text-xs text-gray-500 max-w-xs truncate" title={incident.description || incident.evidenceSummary?.notes?.join(', ')}>
                          {incident.description || (incident.occurrenceCount ? `${incident.occurrenceCount} lần lặp • ${incident.evidenceSummary?.lastFailureCode || 'Phát hiện logic'}` : 'Phát hiện theo logic hệ thống')}
                        </p>
                      </div>
                    </td>

                    {/* Sinh viên */}
                    <td className="p-4">
                      <div className="flex items-center gap-3">
                        {incident.student?.avatar ? (
                          <img src={incident.student.avatar} alt="avatar" className="w-8 h-8 rounded-full border border-gray-200 bg-gray-100 object-cover" />
                        ) : (
                          <div className="w-8 h-8 rounded-full border border-gray-200 bg-gray-100 flex items-center justify-center text-[10px] font-bold text-gray-400">
                            SV
                          </div>
                        )}
                        <div>
                          <p className="font-bold text-gray-900 text-[13px]">{incident.student?.name || incident.userId?.split('-')[0].toUpperCase() || 'KHÔNG RÕ'}</p>
                          <p className="text-xs text-gray-500 truncate w-24" title={incident.student?.id || incident.userId}>{incident.student?.id || incident.userId}</p>
                        </div>
                      </div>
                    </td>

                    {/* Thời gian */}
                    <td className="p-4">
                      <p className="font-bold text-gray-900 text-[13px]">{incident.timestamp || formatDate(incident.lastDetectedAt)}</p>
                      <p className="text-xs text-gray-500">{incident.date || formatDay(incident.lastDetectedAt)}</p>
                    </td>

                    {/* Mức độ */}
                    <td className="p-4 align-middle">
                      {renderSeverityBadge(incident.severity, incident.confidence)}
                      {(incident.status === 'RESOLVED' || incident.status === 'resolved') && (
                        <div className="mt-1 flex items-center gap-1 text-[10px] text-emerald-600 font-bold">
                          <CheckCircle2 size={10} /> Đã xử lý
                        </div>
                      )}
                    </td>

                    {/* Thao tác */}
                    <td className="p-4 text-right pr-6 align-middle">
                       <div className="flex justify-end gap-2">
                         {!(incident.status === 'RESOLVED' || incident.status === 'resolved') && (
                           <button 
                             onClick={() => handleResolve(incident.id)}
                             className="p-1.5 text-emerald-600 hover:bg-emerald-50 rounded-md transition-colors"
                             title="Đánh dấu là đã xử lý"
                           >
                             <Check size={18} />
                           </button>
                         )}
                         <button 
                           onClick={() => handleViewDetail(incident)}
                           className="p-1.5 text-gray-400 hover:text-gray-900 hover:bg-gray-100 rounded-md transition-colors" 
                           title="Xem chi tiết"
                         >
                           <Info size={18} />
                         </button>
                       </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        
      </div>

      {/* MODAL CHI TIẾT SỰ CỐ GIAN LẬN */}
      {selectedDetail && (
        <div className="fixed inset-0 z-[200] flex items-center justify-center bg-gray-900/50 backdrop-blur-sm p-4 animate-in fade-in duration-200">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-xl overflow-hidden animate-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            
            {/* Header Modal */}
            <div className={`p-6 border-b border-gray-100 flex items-start justify-between ${TYPE_CONFIG[selectedDetail.type]?.bgColor || 'bg-gray-50'}`}>
              <div className="flex gap-4 items-start">
                {renderIncidentIcon(selectedDetail.type)}
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <h2 className="text-lg font-bold text-gray-900">
                      {selectedDetail.title || TYPE_CONFIG[selectedDetail.type]?.label || selectedDetail.type}
                    </h2>
                    {selectedDetail.status === 'RESOLVED' || selectedDetail.status === 'resolved' ? (
                      <span className="text-[10px] font-bold text-emerald-600 bg-emerald-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <Check size={12} /> Đã xử lý
                      </span>
                    ) : (
                      <span className="text-[10px] font-bold text-amber-600 bg-amber-100 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <Loader2 size={12} /> Đang chờ
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-600">{selectedDetail.description || 'Chi tiết bằng chứng vi phạm của hệ thống.'}</p>
                </div>
              </div>
              <button onClick={() => setSelectedDetail(null)} className="p-2 hover:bg-gray-200/50 rounded-full transition-colors text-gray-500">
                <X size={20} />
              </button>
            </div>

            {/* Body Modal */}
            <div className="p-6 overflow-y-auto flex-1 space-y-6">
              
              {/* Thông tin đối tượng vi phạm */}
              <div>
                <h4 className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-3">Đối tượng vi phạm</h4>
                <div className="bg-gray-50 rounded-xl p-4 border border-gray-100 flex items-center gap-4">
                  {selectedDetail.student?.avatar ? (
                    <img src={selectedDetail.student.avatar} alt="avatar" className="w-12 h-12 rounded-full border border-gray-200 bg-gray-100 object-cover" />
                  ) : (
                    <div className="w-12 h-12 rounded-full border border-gray-200 bg-white flex items-center justify-center text-gray-400">
                      <UserX size={24} />
                    </div>
                  )}
                  <div>
                    <p className="font-bold text-gray-900">{selectedDetail.student?.name || selectedDetail.userId?.split('-')[0].toUpperCase() || 'Không rõ'}</p>
                    <p className="text-sm text-gray-500 font-mono mt-0.5">{selectedDetail.student?.id || selectedDetail.userId}</p>
                  </div>
                </div>
              </div>

              {/* Dữ liệu kỹ thuật & Bằng chứng */}
              <div>
                <h4 className="text-[11px] font-bold text-gray-400 uppercase tracking-wider mb-3">Bằng chứng hệ thống</h4>
                <div className="bg-white border border-gray-200 rounded-xl divide-y divide-gray-100 overflow-hidden">
                  
                  <div className="flex px-4 py-3 gap-3">
                    <Calendar size={16} className="text-gray-400 shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <p className="text-xs text-gray-500 font-medium mb-1">Thời gian phát hiện</p>
                      <p className="text-sm font-semibold text-gray-900">
                        {selectedDetail.timestamp || formatDate(selectedDetail.lastDetectedAt)} - {selectedDetail.date || formatDay(selectedDetail.lastDetectedAt)}
                      </p>
                    </div>
                  </div>

                  <div className="flex px-4 py-3 gap-3 bg-red-50/30">
                    <ShieldAlert size={16} className="text-red-400 shrink-0 mt-0.5" />
                    <div className="flex-1">
                      <p className="text-xs text-gray-500 font-medium mb-1">Mức độ rủi ro (AI Đánh giá)</p>
                      <div className="mt-1">
                        {renderSeverityBadge(selectedDetail.severity, selectedDetail.confidence)}
                      </div>
                    </div>
                  </div>

                  {selectedDetail.evidenceSummary && Object.entries(selectedDetail.evidenceSummary).map(([key, value]) => {
                    if (key === 'notes') return null; // Bỏ qua mảng notes vì hiển thị ở mô tả rồi
                    return (
                      <div key={key} className="flex px-4 py-3 gap-3">
                        <FileText size={16} className="text-gray-400 shrink-0 mt-0.5" />
                        <div className="flex-1">
                          <p className="text-xs text-gray-500 font-medium mb-1 capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</p>
                          <p className="text-sm font-semibold text-gray-900 break-all">{String(value)}</p>
                        </div>
                      </div>
                    );
                  })}
                  
                  {/* Nếu không có evidenceSummary nhưng là Mock data */}
                  {!selectedDetail.evidenceSummary && String(selectedDetail.id).startsWith('MOCK-') && (
                     <div className="flex px-4 py-3 gap-3">
                       <Server size={16} className="text-gray-400 shrink-0 mt-0.5" />
                       <div className="flex-1">
                         <p className="text-xs text-gray-500 font-medium mb-1">Dữ liệu thô (Log)</p>
                         <pre className="text-xs font-mono text-gray-800 bg-gray-50 p-3 rounded-lg border border-gray-100 overflow-x-auto">
{`{
  "ip": "116.102.13.44",
  "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X)",
  "deviceId": "A8F2-99B1-C34X",
  "location": { "lat": 21.037, "lng": 105.783 }
}`}
                         </pre>
                       </div>
                     </div>
                  )}

                </div>
              </div>
            </div>
            
            {/* Footer Modal */}
            <div className="p-6 border-t border-gray-100 bg-gray-50 flex gap-3 justify-end">
              <button 
                onClick={() => setSelectedDetail(null)} 
                className="px-5 py-2.5 rounded-xl font-bold text-gray-600 hover:bg-gray-200 transition-colors"
              >
                Đóng
              </button>
              {!(selectedDetail.status === 'RESOLVED' || selectedDetail.status === 'resolved') && (
                <button 
                  onClick={() => {
                    handleResolve(selectedDetail.id);
                    setSelectedDetail(null);
                  }}
                  className="px-5 py-2.5 rounded-xl font-bold bg-emerald-600 hover:bg-emerald-700 text-white flex items-center gap-2 transition-colors"
                >
                  <Check size={18} /> Đánh dấu đã xử lý
                </button>
              )}
            </div>

          </div>
        </div>
      )}
    </div>
  );
}