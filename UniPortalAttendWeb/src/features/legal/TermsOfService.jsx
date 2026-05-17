import React from 'react';
import { Shield, ArrowLeft } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function TermsOfService() {
  return (
    <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
      {/* Header */}
      <header className="w-full h-16 bg-white border-b border-gray-100 flex items-center px-8 lg:px-24 sticky top-0 z-50">
        <Link to="/register" className="flex items-center gap-2 text-gray-500 hover:text-red-600 transition-colors">
          <ArrowLeft size={18} />
          <span className="text-sm font-medium">Quay lại</span>
        </Link>
        <div className="mx-auto flex items-center gap-2">
          <Shield className="text-red-600" size={24} />
          <h1 className="text-xl font-bold text-gray-900">UniAttend</h1>
        </div>
      </header>

      {/* Content */}
      <main className="flex-1 py-12 px-4">
        <div className="max-w-3xl mx-auto bg-white rounded-2xl shadow-sm border border-gray-100 p-8 md:p-12">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Điều khoản dịch vụ</h2>
          <p className="text-sm text-gray-500 mb-8 border-b border-gray-100 pb-6">Cập nhật lần cuối: 16/05/2026</p>

          <div className="space-y-8 text-gray-600 leading-relaxed">
            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">1. Chấp nhận điều khoản</h3>
              <p>Bằng việc truy cập và sử dụng hệ thống UniAttend, bạn đồng ý tuân thủ các điều khoản và điều kiện được quy định dưới đây. Nếu bạn không đồng ý với bất kỳ phần nào của các điều khoản này, vui lòng không sử dụng dịch vụ của chúng tôi.</p>
            </section>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">2. Tài khoản của bạn</h3>
              <ul className="list-disc pl-5 space-y-2">
                <li>Bạn có trách nhiệm bảo mật thông tin tài khoản và mật khẩu của mình.</li>
                <li>Bạn đồng ý cung cấp thông tin chính xác, đầy đủ và cập nhật khi đăng ký tài khoản.</li>
                <li>Hệ thống bảo lưu quyền từ chối dịch vụ, chấm dứt tài khoản hoặc xóa nội dung nếu phát hiện vi phạm.</li>
              </ul>
            </section>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">3. Quy định về điểm danh AI</h3>
              <p>UniAttend sử dụng công nghệ nhận diện khuôn mặt (AI) để xác thực. Bạn đồng ý cấp quyền cho hệ thống sử dụng hình ảnh khuôn mặt của bạn cho mục đích duy nhất là đối chiếu và ghi nhận điểm danh. Các hành vi gian lận (sử dụng ảnh giả, video, thiết bị can thiệp) sẽ bị ghi nhận và báo cáo cho cơ sở giáo dục quản lý.</p>
            </section>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">4. Giới hạn trách nhiệm</h3>
              <p>UniAttend không chịu trách nhiệm đối với bất kỳ thiệt hại gián tiếp, ngẫu nhiên hay hệ quả nào phát sinh từ việc sử dụng hoặc không thể sử dụng dịch vụ, bao gồm nhưng không giới hạn ở việc mất dữ liệu hoặc lỗi kết nối mạng từ phía người dùng.</p>
            </section>
          </div>
        </div>
      </main>

      <footer className="py-6 text-center text-sm text-gray-400">
        © 2026 UniAttend Inc. All rights reserved.
      </footer>
    </div>
  );
}