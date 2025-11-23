// app/build.gradle.kts (Module: app)

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)

    // --- NOVO PLUGIN ---
    // Aplica o plugin do Google Services neste módulo
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.nutrih"
    compileSdk = 34

    defaultConfig {
        // ... (o resto do defaultConfig) ...
        applicationId = "com.example.nutrih"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ... (buildTypes, compileOptions, kotlinOptions, buildFeatures ... )
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // ... (Core, ViewModel, Corrotinas, Room, Navegação - tudo igual) ...
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
// --- ADICIONE ESTAS DUAS LINHAS AQUI ---
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // ---------------------------------------
    // --- NOVAS DEPENDÊNCIAS (RNF03 - FIREBASE) ---

    // Importa o Bill of Materials (BOM) do Firebase
    // Isso gerencia as versões de todas as bibliotecas Firebase
    implementation(platform(libs.firebase.bom))

    // Dependência para Firebase Analytics (recomendado)
    implementation(libs.firebase.analytics)

    // Dependência para Firebase Authentication (Login)
    implementation(libs.firebase.auth.ktx)

    // Dependência para Firebase Firestore (Banco de Dados em Nuvem)
    implementation(libs.firebase.firestore.ktx)

    // ... (Testes) ...
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}