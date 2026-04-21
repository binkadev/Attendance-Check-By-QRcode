package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.domain.models.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreateClassActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvStartTime, tvEndTime, tvSemester;
    private EditText etClassName, etSubjectCode, etClassCode, etRoom, etDescription;
    private Button btnCreate;

    private TextView tvTotalSessions, tvMaxAbsence;
    private ImageView btnMinusSession, btnPlusSession, btnMinusAbsent, btnPlusAbsent;
    private int totalSessions = 15;
    private int maxAbsence = 3;

    private TextView[] dayViews;
    private final int[] dayIds = {R.id.tvDay2, R.id.tvDay3, R.id.tvDay4, R.id.tvDay5, R.id.tvDay6, R.id.tvDay7, R.id.tvDay8};

    // Ánh xạ thứ tự ID với chuẩn Tiếng Anh của Backend
    private final String[] daysOfWeek = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};

    private User currentUser;
    private ClassViewModel classViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_class);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        if (getIntent().hasExtra("currentUser")) {
            currentUser = (User) getIntent().getSerializableExtra("currentUser");
        }

        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);

        initViews();
        setupListeners();
        setDefaultSemester();
        observeViewModel();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etClassName = findViewById(R.id.etClassName);
        etSubjectCode = findViewById(R.id.etSubjectCode);
        etClassCode = findViewById(R.id.etClassCode);
        etRoom = findViewById(R.id.etRoom);
        etDescription = findViewById(R.id.etDescription);

        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);
        tvSemester = findViewById(R.id.tvSemester);
        btnCreate = findViewById(R.id.btnCreate);

        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        btnMinusSession = findViewById(R.id.btnMinusSession);
        btnPlusSession = findViewById(R.id.btnPlusSession);

        tvMaxAbsence = findViewById(R.id.tvMaxAbsence);
        btnMinusAbsent = findViewById(R.id.btnMinusAbsent);
        btnPlusAbsent = findViewById(R.id.btnPlusAbsent);

        dayViews = new TextView[7];
        for (int i = 0; i < 7; i++) {
            dayViews[i] = findViewById(dayIds[i]);
        }

        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvMaxAbsence.setText(String.valueOf(maxAbsence));
    }

    private void observeViewModel() {
        classViewModel.getCreateClassResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    btnCreate.setEnabled(false);
                    btnCreate.setText("Đang tạo...");
                    break;
                case SUCCESS:
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Tạo lớp");
                    Toast.makeText(this, "Tạo lớp thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                case ERROR:
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Tạo lớp");
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));
        tvSemester.setOnClickListener(v -> showSemesterPicker());

        btnMinusSession.setOnClickListener(v -> {
            if (totalSessions > 1) {
                totalSessions--;
                tvTotalSessions.setText(String.valueOf(totalSessions));
            }
        });
        btnPlusSession.setOnClickListener(v -> {
            totalSessions++;
            tvTotalSessions.setText(String.valueOf(totalSessions));
        });

        btnMinusAbsent.setOnClickListener(v -> {
            if (maxAbsence > 0) {
                maxAbsence--;
                tvMaxAbsence.setText(String.valueOf(maxAbsence));
            }
        });
        btnPlusAbsent.setOnClickListener(v -> {
            maxAbsence++;
            tvMaxAbsence.setText(String.valueOf(maxAbsence));
        });

        for (TextView dayView : dayViews) {
            dayView.setOnClickListener(v -> {
                boolean isCurrentlySelected = v.isSelected();
                v.setSelected(!isCurrentlySelected);
                TextView tv = (TextView) v;

                if (!isCurrentlySelected) {
                    tv.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                } else {
                    tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
            });
        }

        btnCreate.setOnClickListener(v -> {
            if (validateInputs()) {
                createNewClassData();
            }
        });
    }

    private String generateSecureJoinCode(String subjectCode) {
        String prefix = subjectCode.trim().toUpperCase().replaceAll("\\s+", "");
        if (prefix.length() > 2) prefix = prefix.substring(0, 2);
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        long timeSalt = System.currentTimeMillis() % 100000;
        return String.format("%s_%s_%05d", prefix, randomPart, timeSalt);
    }

    private void createNewClassData() {
        String className = etClassName.getText().toString().trim();
        String subjectCode = etSubjectCode.getText().toString().trim();
        String classCode = etClassCode.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String room = etRoom.getText().toString().trim();

        String fullSemester = tvSemester.getText().toString();
        String semester = "HK1";
        String academicYear = "2025-2026";
        if (fullSemester.contains(", ")) {
            String[] parts = fullSemester.split(", ");
            semester = parts[0].toUpperCase();
            academicYear = parts[1];
        }

        String systemCode = subjectCode + "-" + classCode;

        List<CreateGroupRequest.WeeklySchedule> schedules = new ArrayList<>();

        // CẬP NHẬT: Cắt sạch khoảng trắng ở hai đầu chuỗi giờ
        String startTime = tvStartTime.getText().toString().trim();
        String endTime = tvEndTime.getText().toString().trim();

        for (int i = 0; i < 7; i++) {
            if (dayViews[i].isSelected()) {
                schedules.add(new CreateGroupRequest.WeeklySchedule(daysOfWeek[i], startTime, endTime));
            }
        }

        String uniqueJoinCode = generateSecureJoinCode(subjectCode);

        CreateGroupRequest request = new CreateGroupRequest(
                className,
                systemCode,
                subjectCode,
                classCode,
                uniqueJoinCode,
                description,
                semester,
                academicYear,
                "Cơ sở Q9",
                room,
                "AUTO",
                true,
                schedules,
                totalSessions,
                maxAbsence
        );

        classViewModel.createClass(request, maxAbsence);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etClassName.getText())) { etClassName.setError("Nhập tên lớp"); return false; }
        if (TextUtils.isEmpty(etSubjectCode.getText())) { etSubjectCode.setError("Nhập mã môn"); return false; }
        if (TextUtils.isEmpty(etClassCode.getText())) { etClassCode.setError("Nhập mã lớp"); return false; }

        // CẬP NHẬT: Dùng Regex kiểm tra chuẩn giờ HH:mm (VD: 07:30, 14:05)
        String startTime = tvStartTime.getText().toString().trim();
        String endTime = tvEndTime.getText().toString().trim();
        String timeRegex = "^([01]\\d|2[0-3]):([0-5]\\d)$";

        if (!startTime.matches(timeRegex) || !endTime.matches(timeRegex)) {
            Toast.makeText(this, "Vui lòng chọn thời gian bắt đầu và kết thúc hợp lệ (VD: 07:30)", Toast.LENGTH_LONG).show();
            return false;
        }

        boolean hasDay = false;
        for (TextView tv : dayViews) if (tv.isSelected()) hasDay = true;
        if (!hasDay) { Toast.makeText(this, "Chọn ít nhất 1 ngày học trong tuần!", Toast.LENGTH_SHORT).show(); return false; }

        return true;
    }

    private void setDefaultSemester() {
        Calendar c = Calendar.getInstance();
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        String sem = (m >= 8) ? "HK1" : (m <= 5 ? "Hk2" : "Hk Hè");
        String yStr = (m >= 8) ? y + "-" + (y + 1) : (y - 1) + "-" + y;
        tvSemester.setText(sem + ", " + yStr);
    }

    private void showSemesterPicker() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        String[] sems = {"HK1, " + (y - 1) + "-" + y, "HK2, " + (y - 1) + "-" + y, "HK1, " + y + "-" + (y + 1)};
        new AlertDialog.Builder(this).setTitle("Chọn học kỳ").setItems(sems, (d, i) -> tvSemester.setText(sems[i])).show();
    }

    private void showTimePicker(TextView target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
}