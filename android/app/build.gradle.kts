plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.anticdrazelb.ringshift"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anticdrazelb.ringshift"

        /* minSdk 24 — Android 7.0.
         *
         * The real floor is not the OS, it is the WebView: the game needs
         * Chromium 79 for CSS min()/max()/clamp() and wants 90+ so flex `gap`
         * and `aspect-ratio` behave. Android System WebView updates through
         * the Play Store independently of the OS, so a 2016 phone that still
         * gets Play updates is fine and a 2021 phone with Play Services
         * stripped out may not be. 24 is where adaptive icons stop being
         * available, which is why there are PNG launcher icons alongside
         * them in res/mipmap-*. */
        minSdk = 24
        targetSdk = 35

        versionCode = 51
        versionName = "5.1"

        // No instrumentation tests: the thing under test is a web page, and it
        // has its own headless suite driven by Playwright.
        resourceConfigurations += listOf("en")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            /* The APK is one Kotlin file and a 1.3MB HTML asset. Shrinking the
             * code saves almost nothing, but it also costs nothing and keeps
             * the release path honest. Resource shrinking is ON and the asset
             * is not a resource, so the game is not at risk from it. */
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Sign with your own key before publishing. Left unset on purpose:
            // a committed keystore is worse than an unsigned build.
        }
    }

    /* THE GAME ASSET MUST NOT BE COMPRESSED... or rather, it must not be
     * left uncompressed. index.html is 1.3MB of text and compresses to about
     * a fifth of that, which is the difference between a 1.4MB and a 400KB
     * download. This is the default; it is stated because the temptation with
     * a large single asset is to add it to noCompress for load speed, and
     * decompressing 1.3MB is not the slow part of starting this game. */

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true   // MainActivity reads BuildConfig.DEBUG
        viewBinding = false
        compose = false
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "DebugProbesKt.bin",
            "kotlin-tooling-metadata.json"
        )
    }

    lint {
        // The build must not pass with a broken manifest or a missing string.
        abortOnError = true
        warningsAsErrors = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)      // WindowCompat, WindowInsetsControllerCompat
    implementation(libs.androidx.activity)      // ComponentActivity, onBackPressedDispatcher
    implementation(libs.androidx.webkit)        // WebViewAssetLoader, WebViewClientCompat
}
