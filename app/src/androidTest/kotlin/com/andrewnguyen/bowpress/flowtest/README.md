# Flow-spec test runner

End-to-end UI verification harness for the Android app. Reads
`flows/*.flow.json` (the same files iOS XCUITest reads) and drives a
`ComposeContentTestRule` through them, asserting every tap/wait/text/api
expectation. The flow specs are the source of truth for parity with iOS.

## How a flow becomes a test

```
flows/settings_sign_out.flow.json  ──┐
                                     ├─► FlowSpecJson.parse()
                                     │     ▼
                                     │   FlowSpec (typed model)
                                     │     ▼
                                     │   FlowRunner.run(spec)
                                     │     ▼
                                     └─► ComposeContentTestRule actions
                                         + assertions (testTag-based)
```

`flows/` lives at the repo root; gradle copies it into
`app/src/androidTest/assets/flows/` at test build time so the test APK
ships its own copy.

## Running

```sh
# All flow tests (fast — parse-only sanity checks first)
./gradlew :app:connectedDebugAndroidTest --tests "*flowtest*"

# Single flow
./gradlew :app:connectedDebugAndroidTest \
  --tests "com.andrewnguyen.bowpress.flowtest.FlowSpecParseTest.paywall_purchase_monthly_round_trips"
```

## Authoring a new flow test

1. Add `flows/<your_flow>.flow.json` (mirrors `schema.json`).
2. Make sure every `tag` referenced exists in
   `core-designsystem/.../testing/TestTags.kt` and is applied to the
   relevant `Modifier.testTag(...)`.
3. Write a test:
   ```kotlin
   @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

   @Test fun yourFlow() {
       val spec = FlowSpecJson.loadFromAssets("your_flow")
       FlowRunner(composeRule, networkProbe = mockProbe).run(spec)
   }
   ```

## Currently shipped

- `FlowSpec.kt` — typed model
- `FlowSpecJson.kt` — JSON parser + asset loader
- `FlowRunner.kt` — step executor (Compose UI Test backed)
- `NetworkProbe.kt` — interface for network-side stubbing/recording
- `FlowSpecParseTest.kt` — every flow file parses cleanly + ordering
  invariants on the canonical paywall flow

## Remaining wire-up (tracked in BLOCKERS.md)

- `MockWebServerProbe` — concrete `NetworkProbe` over OkHttp's
  MockWebServer. Needs a Hilt test module that overrides
  `NetworkConfig.baseUrl` to point at the local server URL.
- `BowPressFlowTestApplication` (Hilt test app) wired with a
  `@HiltAndroidTest` rule so the runner can boot the full nav graph.
- `ServerPatch` step support — small HTTP client targeting the
  `/__test__/*` routes on the live backend, gated by `ENVIRONMENT=test`.
- `AssertScreen` step support — requires a `NavController` probe that
  hands the current back-stack route to the runner.

Until these land, the runner already validates flow JSON well-formedness
and can drive in-process Composable tests where the SUT is a single
screen plus a fake ViewModel. The full multi-screen, network-mocked
integration test is one focused PR away.
