package com.example.roomie.data.remote;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.roomie.BuildConfig;
import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.ui.auth.MainActivity;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Access token sống 15 phút. Khi API trả 401, OkHttp gọi lớp này: dùng refresh token
 * xin cặp token mới rồi gửi lại request. Refresh hỏng => xóa phiên, về màn chào mừng.
 */
class TokenAuthenticator implements Authenticator {

    private final Context appContext;
    private final SessionManager session;
    private AccountsApi refreshApi;

    TokenAuthenticator(Context context, SessionManager session) {
        this.appContext = context.getApplicationContext();
        this.session = session;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        Request request = response.request();
        String sentHeader = request.header("Authorization");
        // Không refresh khi: request không gắn token, đã thử lại một lần, hoặc là API đăng nhập/đăng ký/refresh
        // (401 ở đó nghĩa là sai mật khẩu, mã...). /auth/me vẫn phải refresh: Splash dùng nó để kiểm tra phiên.
        String path = request.url().encodedPath();
        boolean authEndpoint = path.contains("/auth/") && !path.endsWith("/auth/me");
        if (sentHeader == null || authEndpoint || response.priorResponse() != null) {
            return null;
        }

        synchronized (this) {
            String current = session.getAccessToken();
            if (current == null) return null;
            // Request khác vừa refresh xong trong lúc chờ
            if (!sentHeader.equals("Bearer " + current)) {
                return withToken(request, current);
            }

            String refreshToken = session.getRefreshToken();
            if (refreshToken == null) return expire();

            retrofit2.Response<ApiResponse<AccountModels.AuthResult>> result =
                    api().refresh(new AccountModels.RefreshRequest(refreshToken)).execute();
            if (result.isSuccessful() && result.body() != null && result.body().data != null) {
                AccountModels.AuthResult data = result.body().data;
                session.saveTokens(data.accessToken, data.refreshToken);
                return withToken(request, data.accessToken);
            }
            // 401/403: refresh token hết hạn hoặc bị thu hồi. Lỗi server khác: giữ phiên, để màn hình báo lỗi.
            if (result.code() == 401 || result.code() == 403) return expire();
            return null;
        }
    }

    private Request withToken(Request request, String token) {
        return request.newBuilder().header("Authorization", "Bearer " + token).build();
    }

    private Request expire() {
        session.clear();
        Intent intent = new Intent(appContext, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(MainActivity.EXTRA_SESSION_EXPIRED, true);
        appContext.startActivity(intent);
        return null;
    }

    // Retrofit riêng, không có authenticator để tránh gọi lặp
    private AccountsApi api() {
        if (refreshApi == null) {
            refreshApi = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(new OkHttpClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AccountsApi.class);
        }
        return refreshApi;
    }
}
