package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.GroupResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.JoinGroupRequest;       // Đã thêm Import
import com.androidapp.attendencecheckqrcode.data.dto.group.MemberResponse;         // Đã thêm Import
import com.androidapp.attendencecheckqrcode.data.dto.teaching.AttendancePolicyRequest;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
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

        apiService.getEnrolledClasses("STUDENT").enqueue(new Callback<List<Attendance.Classroom>>() {
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

    // ĐÃ SỬA LỖI: Thêm chữ "LECTURER" vào hàm này
    public LiveData<Resource<List<Attendance.Classroom>>> getTeachingClasses() {
        MutableLiveData<Resource<List<Attendance.Classroom>>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

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

    public LiveData<Resource<GroupResponse>> createClass(CreateGroupRequest request) {
        MutableLiveData<Resource<GroupResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        apiService.createClassGroup(request).enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    // ĐỌC CHI TIẾT LỖI TỪ BACKEND
                    String errorDetail = "Lỗi " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorDetail = response.errorBody().string();
                            // In ra Logcat
                            android.util.Log.e("LOI_400_BACKEND", "Chi tiết: " + errorDetail);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (response.code() == 409) {
                        data.setValue(Resource.error("Mã lớp hoặc Join Code đã tồn tại!", null));
                    } else {
                        data.setValue(Resource.error("Lỗi: " + errorDetail, null));
                    }
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }

    public LiveData<Resource<Void>> updatePolicy(String groupId, AttendancePolicyRequest request) {
        MutableLiveData<Resource<Void>> data = new MutableLiveData<>();

        apiService.updateAttendancePolicy(groupId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    data.setValue(Resource.success(null));
                } else {
                    data.setValue(Resource.error("Lỗi cập nhật luật điểm danh: " + response.code(), null));
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }

    // ĐÃ BỔ SUNG LẠI HÀM: Tham gia lớp học (Join Class)
    public LiveData<Resource<MemberResponse>> joinClass(String joinCode) {
        MutableLiveData<Resource<MemberResponse>> data = new MutableLiveData<>();
        data.setValue(Resource.loading(null));

        JoinGroupRequest request = new JoinGroupRequest(joinCode);

        apiService.joinClass(request).enqueue(new Callback<MemberResponse>() {
            @Override
            public void onResponse(Call<MemberResponse> call, Response<MemberResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(Resource.success(response.body()));
                } else {
                    data.setValue(Resource.error("Mã lớp không hợp lệ hoặc đã tham gia! (" + response.code() + ")", null));
                }
            }

            @Override
            public void onFailure(Call<MemberResponse> call, Throwable t) {
                data.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
        return data;
    }
}