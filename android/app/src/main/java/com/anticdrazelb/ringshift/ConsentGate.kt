package com.anticdrazelb.ringshift

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * THE CONSENT GATE — THE ONE THING BETWEEN THIS APP AND A UK RELEASE.
 *
 * Google's EU User Consent Policy requires a consent mechanism before ads are
 * served to anyone in the UK, the EEA or Switzerland, and AdMob enforces it
 * against the account rather than the app. The mechanism is the User
 * Messaging Platform: this class asks it whether consent is needed, shows the
 * form if it is, and only then lets the ad SDK start.
 *
 * Two halves, and only one of them is code:
 *
 *  1. **Here.** Ask, show, report back.
 *  2. **In the AdMob console.** Privacy & messaging → GDPR → create a message,
 *     add the privacy policy URL, and **publish** it. UMP shows what you have
 *     configured there and nothing else.
 *
 * If half 2 is missing the SDK behaves correctly and unhelpfully: a UK device
 * gets consent status REQUIRED, no form to show, and `canRequestAds()` false —
 * which is *no ads at all in your home market*, silently. That case is logged
 * loudly below rather than left to be discovered in the earnings report.
 *
 * Outside those regions there is nothing to consent to: the status comes back
 * NOT_REQUIRED, no form appears, and ads start immediately.
 *
 * Nothing here can stop the game. Every failure path ends in the same call to
 * `then(...)`, and the worst outcome is an app with no ads in it.
 */
class ConsentGate(private val activity: Activity) {

    companion object {
        private const val TAG = "RingshiftConsent"
    }

    private val info: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /** True once a choice exists that the player is entitled to change. */
    val privacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Ask, show if needed, then hand back whether ads may be requested.
     *
     * `then(false)` is a normal outcome, not an error: it is what a player who
     * declined looks like, and the app must be a complete game for them.
     */
    fun run(then: (Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply { debugSettings()?.let { setConsentDebugSettings(it) } }
            .build()

        info.requestConsentInfoUpdate(
            activity, params,
            {
                /* Loads and shows the form only if the status calls for it, and
                 * calls back on the main thread either way. */
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "consent form: ${formError.errorCode} ${formError.message}")
                    }
                    report()
                    then(info.canRequestAds())
                }
            },
            { requestError ->
                /* Could not reach the consent service. `canRequestAds()` still
                 * answers from whatever was stored last time, so a player who
                 * consented yesterday is not punished for a bad train tunnel. */
                Log.w(TAG, "consent info: ${requestError.errorCode} ${requestError.message}")
                then(info.canRequestAds())
            }
        )
    }

    /**
     * Let the player change their mind. Only meaningful when
     * [privacyOptionsRequired] is true; the game only offers the row then.
     */
    fun showPrivacyOptions(then: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "privacy options: ${formError.errorCode} ${formError.message}")
            }
            then()
        }
    }

    /** Wipes the stored choice so the form can be seen again. Debug only. */
    fun reset() {
        if (BuildConfig.DEBUG) info.reset()
    }

    private fun report() {
        val status = when (info.consentStatus) {
            ConsentInformation.ConsentStatus.REQUIRED -> "REQUIRED"
            ConsentInformation.ConsentStatus.NOT_REQUIRED -> "NOT_REQUIRED"
            ConsentInformation.ConsentStatus.OBTAINED -> "OBTAINED"
            else -> "UNKNOWN"
        }
        Log.i(TAG, "status=$status canRequestAds=${info.canRequestAds()} " +
                "privacyOptions=$privacyOptionsRequired")

        /* THE SILENT FAILURE THIS EXISTS TO MAKE LOUD.
         * Consent is required, the SDK has no form to show, so no ad will ever
         * be requested on this device. That is not a bug in the code — it is a
         * message that was never published in the AdMob console. */
        if (!info.canRequestAds() && !info.isConsentFormAvailable) {
            Log.e(TAG, "consent required but NO FORM IS AVAILABLE — no ads will " +
                    "serve here. Publish a GDPR message in AdMob: " +
                    "Privacy & messaging > GDPR > create, add the privacy policy " +
                    "URL, then Publish.")
        }
    }

    /**
     * Debug settings let you see the European form from anywhere. Both values
     * come from the build, so nothing about a physical device is committed:
     *
     *     -Pringshift.testDeviceIds=<hashed id from logcat>
     *     -Pringshift.consentGeography=EEA        (or NOT_EEA, or unset)
     *
     * `DEBUG_GEOGRAPHY_EEA` is honoured **only** for a registered test device,
     * which is the SDK's rule and not ours.
     */
    private fun debugSettings(): ConsentDebugSettings? {
        val ids = BuildConfig.AD_TEST_DEVICES
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val geo = when (BuildConfig.CONSENT_GEOGRAPHY.uppercase()) {
            "EEA" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA
            "NOT_EEA" -> ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA
            else -> return null
        }
        if (ids.isEmpty()) {
            Log.w(TAG, "consentGeography set but no test device registered — ignored")
            return null
        }
        return ConsentDebugSettings.Builder(activity)
            .setDebugGeography(geo)
            .apply { ids.forEach { addTestDeviceHashedId(it) } }
            .build()
    }
}
