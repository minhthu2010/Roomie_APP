package com.example.roomie.data.remote;

import android.content.Context;

import com.example.roomie.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit dùng chung cho cả nhóm. BASE_URL cấu hình trong app/build.gradle.kts.
 * Cách dùng: DiscoveryApi api = ApiClient.create(context, DiscoveryApi.class);
 */
public final class ApiClient {

    private static Retrofit retrofit;

    private ApiClient() {}

    public static synchronized Retrofit get(Context context) {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BODY
                    : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(new SessionManager(context)))
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static <T> T create(Context context, Class<T> service) {
        return get(context).create(service);
    }
}
