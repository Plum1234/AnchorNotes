plugins { id("com.android.application") }

android {
    namespace = "com.example.anchornotes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.anchornotes"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { viewBinding = true; compose = false }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Lifecycle / ViewModel (Java-friendly)
    implementation("androidx.lifecycle:lifecycle-runtime:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")

    // Room (Java) – use annotationProcessor, not kapt
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Glide (images)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Fragments / activity helpers
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.fragment:fragment:1.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
