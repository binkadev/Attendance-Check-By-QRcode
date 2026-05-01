//package com.androidapp.attendencecheckqrcode.ui.clazz;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.TextView;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.androidapp.attendencecheckqrcode.R;
//import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
//import com.androidapp.attendencecheckqrcode.ui.qr.CreateQRActivity;
//import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;
//
//public class ClassDetailActivity extends AppCompatActivity {
//
//    private TextView tvClassName, tvClassCode, tvLecturer, tvRoom, tvTime, tvDayDate;
//    private TextView tvSubjectCode, tvStudentCount, tvSemester;
//    private android.widget.Button btnScanQR, btnCreateQR;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_class_detail);
//        if (getSupportActionBar() != null) getSupportActionBar().hide();
//
//        initViews();
//
//        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
//
//        Intent intent = getIntent();
//        if (intent != null && intent.hasExtra("classData")) {
//            Classroom classroom = (Classroom) intent.getSerializableExtra("classData");
//            if (classroom != null) {
//                bindData(classroom);
//                setupRoleLogic(classroom);
//            }
//        }
//    }
//
//    private void initViews() {
//        tvSubjectCode = findViewById(R.id.tvSubjectCode);
//        tvStudentCount = findViewById(R.id.tvStudentCount);
//        tvClassName = findViewById(R.id.tvClassName);
//        tvLecturer = findViewById(R.id.tvLecturer);
//
//        tvClassCode = findViewById(R.id.tvClassCode);
//        tvSemester = findViewById(R.id.tvSemester);
//        tvTime = findViewById(R.id.tvTime);
//        tvRoom = findViewById(R.id.tvRoom);
//        tvDayDate = findViewById(R.id.tvDayDate);
//
//        btnScanQR = findViewById(R.id.btnScanQR);
//        btnCreateQR = findViewById(R.id.btnCreateQR);
//    }
//
//    private void bindData(Classroom classroom) {
//        tvSubjectCode.setText(classroom.getCourseCode() != null ? classroom.getCourseCode() : "N/A");
//        tvClassCode.setText(classroom.getClassCode() != null ? classroom.getClassCode() : "N/A");
//        tvStudentCount.setText("● " + classroom.getApprovedStudentCount() + " Sinh viên");
//        tvClassName.setText(classroom.getGroupName() != null ? classroom.getGroupName() : "Chưa có tên");
//        tvLecturer.setText(classroom.getLecturerName() != null ? classroom.getLecturerName() : "Chưa phân công");
//
//        String semester = classroom.getSemester() != null ? classroom.getSemester() : "N/A";
//        String year = classroom.getAcademicYear() != null ? classroom.getAcademicYear() : "N/A";
//        tvSemester.setText("HK" + semester + ", " + year);
//
//        String room = classroom.getRoom() != null ? classroom.getRoom() : "Chưa có phòng";
//        String campus = classroom.getLocationDisplay() != null ? classroom.getLocationDisplay() : "";
//        tvRoom.setText(room + (campus.isEmpty() ? "" : " - " + campus));
//
//        displayTime(classroom);
//    }
//
//    private void displayTime(Classroom classroom) {
//        // 1. Ưu tiên dữ liệu từ mảng Lịch học (Nếu đã gọi API Detail thành công)
//        if (classroom.getWeeklySchedules() != null && !classroom.getWeeklySchedules().isEmpty()) {
//            Classroom.WeeklySchedule schedule = classroom.getWeeklySchedules().get(0);
//            tvDayDate.setText(translateDay(schedule.getDayOfWeek()));
//
//            String start = schedule.getStartTime();
//            String end = schedule.getEndTime();
//            if (start != null && end != null) {
//                if (start.length() >= 5) start = start.substring(0, 5);
//                if (end.length() >= 5) end = end.substring(0, 5);
//                tvTime.setText(start + " - " + end);
//            } else {
//                tvTime.setText("Chưa xếp giờ");
//            }
//        }
//        // 2. Dự phòng: Dùng dữ liệu ISO từ màn hình danh sách truyền sang
//        else if (classroom.getStartTime() != null && !classroom.getStartTime().isEmpty()) {
//            tvDayDate.setText(extractDayOfWeek(classroom.getStartTime()));
//            String start = extractTime(classroom.getStartTime());
//            String end = extractTime(classroom.getEndTime());
//            tvTime.setText(end.isEmpty() ? start : start + " - " + end);
//        }
//        else {
//            tvDayDate.setText("Chưa xếp lịch");
//            tvTime.setText("--:--");
//        }
//    }
//
//    private String extractDayOfWeek(String isoDateString) {
//        try {
//            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
//            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
//            java.util.Date date = sdf.parse(isoDateString);
//            java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", new java.util.Locale("vi", "VN"));
//            String dayName = dayFormat.format(date);
//            return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
//        } catch (Exception e) { return "N/A"; }
//    }
//
//    private String extractTime(String isoDateString) {
//        if (isoDateString == null || isoDateString.isEmpty()) return "";
//        try {
//            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
//            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
//            java.util.Date date = sdf.parse(isoDateString);
//            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
//            return timeFormat.format(date);
//        } catch (Exception e) { return ""; }
//    }
//
//    private String translateDay(String englishDay) {
//        if (englishDay == null) return "N/A";
//        switch (englishDay.toUpperCase()) {
//            case "MONDAY": return "Thứ 2";
//            case "TUESDAY": return "Thứ 3";
//            case "WEDNESDAY": return "Thứ 4";
//            case "THURSDAY": return "Thứ 5";
//            case "FRIDAY": return "Thứ 6";
//            case "SATURDAY": return "Thứ 7";
//            case "SUNDAY": return "Chủ Nhật";
//            default: return englishDay;
//        }
//    }
//
//    private void setupRoleLogic(Classroom classroom) {
//        if (btnCreateQR != null && btnScanQR != null) {
//            if ("LECTURER".equals(classroom.getMyRole())) {
//                btnCreateQR.setVisibility(View.VISIBLE);
//                btnScanQR.setVisibility(View.GONE);
//
//                TextView tvActionTitle = findViewById(R.id.tvActionTitle);
//                if (tvActionTitle != null) tvActionTitle.setText("Quản lý điểm danh");
//
//                btnCreateQR.setOnClickListener(v -> {
//                    Intent i = new Intent(this, CreateQRActivity.class);
//                    i.putExtra("classData", classroom);
//                    startActivity(i);
//                });
//            } else {
//                btnCreateQR.setVisibility(View.GONE);
//                btnScanQR.setVisibility(View.VISIBLE);
//
//                btnScanQR.setOnClickListener(v -> {
//                    startActivity(new Intent(this, QRScanActivity.class));
//                });
//            }
//        }
//    }
//}


