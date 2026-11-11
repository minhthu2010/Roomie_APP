import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Địa chỉ API: mặc định cho máy ảo (10.0.2.2 = localhost của máy tính).
// Điện thoại thật: thêm vào local.properties dòng
//   roomie.apiBaseUrl=http://192.168.1.x:3000/api/v1/

// Địa chỉ API:
// Debug: mặc định dùng Android Emulator.
// Release: sử dụng địa chỉ backend production.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val debugApiBaseUrl = localProps.getProperty(
    "roomie.debugApiBaseUrl",
    "http://10.0.2.2:3000/api/v1/"
)

val releaseApiBaseUrl = localProps.getProperty(
    "roomie.releaseApiBaseUrl",
    "https://180-93-37-88.sslip.io/api/v1/"
)
//val apiBaseUrl: String = localProps.getProperty("roomie.apiBaseUrl", "http://10.0.2.2:3000/api/v1/")
// User giả lập khi chưa có đăng nhập thật (backend bật AUTH_DEV_BYPASS). Mặc định: seeker trong seed.
val devUserId: String = localProps.getProperty("roomie.devUserId", "00000000-0000-0000-0000-000000000003")
// Đăng nhập Google: Web client ID của nhóm trên Google Cloud (cùng giá trị GOOGLE_CLIENT_IDS ở backend .env).
// Không phải bí mật (nằm sẵn trong APK) nên ghi mặc định ở đây để ai build cũng có.
// Muốn dùng client khác thì thêm roomie.googleWebClientId=... vào local.properties.
// Lưu ý: SHA-1 của máy/khóa build vẫn phải được khai báo thành client Android trên Google Cloud.
val googleWebClientId: String = localProps.getProperty(
    "roomie.googleWebClientId",
    "989092068165-c2o7b5sq45ctpurg0ifljlmvftkb1re2.apps.googleusercontent.com"
)

android {
    namespace = "com.example.roomie"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.roomie"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"$debugApiBaseUrl\""
            )

            buildConfigField(
                "String",
                "DEV_USER_ID",
                "\"$devUserId\""
            )
        }

        release {
            buildConfigField(
                "String",
                "BASE_URL",
                "\"$releaseApiBaseUrl\""
            )

            buildConfigField(
                "String",
                "DEV_USER_ID",
                "\"\""
            )

            optimization {
                enable = false
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.glide)
    implementation(libs.osmdroid)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
