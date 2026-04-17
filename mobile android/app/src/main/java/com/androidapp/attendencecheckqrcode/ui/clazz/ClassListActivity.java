package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidapp.attendencecheckqrcode.R;
import com.androidapp.attendencecheckqrcode.adapters.ClassAdapter;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.models.entities.User;
import com.androidapp.attendencecheckqrcode.utils.MockData;

import java.util.ArrayList;
import java.util.List;


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
import com.androidapp.attendencecheckqrcode.adapters.ClassAdapter;
import com.androidapp.attendencecheckqrcode.models.entities.User;

import java.util.ArrayList;

public class ClassListActivity extends AppCompatActivity {

    private RecyclerView rcvClassList;
    private ClassAdapter classAdapter;
    private ImageView btnBack;
    private ProgressBar progressBar; // Bạn nên thêm một ProgressBar vào file XML nhé

    private ClassViewModel classViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_list);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        classViewModel = new ViewModelProvider(this).get(ClassViewModel.class);

        initViews();
        setupRecyclerView();

        btnBack.setOnClickListener(v -> finish());

        loadData();
    }

    private void initViews() {
        rcvClassList = findViewById(R.id.rcvClassList);
        btnBack = findViewById(R.id.btnBack);
        // Nhớ thêm <ProgressBar android:id="@+id/progressBar" ... /> vào activity_class_list.xml
        // progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        classAdapter = new ClassAdapter(new ArrayList<>());
        rcvClassList.setLayoutManager(new LinearLayoutManager(this));
        rcvClassList.setAdapter(classAdapter);
    }

    private void loadData() {
        classViewModel.getEnrolledClasses().observe(this, response -> {
            switch (response.status) {
                case LOADING:
                    // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    // if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (response.data != null) {
                        classAdapter.setData(response.data);
                        if (response.data.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa tham gia lớp nào.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case ERROR:
                    // if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}