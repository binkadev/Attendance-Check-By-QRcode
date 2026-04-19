package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;

public class TeachingDetailActivity extends AppCompatActivity {
    private TextView tvClassName, tvClassCode, tvRoom, tvTime;
    private Attendance.Classroom currentClass;
    private TeachingViewModel teachingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teaching_detail);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tvClassName = findViewById(R.id.tvClassName);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvRoom = findViewById(R.id.tvRoom);
        tvTime = findViewById(R.id.tvTime);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            currentClass = (Attendance.Classroom) intent.getSerializableExtra("classData");
            if (currentClass != null) {
                tvClassName.setText(currentClass.getClassName());
                tvClassCode.setText(currentClass.getClassCode());
                tvRoom.setText(currentClass.getRoom());
                tvTime.setText(currentClass.getDayOfWeek() + " | " + currentClass.getTimeSlot());
            }
        }

        // Nút Mở Form tạo QR Code điểm danh (Dành cho Giảng viên)
        findViewById(R.id.btnCreateQR).setOnClickListener(v -> {
            if (currentClass != null) {
                Intent i = new Intent(TeachingDetailActivity.this, CreateQRActivity.class);
                i.putExtra("classData", currentClass);
                startActivity(i);
            }
        });

        // MVVM: Gọi API lấy chi tiết lớp giảng
        teachingViewModel = new ViewModelProvider(this).get(TeachingViewModel.class);
        observeViewModel();

        if (currentClass != null && currentClass.getClassId() != null && !currentClass.getClassId().isEmpty()) {
            teachingViewModel.fetchClassDetails(currentClass.getClassId());
        }
    }

    private void observeViewModel() {
        teachingViewModel.getClassDetailsResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    // Hiện loading nhẹ
                    break;
                case SUCCESS:
                    if (response.data != null) {
                        // 1. Lấy Policy AN TOÀN (Kiểm tra null)
                        int maxAbsences = 0;
                        if (response.data.getPolicy() != null) {
                            maxAbsences = response.data.getPolicy().getCriticalAbsentCount();
                        }

                        // 2. Lấy danh sách sinh viên AN TOÀN
                        int studentCount = 0;
                        if (response.data.getStudents() != null) {
                            studentCount = response.data.getStudents().size();
                        }

                        Toast.makeText(this, "Lớp có " + studentCount + " SV. Vắng tối đa: " + maxAbsences, Toast.LENGTH_LONG).show();
                    }
                    break;
                case ERROR:
                    // LỖI 500 TỪ BACKEND
                    android.util.Log.e("TEACHING_DETAIL", "Lỗi tải chi tiết: " + response.message);

                    Toast.makeText(this, "Không thể tải chi tiết lớp lúc này. Lớp có thể chưa cấu hình luật điểm danh.", Toast.LENGTH_LONG).show();

                    break;
            }
        });
    }
}