import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.serialization)
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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.bantunow"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //Defines configuration field for database url
        val dbUrl = databaseProperties.getProperty("DATABASE_URL", "")
        buildConfigField("String", "DATABASE_URL", "\"${dbUrl.removeSurrounding("\"")}\"")

        //Defines configuration fields for the Supabase project. Override SUPABASE_URL /
        //SUPABASE_KEY in database.properties to point at a different project.
        val supabaseUrl = databaseProperties.getProperty("SUPABASE_URL", "https://rqitqgtaivdtdqbkjrap.supabase.co")
        val supabaseKey = databaseProperties.getProperty("SUPABASE_KEY", "sb_publishable_uUgYmcsp0Iy6lWRUVIVEtQ_BT8Ygm9C")
        buildConfigField("String", "SUPABASE_URL", "\"${supabaseUrl.removeSurrounding("\"")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${supabaseKey.removeSurrounding("\"")}\"")
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
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
