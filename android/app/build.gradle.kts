import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

/* TEST DEVICES — HOW YOU LOOK AT YOUR OWN AD UNITS WITHOUT GETTING BANNED.
 *
 * "The app isn't published yet, so I can request live ads" is the single most
 * common way an AdMob account gets suspended. Google's rule does not turn on
 * at publication: any request from a device you control, against your own ad
 * units, is invalid traffic whether the listing exists or not. Clicking one of
 * those ads is worse.
 *
 * The sanctioned route is to register the device. A registered device gets a
 * real request, through your real ad unit, filled with a test creative — so
 * you see your placement, your frequency, your mediation, and none of it is
 * counted or paid.
 *
 * Find the id in logcat on first launch; the SDK prints it for you:
 *
 *     I/Ads: Use RequestConfiguration.Builder()
 *              .setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
 *            to get test ads on this device.
 *
 * Then pass it in — comma-separated for more than one — WITHOUT committing it,
 * because it identifies a physical phone:
 *
 *     ./gradlew assembleRelease -Pringshift.testDeviceIds=33BE2250B43518CCDA7DE426D04EE231
 *
 * or put the same line in ~/.gradle/gradle.properties so Android Studio picks
 * it up too. Unset, it compiles to an empty list and changes nothing. */
val testDeviceIds: String =
    (project.findProperty("ringshift.testDeviceIds") as String? ?: "")
        .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        .joinToString(",")

/* Force the UK/EEA consent form to appear (or not) from anywhere, so the
 * consent flow can be tested without flying. Honoured only for a registered
 * test device, which is the UMP SDK's rule rather than ours.
 *
 *     ./gradlew installRelease -Pringshift.testDeviceIds=<id> \
 *                              -Pringshift.consentGeography=EEA
 */
val consentGeography: String =
    (project.findProperty("ringshift.consentGeography") as String? ?: "").trim()

/* RELEASE SIGNING, FROM A FILE THAT IS NOT IN THIS REPOSITORY.
 *
 * Create android/keystore.properties (git-ignored) with:
 *
 *     storeFile=/absolute/path/to/ringshift.jks
 *     storePassword=...
 *     keyAlias=ringshift
 *     keyPassword=...
 *
 * Absent, the release build is simply unsigned, exactly as it was before —
 * a missing key must not break the build for anyone who only wants to read
 * the code. */
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val hasSigning = keystoreProps.getProperty("storeFile")?.isNotBlank() == true

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

        versionCode = 54
        versionName = "5.4"

        // No instrumentation tests: the thing under test is a web page, and it
        // has its own headless suite driven by Playwright.
        resourceConfigurations += listOf("en")

        /* THE ADMOB APPLICATION ID, INJECTED INTO THE MANIFEST.
           A placeholder rather than a literal in AndroidManifest.xml so the
           id lives with the other two and there is one place to change when
           the account does. */
        manifestPlaceholders["admobAppId"] = "ca-app-pub-6248261164711853~1977343919"

        // Comma-separated, empty by default. Both build types get it: the whole
        // point is to make a RELEASE build safe to run on your own phone.
        buildConfigField("String", "AD_TEST_DEVICES", "\"$testDeviceIds\"")

        /* Forces the consent form to appear (or not) regardless of where
           the phone is. Honoured only for a registered test device — the
           SDK's rule, not ours. EEA | NOT_EEA | unset.
             -Pringshift.consentGeography=EEA */
        buildConfigField("String", "CONSENT_GEOGRAPHY", "\"$consentGeography\"")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
                /* Schemes are left to AGP, which picks from minSdk. At 24 that
                 * is v2 and no v1 — verified on the output rather than assumed,
                 * because setting enableV1Signing here does not produce a v1
                 * signature and a comment claiming otherwise would be a lie. */
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false

            /* GOOGLE'S TEST AD UNITS, NOT YOURS, AND THIS IS NOT OPTIONAL.

               Requesting a live ad from a build you are developing against is
               invalid traffic. AdMob does not warn — it suspends the account,
               and an account suspension takes the whole app's revenue with
               it. These two are Google's published test units; they always
               fill, they always show a test card, and they earn nothing. */
            buildConfigField("String", "AD_INTERSTITIAL",
                "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "AD_REWARDED",
                "\"ca-app-pub-3940256099942544/5354046379\"")
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

            // The live units. Only a release build ever asks for a real ad.
            buildConfigField("String", "AD_INTERSTITIAL",
                "\"ca-app-pub-6248261164711853/4350170471\"")
            buildConfigField("String", "AD_REWARDED",
                "\"ca-app-pub-6248261164711853/7945534623\"")
            /* Signed only if android/keystore.properties exists. No keystore
             * is committed and none ever should be; without one this produces
             * app-release-unsigned.apk exactly as before. */
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
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
    implementation(libs.androidx.webkit)        // WebViewAssetLoader, WebViewClientCompat, WebMessageListener
    implementation(libs.play.services.ads)      // AdMob
    implementation(libs.user.messaging.platform) // UMP — UK/EEA consent
}
