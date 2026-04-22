package com.androidapp.attendencecheckqrcode.ui.qr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class CreateQRActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTimer, tvSubTitle;
    private ImageView imgQRCode;

    private CountDownTimer countDownTimer;
    private QRViewModel qrViewModel;

    // Ghi chú quan trọng: Tạm thời do Backend chưa có API tạo Session
    // Mình sẽ tạm gán sessionId bằng groupId để test. Sau này có API bạn cập nhật lại nhé!
    private String currentSessionId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_qr);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        qrViewModel = new ViewModelProvider(this).get(QRViewModel.class);

        initViews();
        setupData();
        setupListeners();
        observeViewModel();

        // Lần đầu tiên vào màn hình, gõ cửa server xin QR liền
        requestNewQrCode();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTimer = findViewById(R.id.tvTimer);
        tvSubTitle = findViewById(R.id.tvSubTitle);
        imgQRCode = findViewById(R.id.imgQRCode);
    }

    private void setupData() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("classData")) {
            Classroom currentClass = (Classroom) intent.getSerializableExtra("classData");
            if (currentClass != null) {
                tvSubTitle.setText(currentClass.getGroupName());
                currentSessionId = currentClass.getGroupId(); // Tạm xài groupId làm sessionId
            }
        }
    }

    // Lắng nghe token trả về từ API
    private void observeViewModel() {
        qrViewModel.getQrTokenResult().observe(this, response -> {
            if (response == null) return;
            switch (response.status) {
                case SUCCESS:
                    if (response.data != null) {
                        // Nhận được token XỊN từ server, tiến hành vẽ QR
                        drawQRCode(response.data);
                        // Vẽ xong thì bắt đầu đếm ngược 5 giây để làm mới
                        startCountdownTimer();
                    }
                    break;
                case ERROR:
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                    // Lỗi mạng thì đếm ngược 5 giây rồi thử lấy lại
                    startCountdownTimer();
                    break;
            }
        });
    }

    // Hàm gõ cửa API
    private void requestNewQrCode() {
        if (!currentSessionId.isEmpty()) {
            tvTimer.setText("Đang làm mới...");
            qrViewModel.fetchNewQrToken(currentSessionId);
        }
    }

    // Hàm đếm ngược hiển thị lên màn hình (5 -> 0)
    private void startCountdownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Đếm 5000ms (5s), mỗi nhịp 1000ms (1s)
        countDownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvTimer.setText("Làm mới trong: 0" + seconds + "s");
            }

            @Override
            public void onFinish() {
                // Hết 5 giây, tự động gọi API lấy mã mới
                requestNewQrCode();
            }
        }.start();
    }

    // Dùng ZXing vẽ cái token thành hình QR
    private void drawQRCode(String token) {
        try {
            int width = 500;
            int height = 500;
            BitMatrix bitMatrix = new MultiFormatWriter().encode(token, BarcodeFormat.QR_CODE, width, height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            imgQRCode.setImageBitmap(bitmap);

            Log.d("QR_CREATE", "Đã vẽ mã QR thành công với token: " + token);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi vẽ mã QR!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Nếu bấm trực tiếp vào mã QR thì làm mới ngay lập tức
        imgQRCode.setOnClickListener(v -> requestNewQrCode());
    }

    // Quan trọng: Thoát Activity phải hủy Timer để không bị rò rỉ bộ nhớ!
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}