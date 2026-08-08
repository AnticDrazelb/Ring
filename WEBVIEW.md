# Running RINGSHIFT in an Android WebView

`index.html` is the whole game — one file, no network, no assets beside it. It
will run in a WebView, but a WebView is not a browser and four of its defaults
are wrong for this. Getting those four right is the difference between the game
working and a black screen.

> **There is a finished Android Studio project in [`android/`](android/) that
> does all of this.** Open that directory in Android Studio and run it. What
> follows is the reasoning behind what it does, and what you need if you are
> hosting the game in an app of your own instead.

---

## The engine floor

| Needs | Chromium | Why |
|---|---|---|
| **Hard floor** | **79** | CSS `min()` / `max()` / `clamp()`, in 22 declarations — nine of them the safe-area padding that keeps the HUD out from under the notch. An engine that does not know them drops the whole declaration, so the HUD loses its position rather than degrading. |
| Recommended | **90+** | Below 84 flex and grid `gap` collapses, and the page sets it 46 times, so the chrome bunches up. Below 88 `aspect-ratio` falls back — the menu tiles keep their `min-height`, so they stay usable, but they stop being square. |
| Nice to have | 108+ | `100svh`, so the boot card is centred on the visible viewport from the first frame rather than after the toolbar settles. There is a `visualViewport` fallback that covers this anyway. |

Everything above that floor degrades on purpose — `aspect-ratio` has an
`@supports` fallback, `100svh` has `100vh` under it, and every full-bleed layer
writes `top/right/bottom/left` in full rather than `inset`, precisely so an
older engine cannot collapse the canvas.

**Android System WebView updates through the Play Store**, so most devices are
far above this. The ones that are not are usually devices with Play Services
stripped out.

---

## The four settings that matter

```kotlin
val web = WebView(this)

// 1. HARDWARE ACCELERATION — AND THE LINE THAT LOOKS LIKE IT AND IS NOT.
//
//    WebGL is available because the WINDOW is hardware accelerated:
//    android:hardwareAccelerated, on by default since API 14. The LAYER TYPE
//    is a different question — whether this View's output is rendered into an
//    off-screen texture before compositing.
//
//    LAYER_TYPE_HARDWARE is a win for a static view being animated, because
//    the texture is drawn once and reused. It is a straight LOSS for a view
//    repainting sixty times a second: the layer is invalidated every frame,
//    so the whole screen is rendered into an FBO and then that FBO is drawn
//    to the screen. One extra full-screen write and read per frame, on a
//    renderer that is already fill-rate bound.
//
//    NONE is the default and the right answer. LAYER_TYPE_SOFTWARE is the
//    value that genuinely breaks WebGL — check for it if the "cannot run"
//    card ever appears on a device with a GPU.
web.setLayerType(View.LAYER_TYPE_NONE, null)

// Keep the renderer process important. A WebView's renderer runs in its own
// process, and the default policy waives its priority the moment the view is
// not visible — which is how a game comes back from the task switcher having
// been reaped.
web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)

web.settings.apply {
    javaScriptEnabled = true

    // 2. DOM STORAGE. The entire save — progress, stars, credits, settings,
    //    the daily, the per-level ledger — is localStorage. Off by default in
    //    a WebView, and off means every session starts from level 1 with no
    //    warning that anything is wrong.
    domStorageEnabled = true

    // 3. AUDIO WITHOUT A GESTURE. The game builds its AudioContext on the
    //    first touch anyway, so this is not required — but leaving it true
    //    also suspends the context on some versions after a resume.
    mediaPlaybackRequiresUserGesture = false

    // no pinch-zoom, no text autosizing: the layout is already responsive
    // and both of them fight it
    setSupportZoom(false)
    builtInZoomControls = false
    textZoom = 100
}

// 4. BACKGROUND. The default is white, and the game's first paint is black.
//    Without this every launch flashes white before the studio card.
web.setBackgroundColor(Color.BLACK)
web.overScrollMode = View.OVER_SCROLL_NEVER
```

