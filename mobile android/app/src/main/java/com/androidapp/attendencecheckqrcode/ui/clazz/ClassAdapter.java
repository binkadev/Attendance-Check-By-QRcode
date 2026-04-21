package com.androidapp.attendencecheckqrcode.ui.clazz; // Đảm bảo đúng package của bạn

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

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<Classroom> mListClass;

    public ClassAdapter(List<Classroom> mListClass) {
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
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        Classroom item = mListClass.get(position);
        if (item == null) return;

        holder.tvClassName.setText(item.getGroupName() != null ? item.getGroupName() : "Chưa có tên");
        holder.tvSubjectCode.setText(item.getCourseCode() != null ? item.getCourseCode() : "N/A");
        holder.tvClassCode.setText(item.getClassCode() != null ? item.getClassCode() : "N/A");
        holder.tvLecturer.setText(item.getLecturerName() != null ? item.getLecturerName() : "Chưa phân công");
        holder.tvStudentCount.setText(item.getApprovedStudentCount() + " SV >");

        if ("LECTURER".equals(item.getMyRole())) {
            holder.tvBadge.setText("Giảng viên");
             holder.tvBadge.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.app_primary));
        } else {
            holder.tvBadge.setText("Sinh viên");
        }

        if (item.getWeeklySchedules() != null && !item.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule firstSchedule = item.getWeeklySchedules().get(0);

            if (firstSchedule.getStartTime() != null && firstSchedule.getEndTime() != null) {
                try {
                    String start = firstSchedule.getStartTime();
                    String end = firstSchedule.getEndTime();

                    // Cắt lấy định dạng HH:mm
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
        } else if (!room.isEmpty()) {
            holder.tvRoom.setText(room);
        } else {
            holder.tvRoom.setText("Chưa có phòng");
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ClassDetailActivity.class);
            intent.putExtra("classData", item);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mListClass != null ? mListClass.size() : 0;
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvBadge, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvBadge = itemView.findViewById(R.id.tvBadge); // Ánh xạ tvBadge
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        }
    }
}