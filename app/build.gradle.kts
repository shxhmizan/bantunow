import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

//The database configurstion can be stored in a file called database,properties in the project root directory
//The contents of the database.properties file should look like this:
//DATABASE_URL="<YOUR FIREBASE DATABASE URL HERE>"

val databasePropertiesFile = rootProject.file("database.properties")

val databaseProperties = Properties()

if (databasePropertiesFile.exists()){
    databaseProperties.load(FileInputStream(databasePropertiesFile))
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

        //Defines configuration field for database url
        buildConfigField("String", "DATABASE_URL", databaseProperties.getProperty("DATABASE_URL",""))
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
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}