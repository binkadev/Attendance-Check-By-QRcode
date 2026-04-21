package com.androidapp.attendencecheckqrcode.ui.join;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.data.dto.group.MemberResponse;
import com.androidapp.attendencecheckqrcode.data.repository.ClassRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

public class JoinClassViewModel extends AndroidViewModel {
    private final ClassRepository classRepository;
    private final MutableLiveData<Resource<MemberResponse>> joinClassResult = new MutableLiveData<>();

    public JoinClassViewModel(@NonNull Application application) {
        super(application);
        classRepository = new ClassRepository(application.getApplicationContext());
    }

    public LiveData<Resource<MemberResponse>> getJoinClassResult() {
        return joinClassResult;
    }

    public void joinClass(String joinCode) {
        joinClassResult.setValue(Resource.loading(null));
        classRepository.joinClass(joinCode, joinClassResult);
    }
}