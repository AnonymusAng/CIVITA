plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sin este archivo el APK compila y muestra instrucciones; no inventa un proyecto Firebase.
if (file("google-services.json").isFile && providers.gradleProperty("civitaSimulation").orElse("false").get() != "true") {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "co.edu.uniautonoma.civita"
    compileSdk = 37
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "co.edu.uniautonoma.civita"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.742"
    }

    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        debug {
            buildConfigField("boolean", "SIMULATION", providers.gradleProperty("civitaSimulation").orElse("false").get())
            if (providers.gradleProperty("civitaSimulation").orElse("false").get() == "true") applicationIdSuffix = ".simulation"
            manifestPlaceholders["civitaCleartext"] = providers.gradleProperty("civitaEmulators").orElse("false").get(); buildConfigField("boolean", "USE_FIREBASE_EMULATORS", providers.gradleProperty("civitaEmulators").orElse("false").get()) }
        release { buildConfigField("boolean", "SIMULATION", "false"); buildConfigField("boolean", "USE_FIREBASE_EMULATORS", "false") }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
