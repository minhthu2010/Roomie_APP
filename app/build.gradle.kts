import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Địa chỉ API: mặc định cho máy ảo (10.0.2.2 = localhost của máy tính).
// Điện thoại thật: thêm vào local.properties dòng
//   roomie.apiBaseUrl=http://192.168.1.x:3000/api/v1/
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val apiBaseUrl: String = localProps.getProperty("roomie.apiBaseUrl", "http://10.0.2.2:3000/api/v1/")
// User giả lập khi chưa có đăng nhập thật (backend bật AUTH_DEV_BYPASS). Mặc định: seeker trong seed.
val devUserId: String = localProps.getProperty("roomie.devUserId", "00000000-0000-0000-0000-000000000003")

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

        buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "DEV_USER_ID", "\"$devUserId\"")
        }
        release {
            buildConfigField("String", "DEV_USER_ID", "\"\"")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
