package com.ptithcm.attendapp.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ptithcm.attendapp.R;
import com.ptithcm.attendapp.model.UpcomingSessionItem;
import java.util.List;

public class HomeScheduleAdapter extends RecyclerView.Adapter<HomeScheduleAdapter.ViewHolder> {

    private List<UpcomingSessionItem> list;

    public HomeScheduleAdapter(List<UpcomingSessionItem> list) {
        this.list = list;
    }

    public void updateData(List<UpcomingSessionItem> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_class, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingSessionItem item = list.get(position);

        holder.tvSubjectName.setText(item.getGroupName() != null ? item.getGroupName() : "Tên môn học");
        holder.tvRoom.setText(item.getRoom() != null ? item.getRoom() : "Chưa có phòng");

        String start = item.getStartTime() != null ? item.getStartTime() : "--:--";
        String end = item.getEndTime() != null ? item.getEndTime() : "--:--";
        holder.tvTime.setText(start + " - " + end);
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvSubjectName, tvRoom;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvRoom = itemView.findViewById(R.id.tvRoom);
        }
    }
}