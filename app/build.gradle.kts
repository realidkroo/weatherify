import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(secretsFile.inputStream())
}

android {
    namespace = "com.app.weather"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roo.weatherify.app.dev"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API Keys from secrets.properties
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"${secrets.getProperty("OPENWEATHER_API_KEY") ?: ""}\"")
        buildConfigField("String", "BMKG_API_KEY", "\"${secrets.getProperty("BMKG_API_KEY") ?: ""}\"")
        buildConfigField("String", "OPENMAP_API_KEY", "\"${secrets.getProperty("OPENMAP_API_KEY") ?: ""}\"")
        buildConfigField("String", "GOOGLE_API_KEY", "\"${secrets.getProperty("GOOGLE_API_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf("-Xno-param-assertions", "-Xno-call-assertions")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.6.2")
    implementation("androidx.compose.ui:ui-graphics:1.6.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.2")
    implementation("androidx.compose.animation:animation:1.6.2")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.2")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains:annotations:24.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

tasks.register("unitTestClasses") {
    dependsOn("testDebugUnitTest")
}
