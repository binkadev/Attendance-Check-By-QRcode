// Cau hinh network
package com.androidapp.attendencecheckqrcode.data.api;

import android.content.Context;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    //private static final String BASE_URL = "http://10.251.0.93:8081/";
    private static final String BASE_URL = "http://192.168.46.139:8081/";
    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            TokenManager tokenManager = new TokenManager(context);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();
                    String token = tokenManager.getToken();
                    if (token != null) {
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    }
                    return chain.proceed(originalRequest);
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}