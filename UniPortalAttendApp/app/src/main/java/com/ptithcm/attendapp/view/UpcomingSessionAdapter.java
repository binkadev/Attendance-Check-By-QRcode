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
import java.text.SimpleDateFormat;
import java.util.List;

public class UpcomingSessionAdapter extends RecyclerView.Adapter<UpcomingSessionAdapter.ViewHolder> {

    private List<UpcomingSessionItem> list;
    private FragmentManager fragmentManager;

    public UpcomingSessionAdapter(List<UpcomingSessionItem> list, FragmentManager fragmentManager) {
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upcoming_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UpcomingSessionItem item = list.get(position);

        holder.tvGroupName.setText(item.getGroupName() != null ? item.getGroupName() : "Lớp học");
        holder.tvRoom.setText(item.getRoom() != null ? item.getRoom() : "Chưa xếp phòng");

        String start = formatTimeUTCtoLocal(item.getStartAt());
        String end = formatTimeUTCtoLocal(item.getEndAt());
        holder.tvTime.setText(start + " - " + end);

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
        TextView tvGroupName, tvTime, tvRoom;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGroupName = itemView.findViewById(R.id.tvGroupName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
        }
    }

    private String formatTimeUTCtoLocal(String utcTimeString) {
        if (utcTimeString == null || utcTimeString.isEmpty()) return "--:--";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            inputFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = inputFormat.parse(utcTimeString);
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm");
            outputFormat.setTimeZone(java.util.TimeZone.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return "--:--";
        }
    }
}