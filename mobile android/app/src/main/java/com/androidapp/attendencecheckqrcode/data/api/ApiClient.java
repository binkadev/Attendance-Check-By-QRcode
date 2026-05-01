package com.androidapp.attendencecheckqrcode.data.api;

import android.content.Context;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // Nên cân nhắc đưa BASE_URL vào BuildConfig trong file build.gradle sau này
    private static final String BASE_URL = "http://192.168.46.157:8081/";

    // Thêm từ khóa volatile để đảm bảo an toàn luồng (Thread-safe)
    private static volatile Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            // Khóa đồng bộ hóa (Double-checked locking)
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    Context appContext = context.getApplicationContext();
                    TokenManager tokenManager = new TokenManager(appContext);

                    // 1. Interceptor gắn Access Token
                    Interceptor authInterceptor = chain -> {
                        Request originalRequest = chain.request();
                        String token = tokenManager.getAccessToken();

                        if (token != null && !token.isEmpty()) {
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                            return chain.proceed(newRequest);
                        }
                        return chain.proceed(originalRequest);
                    };

                    // 2. Logging Interceptor (Giúp debug siêu chi tiết trên Logcat)
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                    // Set level BODY để thấy được cả dữ liệu gửi đi và trả về
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                    // 3. Khởi tạo OkHttpClient
                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(authInterceptor)
                            .addInterceptor(loggingInterceptor) // Gắn logger vào
                            .authenticator(new TokenAuthenticator(appContext))
                            .build();

                    // 4. Build Retrofit
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return retrofit.create(ApiService.class);
    }
}