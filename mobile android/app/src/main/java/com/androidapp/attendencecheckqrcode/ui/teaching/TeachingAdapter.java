package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;

import java.util.ArrayList;
import java.util.List;

public class TeachingAdapter extends RecyclerView.Adapter<TeachingAdapter.TeachingViewHolder> {

    private List<Classroom> mListClass;

    public TeachingAdapter(List<Classroom> mListClass) {
        this.mListClass = mListClass != null ? mListClass : new ArrayList<>();
    }

    public void updateData(List<Classroom> list) {
        this.mListClass.clear();
        if (list != null) {
            this.mListClass.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TeachingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_teaching, parent, false);
        return new TeachingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeachingViewHolder holder, int position) {
        Classroom item = mListClass.get(position);
        if (item == null) return;

        holder.tvClassName.setText(item.getGroupName() != null ? item.getGroupName() : "N/A");
        holder.tvSubjectCode.setText(item.getCourseCode() != null ? item.getCourseCode() : "N/A");
        holder.tvClassCode.setText(item.getClassCode() != null ? item.getClassCode() : "N/A");
        holder.tvLecturer.setText(item.getLecturerName() != null ? item.getLecturerName() : "Giảng viên");
        holder.tvStudentCount.setText(item.getApprovedStudentCount() + " SV >");

        holder.tvBadge.setText("Giáo viên");

        if (item.getWeeklySchedules() != null && !item.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule firstSchedule = item.getWeeklySchedules().get(0);

            if (firstSchedule.getStartTime() != null && firstSchedule.getEndTime() != null) {
                try {
                    String start = firstSchedule.getStartTime();
                    String end = firstSchedule.getEndTime();

                    if (start.length() >= 5) start = start.substring(0, 5);
                    if (end.length() >= 5) end = end.substring(0, 5);

                    holder.tvTime.setText(start + " - " + end);
                } catch (Exception e) {
                    holder.tvTime.setText("--:-- - --:--");
                }
            } else {
                holder.tvTime.setText("Chưa xếp lịch");
            }
        } else {
            holder.tvTime.setText("Chưa xếp lịch");
        }

        String room = item.getRoom() != null ? item.getRoom() : "";
        String campus = item.getLocationDisplay() != null ? item.getLocationDisplay() : "";
        if (!room.isEmpty() && !campus.isEmpty()) {
            holder.tvRoom.setText(room + " - " + campus);
        } else {
            holder.tvRoom.setText(room.isEmpty() ? "Trực tuyến" : room);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TeachingDetailActivity.class);
            intent.putExtra("classData", item);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mListClass != null ? mListClass.size() : 0;
    }

    public static class TeachingViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvBadge, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount;

        public TeachingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        }
    }
}