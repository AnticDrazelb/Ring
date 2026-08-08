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
./gradlew assembleRelease        # see "Publishing"
./gradlew bundleRelease          # the .aab you upload to Play
```

If Gradle cannot find your SDK, copy `local.properties.example` to
`local.properties` and set `sdk.dir`. That file is git-ignored on purpose —
it is a path on your machine, not a property of the project.

---

## What is set up, and why it is not optional

| | |
|---|---|
| `domStorageEnabled = true` | **Off by default in a WebView.** The entire save is `localStorage` — progress, stars, credits, settings, the daily, the per-level ledger, every grade. Off means every session silently restarts at level 1. |
| `WebViewAssetLoader` | `file:///android_asset/` gets an **opaque origin** on several WebView versions, and an opaque origin has no `localStorage`. The game would run and quietly never save. The loader gives it a real `https://appassets.androidplatform.net` origin and a real storage bucket. |
| `mediaPlaybackRequiresUserGesture = false` | The game builds its AudioContext on first touch anyway, but leaving this true also suspends the context after a resume on some versions — a game that comes back from the task switcher silent. |
| `VIBRATE` permission | `navigator.vibrate` fails **silently** without it: no crash, no log, just a game that never buzzes. |
| `VibrationEffect.createWaveform` at amplitude 255 | `navigator.vibrate` asks for `DEFAULT_AMPLITUDE`, which One UI multiplies by the user's vibration-intensity slider — so the game's haptics arrive at a fraction of their designed strength on exactly the phones (rotary motors) with the least to give. The page routes patterns through `window.__rsVibrate` when the host is there. |
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

There is deliberately no `@JavascriptInterface` bridge.

Almost everything runs in one direction: the host calls `evaluateJavascript`
and reads the result. The page can be *asked* things — `__rsBack`, `__rsAudio`,
`__rsDuck`, `__rsPrivacySync` — and cannot reach anything.

The one upward channel is `WebViewCompat.addWebMessageListener`, scoped to
`https://appassets.androidplatform.net`, carrying strings, and accepting
exactly three verbs: "show an ad", "reopen my privacy choice", and "play this
haptic pattern". A
`@JavascriptInterface` would instead hand the page a live Java object and
everything reachable from it, which is the difference between "the game has a
bug" and "the game has a bug that can touch the filesystem".

The `INTERNET` permission arrived with ads and is used only by the Google SDKs;
the game's own WebView keeps `blockNetworkLoads = true`.

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

Open `index.html` in any browser and go to **Settings → Ad preview →
Simulate ads**. A fake host installs: the **real** policy runs — the hour cap,
the once-per-account ship unlock, one continue per level — and only the final
"draw a Google ad" step becomes a dashed gold placeholder. **Show one now**
puts an interstitial or a rewarded ad on screen immediately.

Two production timings are relaxed while previewing, because they exist to
protect a player from interruption and while previewing they only hide the
thing being previewed: the 120-second session floor is waived and the global
cooldown drops to 12 seconds. Everything else is the shipping policy, and when
a trigger is blocked the reason is shown as a toast rather than swallowed.

Same thing from a link, for sharing a repro:

| | |
|---|---|
| `?ads=sim` | an ad plays; rewarded pays out → `earned` / `shown` |
| `?ads=skip` | the player closes a rewarded early → `skipped` |
| `?ads=nofill` | nothing in inventory, answered instantly → `nofill` |
| `?ads=off` | back to normal |

It cannot reach a player. The Settings group is present only when the page was
**not** served from `appassets.androidplatform.net`, which is the app's own
origin and the only place the host serves from — so the test is where the page
came from, not a flag anyone could set. The simulator also refuses to install
over a real host.

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

### Consent — the UK and EEA gate

