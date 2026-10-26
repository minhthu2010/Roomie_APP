package com.example.roomie.data.remote;

import androidx.annotation.NonNull;

import com.example.roomie.BuildConfig;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Gắn token vào mọi request. Khi chưa đăng nhập và đang ở bản debug,
 * gắn header X-Dev-User-Id để test API trước khi có đăng nhập thật.
 */
class AuthInterceptor implements Interceptor {

    private final SessionManager session;

    AuthInterceptor(SessionManager session) {
        this.session = session;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        String token = session.getAccessToken();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        } else if (BuildConfig.DEBUG && !BuildConfig.DEV_USER_ID.isEmpty()) {
            builder.header("X-Dev-User-Id", BuildConfig.DEV_USER_ID);
        }
        return chain.proceed(builder.build());
    }
}
