# RINGSHIFT — Android

An Android Studio project that wraps `index.html` in a WebView. The game is the
whole app: one self-contained HTML file in `app/src/main/assets/`, no network,
no sibling assets, no server.

This project is the implementation of [`../WEBVIEW.md`](../WEBVIEW.md). If the
two ever disagree, that document is the reasoning and this is the code.

---

## Opening it

**Android Studio** → *Open* → select this `android/` directory. Sync, then run.

**Command line** (needs `local.properties` or `ANDROID_HOME`):

```
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew installDebug           # to a connected device
./gradlew assembleRelease        # unsigned; see "Publishing"
```

If Gradle cannot find your SDK, copy `local.properties.example` to
`local.properties` and set `sdk.dir`. That file is git-ignored on purpose —
it is a path on your machine, not a property of the project.

---

## What is set up, and why it is not optional

| | |
|---|---|
| `setLayerType(LAYER_TYPE_HARDWARE)` | No hardware layer means no WebGL, and the game shows its "this device cannot run RINGSHIFT" card instead of starting. |
| `domStorageEnabled = true` | **Off by default in a WebView.** The entire save is `localStorage` — progress, stars, credits, settings, the daily, the per-level ledger, every grade. Off means every session silently restarts at level 1. |
| `WebViewAssetLoader` | `file:///android_asset/` gets an **opaque origin** on several WebView versions, and an opaque origin has no `localStorage`. The game would run and quietly never save. The loader gives it a real `https://appassets.androidplatform.net` origin and a real storage bucket. |
| `mediaPlaybackRequiresUserGesture = false` | The game builds its AudioContext on first touch anyway, but leaving this true also suspends the context after a resume on some versions — a game that comes back from the task switcher silent. |
| `VIBRATE` permission | `navigator.vibrate` fails **silently** without it: no crash, no log, just a game that never buzzes. |
| `configChanges=...` | Without the full list, rotating the phone recreates the activity, which reloads the WebView, which restarts the game and abandons the run. |
| `systemGestureExclusionRects` | Android 10+ reads a horizontal swipe from either edge as Back. This game is *steered* by horizontal swipes. The middle 200dp of each edge is claimed, which is the cap the system allows. |
| `setBackgroundColor(BLACK)` on both window and WebView | The default is white and the game's first paint is black — otherwise every cold start flashes. |
| `volumeControlStream = STREAM_MUSIC` | Without it the hardware volume keys move the **ringer**. The player turns the game up, their ringtone gets louder, and the game does not. |
| Audio focus + `window.__rsDuck()` | Web Audio in a WebView does not participate in audio focus, so without a host request the game talks over phone calls and fights whatever was already playing. |
| `window.__rsAudio()` on resume | An `AudioContext` comes back **suspended** from a pause and will not restart itself. Without this the player returns mid-run to silence. |
| Insets → `--sa-*` | `env(safe-area-inset-*)` reads **zero** in a WebView — it is plumbed through Chromium's own cutout handling and a WebView is not Chromium's window. The host measures and injects; the game's 16 safe-area reads are `var(--sa-top, env(...))`. |
| `CUTOUT_MODE_ALWAYS` | `SHORT_EDGES` letterboxes a sideways phone away from its own camera and loses a black bar down one edge. |
| `window.__rsBack()` | `canGoBack()` is always true because the page arms a history sentinel, so the standard snippet spends the first Back at the main menu doing nothing. Asking the game costs no press. |
| `onRenderProcessGone` | Not overriding it means the app is **killed** when Android reclaims the WebView renderer in the background. |
| `setLayerType(LAYER_TYPE_NONE)` | **Not** `LAYER_TYPE_HARDWARE`, which sounds like asking for the GPU and is not. It renders the view into an off-screen texture first — free for a static view being animated, a full-screen copy *every frame* for one repainting at 60fps. Hardware acceleration comes from the window, not the layer. |
| `RENDERER_PRIORITY_IMPORTANT`, waived=false | The default waives the renderer process's priority the moment the view is not visible, which is how a game gets reaped in the task switcher. |
| `window.setBackgroundDrawable(null)` on page finish | It exists to stop a white flash on cold start. After that it is a full-screen opaque fill under a full-screen opaque view — every pixel painted twice, forever. |

There is deliberately **no `INTERNET` permission** and no
`@JavascriptInterface` bridge.

