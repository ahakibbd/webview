import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.offlinescanner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.offlinescanner"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // APK ছোট রাখতে শুধু দরকারি প্রসেসর
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    buildTypes {
        release {
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

    androidResources {
        // ভাষার ফাইল সংকুচিত না করে রাখা (দ্রুত কপি হয়)
        noCompress += "traineddata"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // Tesseract OCR ইঞ্জিন (Android-এর জন্য)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.7.0")
}

// ------------------------------------------------------------------
// ভাষার ডাটা: বিল্ডের সময় একবার ইন্টারনেট থেকে নেমে APK-এর ভেতরে ঢুকে যায়।
// ফলে অ্যাপ চালাতে ফোনে আর ইন্টারনেট লাগে না।
//
// আরও ভাষা চাইলে তালিকায় যোগ করুন, যেমন "hin" (হিন্দি), "urd" (উর্দু)।
// বেশি নির্ভুলতা চাইলে "tessdata_fast" এর জায়গায় "tessdata_best" লিখুন (ফাইল বড় হবে, স্ক্যান ধীর হবে)।
// ------------------------------------------------------------------
val tessLanguages = listOf("ben", "eng", "ara")
val tessRepo = "tessdata_fast"
val tessAssetsDir = layout.projectDirectory.dir("src/main/assets/tessdata").asFile

val downloadTessdata by tasks.registering {
    description = "Tesseract ভাষার ফাইল নামায়"
    outputs.dir(tessAssetsDir)
    doLast {
        tessAssetsDir.mkdirs()
        tessLanguages.forEach { lang ->
            val target = File(tessAssetsDir, "$lang.traineddata")
            if (target.exists() && target.length() > 10_000) return@forEach
            val url = "https://github.com/tesseract-ocr/$tessRepo/raw/main/$lang.traineddata"
            println("ডাউনলোড হচ্ছে: $lang  ($url)")
            try {
                val conn = URI(url).toURL().openConnection()
                conn.connectTimeout = 30_000
                conn.readTimeout = 120_000
                conn.getInputStream().use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                target.delete()
                throw GradleException(
                    "'$lang' ভাষার ফাইল নামানো যায়নি। ইন্টারনেট দেখে আবার Sync/Build করুন, " +
                    "অথবা $url থেকে হাতে নামিয়ে app/src/main/assets/tessdata/ ফোল্ডারে রাখুন। কারণ: ${e.message}"
                )
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadTessdata)
}
