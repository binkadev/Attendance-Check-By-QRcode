import React, { useState, useEffect, useMemo } from 'react';
import {
  Search, FileText, CheckCircle2, XCircle, Clock, Filter,
  Download, AlertCircle, Eye, ThumbsUp, ThumbsDown, Loader2,
  CalendarX, Paperclip, ChevronDown, X
} from 'lucide-react';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// --- MOCK DATA (dùng khi chưa có API) ---
const MOCK_REQUESTS = [
  {
    id: 'req-1',
    student: { name: 'Nguyễn Văn An', id: 'STU-8832', avatar: null },
    sessionDate: 'Thứ 7, 16 tháng 5, 2026',
    reason: 'Cấp cứu y tế',
    attachment: 'medical_cert.pdf',
    attachmentType: 'pdf',
    evidenceUrl: 'https://raw.githubusercontent.com/mozilla/pdf.js/master/web/compressed.tracemonkey-pldi-09.pdf', // Tài liệu PDF mẫu chất lượng cao
    policyStatus: 'late', // 'on_time' | 'late'
    status: 'PENDING',
    submittedAt: new Date(Date.now() - 26 * 3600 * 1000).toISOString(),
  },
  {
    id: 'req-2',
    student: { name: 'Trần Thị Bình', id: 'STU-1044', avatar: null },
    sessionDate: 'Thứ 7, 16 tháng 5, 2026',
    reason: 'Sự kiện thể thao trường',
    attachment: 'coach_letter.jpg',
    attachmentType: 'img',
    evidenceUrl: 'https://images.unsplash.com/photo-1576091160550-2173dba999ef?auto=format&fit=crop&q=80&w=1200', // Hình ảnh minh chứng y tế/thể thao mẫu
    policyStatus: 'on_time',
    status: 'PENDING',
    submittedAt: new Date(Date.now() - 4 * 3600 * 1000).toISOString(),
  },
  {
    id: 'req-3',
    student: { name: 'Lê Hoàng Cường', id: 'STU-0921', avatar: null },
    sessionDate: 'Thứ 7, 16 tháng 5, 2026',
    reason: 'Việc gia đình khẩn cấp',
    attachment: null,
    attachmentType: null,
    evidenceUrl: null,
    policyStatus: 'on_time',
    status: 'APPROVED',
    submittedAt: new Date(Date.now() - 10 * 3600 * 1000).toISOString(),
    reviewedBy: 'Giảng viên',
    reviewNote: 'Chấp nhận vì lý do chính đáng.',
  },
  {
    id: 'req-4',
    student: { name: 'Phạm Thị Dung', id: 'STU-4412', avatar: null },
    sessionDate: 'Thứ 2, 12 tháng 5, 2026',
    reason: 'Lý do cá nhân',
    attachment: null,
    attachmentType: null,
    evidenceUrl: null,
    policyStatus: 'late',
    status: 'DENIED',
    submittedAt: new Date(Date.now() - 50 * 3600 * 1000).toISOString(),
    reviewedBy: 'Giảng viên',
    reviewNote: 'Không đủ bằng chứng.',
  },
];

