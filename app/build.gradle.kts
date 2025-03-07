// import com.android.build.gradle.internal.tasks.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.lsplugin.jgit)
    alias(libs.plugins.lsplugin.apksign)
    alias(libs.plugins.lsplugin.apktransform)
}

val repo = jgit.repo()
val VerCode = repo?.commitCount("refs/remotes/origin/main") ?: 1
val VerName = repo?.latestTag?.removePrefix("v") ?: "1.0"

apksign {
    storeFileProperty = "releaseStoreFile"
    storePasswordProperty = "releaseStorePassword"
    keyAliasProperty = "releaseKeyAlias"
    keyPasswordProperty = "releaseKeyPassword"
}

apktransform {
    copy {
        when (it.buildType) {
            "release" -> file("${it.name}/ink.chyk.neuqrcode.${VerName}.apk")
            else -> null
        }
    }
}

android {
    namespace = "ink.chyk.neuqrcode"
    compileSdk = 35

    defaultConfig {
        applicationId = "ink.chyk.neuqrcode"
        minSdk = 28
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 28
        versionCode = VerCode
        versionName = VerName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // 只包含 arm64-v8a 和 armeabi-v7a
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

  compileOptions {
    // Flag to enable support for the new language APIs

    // For AGP 4.1+
    isCoreLibraryDesugaringEnabled = true
    // For AGP 4.0
    // coreLibraryDesugaringEnabled = true

    // Sets Java compatibility to Java 8
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    /*
    tasks.withType<L8DexDesugarLibTask> {
        keepRulesConfigurations.set(listOf("-keep class java.time.zone.** { *; }", "-keep interface java.time.zone.** { *; }"))
    }
    */
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.okhttp)
    implementation(libs.mmkv)
    implementation(libs.serialization.json)
    implementation(libs.zxing)
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.viewmodel.compose)
    implementation(libs.pangu)
    coreLibraryDesugaring(libs.desugar)
    implementation(libs.paging)
    implementation(libs.paging.compose)
    implementation(libs.browser)
    implementation(libs.coil)
    implementation(libs.coil.net)
    implementation(libs.webview)
}