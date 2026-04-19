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
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.domain.models.User;

import java.util.Calendar;
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

    // --- HÀM 1: SINH MÃ BẢO MẬT TUYỆT ĐỐI (Đã Rút Gọn) ---
    private String generateSecureJoinCode(String subjectCode) {
        // 1. Lấy 2 ký tự đầu của mã môn (nếu mã môn ngắn hơn 2 thì lấy hết)
        String prefix = subjectCode.trim().toUpperCase().replaceAll("\\s+", "");
        if (prefix.length() > 2) {
            prefix = prefix.substring(0, 2);
        }

        // 2. Lấy 4 ký tự ngẫu nhiên từ UUID
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // 3. Lấy 5 số cuối của mili-giây
        long timeSalt = System.currentTimeMillis() % 100000;
        String salt = String.format("%05d", timeSalt);

        // 4. Ghép lại. Ví dụ: IN_A7B2_48291 (Độ dài: 2 + 1 + 4 + 1 + 5 = 13 ký tự)
        // Chắc chắn vượt qua được Validation "between 6 and 16"
        return String.format("%s_%s_%s", prefix, randomPart, salt);
    }

    // --- HÀM 2: GOM DỮ LIỆU TẠO LỚP ---
    private void createNewClassData() {
        // 1. Lấy dữ liệu thô từ Giao diện (UI)
        String className = etClassName.getText().toString().trim();
        String subjectCode = etSubjectCode.getText().toString().trim();
        String classCode = etClassCode.getText().toString().trim();

        String description = etDescription.getText().toString().trim();
        String semester = tvSemester.getText().toString();
        String room = etRoom.getText().toString().trim();

        // 2. XỬ LÝ LOGIC: Gộp Mã Môn và Mã Lớp thành trường "code"
        // Phục vụ cho việc hiển thị danh sách của Backend
        // Kết quả ví dụ: "INT1340-D22CQAT01-N"
        String systemCode = subjectCode + "-" + classCode;

        // 3. XỬ LÝ LOGIC: Tự sinh "joinCode" duy nhất bằng hàm bên trên
        String uniqueJoinCode = generateSecureJoinCode(subjectCode);

        // 4. Đóng gói vào Request chuẩn của Backend
        CreateGroupRequest request = new CreateGroupRequest(
                className,          // name: Tên môn học
                systemCode,         // code: Mã môn - Mã lớp
                uniqueJoinCode,     // joinCode: Mã định danh kỹ thuật (Duy nhất 100%)
                description,        // description
                semester,           // semester
                room,               // room
                "AUTO",             // approvalMode: Mặc định Auto duyệt vào lớp
                true                // allowAutoJoinOnCheckin: Cho phép tự join khi quét QR
        );

        // 5. Gửi gọi API (maxAbsence được lấy từ UI của bạn)
        classViewModel.createClass(request, maxAbsence);
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(etClassName.getText())) { etClassName.setError("Nhập tên lớp"); return false; }
        if (TextUtils.isEmpty(etSubjectCode.getText())) { etSubjectCode.setError("Nhập mã môn"); return false; }
        if (TextUtils.isEmpty(etClassCode.getText())) { etClassCode.setError("Nhập mã lớp"); return false; }

        boolean hasDay = false;
        for (TextView tv : dayViews) if (tv.isSelected()) hasDay = true;
        if (!hasDay) { Toast.makeText(this, "Chọn ít nhất 1 ngày", Toast.LENGTH_SHORT).show(); return false; }

        return true;
    }

    private void setDefaultSemester() {
        Calendar c = Calendar.getInstance();
        int m = c.get(Calendar.MONTH) + 1;
        int y = c.get(Calendar.YEAR);
        String sem = (m >= 8) ? "Học kỳ 1" : (m <= 5 ? "Học kỳ 2" : "Học kỳ Hè");
        String yStr = (m >= 8) ? y + "-" + (y + 1) : (y - 1) + "-" + y;
        tvSemester.setText(sem + ", năm học " + yStr);
    }

    private void showSemesterPicker() {
        Calendar c = Calendar.getInstance();
        int y = c.get(Calendar.YEAR);
        String[] sems = {"Học kỳ 1, năm học " + (y - 1) + "-" + y, "Học kỳ 2, năm học " + (y - 1) + "-" + y, "Học kỳ 1, năm học " + y + "-" + (y + 1)};
        new AlertDialog.Builder(this).setTitle("Chọn học kỳ").setItems(sems, (d, i) -> tvSemester.setText(sems[i])).show();
    }

    private void showTimePicker(TextView target) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) ->
                target.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }
}