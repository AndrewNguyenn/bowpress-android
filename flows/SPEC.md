# BowPress Flow Specification

This directory holds the **behavioral source of truth** for every user-facing
flow in BowPress. Both the iOS UI tests (XCUITest) and the Android UI tests
(Compose UI Test + Hilt) read these files and assert the same observations.
A flow that passes on iOS and fails on Android is a parity bug; a flow that
fails on both is an app bug.

## Why this exists

The iOS app shipped first and is the de-facto product spec. Android needs
to behave identically across the user-visible surface. Rather than freeze
parity by hand-translating XCUITest source into Compose UI tests (which
inevitably drifts), we encode flows declaratively here and have both
platforms run the same script.

## File layout

```
flows/
├── SPEC.md                                    ← this file
├── schema.json                                ← JSON Schema for flow files
├── fixtures/                                  ← API response fixtures
├── auth_email_signin.flow.json                live
├── auth_email_signup_verify.flow.json         draft
├── equipment_create_bow.flow.json             draft
├── equipment_create_arrow.flow.json           draft
├── session_start_plot_end.flow.json           draft
├── analytics_view_dashboard.flow.json         live
├── analytics_apply_suggestion.flow.json       draft
├── paywall_purchase_monthly.flow.json         live
├── paywall_lapsed_recovers.flow.json          live
├── paywall_purchase_monthly_mock.flow.json    live
├── settings_sign_out.flow.json                live
├── settings_edit_profile.flow.json            draft
├── settings_change_password.flow.json         live
├── settings_delete_account.flow.json          live
└── sight_marks_add.flow.json                  draft
```

Each `.flow.json` declares the steps for one user-visible flow.

`live` flows have all their referenced testTags wired into Compose
production code today; the parse test cross-validates them against
`TestTags.kt`. `draft` flows are spec-only — they capture iOS behavior
we haven't yet wired into Android Composables and are exempt from tag
validation. Graduate a draft to live by adding the missing
`Modifier.testTag(TestTags.X)` to the relevant Composable.

**Future flows** (not yet authored): auth_google_signin, auth_password_reset,
equipment_edit_bow_config, equipment_delete_bow, session_resume_after_relaunch,
sight_marks_suggest. Add as draft, graduate when wired.

## Identity contract: testTags / accessibility identifiers

Both platforms expose interactive elements with **identical string IDs**.
The iOS app uses `.accessibilityIdentifier("upgrade_banner")`; the Android
Compose tree uses `Modifier.testTag("upgrade_banner")`. Same string, both
platforms. Constants live in:

- iOS: `Sources/BowPress/Support/AccessibilityIdentifiers.swift`
- Android: `core/core-designsystem/src/main/kotlin/.../testing/TestTags.kt`

Adding a new flow always implies adding/keeping the matching tag in both
platforms.

## Step types

| Step | Description |
|------|-------------|
| `launch` | Cold-launch the app with given launch args & env vars (e.g. auto-signin email, force entitlement) |
| `tap` | Tap an element by its testTag/accessibilityID |
| `tapTab` | Tap a bottom-nav tab by label |
| `type` | Type text into a field (testTag-addressed) |
| `swipe` | Swipe in a direction on an element (used for target plot) |
| `wait` | Wait up to N seconds for an element to appear |
| `waitGone` | Wait up to N seconds for an element to disappear |
| `assertText` | Assert text content of an element |
| `assertScreen` | Assert the current screen route (Compose) / nav title (UIKit) |
| `assertExists` | Assert an element is in the tree |
| `assertAbsent` | Assert an element is NOT in the tree |
| `apiExpect` | Assert the next outgoing HTTP call matches method + path (+ optional body shape) |
| `apiRespond` | Stub the response for an upcoming HTTP call |
| `serverPatch` | (e2e against real backend only) PATCH a test-only endpoint to force state |

## Run modes

A flow can run in two modes:

- **`mock`** — outgoing HTTP is intercepted by a fixture server (MockWebServer
  on Android, OHHTTPStubs-equivalent on iOS). Deterministic, fast, runs in CI
  without network. Default for parity tests.
- **`live`** — runs against a real `wrangler dev` backend with seeded D1
  fixtures. Used for the paywall flows where StoreKit/Play Billing must
  round-trip a real `/subscription/verify`. Requires `e2e-free@bowpress.dev`
  seeded.

The flow file declares which mode(s) it supports under `runModes`.

## Schema

See [`schema.json`](./schema.json). Highlights:

```jsonc
{
  "name": "paywall_purchase_monthly",
  "description": "Unentitled user taps upgrade banner, buys monthly, banner clears",
  "runModes": ["live"],            // or ["mock"], or both
  "fixtures": {                    // mock-mode only; ignored in live runs
    "GET /me": "fixtures/user_e2e_free.json",
    "GET /subscription": "fixtures/entitlement_inactive.json"
  },
  "launch": {
    "autoSignInEmail": "e2e-free@bowpress.dev",
    "autoSignInPassword": "bowpress-e2e-pw-1234",
    "env": { "REAL_ENTITLEMENT": "1", "USE_LOCAL_API": "1" }
  },
  "steps": [
    { "type": "waitGone", "tag": "hydration_splash", "timeout": 20 },
    { "type": "tapTab", "label": "Equipment" },
    { "type": "wait", "tag": "upgrade_banner", "timeout": 10 },
    { "type": "tap", "tag": "upgrade_banner" },
    { "type": "wait", "tag": "paywall_monthly_button", "timeout": 15 },
    { "type": "tap", "tag": "paywall_monthly_button" },
    { "type": "waitGone", "tag": "upgrade_banner", "timeout": 15 }
  ]
}
```

## Authoring rules

1. **One flow == one user goal.** "Sign in with email" is a flow.
   "Sign in then buy monthly" is two flows; compose at the runner level.
2. **No platform-specific branches.** If iOS and Android genuinely diverge,
   that's a parity bug to fix, not a fork in the spec.
3. **Use stable tags, not text.** Text changes; tags are contract.
   `assertText` is allowed for content the user reads (e.g. "You're a Pro"),
   not for matching control labels.
4. **Mock mode flows must be self-contained.** All required fixtures listed
   in `fixtures`; no hidden dependencies on prior flows.
5. **Live mode flows must declare seed expectations** in their description
   (e.g. "requires e2e-free@bowpress.dev seeded with no entitlement").

## Adding a new flow

1. Add `<name>.flow.json` here, conforming to `schema.json`.
2. If new tags are introduced, add them to both:
   - `bowpress-ios/Sources/BowPress/Support/AccessibilityIdentifiers.swift`
   - `bowpress-android/core/core-designsystem/src/main/kotlin/.../testing/TestTags.kt`
   - Apply at the relevant Composable / SwiftUI view.
3. Add a one-line entry in this README's file layout list above.
4. Both platforms' test runners auto-pick up new flow files via filename
   glob; no test file additions needed for the common case.

A flow that does not pass on either platform should be considered the
canonical spec for what the feature should do. Fix the implementation, not
the flow.
