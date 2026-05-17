package com.ptithcm.attendapp.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.UpcomingSessionItem;
import java.util.List;

public class SessionTodayAdapter extends RecyclerView.Adapter<SessionTodayAdapter.ViewHolder> {

    private List<UpcomingSessionItem> list;
    private FragmentManager fragmentManager;

    public SessionTodayAdapter(List<UpcomingSessionItem> list, FragmentManager fragmentManager) {
        this.list = list;
        this.fragmentManager = fragmentManager;
    }

    public void updateData(List<UpcomingSessionItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_today, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingSessionItem item = list.get(position);

        // 1. Gán TÊN MÔN HỌC (Group Name)
        holder.tvSubjectName.setText(item.getGroupName() != null ? item.getGroupName() : "Tên môn học");

        // 2. Gán TÊN BUỔI HỌC (Ví dụ: Buổi 1, Buổi 2)
        if (item.getSessionName() != null && !item.getSessionName().isEmpty()) {
            holder.tvSessionName.setText(item.getSessionName());
            holder.tvSessionName.setVisibility(View.VISIBLE);
        } else {
            holder.tvSessionName.setVisibility(View.GONE); // Ẩn đi nếu không có data
        }

        // 3. Gán MÃ LỚP / NHÓM (API hiện tại không có mã môn "MAT101", có thể để tạm chữ "Lớp/Nhóm")
        holder.tvGroupCode.setText("Lớp/Nhóm");

        // 4. Phòng học và Tên Giảng viên
        holder.tvRoom.setText(item.getRoom() != null ? item.getRoom() : "Chưa xếp phòng");
        holder.tvTeacher.setText(item.getLecturerName() != null ? item.getLecturerName() : "Đang cập nhật GV");

        // 5. Giờ học
        String start = item.getStartTime() != null ? item.getStartTime() : "--:--";
        String end = item.getEndTime() != null ? item.getEndTime() : "--:--";
        holder.tvTime.setText(start + " - " + end);

        // 6. Xử lý màu sắc Trạng thái (Badge) dựa vào attendanceStatus
        String status = item.getAttendanceStatus();

        if (status == null || status.equalsIgnoreCase("NOT_OPEN")) {
            // 1. CHƯA MỞ PHIÊN -> Màu Xám
            holder.viewColorStrip.setBackgroundColor(Color.parseColor("#9CA3AF"));
            holder.tvStatusBadge.setText("Chưa mở phiên");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#6B7280"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_gray);

        } else if (status.equalsIgnoreCase("PENDING")) {
            // 2. ĐANG MỞ PHIÊN (CẦN ĐIỂM DANH) -> Màu Cam
            holder.viewColorStrip.setBackgroundColor(Color.parseColor("#F59E0B"));
            holder.tvStatusBadge.setText("Đang điểm danh");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#D97706"));
            // Lưu ý: Nhớ tạo thêm 1 file bg_badge_orange_light.xml nhé
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_orange_light);

        } else if (status.equalsIgnoreCase("PRESENT") || status.equalsIgnoreCase("ATTENDED")) {
            // 3. THÀNH CÔNG (CÓ MẶT) -> Màu Xanh lá
            holder.viewColorStrip.setBackgroundColor(Color.parseColor("#10B981"));
            holder.tvStatusBadge.setText("✓ Thành công");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light);

        } else if (status.equalsIgnoreCase("LATE")) {
            // 4. ĐI TRỄ -> Màu Đỏ Nhạt / Đỏ Cam
            holder.viewColorStrip.setBackgroundColor(Color.parseColor("#EF4444"));
            holder.tvStatusBadge.setText("⏰ Đi trễ");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light);

        } else if (status.equalsIgnoreCase("ABSENT")) {
            // 5. VẮNG MẶT -> Màu Đỏ Đậm
            holder.viewColorStrip.setBackgroundColor(Color.parseColor("#B91C1C"));
            holder.tvStatusBadge.setText("✕ Vắng mặt");
            holder.tvStatusBadge.setTextColor(Color.parseColor("#B91C1C"));
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light);
        }



        // 7. Sự kiện Click vào Lớp học
        holder.itemView.setOnClickListener(v -> {
            if (fragmentManager != null && item.getGroupId() != null) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ClassDetailFragment.newInstance(item.getGroupId()))
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewColorStrip;
        TextView tvGroupCode, tvStatusBadge, tvSubjectName, tvSessionName, tvTime, tvRoom, tvTeacher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewColorStrip = itemView.findViewById(R.id.viewColorStrip);
            tvGroupCode = itemView.findViewById(R.id.tvGroupCode);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);

            // Ánh xạ 2 trường Môn học và Buổi học
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSessionName = itemView.findViewById(R.id.tvSessionName);

            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
        }
    }
}