import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.plugin.serialization")
}


/**
 * The below code segments loads environment-specific application properties included during build time
 * This includes the application database and authentication configuurations
 * The properties are to be included in a file called 'app.properties' located in the project root directory
 */
val appPropertiesFile = rootProject.file("app.properties")
val appProperties = Properties()
if (appPropertiesFile.exists()){
    appProperties.load(FileInputStream(appPropertiesFile))
}


android {
    namespace = "com.example.bantunow"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.bantunow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //Adds the application properties that can be referenced in application code
        buildConfigField("String", "DATABASE_URL", "\"https://bantunow-default-rtdb.asia-southeast1.firebasedatabase.app/\"")
        buildConfigField("String", "AUTH_WEB_CLIENT_ID", "\"76839332560-6b6v6v6v6v6v6v6v6v6v6v6v6v6v6v6v.apps.googleusercontent.com\"")
        
        // Gemini API Key from local.properties
        val geminiKey = project.rootProject.file("local.properties").let {
            if (it.exists()) {
                val props = Properties()
                props.load(it.inputStream())
                props.getProperty("GEMINI_API_KEY", "")
            } else ""
        }
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.webkit)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.googleid)
    implementation(libs.material)
    implementation(libs.play.services.location)
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.kotlinx.serialization.json)
}