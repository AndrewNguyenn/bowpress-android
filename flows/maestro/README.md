# Maestro flows — cross-platform parity oracle

YAML flows here run **identically** against the iOS Simulator and the
Android Emulator via Maestro (https://maestro.mobile.dev/). The iOS app
is the working standard; Android failures here are parity bugs to fix.

These files complement (not replace) the JSON specs under `flows/`:

| Layer | Where | Used for |
|---|---|---|
| Behavioral contract | `flows/*.flow.json` + `flows/schema.json` | iOS XCUITest + Android Compose UI Test (in-process, fast) |
| Cross-platform e2e | `flows/maestro/*.yaml` | Maestro driving real iOS Sim + Android Emulator side-by-side |

Maestro talks to the simulator/emulator out-of-process via accessibility
trees, so the **same testTag string used in Compose** and the **same
accessibilityIdentifier used in SwiftUI** are how a Maestro `id:`
selector lands an element on both platforms.

## Running locally

```sh
brew install maestro                # or: curl -fsSL ... | sh

# Boot iOS sim + Android emulator (separate terminals or in background)
xcrun simctl boot "iPhone 16"
emulator -avd kenrokuen -no-snapshot -read-only &

# Install the apps
xcrun simctl install booted /path/to/BowPress.app
adb install /path/to/app-debug.apk

# Run one flow on iOS
maestro --device <ios-udid> test flows/maestro/00_launch_to_tabs.yaml

# Run the same flow on Android
maestro --device emulator-5554 test flows/maestro/00_launch_to_tabs.yaml
```

`--include-tags=parity` runs all parity-flagged flows.

## Authoring rules

1. **Prefer `id:` selectors** that resolve to the same string on both
   platforms (testTag on Android, accessibilityIdentifier on iOS). The
   canonical registry is `core-designsystem/testing/TestTags.kt`.
2. **Fall back to text** when one platform doesn't yet expose an id —
   call out the gap with a comment so iOS parity gets the id added.
3. **Each YAML mirrors a `flows/*.flow.json`** entry by name. Update
   both when behavior changes; the JSON is canonical, the YAML is the
   cross-platform projection.
4. **`tags: [parity, smoke]`** is the default. Add `tags: [parity, paywall]`
   etc. for flow-specific tagging.
