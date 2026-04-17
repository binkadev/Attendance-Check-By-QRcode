package com.androidapp.attendencecheckqrcode.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.ui.teaching.TeachingDetailActivity;
import java.util.List;

public class TeachingAdapter extends RecyclerView.Adapter<TeachingAdapter.TeachingViewHolder> {
    private List<Attendance.Classroom> mListClass;

    public TeachingAdapter(List<Attendance.Classroom> mListClass) { this.mListClass = mListClass; }

    public void setData(List<Attendance.Classroom> list) {
        this.mListClass = list;
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
        Attendance.Classroom item = mListClass.get(position);
        if (item == null) return;

        holder.tvClassName.setText(item.getClassName());
        holder.tvSubjectCode.setText(item.getSubjectCode());
        holder.tvClassCode.setText(item.getClassCode());
        holder.tvLecturer.setText(item.getLecturerName());
        holder.tvTime.setText(item.getDayOfWeek() + " | " + item.getTimeSlot());
        holder.tvRoom.setText(item.getRoom());
        holder.tvStudentCount.setText(item.getTotalStudents() + " SV >");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TeachingDetailActivity.class);
            intent.putExtra("classData", item); // Truyền Object
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return mListClass != null ? mListClass.size() : 0; }

    public static class TeachingViewHolder extends RecyclerView.ViewHolder {
        TextView tvClassName, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount;
        public TeachingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvSubjectCode = itemView.findViewById(R.id.tvSubjectCode);
            tvClassCode = itemView.findViewById(R.id.tvClassCode);
            tvLecturer = itemView.findViewById(R.id.tvLecturer);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvStudentCount = itemView.findViewById(R.id.tvStudentCount);
        }
    }
}