package com.ptithcm.attendapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.ClassItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<ClassItem> classList;
    private FragmentManager fragmentManager;

    public ClassAdapter(List<ClassItem> classList, FragmentManager fragmentManager) {
        this.classList = classList;
        this.fragmentManager = fragmentManager;
    }

    public void updateData(List<ClassItem> newList) {
        this.classList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_joined_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        ClassItem item = classList.get(position);

        // 1. Thông tin cơ bản
        holder.tvClassCode.setText((item.getCourseCode() != null ? item.getCourseCode() : "N/A") + " • " + (item.getClassCode() != null ? item.getClassCode() : ""));
        holder.tvClassName.setText(item.getGroupName() != null ? item.getGroupName() : "Chưa có tên");
        holder.tvLocation.setText(item.getRoom() != null ? item.getRoom() : "Chưa xếp phòng");
        holder.tvLecturer.setText(item.getLecturerName() != null ? item.getLecturerName() : "Chưa có GV");
        holder.tvAttendanceStatus.setText(item.getMyMemberStatus() != null ? item.getMyMemberStatus() : "Đang học");

        // 2. Xử lý Thời gian (Cắt chuỗi ISO 8601: 2026-04-26T17:33:06.717Z)
        String rawStartTime = item.getStartTime();
        String rawEndTime = item.getEndTime();

        if (rawStartTime != null && !rawStartTime.isEmpty()) {
            try {
                // Bộ đọc chuỗi từ API (UTC)
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date dateObj = inputFormat.parse(rawStartTime);

                // Nếu có giờ kết thúc thì parse luôn
                Date endDateObj = null;
                if (rawEndTime != null && !rawEndTime.isEmpty()) {
                    endDateObj = inputFormat.parse(rawEndTime);
                }

                // Bộ xuất chuỗi ra giao diện (Giờ VN)
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("vi", "VN")); // Thứ
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()); // Ngày
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault()); // Giờ

                // Đổ dữ liệu lên UI
                holder.tvDayOfWeek.setText(dayFormat.format(dateObj)); // VD: Thứ Hai
                holder.tvDate.setText(dateFormat.format(dateObj));     // VD: 26/04/2026

                String timeStr = timeFormat.format(dateObj);
                if (endDateObj != null) {
                    timeStr += " - " + timeFormat.format(endDateObj); // VD: 17:33 - 19:30
                }
                holder.tvTime.setText(timeStr);

            } catch (ParseException e) {
                e.printStackTrace();
                // Báo lỗi nội bộ nếu parse không được
                holder.tvDayOfWeek.setText("Thứ N/A");
                holder.tvDate.setText("Ngày N/A");
                holder.tvTime.setText("Giờ N/A");
            }
        } else {
            holder.tvDayOfWeek.setText("Chưa cập nhật");
            holder.tvDate.setText("");
            holder.tvTime.setText("Chưa xếp giờ");
        }

        // 3. Xử lý Tổng buổi học (API này không có, đành set cứng cảnh báo)
        holder.tvTotalSessions.setText("Tổng buổi: N/A");

        // Gắn sự kiện click
//        holder.itemView.setOnClickListener(v -> {
//            if (fragmentManager != null) {
//                fragmentManager.beginTransaction()
//                        .replace(R.id.fragment_container, ClassDetailFragment.newInstance(item))
//                        .addToBackStack(null)
//                        .commit();
//            }
//        });

        // Gắn sự kiện click: CHỈ TRUYỀN ID SANG THAY VÌ TRUYỀN OBJECT - DUNG API CLASS DETAIL
        holder.itemView.setOnClickListener(v -> {
            if (fragmentManager != null) {
                String idToPass = item.getGroupId(); // Rút lấy cái ID

                fragmentManager.beginTransaction()
                        // Gọi newInstance với String ID
                        .replace(R.id.fragment_container, ClassDetailFragment.newInstance(idToPass))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return classList != null ? classList.size() : 0;
    }

    static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassCode, tvClassName, tvLocation, tvLecturer, tvAttendanceStatus;
        TextView tvTime, tvDayOfWeek, tvDate, tvTotalSessions; // THÊM CÁC BIẾN MỚI

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
            tvAttendanceStatus = itemView.findViewById(R.id.tvAttendanceStatus);

            // ÁNH XẠ CÁC VIEW MỚI (Lấy đúng ID từ file item_joined_class.xml)
            tvTime = itemView.findViewById(R.id.tvTime);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotalSessions = itemView.findViewById(R.id.tvTotalSessions);
        }
    }
}