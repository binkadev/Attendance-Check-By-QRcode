package com.ptithcm.attendapp.view; // Sửa package theo cấu trúc thư mục của bạn

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.NotificationItem;
import java.util.List;

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

        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());

        // (Tùy chọn) Format lại chữ createdAt cho đẹp, ví dụ: "2026-04-30" -> "Hôm nay"
        holder.tvTime.setText(item.getCreatedAt().substring(0, 10));

        // Logic UI cho thông báo Chưa đọc vs Đã đọc
        if (!item.isRead()) {
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvTime.setTextColor(Color.parseColor("#A6192E")); // Đỏ đỏ
        } else {
            holder.viewUnreadIndicator.setVisibility(View.INVISIBLE); // Ẩn vạch đỏ
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvTime.setTextColor(Color.parseColor("#9CA3AF")); // Xám xám
        }

        // Tùy biến Icon theo Severity (INFO, WARNING...)
        if ("WARNING".equalsIgnoreCase(item.getSeverity())) {
            holder.ivIcon.setImageResource(R.drawable.ic_warning); // Nhớ đổi tên icon tương ứng
            // holder.ivIcon.setColorFilter(Color.parseColor("#EF4444"));
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_announcement);
        }

        // Click vào 1 thông báo
        holder.itemView.setOnClickListener(v -> listener.onClick(item, position));
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