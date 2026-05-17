package com.ptithcm.attendapp.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.api.RetrofitClient;
import com.ptithcm.attendapp.model.GroupDetail;
import com.ptithcm.attendapp.model.ClassItem;
import android.content.Intent;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ptithcm.attendapp.model.AttendanceHistoryResponse;
import com.ptithcm.attendapp.model.AttendanceHistoryItem;
import java.util.ArrayList;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassDetailFragment extends Fragment {

    private String groupId = ""; // ID của lớp học cần tải chi tiết

    // Các biến giao diện
    private ImageView btnBack;
    private TextView tvCourseClassCode, tvClassName, tvSemesterYear;
    private TextView tvTimeLabel, tvTimeValue, tvRoomValue;
    private TextView tvLecturerName;

    private ClassItem currentClass;
    private View btnAttendQR;

    private RecyclerView rvAttendanceHistory;
    private TextView tvAttendanceSummary;
    private AttendanceHistoryAdapter adapter;
    private List<AttendanceHistoryItem> attendanceList = new ArrayList<>();

    // Hàm khởi tạo Fragment có kèm ID truyền vào
    public static ClassDetailFragment newInstance(String groupId) {
        ClassDetailFragment fragment = new ClassDetailFragment();
        Bundle args = new Bundle();
        args.putString("GROUP_ID", groupId);
        fragment.setArguments(args);
        return fragment;
    }

//    public static ClassDetailFragment newInstance(ClassItem item) {
//        ClassDetailFragment fragment = new ClassDetailFragment();
//        Bundle args = new Bundle();
//        args.putSerializable("CLASS_DATA", item);
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            groupId = getArguments().getString("GROUP_ID", "");
//            // Mở gói dữ liệu ra
//            // Kiểm tra: Nếu là Android 13 (TIRAMISU) trở lên thì dùng cách mới
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
//                currentClass = getArguments().getSerializable("CLASS_DATA", ClassItem.class);
//            }
//            // Nếu là Android cũ thì dùng cách ép kiểu truyền thống
//            else {
//                currentClass = (ClassItem) getArguments().getSerializable("CLASS_DATA");
//            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_class_detail, container, false);
        initViews(view);
        setupListeners();

        // Gọi API nếu có ID
        if (!groupId.isEmpty()) {
            fetchClassDetail();
        } else {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID lớp học", Toast.LENGTH_SHORT).show();
        }

//        // NẾU NHẬN ĐƯỢC DỮ LIỆU TỪ MÀN HÌNH TRƯỚC -> HIỂN THỊ NGAY LẬP TỨC!
//        if (currentClass != null) {
//            updateUIFromList();
//        }
//
//        // Tùy chọn: Bạn có thể tiếp tục gọi hàm fetchClassDetail() ở đây để
//        // lấy thêm những thông tin mà màn danh sách không có (nếu Backend sửa xong lỗi 403).

        return view;
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);

        // Nhớ đặt ID cho các TextView này trong file XML của bạn nhé!
        tvCourseClassCode = view.findViewById(R.id.tvCourseClassCode);
        tvClassName = view.findViewById(R.id.tvClassName);
        tvSemesterYear = view.findViewById(R.id.tvSemesterYear);
        tvTimeValue = view.findViewById(R.id.tvTimeValue);
        tvRoomValue = view.findViewById(R.id.tvRoomValue);

        // Phần API thiếu, tạm thời ánh xạ để set cứng
        tvLecturerName = view.findViewById(R.id.tvLecturerName);
        btnAttendQR = view.findViewById(R.id.btnAttendQR);

        rvAttendanceHistory = view.findViewById(R.id.rvAttendanceHistory);
        tvAttendanceSummary = view.findViewById(R.id.tvAttendanceSummary);

        // Setup RecyclerView
        rvAttendanceHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AttendanceHistoryAdapter(attendanceList);
        rvAttendanceHistory.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack(); // Quay lại màn hình trước
            }
        });