// --- SUB-COMPONENT: FILE PREVIEW MODAL ---
function FilePreviewModal({ file, onClose }) {
  if (!file || !file.url) return null;

  return (
    <div className="fixed inset-0 z-[250] flex items-center justify-center p-4">
      {/* Backdrop mờ sang trọng */}
      <div 
        className="absolute inset-0 bg-gray-950/60 backdrop-blur-md transition-opacity duration-300 animate-in fade-in"
        onClick={onClose}
      ></div>

      {/* Card Preview */}
      <div className="relative bg-white rounded-3xl shadow-2xl max-w-4xl w-full h-[80vh] flex flex-col overflow-hidden border border-gray-100/80 animate-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-slate-50">
          <div>
            <h3 className="font-bold text-gray-900 flex items-center gap-2">
              <FileText size={18} className="text-red-500" />
              Minh chứng: {file.name}
            </h3>
            <p className="text-[11px] text-gray-400 mt-0.5 font-bold uppercase tracking-wider">Tài liệu đính kèm đơn xin nghỉ</p>
          </div>
          <div className="flex items-center gap-3">
            <a 
              href={file.url} 
              target="_blank" 
              rel="noreferrer" 
              className="px-4 py-2 bg-white border border-gray-200 hover:border-gray-300 text-gray-700 font-bold text-xs rounded-xl shadow-sm transition-all hover:scale-105 active:scale-95 flex items-center gap-1.5"
            >
              <Download size={14} className="text-gray-500" /> Tải file gốc
            </a>
            <button 
              onClick={onClose} 
              className="p-2 hover:bg-gray-200 text-gray-400 hover:text-gray-600 rounded-full transition-all"
            >
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Viewer */}
        <div className="flex-1 bg-slate-100 flex items-center justify-center overflow-hidden p-4">
          {file.type === 'pdf' ? (
            <iframe 
              src={`https://docs.google.com/viewer?url=${encodeURIComponent(file.url)}&embedded=true`}
              className="w-full h-full rounded-2xl border border-gray-200/50 shadow-inner bg-white"
              title={file.name}
            />
          ) : (
            <div className="max-w-full max-h-full overflow-auto flex items-center justify-center p-2">
              <img 
                src={file.url} 
                alt={file.name} 
                className="max-w-full max-h-[68vh] object-contain rounded-2xl shadow-lg border border-white"
                onError={(e) => {
                  e.target.onerror = null;
                  e.target.src = "https://placehold.co/600x400?text=Không+thể+hiển+thị+ảnh";
                }}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// --- SUB-COMPONENT: REVIEW MODAL ---
function ReviewModal({ request, onClose, onApprove, onDeny, onPreviewFile }) {
  const [note, setNote] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAction = async (action) => {
    setLoading(true);
    if (action === 'approve') await onApprove(request.id, note);
    else await onDeny(request.id, note);
    setLoading(false);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-6 border-b border-gray-100 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-gray-900">Xem xét đơn xin nghỉ</h2>
            <p className="text-sm text-gray-500 mt-0.5">{request.student.name} · {request.student.id}</p>
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X size={20} className="text-gray-500" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-4">
          <div className="bg-gray-50 rounded-xl p-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500 font-medium">Buổi học</span>
              <span className="font-bold text-gray-800">{request.sessionDate}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 font-medium">Lý do</span>
              <span className="font-bold text-gray-800">{request.reason}</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-gray-500 font-medium">Trạng thái nộp</span>
              {request.policyStatus === 'on_time' ? (
                <span className="text-[11px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md flex items-center gap-1">
                  <CheckCircle2 size={11} /> Đúng hạn
                </span>
              ) : (
                <span className="text-[11px] font-bold text-red-600 bg-red-50 border border-red-200 px-2 py-0.5 rounded-md flex items-center gap-1">
                  <AlertCircle size={11} /> Nộp muộn
                </span>
              )}
            </div>
            {request.attachment && (
              <div className="flex justify-between items-center">
                <span className="text-gray-500 font-medium">Tệp đính kèm</span>
                <button 
                  onClick={(e) => {
                    e.preventDefault();
                    onPreviewFile({
                      url: request.evidenceUrl || request.url,
                      type: request.attachmentType,
                      name: request.attachment
                    });
                  }}
                  className="text-indigo-600 font-semibold text-xs flex items-center gap-1 hover:underline bg-transparent border-none p-0 cursor-pointer"
                >
                  <Paperclip size={12} /> {request.attachment}
                </button>
              </div>
            )}
          </div>

          <div>
            <label className="block text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-1.5 ml-1">
              Ghi chú của giảng viên (tùy chọn)
            </label>
            <textarea
              value={note}
              onChange={e => setNote(e.target.value)}
              rows={3}
              placeholder="Nhập ghi chú phản hồi cho sinh viên..."
              className="w-full border border-gray-200 rounded-xl px-4 py-3 text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50 resize-none transition-all text-gray-700"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="p-6 pt-0 flex gap-3">
          <button
            onClick={() => handleAction('deny')}
            disabled={loading}
            className="flex-1 bg-red-50 border border-red-200 text-red-600 hover:bg-red-100 px-4 py-2.5 rounded-xl text-sm font-bold flex items-center justify-center gap-2 transition-colors disabled:opacity-50 active:scale-[0.98]"
          >
            <ThumbsDown size={16} /> Từ chối
          </button>
          <button
            onClick={() => handleAction('approve')}
            disabled={loading}
            className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2.5 rounded-xl text-sm font-bold flex items-center justify-center gap-2 transition-colors disabled:opacity-50 active:scale-[0.98]"
          >
            {loading ? <Loader2 size={16} className="animate-spin" /> : <ThumbsUp size={16} />}
            Phê duyệt
          </button>
        </div>
      </div>
    </div>
  );
}

// =============================================================
// MAIN COMPONENT
// =============================================================
export default function AbsenceTab({ classId }) {
  const [requests, setRequests] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [reviewingRequest, setReviewingRequest] = useState(null);
  
  // State quản lý xem thử tệp đính kèm trực tuyến
  const [previewFile, setPreviewFile] = useState(null); // { url, type, name }

  // Fetch data from API
  const fetchAbsenceRequests = async () => {
    if (!classId || classId.startsWith('mock-')) {
      setRequests(MOCK_REQUESTS);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      // Lấy cả đơn xin phép và danh sách sinh viên để map tên
      const [absenceRes, membersRes] = await Promise.all([
        classApi.getAbsenceRequests(classId, { size: 100 }),
        classApi.getClassMembers(classId)
      ]);

      const members = membersRes.items || [];
      const memberMap = new Map(members.map(m => [m.userId, m]));
      
      const realRequests = (absenceRes.items || []).map(item => {
        const studentInfo = memberMap.get(item.requesterUserId) || {};
        return {
          id: item.id,
          student: { 
            name: studentInfo.fullName || `SV-${item.requesterUserId.substring(0, 4)}`, 
            id: studentInfo.userCode || item.requesterUserId.substring(0, 8), 
            avatar: null 
          },
          sessionDate: item.requestedDate ? new Date(item.requestedDate).toLocaleDateString('vi-VN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) : 'Không rõ',
          reason: item.reason || 'Không có lý do',
          attachment: item.evidenceUrl ? 'Bằng chứng kèm theo.jpg' : null,
          attachmentType: item.evidenceUrl?.endsWith('.pdf') ? 'pdf' : 'img',
          evidenceUrl: item.evidenceUrl,
          policyStatus: 'on_time', // Mặc định
          status: item.requestStatus, // PENDING, APPROVED, REJECTED
          submittedAt: item.createdAt,
          reviewedBy: item.reviewerUserId ? 'Giảng viên' : null,
          reviewNote: item.reviewerNote,
        };
      });

      // Kết hợp dữ liệu thật và dữ liệu giả (ưu tiên thật)
      if (realRequests.length === 0) {
        setRequests(MOCK_REQUESTS);
      } else {
        setRequests([...realRequests, ...MOCK_REQUESTS]);
      }
    } catch (error) {
      console.error("Lỗi khi tải đơn xin phép:", error);
      setRequests(MOCK_REQUESTS);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAbsenceRequests();
  }, [classId]);

  // Stats
  const stats = useMemo(() => ({
    total: requests.length,
    pending: requests.filter(r => r.status === 'PENDING').length,
    approved: requests.filter(r => r.status === 'APPROVED' || r.status === 'accepted').length,
    denied: requests.filter(r => r.status === 'REJECTED' || r.status === 'DENIED' || r.status === 'rejected').length,
  }), [requests]);

  // Filtered list
  const filtered = useMemo(() => {
    return requests.filter(r => {
      const matchSearch = (r.student?.name || '').toLowerCase().includes(search.toLowerCase())
        || (r.student?.id || '').toLowerCase().includes(search.toLowerCase());
      
      let matchStatus = false;
      if (statusFilter === 'ALL') matchStatus = true;
      else if (statusFilter === 'PENDING') matchStatus = r.status === 'PENDING';
      else if (statusFilter === 'APPROVED') matchStatus = (r.status === 'APPROVED' || r.status === 'accepted');
      else if (statusFilter === 'DENIED') matchStatus = (r.status === 'REJECTED' || r.status === 'DENIED' || r.status === 'rejected');
      
      return matchSearch && matchStatus;
    });
  }, [requests, search, statusFilter]);

  const handleApprove = async (id, note) => {
    if (String(id).startsWith('req-')) {
      setRequests(prev => prev.map(r =>
        r.id === id ? { ...r, status: 'APPROVED', reviewedBy: 'Giảng viên', reviewNote: note } : r
      ));
      toast.success("Phê duyệt đơn thành công (Dữ liệu giả)");
      return;
    }

    try {
      await classApi.reviewAbsenceRequest(id, { action: 'APPROVE', reviewerNote: note });
      toast.success("Đã phê duyệt đơn xin nghỉ");
      fetchAbsenceRequests();
    } catch (error) {
      toast.error("Lỗi khi phê duyệt đơn: " + (error.message || "Không xác định"));
    }
  };

  const handleDeny = async (id, note) => {
    if (String(id).startsWith('req-')) {
      setRequests(prev => prev.map(r =>
        r.id === id ? { ...r, status: 'DENIED', reviewedBy: 'Giảng viên', reviewNote: note } : r
      ));
      toast.success("Từ chối đơn thành công (Dữ liệu giả)");
      return;
    }

    try {
      await classApi.reviewAbsenceRequest(id, { action: 'REJECT', reviewerNote: note });
      toast.success("Đã từ chối đơn xin nghỉ");
      fetchAbsenceRequests();
    } catch (error) {
      toast.error("Lỗi khi xử lý đơn: " + (error.message || "Không xác định"));
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'PENDING':
        return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-bold bg-amber-50 text-amber-600 border border-amber-200"><Clock size={11} /> Đang chờ</span>;
      case 'APPROVED':
        return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-bold bg-emerald-50 text-emerald-600 border border-emerald-200"><CheckCircle2 size={11} /> Đã duyệt</span>;
      case 'DENIED':
        return <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-[11px] font-bold bg-red-50 text-red-600 border border-red-200"><XCircle size={11} /> Từ chối</span>;
      default: return null;
    }
  };

  return (
    <div className="flex flex-col gap-6 animate-in fade-in duration-300">
      {/* === SUMMARY CARDS === */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-9 h-9 rounded-lg bg-gray-100 flex items-center justify-center text-gray-500">
              <FileText size={18} />
            </div>
            <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Tổng đơn xin nghỉ</p>
          </div>
          <p className="text-3xl font-extrabold text-gray-900 tracking-tight">{stats.total}</p>
        </div>

        {/* Pending */}
        <div className={`rounded-xl border shadow-sm p-5 ${stats.pending > 0 ? 'bg-red-50 border-red-200' : 'bg-white border-gray-200'}`}>
          <div className="flex items-center gap-3 mb-2">
            <div className={`w-9 h-9 rounded-lg flex items-center justify-center ${stats.pending > 0 ? 'bg-red-100 text-red-600' : 'bg-gray-100 text-gray-400'}`}>
              <Clock size={18} />
            </div>
            <p className={`text-[11px] font-bold uppercase tracking-wider ${stats.pending > 0 ? 'text-red-500' : 'text-gray-400'}`}>Chờ xem xét</p>
          </div>
          <p className={`text-3xl font-extrabold tracking-tight ${stats.pending > 0 ? 'text-red-600' : 'text-gray-900'}`}>{stats.pending}</p>
        </div>

        {/* Approved */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-9 h-9 rounded-lg bg-emerald-50 flex items-center justify-center text-emerald-600">
              <CheckCircle2 size={18} />
            </div>
            <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Đã duyệt</p>
          </div>
          <p className="text-3xl font-extrabold text-gray-900 tracking-tight">{stats.approved}</p>
        </div>

        {/* Denied */}
        <div className="bg-white rounded-xl border border-gray-200 shadow-sm p-5">
          <div className="flex items-center gap-3 mb-2">
            <div className="w-9 h-9 rounded-lg bg-gray-100 flex items-center justify-center text-gray-500">
              <XCircle size={18} />
            </div>
            <p className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">Từ chối</p>
          </div>
          <p className="text-3xl font-extrabold text-gray-900 tracking-tight">{stats.denied}</p>
        </div>
      </div>

      {/* === MAIN TABLE CARD === */}
      <div className="bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
        {/* Toolbar */}
        <div className="p-4 sm:p-5 border-b border-gray-100 flex flex-col sm:flex-row gap-3 items-start sm:items-center justify-between">
          <div className="flex gap-3 w-full sm:w-auto">
            {/* Search */}
            <div className="relative flex-1 sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={15} />
              <input
                type="text"
                placeholder="Tìm sinh viên hoặc MSSV..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-lg text-sm outline-none focus:border-red-400 focus:ring-2 focus:ring-red-50 transition-all"
              />
            </div>

            {/* Status filter */}
            <div className="relative">
              <select
                value={statusFilter}
                onChange={e => setStatusFilter(e.target.value)}
                className="appearance-none bg-white border border-gray-200 rounded-lg px-3 py-2 pr-8 text-sm font-semibold text-gray-700 outline-none focus:border-red-400 cursor-pointer"
              >
                <option value="ALL">Tất cả trạng thái</option>
                <option value="PENDING">Đang chờ</option>
                <option value="APPROVED">Đã duyệt</option>
                <option value="DENIED">Từ chối</option>
              </select>
              <ChevronDown size={14} className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {/* Export */}
          <button className="flex items-center gap-2 bg-[#111827] hover:bg-gray-800 text-white px-4 py-2 rounded-lg text-sm font-bold transition-colors shrink-0">
            <Download size={15} /> Xuất file CSV/PDF
          </button>
        </div>

        {/* Table */}
        <div className="overflow-x-auto min-h-[300px]">
          {isLoading ? (
            <div className="flex items-center justify-center h-48">
              <Loader2 className="animate-spin text-gray-400" size={28} />
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-48 text-gray-400">
              <CalendarX size={32} className="mb-2 opacity-40" />
              <p className="font-medium">Không tìm thấy đơn xin nghỉ nào.</p>
            </div>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-50 border-b border-gray-100 text-[11px] text-gray-500 font-bold uppercase tracking-wider">
                <tr>
                  <th className="px-5 py-3.5 w-[260px]">Thông tin sinh viên</th>
                  <th className="px-5 py-3.5">Lý do & Bằng chứng</th>
                  <th className="px-5 py-3.5">Trạng thái quy định</th>
                  <th className="px-5 py-3.5">Trạng thái</th>
                  <th className="px-5 py-3.5 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {filtered.map(req => (
                  <tr key={req.id} className="hover:bg-gray-50/60 transition-colors">
                    {/* Student Info */}
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <img
                          src={req.student.avatar || `https://ui-avatars.com/api/?name=${encodeURIComponent(req.student.name)}&background=random&size=64`}
                          alt={req.student.name}
                          className="w-9 h-9 rounded-full shrink-0"
                        />
                        <div>
                          <p className="font-bold text-gray-900 leading-tight">{req.student.name}</p>
                          <p className="text-[11px] text-gray-400 font-mono mt-0.5">{req.student.id}</p>
                          <p className="text-[11px] text-gray-400 mt-0.5">Yêu cầu cho: {req.sessionDate}</p>
                        </div>
                      </div>
                    </td>

                    {/* Reason & Evidence */}
                    <td className="px-5 py-4">
                      <p className="font-semibold text-gray-800 mb-1">{req.reason}</p>
                      {req.attachment ? (
                        <button
                          onClick={(e) => {
                            e.preventDefault();
                            setPreviewFile({
                              url: req.evidenceUrl,
                              type: req.attachmentType,
                              name: req.attachment
                            });
                          }}
                          className="flex items-center gap-1.5 text-indigo-600 hover:underline text-xs font-semibold bg-transparent border-none p-0 cursor-pointer"
                        >
                          {req.attachmentType === 'pdf' ? (
                            <span className="bg-red-100 text-red-600 text-[10px] font-black px-1.5 py-0.5 rounded uppercase">PDF</span>
                          ) : (
                            <span className="bg-blue-100 text-blue-600 text-[10px] font-black px-1.5 py-0.5 rounded uppercase">IMG</span>
                          )}
                          {req.attachment}
                        </button>
                      ) : (
                        <span className="text-xs text-gray-400 italic">Không có tệp đính kèm</span>
                      )}
                    </td>

                    {/* Policy Status */}
                    <td className="px-5 py-4">
                      {req.policyStatus === 'on_time' ? (
                        <div>
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-emerald-600 bg-emerald-50 border border-emerald-200 px-2.5 py-1 rounded-md">
                            <CheckCircle2 size={11} /> Đúng hạn
                          </span>
                        </div>
                      ) : (
                        <div>
                          <span className="inline-flex items-center gap-1 text-[11px] font-bold text-red-600 bg-red-50 border border-red-200 px-2.5 py-1 rounded-md">
                            <AlertCircle size={11} /> Nộp muộn
                          </span>
                          <p className="text-[10px] text-gray-400 mt-1">&gt;24h sau buổi học</p>
                        </div>
                      )}
                    </td>

                    {/* Status */}
                    <td className="px-5 py-4">
                      <div>
                        {getStatusBadge(req.status)}
                        {req.reviewedBy && (
                          <p className="text-[10px] text-gray-400 mt-1">bởi {req.reviewedBy}</p>
                        )}
                      </div>
                    </td>

                    {/* Actions */}
                    <td className="px-5 py-4 text-right">
                      {req.status === 'PENDING' ? (
                        <button
                          onClick={() => setReviewingRequest(req)}
                          className="bg-red-600 hover:bg-red-700 text-white text-xs font-bold px-4 py-2 rounded-lg transition-colors active:scale-[0.95]"
                        >
                          Xem xét
                        </button>
                      ) : (
                        <button
                          onClick={() => setReviewingRequest(req)}
                          className="text-indigo-600 hover:bg-indigo-50 border border-indigo-200 text-xs font-bold px-4 py-2 rounded-lg transition-colors active:scale-[0.95]"
                        >
                          Xem chi tiết
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Footer count */}
        {!isLoading && filtered.length > 0 && (
          <div className="px-5 py-3 border-t border-gray-100 bg-gray-50/50">
            <p className="text-xs text-gray-400 font-medium">
              Hiển thị <span className="font-bold text-gray-600">{filtered.length}</span> / {requests.length} đơn xin nghỉ
            </p>
          </div>
        )}
      </div>

      {/* Review Modal */}
      {reviewingRequest && (
        <ReviewModal
          request={reviewingRequest}
          onClose={() => setReviewingRequest(null)}
          onApprove={handleApprove}
          onDeny={handleDeny}
          onPreviewFile={(file) => setPreviewFile(file)}
        />
      )}

      {/* File Preview Modal */}
      {previewFile && (
        <FilePreviewModal
          file={previewFile}
          onClose={() => setPreviewFile(null)}
        />
      )}
    </div>
  );
}
