package com.example.roomie.data.accounts;

import android.os.Build;

/** DTO cho API tài khoản; tên field trùng với JSON camelCase của backend. */
public final class AccountModels {

    private AccountModels() {}

    /** Tên thiết bị gửi kèm khi đăng nhập, để sau này xem các phiên đăng nhập. */
    public static String deviceLabel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    // ---------- Request ----------

    public static class RegisterRequest {
        public final String fullName;
        public final String email;
        public final String password;
        public final boolean acceptTerms = true;

        public RegisterRequest(String fullName, String email, String password) {
            this.fullName = fullName;
            this.email = email;
            this.password = password;
        }
    }

    public static class VerifyOtpRequest {
        public final String email;
        public final String code;
        public final String deviceLabel = deviceLabel();

        public VerifyOtpRequest(String email, String code) {
            this.email = email;
            this.code = code;
        }
    }

    public static class EmailRequest {
        public final String email;

        public EmailRequest(String email) {
            this.email = email;
        }
    }

    public static class LoginRequest {
        public final String email;
        public final String password;
        public final String deviceLabel = deviceLabel();

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class GoogleLoginRequest {
        public final String idToken;
        public final String deviceLabel = deviceLabel();

        public GoogleLoginRequest(String idToken) {
            this.idToken = idToken;
        }
    }

    public static class RefreshRequest {
        public final String refreshToken;

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static class ChangePasswordRequest {
        public final String currentPassword;
        public final String newPassword;
        public final String deviceLabel = deviceLabel();

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }

    public static class VerifyResetOtpRequest {
        public final String email;
        public final String code;

        public VerifyResetOtpRequest(String email, String code) {
            this.email = email;
            this.code = code;
        }
    }

    public static class ResetPasswordRequest {
        public final String resetToken;
        public final String newPassword;

        public ResetPasswordRequest(String resetToken, String newPassword) {
            this.resetToken = resetToken;
            this.newPassword = newPassword;
        }
    }

    /** Để null những trường không đổi. Chuỗi rỗng "" = xóa giá trị. */
    public static class UpdateProfileRequest {
        public String displayName;
        public String phoneNumber;
        public String bio;
        public String gender;
        public Integer birthYear;
        public String occupation;
        public String schoolOrWorkplace;
        public String livingAreaText;
        public Long budgetMin;
        public Long budgetMax;
        /** yyyy-MM-dd */
        public String moveInDate;
        public String onboardingGoal;
        public Boolean completeSetup;
    }

    // ---------- Response ----------

    public static class OtpChallenge {
        public String email;
        public String expiresAt;
        public String resendAvailableAt;
        public int otpLength;
    }

    public static class ResetToken {
        public String resetToken;
        public long expiresIn;
    }

    /** Dùng cho cả GET (đủ field) và PATCH (chỉ field khác null). */
    public static class Settings {
        public Boolean notifyRoomMatches;
        public Boolean notifyPostActivity;
        public Boolean notifyPostExpiring;
        public Boolean notifyTips;
        public Boolean showProfile;
        public Boolean showContactOnPosts;
    }

    public static class Notification {
        public String notificationId;
        /** room_match, post_activity, post_expiring, post_moderation, report_update, feedback_reply, tip, system */
        public String type;
        public String title;
        public String body;
        public boolean isRead;
        public String createdAt;
    }

    public static class AuthResult {
        public String accessToken;
        public String refreshToken;
        public long expiresIn;
        public User user;
    }

    public static class User {
        public String userId;
        public String email;
        public String phoneNumber;
        public String fullName;
        public String displayName;
        public String avatarUrl;
        public boolean emailVerified;
        public boolean profileCompleted;
        public String createdAt;
    }

    public static class Profile extends User {
        public boolean hasPassword;
        public String memberSince;
        public String bio;
        public String gender;
        public Integer birthYear;
        public String occupation;
        public String schoolOrWorkplace;
        public String livingAreaText;
        public Long budgetMin;
        public Long budgetMax;
        public String moveInDate;
        public String preferredGender;
        public String onboardingGoal;
    }
}
