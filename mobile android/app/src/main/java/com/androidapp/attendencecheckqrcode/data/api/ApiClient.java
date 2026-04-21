package com.androidapp.attendencecheckqrcode.data.api;

import android.content.Context;
import android.util.Log;

import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://192.168.46.157:8081/";
    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            Context appContext = context.getApplicationContext();
            TokenManager tokenManager = new TokenManager(appContext);

            Interceptor authInterceptor = new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();

                    String token = tokenManager.getAccessToken();

                    if (token != null && !token.isEmpty()) {
                        Log.d("DEBUG_NETWORK", "===> Đã gắn Token: " + token.substring(0, Math.min(token.length(), 10)) + "...");
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    }

                    Log.e("DEBUG_NETWORK", "===> CẢNH BÁO: KHÔNG CÓ TOKEN TRONG MÁY!");
                    return chain.proceed(originalRequest);
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .authenticator(new TokenAuthenticator(appContext))
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