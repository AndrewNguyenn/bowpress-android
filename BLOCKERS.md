# Android Parity / Launch Blockers

This file enumerates every gap that prevents the Android app from
reaching full feature + verification parity with iOS, grouped by what
unblocks each one. Items needing human credentials or a backend
deployment land in the **External** section; items that can be closed
purely with code land in **Code**.

Last updated: 2026-05-10 by the firebase-wireup work.

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

### 2. ~~Real `app/google-services.json`~~ ✅ (release SHA-1 remaining)

Firebase added to `bowpress-ios` GCP project on 2026-05-10. Two Android
apps registered (release `1:516990179779:android:538f20eaa89edef3f44576`,
debug `1:516990179779:android:438afb3b5471233bf44576`). Debug keystore
SHA-1 `7A:0E:6D:F8:80:0C:47:6E:50:2A:71:36:14:F8:BA:A8:44:54:8A:52`
registered on both apps. `app/google-services.json` replaced with the
real config — FCM functional in debug builds.

**Remaining:** when the upload (release) keystore exists, register its
SHA-1 with both Firebase apps via the curl snippet documented in
`README.md` "Firebase / FCM setup" section.

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
6. Update `feature-subscription/SubscriptionVerifier.kt` to call
   `/subscription/verify-google` with `{purchaseToken, productId,
   packageName}` and propagate the returned `Entitlement`.
7. Add a webhook receiver for Google Play Real-Time Developer
   Notifications (RTDN) so server-side state stays in sync between
   client verifies (mirrors the existing Apple webhook in
   `bowpress-api/src/routes/appleWebhookRoutes.ts`).

---

## Code — closable in this repo without external dependencies

### 4. Tab order divergence (Android Dashboard vs iOS Log)

**Status:** Android tab 0 is `Dashboard` (label "Home", content =
`SuggestionsDashboardScreen`); iOS tab 0 is `Analytics` and tab 1 is
`Log` (`HistoricalSessionsView`). The 5 tabs are not 1:1.

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
