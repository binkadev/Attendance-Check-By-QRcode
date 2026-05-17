import React from 'react';
import { 
  Shield, Search, Book, User, Camera, 
  Settings, MessageCircle, ChevronRight, ArrowLeft 
} from 'lucide-react';
import { Link } from 'react-router-dom';

export default function HelpCenter() {
  const categories = [
    { icon: <Book size={24} />, title: 'Bắt đầu sử dụng', desc: 'Hướng dẫn cài đặt và thiết lập lần đầu' },
    { icon: <User size={24} />, title: 'Quản lý tài khoản', desc: 'Đổi mật khẩu, cập nhật thông tin cá nhân' },
    { icon: <Camera size={24} />, title: 'Lỗi nhận diện khuôn mặt', desc: 'Cách khắc phục khi AI không nhận ra bạn' },
    { icon: <Settings size={24} />, title: 'Báo cáo & Thống kê', desc: 'Xem lịch sử chuyên cần và xuất file Excel' }
  ];

  const faqs = [
    "Tôi phải làm gì khi quên mật khẩu?",
    "Hệ thống báo 'Không tìm thấy khuôn mặt hợp lệ'?",
    "Làm sao để đăng ký khuôn mặt mới khi bị thay đổi ngoại hình?",
    "App UniAttend hỗ trợ những nền tảng nào?"
  ];

  return (
    <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
      {/* Header */}
      <header className="w-full h-16 bg-white border-b border-gray-100 flex items-center px-8 lg:px-24 sticky top-0 z-50">
        <Link to="/login" className="flex items-center gap-2 text-gray-500 hover:text-red-600 transition-colors">
          <ArrowLeft size={18} />
          <span className="text-sm font-medium">Trang chủ</span>
        </Link>
        <div className="mx-auto flex items-center gap-2">
          <Shield className="text-red-600" size={24} />
          <h1 className="text-xl font-bold text-gray-900">UniAttend Help</h1>
        </div>
        <div className="w-20"></div> {/* Spacer for centering */}
      </header>

      {/* Hero Search Area */}
      <section className="bg-red-900 py-20 px-4 relative overflow-hidden">
        {/* Background Pattern */}
        <div className="absolute inset-0 opacity-10" style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.4'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4v-2h-4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4v-2H6zM6 4V0H4v4H0v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")`,
            backgroundRepeat: 'repeat'
        }}></div>
        
        <div className="max-w-3xl mx-auto relative z-10 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-8">Xin chào, chúng tôi có thể giúp gì cho bạn?</h2>
          
          <div className="relative max-w-2xl mx-auto group">
            <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
              <Search className="text-gray-400 group-focus-within:text-red-500 transition-colors" size={20} />
            </div>
            <input 
              type="text" 
              className="w-full pl-12 pr-4 py-4 rounded-xl text-gray-900 shadow-lg outline-none focus:ring-4 focus:ring-red-500/30 transition-all text-lg"
              placeholder="Nhập từ khóa cần tìm (VD: quên mật khẩu, điểm danh lỗi...)"
            />
          </div>
        </div>
      </section>

      {/* Main Content Area */}
      <main className="flex-1 max-w-5xl mx-auto w-full px-4 py-12 -mt-10 relative z-20">
        
        {/* Categories Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-16">
          {categories.map((cat, idx) => (
            <div key={idx} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow cursor-pointer group flex items-start gap-4">
              <div className="p-3 bg-gray-50 text-red-600 rounded-xl group-hover:bg-red-50 transition-colors">
                {cat.icon}
              </div>
              <div className="flex-1">
                <h3 className="text-lg font-bold text-gray-900 mb-1 group-hover:text-red-600 transition-colors">{cat.title}</h3>
                <p className="text-sm text-gray-500">{cat.desc}</p>
              </div>
            </div>
          ))}
        </div>

        {/* FAQ Area */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 mb-12">
          <h3 className="text-xl font-bold text-gray-900 mb-6">Câu hỏi thường gặp (FAQ)</h3>
          <div className="space-y-4">
            {faqs.map((faq, idx) => (
              <div key={idx} className="flex items-center justify-between py-4 border-b border-gray-50 cursor-pointer hover:text-red-600 group">
                <span className="font-medium text-gray-700 group-hover:text-red-600 transition-colors">{faq}</span>
                <ChevronRight size={18} className="text-gray-400 group-hover:text-red-600 transition-colors" />
              </div>
            ))}
          </div>
        </div>

        {/* Contact Support */}
        <div className="bg-gradient-to-br from-red-50 to-orange-50 rounded-2xl border border-red-100 p-8 text-center">
          <div className="w-16 h-16 bg-white text-red-600 rounded-full flex items-center justify-center mx-auto mb-4 shadow-sm">
            <MessageCircle size={28} />
          </div>
          <h3 className="text-xl font-bold text-gray-900 mb-2">Vẫn chưa giải quyết được vấn đề?</h3>
          <p className="text-gray-600 mb-6 max-w-md mx-auto">Đội ngũ kỹ thuật của chúng tôi luôn sẵn sàng hỗ trợ bạn 24/7. Vui lòng tạo ticket hoặc chat trực tiếp.</p>
          <button className="px-8 py-3 bg-red-600 hover:bg-red-700 text-white font-medium rounded-lg shadow-md transition-colors">
            Liên hệ hỗ trợ ngay
          </button>
        </div>

      </main>

      <footer className="py-8 bg-white border-t border-gray-100 text-center text-sm text-gray-400 mt-auto">
        <p>© 2026 UniAttend Inc. All rights reserved.</p>
      </footer>
    </div>
  );
}