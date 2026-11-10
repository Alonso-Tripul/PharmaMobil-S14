import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    // Lee androidApp/google-services.json (descargado de la consola de Firebase).
    alias(libs.plugins.googleServices)
    // Sin este complemento Crashlytics se cierra al arrancar: falta el build ID.
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    // ContextCompat, ActivityCompat y NotificationCompat (sesion 13).
    implementation(libs.androidx.core.ktx)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// ---------------------------------------------------------------------------
// Sesion 14 · Firma de entrega. Credenciales en keystore.properties (raiz,
// ignorado por Git). Si el archivo no existe, la firma de release no se
// configura y la compilacion de depuracion funciona igual para todos.
// ---------------------------------------------------------------------------
val firmaFile = rootProject.file("keystore.properties")
val firma = Properties().apply {
    if (firmaFile.exists()) firmaFile.inputStream().use { load(it) }
}
val hayFirma = firmaFile.exists() && firma.getProperty("storeFile") != null

// Sesion 14 · URL de PharmaSoft. En el emulador es 10.0.2.2; en un telefono
// real es la IP de la PC en la red local. Se pasa con -Ppharmasoft.url=...
// (lo hace scripts/2_generar_builds.bat) o con pharmasoft.url en local.properties.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val pharmasoftUrl: String = providers.gradleProperty("pharmasoft.url").orNull
    ?: localProps.getProperty("pharmasoft.url")
    ?: "http://10.0.2.2:8080/api/v1/"

android {
    namespace = "pe.edu.upeu.pharmamobil"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobil"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["pharmasoftUrl"] = pharmasoftUrl
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            if (hayFirma) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}