...and then **throw the window background away** once the page is up:

```kotlin
override fun onPageFinished(view: WebView, url: String) {
    window.setBackgroundDrawable(null)
}
```

It exists so the first frame of a cold start is black rather than white. After
that it is a full-screen opaque fill underneath a full-screen opaque view —
every pixel painted twice, every frame, forever. It is the cheapest overdraw
win an app like this has.

---

## Is it actually using the GPU?

A software renderer draws the same picture as a GPU, just slowly, so "hardware
acceleration is off" and "this phone is a bit slow" look identical from the
sofa. **Settings answers it**: under the version line the game prints the
unmasked WebGL renderer and its current render scale.

```
Adreno (TM) 740 · render scale 2.00×
⚠ SOFTWARE RENDERER · Mesa/X.org, llvmpipe · render scale 0.75×
```

If that second line ever appears on a real handset, the cause is one of three
things, in order of likelihood: `LAYER_TYPE_SOFTWARE` set on the view,
`android:hardwareAccelerated="false"` somewhere in the manifest, or a device
whose GPU driver is blocklisted by Chromium.

The render scale beside it is the adaptive tier the game has settled on, so
the two figures together explain any frame rate you are looking at.

And in the manifest, on the `<application>` or the hosting `<activity>`:

```xml
android:hardwareAccelerated="true"
```

### Back, which costs a press if you ask the wrong thing

`if (web.canGoBack()) web.goBack() else finish()` is the standard snippet and
it is wrong here. The page keeps its own history — a sentinel entry pushed so
a browser Back closes whatever screen is open — so `canGoBack()` is *always*
true, and the first press at the main menu is spent popping a sentinel while
the game decides it had nothing to close. Measured: **two presses to leave the
menu, and the first one did nothing.**

Ask the game instead:

```kotlin
web.evaluateJavascript("(window.__rsBack && window.__rsBack()) === true") { r ->
    if (r != "true") finish()
}
```

`evaluateJavascript` returns a result without exposing anything to the page,
so this needs no `@JavascriptInterface`. True means something was closed;
false is the press that quits.

### The permission everyone forgets

```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

The game's haptics are `navigator.vibrate`, and without this it fails
**silently**: no crash, no log, no exception to catch — just a game that never
buzzes and a Haptics switch in Settings that appears to do nothing. It is a
normal permission, granted at install, with no runtime prompt.

### The notch: `env(safe-area-inset-*)` reads ZERO in a WebView

This is the one that looks like it works and does not. `viewport-fit=cover` in
the page, `LAYOUT_IN_DISPLAY_CUTOUT_MODE` on the window, everything by the
book — and the insets still come back **0**, because `env(safe-area-inset-*)`
is plumbed through Chromium's own display-cutout handling and a WebView is not
Chromium's window. It is a View inside somebody else's Activity and it has no
idea where the camera is.

So the host measures and the page reads a variable:

```kotlin
ViewCompat.setOnApplyWindowInsetsListener(web) { _, insets ->
    val cut  = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
    val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
    // the union, not just the cutout: a gesture pill with no notch still
    // needs the bottom, and a device with both takes the larger
    web.evaluateJavascript("""
        var r = document.documentElement.style;
        r.setProperty('--sa-top',    '${'$'}{max(cut.top, bars.top) / density}px');
        ...
    """, null)
    insets
}
```

The game's sixteen safe-area reads are written `var(--sa-top, env(...))`, so
`env()` is only ever the default. In a browser nothing changes; in the app the
four variables are real. Use **`LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS`** on API
30+ rather than `SHORT_EDGES`, or a phone turned sideways is letterboxed away
from its own camera and loses a black bar down one edge.

### Sound, which has three separate ways to be wrong

```kotlin
volumeControlStream = AudioManager.STREAM_MUSIC
```

Without that one line the hardware volume keys move the **ringer**. The player
turns the game up, their ringtone gets louder, and the game does not.

Then **audio focus**: Web Audio in a WebView does not participate in it, so
without a request from the host the game talks over phone calls and fights
whatever was already playing. Request `AUDIOFOCUS_GAIN` with
`USAGE_GAME`, and on loss call into the page to duck — the game exposes
`window.__rsDuck(bool)`, which rides the master gain so the player's own
volume settings are untouched and read the same afterwards.

Then **resume**: an `AudioContext` comes back *suspended* from a pause and Web
Audio will not restart it. In a browser this is invisible because every screen
is one tap from a sound; in an app the player returns mid-run to silence. Call
`window.__rsAudio()` from `onResume`.

### The renderer can be killed, and by default it takes the app with it

```kotlin
override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean
```

Not overriding this means the process is torn down when Android reclaims the
WebView renderer in the background: the player switches away, comes back, and
gets a crash instead of their game. Rebuild the view and reload — the save is
on disk, so nothing is lost but the current run.

### And the one nobody expects

```kotlin
// Android 10 and up reads a horizontal swipe from either edge as Back.
// This game is STEERED by horizontal swipes.
view.systemGestureExclusionRects = listOf(Rect(0, top, width, bottom))
```

Without it, a steer that begins near the side of the screen — which is most of
them, because that is where a thumb reaches — navigates out of the run instead
of turning the conduit. The system caps the claim at 200dp per edge and
silently drops anything larger, so take the middle 200dp and leave the rest of
the edge to the system gesture.

---

## Serve it, do not `file://` it

