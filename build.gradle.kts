// build.gradle.kts (Project: NutriH)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false

    // --- NOVO PLUGIN ---
    // Plugin do Google Services para o Firebase
    alias(libs.plugins.google.gms.google.services) apply false
}