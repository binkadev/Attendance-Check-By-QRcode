package com.ptithcm.attendapp.view;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.AttendanceHistoryItem;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private List<AttendanceHistoryItem> list;

    public AttendanceHistoryAdapter(List<AttendanceHistoryItem> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceHistoryItem item = list.get(position);

        holder.tvSessionName.setText(item.getSessionName() != null ? item.getSessionName() : "Buổi học");

        //holder.tvSessionTime.setText(item.getStartTime() != null ? item.getStartTime().substring(11,16) : "--:--");
        String localStartTime = formatTimeUTCtoLocal(item.getStartTime());
        String localEndTime = formatTimeUTCtoLocal(item.getEndTime());
        String localCheckInTime = formatTimeUTCtoLocal(item.getCheckInAt());

        // Gắn lên giao diện
        holder.tvSessionTime.setText(localStartTime + " - " + localEndTime);

        // Gắn thời gian checkin
        if (item.getCheckInAt() != null) {
            holder.tvCheckinTime.setText("Lúc " + localCheckInTime);
        }

        // Xử lý Ngày tháng (Ví dụ "2026-05-04" -> "04/05")
        if (item.getSessionDate() != null && item.getSessionDate().length() >= 10) {
            String[] parts = item.getSessionDate().split("-");
            if(parts.length == 3) {
                holder.tvDateMonth.setText(parts[2] + "/" + parts[1]);
            }
        }

        // Logic phân loại Có mặt / Vắng mặt
        String status = item.getAttendanceStatus() != null ? item.getAttendanceStatus() : "";
        if ("PRESENT".equalsIgnoreCase(status)) {
            // Có mặt (Xanh lá)
            holder.llDateBadge.setBackgroundResource(R.drawable.bg_badge_green_light);
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#10B981"));
            holder.tvDateMonth.setTextColor(Color.parseColor("#10B981"));

            holder.tvStatusBadge.setText("✓ Có mặt");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_light);
            holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"));

            if(item.getCheckInAt() != null) {
                holder.tvCheckinTime.setText("Lúc " + localCheckInTime); // cap nhat theo mui gio real
                holder.tvCheckinTime.setVisibility(View.VISIBLE);
            } else {
                holder.tvCheckinTime.setVisibility(View.GONE);
            }
        } else {
            // Vắng mặt (Đỏ)
            holder.llDateBadge.setBackgroundResource(R.drawable.bg_badge_red_light);
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#EF4444"));
            holder.tvDateMonth.setTextColor(Color.parseColor("#EF4444"));

            holder.tvStatusBadge.setText("× Vắng");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_light);
            holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"));

            holder.tvCheckinTime.setText("Không phép");
            holder.tvCheckinTime.setTextColor(Color.parseColor("#EF4444"));
            holder.tvCheckinTime.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout llDateBadge;
        TextView tvDayOfWeek, tvDateMonth, tvSessionName, tvSessionTime, tvStatusBadge, tvCheckinTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            llDateBadge = itemView.findViewById(R.id.llDateBadge);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDateMonth = itemView.findViewById(R.id.tvDateMonth);
            tvSessionName = itemView.findViewById(R.id.tvSessionName);
            tvSessionTime = itemView.findViewById(R.id.tvSessionTime);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvCheckinTime = itemView.findViewById(R.id.tvCheckinTime);
        }
    }

    // Hàm phép thuật đổi UTC sang Giờ Local
    private String formatTimeUTCtoLocal(String utcTimeString) {
        if (utcTimeString == null || utcTimeString.isEmpty()) return "--:--";
        try {
            // 1. Dạy cho Java biết định dạng của Backend (có chữ T và chữ Z)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC")); // Bắt buộc: Ép nó hiểu đây là giờ UTC 0

            // Dịch chuỗi thành đối tượng Date
            java.util.Date date = inputFormat.parse(utcTimeString);

            // 2. Dạy cho Java cách xuất ra màn hình (Chỉ lấy Giờ:Phút)
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");
            // Tự động lấy múi giờ hiện tại của cái điện thoại (VN là GMT+7)
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());

            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            // Nếu lỗi, fallback về cắt chuỗi như cũ
            if (utcTimeString.length() >= 16) {
                return utcTimeString.substring(11, 16);
            }
            return "--:--";
        }
    }
}