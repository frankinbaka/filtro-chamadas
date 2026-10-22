plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "br.filtro.chamadas"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.filtro.chamadas"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("fixa") {
            // Chave fixa para que novas versões possam ser instaladas por cima da anterior
            storeFile = file("assinatura.keystore")
            storePassword = "filtrochamadas"
            keyAlias = "filtro"
            keyPassword = "filtrochamadas"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixa")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
