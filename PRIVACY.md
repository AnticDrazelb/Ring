<!-- ---------------------------------------------------------------------
     NOTE TO WHOEVER PUBLISHES THIS — not part of the policy.

     1. This is a draft written from what the code actually does. It is not
        legal advice. Read every line before it goes out under your name.
     2. Section 5's consent paragraph is true only once the GDPR message is
        PUBLISHED in the AdMob console (Privacy & messaging > GDPR). The UMP
        SDK is in the app and will show whatever is configured there — and
        nothing at all if nothing is. See android/README.md.
     3. The published copy is docs/privacy.html. Keep the two in step.
     --------------------------------------------------------------------- -->

# RINGSHIFT — Privacy Policy

**Last updated: 8 August 2026**

RINGSHIFT is a game published by **AnticDrazelb** ("we", "us"). This policy
describes what the app collects, what it does not, and what you can do about
it.

Contact for any privacy question or request: **jeffrey_blezard@icloud.com**

---

## The short version

- **The game collects nothing.** Your progress, stars, credits, settings and
  grades are stored on your device and are never sent anywhere. There is no
  account, no login, no analytics, and no crash reporting.
- **The ads do collect data.** RINGSHIFT shows ads through Google AdMob, and
  the Google Mobile Ads SDK collects an advertising identifier and technical
  device information in order to serve and measure them.
- **We do not run a server.** There is nothing for us to store, because
  nothing reaches us.

---

## 1. Data the app stores on your device

RINGSHIFT keeps a single save record in its own private app storage
(`localStorage` inside the app's WebView). It contains:

- which levels you have cleared, your stars, best times and per-level grades
- credits, the ship you have selected and any ships you have unlocked
- your settings — sound, music, haptics, reduced motion, control preferences
- your daily-challenge state and total play time and death count
- a record of when ads have been shown, so the app can enforce its own
  frequency limits

This data never leaves your device. It is not transmitted to us or to anyone
else. **Uninstalling the app deletes all of it**, and there is no copy
anywhere for us to return or erase on request, because we never had one.

**Android Backup.** If you have Android's backup to Google Drive enabled, the
operating system may include this save file in your device backup so your
progress survives a new phone. That backup is between you and Google under
[Google's privacy policy](https://policies.google.com/privacy); we cannot read
it. You can turn it off in **Settings → Google → Backup**.

---

## 2. Data collected by advertising

RINGSHIFT displays ads supplied by **Google AdMob**. Google is an independent
data controller for what it collects; we do not receive, see, or store any of
it beyond aggregate earnings figures in our AdMob dashboard, which contain no
information about any individual.

The Google Mobile Ads SDK may collect:

| | |
|---|---|
| **Advertising ID (AAID)** | A resettable identifier Android provides for advertising. |
| **IP address** | Which implies an approximate location, typically at city level. |
| **Device information** | Model, manufacturer, operating system version, language, screen size, and similar technical attributes. |
| **Ad interaction data** | Which ads were requested, shown, dismissed, or clicked. |

Google uses this to select and deliver ads, to measure their performance, to
cap how often you see the same ad, and to detect fraud and invalid traffic.

- Google's privacy policy: <https://policies.google.com/privacy>
- How Google uses data from apps that use its services:
  <https://policies.google.com/technologies/partner-sites>

Ads in RINGSHIFT appear only between runs — after a level, on a death card, or
when you deliberately choose to watch one to unlock a ship or continue a run.
Ads never interrupt play, and retrying a level is never gated behind one.

---

## 3. Data we do **not** collect

- No name, email address, phone number, or any other contact detail
- No account or login of any kind
- No contacts, photos, files, calendar, camera, or microphone access
- No precise (GPS) location
- No analytics, telemetry, behavioural profiling, or crash reporting
- No purchases; the app contains no in-app payments

---

## 4. Permissions, and why each one exists

| Permission | Why |
|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Used **only** by the Google Mobile Ads SDK. The game itself is loaded from inside the app package and is configured to block all network loads, so no part of the game ever reaches the network. |
| `VIBRATE` | Haptic feedback. Vibration happens on your device and produces no data. You can turn it off in the game's Settings. |
| `com.google.android.gms.permission.AD_ID` | Lets the ads SDK read the advertising ID. If you remove it (see below), ads still work — they just become non-personalised. |

---

## 5. Your choices and rights

**Reset or delete your advertising ID.** On Android:
**Settings → Privacy → Ads**, then *Reset advertising ID* or *Delete
advertising ID*. Deleting it stops apps, including this one, from receiving a
personalised advertising identifier.

**Delete everything the app stores.** Uninstall RINGSHIFT, or use
**Settings → Apps → RINGSHIFT → Storage → Clear data**. Either removes your
entire save permanently.

**Consent in the UK, EEA and Switzerland.** Where required, a consent message
is shown before any ad is requested, and no ad is requested until you have
answered it. You can change your answer at any time from
**Settings → Privacy choices** inside the game. This is provided through
Google's User Messaging Platform.

**GDPR / UK GDPR.** Where these apply, the legal bases are: your **consent**
for personalised advertising, and our **legitimate interest** in showing
non-personalised advertising to fund a free game. You have the right to
access, correct, erase, restrict, and object to processing, and to data
portability. Because we hold no personal data ourselves, requests about
advertising data should be directed to Google using the links in section 2;
we will help where we can — write to the address at the top of this policy.

**California (CCPA/CPRA).** We do not sell or share personal information for
money. Sharing an advertising identifier for personalised advertising may be
treated as "sharing" under the CPRA; deleting your advertising ID as described
above is the opt-out.

---

## 6. Children

RINGSHIFT is not directed to children under 13, and we do not knowingly
collect any personal information from children. If you believe a child has
provided personal information through this app, contact us and we will act on
it.

---

## 7. Data retention

We retain nothing, because we receive nothing. Data held by Google is retained
according to Google's own policies, linked in section 2. Data on your device
is retained until you delete it.

---

## 8. Changes to this policy

If this policy changes, the "Last updated" date above changes with it and the
new version replaces this one at the same address. Material changes affecting
how advertising works will also be reflected in the app.

---

## 9. Contact

**jeffrey_blezard@icloud.com**
