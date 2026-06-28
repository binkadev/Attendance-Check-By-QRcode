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

        String createdAt = item.getCreatedAt();
        if (createdAt != null && createdAt.length() >= 10) {
            holder.tvTime.setText(createdAt.substring(0, 10));
        } else {
            holder.tvTime.setText(createdAt != null ? createdAt : "Không rõ thời gian");
        }

        // 1. Logic UI cho thông báo Chưa đọc vs Đã đọc (Nên để lên trước)
        if (!item.isRead()) {
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.tvTime.setTextColor(Color.parseColor("#A6192E")); // Đỏ
        } else {
            holder.viewUnreadIndicator.setVisibility(View.INVISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.tvTime.setTextColor(Color.parseColor("#9CA3AF")); // Xám
        }

        // 2. Logic Tùy biến Icon và Màu chữ theo Severity (GỘP CHUNG VÀO ĐÂY)
        String severity = item.getSeverity();
        if ("CRITICAL".equalsIgnoreCase(severity) || "ERROR".equalsIgnoreCase(severity)) {
            holder.ivIcon.setImageResource(R.drawable.ic_error);
            holder.tvTitle.setTextColor(Color.parseColor("#DC2626"));
        } else if ("WARNING".equalsIgnoreCase(severity)) {
            holder.ivIcon.setImageResource(R.drawable.ic_warning);
            holder.tvTitle.setTextColor(Color.parseColor("#D97706"));
        } else {
            // Mặc định cho INFO hoặc các loại khác
            holder.ivIcon.setImageResource(R.drawable.ic_announcement);
            holder.tvTitle.setTextColor(Color.BLACK); // Hoặc màu mặc định của app bạn
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