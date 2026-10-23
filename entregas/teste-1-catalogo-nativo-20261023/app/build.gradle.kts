import java.util.Properties
import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties =
    Properties().apply {
        val localPropertiesFile =
            rootProject.file(
                "local.properties"
            )

        if (localPropertiesFile.exists()) {
            localPropertiesFile
                .inputStream()
                .use {
                    load(it)
                }
        }
    }

val tmdbReadToken =
    localProperties
        .getProperty(
            "TMDB_READ_TOKEN"
        )
        ?.trim()
        .orEmpty()

// Firebase client configuration stays local; no Google services Gradle plugin required.
val firebaseConfigFile = file("google-services.json")
val firebaseConfig = if (firebaseConfigFile.exists()) JsonSlurper().parse(firebaseConfigFile) as Map<*, *> else emptyMap<Any, Any>()
val firebaseProject = firebaseConfig["project_info"] as? Map<*, *> ?: emptyMap<Any, Any>()
val firebaseClient = (firebaseConfig["client"] as? List<*>)?.mapNotNull { it as? Map<*, *> }?.firstOrNull {
    val info = it["client_info"] as? Map<*, *>
    val androidInfo = info?.get("android_client_info") as? Map<*, *>
    androidInfo?.get("package_name") == "br.com.flaviotorres.iptvpremium"
}
require(!firebaseConfigFile.exists() || firebaseClient != null) { "google-services.json pertence a outro aplicativo." }
val firebaseClientInfo = firebaseClient?.get("client_info") as? Map<*, *>
val firebaseApiKey = ((firebaseClient?.get("api_key") as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("current_key")?.toString().orEmpty()
require(!firebaseConfigFile.exists() || (firebaseApiKey.isNotBlank() &&
    !firebaseClientInfo?.get("mobilesdk_app_id")?.toString().isNullOrBlank() &&
    !firebaseProject["project_number"]?.toString().isNullOrBlank() &&
    !firebaseProject["project_id"]?.toString().isNullOrBlank())) { "Configuracao Firebase incompleta. Baixe google-services.json do aplicativo Android." }
fun firebaseLiteral(value: Any?): String = "\"" + value?.toString().orEmpty().replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "br.com.flaviotorres.iptvpremium"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        val portalUrl = localProperties.getProperty("DEVICE_PORTAL_URL", "").trim().trimEnd('/')
        buildConfigField("String", "DEVICE_PORTAL_URL", "\"" + portalUrl.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
        applicationId = "br.com.flaviotorres.iptvpremium"
        // Diagnostic opt-in only for debug; release always retains shared links.
        buildConfigField("boolean", "DEBUG_PAUSE_SHARED_LIBRARY", "false")
        buildConfigField("String", "FIREBASE_APP_ID", firebaseLiteral(firebaseClientInfo?.get("mobilesdk_app_id")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseLiteral(firebaseProject["project_id"]))
        buildConfigField("String", "FIREBASE_SENDER_ID", firebaseLiteral(firebaseProject["project_number"]))
        buildConfigField("String", "FIREBASE_API_KEY", firebaseLiteral(firebaseApiKey))
        if (firebaseClient != null) {
            resValue("string", "google_app_id", firebaseClientInfo?.get("mobilesdk_app_id").toString())
            resValue("string", "google_api_key", firebaseApiKey)
            resValue("string", "gcm_defaultSenderId", firebaseProject["project_number"].toString())
            resValue("string", "project_id", firebaseProject["project_id"].toString())
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 28
        targetSdk = 37
        versionCode = maxOf(14, localProperties.getProperty("APP_VERSION_CODE", "14").toInt()).also { require(it in 2..2_100_000_000) { "APP_VERSION_CODE deve ser pelo menos 2." } }
        versionName = "1.12-T1"

        buildConfigField(
            type = "String",
            name = "TMDB_READ_TOKEN",
            value = "\"${
                tmdbReadToken
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
            }\""
        )
    }

    // APKs completos por processador e um APK universal de reserva.
    // Mesmo applicationId, versionCode e assinatura em todas as saidas.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "DEBUG_PAUSE_SHARED_LIBRARY",
                providers.gradleProperty("iptvDebugPauseSharedLibrary").orNull
                    .let { if (it == "true") "true" else "false" })
        }
        release {
            optimization {
                enable = true
                packageScope = setOf(
                    "androidx.**",
                    "kotlin.**",
                    "kotlinx.**"
                )
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation("com.google.firebase:firebase-messaging:24.1.2")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0"
    )
    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0"
    )

    implementation("com.google.zxing:core:3.5.3")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)

    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.navigation.compose)

    implementation(
        "androidx.datastore:datastore-preferences:1.2.1"
    )

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // IPTVPremium - dois mecanismos internos de reproducao.
    // Media3/ExoPlayer e o mecanismo principal para Android TV e HDR nativo.
    val media3Version = "1.11.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // LibVLC integrado como player alternativo (nao abre app externo).
    // Biblioteca licenciada sob LGPL 2.1: observar obrigacoes na distribuicao.
    implementation("org.videolan.android:libvlc-all:3.7.7")

    androidTestImplementation(

        platform(
            libs.androidx.compose.bom
        )
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}

// IPTV_PORTAL_PRODUCTION_V1

// IPTV_MANDATORY_UPDATE_V1

// IPTV_FIREBASE_LIST_HINTS_V1

// IPTV_ABI_APKS_AND_UNIVERSAL_V1
