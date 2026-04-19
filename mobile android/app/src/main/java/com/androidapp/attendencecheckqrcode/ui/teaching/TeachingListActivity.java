package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidapp.attendencecheckqrcode.R;

import java.util.ArrayList;

public class TeachingListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rcvTeachingList;
    private ProgressBar progressBar;

    private TeachingAdapter adapter;
    private TeachingViewModel teachingViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teaching_list);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupRecyclerView();

        teachingViewModel = new ViewModelProvider(this).get(TeachingViewModel.class);
        observeViewModel();
        teachingViewModel.fetchTeachingClasses();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rcvTeachingList = findViewById(R.id.rcvTeachingList); // Nhớ đổi ID trong XML cho khớp
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new TeachingAdapter(new ArrayList<>());
        rcvTeachingList.setLayoutManager(new LinearLayoutManager(this));
        rcvTeachingList.setAdapter(adapter);
    }

    private void observeViewModel() {
        teachingViewModel.getTeachingClassesResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    if(progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    rcvTeachingList.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    rcvTeachingList.setVisibility(View.VISIBLE);
                    if (response.data != null && !response.data.isEmpty()) {
                        adapter.updateData(response.data);
                    } else {
                        Toast.makeText(this, "Bạn chưa có lớp giảng nào!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    if(progressBar != null) progressBar.setVisibility(View.GONE);
                    rcvTeachingList.setVisibility(View.VISIBLE);
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}