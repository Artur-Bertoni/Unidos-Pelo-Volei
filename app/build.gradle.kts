import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Chaves de ambiente.
 *
 * Nenhum segredo entra no controle de versao: as chaves vem de `local.properties`
 * (ou de variaveis de ambiente, util em CI) e sao expostas ao codigo via BuildConfig.
 * O prefixo define o ambiente, por exemplo `dev.SUPABASE_URL` e `prod.SUPABASE_URL`.
 */
val localProperties: Properties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

fun secret(
    flavor: String,
    key: String,
): String =
    localProperties.getProperty("$flavor.$key")
        ?: System.getenv("${flavor.uppercase()}_$key")
        ?: ""

fun com.android.build.api.dsl.ApplicationProductFlavor.environmentKeys(flavor: String) {
    listOf("SUPABASE_URL", "SUPABASE_ANON_KEY", "POWERSYNC_URL", "GOOGLE_WEB_CLIENT_ID").forEach { key ->
        buildConfigField("String", key, "\"${secret(flavor, key)}\"")
    }
    buildConfigField("String", "ENVIRONMENT", "\"$flavor\"")
}

android {
    namespace = "com.unidospelovolei"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.unidospelovolei"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Unidos Pelo Volei DEV")
            environmentKeys("dev")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Unidos Pelo Volei")
            environmentKeys("prod")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Login Google via Credential Manager + Google Identity Services
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.identity.googleid)

    // Supabase (Auth + Postgrest) e PowerSync (SQLite local sincronizado)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.powersync.core)
    implementation(libs.powersync.compose)
    implementation(libs.powersync.connector.supabase)

    testImplementation(libs.junit)
}
