package com.anticdrazelb.ringshift

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
 * window, a real origin and the four WebView settings whose defaults would
 * otherwise break it — see WEBVIEW.md in the repository root, which this file
 * is the implementation of.
 */
class MainActivity : ComponentActivity() {

    private lateinit var web: WebView

    // A real https origin rather than file://. See setUpWebView.
    private val loader by lazy {
        WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* EDGE TO EDGE, INCLUDING UNDER THE CUTOUT.
         *
         * The game's viewport meta already carries `viewport-fit=cover` and it
         * pads its own chrome with env(safe-area-inset-*) — nine declarations
         * of it. This is the half of that contract Android owns. Without
         * SHORT_EDGES the system letterboxes the page away from the notch and
         * the game ends up padding against a margin that is already there. */
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        /* The window is black before the WebView has drawn anything. The game's
         * first paint is a black studio card, so any other colour here is a
         * white flash on every cold start. */
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.BLACK))
        /* statusBarColor and navigationBarColor are deprecated as of API 35 and
         * do nothing under setDecorFitsSystemWindows(false) anyway — the bars
         * are transparent because the window is edge to edge, and hidden
         * because of hideSystemBars() below. The theme still declares them for
         * the API levels that read them. */

        web = WebView(this)
        setUpWebView()
        setContentView(web)

        hideSystemBars()
        claimEdgeGestures()

        /* BACK.
         *
         * The game manages its own history: it pushes a state on load and on
         * every screen it opens, and a popstate closes whatever is on top —
         * a document, the map, a pause card. So Back belongs to the WebView
         * for as long as it has anywhere to go, and to the activity exactly
         * once it does not. Anything cleverer here would fight it. */
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState)
        else web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun setUpWebView() = web.apply {

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
        }

        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
    }

    /**
     * ANDROID 10 TAKES A HORIZONTAL SWIPE FROM EITHER EDGE AS BACK, AND THIS
     * GAME IS STEERED BY HORIZONTAL SWIPES.
     *
     * Without this, a steer that begins near the side of the screen — which is
     * most of them, because that is where a thumb reaches — navigates back out
     * of the run instead of turning the conduit.
     *
     * The system caps exclusions at 200dp per edge and silently drops anything
     * over, so the claim is the middle 200dp of each side, where the thumb
     * actually rests. The top and bottom of each edge are left to the system
     * so the gesture is still there when the player wants it.
     */
    private fun claimEdgeGestures() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        web.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            val h = v.height
            if (h <= 0) return@addOnLayoutChangeListener
            val band = (200 * resources.displayMetrics.density).toInt().coerceAtMost(h)
            val top = ((h - band) / 2).coerceAtLeast(0)
            val bottom = (top + band).coerceAtMost(h)
            v.systemGestureExclusionRects = listOf(
                Rect(0, top, v.width, bottom)
            )
        }
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // A transient bar swipe, a notification shade, a permission dialog —
        // all of them leave the bars up on the way back.
        if (hasFocus) hideSystemBars()
    }

    /* The game must not keep rendering behind the task switcher. pauseTimers
     * stops its rAF loop and its audio scheduler; the countdown and the
     * arrival both tick on the wall clock and both guard against a gap, so
     * they resume where they were rather than losing the time. */
    override fun onPause() {
        super.onPause()
        web.onPause()
        web.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        web.resumeTimers()
        web.onResume()
        hideSystemBars()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        // Detach before destroy, or the view hierarchy holds a dead WebView.
        (web.parent as? android.view.ViewGroup)?.removeView(web)
        web.destroy()
        super.onDestroy()
    }
}
