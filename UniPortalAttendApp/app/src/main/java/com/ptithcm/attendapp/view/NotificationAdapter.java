package com.ptithcm.attendapp.view;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> list;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onClick(NotificationItem item, int position);
    }

    public NotificationAdapter(List<NotificationItem> list, OnItemClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = list.get(position);

        String type = item.getType() != null ? item.getType().toUpperCase() : "";
        String severity = item.getSeverity() != null ? item.getSeverity().toUpperCase() : "";

        // --- BẮT ĐẦU PHẦN CẬP NHẬT LOGIC BODY ---

        // Set tiêu đề an toàn
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Thông báo");

        // Xử lý nội dung (body) hiển thị bản xem trước
        String bodyText = item.getBody();
        if (bodyText == null || bodyText.trim().isEmpty()) {
            // Tự động tạo nội dung giả định nếu Server quên gửi body
            if (type.contains("CHECKIN_SUCCESS")) {
                bodyText = "Hệ thống đã ghi nhận điểm danh thành công. Nhấn để xem chi tiết thông tin phòng và môn học.";
            } else if (type.contains("FRAUD") || type.contains("WARNING") || severity.equals("CRITICAL")) {
                bodyText = "Phát hiện dấu hiệu bất thường. Vui lòng kiểm tra ngay!";
            } else {
                bodyText = "Nhấn vào đây để xem chi tiết thông báo.";
            }
        }
        holder.tvBody.setText(bodyText);

        // --- KẾT THÚC PHẦN CẬP NHẬT LOGIC BODY ---

        // Set thời gian tương đối
        holder.tvTime.setText(getTimeAgo(item.getCreatedAt()));

        // Mặc định reset màu tiêu đề về đen xanh để tránh bị ám màu khi View tái sử dụng
        holder.tvTitle.setTextColor(Color.parseColor("#0A2540"));

        // 1. Phân biệt UI: TIN CHƯA ĐỌC vs TIN ĐÃ ĐỌC
        MaterialCardView cardView = (MaterialCardView) holder.itemView;

        if (!item.isRead()) {
            // TIN CHƯA ĐỌC: Hiện vạch đỏ, chữ đậm, thời gian màu đỏ, nền xám nhạt
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvTime.setTextColor(Color.parseColor("#A6192E"));
            cardView.setCardBackgroundColor(Color.parseColor("#F9FAFB")); // Trắng xám nhẹ
        } else {
            // TIN ĐÃ ĐỌC: Ẩn vạch đỏ, chữ thường, thời gian màu xám, nền trắng
            holder.viewUnreadIndicator.setVisibility(View.INVISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvTime.setTextColor(Color.parseColor("#9CA3AF"));
            cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
        }

        // 2. LOGIC TÙY BIẾN ICON & MÀU SẮC DỰA VÀO BẢN THIẾT KẾ
        if (type.contains("CHECKIN_SUCCESS")) {
            // 2.1: Điểm danh thành công (Xanh lá)
            holder.ivIcon.setImageResource(R.drawable.ic_person_check);
            holder.ivIcon.setColorFilter(Color.parseColor("#10B981"));
            holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_green_light);

        } else if (severity.equals("CRITICAL") || severity.equals("ERROR") || severity.equals("WARNING") || type.contains("FRAUD") || type.contains("ABSENT")) {
            // 2.2: Cảnh báo / Vắng mặt / Gian lận (Đỏ)
            holder.ivIcon.setImageResource(R.drawable.ic_warning);
            holder.ivIcon.setColorFilter(Color.parseColor("#EF4444"));
            holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_red_light_solid);
            // Riêng cảnh báo thì cho chữ tiêu đề màu đỏ luôn cho ngầu
            holder.tvTitle.setTextColor(Color.parseColor("#DC2626"));

        } else if (type.contains("SCHEDULE") || type.contains("ROOM") || type.contains("CALENDAR")) {
            // 2.3: Thay đổi lịch học / Phòng học (Cam)
            holder.ivIcon.setImageResource(R.drawable.ic_calendar);
            holder.ivIcon.setColorFilter(Color.parseColor("#F59E0B"));
            holder.ivIcon.setBackgroundResource(R.drawable.bg_circle_orange_light);

        } else {
            // 2.4: Mặc định / Hệ thống (Xanh dương)
            holder.ivIcon.setImageResource(R.drawable.ic_announcement);
            holder.ivIcon.setColorFilter(Color.parseColor("#3B82F6"));
            holder.ivIcon.setBackgroundResource(R.drawable.bg_icon_circle_blue);
        }

        // 3. Bắt sự kiện Click
        holder.itemView.setOnClickListener(v -> listener.onClick(item, position));
    }

    // Thuật toán tính toán thời gian
    private String getTimeAgo(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Không rõ";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = sdf.parse(dateString);
            if (date == null) return dateString.substring(0, 10);

            long time = date.getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < 60 * 1000) {
                return "Vừa xong";
            } else if (diff < 60 * 60 * 1000) {
                return (diff / (60 * 1000)) + " phút trước";
            } else if (diff < 24 * 60 * 60 * 1000) {
                return (diff / (60 * 60 * 1000)) + " giờ trước";
            } else if (diff < 48 * 60 * 60 * 1000) {
                return "Hôm qua";
            } else {
                SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi"));
                return outFormat.format(date);
            }
        } catch (Exception e) {
            return dateString.length() >= 10 ? dateString.substring(0, 10) : dateString;
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View viewUnreadIndicator;
        ImageView ivIcon;
        TextView tvTitle, tvTime, tvBody;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvBody = itemView.findViewById(R.id.tvBody);
        }
    }
}