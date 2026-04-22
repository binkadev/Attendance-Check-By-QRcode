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

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class TokenAuthenticator implements Authenticator {
    private Context context;
    private TokenManager tokenManager;

    // Khai báo chung với ApiClient hoặc tự định nghĩa
    private static final String REFRESH_URL = "http://192.168.46.169:8081/api/v1/auth/refresh";

    public TokenAuthenticator(Context context) {
        this.context = context.getApplicationContext();
        this.tokenManager = new TokenManager(context);
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        // Ngăn chặn lặp vô hạn nếu token mới vẫn bị 401
        if (responseCount(response) >= 2) {
            return null;
        }

        synchronized (this) {
            // Lấy token cũ của request vừa bị lỗi 401
            String failedToken = getAuthTokenFromRequest(response.request());
            // Lấy token hiện tại đang lưu trong máy
            String currentSavedToken = tokenManager.getAccessToken();

            // QUAN TRỌNG: Nếu token trong máy KHÁC với token bị lỗi -> Luồng khác đã làm mới token rồi!
            if (currentSavedToken != null && !currentSavedToken.equals(failedToken)) {
                // Chỉ cần đính kèm token mới nhất và thử lại, không cần gọi Refresh API nữa
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentSavedToken)
                        .build();
            }

            // Nếu chưa ai làm mới, tiến hành gọi Refresh API
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
        // Tạo một Tag duy nhất để dễ lọc trong Logcat
        String TAG = "DEBUG_TOKEN";
        Log.d(TAG, "====> BẮT ĐẦU GỌI API REFRESH <====");
        Log.d(TAG, "Refresh Token đang gửi: " + refreshToken);

        try {
            OkHttpClient client = new OkHttpClient();

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("refreshToken", refreshToken);
            RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonBody.toString());

            Request request = new Request.Builder()
                    .url(REFRESH_URL)
                    .post(body)
                    .build();

            Log.d(TAG, "URL đang gọi: " + REFRESH_URL);

            try (Response refreshResponse = client.newCall(request).execute()) {

                // TRƯỜNG HỢP 1: SERVER TRẢ VỀ THÀNH CÔNG (HTTP 200)
                if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                    String responseBody = refreshResponse.body().string();
                    Log.d(TAG, "====> THÀNH CÔNG! Phản hồi từ Server: " + responseBody);

                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);

                        // Kiểm tra xem JSON có chứa 2 key này không
                        if (!jsonObject.has("accessToken") || !jsonObject.has("refreshToken")) {
                            Log.e(TAG, "====> LỖI: JSON server trả về KHÔNG CÓ key 'accessToken' hoặc 'refreshToken'");
                        }

                        String newAccess = jsonObject.getString("accessToken");
                        String newRefresh = jsonObject.getString("refreshToken");

                        tokenManager.saveTokens(newAccess, newRefresh);
                        Log.d(TAG, "====> LƯU TOKEN MỚI THÀNH CÔNG!");
                        return newAccess;

                    } catch (JSONException jsonE) {
                        Log.e(TAG, "====> LỖI PARSE JSON: Tên biến có thể bị sai lệch với Server. Chi tiết: " + jsonE.getMessage());
                    }
                }
                // TRƯỜNG HỢP 2: SERVER TỪ CHỐI (HTTP 400, 401, 403, 500...)
                else {
                    String errorBody = refreshResponse.body() != null ? refreshResponse.body().string() : "Không có nội dung lỗi";
                    Log.e(TAG, "====> SERVER TỪ CHỐI REFRESH!");
                    Log.e(TAG, "HTTP Status Code: " + refreshResponse.code());
                    Log.e(TAG, "Nội dung lỗi từ Server: " + errorBody);
                }
            }
        }
        // TRƯỜNG HỢP 3: LỖI MẠNG, LỖI TIME OUT, SAI ĐỊA CHỈ IP...
        catch (Exception e) {
            Log.e(TAG, "====> CRASH/LỖI KẾT NỐI: Không thể gọi được API.");
            Log.e(TAG, "Chi tiết lỗi Exception: " + e.getMessage());
            e.printStackTrace();
        }

        Log.e(TAG, "====> KẾT QUẢ CUỐI CÙNG: Hàm trả về NULL -> Ép đăng xuất!");
        return null;
    }

    private void forceLogout() {
        tokenManager.clearAll();
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(context, LoginActivity.class);
            // Cờ này sẽ xóa toàn bộ các Activity trước đó, ngăn user bấm nút Back quay lại app
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        });
    }

    // Hàm phụ trợ đếm số lần retry (an toàn hơn check priorResponse đơn thuần)
    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    // Hàm phụ trợ lấy token từ Request
    private String getAuthTokenFromRequest(Request request) {
        String authHeader = request.header("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Cắt bỏ chữ "Bearer "
        }
        return "";
    }
}