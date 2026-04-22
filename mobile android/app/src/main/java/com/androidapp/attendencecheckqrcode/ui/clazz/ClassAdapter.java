package com.androidapp.attendencecheckqrcode.ui.clazz;

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

        // --- XỬ LÝ LỊCH HỌC (THỨ VÀ GIỜ) ---
        if (item.getWeeklySchedules() != null && !item.getWeeklySchedules().isEmpty()) {
            Classroom.WeeklySchedule firstSchedule = item.getWeeklySchedules().get(0);

            // 1. Dịch Thứ tiếng Anh sang tiếng Việt
            String engDay = firstSchedule.getDayOfWeek();
            holder.tvDayDate.setText(translateDay(engDay)); // Hiển thị "Thứ 2", "Thứ 3"...

            // 2. Hiển thị Giờ
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
            holder.tvDayDate.setText("Chưa xếp lịch");
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

    // Hàm phụ trợ dịch tiếng Anh sang tiếng Việt
    private String translateDay(String englishDay) {
        if (englishDay == null) return "N/A";
        switch (englishDay.toUpperCase()) {
            case "MONDAY": return "Thứ 2";
            case "TUESDAY": return "Thứ 3";
            case "WEDNESDAY": return "Thứ 4";
            case "THURSDAY": return "Thứ 5";
            case "FRIDAY": return "Thứ 6";
            case "SATURDAY": return "Thứ 7";
            case "SUNDAY": return "Chủ Nhật";
            default: return englishDay;
        }
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvBadge, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount, tvDayDate;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvBadge = itemView.findViewById(R.id.tvBadge);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
            tvDayDate = itemView.findViewById(R.id.tvDayDate); // Ánh xạ tvDayDate
        }
    }
}