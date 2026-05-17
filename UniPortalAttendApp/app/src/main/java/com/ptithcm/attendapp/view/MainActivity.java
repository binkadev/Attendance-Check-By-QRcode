package com.ptithcm.attendapp.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ptithcm.attendapp.R;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabScanQR;

    // 1. Khai báo bộ phóng (Launcher) để lắng nghe kết quả trả về
    private ActivityResultLauncher<Intent> qrScanLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. Bắt buộc khởi tạo Launcher trước khi màn hình được tạo xong
        initQRCodeLauncher();

        initViews();
        setupListeners();

        // Load HomeFragment mặc định khi vừa mở app lên
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }
    }

//    private void initQRCodeLauncher() {
//        // Đăng ký bộ lắng nghe kết quả từ Activity khác
//        qrScanLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    // Kiểm tra xem tín hiệu trả về có phải là THÀNH CÔNG (RESULT_OK) không
//                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
//                        boolean isSuccess = result.getData().getBooleanExtra("SCAN_SUCCESS", false);
//
//                        if (isSuccess) {
//                            // Tắt sáng tab hiện tại trên Bottom Navigation (vì đang ở màn hình Thành công)
//                            bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
//
//                            // Bơm Fragment Kết quả điểm danh vào màn hình
//                            getSupportFragmentManager()
//                                    .beginTransaction()
//                                    .replace(R.id.fragment_container, new AttendanceSuccessFragment())
//                                    .addToBackStack(null) // Cho phép bấm phím Back để quay lại
//                                    .commit();
//                        }
//                    }
//                }
//        );
//    }
private void initQRCodeLauncher() {
    // Đăng ký bộ lắng nghe kết quả từ Activity khác
    qrScanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                // Kiểm tra xem tín hiệu trả về có phải là THÀNH CÔNG (RESULT_OK) không
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    boolean isSuccess = data.getBooleanExtra("SCAN_SUCCESS", false);
                    String type = data.getStringExtra("QR_TYPE");

                    if (isSuccess && "ATTEND".equals(type)) {
                        String sessionId = data.getStringExtra("SCANNED_SESSION_ID"); // Lấy ID thật từ Scanner trả về

                        Bundle bundle = new Bundle();
                        bundle.putString("SCANNED_SESSION_ID", sessionId);

                        AttendanceSuccessFragment successFragment = new AttendanceSuccessFragment();
                        successFragment.setArguments(bundle);

                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, successFragment)
                                .addToBackStack(null)
                                .commitAllowingStateLoss();
                        
//                        // Tắt sáng tab hiện tại trên Bottom Navigation
//                        bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
//
//                        // BƯỚC 1: Lấy thông tin từ Intent (đã được đẩy vào ở QRScannerActivity)
//                        String className = data.getStringExtra("CLASS_NAME");
//                        String classCode = data.getStringExtra("CLASS_CODE");
//                        String room = data.getStringExtra("ROOM");
//
//                        // BƯỚC 2: Chuẩn bị Gói dữ liệu (Bundle) để gửi sang Fragment
//                        Bundle bundle = new Bundle();
//                        bundle.putString("CLASS_NAME", className != null ? className : "Môn học chưa rõ");
//                        bundle.putString("CLASS_CODE", classCode != null ? classCode : "N/A");
//                        bundle.putString("ROOM", room != null ? room : "Chưa rõ phòng");
//
//                        // BƯỚC 3: Tạo Fragment và nhét Bundle vào
//                        AttendanceSuccessFragment successFragment = new AttendanceSuccessFragment();
//                        successFragment.setArguments(bundle);
//
//                        // Bơm Fragment Kết quả điểm danh vào màn hình
//                        getSupportFragmentManager()
//                                .beginTransaction()
//                                .replace(R.id.fragment_container, successFragment)
//                                .addToBackStack(null) // Cho phép bấm phím Back để quay lại
//                                .commit();
                    }
                }
            }
    );
}

    private void initViews() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fabScanQR = findViewById(R.id.fabScanQR);

        // Bỏ qua item giữa để không thể click vào placeholder của nút QR
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
    }

    private void setupListeners() {
        // Nút FAB nổi ở giữa: Quét mã QR nhanh
        fabScanQR.setOnClickListener(v -> {
            launchQRScanner("Điểm quét ...", "Real time ...");
        });

        // Xử lý chuyển tab Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Bật lại tính năng sáng màu cho tab khi click vào
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

            int itemId = item.getItemId();
            Fragment selectedFragment = null;

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_classes) {
                selectedFragment = new MyClassesFragment();
            } else if (itemId == R.id.nav_notifications) {
                Toast.makeText(this, "Chuyển sang Thông báo", Toast.LENGTH_SHORT).show();
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Chuyển sang Cá nhân", Toast.LENGTH_SHORT).show();
                selectedFragment = new ProfileFragment();
                //return true; // Tạm thời return true nếu chưa có Fragment
            }

            // Bơm Fragment tương ứng vào Container
            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    // 3. Tạo một hàm public để mở Camera.
    // Các Fragment con (như ClassDetailFragment) có thể gọi ké hàm này.
//    public void launchQRScanner(String className) {
//        Intent intent = new Intent(this, QRScannerActivity.class);
//        // intent.putExtra("CLASS_NAME", className);
//        // Dùng launcher để phóng Activity thay vì startActivity()
//        qrScanLauncher.launch(intent);
//    }
    // Trong MainActivity.java
    public void launchQRScanner(String className, String classTime) {
        Intent intent = new Intent(this, QRScannerActivity.class);

        // Gắn dữ liệu vào chuyến xe Intent tại đây
        intent.putExtra("CLASS_NAME", className);
        intent.putExtra("CLASS_TIME", classTime);

        // Phóng Activity bằng Launcher để nó nhớ đường vòng về
        qrScanLauncher.launch(intent);
    }

    public void loadFragment(Fragment fragment) {
//        getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .commit();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss(); // <-- SỬA DÒNG NÀY CHỐNG CRASH
    }


}