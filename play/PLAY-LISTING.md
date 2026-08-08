# RINGSHIFT — Play Console pack

Everything the Play Console will ask for, drafted. The assets beside this file
are ready to upload as they are.

**Nothing here is legal advice, and the Console is the authority on its own
requirements** — they change. Where a rule is likely to have moved since this
was written it says so.

---

## 1. Assets in this folder

| File | Where it goes | Spec |
|---|---|---|
| `icon-512.png` | Store listing → App icon | 512×512, PNG, no alpha ✓ |
| `feature-1024x500.png` | Store listing → Feature graphic | 1024×500, PNG ✓ |
| `screen-1-menu.png` … `screen-6-record.png` | Phone screenshots | 1242×2208, 9:16 ✓ (min 2, max 8) |
| `tablet7-*.png` | 7-inch tablet screenshots | 1200×1920 ✓ (optional) |
| `tablet10-*.png` | 10-inch tablet screenshots | 1600×2560 ✓ (optional) |

The tablet ones are optional. Without them the listing simply is not shown as
tablet-optimised; the app still installs and runs on tablets.

---

## 2. Store listing text

**App name** (30 characters max)

```
RINGSHIFT
```

*Alternative, if you want the keyword:* `RINGSHIFT: Shift the Signal` (27)

**Short description** (80 characters max — 68 used)

```
Turn the tunnel, match the colour, fly two million light years home.
```

**Full description** (4000 characters max)

```
You are the last signal from a ship that went too far out. Two and a half
million light years from home, one thumb on the glass, and 246 conduits
between you and Earth.

Rings come at you out of the dark. Each one is a circle cut into coloured
wedges with a gap or two in it, and there are exactly two ways through: line
up a gap, or be wearing the same colour as the wedge you meet.

Drag to turn the tunnel. Tap to change your colour. That is the whole
control scheme, and the whole game is the argument between the two — take
the safe gap, or take the colour, because the colour is worth points.

— THE LONG WAY HOME —

246 levels across 25 sectors, each one a real place: the Void, the Sagittarius
Arm, the Perseus Transit, the last approach. The distance readout on the menu
is not decoration — it counts down, in light years, every time you clear a
conduit. ECHO writes the ship's log as you go, and the last entry is written
when you come out the far side of the final horizon with Earth ahead of you.

— GRADED, NOT JUST BEATEN —

Every level is graded S to D on stars, attempts and time against par. Every
sector gets an aggregate. The campaign gets one letter. Anyone can finish it
on the fortieth try; the grade is the difference between finishing and flying.

— BUILT FOR ONE THUMB —

· No account, no login, no sign-up
· Your save never leaves your phone
· Colour-blind glyphs on every wedge, on by default
· Reduced motion, flicker reduction, screen shake and haptic strength, all
  in Settings
· Six ships, each with a real trade-off — wider resonance for slower turning,
  faster spin for a tighter razor window

— HONEST ABOUT THE ADS —

Ads appear between runs and never during one. Retry is never delayed and never
gated: it is the button you press after almost every death, and it stays
instant. The two ads that pay you back — a ship unlock and a continue — are
things you choose to watch, never things you are made to.

Free. No in-app purchases.
```

---

## 3. Category and contact

| Field | Value |
|---|---|
| App or game | **Game** |
| Category | **Arcade** *(Casual is the alternative; Arcade fits the reflex loop)* |
| Tags | Arcade, Reflex, Sci-fi, Single player, Offline |
| Email | jeffrey_blezard@icloud.com |
| Website | `https://anticdrazelb.github.io/Ring/privacy.html` (or the repo) |
| Privacy policy | `https://anticdrazelb.github.io/Ring/privacy.html` |
| External marketing | This app is not marketed externally — leave unticked |

---

## 4. App content — every declaration

### Privacy policy
`https://anticdrazelb.github.io/Ring/privacy.html`

### Ads
**Yes, my app contains ads.**

### App access
**All functionality is available without special access.** No login exists.

