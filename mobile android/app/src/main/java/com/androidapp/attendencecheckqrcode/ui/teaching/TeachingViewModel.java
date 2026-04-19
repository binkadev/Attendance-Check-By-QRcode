package com.androidapp.attendencecheckqrcode.ui.teaching;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.dto.teaching.GroupStudentPolicyResponse;
import com.androidapp.attendencecheckqrcode.data.repository.TeachingRepository;
import com.androidapp.attendencecheckqrcode.domain.models.Attendance;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;

public class TeachingViewModel extends AndroidViewModel {

    private final TeachingRepository repository;

    private final MutableLiveData<Resource<List<Attendance.Classroom>>> teachingClasses = new MutableLiveData<>();
    private final MutableLiveData<Resource<GroupStudentPolicyResponse>> classDetailsResult = new MutableLiveData<>();

    public TeachingViewModel(@NonNull Application application) {
        super(application);
        repository = new TeachingRepository(application);
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getTeachingClassesResult() {
        return teachingClasses;
    }

    public LiveData<Resource<GroupStudentPolicyResponse>> getClassDetailsResult() {
        return classDetailsResult;
    }

    public void fetchTeachingClasses() {
        teachingClasses.setValue(Resource.loading(null));
        repository.getTeachingClasses().observeForever(teachingClasses::setValue);
    }

    public void fetchClassDetails(String groupId) {
        classDetailsResult.setValue(Resource.loading(null));
        repository.getTeachingClassDetails(groupId).observeForever(classDetailsResult::setValue);
    }
}