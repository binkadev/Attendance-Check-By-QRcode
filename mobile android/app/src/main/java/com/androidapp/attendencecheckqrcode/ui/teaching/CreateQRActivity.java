package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.androidapp.attendencecheckqrcode.R;
// --- THÊM IMPORT DOMAIN CHUẨN ---
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class CreateQRActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTimer, tvSubTitle;
    private ImageView imgQRCode;
    private CountDownTimer countDownTimer;

    // Biến lưu thông tin lớp để tạo QR (Nên dùng ClassID để bảo mật hơn)
    private String classInfo = "Default_Class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_qr);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupData();

        // Tạo mã QR lần đầu tiên ngay khi mở màn hình
        refreshQRCode();

        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        imgQRCode = findViewById(R.id.imgQRCode);
    }

    private void setupData() {
        Intent intent = getIntent();
        // Bắt đối tượng ClassData từ TeachingDetailActivity gửi sang
        if (intent != null && intent.hasExtra("classData")) {
            Attendance.Classroom currentClass = (Attendance.Classroom) intent.getSerializableExtra("classData");

            if (currentClass != null) {
                // Hiển thị tên lớp lên SubTitle
                tvSubTitle.setText(currentClass.getClassName());

                // Dùng ClassID hoặc ClassCode để làm nội dung mã QR (tránh sinh viên giả mạo lớp khác)
                classInfo = currentClass.getClassId() != null ? currentClass.getClassId() : currentClass.getClassCode();
            }
        }
    }

    // --- HÀM 1: TẠO MÃ QR MỚI VÀ CHẠY TIMER ---
    private void refreshQRCode() {
        // 1. Tạo nội dung mã QR (Gồm ID Lớp + Thời gian hiện tại để mã luôn khác nhau)
        String qrContent = classInfo + "_" + System.currentTimeMillis();

        // 2. Tạo ảnh Bitmap từ nội dung
        try {
            Bitmap bitmap = generateQRCode(qrContent);
            imgQRCode.setImageBitmap(bitmap); // Hiển thị lên ImageView
        } catch (WriterException e) {
            e.printStackTrace();
        }

        // 3. Bắt đầu đếm ngược 5 giây
        startTimer();
    }

    // --- HÀM 2: LOGIC ĐẾM NGƯỢC ---
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel(); // Hủy timer cũ nếu có
        }

        // 6000ms (6s) để hiển thị mượt từ số 5 xuống 0
        countDownTimer = new CountDownTimer(6000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Hiển thị: 00 : 05
                long seconds = millisUntilFinished / 1000;
                // Nếu giây > 5 (do độ trễ) thì ép về 5 cho đẹp
                if (seconds > 5) seconds = 5;
                tvTimer.setText("00 : 0" + seconds);
            }

            @Override
            public void onFinish() {
                // Hết giờ -> Gọi lại hàm refresh để tạo mã mới
                refreshQRCode();
            }
        }.start();
    }

    // --- HÀM 3: THUẬT TOÁN TẠO ẢNH QR (ZXing) ---
    private Bitmap generateQRCode(String text) throws WriterException {
        int width = 500;
        int height = 500;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);

        // Chuyển BitMatrix thành Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Nếu bit là true -> màu Đen, false -> màu Trắng
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Click vào QR để làm mới ngay lập tức
        imgQRCode.setOnClickListener(v -> {
            Toast.makeText(this, "Đã làm mới mã QR", Toast.LENGTH_SHORT).show();
            refreshQRCode();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy timer khi thoát để tránh lỗi
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}