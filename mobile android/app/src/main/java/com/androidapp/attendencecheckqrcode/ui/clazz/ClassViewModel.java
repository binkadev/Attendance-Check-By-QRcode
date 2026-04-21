package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.dto.group.GroupResponse;
import com.androidapp.attendencecheckqrcode.data.dto.group.CreateGroupRequest;
import com.androidapp.attendencecheckqrcode.data.repository.ClassRepository;
import com.androidapp.attendencecheckqrcode.domain.models.Classroom;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;

public class ClassViewModel extends AndroidViewModel {
    private final ClassRepository repository;

    private final MutableLiveData<Resource<List<Classroom>>> enrolledClasses = new MutableLiveData<>();
    private final MutableLiveData<Resource<GroupResponse>> createClassResult = new MutableLiveData<>();

    public ClassViewModel(@NonNull Application application) {
        super(application);
        repository = new ClassRepository(application);
    }

    public LiveData<Resource<List<Classroom>>> getEnrolledClassesResult() {
        return enrolledClasses;
    }

    public LiveData<Resource<GroupResponse>> getCreateClassResult() {
        return createClassResult;
    }

    public void fetchEnrolledClasses() {
        enrolledClasses.setValue(Resource.loading(null));
        repository.getEnrolledClasses(enrolledClasses);
    }

    public void createClass(CreateGroupRequest request, int maxAbsence) {
        createClassResult.setValue(Resource.loading(null));


        repository.createClass(request, createClassResult);

    }
}