package com.anticdrazelb.ringshift

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

/**
 * THE AD SIDE, AND ONLY THE AD SIDE.
 *
 * This class knows how to have an ad ready and how to put one on the screen.
 * It knows nothing about when one should be shown — every "every third level",
 * "every fifth death", "ninety second cooldown" decision lives in the game,
 * in one policy block, where it can be read and tested without an Android
 * device anywhere near it.
 *
 * That split is deliberate. Ad policy is a product decision that will be
 * argued about and tuned; it should not require a rebuild of an APK to
 * change, and it should not be spread across two languages.
 *
 * The contract is four words wide:
 *
 *     request(kind, tag)  ->  result(tag, outcome)
 *
 * `kind` is "interstitial" or "rewarded". `outcome` is one of:
 *
 *     earned    the rewarded ad ran to the end — pay out
 *     shown     an interstitial was displayed and dismissed
 *     skipped   a rewarded ad was closed before the reward
 *     nofill    nothing to show; no network, no inventory, still loading
 *     failed    the SDK refused at show time
 *
 * Every request produces exactly one result. The game also runs its own ten
 * second timeout underneath, because "exactly one" is a promise this class
 * makes and a promise the SDK does not.
 */
class AdHost(
    private val activity: Activity,
    private val onResult: (String, String) -> Unit,
    /** Called on the main thread with true just before an ad covers the
     *  screen and false once it is gone. Exactly once each, per shown ad. */
    private val onCover: (Boolean) -> Unit = {}
) {

    companion object {
        private const val TAG = "RingshiftAds"
    }

    private var ready = false
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedInterstitialAd? = null
    private var loadingInterstitial = false
    private var loadingRewarded = false
    private var covering = false

    /** True from just before `show()` until the ad is dismissed or fails.
     *
     *  The host has to know, because the obvious way to pause a game while an
     *  ad plays — `WebView.pauseTimers()` — is documented as global to every
     *  WebView in the process, and this SDK draws its ads in a WebView. */
    val showing: Boolean get() = covering

    private fun cover(on: Boolean) {
        if (covering == on) return
        covering = on
        onCover(on)
    }

    /** True once the SDK has initialised and the bridge may be installed. */
    val initialised: Boolean get() = ready

    /**
     * Initialisation is slow and does network, so it happens off the main
     * thread and off the critical path — the game is already drawing by the
     * time this is called, and if it never finishes, nothing breaks: the
     * bridge is simply never installed and every trigger in the game finds no
     * host and carries on.
     */
    fun start(then: () -> Unit) {
        Thread {
            registerTestDevices()
            MobileAds.initialize(activity) {
                activity.runOnUiThread {
                    ready = true
                    preload()
                    then()
                }
            }
        }.start()
    }

    /**
     * REGISTER THIS PHONE, OR DO NOT LOOK AT YOUR OWN ADS.
     *
     * A debug build already asks for Google's test units, so it is safe by
     * construction. A release build asks for the real ones, and the moment you
     * side-load that onto your own phone to check a placement, every request
     * is invalid traffic — the rule does not wait for the app to be published,
     * and the penalty is account suspension rather than a warning.
     *
     * A registered device gets a real request, through the real ad unit,
     * filled with a test creative. Nothing is counted and nothing is paid, and
     * you are looking at your own configuration rather than Google's.
     *
     * The ids come from `-Pringshift.testDeviceIds=...` at build time so no
     * device identifier is ever committed. Unset, this is a no-op and a
     * release build is a release build.
     */
    private fun registerTestDevices() {
        val ids = BuildConfig.AD_TEST_DEVICES
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) {
            /* Said out loud, because the failure mode is silent and expensive:
             * a release build with no registered device, running on a desk. */
            if (!BuildConfig.DEBUG) {
                Log.w(TAG, "release build, no test devices registered — " +
                        "any ad you see on this device is live traffic")
            }
            return
        }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(ids).build()
        )
        Log.i(TAG, "test devices registered: ${ids.size}")
    }

    /**
     * Keep one of each in the chamber. An ad that has to be fetched at the
     * moment it is wanted is an ad the player waits for, and a player waiting
     * on a spinner they did not ask for is worse than no ad at all.
     */
    fun preload() {
        if (!ready) return
        loadInterstitial()
        loadRewarded()
    }

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            activity, BuildConfig.AD_INTERSTITIAL, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    loadingInterstitial = false
                    interstitial = null
                    Log.i(TAG, "interstitial no fill: ${e.message}")
                }
            }
        )
    }

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedInterstitialAd.load(
            activity, BuildConfig.AD_REWARDED, AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    loadingRewarded = false
                    rewarded = ad
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    loadingRewarded = false
                    rewarded = null
                    Log.i(TAG, "rewarded no fill: ${e.message}")
                }
            }
        )
    }

    /** Called from the bridge. Always answers, exactly once. */
    fun request(kind: String, tag: String) {
        activity.runOnUiThread {
            if (!ready) { done(tag, "nofill"); return@runOnUiThread }
            if (kind == "rewarded") showRewarded(tag) else showInterstitial(tag)
        }
    }

    private fun showInterstitial(tag: String) {
        val ad = interstitial
        if (ad == null) { loadInterstitial(); done(tag, "nofill"); return }
        interstitial = null                       // one shot; the next is loaded on dismiss
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                cover(false); loadInterstitial(); done(tag, "shown")
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                cover(false); loadInterstitial(); done(tag, "failed")
            }
        }
        cover(true)
        ad.show(activity)
    }

    private fun showRewarded(tag: String) {
        val ad = rewarded
        if (ad == null) { loadRewarded(); done(tag, "nofill"); return }
        rewarded = null
        /* `earned` is latched on the reward callback and reported on DISMISS,
         * not on the callback itself. The two arrive in that order and the
         * game must not be handed a reward while the ad is still covering the
         * screen — it would unlock a ship behind an ad the player is still
         * watching. */
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                cover(false); loadRewarded()
                Log.i(TAG, "rewarded dismissed, earned=$earned")
                done(tag, if (earned) "earned" else "skipped")
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                cover(false); loadRewarded(); done(tag, "failed")
            }
        }
        cover(true)
        ad.show(activity) { earned = true; Log.i(TAG, "reward earned") }
    }

    private fun done(tag: String, outcome: String) {
        activity.runOnUiThread { onResult(tag, outcome) }
    }
}
