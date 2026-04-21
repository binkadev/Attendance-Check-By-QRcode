package com.androidapp.attendencecheckqrcode.data.api;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.androidapp.attendencecheckqrcode.ui.auth.LoginActivity;
import com.androidapp.attendencecheckqrcode.utils.TokenManager;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import org.json.JSONObject;

public class TokenAuthenticator implements Authenticator {
    private Context context;
    private TokenManager tokenManager;

    public TokenAuthenticator(Context context) {
        this.context = context.getApplicationContext();
        this.tokenManager = new TokenManager(context);
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (response.priorResponse() != null && response.priorResponse().code() == 401) {
            return null;
        }

        synchronized (this) {
            String currentRefreshToken = tokenManager.getRefreshToken();

            if (currentRefreshToken == null || currentRefreshToken.isEmpty()) {
                forceLogout();
                return null;
            }

            String newAccessToken = fetchNewAccessToken(currentRefreshToken);

            if (newAccessToken != null) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + newAccessToken)
                        .build();
            } else {
                forceLogout();
                return null;
            }
        }
    }

    private String fetchNewAccessToken(String refreshToken) {
        try {
            OkHttpClient client = new OkHttpClient();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("refreshToken", refreshToken);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());

            Request request = new Request.Builder()
                    .url("http://YOUR_SERVER_IP:PORT/api/v1/auth/refresh")
                    .post(body)
                    .build();

            Response refreshResponse = client.newCall(request).execute();

            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String responseBody = refreshResponse.body().string();
                JSONObject jsonObject = new JSONObject(responseBody);

                String newAccess = jsonObject.getString("accessToken");
                String newRefresh = jsonObject.getString("refreshToken");
                tokenManager.saveTokens(newAccess, newRefresh);
                return newAccess;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void forceLogout() {
        tokenManager.clearAll();
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(context, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }
}