```kotlin
val loader = WebViewAssetLoader.Builder()
    .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
    .build()

web.webViewClient = object : WebViewClientCompat() {
    override fun shouldInterceptRequest(v: WebView, req: WebResourceRequest) =
        loader.shouldInterceptRequest(req.url)
}
web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
```

`loadUrl("file:///android_asset/index.html")` mostly works and then does not:
several WebView versions give `file://` pages an opaque origin, and an opaque
origin has **no localStorage**. The game will run and quietly never save. An
asset loader gives the page a real https origin and a real, persistent storage
bucket, and costs one class.

---

## What the game already handles

You do not need to add error handling around it.

- **No WebGL** — a card explaining what to try, with the save left alone.
- **A boot-time script error** — a card naming the likely cause, which on a
  WebView is nearly always an engine older than the floor above. There is no
  console in a WebView, so a black screen would tell the player nothing.
- **An error after the first frame** — swallowed. The game is demonstrably
  running by then and one async throw is not a reason to replace it.
- **A frame rate it cannot hold** — the render resolution is walked down a
  tier at a time first; the post-processing chain is only dropped once it is
  already at the floor, and the player is told.
- **A screen size you did not test** — the UI has no fixed pixel sizes left in
  it. Every control height and every type size is a `clamp()` calibrated to
  hit its designed value at 430dp and shrink continuously below that, so a
  320dp phone gets the same layout at a smaller size rather than a different
  one. Anything tappable holds a hard 44px floor regardless of how small the
  screen gets — the chrome shrinks, the touch targets do not.
- **A corrupt or hand-edited save** — checked and repaired on load.
- **`prefers-reduced-motion`** — respected without being asked. It softens the
  countdown's warp and suppresses its long rumble; it deliberately does *not*
  silence ordinary haptics, which are feedback rather than animation.
- **A device with no vibrator, or the permission missing** — `navigator.vibrate`
  is called inside a try/catch and behind a feature test, so the game does not
  care either way.

---

## Checking it worked

Load the game and open **Settings**. The version line at the bottom tells you
which build you are running. Then:

1. Clear a level, force-quit the app, reopen it. If you are back on level 1,
   `domStorageEnabled` is off or you are on a `file://` origin.
2. If the WebGL card appears on a device that certainly has a GPU, the layer
   type is software.
3. If the layout is bunched or the tiles are not square, the WebView is below
   84 and wants updating.
4. Rotate the phone and resize the window if you can. Nothing should jump: the
   layout has no width breakpoints left that change scale, only ones that
   *hide* a chip when the top tray runs out of room.