//        // THÊM SỰ KIỆN CLICK CHO NÚT QUÉT QR Ở ĐÂY
//        if (btnAttendQR != null) {
//            btnAttendQR.setOnClickListener(v -> {
//                Intent intent = new Intent(requireActivity(), QRScannerActivity.class);
//
//                // Lấy Text trực tiếp từ giao diện (Giao diện hiện gì thì gửi cái đó đi)
//                String className = tvClassName.getText().toString();
//                String classTime = tvTimeValue.getText().toString();
//
//                // Gắn dữ liệu vào chuyến xe Intent
//                intent.putExtra("CLASS_NAME", className);
//                intent.putExtra("CLASS_TIME", classTime);
//
//                // Khởi hành qua màn hình QR
//                startActivity(intent);
//            });
//        }

        // THÊM SỰ KIỆN CLICK CHO NÚT QUÉT QR Ở ĐÂY
        if (btnAttendQR != null) {
            btnAttendQR.setOnClickListener(v -> {
                // Lấy Text trực tiếp từ giao diện
                String className = tvClassName.getText().toString();
                String classTime = tvTimeValue.getText().toString();

                // Gọi hàm launchQRScanner CỦA MainActivity và truyền cả 2 thông tin đi
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).launchQRScanner(className, classTime);
                }
            });
        }
    }

    // Hàm hiển thị giao diện dùng dữ liệu tức thì từ Danh sách
    private void updateUIFromList() {
        String courseCode = currentClass.getCourseCode() != null ? currentClass.getCourseCode() : "N/A";
        String classCode = currentClass.getClassCode() != null ? currentClass.getClassCode() : "";
        if(tvCourseClassCode != null) tvCourseClassCode.setText(courseCode + " • " + classCode);

        if(tvClassName != null) tvClassName.setText(currentClass.getGroupName() != null ? currentClass.getGroupName() : "Chưa có tên");

        if(tvRoomValue != null) tvRoomValue.setText(currentClass.getRoom() != null ? currentClass.getRoom() : "Chưa xếp phòng");

        if(tvLecturerName != null) tvLecturerName.setText(currentClass.getLecturerName() != null ? currentClass.getLecturerName() : "Chưa cập nhật GV");

        // Format thời gian từ danh sách (ví dụ startTime, endTime)
        if(tvTimeValue != null) {
            String start = currentClass.getStartTime() != null ? currentClass.getStartTime() : "N/A";
            tvTimeValue.setText(start);
        }

        if(tvSemesterYear != null) {
            tvSemesterYear.setText("Đang tải học kỳ..."); // Nếu danh sách ko có học kỳ thì để tạm
        }
    }

    private void fetchClassDetail() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");
        if (token.isEmpty()) return;

        // In ra Logcat cái ID đang được truyền đi để kiểm tra xem có bị sai hoặc null không
        android.util.Log.d("API_DETAIL", "Đang gọi API cho Group ID: " + groupId);

        RetrofitClient.getApiService().getGroupDetail("Bearer " + token, groupId)
                .enqueue(new Callback<GroupDetail>() {
                    @Override
                    public void onResponse(Call<GroupDetail> call, Response<GroupDetail> response) {
                        if (!isAdded() || getContext() == null) return;

                        if (response.isSuccessful() && response.body() != null) {
                            GroupDetail detail = response.body();
                            updateUI(detail);
                        } else {
                            // 🚨 ĐOẠN CODE NÀY ĐÃ ĐƯỢC THÊM LOG ĐỂ BẮT BỆNH 🚨
                            try {
                                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                                int errorCode = response.code();

                                // In lỗi đỏ lòm ra Logcat để bạn dễ nhìn
                                android.util.Log.e("API_DETAIL", "Lỗi Server - Mã " + errorCode + ": " + errorBody);

                                Toast.makeText(getContext(), "Lỗi tải dữ liệu (Mã " + errorCode + ")", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<GroupDetail> call, Throwable t) {
                        if (!isAdded() || getContext() == null) return;
                        android.util.Log.e("API_DETAIL", "Lỗi Mạng / Crash API: " + t.getMessage());
                        Toast.makeText(getContext(), "Lỗi mạng", Toast.LENGTH_SHORT).show();
                    }
                });

        // GỌI API LỊCH SỬ ĐIỂM DANH
        fetchAttendanceHistory();
    }

    private void fetchAttendanceHistory() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UniPortalPrefs", Context.MODE_PRIVATE);
        String token = prefs.getString("ACCESS_TOKEN", "");

        RetrofitClient.getApiService().getAttendanceHistory("Bearer " + token, groupId, 0, 10) // Lấy 10 buổi gần nhất
                .enqueue(new Callback<AttendanceHistoryResponse>() {
                    @Override
                    public void onResponse(Call<AttendanceHistoryResponse> call, Response<AttendanceHistoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            attendanceList.clear();
                            attendanceList.addAll(response.body().getItems());
                            adapter.notifyDataSetChanged();

                            // Tính toán số buổi có mặt
                            int presentCount = 0;
                            for(AttendanceHistoryItem item : attendanceList) {
                                if("PRESENT".equalsIgnoreCase(item.getAttendanceStatus())) {
                                    presentCount++;
                                }
                            }
                            int total = response.body().getTotalElements();
                            tvAttendanceSummary.setText("Tổng: " + presentCount + "/" + total + " buổi");
                        }
                    }

                    @Override
                    public void onFailure(Call<AttendanceHistoryResponse> call, Throwable t) {
                        // Xử lý lỗi mạng
                    }
                });
    }

    private void updateUI(GroupDetail detail) {
        // 1. Mã môn và Nhóm
        String courseCode = detail.getCourseCode() != null ? detail.getCourseCode() : "N/A";
        String classCode = detail.getClassCode() != null ? detail.getClassCode() : "";
        if(tvCourseClassCode != null) tvCourseClassCode.setText(courseCode + " • " + classCode);

        // 2. Tên môn
        if(tvClassName != null) tvClassName.setText(detail.getName() != null ? detail.getName() : "Chưa có tên lớp");

        // 3. Học kỳ - Năm học
        String semester = detail.getSemester() != null ? detail.getSemester() : "Học kỳ N/A";
        String year = detail.getAcademicYear() != null ? detail.getAcademicYear() : "N/A";
        if(tvSemesterYear != null) tvSemesterYear.setText(semester + " - Năm học " + year);

        // 4. Phòng học
        if(tvRoomValue != null) tvRoomValue.setText(detail.getRoom() != null ? detail.getRoom() : "Chưa cập nhật");

        // 5. Lịch học (Lấy buổi học đầu tiên trong danh sách mảng)
        if (tvTimeValue != null) {
            List<GroupDetail.WeeklySchedule> schedules = detail.getWeeklySchedules();
            if (schedules != null && !schedules.isEmpty()) {
                GroupDetail.WeeklySchedule firstSchedule = schedules.get(0);
                String day = firstSchedule.getDayOfWeek() != null ? firstSchedule.getDayOfWeek() : "";
                String start = firstSchedule.getStartTime() != null ? firstSchedule.getStartTime() : "";
                String end = firstSchedule.getEndTime() != null ? firstSchedule.getEndTime() : "";

                // Format: Thứ 2, 08:00 - 09:30
                tvTimeValue.setText(day + ", " + start + " - " + end);
            } else {
                tvTimeValue.setText("Chưa cập nhật lịch");
            }
        }

        // 6. Giảng viên.
        if(tvLecturerName != null) {
            tvLecturerName.setText(detail.getLecturerName() != null ? detail.getLecturerName() : "Chưa cập nhật");
        }
    }
}