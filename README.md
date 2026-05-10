# bowpress-android

Native Android client for BowPress — a multi-module Gradle project following a core / feature / app layering.

## Prerequisites

- **JDK 17** (Temurin recommended). On macOS: `brew install --cask temurin@17` or install via [Android Studio](https://developer.android.com/studio) which bundles a JDK.
- **Android SDK** with:
  - Android 15 platform (API 35) — `compileSdk` / `targetSdk`
  - Android 8.0 platform (API 26) — `minSdk`
  - Build-Tools 35.0.0
  - Platform-Tools
- **Android Studio Ladybug (2024.2.x) or newer**.

Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your SDK location, e.g. `~/Library/Android/sdk`. Alternatively, create `local.properties` in the repo root:

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

## One-time wrapper bootstrap

This repo ships `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties`, **but not** `gradle/wrapper/gradle-wrapper.jar` (the JAR was not vendored during scaffolding because it could not be downloaded in the scaffolder's sandbox).

Before the first build you must generate the wrapper JAR. Easiest options:

1. **If you have Gradle installed locally** (`brew install gradle`):
   ```sh
   cd bowpress-android
   gradle wrapper --gradle-version 8.11.1 --distribution-type all
   ```
2. **If you open the project in Android Studio** — the IDE will offer to download the wrapper JAR automatically and sync Gradle.
3. **Or copy a `gradle-wrapper.jar`** from any other Gradle 8.x project into `gradle/wrapper/`.

## Building

```sh
./gradlew assembleDebug
```

Install on a connected device/emulator:

```sh
./gradlew installDebug
```

## Project layout

```
bowpress-android/
├── app/                          # :app — application module
├── build-logic/convention/       # Gradle convention plugins (android/compose/hilt/kotlin)
├── core/
│   ├── core-model/               # Pure Kotlin domain models
│   ├── core-network/             # Retrofit / OkHttp / DTOs (populated by data-layer teammate)
│   ├── core-database/            # Room (populated by data-layer teammate)
│   ├── core-data/                # Repository layer
│   ├── core-analytics/           # Analytics engine
│   ├── core-designsystem/        # Material 3 theme, colors, type
│   └── core-navigation/          # TopLevelDestination + NavHost skeleton
├── feature/
│   ├── feature-auth/
│   ├── feature-equipment/
│   ├── feature-session/
│   ├── feature-analytics/
│   ├── feature-settings/
│   └── feature-subscription/
├── gradle/
│   ├── libs.versions.toml        # Version catalog
│   └── wrapper/
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

## Convention plugins

Module build files apply plugins from `build-logic/convention`:

- `bowpress.android.application` — applied to `:app`
- `bowpress.android.library` — all core/feature Android library modules
- `bowpress.android.library.compose` — adds Compose to an Android library
- `bowpress.android.hilt` — Hilt DI wiring
- `bowpress.kotlin.library` — pure Kotlin JVM libraries

These centralize `compileSdk`, `minSdk`, `targetSdk`, Java/Kotlin 17 toolchains, Compose compiler, and Hilt setup.

## Google Sign-In setup (feature-auth)

`feature-auth` uses Credential Manager + Google Identity to mint an ID token and
POSTs it to `/auth/signin-google`. The server client ID used by the Android
picker lives in `feature/feature-auth/.../GoogleAuthConfig.kt`.

**Current placeholder:** the iOS OAuth client ID
(`516990179779-…-05k066j5guhgc0021jsbl9pvj8bb285m.apps.googleusercontent.com`).
Before Google Sign-In can work on Android you must:

1. Register a new **Android OAuth 2.0 client** in the Google Cloud Console for
   project `516990179779`, keyed by `com.andrewnguyen.bowpress` plus the SHA-1
   fingerprint of the debug and upload keystores.
2. Replace `GoogleAuthConfig.SERVER_CLIENT_ID` with the resulting ID (or with
   the existing Web client ID if we choose to share audiences across
   platforms).
3. Add that client ID to the Worker's `GOOGLE_AUDIENCE` allowlist
   (`bowpress-api/src/auth.ts`) so `verifyGoogleIdToken` accepts tokens minted
   for Android.

Until step 2 is done the Continue-with-Google button will render but the
Credential Manager request will fail at token-mint time.

## Firebase / FCM setup (feature-subscription + app push)

The app ships a **placeholder `app/google-services.json`** so `./gradlew build`
succeeds before a real Firebase project is provisioned. Push notifications will
not deliver (and `FirebaseMessaging.getInstance().token` will fail) until the
real file is in place.

To wire real FCM:

1. Create a Firebase project in the
   [Firebase Console](https://console.firebase.google.com/) (or reuse an
   existing one). Add two Android apps:
   - `com.andrewnguyen.bowpress` (release)
   - `com.andrewnguyen.bowpress.debug` (debug build variant)
2. Upload the **debug** and **release** keystore SHA-1 fingerprints to both
   apps (required for Google Sign-In + SafetyNet integrations).
3. Download the generated `google-services.json` from the Firebase Console and
   replace `app/google-services.json` with it.
4. Add a server key / Admin SDK service account to the BowPress Worker config
   so `/device-tokens` can round-trip via HTTP v1.

The `DeviceTokenRegistrar` (in `core-data`) is idempotent — both
`onNewToken` and the post-auth `PushInitializer` call `register(token)` and
the registrar dedupes on the token string.

## Google Play Billing — Subscription product IDs

`feature-subscription` expects the following product IDs to be configured in
Google Play Console (Monetization → Products → Subscriptions), mirroring the
iOS IDs:

- `com.andrewnguyen.bowpress.monthly`
- `com.andrewnguyen.bowpress.annual`

**Backend blocker:** the BowPress API currently only exposes
`POST /subscription/verify` (Apple JWS). There is no Google equivalent yet.
`feature-subscription` ships a `SubscriptionVerifier` interface with a TODO
pointing at the missing `/subscription/verify-google` endpoint; purchases
complete locally (the Play Billing flow acknowledges the purchase) but the
server-side entitlement is not persisted until the backend endpoint lands.

## App integration

The `:app` module wires everything together via three pieces:

- `AppStateViewModel` — `@HiltViewModel`, single `StateFlow<AppUiState>` with
  `isAuthenticated`, `currentUser`, `isHydrating`, and `unreadSuggestionCount`.
  Mirrors `UserRepository.currentUser` and counts undismissed/unread suggestions
  from `SuggestionRepository.observeAll()`. Hydration (`refreshProfile()` +
  `PushInitializer.start()`) fires when signed in at cold start or after
  sign-in.
- `BowPressApp` — top-level `NavHost`. Gates on `isAuthenticated`: unauth
  mounts `authNavGraph`; auth mounts `MainScaffold`. `HydrationSplashScreen`
  overlays while `isHydrating` is true.
- `MainScaffold` — `Scaffold` with `NavigationBar` (Analytics/Log/Session/
  Equipment/Settings, matching iOS tab order). Each tab is its own
  `navigation { ... }` sub-graph so per-tab back-stacks survive tab switches.
  `subscriptionNavGraph` sits alongside the tabs so the paywall is reachable
  from any tab and from the `bowpress://paywall` deep link. The analytics
  graph registers the `bowpress://suggestion/{id}?bowId={bowId}` deep link.

## Cross-platform flow verification

The `flows/` directory at the repo root is the **behavioral source of
truth** for every user-visible flow, shared with the iOS app. Each
`*.flow.json` file declares an ordered sequence of steps
(tap/wait/assert/api) that both platforms' UI tests execute
identically. See `flows/SPEC.md` for the contract.

The Android side ships:

- `core-designsystem/.../testing/TestTags.kt` — canonical testTag
  string constants. Every `Modifier.testTag(...)` referenced by a flow
  must match one of these.
- `app/src/androidTest/.../flowtest/` — flow parser + runner. Runs in
  Compose UI Test infrastructure; backed by a pluggable `NetworkProbe`
  for HTTP stubbing/recording. See `flowtest/README.md`.
- `.github/workflows/android.yml` — CI: build + unit tests + flow JSON
  validation + emulator-based instrumented tests on PRs.

Outstanding wire-up (Hilt test app, MockWebServer probe, NavController
probe, server-patch HTTP client) is tracked in `BLOCKERS.md`.

## Status

All 14 modules populated. Verification harness scaffolded; full e2e
flow execution gated on the wire-up items in `BLOCKERS.md`. See that
file for everything that's known to be missing — both code-level
(stubs, divergences) and external (OAuth, Firebase, backend endpoints).
