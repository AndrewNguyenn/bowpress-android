# Android Parity / Launch Blockers

This file enumerates every gap that prevents the Android app from
reaching full feature + verification parity with iOS, grouped by what
unblocks each one. Items needing human credentials or a backend
deployment land in the **External** section; items that can be closed
purely with code land in **Code**.

Last updated: 2026-05-19 by the Play-Console-account work.

---

## Release pipeline state (Play Store readiness)

**Repo-side: ✅ Ready to upload an AAB.**
- Upload keystore: `~/.bowpress-secrets/bowpress-upload.keystore` (mode 600)
- Release SHA-1: `BE:F3:E2:AB:C7:1C:DC:56:DD:E8:E8:53:BC:B4:3B:EC:00:C4:9C:F4`
- ✅ Upload keystore SHA-1 registered on both Firebase apps via the
  Firebase Management API (2026-05-19). Confirmed active on Release
  (`538f20ea…`) and Debug (`438afb3b…`).
- ✅ `app/google-services.json` re-downloaded with the new fingerprint;
  fresh AAB rebuilt against it.
- Signing wired in `app/build.gradle.kts` (reads from gitignored
  `local.properties`). `./gradlew :app:bundleRelease` produces
  `app/build/outputs/bundle/release/app-release.aab` (8.1M).
- R8 + resource shrinking pass cleanly; release APK smoke-tested on
  emulator (renders auth screen, no crash).
- ✅ Play Console listing copy pre-written at
  `scripts/play-listing/listing.md`. Copy-paste source for app
  name / short / full description, Data Safety answers, content
  rating expected outcomes, and the exact subscription product IDs.

**Play Console developer account: ✅ exists** (`stageandrewnguyen@gmail.com`,
2026-05-19).

**Still needed before first internal-test build can install:**

1. **Upload AAB to Internal Testing track** (manual — Play Console
   UI; the Play Developer API for upload would need a separate
   OAuth scope grant from this account that gcloud doesn't carry
   by default). First upload generates the Play App-Signing key.
   After upload, paste the App-Signing SHA-1 from **Setup → App
   integrity** back into this session and I'll register that with
   Firebase too.
2. **Create subscription products** in Play Console (Monetize →
   Products → Subscriptions) with the exact IDs
   `com.andrewnguyen.bowpress.monthly` and
   `com.andrewnguyen.bowpress.annual` — code at
   `feature-subscription/PlayBillingManager.kt:46-47` queries by
   these IDs.
3. **Grant service account** `bowpress-play-billing@bowpress-ios.iam.gserviceaccount.com`
   "View app information" + "Manage orders and subscriptions" on
   the BowPress app under **Setup → API access**. Once granted, I'll
   `wrangler secret put GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` from the
   existing key at `~/.bowpress-secrets/play-billing-key.json`.
4. **Real-Time Developer Notifications (RTDN)** topic in Play
   Console Monetize → Real-time developer notifications. Backend
   `bowpress-api` needs a Pub/Sub webhook receiver — separate scope.
5. **Listing visual assets**: icon (512×512), feature graphic
   (1024×500), 4–8 phone screenshots. Copy already pre-written.
6. **`GOOGLE_CLIENT_ID` Worker secret** push to include the Android
   client ID (External § 1 below). I can do this with `wrangler` —
   just need confirmation.
7. **12 testers × 14 days closed test** before production
   graduation (new-personal-account Play requirement).

---

## External — needs human credentials, accounts, or a backend deploy

### 1. ~~Google OAuth client ID~~ ✅ (one Worker secret push remaining)

OAuth Web client created in `bowpress-ios` Cloud project on 2026-05-10:
`516990179779-ktgfk5rv1taubsht419mlvv2clbh6qhc.apps.googleusercontent.com`.
Wired into `feature-auth/.../GoogleAuthConfig.kt`.

**Remaining:** push the Web client ID into the Worker's `GOOGLE_CLIENT_ID`
audience allowlist so `verifyIdToken` accepts Android-minted tokens:

```sh
cd bowpress-api
wrangler login              # one-time, if not already
wrangler secret put GOOGLE_CLIENT_ID --env production
# paste: <existing-iOS-client-id>,516990179779-ktgfk5rv1taubsht419mlvv2clbh6qhc.apps.googleusercontent.com
```

The Worker reads `GOOGLE_CLIENT_ID` and splits on comma
(`authController.ts:443-450`), so both iOS and Android audiences are
honored.

### 2. ~~Real `app/google-services.json`~~ ✅ (release SHA-1 registration is the last step)

Firebase added to `bowpress-ios` GCP project on 2026-05-10. Two Android
apps registered (release `1:516990179779:android:538f20eaa89edef3f44576`,
debug `1:516990179779:android:438afb3b5471233bf44576`). Debug keystore
SHA-1 `7A:0E:6D:F8:80:0C:47:6E:50:2A:71:36:14:F8:BA:A8:44:54:8A:52`
registered on both apps. `app/google-services.json` replaced with the
real config — FCM functional in debug builds.

**Remaining:** register the upload (release) keystore SHA-1 with both
Firebase apps. The upload keystore was generated on 2026-05-13 and lives
at `~/.bowpress-secrets/bowpress-upload.keystore` (mode 600). Credentials
are in `local.properties` (gitignored). Fingerprints:

```
SHA-1:   BE:F3:E2:AB:C7:1C:DC:56:DD:E8:E8:53:BC:B4:3B:EC:00:C4:9C:F4
SHA-256: F4:C8:9F:9E:CB:83:8A:CE:A7:B6:5E:F9:1C:E8:8F:08:A8:54:04:E6:1E:BA:1A:F7:B0:B0:B3:37:E9:CD:12:58
```

Register both apps via the Firebase REST API (gcloud creds required):

```sh
PROJECT=bowpress-ios
SHA1=BEF3E2ABC71CDC56DDE8E853BCB43BEC00C49CF4
# Release app:
curl -X POST "https://firebase.googleapis.com/v1beta1/projects/${PROJECT}/androidApps/538f20eaa89edef3f44576/sha:create" \
  -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  -H "Content-Type: application/json" \
  -d "{\"shaHash\":\"${SHA1}\",\"certType\":\"SHA_1\"}"
# Debug app (so Play App Signing's eventual key matches too):
curl -X POST "https://firebase.googleapis.com/v1beta1/projects/${PROJECT}/androidApps/438afb3b5471233bf44576/sha:create" \
  -H "Authorization: Bearer $(gcloud auth print-access-token)" \
  -H "Content-Type: application/json" \
  -d "{\"shaHash\":\"${SHA1}\",\"certType\":\"SHA_1\"}"
```

After Play Console enrollment, also register the Play App Signing SHA-1
(Play holds the actual signing key for production; the upload key only
authenticates uploads).

### 3. Backend `/subscription/verify-google` endpoint — blocked on Play Console

**Status:** the Cloud-side prerequisite is done — service account
`bowpress-play-billing@bowpress-ios.iam.gserviceaccount.com` exists
with a JSON key stored at `~/.bowpress-secrets/play-billing-key.json`
(mode 600). The endpoint is wired in
`bowpress-api/src/controllers/subscriptionController.ts:54` and returns
501 in production until the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret
is set.

**Hard blocker — no Play Console developer account yet.** Granting the
service account access to subscription tokens (Play Console → Setup →
API access → User permissions) requires a published developer account
and the BowPress app uploaded to it. The CF secret is intentionally
**not** set today because the service account would be inert without
Play permissions — calls to the Play Developer API would 401.