Communication runs in exactly **one direction**: the host calls
`evaluateJavascript` and reads the result. The page can be asked things
(`__rsBack`, `__rsAudio`, `__rsDuck`); it cannot reach anything. A
`@JavascriptInterface` would invert that and hand the page a Java object,
which is the difference between "the game has a bug" and "the game has a bug
that can touch the filesystem".

---

## Ads

AdMob, four placements, and the split is deliberate:

- **`AdHost.kt` knows how to show an ad.** Preload, show, report an outcome.
- **The game knows when one should be shown.** Every "every third level",
  "every fifth death", "ninety second cooldown" decision is one policy block
  in `index.html` — readable and testable without an Android device, and
  tunable without rebuilding an APK.

The contract is four words wide: `request(kind, tag)` → `result(tag, outcome)`,
where outcome is `earned` / `shown` / `skipped` / `nofill` / `failed`.

### Test ads in debug, live ads only in release

`buildConfigField` puts **Google's test ad units** in the debug build and
yours in release. Verified in the shipped bytes: a debug APK contains only
`ca-app-pub-3940256099942544/…` and no real unit id at all.

This is not a nicety. Requesting a live ad from a build you are developing
against is invalid traffic, and AdMob does not warn you — it suspends the
account, which takes the whole app's revenue with it.

### Looking at your *own* ad units without getting banned

"The app isn't published yet, so live ads are fine for testing" is the most
common way an AdMob account gets suspended. The rule does not switch on at
publication — a request from a device you control, against your own unit, is
invalid traffic whether a listing exists or not, and clicking one is worse.

The sanctioned route is to **register the device**. A registered device gets a
real request, through your real ad unit, filled with a test creative: you see
your placement, your frequency, your mediation, and none of it is counted or
paid.

Launch once and read the id out of logcat — the SDK prints it for you:

```
adb logcat -s Ads | grep setTestDeviceIds
I/Ads: Use RequestConfiguration.Builder()
         .setTestDeviceIds(Arrays.asList("33BE2250B43518CCDA7DE426D04EE231"))
       to get test ads on this device.
```

Then pass it back in — comma-separated for more than one:

```
./gradlew assembleRelease -Pringshift.testDeviceIds=33BE2250B43518CCDA7DE426D04EE231
```

or put the same line in `~/.gradle/gradle.properties` so Android Studio picks
it up too. It is a build property rather than a committed constant because it
identifies a physical phone. Unset, it compiles to an empty list and changes
nothing — and a release build with nothing registered says so in logcat:
`release build, no test devices registered — any ad you see on this device is
live traffic`.

### Seeing where the ads land without building anything

The placements themselves can be checked in a desktop browser. Open
`index.html?ads=sim` and a fake host is installed: the **real** policy runs —
every cooldown, the session floor, the hour cap, the once-per-account ship
unlock — and only the final "draw a Google ad" step is replaced with a dashed
gold placeholder.

| | |
|---|---|
| `?ads=sim` | an ad plays; rewarded pays out → `earned` / `shown` |
| `?ads=skip` | the player closes a rewarded early → `skipped` |
| `?ads=nofill` | nothing in inventory, answered instantly → `nofill` |

It cannot reach the shipped app: the host loads a bare asset URL with no query
string, there is no way for a player to add one, and the simulator refuses to
install if a real host is already attached.

### The bridge is not `@JavascriptInterface`

The page has to be able to say "show one", which is the first time anything
here needs to talk *upwards*. It uses `WebViewCompat.addWebMessageListener`,
scoped to `https://appassets.androidplatform.net` and nothing else, passing
strings. `@JavascriptInterface` would hand the page a live Java object and
everything reachable from it.

### What the INTERNET permission changed

Before ads this app could not reach the network at all, which is a strong
thing to be able to say, and it can no longer be said. What is still true —
and enforced rather than promised — is that the **game** cannot: the WebView
keeps `blockNetworkLoads = true` and is served from inside the APK, so every
byte on the wire belongs to the Mobile Ads SDK and none of it to the page.

### Before you publish — one of these is a blocker

1. **A consent mechanism for the UK and EEA.** Google's EU User Consent Policy
   requires one before serving ads to users there, and AdMob enforces it. That
   means the **UMP SDK** (`com.google.android.ump:user-messaging-platform`),
   plus a privacy message configured in the AdMob console — the SDK only
   fetches and shows what you have set up there, so this cannot be finished
   from the code side alone. **Not implemented here.** Shipping to a UK or EEA
   audience without it puts the AdMob account at risk — and the privacy policy
   in `../PRIVACY.md` already promises a consent message, so until UMP ships
   that promise is not true. Ship it, or cut that paragraph.
