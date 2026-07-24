// Build do módulo do app. É aqui que ligamos os plugins e listamos as
// bibliotecas que o app usa.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")      // compilador do Jetpack Compose
    id("org.jetbrains.kotlin.plugin.serialization") // pra converter JSON <-> objetos
}

android {
    namespace = "com.privatemessenger.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.privatemessenger.app"
        minSdk = 24        // Android 7.0 pra cima (cobre a grande maioria dos aparelhos)
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            // Pra desenvolvimento deixamos sem ofuscação. Em produção dá pra ligar.
            isMinifyEnabled = false
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
        compose = true // liga o Jetpack Compose (nossa interface)
    }
}

plugins {
    id("com.google.gms.google-services") version "4.4.0" // para o Firebase
}

dependencies {
    // --- Firebase (push notifications) ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-messaging")

    // --- Jetpack Compose (a interface do app) ---
    // A "BOM" (Bill of Materials) garante que todas as libs do Compose usem
    // versões compatíveis entre si.
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3") // componentes prontos (sem tema custom por enquanto)
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // --- Rede: Ktor client ---
    // Escolhemos Ktor (e não Retrofit) de propósito: ele é multiplataforma.
    // Assim, quando migrarmos pra Kotlin Multiplatform (Android + iOS), este
    // mesmo código de rede será reaproveitado.
    val ktor = "3.0.1"
    implementation("io.ktor:ktor-client-core:$ktor")
    implementation("io.ktor:ktor-client-okhttp:$ktor")            // motor HTTP no Android
    implementation("io.ktor:ktor-client-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
    implementation("io.ktor:ktor-client-websockets:$ktor")        // tempo real
    implementation("io.ktor:ktor-client-logging:$ktor")

    // --- Serialização JSON ---
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // --- Guardar o token de login no aparelho ---
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- Corrotinas (código assíncrono: rede sem travar a tela) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
