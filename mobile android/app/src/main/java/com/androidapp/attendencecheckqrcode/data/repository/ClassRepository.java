package com.androidapp.attendencecheckqrcode.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.api.ApiClient;
import com.androidapp.attendencecheckqrcode.data.api.ApiService;
import com.androidapp.attendencecheckqrcode.data.dto.PageResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.GroupResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.JoinGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.group.MemberResponse;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.AttendancePolicyRequest;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClassRepository {
    private final ApiService apiService;

    public ClassRepository(Context context) {
        apiService = ApiClient.getApiService(context);
    }

    public void getEnrolledClasses(MutableLiveData<Resource<List<Classroom>>> resultLiveData) {
        android.util.Log.d("DEBUG_API", "===> Đang gọi API lấy danh sách lớp tham gia...");

        apiService.getEnrolledClasses(0, 100).enqueue(new Callback<PageResponse<Classroom>>() {
            @Override
            public void onResponse(Call<PageResponse<Classroom>> call, Response<PageResponse<Classroom>> response) {
                if (response.isSuccessful() && response.body() != null) {

                    List<Classroom> allItems = response.body().getItems();

                    List<Classroom> studentOnlyList = new ArrayList<>();

                    if (allItems != null) {
                        for (Classroom item : allItems) {
                            android.util.Log.d("DEBUG_API", "Lớp: " + item.getGroupName() + " | Role: " + item.getMyRole());

                            String role = item.getMyRole();
                            if (role != null && (role.equalsIgnoreCase("MEMBER") || role.equalsIgnoreCase("STUDENT"))) {
                                studentOnlyList.add(item);
                            }
                        }
                    }

                    resultLiveData.setValue(Resource.success(studentOnlyList));
                    android.util.Log.d("DEBUG_API", "===> Lọc xong! Số lớp tham gia: " + studentOnlyList.size());

                } else {
                    resultLiveData.setValue(Resource.error("Lỗi: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<PageResponse<Classroom>> call, Throwable t) {
                android.util.Log.e("DEBUG_API", "LỖI MẠNG: " + t.getMessage());
                resultLiveData.setValue(Resource.error("Lỗi kết nối mạng", null));
            }
        });
    }

//    public void getEnrolledClasses(MutableLiveData<Resource<List<Classroom>>> resultLiveData) {
//        apiService.getEnrolledClasses(0, 100).enqueue(new Callback<PageResponse<Classroom>>() {
//            @Override
//            public void onResponse(Call<PageResponse<Classroom>> call, Response<PageResponse<Classroom>> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<Classroom> classrooms = response.body().getItems();
//                    resultLiveData.setValue(Resource.success(classrooms));
//                } else {
//                    resultLiveData.setValue(Resource.error("Lỗi lấy danh sách: " + response.code(), null));
//                }
//            }
//
//            // --- ĐÂY LÀ PHẦN BỊ THIẾU DẪN ĐẾN LỖI ---
//            @Override
//            public void onFailure(Call<PageResponse<Classroom>> call, Throwable t) {
//                Log.e("API_ERROR", "onFailure: " + t.getMessage());
//                resultLiveData.setValue(Resource.error("Lỗi kết nối mạng: " + t.getMessage(), null));
//            }
//        });
//    }

    public void joinClass(String joinCode, MutableLiveData<Resource<MemberResponse>> resultLiveData) {
        JoinGroupRequest request = new JoinGroupRequest(joinCode);
        apiService.joinClass(request).enqueue(new Callback<MemberResponse>() {
            @Override
            public void onResponse(Call<MemberResponse> call, Response<MemberResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    resultLiveData.setValue(Resource.error("Mã lớp không hợp lệ hoặc đã tham gia!", null));
                }
            }
            @Override
            public void onFailure(Call<MemberResponse> call, Throwable t) {
                resultLiveData.setValue(Resource.error("Lỗi kết nối: " + t.getMessage(), null));
            }
        });
    }

//    public void createClass(CreateGroupRequest request, MutableLiveData<Resource<GroupResponse>> resultLiveData) {
//        apiService.createClassGroup(request).enqueue(new Callback<GroupResponse>() {
//            @Override
//            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    resultLiveData.setValue(Resource.success(response.body()));
//                } else {
//                    resultLiveData.setValue(Resource.error("Lỗi tạo lớp: " + response.code(), null));
//                }
//            }
//            @Override
//            public void onFailure(Call<GroupResponse> call, Throwable t) {
//                resultLiveData.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
//            }
//        });
//    }

    public void createClass(CreateGroupRequest request, MutableLiveData<Resource<GroupResponse>> resultLiveData) {
        apiService.createClassGroup(request).enqueue(new Callback<GroupResponse>() {
            @Override
            public void onResponse(Call<GroupResponse> call, Response<GroupResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    resultLiveData.setValue(Resource.success(response.body()));
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Không rõ lỗi";

                        android.util.Log.e("API_ERROR", "CHI TIẾT LỖI 400: " + errorBody);

                        resultLiveData.setValue(Resource.error("Lỗi từ Server: " + errorBody, null));

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultLiveData.setValue(Resource.error("Lỗi tạo lớp: " + response.code(), null));
                    }
                }
            }

            @Override
            public void onFailure(Call<GroupResponse> call, Throwable t) {
                resultLiveData.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
    }

    public void updatePolicy(String groupId, AttendancePolicyRequest request, MutableLiveData<Resource<Void>> resultLiveData) {
        apiService.updateAttendancePolicy(groupId, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    resultLiveData.setValue(Resource.success(null));
                } else {
                    resultLiveData.setValue(Resource.error("Lỗi update policy", null));
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                resultLiveData.setValue(Resource.error("Lỗi mạng: " + t.getMessage(), null));
            }
        });
    }
}