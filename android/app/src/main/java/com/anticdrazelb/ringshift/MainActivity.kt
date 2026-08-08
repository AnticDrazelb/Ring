package com.anticdrazelb.ringshift

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import org.json.JSONObject

/**
 * RINGSHIFT.
 *
 * The game is one self-contained HTML file in `assets/`. It has no network
 * calls, no sibling assets and no server. This class exists to give it a
 * window, a real origin, and the handful of behaviours a WebView gets wrong
 * by default — see WEBVIEW.md in the repository root, which this file is the
 * implementation of.
 *
 * MESSAGES BETWEEN THE TWO SIDES
 *
 * Host to page is `evaluateJavascript`, which exposes nothing.
 *
 * Page to host used to be impossible, and with ads it stops being optional —
 * the game has to be able to say "show one". That is done with
 * `WebViewCompat.addWebMessageListener`, NOT `@JavascriptInterface`, and the
 * difference matters:
 *
 *   - addWebMessageListener is scoped to an explicit ALLOWED ORIGIN. Only a
 *     page served from our own asset loader can see the object at all.
 *   - It passes strings. @JavascriptInterface passes a live Java object, and
 *     everything reachable from it, to anything running in the page.
 *
 * So the page can still only ASK, and it can only ask for one thing.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false

    private var ads: AdHost? = null
    private var consent: ConsentGate? = null
    /* The SDK can be ready before the page is. installAdBridge() then has
     * nothing to inject into, so it leaves a note for onPageFinished. */
    private var adBridgeWanted = false

    private val vibrator: android.os.Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as android.os.VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }
        } catch (e: Exception) { null }
    }
    private val audioManager by lazy { getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    // A real https origin rather than file://. See setUpWebView.
    private val loader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* THE HARDWARE VOLUME KEYS MUST MOVE THE GAME.
         *
         * Without this an Activity routes them to whichever stream it thinks
         * is in play, which for an app that has never called into AudioManager
         * is the RINGER. The player turns the game up, the ringtone gets
         * louder, and the game does not. It is one line and it is the single
         * most common complaint about WebView games. */
        volumeControlStream = AudioManager.STREAM_MUSIC

        /* EDGE TO EDGE, INCLUDING UNDER EVERY CUTOUT.
         *
         * SHORT_EDGES only lets the window into the cutout on the short edges,
         * which means a phone turned sideways gets letterboxed away from its
         * own camera and the game loses a black bar down one side. ALWAYS
         * covers every orientation and every cutout shape — hole punch, notch,
         * waterfall — and hands the responsibility for keeping content clear
         * of them to us, which is handled in pushSafeArea(). */
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    else
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        /* The window is black before the WebView has drawn anything. The game's
         * first paint is a black studio card, so any other colour here is a
         * white flash on every cold start. */
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))

        web = WebView(this)
        setUpWebView(web)
        setContentView(web)

        hideSystemBars()
        watchInsets()

        /* ADS.
         *
         * Initialised off the critical path, and the bridge is installed only
         * once the SDK says it is ready. Until then — and forever, if it
         * never manages it — `window.__rsAdHost` does not exist, every
         * trigger in the game finds no host, and the game plays exactly as it
         * does in a browser. Ads are an addition to this app, never a
         * dependency of it. */
        ads = AdHost(
            this,
            onResult = { tag, outcome ->
                if (pageReady) {
                    web.evaluateJavascript(
                        "window.__rsAdResult && window.__rsAdResult(" +
                            JSONObject.quote(tag) + "," + JSONObject.quote(outcome) + ")", null
                    )
                }
            },
            onCover = { on -> adCover(on) }
        )

        /* CONSENT FIRST, ADS SECOND, AND THE GAME REGARDLESS.
         *
         * In the UK, the EEA and Switzerland an ad may not be requested until
         * the player has been asked. The gate asks, shows the form if one is
         * needed, and answers whether ads may run at all. `false` is a normal
         * answer — a player who declined — and the only consequence is an app
         * with no ads in it, which must still be a complete game.
         *
         * Everywhere else the status comes back NOT_REQUIRED, no form appears,
         * and this costs one asynchronous round trip that the game spends
         * drawing its studio card. */
        consent = ConsentGate(this)
        consent?.run { canRequestAds ->
            if (canRequestAds) ads?.start { installAdBridge() }
            pushPrivacyOptions()
        }

        /* BACK.
         *
         * Ask the GAME, not the WebView's history.
         *
         * The page keeps a browser history of its own — a sentinel entry it
         * pushes so a browser Back closes whatever screen is open. Driving
         * that from here with canGoBack() costs a press: the answer is always
         * yes because the sentinel is armed, so the first Back at the main
         * menu is spent popping a sentinel while the game decides it had
         * nothing to close, and the second one quits. Measured, before this:
         * two presses to leave the menu and the first did nothing.
         *
         * `__rsBack()` returns true if it closed something. False means there
         * is nothing left, and that is the press that finishes the activity.
         * evaluateJavascript hands back a result without exposing anything to
         * the page, so this needs no bridge. */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!pageReady) { finish(); return }
                web.evaluateJavascript("(window.__rsBack && window.__rsBack()) === true") { r ->
                    if (r != "true") {
                        // Nothing left to close. Fall back to the WebView's own
                        // history in case the hook is missing, then give up.
                        if (web.canGoBack()) web.goBack() else finish()
                    }
                }
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setUpWebView(v: WebView): Unit = v.run {

        /* Before anything else, and before loadUrl: the message channel only
         * reaches documents that start after it is added. */
        installBridge(v)

        /* 1. HARDWARE ACCELERATION, AND THE LINE THAT WAS QUIETLY COSTING US
         *    A FULL-SCREEN COPY EVERY FRAME.
         *
         * This used to say LAYER_TYPE_HARDWARE, which sounds like the way to
         * ask for the GPU and is not. WebGL is available because the WINDOW is
         * hardware accelerated — android:hardwareAccelerated, on by default
         * since API 14 and declared explicitly in the manifest. The layer type
         * is a different question: it asks whether this View's output should
         * be rendered into an off-screen texture before being composited.
         *
         * That is a win for a view whose content is STATIC while the view
         * itself is animated — the texture is drawn once and reused. It is a
         * straight loss for a view repainting sixty times a second, because
         * the layer is invalidated on every one of those frames: the whole
         * screen is rendered into an FBO and then that FBO is drawn to the
         * screen. One extra full-screen write and read per frame, on a
         * renderer that is already fill-rate bound.
         *
         * NONE is the default. It is written out here because the value is
         * load-bearing and the wrong one looks more correct than the right
         * one. LAYER_TYPE_SOFTWARE is the value that would genuinely break
         * WebGL, and it is the one to check for if the game ever shows its
         * "cannot run" card on a device with a GPU. */
        setLayerType(View.LAYER_TYPE_NONE, null)

        /* KEEP THE RENDERER PROCESS IMPORTANT.
         *
         * A WebView's renderer runs in its own process and Android is free to
         * deprioritise or kill it under memory pressure. The default policy
         * waives priority the moment the view is not visible, which is how a
         * game comes back from the task switcher having been reaped.
         * IMPORTANT with waived=false says: this is the app, not a background
         * tab. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
        }

        settings.apply {
            javaScriptEnabled = true

            /* 2. DOM STORAGE.
             *
             * The entire save is localStorage — progress, stars, credits,
             * settings, the daily, the per-level ledger and every grade. This
             * is OFF by default in a WebView, and off means every session
             * silently starts again from level 1. */
            domStorageEnabled = true

            /* 3. AUDIO WITHOUT A GESTURE.
             *
             * The game builds its AudioContext on the first touch anyway, so
             * this is not strictly required — but leaving it true also
             * suspends the context on some WebView versions after a resume,
             * and a game that comes back from the task switcher silent is a
             * bug nobody can explain. */
            mediaPlaybackRequiresUserGesture = false

            // The layout is already responsive from 320dp up and both of these
            // fight it.
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100

            // Nothing is fetched. Saying so is cheaper than a policy.
            blockNetworkLoads = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        /* 4. BACKGROUND. Same reason as the window above: the default is white. */
        setBackgroundColor(Color.BLACK)
        overScrollMode = View.OVER_SCROLL_NEVER
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false

        // The screen must not go out mid-run.
        keepScreenOn = true

        /* A long press in a WebView selects text and pops a context menu. In a
         * game a long press is a held control, and the magnifier landing on
         * top of it is the end of the run. */
        isLongClickable = false
        setOnLongClickListener { true }
        setHapticFeedbackEnabled(false)   // the game issues its own, precisely

        /* SERVE IT, DO NOT file:// IT.
         *
         * loadUrl("file:///android_asset/index.html") mostly works and then
         * does not: several WebView versions give file:// pages an opaque
         * origin, and an opaque origin has no localStorage. The game runs and
         * quietly never saves. The asset loader gives it a real https origin
         * and a real, persistent storage bucket, and costs one class. */
        webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)

            // Nothing in the game navigates anywhere. If something ever tries,
            // it is not going to be this WebView that follows it.
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = true

            override fun onPageFinished(view: WebView, url: String) {
                pageReady = true
                // The first measurement usually happens before the page exists.
                pushSafeArea(ViewCompat.getRootWindowInsets(view))
                // The consent answer usually lands before the page does.
                pushPrivacyOptions()
                // ...and so does the ad SDK, on a warm start.
                if (adBridgeWanted) installAdBridge()

                /* THE WINDOW BACKGROUND HAS DONE ITS JOB — STOP PAINTING IT.
                 *
                 * It exists so the first frame of a cold start is black rather
                 * than white. Once the WebView is drawing, it is a full-screen
                 * opaque fill underneath a full-screen opaque view: every
                 * pixel painted twice, every frame, forever. Dropping it is
                 * the single cheapest overdraw win an app like this has. */
                window.setBackgroundDrawable(null)
            }

            /* THE WEBVIEW RENDERER CAN BE KILLED WHILE YOU ARE IN THE
             * BACKGROUND, AND THE DEFAULT IS THAT IT TAKES THE APP WITH IT.
             *
             * Returning false here — or not overriding this at all — means the
             * whole process is torn down: the player switches away, Android
             * reclaims memory, and coming back they get a crash rather than
             * their game. Returning true says we handled it, and since the
             * save is in localStorage on disk, rebuilding the view loses
             * nothing but the current run. */
            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                if (view !== web) return true
                pageReady = false
                (web.parent as? android.view.ViewGroup)?.removeView(web)
                web.destroy()
                val fresh = WebView(this@MainActivity)
                web = fresh
                setUpWebView(fresh)
                setContentView(fresh)
                hideSystemBars()
                watchInsets()
                fresh.loadUrl("https://appassets.androidplatform.net/assets/index.html")
                return true
            }
        }

        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }

    // --------------------------------------------------------------- bridge

    /**
     * THE ONE CHANNEL THE PAGE CAN SPEAK ON, INSTALLED BEFORE THE PAGE LOADS.
     *
     * `addWebMessageListener` injects its object at **document start**, which
     * means for documents that start after the call. The previous version
     * installed it from the ad SDK's ready callback — a background thread
     * racing the page load — so on any launch where the SDK won an already
     * loaded document never saw `rsAds`, and the `typeof rsAds === 'undefined'`
     * guard turned that into a silent no-ads app. It is called from
     * setUpWebView now, before `loadUrl`, which is the documented order and
     * the only one that is not a race.
     *
     * Origin-scoped to the asset loader and nothing else; strings only. The
     * page may ask for three things by name and can reach none of them:
     *
     *     {kind:'interstitial'|'rewarded', tag}   show an ad
     *     {kind:'privacy'}                        reopen the consent choice
     *     {kind:'vibrate', p:[…]}                 play a haptic pattern
     */
    private fun installBridge(v: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        try {
            WebViewCompat.addWebMessageListener(
                v, "rsHost", setOf("https://appassets.androidplatform.net")
            ) { _, message, _, _, _ ->
                val body = message.data ?: return@addWebMessageListener
                try {
                    val o = JSONObject(body)
                    when (o.optString("kind")) {
                        "privacy" -> consent?.showPrivacyOptions { pushPrivacyOptions() }
                        "vibrate" -> vibrate(o.optJSONArray("p"))
                        else -> ads?.request(o.optString("kind"), o.optString("tag"))
                    }
                } catch (e: Exception) {
                    // A malformed message is a bug on the page's side, and the
                    // page is ours. Drop it; do not let it reach anything.
                }
            }
        } catch (e: Exception) {
            return          // no bridge, still a game
        }
        /* The haptic hook is installed with the bridge rather than with the
         * ads, because it must work for a player who declined consent. */
        v.evaluateJavascript(
            """
            (function(){
              if (typeof rsHost === 'undefined') return;
              window.__rsVibrate = function(p){
                rsHost.postMessage(JSON.stringify(
                  {kind:'vibrate', p: (typeof p === 'number') ? [p] : (p || [])}));
              };
              window.__rsPrivacy = function(){
                rsHost.postMessage(JSON.stringify({kind:'privacy'}));
              };
            })();
            """.trimIndent(), null
        )
    }

    // ------------------------------------------------------------------ ads

    /**
     * Hand the page an ad host. Only the shim — the channel underneath it was
     * installed before the page loaded. `window.__rsAdHost` appearing is the
     * game's single signal that ads exist at all, so it must not appear until
     * the SDK can actually serve one.
     */
    private fun installAdBridge() {
        if (!pageReady) { adBridgeWanted = true; return }
        adBridgeWanted = false
        web.evaluateJavascript(
            """
            (function(){
              if (typeof rsHost === 'undefined') return;
              window.__rsAdHost = { request: function(kind, tag){
                rsHost.postMessage(JSON.stringify({kind:kind, tag:tag}));
              }};
              /* The SDK finishes two to five seconds after the menu is up, and
                 the hangar's WATCH AD chip and the death card's Continue
                 button are both drawn from "do ads exist" at the moment their
                 screen was built. Tell the page so it can redraw them. */
              if (window.__rsAdsReady) window.__rsAdsReady();
            })();
            """.trimIndent(), null
        )
        pushPrivacyOptions()
    }

    // ------------------------------------------------------------- haptics

    /**
     * WHY THE GAME DOES NOT JUST USE `navigator.vibrate`.
     *
     * It does, in a browser, and it is the whole API there. In the app it
     * routes here for one reason: amplitude. Chromium asks the system for
     * `DEFAULT_AMPLITUDE`, and One UI multiplies that by the user's "Vibration
     * intensity" slider, which ships nowhere near maximum. A game's haptics
     * then arrive at a fraction of the strength they were designed at, on
     * exactly the hardware — a rotary motor — that already had the least to
     * give.
     *
     * `createWaveform` with an explicit amplitude asks for full power instead.
     * The page has already floored every pulse at 22ms, which is the other
     * half of the same problem: below that a rotary motor never spins up.
     */
    private fun vibrate(arr: org.json.JSONArray?) {
        val vib = vibrator ?: return
        try {
            if (arr == null || arr.length() == 0) { vib.cancel(); return }
            val timings = LongArray(arr.length()) { arr.optLong(it, 0L).coerceIn(0L, 5000L) }
            // A single zero is the page saying "stop".
            if (timings.size == 1 && timings[0] == 0L) { vib.cancel(); return }
            if (timings.sum() == 0L) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Even indices are vibrations, odd are pauses — the same shape
                // navigator.vibrate takes, and the shape the page builds.
                val amps = IntArray(timings.size) { if (it % 2 == 0) 255 else 0 }
                vib.vibrate(android.os.VibrationEffect.createWaveform(timings, amps, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            // A phone with no vibrator, or a manufacturer with opinions.
        }
    }

    /**
     * Tell the page whether a "Privacy choices" row is due.
     *
     * It is due exactly when the consent SDK says the player has a choice they
     * are entitled to revisit — which is a UK/EEA player and nobody else. The
     * page shows the row on that flag alone, so a player in a region with no
     * consent requirement never sees a control that would do nothing.
     *
     * Pushed again after the form closes, because declining can change the
     * answer.
     */
    private fun pushPrivacyOptions() {
        if (!pageReady) return
        val on = consent?.privacyOptionsRequired == true
        web.evaluateJavascript(
            "window.__rsPrivacyOptions = $on;" +
                "window.__rsPrivacySync && window.__rsPrivacySync();", null
        )
    }

    // ---------------------------------------------------------------- insets

    /**
     * THE NOTCH, MEASURED HERE AND HANDED TO THE PAGE.
     *
     * `env(safe-area-inset-*)` reads **zero** in an Android WebView. It is
     * plumbed through Chromium's own display-cutout handling and a WebView is
     * not Chromium's window — it is a View inside somebody else's Activity.
     * Setting `viewport-fit=cover` and the right cutout mode does not change
     * that; the page simply has no way to find out where the camera is.
     *
     * So the host measures it. The game's sixteen safe-area reads go through
     * `var(--sa-top, env(...))` and friends, which means env() is only ever
     * the default — writing these four variables onto the root element is the
     * seam that makes a notch real to the page.
     *
     * The union of the display cutout and the system bars is used, not just
     * the cutout: on a device with a gesture pill and no notch the bottom
     * inset still matters, and on one with both the larger of the two wins.
     */
    private fun watchInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(web) { _, insets ->
            pushSafeArea(insets)
            // A cutout change is also a rotation, and a rotation is one of the
            // ways the system bars come back. Not while an ad is up, though:
            // AdActivity is translucent and shows the bars, so re-hiding them
            // on every inset pass is a fight the ad has to relayout through.
            if (ads?.showing != true) hideSystemBars()
            insets
        }
        ViewCompat.requestApplyInsets(web)
    }

    private fun pushSafeArea(insets: WindowInsetsCompat?) {
        if (insets == null || !pageReady) return
        val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val d = resources.displayMetrics.density
        fun px(a: Int, b: Int) = (maxOf(a, b) / d).toInt()

        val js = """
            (function(){
              var r = document.documentElement.style;
              r.setProperty('--sa-top',    '${px(cutout.top, bars.top)}px');
              r.setProperty('--sa-right',  '${px(cutout.right, bars.right)}px');
              r.setProperty('--sa-bottom', '${px(cutout.bottom, bars.bottom)}px');
              r.setProperty('--sa-left',   '${px(cutout.left, bars.left)}px');
              window.dispatchEvent(new Event('resize'));
            })();
        """.trimIndent()
        web.evaluateJavascript(js, null)
    }

    // ------------------------------------------------------------ fullscreen

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        /* BY_SWIPE, not BY_TOUCH. The transient variant brings the bars back
         * for a swipe from the edge and hides them again on their own; the
         * touch variant would show them on any tap, which in this game is
         * every single input. */
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(focus: Boolean) {
        super.onWindowFocusChanged(focus)
        // A transient bar swipe, the notification shade, a permission dialog,
        // the task switcher — all of them leave the bars up on the way back.
        if (focus) hideSystemBars()
    }

    // ----------------------------------------------------------- audio focus

    /**
     * ASK FOR THE SPEAKER, AND GIVE IT BACK.
     *
     * Without this the game talks over a phone call, keeps playing under a
     * navigation prompt, and fights whatever music was already going. Web
     * Audio in a WebView does not participate in audio focus on its own — the
     * host has to, and then tell the page.
     *
     * The duck rides on the page's master gain, so it never touches the
     * player's own volume settings and their sliders read the same afterwards.
     */
    private fun requestAudioFocus() {
        if (hasFocus) return
        val listener = AudioManager.OnAudioFocusChangeListener { change ->
            when (change) {
                AudioManager.AUDIOFOCUS_GAIN -> duck(false)
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duck(true)
                AudioManager.AUDIOFOCUS_LOSS -> { duck(true); hasFocus = false }
            }
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(listener)
                .build()
            focusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasFocus = false
    }

    private fun duck(on: Boolean) {
        if (!pageReady) return
        web.evaluateJavascript("window.__rsDuck && window.__rsDuck($on)", null)
    }

    // ------------------------------------------------------------ ad cover

    /**
     * AN AD IS ABOUT TO COVER THE SCREEN — AND THE OBVIOUS WAY TO HANDLE THAT
     * IS THE BUG THIS EXISTS TO FIX.
     *
     * Showing an ad starts a translucent AdActivity, which pauses this one, so
     * `onPause()` ran and called `WebView.pauseTimers()`. That call is
     * documented as **global to every WebView in the process** — and the
     * Mobile Ads SDK renders its ads in a WebView.
     *
     * So "pause the game while the ad plays" froze the ad. A rewarded ad's
     * "Reward in 8 seconds" never counted down, the close button it turns into
     * never appeared, the reward was never earned and the unlock never
     * happened — and because `resumeTimers()` only runs in `onResume()`, which
     * cannot happen until the ad is dismissed, the ad could not be dismissed
     * at all. An interstitial survived it only because its X is there from the
     * first frame.
     *
     * The replacement does the same job aimed at one view: the page is told to
     * stop drawing, which stops a nine-pass WebGL chain rendering a conduit
     * nobody can see while a video tries to play next door.
     */
    private fun adCover(on: Boolean) {
        if (on) {
            /* Undo any global pause already in effect before the ad gets going
             * — belt and braces against a race with onPause(). */
            web.resumeTimers()
            if (pageReady) web.evaluateJavascript("window.__rsAdOpen && window.__rsAdOpen(true)", null)
        } else {
            web.resumeTimers()
            if (pageReady) web.evaluateJavascript("window.__rsAdOpen && window.__rsAdOpen(false)", null)
            hideSystemBars()
        }
    }

    // ------------------------------------------------------------- lifecycle

    /* The game must not keep rendering behind the task switcher. pauseTimers
     * stops its rAF loop and its audio scheduler; the countdown and the
     * arrival both tick on the wall clock and both guard against a gap, so
     * they resume where they were rather than losing the time.
     *
     * It is skipped for one case, and the reason is the whole of adCover():
     * pauseTimers is global to every WebView in this process, an ad IS a
     * WebView, and an ad launches by pausing us. Freezing the game here froze
     * the ad with it. When the ad is ours, the page has already been told to
     * stop drawing and that is the whole of what was wanted. */
    override fun onPause() {
        super.onPause()
        abandonAudioFocus()
        if (ads?.showing == true) return
        web.onPause()
        web.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
        web.onResume()
        // Not while an ad owns the screen: yanking the system bars away from
        // underneath it relayouts the ad mid-play.
        if (ads?.showing != true) hideSystemBars()
        requestAudioFocus()
        // Top the chamber back up after an ad was spent, or after a spell in
        // the background with no network.
        ads?.preload()
        /* An AudioContext comes back SUSPENDED from a pause and Web Audio will
         * not restart it by itself. In a browser this is invisible because
         * every screen is one tap from a sound; in an app the player comes
         * back mid-run and the game is silent until they happen to touch
         * something. Ask for it now; the next tap is the fallback. */
        if (pageReady) {
            web.evaluateJavascript("window.__rsAudio && window.__rsAudio()", null)
            duck(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        abandonAudioFocus()
        // Detach before destroy, or the view hierarchy holds a dead WebView.
        (web.parent as? android.view.ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }
}
