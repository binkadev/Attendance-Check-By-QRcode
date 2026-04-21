package com.androidapp.attendencecheckqrcode.ui.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRScanActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private ExecutorService cameraExecutor;
    private static final int REQUEST_CODE_CAMERA = 10;

    private boolean isProcessing = false;
    private ImageView btnSwitchCamera;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private View viewLaser;

    private QRViewModel qrViewModel;
    private Camera camera; // Biến lưu trữ Camera để điều khiển Zoom

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrscan);

        qrViewModel = new ViewModelProvider(this).get(QRViewModel.class);
        cameraExecutor = Executors.newSingleThreadExecutor();

        viewFinder = findViewById(R.id.viewFinder);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        viewLaser = findViewById(R.id.viewLaser);

        setupAnimation();
        setupListeners();
        observeViewModel();

        checkPermissionAndStartCamera();
    }

    private void observeViewModel() {
        qrViewModel.getCheckinResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    Toast.makeText(this, "Đang xác thực điểm danh...", Toast.LENGTH_SHORT).show();
                    break;
                case SUCCESS:
                    Toast.makeText(this, "Điểm danh thành công!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                    break;
                case ERROR:
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    isProcessing = false;
                    break;
            }
        });
    }

    private void setupAnimation() {
        final float scale = getResources().getDisplayMetrics().density;
        int distanceInPixels = (int) (140 * scale + 0.5f);

        TranslateAnimation animation = new TranslateAnimation(
                0, 0,
                -distanceInPixels,
                distanceInPixels
        );
        animation.setDuration(2500);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        viewLaser.startAnimation(animation);
    }

    private void setupListeners() {
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        btnSwitchCamera.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ?
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });
    }

    private void checkPermissionAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Nhận thêm boundingBox và kích thước ảnh
                imageAnalysis.setAnalyzer(cameraExecutor, new QRCodeAnalyzer((qrCode, boundingBox, imgWidth, imgHeight) -> {
                    runOnUiThread(() -> handleQRCodeResult(qrCode, boundingBox, imgWidth, imgHeight));
                }));

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();

                // Gán vào biến camera để dùng cho Auto-Zoom
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("QRScan", "Lỗi mở camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Xử lý kết quả quét và tự động Zoom
    private void handleQRCodeResult(String qrCodeText, Rect boundingBox, int imgWidth, int imgHeight) {
        if (isProcessing) return;

        // Xử lý AUTO-ZOOM nếu mã QR ở quá xa
        if (camera != null && boundingBox != null) {
            float qrWidth = boundingBox.width();
            float ratio = qrWidth / imgWidth; // Tính tỷ lệ mã QR so với khung hình

            // Nếu mã QR chiếm ít hơn 25% chiều rộng màn hình -> Đang ở xa
            if (ratio < 0.25f) {
                ZoomState zoomState = camera.getCameraInfo().getZoomState().getValue();
                if (zoomState != null) {
                    float currentZoom = zoomState.getZoomRatio();
                    float maxZoom = zoomState.getMaxZoomRatio();

                    // Tính toán mức zoom mới sao cho mã QR to lên khoảng 40% màn hình
                    float targetZoom = currentZoom * (0.4f / ratio);
                    if (targetZoom > maxZoom) targetZoom = maxZoom;

                    // Thực hiện hiệu ứng Zoom
                    camera.getCameraControl().setZoomRatio(targetZoom);

                    isProcessing = true; // Khóa lại để tránh gọi nhiều lần

                    // Đợi 300ms để người dùng nhìn thấy hiệu ứng camera kéo gần vào QR, sau đó mới gọi API
                    viewFinder.postDelayed(() -> processApiCheckIn(qrCodeText), 300);
                    return;
                }
            }
        }

        // Nếu mã QR đã đủ to và rõ, xử lý luôn
        isProcessing = true;
        processApiCheckIn(qrCodeText);
    }

    // Xử lý logic gọi API
    private void processApiCheckIn(String qrCodeText) {
        // Trong hệ thống mới, qrCodeText chính là "sessionId_timestamp" hoặc tương tự
        // Tùy thuộc vào cách bạn sinh mã QR ở CreateQRActivity

        try {
            // Giả sử mã QR của bạn có dạng: "3fa85f64-5717..._1678889990" (được phân cách bởi dấu _)
            // Hoặc dạng: "3fa85f64-5717...|1678889990" (phân cách bởi dấu |)

            // Ở đây mình dùng chung ký tự phân cách là dấu | hoặc _
            String[] parts = qrCodeText.split("[|_]");

            if (parts.length >= 1) {
                // sessionId là chuỗi (String), không ép kiểu sang int nữa
                String sessionId = parts[0];

                // Gọi ViewModel với 2 tham số đều là String
                // Truyền toàn bộ qrCodeText lên làm token để Backend xác thực
                qrViewModel.processQRCode(sessionId, qrCodeText);
            } else {
                Toast.makeText(this, "Mã QR rỗng hoặc không hợp lệ!", Toast.LENGTH_SHORT).show();
                isProcessing = false;
            }
        } catch (Exception e) {
            Toast.makeText(this, "Mã QR không đúng định dạng của trường!", Toast.LENGTH_SHORT).show();
            isProcessing = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần quyền Camera để quét mã", Toast.LENGTH_SHORT).show();
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