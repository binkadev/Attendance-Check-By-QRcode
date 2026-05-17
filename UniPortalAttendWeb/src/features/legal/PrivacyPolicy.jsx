// MỚI: Nhóm các trang về pháp lý/chính sách
import React from 'react';
import { Shield, ArrowLeft, Lock } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function PrivacyPolicy() {
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
          <div className="flex items-center gap-3 mb-2">
            <div className="p-2 bg-red-50 rounded-lg text-red-600">
              <Lock size={24} />
            </div>
            <h2 className="text-3xl font-bold text-gray-900">Chính sách bảo mật</h2>
          </div>
          <p className="text-sm text-gray-500 mb-8 border-b border-gray-100 pb-6 mt-4">Hiệu lực từ: 16/05/2026</p>

          <div className="space-y-8 text-gray-600 leading-relaxed">
            <p>Sự riêng tư của bạn là ưu tiên hàng đầu tại UniAttend. Chúng tôi cam kết bảo vệ dữ liệu cá nhân của bạn theo các tiêu chuẩn bảo mật khắt khe nhất.</p>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">1. Thông tin chúng tôi thu thập</h3>
              <p>Hệ thống chỉ thu thập các thông tin cần thiết phục vụ cho việc vận hành:</p>
              <ul className="list-disc pl-5 mt-2 space-y-1">
                <li><strong>Thông tin cơ bản:</strong> Họ tên, Email, Mã số định danh.</li>
                <li><strong>Dữ liệu sinh trắc học:</strong> Các đặc trưng khuôn mặt (được mã hóa dưới dạng vector số học) để phục vụ AI nhận diện. Hệ thống <strong>không</strong> lưu trữ ảnh gốc của bạn.</li>
                <li><strong>Dữ liệu thiết bị:</strong> Địa chỉ IP, loại thiết bị và trình duyệt để phát hiện đăng nhập bất thường.</li>
              </ul>
            </section>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">2. Cách chúng tôi sử dụng thông tin</h3>
              <p>Dữ liệu của bạn được sử dụng tuyệt đối cho mục đích giáo dục và quản lý nội bộ:</p>
              <ul className="list-disc pl-5 mt-2 space-y-1">
                <li>Xác thực danh tính khi vào lớp.</li>
                <li>Cung cấp báo cáo chuyên cần cho quản lý nhà trường/giảng viên.</li>
                <li>Cải thiện độ chính xác của thuật toán AI nội bộ.</li>
              </ul>
            </section>

            <section>
              <h3 className="text-lg font-bold text-gray-900 mb-3">3. Cam kết không chia sẻ</h3>
              <p>Chúng tôi cam kết <strong>không bán, cho thuê hoặc trao đổi</strong> thông tin cá nhân hay dữ liệu khuôn mặt của bạn cho bất kỳ bên thứ ba nào vì mục đích quảng cáo hay thương mại. Dữ liệu chỉ được chia sẻ nội bộ với Cơ sở giáo dục mà bạn trực thuộc.</p>
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