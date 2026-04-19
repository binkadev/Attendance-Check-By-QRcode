package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;

import java.util.List;

public class ClassAdapter extends RecyclerView.Adapter<ClassAdapter.ClassViewHolder> {

    private List<Attendance.Classroom> mListClass;

    public ClassAdapter(List<Attendance.Classroom> mListClass) {
        this.mListClass = mListClass;
    }

    public void updateData(List<Attendance.Classroom> list) {
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
        TextView tvClassName, tvSubjectCode, tvClassCode, tvLecturer, tvTime, tvRoom, tvStudentCount;
        public ClassViewHolder(@NonNull View itemView) {
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