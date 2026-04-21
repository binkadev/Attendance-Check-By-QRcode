package com.androidapp.attendencecheckqrcode.ui.qr; // Sửa lại đúng package folder

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

    private String classGroupId = "Default_Class";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_qr);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupData();
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
        if (intent != null && intent.hasExtra("classData")) {
            Classroom currentClass = (Classroom) intent.getSerializableExtra("classData");
            if (currentClass != null) {
                tvSubTitle.setText(currentClass.getGroupName());
                classGroupId = currentClass.getGroupId();
            }
        }
    }

    private void refreshQRCode() {
        String qrContent = classGroupId + "_" + System.currentTimeMillis() + "_SECURE_PTIT";

        try {
            Bitmap bitmap = generateQRCode(qrContent);
            imgQRCode.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show();
        }

        startTimer();
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(11000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                if (seconds > 10) seconds = 10;
                tvTimer.setText("00 : " + (seconds < 10 ? "0" + seconds : seconds));
            }

            @Override
            public void onFinish() {
                refreshQRCode();
            }
        }.start();
    }

    private Bitmap generateQRCode(String text) throws WriterException {
        int width = 500;
        int height = 500;
        BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        imgQRCode.setOnClickListener(v -> refreshQRCode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}