**Unblock sequence (when ready to ship Play):**
1. Sign up for a [Play Console developer account](https://play.google.com/console/signup) ($25 one-time).
2. Upload a signed AAB of BowPress; wait for the Play Console listing to
   appear.
3. Link the `bowpress-ios` Cloud project under
   Play Console → Setup → API access.
4. Grant the `bowpress-play-billing` service account
   "View app information" + "Manage orders and subscriptions" on the
   BowPress app.
5. Set the CF secret:
   ```sh
   cd bowpress-api
   wrangler secret put GOOGLE_PLAY_SERVICE_ACCOUNT_JSON --env production < ~/.bowpress-secrets/play-billing-key.json
   ```
6. ~~Update `feature-subscription/SubscriptionVerifier.kt` to call
   `/subscription/verify-google` with `{purchaseToken, productId,
   packageName}`~~ ✅ Wired on 2026-05-13. The client posts to
   `/subscription/verify-google`; until the backend goes live it returns
   501 and the client falls back to `Entitlement.Inactive` with a logged
   warning. The local Play Billing ack/acknowledge flow still completes
   so the user's purchase isn't lost — the next launch's
   `GET /subscription` resync picks it up once the server flow exists.
7. Add a webhook receiver for Google Play Real-Time Developer
   Notifications (RTDN) so server-side state stays in sync between
   client verifies (mirrors the existing Apple webhook in
   `bowpress-api/src/routes/appleWebhookRoutes.ts`). **Backend repo
   work, not in this repo.**

---

## Code — closable in this repo without external dependencies

### 3b. iOS-side `main_tab_bar` accessibilityIdentifier missing

**Status:** Android `MainScaffold.kt` tags the NavigationBar with
`Modifier.testTag(TestTags.MainTabBar)` so cross-platform Maestro
flows can assert tab-bar presence by id. iOS `MainTabView.swift`
doesn't have the corresponding `.accessibilityIdentifier("main_tab_bar")`
on its TabView, so a Maestro flow that uses `id: main_tab_bar` fails
on iOS (verified 2026-05-10 — see `flows/maestro/00_launch_to_tabs.yaml`).

**Where to fix:** add `.accessibilityIdentifier("main_tab_bar")` to the
TabView root in `bowpress-ios/Sources/BowPress/Navigation/MainTabView.swift`.
Once landed, the text-based fallback assertion in
`flows/maestro/00_launch_to_tabs.yaml` can be replaced with an id-based
one.

### 4. Tab order divergence (Android Dashboard vs iOS Log)

**Status:** Confirmed visually 2026-05-10 via Maestro screenshot diff
(loop iteration 2). Android tab 0 = "Home" → SuggestionsDashboardScreen
("All caught up / no new insights"). iOS tab 0 = "Analytics" →
AnalyticsView (overview stats, comparison, trends, suggestions inline).
The 5 tabs are not 1:1; iOS's "Log" tab (HistoricalSessionsView) has
no Android counterpart at the top level.

**Where to fix (Android-side):** in `app/MainScaffold.kt` `TopTab` enum,
match iOS order — Analytics, Log, Session, Equipment, Settings.
SuggestionsDashboardScreen content folds into the Analytics tab (iOS
puts suggestions inline in the Analytics overview). Add a Log tab
backed by `HistoricalSessionsScreen` (already exists, currently buried
inside the Analytics nav graph). Remove the standalone Dashboard tab.

### 4c. Android DEBUG entitlement defaults to inactive

**Status:** Confirmed 2026-05-10 (loop iteration 2). On a fresh
emulator install, Android's `PlayBillingManager.entitlement` emits
`Entitlement(isActive=false)` because there's no Play Billing
connection in DEBUG. The `ReadOnlyGate` then overlays the
"Subscribe to log new sessions and edit equipment" banner across the
whole app.

iOS DEBUG hardcodes `SubscriptionManager.isSubscribed=true` unless
the `REAL_ENTITLEMENT=1` env var is set (per spec.md). So iOS DEBUG
never shows the banner unless explicitly testing the lapsed path.

**Where to fix:** in `feature-subscription/PlayBillingManager.kt`,
mirror the iOS DEBUG shortcut — emit `Entitlement.Active` initial
state in DEBUG unless `REAL_ENTITLEMENT=1` system property or env
var is set. Keeps release builds unchanged.

### 4-legacy. Original tab-order divergence note (preserved for context)

**Where to fix:** `app/MainScaffold.kt:200-210` (`TopTab` enum +
ordering) and the corresponding nav graph wiring. Decide whether
Android adopts iOS order (Analytics → Log → Session → Equipment →
Settings) or iOS adopts Android order. The flow files currently use
`tapTab "Equipment"` etc. (label-based, order-agnostic), so the
verification harness will catch a misalignment in any flow that
asserts on tab content.

**Suggestion:** match iOS — iOS shipped the design first. That moves
Android's `Dashboard` into a non-bar entry surface (e.g., a header
shortcut on Analytics) or merges Suggestions into the Analytics tab.

