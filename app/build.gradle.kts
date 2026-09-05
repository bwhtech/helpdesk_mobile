import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Release signing comes from local.properties on a workstation and from the
// environment in CI; without it the release build stays unsigned.
fun signingSecret(propertyKey: String, envKey: String): String? =
    localProperties.getProperty(propertyKey) ?: System.getenv(envKey)

fun latestGitTag(): String? = runCatching {
    providers.exec { commandLine("git", "describe", "--tags", "--abbrev=0") }
        .standardOutput.asText.get().trim().removePrefix("v").ifEmpty { null }
}.getOrNull()

val appVersionName = latestGitTag() ?: "0.0.0"
val appVersionCode = appVersionName.substringBefore('-').split('.').let { parts ->
    val (major, minor, patch) = List(3) { parts.getOrNull(it)?.toIntOrNull() ?: 0 }
    (major * 10_000 + minor * 100 + patch).coerceAtLeast(1)
}

android {
    namespace = "io.github.kaulith.helpdeskanalytics"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.kaulith.helpdeskanalytics"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storePath = signingSecret("release.storeFile", "RELEASE_STORE_FILE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = signingSecret("release.storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingSecret("release.keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingSecret("release.keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        create("internal") {
            initWith(getByName("release"))
            versionNameSuffix = "-internal"
            signingConfig = signingConfigs.getByName("debug")
            // CI builds this variant against the sample Firebase config; app id is a placeholder.
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            matchingFallbacks += listOf("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.kizitonwose.calendar:compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Networking - Retrofit + OkHttp + Gson
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Dependency Injection - Koin
    implementation("io.insert-koin:koin-android:3.5.3")
    implementation("io.insert-koin:koin-androidx-compose:3.5.3")

    // Local Storage - Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Preferences - DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0")

    // Background Processing - WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Image Loading - Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Date/Time
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
