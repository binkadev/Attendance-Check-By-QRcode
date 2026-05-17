import React, { useState } from 'react';
import { 
  Search, ChevronDown, Calendar, Users as UsersIcon, 
  AlertCircle, X, UploadCloud, QrCode, UserSquare2 
} from 'lucide-react';

import Sidebar from '../../../components/layout/Sidebar';

// --- DỮ LIỆU MẪU ---
const MOCK_ATTENDANCE_LOGS = [
  {
    id: 'rec-1',
    student: { name: 'Nguyễn Văn Mạnh', id: 'STU-1102', avatar: 'https://i.pravatar.cc/150?img=33' },
    session: { classCode: 'CS204: Cấu trúc dữ liệu', date: '24 Th10, 2023 • 10:00 SA' },
    status: 'Có mặt',
    method: 'QR Động',
    isFlagged: true,
    history: [
      { time: '24 Th10, 2023 lúc 10:05 SA', action: 'Cảnh báo hệ thống: Sai lệch vị trí', type: 'flag' },
      { time: '24 Th10, 2023 lúc 10:00 SA', action: 'Quét mã lần đầu (QR Động)', type: 'info' }
    ]
  },
  {
    id: 'rec-2',
    student: { name: 'Lê Thị Hồng', id: 'STU-4491', avatar: 'https://i.pravatar.cc/150?img=47' },
    session: { classCode: 'CS204: Cấu trúc dữ liệu', date: '24 Th10, 2023 • 10:00 SA' },
    status: 'Có mặt',
    method: 'QR Động',
    isFlagged: false,
  },
  {
    id: 'rec-3',
    student: { name: 'Trần Đăng Khoa', id: 'STU-7721', avatar: 'https://i.pravatar.cc/150?img=12' },
    session: { classCode: 'CS301: Thuật toán', date: '23 Th10, 2023 • 14:00 CH' },
    status: 'Vắng mặt',
    method: 'Hệ thống ghi nhận',
    isFlagged: false,
  },
  {
    id: 'rec-4',
    student: { name: 'Phạm Minh Anh', id: 'STU-3382', avatar: 'https://i.pravatar.cc/150?img=5' },
    session: { classCode: 'SE101: Kỹ thuật phần mềm', date: '22 Th10, 2023 • 09:00 SA' },
    status: 'Có phép',
    method: 'Chỉnh sửa thủ công',
    isFlagged: false,
  }
];

