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

/**
 * RINGSHIFT.
 *
 * The game is one self-contained HTML file in `assets/`. It has no network
 * calls, no sibling assets and no server. This class exists to give it a
 * window, a real origin, and the handful of behaviours a WebView gets wrong
 * by default — see WEBVIEW.md in the repository root, which this file is the
 * implementation of.
 *
 * The page and the host communicate in exactly one direction: the host calls
 * `evaluateJavascript` and reads the result. There is no `@JavascriptInterface`
 * and there must never be one.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView
    private var pageReady = false

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

        /* 1. HARDWARE ACCELERATION.
         *
         * Without it there is no WebGL and the game shows its "this device
         * cannot run RINGSHIFT" card instead of starting. It is on by default
         * at the application level, and this line only guards against a
         * software layer type being set on the view somewhere else. */
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

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
            // ways the system bars come back.
            hideSystemBars()
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

    // ------------------------------------------------------------- lifecycle

    /* The game must not keep rendering behind the task switcher. pauseTimers
     * stops its rAF loop and its audio scheduler; the countdown and the
     * arrival both tick on the wall clock and both guard against a gap, so
     * they resume where they were rather than losing the time. */
    override fun onPause() {
        super.onPause()
        abandonAudioFocus()
        web.onPause()
        web.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
        web.onResume()
        hideSystemBars()
        requestAudioFocus()
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
