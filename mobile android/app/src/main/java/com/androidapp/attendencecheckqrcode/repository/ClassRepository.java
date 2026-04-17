package com.androidapp.attendencecheckqrcode.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.api.ApiClient;
import com.androidapp.attendencecheckqrcode.api.ApiService;
import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassRepository {
    private final ApiService apiService;

    public ClassRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getEnrolledClasses() {
        MutableLiveData<Resource<List<Attendance.Classroom>>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.getEnrolledClasses().enqueue(new Callback<List<Attendance.Classroom>>() {
            @Override
            public void onResponse(Call<List<Attendance.Classroom>> call, Response<List<Attendance.Classroom>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    data.setValue(Resource.error("Lỗi lấy dữ liệu: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Attendance.Classroom>> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getTeachingClasses() {
        MutableLiveData<Resource<List<Attendance.Classroom>>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.getTeachingClasses().enqueue(new Callback<List<Attendance.Classroom>>() {
            @Override
            public void onResponse(Call<List<Attendance.Classroom>> call, Response<List<Attendance.Classroom>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    data.setValue(Resource.error("Lỗi lấy dữ liệu: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Attendance.Classroom>> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }

    // Thêm hàm tạo lớp học
    public LiveData<Resource<Attendance.Classroom>> createClass(Attendance.Classroom newClass) {
        MutableLiveData<Resource<Attendance.Classroom>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.createClass(newClass).enqueue(new Callback<Attendance.Classroom>() {
            @Override
            public void onResponse(Call<Attendance.Classroom> call, Response<Attendance.Classroom> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    data.setValue(Resource.error("Lỗi tạo lớp: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<Attendance.Classroom> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}
