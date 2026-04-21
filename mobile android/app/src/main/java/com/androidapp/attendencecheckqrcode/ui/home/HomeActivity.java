package com.androidapp.attendencecheckqrcode.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.androidapp.attendencecheckqrcode.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// Import các màn hình con
import com.androidapp.attendencecheckqrcode.ui.clazz.CreateClassActivity;
import com.androidapp.attendencecheckqrcode.ui.clazz.ClassListActivity;
import com.androidapp.attendencecheckqrcode.ui.settings.SettingsActivity;
import com.androidapp.attendencecheckqrcode.ui.stats.StatsActivity;
import com.androidapp.attendencecheckqrcode.ui.qr.QRScanActivity;
import com.androidapp.attendencecheckqrcode.ui.teaching.TeachingListActivity;

public class HomeActivity extends AppCompatActivity {

    private FloatingActionButton fabQR;
    private BottomNavigationView bottomNavigationView;

    private CardView btnJoin, btnCreate, btnClass, btnTeaching;
    private TextView tvSummary, tvDate, tvGreeting, tvName;
    private ImageView btnNotification;

    // Khai báo ViewModel
    private HomeViewModel homeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupUI();

        // Khởi tạo ViewModel và Lắng nghe dữ liệu
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        observeViewModel();

        setupListeners();
    }

    private void initViews() {
        fabQR = findViewById(R.id.fabQR);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        btnJoin = findViewById(R.id.btnJoin);
        btnCreate = findViewById(R.id.btnCreate);
        btnClass = findViewById(R.id.btnClass);
        btnTeaching = findViewById(R.id.btnTeaching);

        tvSummary = findViewById(R.id.tvSummary);
        btnNotification = findViewById(R.id.btnNotification);
        tvDate = findViewById(R.id.tvDate);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvName = findViewById(R.id.tvName);
    }

    private void setupUI() {
        String text = "Bạn có <font color='#FFEB3B'><b>2 lớp học</b></font> hôm nay";
        tvSummary.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));

        bottomNavigationView.setBackground(null);
        if (bottomNavigationView.getMenu().size() >= 3) {
            bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        }
    }

    // --- MVVM: LẮNG NGHE SỰ THAY ĐỔI TỪ VIEWMODEL ---
    private void observeViewModel() {
        // Cập nhật Lời chào
        homeViewModel.getGreetingText().observe(this, greeting -> {
            if (tvGreeting != null) tvGreeting.setText(greeting);
        });

        // Cập nhật Ngày giờ
        homeViewModel.getCurrentDateText().observe(this, date -> {
            if (tvDate != null) tvDate.setText(date);
        });

        // Cập nhật Tên User
        homeViewModel.getUserName().observe(this, name -> {
            if (tvName != null) tvName.setText(name);
        });
    }

    private void setupListeners() {
        fabQR.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, QRScanActivity.class));
        });

        btnJoin.setOnClickListener(v ->
                Toast.makeText(HomeActivity.this, "Chức năng: Tham gia lớp", Toast.LENGTH_SHORT).show()
        );

        btnCreate.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CreateClassActivity.class));
        });

        btnClass.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, ClassListActivity.class));
        });

        btnTeaching.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, TeachingListActivity.class));
        });

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
            return false;
        });
    }
}