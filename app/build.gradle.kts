plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.rubikaghost"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.rubikaghost"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("androidx.appcompat:appcompat:1.6.1")
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
}