### Content rating (IARC questionnaire)

| Question | Answer |
|---|---|
| Category | Game |
| Violence — realistic or cartoon | **No.** Nothing depicts a person or creature. A crash is an abstract shape breaking up. |
| Blood, gore, injury | No |
| Sexuality, nudity | No |
| Language, crude humour | No |
| Controlled substances, gambling | No |
| Simulated gambling / loot boxes | **No** — the ships are unlocked with stars or a rewarded ad, and there is no randomised reward |
| Horror, fear | No |
| User interaction / user-generated content | **No.** There is no chat, no leaderboard and no server. The Share button hands a GIF to the phone's own share sheet; nothing is uploaded by the app. |
| Shares user location | No |
| Digital purchases | **No** |
| Does the app contain ads | **Yes** |

Expected outcome: **PEGI 3 / ESRB Everyone / USK 0**.

### Target audience and content
- Target age groups: **13–15, 16–17, 18+**. Do **not** tick any band under 13.
  Selecting an under-13 band puts the app under the Families policy, which
  brings a certified-ads-SDK requirement the current setup does not meet.
- Does the app appeal to children? **No.**

### Data safety

The game collects nothing. Everything below exists because of the Google
Mobile Ads SDK, and Play counts data an SDK collects as data **you** collect.

> Cross-check against Google's own published Data safety guidance for the
> Mobile Ads SDK before submitting — Google maintains a mapping page for it and
> it is the definitive answer for these fields.

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** |
| Do you provide a way for users to request that their data is deleted? | **No** — there is no account and no server; uninstalling removes everything local, and advertising data is reset from Android's own Settings → Privacy → Ads |

**Data types to declare:**

| Type | Collected | Shared | Purpose | Optional? |
|---|---|---|---|---|
| Device or other IDs *(advertising ID)* | Yes | Yes | Advertising or marketing; Fraud prevention | Required |
| Location → Approximate location *(derived from IP by the ads SDK)* | Yes | Yes | Advertising or marketing | Required |
| App activity → Other actions *(ad interactions)* | Yes | Yes | Advertising or marketing; Analytics | Required |

**Do not declare:** name, email, phone, contacts, photos, files, precise
location, health, financial, messages, calendar, or app performance. The game
touches none of them and there is no crash reporting.

### Government apps / Financial features / Health
No to all three.

### Advertising ID
Declare **yes, the app uses advertising ID**, for Advertising or marketing and
Fraud prevention. The `AD_ID` permission is in the manifest.

---

## 5. What is still outstanding before you press publish

1. **Publish a GDPR message in AdMob.** Privacy & messaging → GDPR → create →
   add the privacy policy URL → **Publish**. Without it, UK and EEA devices get
   consent REQUIRED with no form to show, `canRequestAds()` stays false, and
   the app serves **no ads at all in your home market** — silently. The app
   logs it: `adb logcat -s RingshiftConsent`.
2. **Turn on GitHub Pages** so the privacy policy URL resolves: repository
   Settings → Pages → *Deploy from a branch* → default branch, `/docs`. Needs
   this work merged to that branch first.
3. **Read `PRIVACY.md`** end to end and delete the publisher note at the top.
4. **Create the upload key** and `android/keystore.properties`. Back both up
   somewhere you will still have in five years — losing the key means never
   updating this listing again.
5. **Check the target API level requirement.** `targetSdk` is **35**. Google
   raises the floor for new apps roughly every August; if the Console now
   requires **36**, that is a `compileSdk`/`targetSdk` bump and an AGP upgrade,
   not a checkbox. The Console will tell you at upload.
6. **Confirm `applicationId`.** `com.anticdrazelb.ringshift` becomes permanent
   the moment the listing goes live.

---

## 6. The upload

```
cd android
./gradlew bundleRelease
# app/build/outputs/bundle/release/app-release.aab
```

Upload the **.aab**, not an APK — Play requires a bundle for new apps and signs
the delivered APKs itself from it.