`ConsentGate.kt` is the UMP SDK. Before a single ad is requested it asks
whether consent is required, shows the form if it is, and answers whether ads
may run at all. A player who declines gets a complete game with no ads in it.
Outside those regions the status comes back NOT_REQUIRED, no form appears, and
the whole thing costs one round trip spent behind the studio card.

**Half of this is not code.** UMP shows only what you have configured in the
AdMob console:

> **Privacy & messaging → GDPR → create message → add the privacy policy URL →
> Publish.**

Skip it and the SDK behaves correctly and unhelpfully: a UK device gets consent
REQUIRED, no form available, `canRequestAds()` false — **no ads at all in your
home market**, silently. That case is logged as an error rather than left to be
found in the earnings report:

```
E/RingshiftConsent: consent required but NO FORM IS AVAILABLE — no ads will serve here.
```

To see the European form from anywhere, register a test device and force the
geography:

```
./gradlew installRelease -Pringshift.testDeviceIds=<id> \
                         -Pringshift.consentGeography=EEA
```

Where the player changes their mind: **Settings → Privacy choices**, which the
game shows only when the host says a choice exists to revisit. The host learns
that from `privacyOptionsRequirementStatus` and pushes it to the page; nobody
outside the UK/EEA is offered a control that would do nothing.

### Before you publish

1. **Publish the GDPR message** in the AdMob console — see above. This is the
   one that costs you your UK revenue if you forget it.
2. **A privacy policy URL**, in the Play listing and reachable from the app.
   One is written — [`../PRIVACY.md`](../PRIVACY.md), and as a page in
   [`../docs/privacy.html`](../docs/privacy.html) ready for GitHub Pages. It
   is drafted from what this project actually does and is **not legal
   advice**; read it before you publish it under your name.
3. **The Play Console Data safety form** — the Mobile Ads SDK collects a
   device identifier, and the form has to say so.
4. **Check the ids** in `app/build.gradle.kts` against your console. A new ad
   unit can take an hour to start filling; until then a release build gets
   `nofill`, which the game treats as "no ad today" and moves on.
5. **`applicationId`** is permanent once published. `com.anticdrazelb.ringshift`
   is derived from the repository owner; change it now or never.

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

## Diagnosing ads on a device

Settings prints two lines under the version. The second is the ad state, and it
exists because every way ads can fail is silent and they all look identical
from the sofa:

```
ADS · live host · 3 shown · next: cooldown
ADS · no host · consent declined, offline, or still starting
ADS · none in a browser · use Ad preview above
```

**"no host" in the UK almost always means the GDPR message is not published.**
UMP correctly reports consent REQUIRED, has no form to show, `canRequestAds()`
returns false, the ad SDK is never started, and the app is silently ad-free.
`adb logcat -s RingshiftConsent` says so explicitly.

---

## Three ways an opt-in ad can be wired correctly and still never be seen

All three shipped, and all three were invisible rather than broken.

1. **A CSS class collision.** The hangar's WATCH AD chip had been
   `.lockBadge.adBadge` since the ads went in. The browser preview badge added
   later claimed the bare `.adBadge` with `position:fixed` on it. Specificity
   resolves per *property*, not per rule, so the chip kept its colours and
   inherited the fixed positioning — torn out of the card it labels and parked
   at the bottom-left of the screen.
2. **A button that looked disabled.** The rewarded continue fired correctly
   when pressed, but the death card rendered one way: *Continue this run · 25*,
   greyed out when you could not afford it. Nobody presses a locked button with
   a price on it. It now says **Watch an ad to continue** whenever that is what
   it does.
3. **The host arriving after the screen was drawn.** Consent, then
   `MobileAds.initialize`, is two to five seconds of network on a background
   thread — and both opt-in offers are drawn from "do ads exist" at the moment
   their screen was built. Open the hangar quickly and the chip was never
   there, and nothing redrew it. The host now calls `window.__rsAdsReady()`
   when it attaches.

None of the three is detectable from the ad policy's unit tests, which is
where the coverage was: eighteen assertions all passing on functions that
returned exactly the right answers to a UI that never asked.

