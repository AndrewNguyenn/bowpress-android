# Android Parity / Launch Blockers

This file enumerates every gap that prevents the Android app from
reaching full feature + verification parity with iOS, grouped by what
unblocks each one. Items needing human credentials or a backend
deployment land in the **External** section; items that can be closed
purely with code land in **Code**.

Last updated: 2026-05-10 by the parity-verification work.

---

## External — needs human credentials, accounts, or a backend deploy

### 1. Google OAuth Android client ID

**Status:** placeholder iOS client ID is in
`feature/feature-auth/.../GoogleAuthConfig.kt:SERVER_CLIENT_ID`.
Continue-with-Google renders but Credential Manager fails at token-mint
because the audience is wrong.

**Unblock:**
1. In Google Cloud Console (project `516990179779`), create an Android
   OAuth 2.0 client keyed by package `com.andrewnguyen.bowpress` plus
   the SHA-1 fingerprints of debug + upload keystores.
2. Replace `SERVER_CLIENT_ID` with the new client (or the existing Web
   client ID if cross-platform audience sharing is preferred).
3. Add the resulting client ID to the Worker's `GOOGLE_AUDIENCE`
   allowlist in `bowpress-api/src/auth.ts`'s `verifyGoogleIdToken`.

### 2. Real `app/google-services.json`

**Status:** placeholder `app/google-services.json` keeps `./gradlew
build` green but FCM is non-functional. `FirebaseMessaging.getInstance().token`
will fail until the real file is in place.

**Unblock:**
1. Provision (or reuse) a Firebase project. Add two Android apps:
   - `com.andrewnguyen.bowpress` (release)
   - `com.andrewnguyen.bowpress.debug` (debug build variant suffix)
2. Upload SHA-1 fingerprints for debug + upload keystores.
3. Download the generated `google-services.json` and replace the
   placeholder.
4. Add a server key / Admin SDK service account to the Worker config so
   `/device-tokens` can round-trip via FCM HTTP v1.

### 3. Backend `/subscription/verify-google` endpoint

**Status:** `bowpress-api` has `/subscription/verify` (Apple JWS) and
`/subscription/verify-google` is wired in
`subscriptionController.ts:54` BUT requires
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` to be configured; in production
without it, the endpoint returns 501. The Android client
(`HttpSubscriptionVerifier`) currently treats Google verification as a
TODO and leaves entitlement client-only after a Play Billing purchase
acknowledgement.

**Unblock:**
1. Configure `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` in production via
   `wrangler secret put GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. The service
   account needs read access to Play Console subscription tokens.
2. Update `feature-subscription/SubscriptionVerifier.kt` to call
   `/subscription/verify-google` with `{purchaseToken, productId,
   packageName}` and propagate the returned `Entitlement`.
3. Add a webhook receiver for Google Play Real-Time Developer
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
