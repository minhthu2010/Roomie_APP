package com.example.roomie.ui.auth;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.roomie.BuildConfig;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

/**
 * "Tiếp tục với Google" bằng Credential Manager: lấy idToken của tài khoản Google trên máy,
 * app gửi idToken lên POST /auth/google để backend kiểm tra và cấp token Roomie.
 * Cần roomie.googleWebClientId trong local.properties (Web client ID trên Google Cloud).
 */
final class GoogleSignIn {

    interface Listener {
        void onIdToken(String idToken);

        /** message null: người dùng tự đóng hộp chọn tài khoản, không cần báo lỗi */
        void onError(String message);
    }

    private GoogleSignIn() {}

    static boolean isConfigured() {
        return !BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty();
    }

    static void start(Activity activity, Listener listener) {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            listener.onIdToken(GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken());
                        } else {
                            listener.onError("Không nhận được tài khoản Google, vui lòng thử lại");
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            listener.onError(null);
                        } else if (e instanceof NoCredentialException) {
                            listener.onError("Máy chưa có tài khoản Google. Hãy thêm tài khoản trong Cài đặt rồi thử lại");
                        } else {
                            listener.onError("Đăng nhập Google thất bại: " + e.getMessage());
                        }
                    }
                });
    }
}
