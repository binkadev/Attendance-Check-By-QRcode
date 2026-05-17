package com.ptithcm.attendapp.view;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Image;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.ptithcm.attendapp.R;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.content.SharedPreferences;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.CheckInQrResponse;
import com.ptithcm.attendapp.model.JoinGroupRequest;
import com.ptithcm.attendapp.model.JoinGroupResponse;

import com.google.android.material.card.MaterialCardView;
import com.ptithcm.attendapp.model.QrCheckInRequest;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private ImageView btnClose, icFlash;
    private LinearLayout btnHelp, btnFlash;
    private TextView tvFlash, tvClassName;
    private boolean isFlashOn = false;

    // CameraX
    private PreviewView previewView;
    private Camera camera;
    private ExecutorService cameraExecutor;

    private MaterialCardView cardClassInfo;
    private TextView tvClassTime;
    private QROverlayView overlayView;

    // THÊM BIẾN NÀY ĐỂ KHOÁ CAMERA KHI ĐANG XỬ LÝ API
    private boolean isProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscanner);

        initViews();
        setupListeners();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        btnClose = findViewById(R.id.btnClose);
        btnHelp = findViewById(R.id.btnHelp);
        btnFlash = findViewById(R.id.btnFlash);
        icFlash = findViewById(R.id.icFlash);
        tvFlash = findViewById(R.id.tvFlash);
        tvClassName = findViewById(R.id.tvClassName);

        overlayView = findViewById(R.id.overlayView);

        // Ánh xạ các thành phần của thẻ thông tin
        cardClassInfo = findViewById(R.id.cardClassInfo);
        tvClassName = findViewById(R.id.tvClassName);
        tvClassTime = findViewById(R.id.tvClassTime);

        // ========================================================
        // LOGIC ẨN/HIỆN THẺ THÔNG TIN LỚP HỌC
        // ========================================================
        Intent intent = getIntent();
        String className = intent.getStringExtra("CLASS_NAME");
        String classTime = intent.getStringExtra("CLASS_TIME");

        // Nếu có truyền tên lớp sang (Mở từ màn hình Chi tiết Lớp)
        if (className != null && !className.isEmpty()) {
            cardClassInfo.setVisibility(View.VISIBLE); // HIỆN THẺ
            tvClassName.setText(className);

            if (classTime != null) {
                tvClassTime.setText(classTime);
            } else {
                tvClassTime.setText("Đang điểm danh");
            }
        }
        // Nếu KHÔNG truyền tên lớp sang (Mở từ Bottom Navigation)
        else {
            cardClassInfo.setVisibility(View.GONE); // ẨN THẺ HOÀN TOÀN
        }
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());
        btnHelp.setOnClickListener(v -> Toast.makeText(this, "Đưa mã QR vào khung hình.", Toast.LENGTH_SHORT).show());

        btnFlash.setOnClickListener(v -> {
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                isFlashOn = !isFlashOn;
                camera.getCameraControl().enableTorch(isFlashOn);

                if (isFlashOn) {
                    icFlash.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
                    icFlash.setBackgroundResource(R.drawable.bg_circle_flash_active);
                    tvFlash.setText("Tắt Flash");
                } else {
                    icFlash.setColorFilter(ContextCompat.getColor(this, R.color.red_500));
                    icFlash.setBackgroundResource(R.drawable.bg_circle_flash);
                    tvFlash.setText("Bật Flash");
                }
            } else {
                Toast.makeText(this, "Thiết bị không hỗ trợ Flash", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= CAMERAX & ML KIT ================= //

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Cấu hình View (Khung ngắm)
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Cấu hình Phân tích hình ảnh (Quét mã)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> processImageProxy(scanner, imageProxy));

                // Chọn Camera sau
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi động Camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // HÀM GỌI API THAM GIA LỚP
    private void callJoinGroupApi(String joinCode) {
        SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");
        if (token.isEmpty()) {
            showErrorAndRestartCamera("Chưa đăng nhập!");
            return;
        }

        JoinGroupRequest request = new JoinGroupRequest(joinCode);

        RetrofitClient.getApiService().joinGroup("Bearer " + token, request)
                .enqueue(new Callback<JoinGroupResponse>() {
                    @Override
                    public void onResponse(Call<JoinGroupResponse> call, Response<JoinGroupResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String status = response.body().getMemberStatus();

                            // Xử lý thông báo dựa vào quy chế lớp (Tự động duyệt hay Thủ công)
                            if ("PENDING".equalsIgnoreCase(status)) {
                                Toast.makeText(QRScannerActivity.this, "Xin vào lớp thành công! Vui lòng chờ Giảng viên duyệt.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(QRScannerActivity.this, "Tuyệt vời! Bạn đã tham gia lớp thành công.", Toast.LENGTH_LONG).show();
                            }

                            // Đóng màn hình quét và báo về Fragment trước
                            closeScannerWithSuccess("JOIN", joinCode);
                        } else {
                            // Xử lý các lỗi từ Server (Mã sai, đã tham gia rồi...)
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                                if (response.code() == 409) {
                                    showErrorAndRestartCamera("Bạn đã tham gia hoặc đang chờ duyệt lớp này rồi!");
                                } else if (response.code() == 404) {
                                    showErrorAndRestartCamera("Mã lớp không tồn tại!");
                                } else {
                                    showErrorAndRestartCamera("Lỗi tham gia lớp (Mã " + response.code() + ")");
                                }
                            } catch (Exception e) {
                                showErrorAndRestartCamera("Lỗi không xác định!");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<JoinGroupResponse> call, Throwable t) {
                        showErrorAndRestartCamera("Lỗi kết nối mạng, vui lòng thử lại!");
                    }
                });
    }

    @androidx.annotation.OptIn(markerClass = ExperimentalGetImage.class)
    private void processImageProxy(BarcodeScanner scanner, ImageProxy imageProxy) {
        // NẾU ĐANG GỌI API RỒI THÌ BỎ QUA KHÔNG QUÉT NỮA
        if (isProcessing) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            scanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String rawValue = barcode.getRawValue();

                            // NẾU TÌM THẤY MÃ VÀ CHƯA BỊ KHÓA
                            if (rawValue != null && !isProcessing) {
                                isProcessing = true; // NGAY LẬP TỨC KHÓA LẠI (Chống quét đúp)

                                // GỌI HIỆU ỨNG BÓP KHUNG LẠI VÀ CHUYỂN MÀU XANH
                                if (overlayView != null) {
                                    overlayView.animateToBarcode(barcode.getBoundingBox());
                                }

                                // 1. PHÁT TIẾNG BEEP
                                ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                                toneGen1.startTone(ToneGenerator.TONE_PROP_BEEP, 150);

                                // 2. DỪNG CAMERAX ĐỂ KHÔNG QUÉT LIÊN TỤC 1 MÃ
                                try {
                                    ProcessCameraProvider.getInstance(QRScannerActivity.this).get().unbindAll();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                // ========================================================
                                // 3. PHÂN LỒNG LOGIC TỰ ĐỘNG DỰA TRÊN TIỀN TỐ (PREFIX)
                                // ========================================================
                                if (rawValue.startsWith("JOIN:")) {
                                    String joinCode = rawValue.replace("JOIN:", "").trim();

                                    // TODO: Xử lý gọi API Xin tham gia lớp
                                    Toast.makeText(this, "Đang xử lý mã: " + joinCode, Toast.LENGTH_SHORT).show();
                                    // call API
                                    callJoinGroupApi(joinCode);
                                } else if (rawValue.startsWith("ATTEND:")) {

                                    // 1. In ra toàn bộ chuỗi QR gốc mà Camera đọc được
                                    Log.e("CHECKIN_DEBUG", "=== MÃ QR GỐC TỪ CAMERA ===");
                                    Log.e("CHECKIN_DEBUG", "RAW_VALUE: [" + rawValue + "]");

                                    // 2. Bỏ chữ "ATTEND:" và dọn sạch khoảng trắng/xuống dòng 2 đầu
                                    String payload = rawValue.substring(7).trim();

                                    // Cắt tại vị trí dấu hai chấm (:) đầu tiên
                                    int firstColonIndex = payload.indexOf(':');

                                    if (firstColonIndex != -1) {
                                        String sessionId = payload.substring(0, firstColonIndex).trim();

                                        // TOÀN BỘ phần đuôi phía sau dấu hai chấm
                                        String qrTokenId = payload.substring(firstColonIndex + 1).trim();

                                        // Chuyển dấu hai chấm thành dấu chấm (nếu team Web lỡ dùng sai)
                                        qrTokenId = qrTokenId.replace(":", ".");

                                        // 3. In ra kết quả sau khi cắt
                                        Log.e("CHECKIN_DEBUG", "=== KẾT QUẢ CẮT CHUỖI ===");
                                        Log.e("CHECKIN_DEBUG", "Session ID: [" + sessionId + "]");
                                        Log.e("CHECKIN_DEBUG", "Token gửi đi: [" + qrTokenId + "]");

                                        // BẮT ĐẦU GỌI API NHƯ CŨ
                                        SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
                                        String tokenCuaSinhVien = prefs.getString("ACCESS_TOKEN", "");

                                        if (tokenCuaSinhVien.isEmpty()) {
                                            showErrorAndRestartCamera("Chưa đăng nhập. Không tìm thấy token.");
                                            return;
                                        }

                                        String myAuthToken = "Bearer " + tokenCuaSinhVien;
                                        QrCheckInRequest requestBody = new QrCheckInRequest(qrTokenId);

//                                    // Bỏ chữ "ATTEND:" đi. Chuỗi còn lại sẽ là: "sessionId:tokenId.secret" hoặc "sessionId:tokenId:secret"
//                                    String payload = rawValue.substring(7);
//
//                                    // Cắt tại vị trí dấu hai chấm (:) đầu tiên
//                                    int firstColonIndex = payload.indexOf(':');
//
//                                    if (firstColonIndex != -1) {
//                                        String sessionId = payload.substring(0, firstColonIndex);
//
//                                        // TOÀN BỘ phần đuôi phía sau dấu hai chấm sẽ là Token + Secret
//                                        String qrTokenId = payload.substring(firstColonIndex + 1);
//
//                                        // 🚨 TRICK CHỐNG LỖI CỰC MẠNH:
//                                        // Nhỡ team Web tạo mã QR bằng dấu hai chấm (tokenId:secret) thay vì dấu chấm,
//                                        // thì Android sẽ tự động đổi nó thành dấu chấm cho đúng format Backend yêu cầu (<tokenId>.<secret>)
//                                        qrTokenId = qrTokenId.replace(":", ".");
//
////                                    String[] parts = rawValue.split(":");
////
////                                    // Chuỗi mới: ATTEND:{sessionId}:{qrTokenId}
////                                    if (parts.length >= 3) {
////                                        String sessionId = parts[1];
////                                        String qrTokenId = parts[2];
//
//                                        // Lấy Token của Sinh viên từ SharedPreferences
//                                        SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
//                                        String tokenCuaSinhVien = prefs.getString("ACCESS_TOKEN", "");
//
//                                        if (tokenCuaSinhVien.isEmpty()) {
//                                            showErrorAndRestartCamera("Chưa đăng nhập. Không tìm thấy token.");
//                                            return; // Dừng lại nếu không có token
//                                        }
//
//                                        String myAuthToken = "Bearer " + tokenCuaSinhVien;
//
//                                        // Tạo body request
//                                        QrCheckInRequest requestBody = new QrCheckInRequest(qrTokenId);


                                        // GỌI API ĐIỂM DANH
                                        RetrofitClient.getApiService().checkinWithQr(myAuthToken, sessionId, requestBody)
                                                .enqueue(new retrofit2.Callback<CheckInQrResponse>() {
                                                    @Override
                                                    public void onResponse(retrofit2.Call<CheckInQrResponse> call, retrofit2.Response<CheckInQrResponse> response) {
                                                        int errorCode = response.code();
                                                        String errorDetail = "Không có chi tiết"; // Rút biến này ra ngoài để dùng chung

                                                        // ========================================================
                                                        // 1. ĐỌC VÀ IN LOGCAT CHO MỌI TRƯỜNG HỢP LỖI (Bao gồm cả 409)
                                                        // ========================================================
                                                        if (!response.isSuccessful()) {
                                                            try {
                                                                if (response.errorBody() != null) {
                                                                    errorDetail = response.errorBody().string();
                                                                }
                                                            } catch (Exception e) {
                                                                errorDetail = "Lỗi đọc ErrorBody: " + e.getMessage();
                                                            }

                                                            // In ra Logcat (Mã 409 cũng sẽ in cục JSON ra đây)
                                                            Log.e("CHECKIN_DEBUG", "===========================================");
                                                            Log.e("CHECKIN_DEBUG", "THÔNG BÁO TỪ SERVER (HTTP " + errorCode + "):");
                                                            Log.e("CHECKIN_DEBUG", "Chi tiết (JSON): " + errorDetail);
                                                            Log.e("CHECKIN_DEBUG", "===========================================");
                                                        }

                                                        // ========================================================
                                                        // 2. XỬ LÝ LOGIC UI DỰA TRÊN HTTP CODE
                                                        // ========================================================
                                                        if (response.isSuccessful() || errorCode == 409) {

                                                            if (errorCode == 409) {
                                                                // Tại đây bạn có thể log thêm 1 lần nữa hoặc bóc tách errorDetail để show Toast nếu muốn
                                                                Log.i("CHECKIN_DEBUG", "=> XỬ LÝ 409: ĐÃ ĐIỂM DANH TỪ TRƯỚC!");
                                                                Toast.makeText(QRScannerActivity.this, "Bạn đã điểm danh thành công trước đó rồi!", Toast.LENGTH_LONG).show();
                                                            } else {
                                                                Log.i("CHECKIN_DEBUG", "=> XỬ LÝ 200: ĐIỂM DANH THÀNH CÔNG!");
                                                                Toast.makeText(QRScannerActivity.this, "Điểm danh thành công!", Toast.LENGTH_LONG).show();
                                                            }

                                                            // Gói dữ liệu truyền về và đóng Scanner
                                                            Intent resultIntent = new Intent();
                                                            resultIntent.putExtra("SCAN_SUCCESS", true);
                                                            resultIntent.putExtra("QR_TYPE", "ATTEND");
                                                            resultIntent.putExtra("SCANNED_SESSION_ID", sessionId);

                                                            setResult(RESULT_OK, resultIntent);
                                                            finish();
                                                        }
                                                        else {
                                                            // ========================================================
                                                            // 3. XỬ LÝ CÁC LỖI THỰC SỰ (BẮT QUÉT LẠI HOẶC ĐĂNG NHẬP LẠI)
                                                            // ========================================================
                                                            if (errorCode == 401) {
                                                                Toast.makeText(QRScannerActivity.this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();

                                                                SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
                                                                prefs.edit().clear().apply();

                                                                Intent loginIntent = new Intent(QRScannerActivity.this, LoginActivity.class);
                                                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                startActivity(loginIntent);
                                                                finish();

                                                            } else if (errorCode == 410) {
                                                                showErrorAndRestartCamera("Mã QR đã hết hạn! Vui lòng quét mã mới trên bảng.");
                                                            } else if (errorCode == 403) {
                                                                showErrorAndRestartCamera("Bạn không thuộc lớp này hoặc chưa được duyệt!");
                                                            } else if (errorCode == 400) {
                                                                showErrorAndRestartCamera("Mã QR lỗi hoặc thiết bị không khớp!");
                                                            } else {
                                                                showErrorAndRestartCamera("Lỗi " + errorCode + ": Quét lại hoặc báo Giảng viên.");
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(retrofit2.Call<CheckInQrResponse> call, Throwable t) {
                                                        Log.e("CHECKIN_DEBUG", "CRASH API/LỖI MẠNG: " + t.getMessage());
                                                        showErrorAndRestartCamera("Lỗi kết nối mạng: " + t.getMessage());
                                                    }
                                                });

//                                        // GỌI API ĐIỂM DANH
//                                        RetrofitClient.getApiService().checkinWithQr(myAuthToken, sessionId, requestBody)
//                                                .enqueue(new retrofit2.Callback<CheckInQrResponse>() {
//                                                    @Override
//                                                    public void onResponse(retrofit2.Call<CheckInQrResponse> call, retrofit2.Response<CheckInQrResponse> response) {
//                                                        int errorCode = response.code();
//
//                                                        // ========================================================
//                                                        // 1. ĐỌC VÀ IN LOGCAT CHO MỌI TRƯỜNG HỢP CÓ LỖI (KHÔNG PHẢI 200)
//                                                        // ========================================================
//                                                        if (!response.isSuccessful()) {
//                                                            String errorDetail = "Không có chi tiết";
//                                                            try {
//                                                                if (response.errorBody() != null) {
//                                                                    errorDetail = response.errorBody().string();
//                                                                }
//                                                            } catch (Exception e) {
//                                                                errorDetail = "Lỗi đọc ErrorBody: " + e.getMessage();
//                                                            }
//
//                                                            // In ra Logcat với Tag nổi bật để dễ tìm
//                                                            Log.e("CHECKIN_DEBUG", "===========================================");
//                                                            Log.e("CHECKIN_DEBUG", "THÔNG BÁO TỪ SERVER:");
//                                                            Log.e("CHECKIN_DEBUG", "HTTP Code: " + errorCode);
//                                                            Log.e("CHECKIN_DEBUG", "Chi tiết (JSON): " + errorDetail);
//                                                            Log.e("CHECKIN_DEBUG", "===========================================");
//                                                        }
//
//                                                        // ========================================================
//                                                        // 2. XỬ LÝ LOGIC UI DỰA TRÊN HTTP CODE
//                                                        // ========================================================
//                                                        if (response.isSuccessful() || errorCode == 409) {
//
//                                                            if (errorCode == 409) {
//                                                                Toast.makeText(QRScannerActivity.this, "Bạn đã điểm danh thành công trước đó rồi!", Toast.LENGTH_LONG).show();
//                                                                Log.i("CHECKIN_DEBUG", "=> ĐÃ ĐIỂM DANH TỪ TRƯỚC (409)!");
//                                                            } else {
//                                                                Toast.makeText(QRScannerActivity.this, "Điểm danh thành công!", Toast.LENGTH_LONG).show();
//                                                                Log.i("CHECKIN_DEBUG", "=> ĐIỂM DANH THÀNH CÔNG (200)!");
//                                                            }
//
//                                                            // Gói dữ liệu truyền về
//                                                            Intent resultIntent = new Intent();
//                                                            resultIntent.putExtra("SCAN_SUCCESS", true);
//                                                            resultIntent.putExtra("QR_TYPE", "ATTEND");
//                                                            resultIntent.putExtra("SCANNED_SESSION_ID", sessionId);
//
//                                                            setResult(RESULT_OK, resultIntent);
//                                                            finish();
//                                                        }
//                                                        else {
//                                                            // Phân loại các lỗi cần quét lại hoặc đăng nhập lại
//                                                            if (errorCode == 401) {
//                                                                Toast.makeText(QRScannerActivity.this, "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
//
//                                                                // Xóa token cũ và về trang Login
//                                                                SharedPreferences prefs = getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
//                                                                prefs.edit().clear().apply();
//
//                                                                Intent loginIntent = new Intent(QRScannerActivity.this, LoginActivity.class);
//                                                                loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                                                startActivity(loginIntent);
//                                                                finish();
//
//                                                            } else if (errorCode == 410) {
//                                                                showErrorAndRestartCamera("Mã QR đã hết hạn! Vui lòng quét mã mới trên bảng.");
//                                                            } else if (errorCode == 403) {
//                                                                showErrorAndRestartCamera("Bạn không thuộc lớp này hoặc chưa được duyệt!");
//                                                            } else if (errorCode == 400) {
//                                                                showErrorAndRestartCamera("Vị trí không hợp lệ hoặc thiết bị không khớp!");
//                                                            } else {
//                                                                showErrorAndRestartCamera("Lỗi " + errorCode + ": Quét lại hoặc báo Giảng viên.");
//                                                            }
//                                                        }
//                                                    }
//
//                                                    @Override
//                                                    public void onFailure(retrofit2.Call<CheckInQrResponse> call, Throwable t) {
//                                                        Log.e("CHECKIN_DEBUG", "CRASH API/LỖI MẠNG: " + t.getMessage());
//                                                        showErrorAndRestartCamera("Lỗi kết nối mạng: " + t.getMessage());
//                                                    }
//                                                });

                                        // GỌI API ĐIỂM DANH (Sử dụng RetrofitClient để lấy ApiService)
//                                        RetrofitClient.getApiService().checkinWithQr(myAuthToken, sessionId, requestBody)
//                                                .enqueue(new retrofit2.Callback<CheckInQrResponse>() {
//                                                    @Override
//                                                    public void onResponse(retrofit2.Call<CheckInQrResponse> call, retrofit2.Response<CheckInQrResponse> response) {
//                                                        int errorCode = response.code();
//
//                                                        if (response.isSuccessful() || errorCode == 409) {
//
//                                                            if (errorCode == 409) {
//                                                                Toast.makeText(QRScannerActivity.this, "Bạn đã điểm danh thành công trước đó rồi!", Toast.LENGTH_LONG).show();
//                                                                Log.i("CHECKIN_DEBUG", "=> ĐÃ ĐIỂM DANH TỪ TRƯỚC (409)!");
//                                                            } else {
//                                                                Toast.makeText(QRScannerActivity.this, "Điểm danh thành công!", Toast.LENGTH_LONG).show();
//                                                                Log.i("CHECKIN_DEBUG", "=> ĐIỂM DANH THÀNH CÔNG (200)!");
//                                                            }
//
//                                                            // Gói dữ liệu truyền về
//                                                            Intent resultIntent = new Intent();
//                                                            resultIntent.putExtra("SCAN_SUCCESS", true);
//                                                            resultIntent.putExtra("QR_TYPE", "ATTEND");
//                                                            resultIntent.putExtra("SCANNED_SESSION_ID", sessionId);
//
////                                                            // Dữ liệu mock (Thực tế nên lấy từ response API hoặc State trước đó)
////                                                            resultIntent.putExtra("CLASS_NAME", "Cơ sở dữ liệu nâng cao");
////                                                            resultIntent.putExtra("CLASS_CODE", "CS204.01");
////                                                            resultIntent.putExtra("ROOM", "Phòng 3D2 - Cơ sở Quận 9");
//
//                                                            setResult(RESULT_OK, resultIntent);
//                                                            Log.i("CHECKIN_DEBUG", "=> ĐIỂM DANH THÀNH CÔNG!");
//                                                            finish();
//                                                        }
//                                                        else {
//                                                            // ========================================================
//                                                            // ĐOẠN CODE CHẨN ĐOÁN LỖI (LOGCAT)
//                                                            // ========================================================
//                                                            String errorDetail = "Không có chi tiết";
//
//                                                            try {
//                                                                if (response.errorBody() != null) {
//                                                                    errorDetail = response.errorBody().string();
//                                                                }
//                                                            } catch (Exception e) {
//                                                                errorDetail = "Lỗi đọc ErrorBody: " + e.getMessage();
//                                                            }
//
//                                                            // In ra Logcat với Tag nổi bật để dễ tìm
//                                                            Log.e("CHECKIN_DEBUG", "===========================================");
//                                                            Log.e("CHECKIN_DEBUG", "THÔNG BÁO LỖI TỪ SERVER:");
//                                                            Log.e("CHECKIN_DEBUG", "HTTP Code: " + errorCode);
//                                                            Log.e("CHECKIN_DEBUG", "Chi tiết lỗi (JSON): " + errorDetail);
//                                                            Log.e("CHECKIN_DEBUG", "===========================================");
//
//                                                            // Phan loai loi
//                                                            if (errorCode == 410) {
//                                                                showErrorAndRestartCamera("Mã QR đã hết hạn! Vui lòng quét mã mới trên bảng.");
//                                                            } else if (errorCode == 403) {
//                                                                showErrorAndRestartCamera("Bạn không thuộc lớp này hoặc chưa được duyệt!");
//                                                            } else if (errorCode == 400) {
//                                                                showErrorAndRestartCamera("Vị trí không hợp lệ hoặc thiết bị không khớp!");
//                                                            } else {
//                                                                showErrorAndRestartCamera("Lỗi " + errorCode + ": Quét lại hoặc báo Giảng viên.");
//                                                            }
//                                                        }
//                                                    }
//
//                                                    @Override
//                                                    public void onFailure(retrofit2.Call<CheckInQrResponse> call, Throwable t) {
//                                                        Log.e("CHECKIN_DEBUG", "CRASH API/LỖI MẠNG: " + t.getMessage());
//                                                        showErrorAndRestartCamera("Lỗi kết nối mạng: " + t.getMessage());
//                                                    }
//                                                });
                                    } else {
                                        showErrorAndRestartCamera("Mã điểm danh bị lỗi định dạng!");
                                    }
                                } else {
                                    showErrorAndRestartCamera("Mã QR không thuộc hệ thống UniPortal!");
                                }
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Bỏ qua lỗi lẻ tẻ của ML Kit
                    })
                    .addOnCompleteListener(task -> imageProxy.close()); // Bắt buộc đóng ảnh
        }
    }

    // Hàm phụ trợ: Đóng Activity và trả kết quả về màn hình trước
    private void closeScannerWithSuccess(String type, String code) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SCAN_SUCCESS", true);
        resultIntent.putExtra("QR_TYPE", type);
        resultIntent.putExtra("SCANNED_CODE", code);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Hàm phụ trợ: Hiện lỗi và bật lại Camera để người dùng quét tiếp
    private void showErrorAndRestartCamera(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();

        // Khởi động lại Camera sau 2 giây
        previewView.postDelayed(() -> {
            if (allPermissionsGranted()) {
                isProcessing = false; // Mở khóa biến isProcessing để cho phép quét mã tiếp theo
                startCamera();
            }
        }, 2000);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền Camera", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}