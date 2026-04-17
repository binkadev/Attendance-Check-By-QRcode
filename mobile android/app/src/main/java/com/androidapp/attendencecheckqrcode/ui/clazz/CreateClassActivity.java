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

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.models.entities.User;
import com.androidapp.attendencecheckqrcode.utils.MockData;

import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

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
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.models.entities.User;

import java.util.Calendar;
import java.util.Locale;
import androidx.lifecycle.ViewModelProvider;

public class CreateClassActivity extends AppCompatActivity {

    // Views
    private ImageView btnBack;
    private TextView tvStartTime, tvEndTime, tvSemester;
    private EditText etClassName, etSubjectCode, etClassCode, etRoom, etDescription;
    private Button btnCreate;

    // Counter Variables (Logic cũ)
    private TextView tvTotalSessions, tvMaxAbsence;
    private ImageView btnMinusSession, btnPlusSession, btnMinusAbsent, btnPlusAbsent;
    private int totalSessions = 15; // Mặc định
    private int maxAbsence = 3;

    // Day Selection (Logic cũ)
    private TextView[] dayViews;
    private int[] dayIds = {R.id.tvDay2, R.id.tvDay3, R.id.tvDay4, R.id.tvDay5, R.id.tvDay6, R.id.tvDay7, R.id.tvDay8};

    // Data User
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

        // 2. BỔ SUNG DÒNG KHỞI TẠO NÀY TRƯỚC KHI GỌI observeViewModel()
        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);

        initViews();
        setupListeners();
        setDefaultSemester();

        // Khi classViewModel đã được khởi tạo ở trên, hàm bên dưới sẽ không bị lỗi nữa
        observeViewModel();
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
                    finish(); // Trở về danh sách
                    break;
                case ERROR:
                    btnCreate.setEnabled(true);
                    btnCreate.setText("Tạo lớp");
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
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

        // Set giá trị mặc định lên UI
        tvTotalSessions.setText(String.valueOf(totalSessions));
        tvMaxAbsence.setText(String.valueOf(maxAbsence));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Chọn giờ
        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));

        // Chọn học kỳ
        tvSemester.setOnClickListener(v -> showSemesterPicker());

        // --- LOGIC TĂNG GIẢM CŨ GIỮ NGUYÊN ---
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

        // --- LOGIC CHỌN THỨ ---
        for (TextView dayView : dayViews) {
            dayView.setOnClickListener(v -> {
                v.setSelected(!v.isSelected());

                // Ép kiểu v thành TextView
                TextView tv = (TextView) v;

                if (v.isSelected()) {
                    // Dùng biến tv đã ép kiểu để gọi setTextColor
                    tv.setTextColor(getResources().getColor(android.R.color.white));
                } else {
                    tv.setTextColor(getResources().getColor(android.R.color.black));
                }
            });
        }

        // --- NÚT TẠO LỚP (GOM DỮ LIỆU) ---
        btnCreate.setOnClickListener(v -> {
            if (validateInputs()) {
                createNewClassData();
            }
        });
    }

    // Hàm logic gom dữ liệu để lưu
    private void createNewClassData() {
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi User! Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder daysBuilder = new StringBuilder();
        for (TextView day : dayViews) {
            if (day.isSelected()) {
                if (daysBuilder.length() > 0) daysBuilder.append(", ");
                daysBuilder.append(day.getText().toString());
            }
        }

        // Tạo đối tượng gửi lên Backend (Backend sẽ tự sinh ID, nên truyền rỗng hoặc null cho ID)
        Attendance.Classroom newClass = new Attendance.Classroom(
                "", // Backend tự lo class_id
                etClassName.getText().toString().trim(),
                etSubjectCode.getText().toString().trim(),
                etClassCode.getText().toString().trim(),
                etRoom.getText().toString().trim(),
                daysBuilder.toString(),
                tvStartTime.getText() + " - " + tvEndTime.getText(),
                totalSessions,
                maxAbsence,
                tvSemester.getText().toString(),
                etDescription.getText().toString(),
                currentUser.getId(),
                currentUser.getFullName()
        );

        // Gọi ViewModel thay vì MockData
        classViewModel.createClass(newClass);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etClassName.getText())) { etClassName.setError("Nhập tên lớp"); return false; }
        if (TextUtils.isEmpty(etSubjectCode.getText())) { etSubjectCode.setError("Nhập mã môn"); return false; }
        if (TextUtils.isEmpty(etClassCode.getText())) { etClassCode.setError("Nhập mã lớp"); return false; }

        boolean hasDay = false;
        for(TextView tv : dayViews) if(tv.isSelected()) hasDay = true;
        if(!hasDay) { Toast.makeText(this, "Chọn ít nhất 1 ngày", Toast.LENGTH_SHORT).show(); return false; }

        return true;
    }

    // --- CÁC HÀM UI CŨ (Giữ nguyên) ---
    private void setDefaultSemester() {
        Calendar c = Calendar.getInstance();
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        String sem = (m >= 8) ? "Học kỳ 1" : (m <= 5 ? "Học kỳ 2" : "Học kỳ Hè");
        String yStr = (m >= 8) ? y + "-" + (y+1) : (y-1) + "-" + y;
        tvSemester.setText(sem + ", năm học " + yStr);
    }

    private void showSemesterPicker() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        String[] sems = {"Học kỳ 1, năm học " + (y-1) + "-" + y, "Học kỳ 2, năm học " + (y-1) + "-" + y, "Học kỳ 1, năm học " + y + "-" + (y+1)};
        new AlertDialog.Builder(this).setTitle("Chọn học kỳ").setItems(sems, (d, i) -> tvSemester.setText(sems[i])).show();
    }

    private void showTimePicker(TextView target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
}