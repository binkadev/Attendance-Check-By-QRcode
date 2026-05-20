import React, { createContext, useState, useContext, useEffect } from 'react';

const LanguageContext = createContext();

const translations = {
  vi: {
    common: {
      brand: "UniAttend",
      home: "Trang chủ",
      help: "Trung tâm trợ giúp",
      terms: "Điều khoản sử dụng",
      privacy: "Chính sách bảo mật",
      about: "Giới thiệu",
      support: "Hỗ trợ khách hàng",
      contact: "Liên hệ",
      vietnamese: "Tiếng Việt",
      english: "Tiếng Anh",
      copyright: "© 2026 UniAttend Inc. Bảo lưu mọi quyền."
    },
    login: {
      title: "Đăng nhập",
      subtitle: "Quản lý điểm danh",
      highlight: "Thông minh & Bảo mật",
      desc: "Hệ thống điểm danh bằng QR Code động. Sinh viên sử dụng App để quét mã, Giảng viên quản lý toàn diện trên nền tảng Web.",
      realtime: "Real-time",
      multiplatform: "Đa nền tảng",
      antifraud: "Anti-fraud",
      email_placeholder: "Email đăng nhập",
      password_placeholder: "Mật khẩu",
      remember_me: "Ghi nhớ tôi",
      forgot_password: "Quên mật khẩu?",
      login_btn: "Đăng nhập",
      authenticating: "Đang xác thực...",
      sms_login: "SMS",
      app_login: "App",
      register_now: "Đăng ký ngay",
      qr_tab: "Mã QR",
      qr_instruction: "Mở ứng dụng UniAttend để quét mã",
      qr_refresh: "Làm mới mã QR",
      qr_safer: "Sử dụng App để quét, an toàn hơn",
      register_free: "Đăng ký tài khoản miễn phí",
      toast_loading: "Đang xác thực...",
      toast_success: "Đăng nhập thành công!",
      toast_error: "Sai email hoặc mật khẩu!"
    },
    register: {
      title: "Tạo tài khoản mới",
      subtitle: "Đăng ký để sử dụng đầy đủ các tính năng",
      promo_title: "Bắt đầu hành trình",
      promo_highlight: "Giáo dục thông minh",
      promo_desc: "Tạo tài khoản Giảng viên ngay hôm nay để quản lý điểm danh bằng QR Code động. Sinh viên quét mã qua App, tiết kiệm thời gian và nâng cao hiệu quả.",
      fast: "Nhanh chóng",
      secure: "Bảo mật cao",
      easy: "Dễ sử dụng",
      fullName: "Họ và tên",
      userCode: "Mã số sinh viên / Giảng viên",
      email: "Email liên hệ",
      password: "Mật khẩu (ít nhất 8 ký tự)",
      confirmPassword: "Xác nhận lại mật khẩu",
      agree_text: "Tôi đã đọc và đồng ý với",
      terms_link: "Điều khoản dịch vụ",
      and: "và",
      privacy_link: "Chính sách bảo mật",
      register_btn: "Đăng ký ngay",
      creating_account: "Đang tạo tài khoản...",
      have_account: "Đã có tài khoản?",
      login_here: "Đăng nhập tại đây",
      toast_agree_terms: "Vui lòng đồng ý với Điều khoản sử dụng",
      toast_mismatch: "Mật khẩu nhập lại không khớp!",
      toast_min_len: "Mật khẩu phải có ít nhất 8 ký tự!",
      toast_loading: "Đang xử lý đăng ký...",
      toast_success: "Đăng ký thành công! Vui lòng đăng nhập để tiếp tục.",
      toast_error: "Đăng ký thất bại. Vui lòng thử lại.",
      strength_very_weak: "Rất yếu",
      strength_weak: "Yếu",
      strength_medium: "Trung bình",
      strength_strong: "Mạnh"
    },
    about: {
      sparkles_label: "Hành trình Kiến tạo Tương lai Số",
      hero_title: "Tiên Phong Kiến Tạo",
      hero_highlight: "Giảng Đường Số",
      hero_desc: "UniAttend - Thành viên của Tập đoàn Công nghệ UniGroup - tự hào là nền tảng quản lý chuyên cần thông minh, bảo mật hàng đầu Việt Nam. Chúng tôi giải phóng giảng đường khỏi phương thức điểm danh truyền thống, kiến tạo sự minh bạch và công bằng tối đa.",
      discover_mission: "Tìm hiểu sứ mệnh",
      get_started_today: "Bắt đầu ngay hôm nay",
      experience_now: "Trải nghiệm ngay",
      back_to_login: "Quay lại Đăng nhập",
      stats_title: "Những con số ấn tượng",
      stats_attendance: "Lượt điểm danh thành công",
      stats_attendance_sub: "Ghi nhận chính xác & tức thời",
      stats_universities: "Học viện & Đại học tin dùng",
      stats_universities_sub: "Hệ thống phủ sóng toàn quốc",
      stats_users: "Sinh viên & Giảng viên sử dụng",
      stats_users_sub: "Hoạt động sôi nổi mỗi ngày",
      stats_accuracy: "Độ chính xác nhận diện AI",
      stats_accuracy_sub: "Phát hiện gian lận thời gian thực",
      position_title: "Định vị chiến lược",
      position_heading: "Sứ mệnh giải phóng thời gian và bảo vệ công bằng học tập",
      position_desc: "Giáo dục là gốc rễ của sự phát triển quốc gia. Chúng tôi tin rằng bằng cách ứng dụng những công nghệ số tiên tiến nhất như Trí tuệ nhân tạo (AI) và định vị thời gian thực, việc quản lý đào tạo sẽ trở nên gọn nhẹ, thông suốt và chuẩn xác tuyệt đối. Giảng viên tập trung 100% vào chuyên môn giảng dạy, sinh viên tự giác rèn luyện kỷ luật chuyên cần.",
      feature_paperless_title: "Chuyển đổi số 100% không giấy tờ",
      feature_paperless_desc: "Loại bỏ hoàn toàn việc gọi tên thủ công hay tích thẻ giấy truyền thống mất thời gian.",
      feature_fairness_title: "Minh bạch & Công bằng tuyệt đối",
      feature_fairness_desc: "Sinh viên được đánh giá đúng thực lực chuyên cần, nói không với tiêu cực điểm danh hộ.",
      vision_title: "Tầm nhìn chiến lược",
      vision_desc: "Trở thành hệ sinh thái quản lý chuyên cần học đường thông minh và chống gian lận phổ biến nhất Đông Nam Á vào năm 2030. Định hình tiêu chuẩn mới cho hoạt động quản lý giảng dạy thời đại số.",
      mission_title: "Sứ mệnh cao cả",
      mission_desc: "Sử dụng công nghệ để phụng sự nền giáo dục hiện đại. Tự động hóa những tác vụ quản trị lặp đi lặp lại để trả lại thời gian vàng ngọc cho Giảng viên, xây dựng văn hóa tự giác và trung thực cho thế hệ trẻ.",
      philosophy_title: "Triết lý hành động",
      philosophy_heading: "4 Giá Trị Cốt Lõi Định Hình UniAttend",
      philosophy_desc: "Kế thừa tinh thần kỷ luật, quyết liệt và tôn trọng thực tiễn từ mô hình doanh nghiệp quốc gia lớn, chúng tôi lấy bốn giá trị này làm ngọn hải đăng cho mọi quyết định kỹ thuật và kinh doanh.",
      val_1_title: "Thực tiễn là tiêu chuẩn kiểm nghiệm chân lý",
      val_1_desc: "Mọi tính năng, công nghệ của UniAttend đều bắt nguồn từ khó khăn thực tế tại các giảng đường: nạn điểm danh hộ, gian lận vị trí và sự lãng phí thời gian của giảng viên.",
      val_2_title: "Sáng tạo là sức sống của sản phẩm",
      val_2_desc: "Không dừng lại ở công nghệ cơ bản, chúng tôi liên tục đổi mới thuật toán AI phát hiện giả mạo hình ảnh, xác thực thiết bị để bảo vệ sự công bằng tối đa cho sinh viên.",
      val_3_title: "Thích ứng nhanh là sức mạnh cạnh tranh",
      val_3_desc: "Linh hoạt và nhanh chóng đón đầu các xu hướng công nghệ mới, tích hợp mượt mòi vào hệ thống phần mềm quản lý đào tạo hiện có của các nhà trường.",
      val_4_title: "Đồng hành cùng phát triển bền vững",
      val_4_desc: "Chúng tôi coi thành công trong việc chuyển đổi số học đường của mỗi Nhà trường và sự hào hứng của mỗi sinh viên là thước đo thành tựu lớn nhất của mình.",
      timeline_subtitle: "Hành trình lịch sử",
      timeline_heading: "Chặng Đường Phát Triển",
      timeline_desc: "Từ những ngày đầu nghiên cứu thuật toán chống gian lận trong phòng lab cho tới cột mốc kết nối hàng trăm nghìn giảng viên, sinh viên trên toàn lãnh thổ.",
      timeline_y2023_title: "Khởi nghiệp & Nghiên cứu lõi",
      timeline_y2023_desc: "Thành lập ban dự án thuộc Tập đoàn Công nghệ UniGroup. Tập trung nghiên cứu giải pháp điểm danh thông minh chống gian lận định vị GPS và phát triển những thuật toán cốt lõi đầu tiên.",
      timeline_y2024_title: "Phát hành QR Code Động",
      timeline_y2024_desc: "Ra mắt giải pháp QR Code tự động thay đổi tần số (Dynamic QR) mã hóa thời gian thực, ngăn chặn triệt để hành vi chụp ảnh chia sẻ mã điểm danh từ xa.",
      timeline_y2025_title: "Tích hợp Nhận dạng AI & Phủ sóng",
      timeline_y2025_desc: "Đột phá công nghệ với lõi AI nhận diện khuôn mặt sinh viên và phát hiện gian lận thiết bị (MAC Address duplication). Đạt cột mốc 100+ trường Đại học trên toàn quốc tin dùng.",
      timeline_y2026_title: "Hệ sinh thái số hóa & Blockchain",
      timeline_y2026_desc: "Nghiên cứu ứng dụng công nghệ Blockchain để bảo chứng độ tin cậy của bảng điểm chuyên cần. Hướng tới tích hợp toàn diện hệ thống LMS quốc tế và mở rộng ra thị trường Đông Nam Á.",
      tech_subtitle: "Hệ sinh thái UniAttend",
      tech_heading: "Nền Tảng Công Nghệ Đột Phá",
      tech_desc: "Sự kết hợp hoàn hảo giữa thiết bị di động cá nhân, công nghệ Cloud thời gian thực và lõi trí tuệ nhân tạo (AI) bảo mật vượt trội.",
      tech_p1_title: "UniAttend Web Portal",
      tech_p1_desc: "Dành cho Giảng viên & Ban giám hiệu nhà trường. Giao diện trực quan thống kê danh sách chuyên cần, thiết lập cấu hình lớp học, kiểm soát và phát hiện tức thì các dấu hiệu gian lận.",
      tech_p1_bullet1: "Thống kê báo cáo chuyên cần tự động",
      tech_p1_bullet2: "Bảng điều khiển giám sát Real-time",
      tech_p1_bullet3: "Tích hợp nhanh với LMS Canvas / Moodle",
      tech_p2_title: "UniAttend Student App",
      tech_p2_desc: "Ứng dụng di động cực kỳ mượt mà cho sinh viên. Thực hiện quét mã QR điểm danh chỉ mất 2 giây, theo dõi lịch sử chuyên cần cá nhân và gửi đơn xin nghỉ học trực tiếp.",
      tech_p2_bullet1: "Quét mã QR code siêu tốc 2 giây",
      tech_p2_bullet2: "Gửi đơn xin nghỉ phép tích hợp chữ ký",
      tech_p2_bullet3: "Widget đếm ngược và thông báo đẩy",
      tech_p3_title: "Lõi AI Anti-Fraud Engine",
      tech_p3_desc: "Bộ não bảo mật tối tân của hệ thống. Phân tích chéo tọa độ GPS của lớp học và thiết bị, so sánh trùng lặp MAC Address, và xác thực sinh trắc học gương mặt chống điểm danh hộ.",
      tech_p3_bullet1: "So khớp định vị GPS đa chiều",
      tech_p3_bullet2: "Phát hiện trùng lặp phần cứng thiết bị",
      tech_p3_bullet3: "AI Face Verification bảo mật sinh trắc",
      cta_subtitle: "Đồng hành cùng hàng trăm trường học Việt",
      cta_heading: "Sẵn Sàng Kiến Tạo Giảng Đường Số Của Bạn?",
      cta_desc: "Hãy gia nhập cộng đồng hơn 200 học viện, trường đại học hàng đầu Việt Nam đang cách mạng hóa mô hình quản lý chuyên cần và nâng cao ý thức tự giác học tập cho thế hệ tương lai.",
      cta_login_btn: "Đăng nhập ngay",
      cta_register_btn: "Đăng ký tài khoản Giảng viên",
      footer_unigroup_desc: "Thành viên của Tập đoàn Công nghệ UniGroup. Tiên phong giải pháp quản lý chuyên cần thông minh ứng dụng Trí tuệ nhân tạo (AI) bảo mật cao hàng đầu khu vực.",
      footer_section_legal: "Thông tin Pháp lý",
      footer_section_products: "Sản phẩm & Dịch vụ",
      footer_section_contact: "Liên hệ Tập đoàn",
      footer_profile_doc: "Hồ sơ năng lực công ty",
      footer_p_qr: "Điểm danh QR Code động",
      footer_p_biometric: "Lõi sinh trắc học AI Face ID",
      footer_p_smart: "Quản lý chuyên cần thông minh",
      footer_p_api: "Tích hợp API Đào tạo",
      footer_c_addr: "Tòa nhà công nghệ UniGroup, Khu công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội.",
      footer_c_phone: "Hotline: 1900 6868 (24/7)",
      footer_c_email: "Email: contact@unigroup.vn"
    }
  },
  en: {
    common: {
      brand: "UniAttend",
      home: "Home",
      help: "Help Center",
      terms: "Terms of Service",
      privacy: "Privacy Policy",
      about: "About Us",
      support: "Customer Support",
      contact: "Contact",
      vietnamese: "Vietnamese",
      english: "English",
      copyright: "© 2026 UniAttend Inc. All rights reserved."
    },
    login: {
      title: "Login",
      subtitle: "Smart & Secure",
      highlight: "Attendance Portal",
      desc: "Dynamic QR Code attendance system. Students use the Mobile App to scan codes, and Lecturers manage comprehensively on the Web Portal.",
      realtime: "Real-time",
      multiplatform: "Multi-platform",
      antifraud: "Anti-fraud",
      email_placeholder: "Username Email",
      password_placeholder: "Password",
      remember_me: "Remember me",
      forgot_password: "Forgot password?",
      login_btn: "Sign In",
      authenticating: "Authenticating...",
      sms_login: "SMS",
      app_login: "App",
      register_now: "Register Now",
      qr_tab: "QR Code",
      qr_instruction: "Open UniAttend App to scan QR Code",
      qr_refresh: "Refresh QR Code",
      qr_safer: "Use App to scan, much more secure",
      register_free: "Create a free account",
      toast_loading: "Authenticating...",
      toast_success: "Log in successfully!",
      toast_error: "Invalid email or password!"
    },
    register: {
      title: "Create New Account",
      subtitle: "Register to unlock all features",
      promo_title: "Start Your Smart",
      promo_highlight: "Education Journey",
      promo_desc: "Create a Lecturer account today to manage attendance via dynamic QR codes. Students scan via the Mobile App, saving time and increasing efficiency.",
      fast: "Fast & Precise",
      secure: "High Security",
      easy: "Easy to Use",
      fullName: "Full Name",
      userCode: "Student / Lecturer Code",
      email: "Contact Email",
      password: "Password (at least 8 characters)",
      confirmPassword: "Re-type password",
      agree_text: "I have read and agree to the",
      terms_link: "Terms of Service",
      and: "and",
      privacy_link: "Privacy Policy",
      register_btn: "Create Account",
      creating_account: "Creating account...",
      have_account: "Already have an account?",
      login_here: "Log in here",
      toast_agree_terms: "Please agree to the Terms of Service",
      toast_mismatch: "Confirm password does not match!",
      toast_min_len: "Password must contain at least 8 characters!",
      toast_loading: "Processing registration...",
      toast_success: "Register successfully! Please log in to continue.",
      toast_error: "Registration failed. Please try again.",
      strength_very_weak: "Very Weak",
      strength_weak: "Weak",
      strength_medium: "Medium",
      strength_strong: "Strong"
    },
    about: {
      sparkles_label: "A Journey Shaping the Digital Future",
      hero_title: "Pioneering the Smart",
      hero_highlight: "Digital Classroom",
      hero_desc: "UniAttend - A member of UniGroup Technology Corporation - is proud to be the leading smart and secure attendance management platform in Vietnam. We liberate classrooms from traditional rolls, creating absolute transparency and fairness.",
      discover_mission: "Discover Our Mission",
      get_started_today: "Get Started Today",
      experience_now: "Experience Now",
      back_to_login: "Back to Login",
      stats_title: "Impressive Numbers",
      stats_attendance: "Successful Attendances",
      stats_attendance_sub: "Precise & real-time record",
      stats_universities: "Universities & Colleges",
      stats_universities_sub: "National scale network coverage",
      stats_users: "Active Daily Users",
      stats_users_sub: "Vibrant interaction every day",
      stats_accuracy: "AI Detection Uptime Accuracy",
      stats_accuracy_sub: "Real-time double identity fraud check",
      position_title: "Strategic Position",
      position_heading: "The mission to free up time and protect educational equity",
      position_desc: "Education is the root of national development. We believe that by applying the most advanced digital technologies such as Artificial Intelligence (AI) and real-time positioning, education management will become light, transparent, and absolutely accurate. Lecturers focus 100% on professional teaching, and students voluntarily practice professional discipline.",
      feature_paperless_title: "100% Paperless Digital Transition",
      feature_paperless_desc: "Completely eliminate manual role calling or time-consuming traditional paper checklists.",
      feature_fairness_title: "Absolute Uptime & Fairness",
      feature_fairness_desc: "Students are evaluated exactly on their real class attendance, leaving no room for roll fraud.",
      vision_title: "Strategic Vision",
      vision_desc: "To become the most popular smart classroom attendance and anti-fraud ecosystem in Southeast Asia by 2030. Shaping the new standard for digital-era educational administration.",
      mission_title: "High Mission",
      mission_desc: "Use technology to serve modern education. Automate repetitive administrative tasks to return golden time to Lecturers, building a culture of self-discipline and honesty for the youth.",
      philosophy_title: "Action Philosophy",
      philosophy_heading: "4 Core Values Shaping UniAttend",
      philosophy_desc: "Inheriting the spirit of discipline, determination, and respect for practical facts from major national technology corporations, we adopt these four values as the beacon for our business.",
      val_1_title: "Practice is the Test of Truth",
      val_1_desc: "Every feature and tech aspect of UniAttend starts from actual classroom struggles: attendance proxy, GPS spoofing, and the massive waste of teaching time.",
      val_2_title: "Creativity is the Lifeblood of Product",
      val_2_desc: "Never stopping at baseline tech, we continuously innovate AI algorithms for spoof detection, biometric validation, and hardware duplicate checking.",
      val_3_title: "Rapid Adaptability is Competitive Strength",
      val_3_desc: "Highly flexible in adopting fresh technological waves, integrating seamlessly into existing university information management databases and major LMS systems.",
      val_4_title: "Growing Together Sustainedly",
      val_4_desc: "We measure our ultimate achievement by the digital transformation success of each partner University and the academic integrity of every single student.",
      timeline_subtitle: "Historical Milestone",
      timeline_heading: "Development Journey",
      timeline_desc: "From the early lab days researching anti-fraud GPS algorithms to the milestone of connecting hundreds of thousands of lecturers and students nationwide.",
      timeline_y2023_title: "Startup & Core R&D",
      timeline_y2023_desc: "Established the project board under UniGroup Tech Corp. Focused on core research for smart positioning, high-accuracy GPS triangulation, and initial database algorithms.",
      timeline_y2024_title: "Dynamic QR Code Launch",
      timeline_y2024_desc: "Released our flagship real-time frequency-changing Dynamic QR Code, completely blocking remote snapshot sharing and off-site roll checking.",
      timeline_y2025_title: "AI Recognition Integration",
      timeline_y2025_desc: "Tech breakthrough by embedding biometric face verification and hardware duplicate detection (MAC address checks). Passed 100+ partner universities milestone.",
      timeline_y2026_title: "Digital Ecosystem & Blockchain",
      timeline_y2026_desc: "Researching decentralized ledger implementations (Web3) for immutable attendance grade cards. Targeting international LMS systems and Southeast Asia expansion.",
      tech_subtitle: "UniAttend Ecosystem",
      tech_heading: "Breakthrough Technological Pillars",
      tech_desc: "The perfect combination of personal mobile devices, real-time Cloud infrastructure, and state-of-the-art Artificial Intelligence (AI).",
      tech_p1_title: "UniAttend Web Portal",
      tech_p1_desc: "Tailored for Lecturers & University Admins. An intuitive interface supplying automated analytics, custom classroom policies, and instantaneous fraud notifications.",
      tech_p1_bullet1: "Automated student attendance logs",
      tech_p1_bullet2: "Real-time class tracking dashboard",
      tech_p1_bullet3: "Seamless Canvas & Moodle LMS sync",
      tech_p2_title: "UniAttend Student App",
      tech_p2_desc: "Vibrant, lightweight mobile application for students. Complete an attendance scan in under 2 seconds, check personal academic stats, and submit leave requests directly.",
      tech_p2_bullet1: "Ultra-fast 2-second QR code scan",
      tech_p2_bullet2: "Submit signed absence leave forms",
      tech_p2_bullet3: "Uptime live countdown widget & alerts",
      tech_p3_title: "AI Anti-Fraud Core",
      tech_p3_desc: "The security brain of our ecosystem. Performs multi-dimensional GPS distance checks, identifies device hardware duplications, and processes AI facial recognition validations.",
      tech_p3_bullet1: "Smart GPS triangulation audits",
      tech_p3_bullet2: "Hardware & MAC address duplicate check",
      tech_p3_bullet3: "AI Face ID biometric authorization",
      cta_subtitle: "Trusted by hundreds of leading educational units",
      cta_heading: "Ready to Revolutionize Your Classrooms?",
      cta_desc: "Join over 200 academies and universities in Vietnam that are digitalizing their campuses, reinforcing classroom integrity, and saving teaching time.",
      cta_login_btn: "Sign In Now",
      cta_register_btn: "Register Lecturer Account",
      footer_unigroup_desc: "A member of UniGroup Tech Corp. Pioneering secure, AI-powered smart attendance solutions across modern educational systems.",
      footer_section_legal: "Legal Info",
      footer_section_products: "Products & Tech",
      footer_section_contact: "Group Headquarters",
      footer_profile_doc: "Company Portfolio Profile",
      footer_p_qr: "Dynamic QR Attendance",
      footer_p_biometric: "AI Face ID Biometrics",
      footer_p_smart: "Smart Absence Management",
      footer_p_api: "Educational Database APIs",
      footer_c_addr: "UniGroup Corporate Tower, Hoa Lac High-Tech Park, Hanoi, Vietnam.",
      footer_c_phone: "Hotline: 1900 6868 (24/7)",
      footer_c_email: "Email: contact@unigroup.vn"
    }
  }
};

export function LanguageProvider({ children }) {
  const [language, setLanguageState] = useState(() => {
    return localStorage.getItem('app_language') || 'vi';
  });

  const setLanguage = (lang) => {
    if (lang === 'vi' || lang === 'en') {
      setLanguageState(lang);
      localStorage.setItem('app_language', lang);
    }
  };

  const toggleLanguage = () => {
    setLanguage(language === 'vi' ? 'en' : 'vi');
  };

  // Helper function to resolve dot-notation strings
  const t = (path) => {
    const keys = path.split('.');
    let current = translations[language];
    for (const key of keys) {
      if (current && current[key] !== undefined) {
        current = current[key];
      } else {
        // Fallback: If not found in English, try Vietnamese, then return key path
        let viFallback = translations['vi'];
        for (const k of keys) {
          if (viFallback && viFallback[k] !== undefined) {
            viFallback = viFallback[k];
          } else {
            viFallback = null;
            break;
          }
        }
        return viFallback || path;
      }
    }
    return current;
  };

  return (
    <LanguageContext.Provider value={{ language, setLanguage, toggleLanguage, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  const context = useContext(LanguageContext);
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider');
  }
  return context;
}