### 5. `core-analytics` is a stub

**Status:** `core/core-analytics/` exposes `LocalAnalyticsEngine` as
an interface only. iOS's `LocalAnalyticsEngine` (Sources/BowPress/
Analytics/) computes the four deterministic insights (multi-session
drift, post-tuning effect, condition correlation, plateau detection)
client-side from SwiftData. Android currently has none of this.

**Where to fix:** implement against `core-database` repositories.
Mirror the iOS algorithms in `spec.md` § "Client-Side Analytics" —
each insight has explicit thresholds and is pure arithmetic, no LLM
needed.

### 6. `core-navigation` is a stub

**Status:** `core/core-navigation/` declares a `TopLevelDestination`
enum and an empty `BowPressNavHost`. The actual nav logic lives in
`app/MainScaffold.kt`. The module is dead weight as-is.

**Where to fix:** either delete `core-navigation` and remove from
`settings.gradle.kts`, or move nav glue (route constants, nav-graph
extension functions exposed today by each feature module) into it for
genuine reuse.

### 7. Structured unit payloads on suggestions

**Status:** `SuggestionDetailScreen` notes that `currentValue` /
`suggestedValue` come from the server as free-form strings (e.g.
`"+3/16\""`) and don't honor the client unit toggle. iOS has the
same issue — both clients re-render the server string verbatim.

**Where to fix:** server-side change in `bowpress-api`. Emit
`{ value: number, unit: string }` payloads instead of formatted
strings; clients format using `core-model/UnitFormatting.kt` (Android)
/ `UnitFormatting.swift` (iOS) so the toggle works.

### 8. Flow-runner full wire-up (the e2e harness this PR introduces)

**Status:** `app/src/androidTest/.../flowtest/` ships the flow parser,
runner, and a sanity-check parse test. The remaining wire-up is:

- **Hilt test app + module** — a `BowPressFlowTestApplication`
  (annotated `@HiltAndroidTest`) that swaps `NetworkConfig.baseUrl` to
  point at MockWebServer.
- **`MockWebServerProbe`** — concrete `NetworkProbe` over OkHttp's
  MockWebServer that consumes `apiRespond` steps and records
  `apiExpect` requests.
- **NavController probe** — exposes the current back-stack route to
  the runner for `assertScreen` step support.
- **`ServerPatch` HTTP client** — for live-mode flows; small Retrofit
  service targeting `/__test__/*` routes on `wrangler dev`.

Once these land, every `live` flow file under `flows/` becomes a
runnable e2e test. The runner's parse test already enforces the
contract on every flow.

### 9. Migrate per-feature testTag constants to centralized `TestTags`

**Status:** `core/core-designsystem/.../testing/TestTags.kt` is the
canonical registry, but only two tags
(`hydration_splash`, `main_tab_bar`) are wired through it today. The
remaining ~50 tags are still declared in three other places:
- inline literals (`testTag("change_password_current")` in
  `ChangePasswordScreen.kt:118`, etc.),
- per-feature objects (`AnalyticsDashboardTestTags.DashboardRoot` in
  `AnalyticsDashboardScreen.kt`,
  `SuggestionsDashboardTestTags.Root` in
  `SuggestionsDashboardScreen.kt`),
- per-file `private const val TAG_*` (e.g.
  `EmailAuthScreen.kt:303-307`).

**Where to fix:** mechanical migration — replace every `testTag("...")`
literal with `Modifier.testTag(TestTags.X)`, delete the redundant
per-feature objects/consts. `core-designsystem` is already a
dependency of every feature module so no new graph edges needed.
Concretely: `feature-auth`, `feature-equipment`, `feature-settings`,
`feature-session`, `feature-analytics`, `feature-subscription`. Each
PR can safely migrate one feature at a time.

