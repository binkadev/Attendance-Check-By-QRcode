import React, { useState, useEffect, useCallback } from 'react';
import {
  Search, ChevronDown, Users as UsersIcon, AlertCircle, X,
  QrCode, UserSquare2, Loader2, CheckCircle, Clock, XCircle, RefreshCw
} from 'lucide-react';
import Sidebar from '../../../components/layout/Sidebar';
import { classApi } from '../../../api/classApi';
import toast from 'react-hot-toast';

// --- Helpers ---
const STATUS_MAP = {
  PRESENT:  { label: 'Có mặt',   cls: 'bg-emerald-100 text-emerald-700 border-emerald-200' },
  LATE:     { label: 'Muộn',     cls: 'bg-amber-100  text-amber-700  border-amber-200'  },
  ABSENT:   { label: 'Vắng mặt', cls: 'bg-red-100    text-red-700    border-red-200'    },
  EXCUSED:  { label: 'Có phép',  cls: 'bg-blue-100   text-blue-700   border-blue-200'   },
};
const STATUS_OPTIONS = ['PRESENT', 'LATE', 'ABSENT', 'EXCUSED'];
const STATUS_LABELS  = { PRESENT: 'Có mặt', LATE: 'Muộn', ABSENT: 'Vắng mặt', EXCUSED: 'Có phép' };

const METHOD_MAP = {
  'MANUAL': 'Thủ công',
  'QR': 'QR Động',
  'QR_DYNAMIC': 'QR Động',
  'GPS': 'GPS',
  'SYSTEM': 'Hệ thống',
};

