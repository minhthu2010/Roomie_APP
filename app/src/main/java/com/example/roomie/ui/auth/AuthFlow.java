package com.example.roomie.ui.auth;

import android.app.Activity;
import android.content.Intent;

import com.example.roomie.data.accounts.AccountModels;
import com.example.roomie.data.accounts.AccountsApi;
import com.example.roomie.data.remote.ApiCallback;
import com.example.roomie.data.remote.ApiClient;
import com.example.roomie.data.remote.SessionManager;
import com.example.roomie.ui.home.HomeActivity;
import com.example.roomie.ui.profile.ProfileSetupActivity;

/** Điều hướng chung sau khi đăng nhập / đăng ký / đăng xuất. */
public final class AuthFlow {

    private AuthFlow() {}

    /** Lưu token rồi vào app: chưa thiết lập hồ sơ thì sang ProfileSetup, rồi thì vào Home. */
    public static void onAuthenticated(Activity activity, AccountModels.AuthResult result) {
        new SessionManager(activity).saveTokens(result.accessToken, result.refreshToken);
        openApp(activity, result.user);
    }

    public static void openApp(Activity activity, AccountModels.User user) {
        if (user != null) new SessionManager(activity).saveDisplayName(user.displayName);
        Intent intent;
        if (user != null && !user.profileCompleted) {
            intent = new Intent(activity, ProfileSetupActivity.class);
            intent.putExtra(ProfileSetupActivity.EXTRA_FULL_NAME, user.displayName);
            intent.putExtra(ProfileSetupActivity.EXTRA_FIRST_SETUP, true);
        } else {
            intent = new Intent(activity, HomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    /** Thu hồi refresh token trên server (không chờ kết quả), xóa phiên, về màn chào mừng. */
    public static void logout(Activity activity) {
        SessionManager session = new SessionManager(activity);
        String refreshToken = session.getRefreshToken();
        if (refreshToken != null) {
            ApiClient.create(activity, AccountsApi.class)
                    .logout(new AccountModels.RefreshRequest(refreshToken))
                    .enqueue(new ApiCallback<Object>(null) {
                        @Override
                        public void onSuccess(Object data) {}

                        @Override
                        public void onError(String message) {}
                    });
        }
        session.clear();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