package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.data.dto.group.PolicyStatusResponse;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.ui.qr.CreateQRActivity;
import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;
import com.androidapp.attendencecheckqrcode.utils.Resource;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class ClassDetailActivity extends AppCompatActivity {

    private TextView tvClassName, tvClassCode, tvLecturer, tvRoom, tvTime, tvDayDate;
    private TextView tvSubjectCode, tvStudentCount, tvSemester;
    private android.widget.Button btnScanQR, btnCreateQR;
    private View cvJoinInfo;

    // --- KHAI BÁO CÁC VIEW CHO KHU VỰC THỐNG KÊ DASHBOARD ---
    private TextView tvTotalSessions, tvPresentCount, tvAbsentCount, tvUpcomingSessions;
    private TextView tvAttendanceRate, tvRemainingAbsences;
    private android.widget.ProgressBar pbAttendanceRate;

    private Classroom currentClass;
    private ClassViewModel classViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            currentClass = (Classroom) intent.getSerializableExtra("classData");
            if (currentClass != null) {
                bindData(currentClass);
                setupRoleLogic(currentClass);
            }
        }

        setupViewModel();
    }

    private void initViews() {
        tvSubjectCode = findViewById(R.id.tvSubjectCode);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvClassName = findViewById(R.id.tvClassName);
        tvLecturer = findViewById(R.id.tvLecturer);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvSemester = findViewById(R.id.tvSemester);
        tvTime = findViewById(R.id.tvTime);
        tvRoom = findViewById(R.id.tvRoom);
        tvDayDate = findViewById(R.id.tvDayDate);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnCreateQR = findViewById(R.id.btnCreateQR);
        cvJoinInfo = findViewById(R.id.cvJoinInfo);

        // --- ÁNH XẠ VIEW CHO DASHBOARD ---
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvPresentCount = findViewById(R.id.tvPresentCount);
        tvAbsentCount = findViewById(R.id.tvAbsentCount);
        tvUpcomingSessions = findViewById(R.id.tvUpcomingSessions);
        tvAttendanceRate = findViewById(R.id.tvAttendanceRate);
        pbAttendanceRate = findViewById(R.id.pbAttendanceRate);
        tvRemainingAbsences = findViewById(R.id.tvRemainingAbsences);
    }

    private void setupViewModel() {
        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);

        if (currentClass != null && currentClass.getGroupId() != null) {
            // 1. Gọi API lấy chi tiết Lớp
            classViewModel.fetchClassDetail(currentClass.getGroupId());
            // 2. Gọi API lấy Thống kê chuyên cần
            classViewModel.fetchPolicyStatus(currentClass.getGroupId());
        }

        // Lắng nghe dữ liệu chi tiết Lớp
        classViewModel.getClassDetailResult().observe(this, resource -> {
            if (resource != null && resource.status == Resource.Status.SUCCESS) {
                if (resource.data != null) {
                    currentClass.setWeeklySchedules(resource.data.getWeeklySchedules());
                    currentClass.setJoinCode(resource.data.getJoinCode());
                    currentClass.setTotalSessions(resource.data.getTotalSessions());
                    currentClass.setMaxAllowedAbsences(resource.data.getMaxAllowedAbsences());

                    bindData(currentClass);
                }
            }
        });

        // --- LẮNG NGHE DỮ LIỆU THỐNG KÊ & VẼ LÊN UI ---
        classViewModel.getPolicyStatusResult().observe(this, resource -> {
            if (resource != null && resource.status == Resource.Status.SUCCESS && resource.data != null) {
                // GỌI HÀM NÀY ĐỂ ĐỔ SỐ LIỆU VÀO CÁC CARD
                bindStatsData(resource.data);
            } else if (resource != null && resource.status == Resource.Status.ERROR) {
                Log.e("DASHBOARD_ERROR", "Lỗi tải thống kê: " + resource.message);
            }
        });
    }

    // =========================================================================
    // HÀM MỚI: XỬ LÝ ĐỔ SỐ LIỆU VÀO GIAO DIỆN DASHBOARD (ĐÃ TỐI ƯU LOGIC)
    // =========================================================================
    private void bindStatsData(PolicyStatusResponse data) {
        if (data == null) return;

        int present = data.getPresentCount();
        int absent = data.getAbsentCount();
        int late = data.getLateCount();

        // 1. CẬP NHẬT THẺ ĐÃ ĐI, VẮNG MẶT
        if (tvPresentCount != null) tvPresentCount.setText(String.valueOf(present));
        if (tvAbsentCount != null) tvAbsentCount.setText(String.valueOf(absent));

        // 2. TỔNG BUỔI & SẮP TỚI (Lấy thẳng từ thông tin lớp học thật)
        int total = currentClass != null ? currentClass.getTotalSessions() : 0;

        // Nếu Backend trả về 0, tạm ép là 15 để test giao diện
        if (total <= 0) total = 15;

        if (tvTotalSessions != null) tvTotalSessions.setText(String.valueOf(total));

        int upcoming = total - (present + absent + late);
        if (tvUpcomingSessions != null) tvUpcomingSessions.setText(String.valueOf(Math.max(0, upcoming)));

        // 3. LOGIC BIỂU ĐỒ %
        if (tvAttendanceRate != null && pbAttendanceRate != null) {
            int rate = 100; // Mặc định 100%
            if (absent > 0) {
                int totalOccurred = present + absent + late;
                if (totalOccurred > 0) rate = (int) (((float) present / totalOccurred) * 100);
            }
            tvAttendanceRate.setText(rate + "%");
            pbAttendanceRate.setProgress(rate);
        }

        // 4. LOGIC SỐ BUỔI VẮNG CÒN LẠI (CẢNH BÁO)
        if (tvRemainingAbsences != null) {
            // Lấy Số buổi vắng tối đa từ dữ liệu lớp
            int maxAbsence = currentClass != null ? currentClass.getMaxAllowedAbsences() : 2;

            // Nếu Backend trả về 0 (chưa cài đặt), mặc định là 2
            if (maxAbsence <= 0) maxAbsence = 2;

            int remaining = maxAbsence - absent;

            if (remaining > 0) {
                tvRemainingAbsences.setText("0" + remaining + " buổi");
                tvRemainingAbsences.setTextColor(android.graphics.Color.parseColor("#757575"));
            } else {
                tvRemainingAbsences.setText("Hết phép");
                tvRemainingAbsences.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
            }
        }
    }

    private void bindData(Classroom classroom) {
        tvSubjectCode.setText(classroom.getCourseCode() != null ? classroom.getCourseCode() : "N/A");
        tvClassCode.setText(classroom.getClassCode() != null ? classroom.getClassCode() : "N/A");
        tvStudentCount.setText("● " + classroom.getApprovedStudentCount() + " Sinh viên");
        tvClassName.setText(classroom.getGroupName() != null ? classroom.getGroupName() : "Chưa có tên");

        tvLecturer.setText(classroom.getLecturerName() != null ? classroom.getLecturerName() : "Chưa phân công");

        String semester = classroom.getSemester() != null ? classroom.getSemester() : "N/A";
        String year = classroom.getAcademicYear() != null ? classroom.getAcademicYear() : "N/A";
        tvSemester.setText("HK" + semester + ", " + year);

        String room = classroom.getRoom() != null ? classroom.getRoom() : "Chưa có phòng";
        String campus = classroom.getLocationDisplay() != null ? classroom.getLocationDisplay() : "";
        tvRoom.setText(room + (campus.isEmpty() ? "" : " - " + campus));

        displayTime(classroom);

        if (cvJoinInfo != null) {
            cvJoinInfo.setOnClickListener(v -> {
                if (classroom.getJoinCode() != null) {
                    showJoinQrDialog(classroom.getJoinCode());
                } else {
                    Toast.makeText(this, "Đang tải mã tham gia...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String extractDayOfWeek(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "N/A";
        try {
            String cleanDate = isoDateString.split("\\.")[0];
            if (cleanDate.endsWith("Z")) cleanDate = cleanDate.substring(0, cleanDate.length() - 1);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(cleanDate);

            java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", new java.util.Locale("vi", "VN"));
            String dayName = dayFormat.format(date);
            return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        } catch (Exception e) {
            android.util.Log.e("PARSE_ERROR", "Lỗi Thứ: " + e.getMessage());
            return "N/A";
        }
    }

    private String extractTime(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "";
        try {
            String cleanDate = isoDateString.split("\\.")[0];
            if (cleanDate.endsWith("Z")) cleanDate = cleanDate.substring(0, cleanDate.length() - 1);

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(cleanDate);

            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return timeFormat.format(date);
        } catch (Exception e) {
            android.util.Log.e("PARSE_ERROR", "Lỗi Giờ: " + e.getMessage());
            return "";
        }
    }

    private void displayTime(Classroom classroom) {
        // 1. Ưu tiên dữ liệu từ mảng Lịch học (API Detail trả về mảng weeklySchedules)
        if (classroom.getWeeklySchedules() != null && !classroom.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule schedule = classroom.getWeeklySchedules().get(0);
            tvDayDate.setText(translateDay(schedule.getDayOfWeek()));

            String start = schedule.getStartTime() != null ? schedule.getStartTime() : "";
            String end = schedule.getEndTime() != null ? schedule.getEndTime() : "";

            try {
                // Cắt lấy HH:mm (Bỏ giây :ss đi nếu Backend trả về "07:00:00")
                if (start.length() >= 5) start = start.substring(0, 5);
                if (end.length() >= 5) end = end.substring(0, 5);
                tvTime.setText(start + " - " + end);
            } catch (Exception e) {
                tvTime.setText(start + " - " + end);
            }
        }
        // 2. Dự phòng: Dùng dữ liệu ISO từ màn hình danh sách truyền sang
        else if (classroom.getStartTime() != null && !classroom.getStartTime().isEmpty()) {
            tvDayDate.setText(extractDayOfWeek(classroom.getStartTime()));
            String start = extractTime(classroom.getStartTime());
            String end = extractTime(classroom.getEndTime());
            tvTime.setText(end.isEmpty() ? start : start + " - " + end);
        }
        // 3. Nếu cả 2 đều trống
        else {
            tvDayDate.setText("Chưa xếp lịch");
            tvTime.setText("--:--");
        }
    }

    private String translateDay(String englishDay) {
        if (englishDay == null) return "N/A";
        switch (englishDay.toUpperCase()) {
            case "MONDAY": return "Thứ 2";
            case "TUESDAY": return "Thứ 3";
            case "WEDNESDAY": return "Thứ 4";
            case "THURSDAY": return "Thứ 5";
            case "FRIDAY": return "Thứ 6";
            case "SATURDAY": return "Thứ 7";
            case "SUNDAY": return "Chủ Nhật";
            default: return englishDay;
        }
    }

    private void showJoinQrDialog(String joinCode) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_join_qr);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView ivDialogQrCode = dialog.findViewById(R.id.ivDialogQrCode);
        TextView tvDialogJoinCode = dialog.findViewById(R.id.tvDialogJoinCode);
        MaterialButton btnCloseDialog = dialog.findViewById(R.id.btnCloseDialog);

        tvDialogJoinCode.setText(joinCode);

        try {
            int size = 600;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(joinCode, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivDialogQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) { e.printStackTrace(); }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void setupRoleLogic(Classroom classroom) {
        if (btnCreateQR != null && btnScanQR != null) {
            if ("LECTURER".equals(classroom.getMyRole())) {
                btnCreateQR.setVisibility(View.VISIBLE);
                btnScanQR.setVisibility(View.GONE);
                btnCreateQR.setOnClickListener(v -> {
                    Intent i = new Intent(this, CreateQRActivity.class);
                    i.putExtra("classData", classroom);
                    startActivity(i);
                });
            } else {
                btnCreateQR.setVisibility(View.GONE);
                btnScanQR.setVisibility(View.VISIBLE);
                btnScanQR.setOnClickListener(v -> {
                    startActivity(new Intent(this, QRScanActivity.class));
                });
            }
        }
    }
}