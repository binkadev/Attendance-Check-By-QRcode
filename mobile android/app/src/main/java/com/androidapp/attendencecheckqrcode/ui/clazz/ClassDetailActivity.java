package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;

public class ClassDetailActivity extends AppCompatActivity {
    private TextView tvClassName, tvClassCode, tvLecturer, tvRoom, tvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_detail);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tvClassName = findViewById(R.id.tvClassName);
        tvClassCode = findViewById(R.id.tvClassCode);
        tvLecturer = findViewById(R.id.tvLecturer);
        tvRoom = findViewById(R.id.tvRoom);
        tvTime = findViewById(R.id.tvTime);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            Attendance.Classroom classroom = (Attendance.Classroom) intent.getSerializableExtra("classData");
            if (classroom != null) {
                tvClassName.setText(classroom.getClassName());
                tvClassCode.setText(classroom.getClassCode());
                tvLecturer.setText(classroom.getLecturerName());
                tvRoom.setText(classroom.getRoom());
                tvTime.setText(classroom.getDayOfWeek() + " | " + classroom.getTimeSlot());
            }
        }

        findViewById(R.id.btnScanQR).setOnClickListener(v -> {
            Intent i = new Intent(ClassDetailActivity.this, QRScanActivity.class);
            startActivity(i);
        });
    }
}