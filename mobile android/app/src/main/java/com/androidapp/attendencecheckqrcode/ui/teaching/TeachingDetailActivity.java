package com.androidapp.attendencecheckqrcode.ui.teaching;

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
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.ui.qr.CreateQRActivity;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class TeachingDetailActivity extends AppCompatActivity {

    private TextView tvClassName, tvClassCode, tvRoom, tvTime, tvDayDate;
    private TextView tvSubjectCode, tvStudentCount, tvLecturer, tvSemester;

    // ĐÃ FIX: Chỉ cần cái thẻ CardView này để làm nút bấm
    private View cvJoinInfo;

    private Classroom currentClass;
    private TeachingViewModel teachingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teaching_detail);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            currentClass = (Classroom) intent.getSerializableExtra("classData");
            if (currentClass != null) {
                displayHeaderData();
            }
        }

        setupActions();
    }

    private void initViews() {
        tvClassName = findViewById(R.id.tvClassName);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvRoom = findViewById(R.id.tvRoom);
        tvTime = findViewById(R.id.tvTime);
        tvSubjectCode = findViewById(R.id.tvSubjectCode);
        tvStudentCount = findViewById(R.id.tvStudentCount);
        tvLecturer = findViewById(R.id.tvLecturer);
        tvSemester = findViewById(R.id.tvSemester);
        tvDayDate = findViewById(R.id.tvDayDate);

        // Ánh xạ khối Nút Tham Gia
        cvJoinInfo = findViewById(R.id.cvJoinInfo);
    }

    private void displayHeaderData() {
        if (currentClass == null) return;

        tvClassName.setText(validateData(currentClass.getGroupName()));
        tvSubjectCode.setText(validateData(currentClass.getCourseCode()));
        tvClassCode.setText(validateData(currentClass.getClassCode()));
        tvLecturer.setText("Tôi (" + validateData(currentClass.getLecturerName()) + ")");
        tvStudentCount.setText("● " + currentClass.getApprovedStudentCount() + " Sinh viên");

        tvSemester.setText(validateData(currentClass.getSemester()) + ", " + validateData(currentClass.getAcademicYear()));

        String room = validateData(currentClass.getRoom());
        String location = validateData(currentClass.getLocationDisplay());
        tvRoom.setText(room.equals("N/A") ? "Chưa xếp phòng" : (room + " - " + location));

        if (currentClass.getWeeklySchedules() != null && !currentClass.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule schedule = currentClass.getWeeklySchedules().get(0);
            tvDayDate.setText(translateDay(schedule.getDayOfWeek()));

            String start = schedule.getStartTime();
            String end = schedule.getEndTime();
            if (start != null && end != null) {
                if (start.length() >= 5) start = start.substring(0, 5);
                if (end.length() >= 5) end = end.substring(0, 5);
                tvTime.setText(start + " - " + end);
            } else {
                tvTime.setText("Chưa xếp giờ");
            }
        } else {
            tvDayDate.setText("Chưa xếp lịch");
            tvTime.setText("--:--");
        }
    }

    private String validateData(String data) {
        return (data == null || data.trim().isEmpty() || data.equalsIgnoreCase("null")) ? "N/A" : data;
    }

    private void setupActions() {
        findViewById(R.id.btnCreateQR).setOnClickListener(v -> {
            if (currentClass != null) {
                Intent i = new Intent(this, CreateQRActivity.class);
                i.putExtra("classData", currentClass);
                startActivity(i);
            }
        });

        // ĐÃ FIX: Nhấn vào nguyên cái thẻ ngang để bung Dialog
        cvJoinInfo.setOnClickListener(v -> {
            if (currentClass != null && currentClass.getJoinCode() != null && !currentClass.getJoinCode().isEmpty()) {
                showJoinQrDialog(currentClass.getJoinCode());
            } else {
                Toast.makeText(this, "Lớp này chưa có mã tham gia hoặc đang tải!", Toast.LENGTH_SHORT).show();
            }
        });

        teachingViewModel = new ViewModelProvider(this).get(TeachingViewModel.class);

        if (currentClass != null && currentClass.getGroupId() != null) {
            String gid = currentClass.getGroupId();
            teachingViewModel.fetchClassFullInfo(gid);
            teachingViewModel.fetchClassDetails(gid);
        }

        observeViewModel();
    }

    private void observeViewModel() {
        teachingViewModel.getClassFullInfo().observe(this, response -> {
            if (response != null && response.status == com.androidapp.attendencecheckqrcode.utils.Resource.Status.SUCCESS) {
                if (response.data != null && this.currentClass != null) {
                    this.currentClass.setWeeklySchedules(response.data.getWeeklySchedules());
                    this.currentClass.setJoinCode(response.data.getJoinCode());
                    this.currentClass.setDescription(response.data.getDescription());
                    this.currentClass.setTotalSessions(response.data.getTotalSessions());
                    this.currentClass.setMaxAllowedAbsences(response.data.getMaxAllowedAbsences());

                    displayHeaderData();
                }
            } else if (response != null && response.status == com.androidapp.attendencecheckqrcode.utils.Resource.Status.ERROR) {
                Log.e("DETAIL", "Không lấy được chi tiết lịch học: " + response.message);
            }
        });

        teachingViewModel.getClassDetailsResult().observe(this, response -> {
            if (response != null && response.status == com.androidapp.attendencecheckqrcode.utils.Resource.Status.SUCCESS) {
                if (response.data != null && response.data.getStudents() != null) {
                    tvStudentCount.setText("● " + response.data.getStudents().size() + " Sinh viên");
                }
            }
        });
    }

    // --- HÀM HIỂN THỊ POPUP QR THAM GIA LỚP ---
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
            int width = 600;
            int height = 600;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(joinCode, BarcodeFormat.QR_CODE, width, height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ivDialogQrCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Log.e("QR_ERROR", "Lỗi tạo QR tham gia lớp: " + e.getMessage());
        }

        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
}