---

## Two things a real device found that a browser could not

**Haptics that were never felt.** A Galaxy S10+ reported "way less haptics than
expected". Nine of the game's cues were 8–14ms pulses, which is fine on a
linear resonant actuator and *nothing at all* on the eccentric-rotating-mass
motor most Android phones carry — the weight needs 20–30ms just to spin up. The
page now floors every pulse at 22ms and offers Off / Light / Medium / Strong,
and the host plays the pattern at full amplitude rather than at whatever the
system slider has been left on.

**A picture that was correctly, deliberately soft.** The same device looked
"unexpectedly pixelated" beside an iPhone browser. It was not a WebView
problem: the adaptive scaler had done its job. Its ladder ran down to 0.75×
*before* it would consider dimming the nine-pass post chain — and 0.75× is
below native, so every pixel on screen is an upscale, which is the most visible
degradation a phone has. The order is now: drop to native, then dim the chain,
and only then draw fewer pixels than the screen has. Settings → Resolution
overrides the whole thing, and the line under the version prints the scale
actually in use.

Neither was reproducible in a headless browser at any viewport. Both were
one real phone.

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

Signing is wired and waiting for a key. There is no keystore in this
repository and there should never be one.

```
# once — keep this file and its passwords safe; losing it means you can never
# update the app again under the same listing
keytool -genkeypair -v -keystore ringshift.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias ringshift
```

Then create `android/keystore.properties` — git-ignored, alongside `*.jks`:

```
storeFile=/absolute/path/to/ringshift.jks
storePassword=...
keyAlias=ringshift
keyPassword=...
```

```
./gradlew bundleRelease      # app/build/outputs/bundle/release/app-release.aab
./gradlew assembleRelease    # app/build/outputs/apk/release/app-release.apk
```

With that file present the release build is signed and the output loses its
`-unsigned` suffix; without it the build still succeeds and produces
`app-release-unsigned.apk`, because a missing key must not break the build for
someone who only wants to read the code. Verified end to end with a throwaway
key: `apksigner verify` reports one signer, APK Signature Scheme v2. AGP picks
the schemes from `minSdk`, which at 24 means v2 and no v1.

**Upload the `.aab`, not the APK.** Play requires a bundle for new apps, and
signs the delivered APKs with its own key from it.

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

---

## Launch checklist

Everything on this list is outside the code. The code is done.

| | |
|---|---|
| ☐ | **Publish a GDPR message** — AdMob → Privacy & messaging → GDPR → create → add the privacy policy URL → **Publish**. Without it, no ads serve in the UK or EEA. |
| ☐ | **Turn on GitHub Pages** — repository Settings → Pages → *Deploy from a branch* → default branch, `/docs`. The policy is then at `https://anticdrazelb.github.io/Ring/privacy.html`. |
| ☐ | **Read `PRIVACY.md`** end to end and make it true of you. Delete the publisher note at the top when you are happy with it. |
| ☐ | **Create the upload key** and `android/keystore.properties` — see Publishing. Back both up somewhere you will still have in five years. |
| ☐ | **`./gradlew bundleRelease`** and upload the `.aab`. |
| ☐ | **Play Console: Data safety** — declare the advertising ID and the device/IP data the Mobile Ads SDK collects. Say data is not collected by you, is shared with Google for advertising, and is not user-deletable on request because you never hold it. |
| ☐ | **Play Console: Ads declaration** — yes, the app contains ads. |
| ☐ | **Play Console: content rating, target audience, privacy policy URL.** Target audience must not include children — the app is not COPPA/families-designed. |
| ☐ | **Register your phone as a test device** and install a release build. Check the four placements and the consent form before anyone else sees them. |
| ☐ | **Confirm `applicationId`.** `com.anticdrazelb.ringshift` becomes permanent the moment the listing goes live. |
