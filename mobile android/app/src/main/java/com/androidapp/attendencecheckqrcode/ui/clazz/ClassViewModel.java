package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.dto.group.GroupResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.data.dto.teaching.AttendancePolicyRequest;
import com.androidapp.attendencecheckqrcode.data.repository.ClassRepository;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;

public class ClassViewModel extends AndroidViewModel {
    private final ClassRepository repository;
    private final MutableLiveData<Resource<List<Attendance.Classroom>>> enrolledClasses = new MutableLiveData<>();
    private final MutableLiveData<Resource<GroupResponse>> createClassResult = new MutableLiveData<>();

    public ClassViewModel(@NonNull Application application) {
        super(application);
        repository = new ClassRepository(application);
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getEnrolledClassesResult() {
        return enrolledClasses; 
    }

    // Getter
    public LiveData<Resource<GroupResponse>> getCreateClassResult() {
        return createClassResult;
    }

    public void fetchEnrolledClasses() {
        enrolledClasses.setValue(Resource.loading(null));
        repository.getEnrolledClasses().observeForever(enrolledClasses::setValue);
    }

    public void createClass(CreateGroupRequest request, int maxAbsence) {
        createClassResult.setValue(Resource.loading(null));

        // Bước 1: Tạo Group
        repository.createClass(request).observeForever(result -> {
            if (result.status == Resource.Status.SUCCESS && result.data != null) {
                String groupId = result.data.getId();

                // Bước 2: Bắn Policy lên ngay lập tức
                AttendancePolicyRequest policyReq = new AttendancePolicyRequest(maxAbsence);

                // (Bạn cần viết thêm hàm updatePolicy trong ClassRepository tương tự các hàm khác)
                repository.updatePolicy(groupId, policyReq).observeForever(policyResult -> {
                    // Dù policy update thành công hay lỗi mạng thì Group cũng đã tạo xong
                    createClassResult.setValue(Resource.success(result.data));
                });
            } else {
                createClassResult.setValue(result); // Báo lỗi tạo Group
            }
        });
    }
}