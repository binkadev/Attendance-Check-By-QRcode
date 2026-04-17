package com.androidapp.attendencecheckqrcode.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.models.entities.User;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Import các màn hình con
import com.androidapp.attendencecheckqrcode.ui.clazz.CreateClassActivity;
import com.androidapp.attendencecheckqrcode.ui.clazz.ClassListActivity;
import com.androidapp.attendencecheckqrcode.ui.settings.SettingsActivity;
import com.androidapp.attendencecheckqrcode.ui.stats.StatsActivity;
import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;
import com.androidapp.attendencecheckqrcode.ui.teaching.TeachingListActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private FloatingActionButton fabQR;
    private BottomNavigationView bottomNavigationView;

    private CardView btnJoin, btnCreate, btnClass, btnTeaching;
    private CardView itemClass1, itemClass2;

    private TextView tvSummary;
    private TextView tvDate;
    private TextView tvGreeting;
    private TextView tvName; // <--- THÊM BIẾN NÀY ĐỂ HIỂN THỊ TÊN
    private ImageView btnNotification;

    private Handler handler = new Handler();
    private Runnable timeUpdater;

    // Biến lưu thông tin User hiện tại
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        getUserDataFromIntent(); // <--- LẤY DỮ LIỆU USER
        setupUI();
        setupListeners();

        startUpdatingTime();
    }

    private void initViews() {
        fabQR = findViewById(R.id.fabQR);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        btnJoin = findViewById(R.id.btnJoin);
        btnCreate = findViewById(R.id.btnCreate);
        btnClass = findViewById(R.id.btnClass);
        btnTeaching = findViewById(R.id.btnTeaching);

        itemClass1 = findViewById(R.id.itemClass1);
        itemClass2 = findViewById(R.id.itemClass2);

        tvSummary = findViewById(R.id.tvSummary);
        btnNotification = findViewById(R.id.btnNotification);
        tvDate = findViewById(R.id.tvDate);
        tvGreeting = findViewById(R.id.tvGreeting);

        tvName = findViewById(R.id.tvName);
    }

    // Hàm nhận dữ liệu User từ màn hình Login gửi sang
    private void getUserDataFromIntent() {
        // 1. Khởi tạo TokenManager
        TokenManager tokenManager = new TokenManager(this);

        // 2. Lấy tên đã lưu trong máy (mặc định là "Khách" nếu chưa có)
        String displayName = tokenManager.getUserName();

        // 3. Kiểm tra Intent (phòng trường hợp vừa đăng nhập xong có dữ liệu mới)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("currentUser")) {
            currentUser = (User) intent.getSerializableExtra("currentUser");
            if (currentUser != null) {
                displayName = currentUser.getFullName();
                // Lưu lại vào máy cho chắc chắn
                tokenManager.saveUserData(currentUser.getFullName(), currentUser.getEmail());
            }
        }

        // 4. Hiển thị lên TextView
        if (tvName != null) {
            tvName.setText(displayName);
        }
    }

    private void setupUI() {
        String text = "Bạn có <font color='#FFEB3B'><b>2 lớp học</b></font> hôm nay";
        tvSummary.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));

        bottomNavigationView.setBackground(null);
        if (bottomNavigationView.getMenu().size() >= 3) {
            bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        }
    }

    private void updateCurrentDate() {
        Date currentDate = new Date();
        // Định dạng: "Thứ Hai 15/01 - 12:34"
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd/MM - HH:mm", new Locale("vi", "VN"));
        String dateString = sdf.format(currentDate);
        String finalString = "📅 HÔM NAY, " + dateString.toUpperCase();

        if (tvDate != null) tvDate.setText(finalString);
    }

    private void setupGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greetingText;

        if (hour >= 6 && hour < 12) {
            greetingText = "Chào buổi sáng 🌤️";
        } else if (hour >= 12 && hour < 18) {
            greetingText = "Chào buổi chiều ☀️";
        } else {
            greetingText = "Chào buổi tối 🌙";
        }

        if (tvGreeting != null) {
            tvGreeting.setText(greetingText);
        }
    }

    private void startUpdatingTime() {
        timeUpdater = new Runnable() {
            @Override
            public void run() {
                updateCurrentDate();
                setupGreeting();
                handler.postDelayed(this, 60000); // Cập nhật mỗi 1 phút
            }
        };
        handler.post(timeUpdater);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && timeUpdater != null) {
            handler.removeCallbacks(timeUpdater);
        }
    }

    private void setupListeners() {
        fabQR.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, QRScanActivity.class);
            // QUAN TRỌNG: Truyền User sang
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        btnJoin.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Chức năng: Tham gia lớp", Toast.LENGTH_SHORT).show()
        );

        btnCreate.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CreateClassActivity.class);
            // --- QUAN TRỌNG: Phải truyền User hiện tại sang ---
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // Đi tới danh sách lớp học (tư cách sinh viên)
        btnClass.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ClassListActivity.class);
            // --- THÊM DÒNG NÀY ---
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // Đi tới danh sách lớp dạy (tư cách giảng viên - người tạo lớp)
        btnTeaching.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TeachingListActivity.class);
            // --- THÊM DÒNG NÀY ---
            intent.putExtra("currentUser", currentUser);
            startActivity(intent);
        });

        // ... các listener khác giữ nguyên
        btnNotification.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Bạn không có thông báo mới", Toast.LENGTH_SHORT).show()
        );

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_stat) {
                startActivity(new Intent(HomeActivity.this, StatsActivity.class));
                return true;
            }
            if (id == R.id.nav_setting) {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                return true;
            }
            // ...
            return false;
        });
    }
}