export default function HistoryAndManualEdit() {
  const [selectedRecord, setSelectedRecord] = useState(MOCK_ATTENDANCE_LOGS[0]);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(() => {
    return localStorage.getItem('sidebar_collapsed') === 'true';
  });

  const handleRowClick = (record) => {
    setSelectedRecord(record);
    setIsPanelOpen(true);
  };

  return (
    <div className="flex min-h-screen bg-gray-50 font-sans text-gray-800">
      
      <Sidebar onCollapseChange={setIsSidebarCollapsed} />

      <div className={`flex-1 flex h-screen overflow-hidden transition-all duration-300 ease-in-out ${isSidebarCollapsed ? 'ml-[80px]' : 'ml-64'}`}>
        
        <main className="flex-1 flex flex-col h-full overflow-y-auto relative">
          <header className="px-8 py-6 bg-white border-b border-gray-200 sticky top-0 z-10">
            <h2 className="text-2xl font-bold text-gray-900">Lịch sử điểm danh & Chỉnh sửa</h2>
          </header>

          <div className="p-8 w-full flex-1">
            {/* Bộ lọc */}
            <div className="flex items-center gap-4 mb-8">
              <div className="relative flex-1 max-w-md">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input 
                  type="text" 
                  placeholder="Tìm sinh viên hoặc MSSV..." 
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500 text-sm bg-white"
                />
              </div>
              <div className="relative w-48">
                <select className="w-full pl-4 pr-10 py-2.5 border border-gray-200 rounded-lg appearance-none text-sm focus:outline-none focus:ring-2 focus:ring-red-500 bg-white">
                  <option>Tất cả lớp học</option>
                  <option>CS204</option>
                  <option>CS301</option>
                </select>
                <ChevronDown className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" size={18} />
              </div>
              <div className="relative w-64">
                <Calendar className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input 
                  type="text" 
                  value="01 Th10 - 31 Th10, 2023" 
                  readOnly
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-200 rounded-lg text-sm cursor-pointer bg-white"
                />
              </div>
            </div>

            {/* Thẻ thống kê */}
            <div className="grid grid-cols-2 gap-6 mb-8">
              <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm flex flex-col justify-between">
                <div className="flex justify-between items-start">
                  <p className="text-gray-500 font-medium">Tổng số bản ghi</p>
                  <UsersIcon className="text-gray-400" size={20} />
                </div>
                <div className="mt-4">
                  <h3 className="text-4xl font-bold text-gray-900">4,821</h3>
                  <p className="text-sm text-gray-500 mt-1">Trong khoảng thời gian đã chọn</p>
                </div>
              </div>

              <div className="bg-red-50 p-6 rounded-2xl border border-red-100 shadow-sm flex flex-col justify-between relative overflow-hidden">
                <div className="flex justify-between items-start">
                  <p className="text-red-800 font-bold">Trường hợp nghi vấn</p>
                  <AlertCircle className="text-red-500" size={20} />
                </div>
                <div className="mt-4 flex items-baseline gap-3">
                  <h3 className="text-4xl font-bold text-red-600">14</h3>
                  <span className="text-xs font-bold bg-red-100 text-red-600 px-2 py-0.5 rounded-full">+2 tuần này</span>
                </div>
                <p className="text-sm text-red-700 mt-1">Cần xem xét thủ công</p>
              </div>
            </div>

            {/* Bảng điểm danh */}
            <div className="bg-white border border-gray-200 rounded-2xl shadow-sm overflow-hidden">
              <div className="px-6 py-5 border-b border-gray-200 bg-white">
                <h3 className="font-bold text-lg text-gray-900">Nhật ký điểm danh</h3>
              </div>
              <table className="w-full text-left text-sm">
                <thead className="bg-gray-50/80 text-gray-500 font-bold uppercase text-xs">
                  <tr>
                    <th className="px-6 py-4 border-b border-gray-100">SINH VIÊN</th>
                    <th className="px-6 py-4 border-b border-gray-100">PHIÊN HỌC</th>
                    <th className="px-6 py-4 border-b border-gray-100">TRẠNG THÁI & PHƯƠNG THỨC</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 bg-white">
                  {MOCK_ATTENDANCE_LOGS.map((record) => (
                    <tr 
                      key={record.id} 
                      onClick={() => handleRowClick(record)}
                      className={`cursor-pointer transition ${record.isFlagged ? 'bg-red-50/40 hover:bg-red-50' : 'hover:bg-gray-50'} ${selectedRecord?.id === record.id && isPanelOpen ? 'bg-gray-50 ring-1 ring-inset ring-gray-200' : ''}`}
                    >
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <img src={record.student.avatar} alt={record.student.name} className="w-10 h-10 rounded-full" />
                          <div>
                            <p className="font-bold text-gray-900">{record.student.name}</p>
                            <p className="text-xs text-gray-500">{record.student.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        <p className="font-bold text-gray-900">{record.session.classCode}</p>
                        <p className="text-xs text-gray-500 mt-0.5">{record.session.date}</p>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex flex-col items-start gap-1">
                          <span className={`px-2.5 py-1 text-xs font-bold rounded-md border ${
                            record.status === 'Có mặt' ? 'bg-gray-100 text-gray-700 border-gray-200' :
                            record.status === 'Vắng mặt' ? 'bg-red-100 text-red-700 border-red-200' :
                            'bg-blue-100 text-blue-700 border-blue-200'
                          }`}>
                            {record.status}
                          </span>
                          <div className="flex items-center gap-1 text-xs text-gray-500 mt-1 font-medium">
                            {record.method.includes('QR') ? <QrCode size={12} /> : <UserSquare2 size={12} />}
                            {record.method}
                          </div>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              
              {/* Phân trang */}
              <div className="px-6 py-4 border-t border-gray-200 flex items-center justify-between bg-gray-50/50">
                <button className="px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-500 disabled:opacity-50" disabled>Trước</button>
                <div className="flex items-center gap-1">
                  <button className="w-8 h-8 rounded-lg bg-red-50 text-red-600 font-bold">1</button>
                  <button className="w-8 h-8 rounded-lg text-gray-600 hover:bg-gray-100 font-bold">2</button>
                  <button className="w-8 h-8 rounded-lg text-gray-600 hover:bg-gray-100 font-bold">3</button>
                  <span className="px-2 text-gray-400">...</span>
                </div>
                <button className="px-4 py-2 border border-gray-200 rounded-lg text-sm font-bold text-gray-600 hover:bg-white shadow-sm">Sau</button>
              </div>
            </div>
          </div>
        </main>

        {/* BẢNG ĐIỀU KHIỂN BÊN PHẢI (Chỉnh sửa thủ công) */}
        {isPanelOpen && selectedRecord && (
          <aside className="w-[420px] bg-white border-l border-gray-200 shadow-2xl flex flex-col h-full shrink-0 sticky top-0">
            <div className="flex items-center justify-between px-6 py-5 border-b border-gray-200 bg-gray-50/50">
              <h3 className="font-bold text-lg text-gray-900">Chỉnh sửa thủ công</h3>
              <button onClick={() => setIsPanelOpen(false)} className="text-gray-400 hover:text-gray-900 bg-white p-1 rounded-md shadow-sm border border-gray-200 transition-colors">
                <X size={18} />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 space-y-8">
              
              {/* Thẻ thông tin sinh viên */}
              <div className="flex items-center gap-4 p-4 bg-gray-50 border border-gray-200 rounded-xl">
                <div className="relative">
                  <img src={selectedRecord.student.avatar} alt="Student" className="w-12 h-12 rounded-full ring-2 ring-white" />
                  {selectedRecord.isFlagged && (
                    <span className="absolute -bottom-1 -right-1 w-4 h-4 bg-red-500 border-2 border-white rounded-full"></span>
                  )}
                </div>
                <div>
                  <h4 className="font-bold text-gray-900">{selectedRecord.student.name}</h4>
                  <p className="text-xs font-medium text-gray-500">{selectedRecord.student.id} • {selectedRecord.session.classCode.split(':')[0]}</p>
                </div>
              </div>

              {/* Tùy chọn cập nhật trạng thái */}
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Cập nhật trạng thái</label>
                <div className="grid grid-cols-2 gap-3">
                  {['Có mặt', 'Muộn', 'Vắng mặt', 'Có phép'].map((statusOption) => (
                    <label key={statusOption} className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer border-2 transition-all ${
                      selectedRecord.status === statusOption 
                        ? 'border-red-500 bg-red-50/50' 
                        : 'border-transparent bg-gray-50 hover:bg-gray-100'
                    }`}>
                      <input 
                        type="radio" 
                        name="status" 
                        defaultChecked={selectedRecord.status === statusOption} 
                        className="accent-red-600 w-4 h-4" 
                      />
                      <span className={`font-bold text-sm ${selectedRecord.status === statusOption ? 'text-gray-900' : 'text-gray-600'}`}>
                        {statusOption}
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Lý do chỉnh sửa */}
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Lý do chỉnh sửa (Nhật ký hệ thống)</label>
                <textarea 
                  rows="3" 
                  placeholder="Giải thích lý do thay đổi bản ghi này..."
                  className="w-full p-3 border border-gray-300 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-red-500 resize-none bg-white shadow-sm"
                ></textarea>
              </div>

              {/* Khu vực tải lên */}
              <div>
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">Đính kèm minh chứng (Không bắt buộc)</label>
                <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 flex flex-col items-center justify-center text-center cursor-pointer hover:bg-gray-50 hover:border-gray-400 transition-colors">
                  <UploadCloud className="text-gray-400 mb-2" size={28} />
                  <p className="text-sm font-bold text-gray-700">Nhấn để tải lên hoặc kéo thả tệp</p>
                  <p className="text-xs text-gray-500 mt-1">PDF, JPG, PNG (tối đa 5MB)</p>
                </div>
              </div>

              {/* Lịch sử thay đổi */}
              {selectedRecord.history && (
                <div>
                  <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-4">Lịch sử thay đổi</label>
                  <div className="relative pl-4 space-y-5 before:absolute before:inset-y-0 before:left-[7px] before:w-0.5 before:bg-gray-200">
                    {selectedRecord.history.map((log, index) => (
                      <div key={index} className="relative">
                        <div className={`absolute -left-[21px] top-1 w-2.5 h-2.5 rounded-full ring-4 ring-white ${log.type === 'flag' ? 'bg-red-500' : 'bg-gray-300'}`}></div>
                        <p className={`text-sm font-bold ${log.type === 'flag' ? 'text-gray-900' : 'text-gray-600'}`}>{log.action}</p>
                        <p className="text-xs text-gray-400 mt-0.5">{log.time}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

            </div>

            {/* Các nút cuối trang */}
            <div className="p-6 border-t border-gray-200 bg-white flex justify-end gap-3 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
              <button 
                onClick={() => setIsPanelOpen(false)}
                className="px-5 py-2.5 text-sm font-bold text-gray-700 bg-white border border-gray-300 rounded-xl hover:bg-gray-50 transition-colors"
              >
                Hủy
              </button>
              <button className="px-5 py-2.5 text-sm font-bold text-white bg-[#b91c1c] rounded-xl hover:bg-red-800 shadow-sm transition-colors">
                Xác nhận thay đổi
              </button>
            </div>
          </aside>
        )}
      </div>

    </div>
  );
}