2. **A privacy policy URL**, in the Play listing and reachable from the app.
   Required once an app serves ads. One is written —
   [`../PRIVACY.md`](../PRIVACY.md), and as a page in
   [`../docs/privacy.html`](../docs/privacy.html) ready for GitHub Pages. It
   is drafted from what this project actually does and is **not legal
   advice**; read it before you publish it under your name.
3. **The Play Console Data safety form** — the Mobile Ads SDK collects a
   device identifier, and the form has to say so.
4. **Check the ids** in `app/build.gradle.kts` against your console. A new ad
   unit can take an hour to start filling; until then a release build gets
   `nofill`, which the game treats as "no ad today" and moves on.

---

## The engine floor

The limit is the WebView, not the OS version.

| Needs | Chromium |
|---|---|
| Hard floor | **79** — CSS `min()`/`max()`/`clamp()`, which the whole responsive layout is built on |
| Recommended | **90+** — below 84, flex/grid `gap` collapses; below 88, `aspect-ratio` falls back |

`minSdk` is **24** (Android 7.0). Android System WebView updates through the
Play Store independently of the OS, so a 2016 phone that still receives Play
updates is fine, while a newer device with Play Services stripped out may not
be. The game detects and explains both failure modes itself.

---

## Checking it actually worked

1. **Clear a level, force-quit, reopen.** Back on level 1 means `domStorageEnabled`
   is off or you are on a `file://` origin.
2. **Open Settings** — the version line at the bottom tells you which build is
   running.
3. **The WebGL card on a device with a GPU** means the layer type is software.
4. **Bunched layout or non-square menu tiles** means the WebView is below 84.
5. **Rotate the phone.** Nothing should jump and the run should survive; if the
   game restarts from the studio card, a `configChanges` value is missing.
6. **Steer with a swipe that starts near the edge of the screen.** If it
   navigates back instead of turning, the gesture exclusion is not applying —
   it is API 29+ only, and the system silently drops any claim over 200dp.
7. **Press the volume keys in a run.** If the ringer slider appears instead of
   media, `volumeControlStream` is not set.
8. **On a phone with a notch, look at the score pill.** If it is under the
   camera, the insets are not reaching the page — check that `__rsBack` and
   friends exist, because it means the whole hook set is missing.
9. **Press Back once at the main menu.** It should quit. If it takes two, the
   activity is using `canGoBack()` rather than asking the game.
10. **Switch away mid-run and come back.** Sound should return without a tap,
    and the app should not have restarted.
11. **Open Settings and read the line under the version.** It names the GPU —
    `Adreno (TM) 740 · render scale 2.00×`. If it says
    **⚠ SOFTWARE RENDERER**, the game is not on the GPU and the cause is one
    of: `LAYER_TYPE_SOFTWARE` on the view, `hardwareAccelerated="false"`
    somewhere in the manifest, or a driver Chromium has blocklisted.

    The render scale beside it is the adaptive tier the game settled on. It is
    also the honest way to judge any performance change on a real device:
    the same phone settling at a higher scale is the win, not a frame counter.

---

## Publishing

`assembleRelease` produces an **unsigned** APK. There is no keystore in this
repository and there should never be one.

```
# once
keytool -genkey -v -keystore ringshift.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias ringshift
```

Then add a `signingConfigs` block to `app/build.gradle.kts` reading from
environment variables or a git-ignored `keystore.properties`, and wire it to
the `release` build type.

Before you ship, change **`applicationId`** in `app/build.gradle.kts` and the
matching `namespace`. `com.anticdrazelb.ringshift` is a placeholder derived
from the repository owner; an application ID is permanent once published.

For the Play Store, build an App Bundle instead: `./gradlew bundleRelease`.

---

## Updating the game

`app/src/main/assets/index.html` is a **copy** of the file in the repository
root. It is not a symlink, because Gradle's asset packaging does not follow
them reliably across platforms.

```
cp ../index.html app/src/main/assets/index.html
```

Bump `versionCode` and `versionName` in `app/build.gradle.kts` to match the
version string the game prints at the bottom of its Settings screen.
