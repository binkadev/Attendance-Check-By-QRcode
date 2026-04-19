package com.androidapp.attendencecheckqrcode.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.ui.auth.LoginActivity;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    // Views
    private ImageView btnBack;
    private LinearLayout btnLogout;
    private SwitchCompat swNotification, swDarkMode;

    // Avatar & Name
    private CircleImageView imgAvatar;
    private LinearLayout btnChangeAvatar; // Nút máy ảnh
    private LinearLayout btnEditName;     // Nút sửa tên
    private TextView tvName;

    // Các dòng chức năng (Click vào để mở dialog)
    private LinearLayout rowPhone, rowAddress, rowChangePass, rowLanguage;

    // Các TextView hiển thị giá trị (để cập nhật lại text sau khi sửa)
    private TextView tvPhoneValue, tvAddressValue, tvLanguageValue;

    // Launcher để mở thư viện ảnh và nhận kết quả
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // Khai báo ở trên cùng class
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Khởi tạo trong onCreate
        tokenManager = new TokenManager(this);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Đăng ký Launcher nhận ảnh TRƯỚC KHI initViews
        registerImagePicker();

        initViews();
        setupListeners();
    }

    // --- 1. ĐĂNG KÝ NHẬN KẾT QUẢ TỪ THƯ VIỆN ẢNH ---
    private void registerImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Lấy đường dẫn ảnh được chọn (Uri)
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            // Cập nhật lên CircleImageView
                            imgAvatar.setImageURI(imageUri);
                            Toast.makeText(this, "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();

                            // Lưu ý: Ở ứng dụng thực tế, bạn cần lưu Uri này vào SharedPreferences
                            // hoặc upload lên Server để lần sau mở app nó vẫn còn.
                        }
                    }
                }
        );
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        swNotification = findViewById(R.id.swNotification);
        swDarkMode = findViewById(R.id.swDarkMode);

        // Ánh xạ Avatar & Tên
        imgAvatar = findViewById(R.id.imgAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnEditName = findViewById(R.id.btnEditName);
        tvName = findViewById(R.id.tvName);

        // Ánh xạ các Row (Cần đảm bảo file XML đã có các ID này)
        rowPhone = findViewById(R.id.rowPhone);
        rowAddress = findViewById(R.id.rowAddress);
        rowChangePass = findViewById(R.id.rowChangePass);

        // Bạn cần thêm ID này vào file XML cho dòng Ngôn ngữ nếu chưa có
        // Ví dụ: android:id="@+id/rowLanguage"
        rowLanguage = findViewById(R.id.rowLanguage); // Giả sử bạn đã đặt ID này trong XML

        // Ánh xạ các TextView hiển thị giá trị (Cần thêm ID vào XML cho các TextView này)
        // Ví dụ: android:id="@+id/tvPhoneValue" cho cái TextView hiện số 0909...
        tvPhoneValue = findViewById(R.id.tvPhoneValue);
        tvAddressValue = findViewById(R.id.tvAddressValue);
        tvLanguageValue = findViewById(R.id.tvLanguageValue); // TextView hiện chữ "Tiếng Việt"
    }

    private void setupListeners() {
        // 1. Back & Logout & Switch
        btnBack.setOnClickListener(v -> finish());

        // --- XỬ LÝ ĐỔI ẢNH ĐẠI DIỆN ---
        btnChangeAvatar.setOnClickListener(v -> {
            // Tạo Intent mở thư viện ảnh
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            // Mở launcher
            imagePickerLauncher.launch(intent);
        });

        // --- XỬ LÝ SỬA TÊN ---
        btnEditName.setOnClickListener(v -> {
            showEditDialog("Đổi tên hiển thị", tvName, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        });

        // 2. Notification & DarkMode
        swNotification.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, "Thông báo: " + (isChecked ? "BẬT" : "TẮT"), Toast.LENGTH_SHORT).show());

        swDarkMode.setOnCheckedChangeListener((buttonView, isChecked) ->
                Toast.makeText(this, "Chế độ tối: " + (isChecked ? "Bật" : "Tắt"), Toast.LENGTH_SHORT).show());

        // 3. SỬA THÔNG TIN CÁ NHÂN (Click row -> Hiện dialog nhập)
        if (rowPhone != null) {
            rowPhone.setOnClickListener(v -> showEditDialog("Cập nhật Số điện thoại", tvPhoneValue, InputType.TYPE_CLASS_PHONE));
        }

        if (rowAddress != null) {
            rowAddress.setOnClickListener(v -> showEditDialog("Cập nhật Địa chỉ", tvAddressValue, InputType.TYPE_CLASS_TEXT));
        }

        // 4. ĐỔI NGÔN NGỮ
        if (rowLanguage != null) {
            rowLanguage.setOnClickListener(v -> showLanguageDialog());
        }

        // 5. ĐỔI MẬT KHẨU
        if (rowChangePass != null) {
            rowChangePass.setOnClickListener(v -> showChangePasswordDialog());
        }

        // 6. Đăng xuất
        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    // --- HÀM 1: DIALOG NHẬP LIỆU ĐỂ SỬA THÔNG TIN ---
    private void showEditDialog(String title, TextView targetTextView, int inputType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Tạo ô nhập liệu
        final EditText input = new EditText(this);
        input.setInputType(inputType);
        if (targetTextView != null) {
            input.setText(targetTextView.getText().toString()); // Điền sẵn text cũ
        }

        // Thêm margin cho đẹp
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(50, 20, 50, 20);
        container.addView(input, params);

        builder.setView(container);

        // Nút Lưu
        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (!newValue.isEmpty() && targetTextView != null) {
                targetTextView.setText(newValue);
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút Hủy
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // --- HÀM 2: DIALOG ĐỔI NGÔN NGỮ ---
    private void showLanguageDialog() {
        final String[] languages = {"Tiếng Việt", "English"};
        // Kiểm tra xem đang chọn ngôn ngữ nào (Logic đơn giản dựa trên text hiện tại)
        int checkedItem = 0;
        if (tvLanguageValue != null && tvLanguageValue.getText().toString().equals("English")) {
            checkedItem = 1;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn ngôn ngữ");
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selectedLang = languages[which];
            if (tvLanguageValue != null) {
                tvLanguageValue.setText(selectedLang);
            }
            Toast.makeText(this, "Đã đổi sang: " + selectedLang, Toast.LENGTH_SHORT).show();

            // Ở đây chỉ thay đổi giao diện hiển thị text.
            dialog.dismiss();
        });
        builder.show();
    }

    // ---  DIALOG ĐỔI MẬT KHẨU ---
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi mật khẩu");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etOldPass = new EditText(this);
        etOldPass.setHint("Mật khẩu cũ");
        etOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etOldPass);

        final EditText etNewPass = new EditText(this);
        etNewPass.setHint("Mật khẩu mới");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etNewPass);

        final EditText etConfirmPass = new EditText(this);
        etConfirmPass.setHint("Nhập lại mật khẩu mới");
        etConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(etConfirmPass);

        builder.setView(layout);

        builder.setPositiveButton("Xác nhận", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Override sự kiện nút Xác nhận để validate dữ liệu
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();

            if (TextUtils.isEmpty(oldPass) || TextUtils.isEmpty(newPass)) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show();
            } else {
                // GỌI API ĐỔI PASS Ở ĐÂY
                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }

    // --- HÀM ĐĂNG XUẤT ---
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {

                    // 1.  Xóa sạch chìa khóa trong máy
                    TokenManager tokenManager = new TokenManager(this);
                    tokenManager.clearAll();

                    // 2. Chuyển về Login và xóa sạch lịch sử các màn hình trước
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}