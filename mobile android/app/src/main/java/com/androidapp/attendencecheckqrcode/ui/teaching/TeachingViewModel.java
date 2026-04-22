package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.dto.teaching.GroupStudentPolicyResponse;
import com.androidapp.attendencecheckqrcode.data.repository.TeachingRepository;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;

public class TeachingViewModel extends AndroidViewModel {

    private final TeachingRepository repository;

    private final MutableLiveData<Resource<List<Classroom>>> teachingClasses = new MutableLiveData<>();
    private final MutableLiveData<Resource<GroupStudentPolicyResponse>> classDetailsResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Classroom>> classFullInfo = new MutableLiveData<>();

    public LiveData<Resource<Classroom>> getClassFullInfo() {
        return classFullInfo;
    }

    public void fetchClassFullInfo(String groupId) {
        classFullInfo.setValue(Resource.loading(null));
        // Giả sử bạn thêm hàm này vào TeachingRepository hoặc gọi trực tiếp từ ApiService qua Repository
        repository.getClassFullInfo(groupId, classFullInfo);
    }

    public TeachingViewModel(@NonNull Application application) {
        super(application);
        repository = new TeachingRepository(application);
    }

    public LiveData<Resource<List<Classroom>>> getTeachingClassesResult() {
        return teachingClasses;
    }

    public LiveData<Resource<GroupStudentPolicyResponse>> getClassDetailsResult() {
        return classDetailsResult;
    }

    public void fetchTeachingClasses() {
        teachingClasses.setValue(Resource.loading(null));

        repository.getTeachingClasses(teachingClasses);
    }

    public void fetchClassDetails(String groupId) {
        classDetailsResult.setValue(Resource.loading(null));

        repository.getTeachingClassDetails(groupId, classDetailsResult);
    }
}