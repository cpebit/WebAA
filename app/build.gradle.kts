import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.protobuf)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                // makes no sense and throws a warning, but compiles somehow, thank you Google
                // https://github.com/google/protobuf-gradle-plugin/issues/349
                create("java")
            }
        }
    }
}

// signing configs moved to prop file, outside of git
//val keystorePropertiesFile = rootProject.file("keystore.properties")
//val keystoreProperties = Properties()
//keystoreProperties.load(FileInputStream(keystorePropertiesFile))

android {
    namespace = "it.cpeb.teslaa"
    compileSdk = 36

    buildFeatures {
        viewBinding = false
        dataBinding = false
        buildConfig = true
    }


  /*  signingConfigs {
        create("config") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
*/

    defaultConfig {
        applicationId = "it.cpeb.teslaa"
        minSdk = 26
        targetSdk = 36
        vectorDrawables.useSupportLibrary = true
        versionCode = 42
        versionName = "WebAA - Web Interface for Android Auto 4.2"
        multiDexEnabled = true
        dexOptions {
            preDexLibraries = false
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
//            signingConfig = signingConfigs.getByName("config")
        }
        debug {
            isDebuggable = true
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            //signingConfig signingConfigs.config
        }
    }

    flavorDimensions += "app"

    productFlavors {
        create("germanstore") {
            dimension = "app"
            applicationId = "it.cpeb.webaa"
            resValue("string", "app_name", "Web Interface for Android Auto™")
            versionName = "WebAA - Web Interface for Android Auto™ 4.2"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation(libs.androidx.navigation)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.eventbus)
    implementation(libs.protobuf)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.work.runtime)
    implementation(libs.material)
    implementation(libs.dexmaker)
    implementation(libs.dexmaker.mockito)
    implementation(libs.websocket)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.tasker)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)
}
