package com.androidapp.attendencecheckqrcode.ui.join;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class JoinClassActivity extends AppCompatActivity {

    private EditText etJoinCode;
    private JoinClassViewModel joinClassViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_class);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();

        joinClassViewModel = new ViewModelProvider(this).get(JoinClassViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        etJoinCode = findViewById(R.id.etJoinCode);
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnJoinByCode).setOnClickListener(v -> {
            String code = etJoinCode.getText().toString().trim();
            if (!code.isEmpty()) {
                joinClassViewModel.joinClass(code);
            } else {
                Toast.makeText(this, "Vui lòng nhập mã lớp!", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnScanQR).setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt("Quét mã QR để tham gia lớp");
            integrator.setCameraId(0); // Dùng camera sau
            integrator.setBeepEnabled(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                joinClassViewModel.joinClass(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void observeViewModel() {
        joinClassViewModel.getJoinClassResult().observe(this, response -> {
            if (response == null) return;

            switch (response.status) {
                case LOADING:
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Chúc mừng! Bạn đã vào lớp thành công.", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK); // Báo cho màn hình trước biết để load lại danh sách
                    finish();
                    break;
                case ERROR:
                    Toast.makeText(this, "Thất bại: " + response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}