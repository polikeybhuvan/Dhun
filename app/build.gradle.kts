plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

val releaseStoreFile = providers.gradleProperty("DHUN_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.gradleProperty("DHUN_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.gradleProperty("DHUN_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.gradleProperty("DHUN_RELEASE_KEY_PASSWORD").orNull

android {
    compileSdk = 33

    defaultConfig {
        applicationId = "it.dhun.music"
        minSdk = 21
        targetSdk = 33
        versionCode = 22
        versionName = "1.0.1"
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    namespace = "it.vfsfitvnm.vimusic"

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Dhun"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "Dhun"
            if (releaseStoreFile != null && releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {
                signingConfig = signingConfigs.create("production") {
                    storeFile = file(releaseStoreFile)
                    storePassword = releaseStorePassword
                    keyAlias = releaseKeyAlias
                    keyPassword = releaseKeyPassword
                }
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    kotlinOptions {
        freeCompilerArgs += "-Xcontext-receivers"
        jvmTarget = "1.8"
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(projects.composePersist)
    implementation(projects.composeRouting)
    implementation(projects.composeReordering)

    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.ripple)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.coil)

    implementation(libs.palette)

    implementation(libs.exoplayer)

    implementation(libs.room)
    kapt(libs.room.compiler)

    implementation(projects.innertube)
    implementation(projects.kugou)

    coreLibraryDesugaring(libs.desugaring)
}
