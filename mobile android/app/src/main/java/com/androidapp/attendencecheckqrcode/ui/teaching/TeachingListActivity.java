package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.adapters.TeachingAdapter;
//import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
//import com.androidapp.attendencecheckqrcode.models.entities.User;
//import com.androidapp.attendencecheckqrcode.utils.MockData;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.adapters.TeachingAdapter;
import com.androidapp.attendencecheckqrcode.ui.clazz.ClassViewModel;

import java.util.ArrayList;

public class TeachingListActivity extends AppCompatActivity {
    private RecyclerView rcvTeachingList;
    private TeachingAdapter teachingAdapter;
    private ImageView btnBack;

    private ClassViewModel classViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teaching_list);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);

        initViews();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        rcvTeachingList = findViewById(R.id.rcvTeachingList);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        teachingAdapter = new TeachingAdapter(new ArrayList<>());
        rcvTeachingList.setLayoutManager(new LinearLayoutManager(this));
        rcvTeachingList.setAdapter(teachingAdapter);
    }

    private void loadData() {
        classViewModel.getTeachingClasses().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    // Hiện loading UI
                    break;
                case SUCCESS:
                    if (response.data != null) {
                        teachingAdapter.setData(response.data);
                        if (response.data.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa tạo lớp nào", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case ERROR:
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}