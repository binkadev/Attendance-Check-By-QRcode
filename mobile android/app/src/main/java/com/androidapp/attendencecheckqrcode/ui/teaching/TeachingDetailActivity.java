//package com.androidapp.attendencecheckqrcode.ui.teaching;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.androidapp.attendencecheckqrcode.R;
//import com.androidapp.attendencecheckqrcode.models.entities.Attendance.Classroom;
//import com.google.android.material.button.MaterialButton;
//
//public class TeachingDetailActivity extends AppCompatActivity {
//
//    private ImageView btnBack;
//    private TextView tvClassName, tvClassCode, tvRoom, tvTime, tvSubjectCode;
//    private MaterialButton btnCreateQR;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_teaching_detail);
//
//        if (getSupportActionBar() != null) getSupportActionBar().hide();
//
//        initViews();
//        bindData();
//        setupListeners();
//    }
//
//    private void initViews() {
//        btnBack = findViewById(R.id.btnBack);
//        tvClassName = findViewById(R.id.tvClassName);
//        tvClassCode = findViewById(R.id.tvClassCode);
//        tvRoom = findViewById(R.id.tvRoom);
//        tvTime = findViewById(R.id.tvTime);
//        tvSubjectCode = findViewById(R.id.tvSubjectCode);
//        btnCreateQR = findViewById(R.id.btnCreateQR);
//    }
//
//    private void bindData() {
//        // Nhận dữ liệu từ Adapter gửi sang
//        Intent intent = getIntent();
////        if (intent != null) {
////            String name = intent.getStringExtra("className");
////            String code = intent.getStringExtra("classCode");
////            String room = intent.getStringExtra("room");
////            String time = intent.getStringExtra("time");
////            String subjectCode = intent.getStringExtra("subjectCode");
////
////            if (name != null) tvClassName.setText(name);
////            if (code != null) tvClassCode.setText(code);
////            if (room != null) tvRoom.setText(room);
////            if (time != null) tvTime.setText(time);
////            if (subjectCode != null) tvSubjectCode.setText(subjectCode);
////        }
//        if (intent != null) {
//            // NHẬN DỮ LIỆU KIỂU OBJECT (Model Classroom)
//            Classroom classroom = (Classroom) intent.getSerializableExtra("classData");
//
//            if (classroom != null) {
//                tvClassName.setText(classroom.getClassName());
//                tvClassCode.setText(classroom.getClassCode());
//                tvSubjectCode.setText(classroom.getSubjectCode());
//                tvRoom.setText(classroom.getRoom());
//
//                // Ghép thời gian hiển thị
//                String timeStr = classroom.getDayOfWeek() + ", " + classroom.getTimeSlot();
//                tvTime.setText(timeStr);
//
//                // Các thông tin khác nếu có View
//                // tvSemester.setText(classroom.getSemester());
//            }
//        }
//    }
//
//    private void setupListeners() {
//        btnBack.setOnClickListener(v -> finish());
//
//        // Sự kiện nút Tạo mã điểm danh
//        btnCreateQR.setOnClickListener(v -> {
//            Toast.makeText(this, "Chuyển sang màn hình tạo mã QR...", Toast.LENGTH_SHORT).show();
//            // TODO: Chuyển sang Activity tạo mã điểm danh sau này
//            // Intent intent = new Intent(this, CreateQRActivity.class);
//            // startActivity(intent);
//            Intent intent = new Intent(TeachingDetailActivity.this, com.androidapp.attendencecheckqrcode.ui.teaching.CreateQRActivity.class);
//            // Truyền tên lớp sang để hiển thị trên Header
//            intent.putExtra("className", tvClassName.getText().toString() + " - " + tvSubjectCode.getText().toString());
//            startActivity(intent);
//        });
//    }
//}


package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.google.android.material.button.MaterialButton;

public class TeachingDetailActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvClassName, tvClassCode, tvRoom, tvTime, tvSubjectCode;
    private MaterialButton btnCreateQR;

    // Biến lưu trữ object lớp học hiện tại
    private Attendance.Classroom currentClassroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teaching_detail);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        bindData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvClassName = findViewById(R.id.tvClassName);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvRoom = findViewById(R.id.tvRoom);
        tvTime = findViewById(R.id.tvTime);
        tvSubjectCode = findViewById(R.id.tvSubjectCode);
        btnCreateQR = findViewById(R.id.btnCreateQR);
    }

    private void bindData() {
        Intent intent = getIntent();
        if (intent != null) {
            // NHẬN DỮ LIỆU KIỂU OBJECT (Model Classroom)
            currentClassroom = (Attendance.Classroom) intent.getSerializableExtra("classData");

            if (currentClassroom != null) {
                tvClassName.setText(currentClassroom.getClassName());
                tvClassCode.setText(currentClassroom.getClassCode());
                tvSubjectCode.setText(currentClassroom.getSubjectCode());
                tvRoom.setText(currentClassroom.getRoom());

                // Ghép thời gian hiển thị: "T2, T4 | 07:00 - 09:00"
                String timeStr = currentClassroom.getDayOfWeek() + " | " + currentClassroom.getTimeSlot();
                tvTime.setText(timeStr);
            }
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Sự kiện nút Tạo mã điểm danh
        btnCreateQR.setOnClickListener(v -> {
            Intent intent = new Intent(TeachingDetailActivity.this, CreateQRActivity.class);

            // Truyền tên lớp để hiển thị tiêu đề bên màn hình QR
            if (currentClassroom != null) {
                String title = currentClassroom.getClassName() + " - " + currentClassroom.getSubjectCode();
                intent.putExtra("className", title);
            } else {
                // Fallback nếu null
                intent.putExtra("className", tvClassName.getText().toString());
            }

            startActivity(intent);
        });
    }
}