**Why now:** the parse test enforces "every live-flow tag exists in
TestTags," so the registry is now load-bearing. Drift between the
registry and per-feature constants is silently tolerated until someone
renames one without the other.

### 10. Tags missing from Composables that draft flows need

Each draft flow under `flows/` references one or more tags that aren't
applied at any `Modifier.testTag(...)` site today. The list, with the
Composable file that needs the tag added:

| Tag | Composable file | Flow(s) that need it |
|-----|------|---|
| `email_auth_signup_toggle` | `feature-auth/.../EmailAuthScreen.kt:165` | `auth_email_signup_verify` |
| `save_bow_button` | `feature-equipment/.../bow/AddBowScreen.kt` | `equipment_create_bow` |
| `save_arrow_button` | `feature-equipment/.../arrow/AddArrowScreen.kt` | `equipment_create_arrow` |
| `end_session_button` | `feature-session/.../ActiveSessionScreen.kt` | `session_start_plot_end` |
| `target_plot_canvas` (rename) | `feature-session/.../TargetPlot.kt:62` (currently `target_plot`) | `session_start_plot_end` |
| `suggestion_apply_button` | `feature-analytics/.../suggestion/SuggestionDetailScreen.kt` | `analytics_apply_suggestion` |
| `settings_edit_profile` | `feature-settings/.../SettingsScreen.kt` | `settings_edit_profile` |
| `edit_profile_name_field`, `edit_profile_save_button` | `feature-settings/.../EditProfileScreen.kt` | `settings_edit_profile` |
| `sight_marks_link`, `sight_marks_add_button`, `sight_mark_distance_field`, `sight_mark_value_field`, `sight_mark_save_button` | (sight-marks UI not yet built on Android) | `sight_marks_add` |

Add the tag, graduate the flow's `status` from `draft` to `live`,
commit together. Each row above is a self-contained PR.

### 11. AGP-native asset source wiring

**Status:** `app/build.gradle.kts:46-60, 132-142` registers the
flow-asset srcDir + Copy task via `sourceSets { ... }` plus an
`afterEvaluate { tasks.findByName(...)?.dependsOn(...) }` block. This
works today (`./gradlew :app:assembleDebugAndroidTest` passes), but
the `findByName(...)?.dependsOn(...)` form silently no-ops if AGP
renames the merge tasks in a future major.

**Where to fix:** rewrite to use
`androidComponents { onVariants { variant -> variant.androidTest?.sources?.assets?.addGeneratedSourceDirectory(taskProvider, wiredWith = ...) } }`.
That gets per-variant isolation + automatic task wiring + survives
AGP upgrades. Quick win when the next gradle PR lands.

### 12. CI: schema-validate flow files (don't just JSON-parse them)

**Status:** `.github/workflows/android.yml`'s `flow-spec-parse` job
runs an inline Python script that validates JSON shape and fixture
existence — but does NOT validate against `flows/schema.json`. Per-step
type enforcement (e.g. that `apiRespond` has a `status` int) only
runs in the emulator job's `FlowSpecParseTest`. So a malformed step can
land on `main` without an instrumented run.

**Where to fix:** swap the inline script for
`pip install jsonschema && python -c "..."` that loads
`flows/schema.json` and validates every `*.flow.json` against it.
Drop the JDK setup from this job (it's pure-python — saves ~30s/PR).

### 13. CI: instrumented job re-builds APKs from scratch

**Status:** `connectedDebugAndroidTest` in the emulator job builds
debug + test APKs inside the emulator-runner script, even though
`build-and-unit-test` already built debug. Gradle cache is
`cache-read-only: true` for the downstream job → full rebuild every
PR.

**Where to fix:** assemble debug + test APKs in the emulator job
before invoking `reactivecircus/android-emulator-runner`, then
`./gradlew :app:connectedDebugAndroidTest -x assembleDebug -x assembleDebugAndroidTest`.
Or set `cache-read-only: false` on the gradle setup so the first run
primes the cache.

---

## Tracking

When a blocker is unblocked, delete its section here and reference the
landing PR / commit SHA in the commit message. This file is the
single source of truth for "what's missing"; if a gap isn't here, it
isn't tracked.
