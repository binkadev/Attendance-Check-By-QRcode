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

        // ========================================================
        // ĐÃ SỬA LỖI TẠI ĐÂY: DÙNG CHUỖI ISO 8601 CỦA API DANH SÁCH
        // ========================================================
        String startTimeStr = item.getStartTime();
        String endTimeStr = item.getEndTime();

        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            // Lấy Thứ từ startTime
            String thu = extractDayOfWeek(startTimeStr);
            // Dòng này rất quan trọng để nó hiện trên Danh sách
            if(holder.tvDayDate != null) holder.tvDayDate.setText(thu);

            // Lấy Giờ từ startTime và endTime
            String gioBatDau = extractTime(startTimeStr);
            String gioKetThuc = extractTime(endTimeStr);

            if (!gioKetThuc.isEmpty()) {
                holder.tvTime.setText(gioBatDau + " - " + gioKetThuc);
            } else {
                holder.tvTime.setText(gioBatDau);
            }
        } else {
            // Dự phòng nếu Backend trả về rỗng
            if(holder.tvDayDate != null) holder.tvDayDate.setText("Chưa xếp lịch");
            holder.tvTime.setText("--:--");
        }
        // ========================================================

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

    // --- 2 HÀM PHỤ TRỢ CẮT CHUỖI THỜI GIAN ---

    // Lấy ra Thứ (Tiếng Việt) từ chuỗi 2026-04-22T15:22:49.605Z
    private String extractDayOfWeek(String isoDateString) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(isoDateString);

            java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", new java.util.Locale("vi", "VN"));
            String dayName = dayFormat.format(date);
            return dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
        } catch (Exception e) {
            return "N/A";
        }
    }

    // Lấy ra Giờ Phút (HH:mm) từ chuỗi 2026-04-22T15:22:49.605Z
    private String extractTime(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            java.util.Date date = sdf.parse(isoDateString);

            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
            return timeFormat.format(date);
        } catch (Exception e) {
            return "";
        }
    }

    public static class TeachingViewHolder extends RecyclerView.ViewHolder {
        // NHỚ THÊM tvDayDate VÀO KHAI BÁO
        TextView tvClassName, tvBadge, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount, tvDayDate;

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
            tvDayDate = itemView.findViewById(R.id.tvDayDate); // Nhớ ánh xạ cái này để không bị Crash!
        }
    }
}