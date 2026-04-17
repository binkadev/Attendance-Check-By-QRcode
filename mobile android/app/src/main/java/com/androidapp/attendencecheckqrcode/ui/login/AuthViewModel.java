package com.androidapp.attendencecheckqrcode.ui.login;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.androidapp.attendencecheckqrcode.models.payloads.AuthResponse;
import com.androidapp.attendencecheckqrcode.models.payloads.LoginRequest;
import com.androidapp.attendencecheckqrcode.models.payloads.RegisterRequest;
import com.androidapp.attendencecheckqrcode.repository.AuthRepository;
import com.androidapp.attendencecheckqrcode.utils.Resource;

public class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;

    private final MutableLiveData<Resource<AuthResponse>> loginResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<AuthResponse>> registerResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AuthRepository(application);
    }

    public LiveData<Resource<AuthResponse>> getLoginResult() { return loginResult; }
    public LiveData<Resource<AuthResponse>> getRegisterResult() { return registerResult; }

    public void login(LoginRequest request) {
        repository.login(request).observeForever(loginResult::setValue);
    }

    public void register(RegisterRequest request) {
        repository.register(request).observeForever(registerResult::setValue);
    }
}