const fmtDate = (iso) => iso
  ? new Date(iso).toLocaleString('vi-VN', { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit' })
  : '--';

const avatarUrl = (seed) => `https://i.pravatar.cc/80?u=${seed}`;

export default function HistoryAndManualEdit() {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(
    () => localStorage.getItem('sidebar_collapsed') === 'true'
  );

  // --- DATA STATES ---
  const [classes,  setClasses]  = useState([]);
  const [sessions, setSessions] = useState([]);
  const [records,  setRecords]  = useState([]); // flat attendance records

  const [isLoadingClasses,  setIsLoadingClasses]  = useState(true);
  const [isLoadingSessions, setIsLoadingSessions] = useState(false);
  const [isLoadingRecords,  setIsLoadingRecords]  = useState(false);

  // --- FILTER STATES ---
  const [selectedClassId, setSelectedClassId] = useState('');
  const [selectedSessionId, setSelectedSessionId] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  // --- PANEL STATES ---
  const [panelRecord,  setPanelRecord]  = useState(null);
  const [editStatus,   setEditStatus]   = useState('');
  const [editNote,     setEditNote]     = useState('');
  const [isSaving,     setIsSaving]     = useState(false);

  // --- PAGINATION ---
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 10;

  // 1. Load danh sách lớp
  useEffect(() => {
    const load = async () => {
      setIsLoadingClasses(true);
      try {
        const res = await classApi.getTeachingClasses();
        const items = res.items || [];
        setClasses(items);
        if (items.length > 0) setSelectedClassId(items[0].groupId || items[0].id);
      } catch (e) {
        toast.error('Không tải được danh sách lớp');
      } finally {
        setIsLoadingClasses(false);
      }
    };
    load();
  }, []);

  // 2. Load phiên khi chọn lớp
  useEffect(() => {
    if (!selectedClassId) return;
    const load = async () => {
      setIsLoadingSessions(true);
      setSessions([]);
      setSelectedSessionId('');
      setRecords([]);
      try {
        const res = await classApi.getGroupSessions(selectedClassId);
        const items = (res.items || []).sort(
          (a, b) => new Date(b.checkinOpenAt || b.createdAt) - new Date(a.checkinOpenAt || a.createdAt)
        );
        setSessions(items);
        if (items.length > 0) setSelectedSessionId(items[0].id);
      } catch (e) {
        toast.error('Không tải được danh sách phiên');
      } finally {
        setIsLoadingSessions(false);
      }
    };
    load();
  }, [selectedClassId]);

  // 3. Load attendance events khi chọn phiên
  const loadRecords = useCallback(async () => {
    if (!selectedSessionId || !selectedClassId) return;
    setIsLoadingRecords(true);
    try {
      const [eventsRes, membersRes] = await Promise.all([
        classApi.getAttendanceEvents(selectedSessionId, 200),
        classApi.getClassMembers(selectedClassId),
      ]);
      const events  = Array.isArray(eventsRes)  ? eventsRes  : (eventsRes.items  || []);
      const members = Array.isArray(membersRes) ? membersRes : (membersRes.items || []);
      const students = members.filter(m => m.role !== 'LECTURER' && m.role !== 'OWNER');

      // Latest status per user from events
      const statusMap = new Map();
      const methodMap = new Map();
      [...events]
        .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt))
        .forEach(e => {
          statusMap.set(String(e.userId), e.newStatus);
          const rawMethod = e.method || e.source || 'QR';
          const cleanMethod = METHOD_MAP[rawMethod.toUpperCase()] || rawMethod;
          methodMap.set(String(e.userId), cleanMethod);
        });

      const session = sessions.find(s => s.id === selectedSessionId);
      const cls     = classes.find(c => (c.groupId || c.id) === selectedClassId);

      const flat = students.map(m => {
        const uid    = String(m.studentCode || m.code || m.userId || m.id);
        const status = statusMap.get(uid) || 'ABSENT';
        const method = methodMap.get(uid) || (status === 'ABSENT' ? 'Hệ thống' : 'QR Động');
        return {
          uid,
          studentName: m.fullName || m.name || 'Không rõ',
          studentCode: m.studentCode || m.code || uid.slice(0, 8).toUpperCase(),
          status,
          method,
          sessionId: selectedSessionId,
          sessionTitle: session?.title || `Phiên ${fmtDate(session?.checkinOpenAt)}`,
          sessionDate:  fmtDate(session?.checkinOpenAt),
          className:    cls?.groupName || '',
          courseCode:   cls?.courseCode || '',
          classCode:    cls?.classCode || '',
        };
      });
      setRecords(flat);
      setPage(1);
    } catch (e) {
      toast.error('Không tải được dữ liệu điểm danh');
    } finally {
      setIsLoadingRecords(false);
    }
  }, [selectedSessionId, selectedClassId, sessions, classes]);

  useEffect(() => { loadRecords(); }, [loadRecords]);

  // --- FILTER + PAGINATION ---
  const filtered = records.filter(r => {
    const q = searchQuery.toLowerCase();
    const matchQ = !q || r.studentName.toLowerCase().includes(q) || r.studentCode.toLowerCase().includes(q);
    const matchS = !statusFilter || r.status === statusFilter;
    return matchQ && matchS;
  });
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const pageRecords = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  // --- STATS ---
  const totalCount    = records.length;
  const absentCount   = records.filter(r => r.status === 'ABSENT').length;
  const presentCount  = records.filter(r => r.status === 'PRESENT' || r.status === 'LATE').length;

  // --- OPEN PANEL ---
  const openPanel = (r) => {
    setPanelRecord(r);
    setEditStatus(r.status);
    setEditNote('');
  };

  // --- SAVE ---
  const handleSave = async () => {
    if (!panelRecord) return;
    setIsSaving(true);
    try {
      await classApi.submitAttendance(panelRecord.sessionId, panelRecord.uid, {
        status: editStatus,
        note: editNote || `Chỉnh sửa thủ công: ${STATUS_LABELS[editStatus]}`,
        method: 'MANUAL',
      });
      toast.success('Đã cập nhật điểm danh!');
      setRecords(prev => prev.map(r =>
        r.uid === panelRecord.uid && r.sessionId === panelRecord.sessionId
          ? { ...r, status: editStatus, method: 'Thủ công' }
          : r
      ));
      setPanelRecord(null);
    } catch (e) {
      toast.error(e.message || 'Lưu thất bại');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="flex min-h-screen bg-gray-50 font-sans text-gray-800">
      <Sidebar onCollapseChange={setIsSidebarCollapsed} />

      <div className={`flex-1 flex h-screen overflow-hidden transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        <main className="flex-1 flex flex-col h-full overflow-y-auto relative">

          {/* HEADER */}
          <header className="px-8 py-6 bg-white border-b border-gray-200 sticky top-0 z-10 flex items-center justify-between">
            <div>
              <h2 className="text-2xl font-bold text-gray-900">Lịch sử điểm danh &amp; Chỉnh sửa</h2>
              <p className="text-sm text-gray-500 mt-0.5">Xem và chỉnh sửa thủ công bản ghi điểm danh</p>
            </div>
            <button onClick={loadRecords} className="flex items-center gap-2 px-4 py-2 border border-gray-200 rounded-lg text-sm font-semibold text-gray-600 hover:bg-gray-50 transition-colors">
              <RefreshCw size={15} /> Làm mới
            </button>
          </header>

          <div className="p-8 w-full flex-1">

            {/* FILTERS */}
            <div className="flex flex-wrap items-center gap-3 mb-6">
              {/* Search */}
              <div className="relative flex-1 min-w-[200px] max-w-xs">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={16} />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={e => { setSearchQuery(e.target.value); setPage(1); }}
                  placeholder="Tìm tên, MSSV..."
                  className="w-full pl-9 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-red-400"
                />
              </div>

              {/* Class select */}
              <div className="relative min-w-[180px]">
                {isLoadingClasses ? (
                  <div className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-lg bg-white text-sm text-gray-400">
                    <Loader2 size={14} className="animate-spin" /> Đang tải...
                  </div>
                ) : (
                  <>
                    <select
                      value={selectedClassId}
                      onChange={e => setSelectedClassId(e.target.value)}
                      className="w-full pl-4 pr-10 py-2.5 border border-gray-200 rounded-lg appearance-none text-sm bg-white focus:outline-none focus:ring-2 focus:ring-red-400"
                    >
                      {classes.map(c => {
                        const classLabel = [
                          c.courseCode,
                          c.classCode || c.code || '',
                          c.groupName || c.name || ''
                        ].filter(Boolean).join(' – ');
                        return (
                          <option key={c.groupId || c.id} value={c.groupId || c.id}>
                            {classLabel || 'Không rõ lớp'}
                          </option>
                        );
                      })}
                    </select>
                    <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" size={16} />
                  </>
                )}
              </div>

              {/* Session select */}
              <div className="relative min-w-[220px]">
                {isLoadingSessions ? (
                  <div className="flex items-center gap-2 px-4 py-2.5 border border-gray-200 rounded-lg bg-white text-sm text-gray-400">
                    <Loader2 size={14} className="animate-spin" /> Đang tải phiên...
                  </div>
                ) : (
                  <>
                    <select
                      value={selectedSessionId}
                      onChange={e => setSelectedSessionId(e.target.value)}
                      className="w-full pl-4 pr-10 py-2.5 border border-gray-200 rounded-lg appearance-none text-sm bg-white focus:outline-none focus:ring-2 focus:ring-red-400"
                    >
                      {sessions.length === 0 && <option value="">Không có phiên nào</option>}
                      {sessions.map(s => (
                        <option key={s.id} value={s.id}>
                          {s.title || fmtDate(s.checkinOpenAt)}
                        </option>
                      ))}
                    </select>
                    <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" size={16} />
                  </>
                )}
              </div>

              {/* Status filter */}
              <div className="relative min-w-[140px]">
                <select
                  value={statusFilter}
                  onChange={e => { setStatusFilter(e.target.value); setPage(1); }}
                  className="w-full pl-4 pr-10 py-2.5 border border-gray-200 rounded-lg appearance-none text-sm bg-white focus:outline-none focus:ring-2 focus:ring-red-400"
                >
                  <option value="">Tất cả trạng thái</option>
                  {STATUS_OPTIONS.map(s => <option key={s} value={s}>{STATUS_LABELS[s]}</option>)}
                </select>
                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" size={16} />
              </div>
            </div>

            {/* STATS */}
            <div className="grid grid-cols-3 gap-5 mb-6">
              <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex items-center gap-4">
                <div className="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center"><UsersIcon size={18} className="text-gray-600" /></div>
                <div>
                  <p className="text-xs font-semibold text-gray-500 mb-0.5">Tổng sinh viên</p>
                  <h3 className="text-2xl font-bold text-gray-900">{isLoadingRecords ? '...' : totalCount}</h3>
                </div>
              </div>
              <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm flex items-center gap-4">
                <div className="w-10 h-10 bg-emerald-50 rounded-full flex items-center justify-center"><CheckCircle size={18} className="text-emerald-600" /></div>
                <div>
                  <p className="text-xs font-semibold text-gray-500 mb-0.5">Đã điểm danh</p>
                  <h3 className="text-2xl font-bold text-emerald-600">{isLoadingRecords ? '...' : presentCount}</h3>
                </div>
              </div>
              <div className="bg-red-50 p-5 rounded-2xl border border-red-100 shadow-sm flex items-center gap-4">
                <div className="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center"><XCircle size={18} className="text-red-600" /></div>
                <div>
                  <p className="text-xs font-semibold text-red-700 mb-0.5">Vắng mặt</p>
                  <h3 className="text-2xl font-bold text-red-600">{isLoadingRecords ? '...' : absentCount}</h3>
                </div>
              </div>
            </div>

            {/* TABLE */}
            <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
              <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
                <h3 className="font-bold text-gray-900">Nhật ký điểm danh</h3>
                <span className="text-xs text-gray-500">{filtered.length} bản ghi</span>
              </div>

              {isLoadingRecords ? (
                <div className="flex justify-center items-center h-40">
                  <Loader2 className="animate-spin text-red-600" size={28} />
                </div>
              ) : filtered.length === 0 ? (
                <div className="text-center py-16 text-gray-400">
                  <UsersIcon size={40} className="mx-auto mb-3 opacity-30" />
                  <p className="font-medium">Không có dữ liệu điểm danh</p>
                </div>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="bg-gray-50 text-gray-500 font-bold uppercase text-xs">
                    <tr>
                      <th className="px-6 py-4 border-b border-gray-100">SINH VIÊN</th>
                      <th className="px-6 py-4 border-b border-gray-100">PHIÊN HỌC</th>
                      <th className="px-6 py-4 border-b border-gray-100">TRẠNG THÁI</th>
                      <th className="px-6 py-4 border-b border-gray-100">PHƯƠNG THỨC</th>
                      <th className="px-6 py-4 border-b border-gray-100"></th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 bg-white">
                    {pageRecords.map((r, idx) => (
                      <tr
                        key={`${r.uid}-${idx}`}
                        className={`transition ${r.status === 'ABSENT' ? 'bg-red-50/30 hover:bg-red-50' : 'hover:bg-gray-50'} ${panelRecord?.uid === r.uid ? 'ring-1 ring-inset ring-red-200' : ''}`}
                      >
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <img src={avatarUrl(r.uid)} alt={r.studentName} className="w-9 h-9 rounded-full bg-gray-100 object-cover" />
                            <div>
                              <p className="font-bold text-gray-900">{r.studentName}</p>
                              <p className="text-xs text-gray-400">{r.studentCode}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <p className="font-semibold text-gray-800 text-sm">
                            {[r.courseCode, r.classCode, r.className].filter(Boolean).join(' – ')}
                          </p>
                          <p className="text-xs text-gray-400 mt-0.5">{r.sessionDate}</p>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`px-2.5 py-1 text-xs font-bold rounded-md border ${STATUS_MAP[r.status]?.cls || 'bg-gray-100 text-gray-600 border-gray-200'}`}>
                            {STATUS_MAP[r.status]?.label || r.status}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-1.5 text-xs text-gray-500 font-medium">
                            {r.method.includes('QR') ? <QrCode size={12} /> : <UserSquare2 size={12} />}
                            {r.method}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => openPanel(r)}
                            className="px-3 py-1.5 text-xs font-bold border border-gray-200 rounded-lg text-gray-600 hover:bg-gray-50 hover:border-gray-300 transition-colors"
                          >
                            Chỉnh sửa
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}

              {/* PAGINATION */}
              {totalPages > 1 && (
                <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between bg-gray-50/50">
                  <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                    className="px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-500 disabled:opacity-40 hover:bg-white transition-colors">
                    Trước
                  </button>
                  <div className="flex items-center gap-1">
                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => i + 1).map(p => (
                      <button key={p} onClick={() => setPage(p)}
                        className={`w-8 h-8 rounded-lg text-sm font-bold transition-colors ${p === page ? 'bg-red-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
                        {p}
                      </button>
                    ))}
                    {totalPages > 5 && <span className="px-2 text-gray-400">...</span>}
                  </div>
                  <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                    className="px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 disabled:opacity-40 hover:bg-white transition-colors">
                    Sau
                  </button>
                </div>
              )}
            </div>
          </div>
        </main>

        {/* SIDE PANEL */}
        {panelRecord && (
          <aside className="w-[400px] bg-white border-l border-gray-200 shadow-2xl flex flex-col h-full shrink-0 sticky top-0">
            <div className="flex items-center justify-between px-6 py-5 border-b border-gray-200 bg-gray-50">
              <h3 className="font-bold text-lg text-gray-900">Chỉnh sửa thủ công</h3>
              <button onClick={() => setPanelRecord(null)} className="p-1.5 rounded-md border border-gray-200 text-gray-400 hover:text-gray-900 hover:bg-white transition-colors">
                <X size={16} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Student info */}
              <div className="flex items-center gap-4 p-4 bg-gray-50 border border-gray-200 rounded-xl">
                <img src={avatarUrl(panelRecord.uid)} alt={panelRecord.studentName} className="w-12 h-12 rounded-full object-cover" />
                <div>
                  <h4 className="font-bold text-gray-900">{panelRecord.studentName}</h4>
                  <p className="text-xs text-gray-500">
                    {panelRecord.studentCode} • {[panelRecord.courseCode, panelRecord.classCode, panelRecord.className].filter(Boolean).join(' – ')}
                  </p>
                  <p className="text-xs text-gray-400 mt-0.5">{panelRecord.sessionDate}</p>
                </div>
              </div>

              {/* Status picker */}
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Cập nhật trạng thái</label>
                <div className="grid grid-cols-2 gap-2">
                  {STATUS_OPTIONS.map(s => (
                    <label key={s} className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer border-2 transition-all ${editStatus === s ? 'border-red-500 bg-red-50' : 'border-transparent bg-gray-50 hover:bg-gray-100'}`}>
                      <input type="radio" name="editStatus" value={s} checked={editStatus === s} onChange={() => setEditStatus(s)} className="accent-red-600 w-4 h-4" />
                      <span className={`font-bold text-sm ${editStatus === s ? 'text-gray-900' : 'text-gray-600'}`}>{STATUS_LABELS[s]}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Note */}
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Ghi chú (Nhật ký hệ thống)</label>
                <textarea
                  rows="3"
                  value={editNote}
                  onChange={e => setEditNote(e.target.value)}
                  placeholder="Lý do chỉnh sửa..."
                  className="w-full p-3 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-400 resize-none bg-white"
                />
              </div>

              {/* Current status */}
              <div className="p-4 bg-gray-50 rounded-xl border border-gray-200">
                <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Trạng thái hiện tại</p>
                <div className="flex items-center gap-3">
                  <span className={`px-3 py-1 text-xs font-bold rounded-md border ${STATUS_MAP[panelRecord.status]?.cls}`}>
                    {STATUS_MAP[panelRecord.status]?.label}
                  </span>
                  <span className="text-xs text-gray-400">→</span>
                  <span className={`px-3 py-1 text-xs font-bold rounded-md border ${STATUS_MAP[editStatus]?.cls}`}>
                    {STATUS_MAP[editStatus]?.label}
                  </span>
                </div>
              </div>
            </div>

            <div className="p-6 border-t border-gray-200 bg-white flex justify-end gap-3">
              <button onClick={() => setPanelRecord(null)}
                className="px-5 py-2.5 text-sm font-bold text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 transition-colors">
                Hủy
              </button>
              <button onClick={handleSave} disabled={isSaving || editStatus === panelRecord.status}
                className="px-5 py-2.5 text-sm font-bold text-white bg-red-700 rounded-xl hover:bg-red-800 shadow-sm transition-colors disabled:opacity-50 flex items-center gap-2">
                {isSaving && <Loader2 size={14} className="animate-spin" />}
                Xác nhận thay đổi
              </button>
            </div>
          </aside>
        )}
      </div>
    </div>
  );
}