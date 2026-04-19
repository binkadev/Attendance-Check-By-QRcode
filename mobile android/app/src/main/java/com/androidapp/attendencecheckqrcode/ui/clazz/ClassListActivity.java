package com.androidapp.attendencecheckqrcode.ui.clazz;

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

public class ClassListActivity extends AppCompatActivity {

    private ImageView btnBack;
    private RecyclerView rcvClassList;
    private ProgressBar progressBar;

    private ClassAdapter adapter;
    private ClassViewModel classViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupRecyclerView();

        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);
        observeViewModel();
        classViewModel.fetchEnrolledClasses();

        btnBack.setOnClickListener(v -> finish());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rcvClassList = findViewById(R.id.rcvClassList);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        adapter = new ClassAdapter(new ArrayList<>());
        rcvClassList.setLayoutManager(new LinearLayoutManager(this));
        rcvClassList.setAdapter(adapter);
    }

    private void observeViewModel() {
        classViewModel.getEnrolledClassesResult().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    progressBar.setVisibility(View.VISIBLE);
                    rcvClassList.setVisibility(View.GONE);
                    break;
                case SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    rcvClassList.setVisibility(View.VISIBLE);
                    if (response.data != null && !response.data.isEmpty()) {
                        adapter.updateData(response.data);
                    } else {
                        Toast.makeText(this, "Bạn chưa tham gia môn học nào!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case ERROR:
                    progressBar.setVisibility(View.GONE);
                    rcvClassList.setVisibility(View.VISIBLE);
                    Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }
}