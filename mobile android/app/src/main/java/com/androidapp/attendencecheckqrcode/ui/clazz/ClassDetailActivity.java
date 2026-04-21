package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.ui.qr.CreateQRActivity;
import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;

public class ClassDetailActivity extends AppCompatActivity {

    private TextView tvClassName, tvClassCode, tvLecturer, tvRoom, tvTime;
    private TextView tvSubjectCode, tvStudentCount, tvSemester;
    private android.widget.Button btnScanQR, btnCreateQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            Classroom classroom = (Classroom) intent.getSerializableExtra("classData");
            if (classroom != null) {
                bindData(classroom);
                setupRoleLogic(classroom);
            }
        }
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

        btnScanQR = findViewById(R.id.btnScanQR);
        btnCreateQR = findViewById(R.id.btnCreateQR);
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
    }

    private void displayTime(Classroom classroom) {
        if (classroom.getWeeklySchedules() != null && !classroom.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule firstSchedule = classroom.getWeeklySchedules().get(0);

            if (firstSchedule.getStartTime() != null && firstSchedule.getEndTime() != null) {
                try {
                    String start = firstSchedule.getStartTime();
                    String end = firstSchedule.getEndTime();

                    if (start.length() >= 5) start = start.substring(0, 5);
                    if (end.length() >= 5) end = end.substring(0, 5);

                    tvTime.setText(start + " - " + end);
                } catch (Exception e) {
                    tvTime.setText("--:-- - --:--");
                }
            } else {
                tvTime.setText("Chưa xếp lịch");
            }
        } else {
            tvTime.setText("Chưa xếp lịch");
        }
    }

    private void setupRoleLogic(Classroom classroom) {
        if (btnCreateQR != null && btnScanQR != null) {
            if ("LECTURER".equals(classroom.getMyRole())) {
                btnCreateQR.setVisibility(View.VISIBLE);
                btnScanQR.setVisibility(View.GONE);

                TextView tvActionTitle = findViewById(R.id.tvActionTitle);
                if (tvActionTitle != null) tvActionTitle.setText("Quản lý điểm danh");

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