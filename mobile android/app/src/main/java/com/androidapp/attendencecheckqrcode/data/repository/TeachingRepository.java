package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.PageResponse;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.GroupStudentPolicyResponse;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
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

    public void getTeachingClasses(MutableLiveData<Resource<List<Classroom>>> result) {
        apiService.getTeachingClasses(0, 100, null).enqueue(new Callback<PageResponse<Classroom>>() {
            @Override
            public void onResponse(Call<PageResponse<Classroom>> call, Response<PageResponse<Classroom>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Resource.success(response.body().getItems()));
                }
            }
            @Override
            public void onFailure(Call<PageResponse<Classroom>> call, Throwable t) {
                result.setValue(Resource.error("Lỗi tải lớp giảng", null));
            }
        });
    }
//    public void getTeachingClasses(MutableLiveData<Resource<List<Classroom>>> resultLiveData) {
//
//        apiService.getTeachingClasses(0, 100, null).enqueue(new Callback<PageResponse<Classroom>>() {
//            @Override
//            public void onResponse(Call<PageResponse<Classroom>> call, Response<PageResponse<Classroom>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<Classroom> classrooms = response.body().getItems();
//                    resultLiveData.setValue(Resource.success(classrooms));
//                    Log.d("REPO_TEACHING", "Lấy danh sách lớp thành công: " + classrooms.size() + " lớp");
//                } else {
//                    int code = response.code();
//                    resultLiveData.setValue(Resource.error("Lỗi lấy dữ liệu: " + code, null));
//                    Log.e("REPO_TEACHING", "Lỗi API: " + code);
//                }
//            }
//
//            // SỬA LỖI TẠI ĐÂY: Phải đổi thành Classroom (bỏ Attendance.)
//            @Override
//            public void onFailure(Call<PageResponse<Classroom>> call, Throwable t) {
//                resultLiveData.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
//                Log.e("REPO_TEACHING", "Thất bại hoàn toàn: " + t.getMessage());
//            }
//        });
//    }

    public void getTeachingClassDetails(String groupId, MutableLiveData<Resource<GroupStudentPolicyResponse>> resultLiveData) {
        apiService.getTeachingClassDetails(groupId).enqueue(new Callback<GroupStudentPolicyResponse>() {
            @Override
            public void onResponse(Call<GroupStudentPolicyResponse> call, Response<GroupStudentPolicyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    String errorBody = "Không rõ lỗi";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception ignored) {}

                    Log.e("REPO_TEACHING", "Lỗi 500/400: " + errorBody);
                    resultLiveData.setValue(Resource.error("Lỗi server: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<GroupStudentPolicyResponse> call, Throwable t) {
                resultLiveData.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
                Log.e("REPO_TEACHING", "Lỗi kết nối chi tiết: " + t.getMessage());
            }
        });
    }

    // =========================================================
    // THÊM HÀM NÀY VÀO ĐỂ VIEWMODEL CÓ THỂ GỌI ĐƯỢC
    // =========================================================
    public void getClassFullInfo(String groupId, MutableLiveData<Resource<Classroom>> resultLiveData) {
        apiService.getClassById(groupId).enqueue(new Callback<Classroom>() {
            @Override
            public void onResponse(Call<Classroom> call, Response<Classroom> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    resultLiveData.setValue(Resource.error("Không thể lấy chi tiết lớp", null));
                }
            }

            @Override
            public void onFailure(Call<Classroom> call, Throwable t) {
                resultLiveData.setValue(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
            }
        });
    }
}