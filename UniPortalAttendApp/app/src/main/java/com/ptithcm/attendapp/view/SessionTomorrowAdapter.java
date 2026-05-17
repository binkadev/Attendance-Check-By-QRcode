package com.ptithcm.attendapp.view;

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

public class SessionTomorrowAdapter extends RecyclerView.Adapter<SessionTomorrowAdapter.ViewHolder> {

    private List<UpcomingSessionItem> list;
    private FragmentManager fragmentManager;

    public SessionTomorrowAdapter(List<UpcomingSessionItem> list, FragmentManager fragmentManager) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_tomorrow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingSessionItem item = list.get(position);

        // Đã sửa lại: tvSubjectName sẽ hứng Tên nhóm/Tên môn học (groupName)
        holder.tvSubjectName.setText(item.getGroupName() != null ? item.getGroupName() : "Tên lớp học");

        // Gán tên buổi học (Ví dụ: Buổi 1)
        if (item.getSessionName() != null && !item.getSessionName().isEmpty()) {
            holder.tvSessionName.setText(item.getSessionName());
            holder.tvSessionName.setVisibility(View.VISIBLE);
        } else {
            holder.tvSessionName.setVisibility(View.GONE);
        }

        holder.tvRoom.setText(item.getRoom() != null ? item.getRoom() : "Chưa xếp phòng");

        // Giao diện ngày mai chỉ hiện giờ bắt đầu
        holder.tvTime.setText(item.getStartTime() != null ? item.getStartTime() : "--:--");

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
        TextView tvSubjectName, tvSessionName, tvTime, tvRoom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvSessionName = itemView.findViewById(R.id.tvSessionName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
        }
    }
}