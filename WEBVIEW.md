# Running RINGSHIFT in an Android WebView

`index.html` is the whole game — one file, no network, no assets beside it. It
will run in a WebView, but a WebView is not a browser and four of its defaults
are wrong for this. Getting those four right is the difference between the game
working and a black screen.

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

// 1. HARDWARE ACCELERATION. Without it there is no WebGL and the game shows
//    its "this device cannot run RINGSHIFT" card instead of starting.
//    It is on by default at the application level, but a software layer type
//    on the view overrides that and is a common copy-paste.
web.setLayerType(View.LAYER_TYPE_HARDWARE, null)

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

And in the manifest, on the `<application>` or the hosting `<activity>`:

```xml
android:hardwareAccelerated="true"
```

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
- **A frame rate it cannot hold** — the post-processing chain is dropped
  automatically and the player is told.
- **A corrupt or hand-edited save** — checked and repaired on load.
- **`prefers-reduced-motion`** — respected without being asked.

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
