package com.example.roomie.data.accounts;

import com.example.roomie.data.remote.ApiResponse;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** API mảng Tài khoản (người A): Roomie_backend/src/modules/accounts */
public interface AccountsApi {

    // ---------- Đăng ký, đăng nhập ----------

    @POST("auth/register")
    Call<ApiResponse<AccountModels.OtpChallenge>> register(@Body AccountModels.RegisterRequest body);

    @POST("auth/register/verify")
    Call<ApiResponse<AccountModels.AuthResult>> verifyRegister(@Body AccountModels.VerifyOtpRequest body);

    @POST("auth/register/resend-otp")
    Call<ApiResponse<AccountModels.OtpChallenge>> resendRegisterOtp(@Body AccountModels.EmailRequest body);

    @POST("auth/login")
    Call<ApiResponse<AccountModels.AuthResult>> login(@Body AccountModels.LoginRequest body);

    /** "Tiếp tục với Google": idToken lấy từ Credential Manager */
    @POST("auth/google")
    Call<ApiResponse<AccountModels.AuthResult>> loginWithGoogle(@Body AccountModels.GoogleLoginRequest body);

    @POST("auth/refresh")
    Call<ApiResponse<AccountModels.AuthResult>> refresh(@Body AccountModels.RefreshRequest body);

    @POST("auth/logout")
    Call<ApiResponse<Object>> logout(@Body AccountModels.RefreshRequest body);

    @GET("auth/me")
    Call<ApiResponse<AccountModels.User>> getMe();

    // ---------- Quên mật khẩu ----------

    @POST("auth/password/forgot")
    Call<ApiResponse<AccountModels.OtpChallenge>> forgotPassword(@Body AccountModels.EmailRequest body);

    @POST("auth/password/verify-otp")
    Call<ApiResponse<AccountModels.ResetToken>> verifyResetOtp(@Body AccountModels.VerifyResetOtpRequest body);

    @POST("auth/password/reset")
    Call<ApiResponse<Object>> resetPassword(@Body AccountModels.ResetPasswordRequest body);

    // ---------- Cài đặt ----------

    @GET("me/settings")
    Call<ApiResponse<AccountModels.Settings>> getSettings();

    /** Chỉ gửi switch vừa đổi (field khác null). */
    @PATCH("me/settings")
    Call<ApiResponse<AccountModels.Settings>> updateSettings(@Body AccountModels.Settings body);

    // ---------- Thông báo ----------

    @GET("notifications")
    Call<ApiResponse<List<AccountModels.Notification>>> getNotifications(@Query("page") int page,
                                                                        @Query("limit") int limit);

    @PATCH("notifications/{id}/read")
    Call<ApiResponse<AccountModels.Notification>> markNotificationRead(@Path("id") String notificationId);

    @POST("notifications/read-all")
    Call<ApiResponse<Object>> markAllNotificationsRead();

    @DELETE("notifications/{id}")
    Call<ApiResponse<Object>> deleteNotification(@Path("id") String notificationId);

    // ---------- Hồ sơ ----------

    @GET("me/profile")
    Call<ApiResponse<AccountModels.Profile>> getMyProfile();

    /** Chỉ gửi các trường khác null (Gson bỏ qua null). */
    @PATCH("me/profile")
    Call<ApiResponse<AccountModels.Profile>> updateMyProfile(@Body AccountModels.UpdateProfileRequest body);

    @Multipart
    @PUT("me/avatar")
    Call<ApiResponse<AccountModels.Profile>> uploadAvatar(@Part MultipartBody.Part avatar);

    @PUT("me/password")
    Call<ApiResponse<AccountModels.AuthResult>> changePassword(@Body AccountModels.ChangePasswordRequest body);
}
