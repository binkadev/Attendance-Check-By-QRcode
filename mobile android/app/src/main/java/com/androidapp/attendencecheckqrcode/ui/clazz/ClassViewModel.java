package com.androidapp.attendencecheckqrcode.ui.clazz;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.models.entities.Attendance;
import com.androidapp.attendencecheckqrcode.repository.ClassRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

import java.util.List;

public class ClassViewModel extends AndroidViewModel {
    private final ClassRepository repository;

    public ClassViewModel(@NonNull Application application) {
        super(application);
        repository = new ClassRepository(application);
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getEnrolledClasses() {
        return repository.getEnrolledClasses();
    }

    public LiveData<Resource<List<Attendance.Classroom>>> getTeachingClasses() {
        return repository.getTeachingClasses();
    }

    // Thêm hàm gọi repository
    private final MutableLiveData<Resource<Attendance.Classroom>> createClassResult = new MutableLiveData<>();

    public LiveData<Resource<Attendance.Classroom>> getCreateClassResult() {
        return createClassResult;
    }

    public void createClass(Attendance.Classroom newClass) {
        repository.createClass(newClass).observeForever(createClassResult::setValue);
    }
}
