package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.ui.qr.CreateQRActivity;

public class TeachingDetailActivity extends AppCompatActivity {

    private TextView tvClassName, tvClassCode, tvRoom, tvTime;
    private TextView tvSubjectCode, tvStudentCount, tvLecturer, tvSemester;

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
    }

    private void displayHeaderData() {
        tvClassName.setText(validateData(currentClass.getGroupName()));
        tvSubjectCode.setText(validateData(currentClass.getCourseCode()));
        tvClassCode.setText(validateData(currentClass.getClassCode()));

        String lecturer = validateData(currentClass.getLecturerName());
        tvLecturer.setText("Tôi (" + lecturer + ")");

        tvStudentCount.setText("● " + currentClass.getApprovedStudentCount() + " Sinh viên");

        if (currentClass.getWeeklySchedules() != null && !currentClass.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule firstSchedule = currentClass.getWeeklySchedules().get(0);

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

        String semester = validateData(currentClass.getSemester());
        String year = validateData(currentClass.getAcademicYear());
        tvSemester.setText(semester + ", " + year);

        String room = validateData(currentClass.getRoom());
        String location = validateData(currentClass.getLocationDisplay());
        tvRoom.setText(room + " - " + location);
    }

    private String validateData(String data) {
        if (data == null || data.trim().isEmpty() || data.equalsIgnoreCase("null")) {
            return "N/A";
        }
        return data;
    }

    private void setupActions() {
        findViewById(R.id.btnCreateQR).setOnClickListener(v -> {
            if (currentClass != null) {
                Intent i = new Intent(this, CreateQRActivity.class);
                i.putExtra("classData", currentClass);
                startActivity(i);
            }
        });

        teachingViewModel = new ViewModelProvider(this).get(TeachingViewModel.class);
        if (currentClass != null && currentClass.getGroupId() != null) {
            teachingViewModel.fetchClassDetails(currentClass.getGroupId());
        }

        observeViewModel();
    }

    private void observeViewModel() {
        teachingViewModel.getClassDetailsResult().observe(this, response -> {
            if (response == null) return;

            switch (response.status) {
                case SUCCESS:
                    if (response.data != null && response.data.getStudents() != null) {
                        int count = response.data.getStudents().size();
                        tvStudentCount.setText("● " + count + " Sinh viên");
                    }
                    break;
                case ERROR:
                    Log.e("DEBUG", "Error fetching class details: " + response.message);
                    break;
            }
        });
    }
}