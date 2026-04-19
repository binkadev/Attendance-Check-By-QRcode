package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.GroupStudentPolicyResponse;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeachingRepository {
    private final ApiService apiService;

    public TeachingRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    // 1. Lấy danh sách lớp giảng
    public LiveData<Resource<List<Attendance.Classroom>>> getTeachingClasses() {
        MutableLiveData<Resource<List<Attendance.Classroom>>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        // LECTURER để Backend phân biệt quyền
        apiService.getTeachingClasses("LECTURER").enqueue(new Callback<List<Attendance.Classroom>>() {
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

    // 2. Lấy chi tiết lớp (Policy + Students)
    public LiveData<Resource<GroupStudentPolicyResponse>> getTeachingClassDetails(String groupId) {
        MutableLiveData<Resource<GroupStudentPolicyResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.getTeachingClassDetails(groupId).enqueue(new Callback<GroupStudentPolicyResponse>() {
            @Override
            public void onResponse(Call<GroupStudentPolicyResponse> call, Response<GroupStudentPolicyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    // Cố gắng đọc nội dung lỗi 500 từ server
                    String errorBody = "Không rõ lỗi";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    // logcat debug
                    android.util.Log.e("API_ERROR", "Lỗi 500: " + errorBody);

                    data.setValue(Resource.error("Lỗi server: " + response.code() + " - Đảm bảo bạn là Giảng viên lớp này!", null));
                }
            }
            @Override
            public void onFailure(Call<GroupStudentPolicyResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}