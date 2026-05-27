# BowPress Android — Play Console listing copy

Drop these straight into **Play Console → Main store listing**. Character
limits enforced in-console; copy is pre-trimmed to fit.

## App details

**App name** (≤ 30 chars):
```
BowPress
```

**Short description** (≤ 80 chars):
```
Log every tuning change and every arrow. Know what's actually working.
```

**Full description** (≤ 4000 chars):
```
Tune smarter. Shoot better.

BowPress is for competitive and recreational archers who want data behind every adjustment they make to their bow — not just feel.

THE PROBLEM
Bow tuning involves dozens of interdependent variables: draw length, peep height, limb turns, cable twist, rest position, nocking height, and more. Most archers track changes through memory alone. When something stops working, it's nearly impossible to know what changed, when, or why.

BowPress replaces guesswork with a logged history of every configuration state and every arrow you shoot under it.

WHAT IT DOES

Configuration tracking
Every time you change something on your bow or arrow setup, BowPress snapshots the full configuration. Nothing is overwritten — the complete history is preserved so you can always see exactly what your bow looked like when a group of arrows was shot.

Session logging
When you shoot, you log arrows against the active configuration. A simple drag-to-place target UI captures where each arrow landed. You can write notes during a session — observations about hold feel, back tension, release — that travel alongside the objective data.

Mid-session changes
Changing something mid-session doesn't break the flow. BowPress handles the bookkeeping quietly in the background, keeping your arrows correctly attributed to the configuration they were shot under.

Analytics
After enough sessions, BowPress surfaces patterns: which configurations produced your tightest groups, how specific changes have affected your point of impact over time, and what your shooting data suggests you try next. The more you log, the more specific the insights.

Sight marks
A per-bow sight-marks list and quadratic-fit suggester. Log measured marks across distance and BowPress interpolates suggested marks for distances you haven't sighted in yet.

BOW PARAMETERS TRACKED
Draw length, let-off %, peep height, D-loop length, top/bottom cable twists, main string twists, top and bottom limb turns, rest position (vertical, horizontal, depth), sight distance, grip angle, nocking height.

ARROW PARAMETERS TRACKED
Length, point weight, fletching type, length and offset, nock type, total arrow weight.

Sign in with Google. Optional subscription unlocks advanced analytics and unlimited session history.

SUBSCRIPTION DETAILS
BowPress Pro — Monthly ($4.99/month) and BowPress Pro — Annual ($49.99/year). Each includes a 1-month free trial for new subscribers. Subscriptions auto-renew unless canceled at least 24 hours before the end of the current period; manage from your Google Play account.

Terms of Use: https://andrewnguyenn.github.io/bowpress-web/terms.html
Privacy Policy: https://andrewnguyenn.github.io/bowpress-web/privacy.html
```

## URLs

- **Privacy policy URL:** `https://andrewnguyenn.github.io/bowpress-web/privacy.html`
- **Website (optional):** `https://anduwu.dev/`
- **Support email:** `bowpresssupport@gmail.com` (public-facing — distinct from the dev-account owner email)

## Category

- **App category:** Sports
- **Tags:** Sports, Health & Fitness — sub-category Recreation if Play offers it

## Content rating questionnaire (expected answers)

- Violence: None
- Sexuality: None
- Profanity: None
- Controlled substances: None
- User-generated content: None (notes are private to the archer)
- Result: should rate Everyone / PEGI 3 / IARC 3+

## Target audience and content

- **Target age group:** 13+
- Does the app appeal to children? **No.**

## Data Safety form (matches Privacy Policy claims)

| Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|
| Email address | Yes | No | Account management | Required |
| User name (display) | Yes | No | Account management | Required |
| App activity (sessions, plots, configs) | Yes | No | App functionality, analytics | Required |
| Device/installation IDs (FCM token) | Yes | No | Push notifications | Optional |
| Crash logs | Yes | No | Crash/diagnostics | Optional |

- All data encrypted in transit (HTTPS to Cloudflare Worker backend)
- Users can request deletion via in-app account screen
- No data sold; no data shared with third parties

## Subscription products to create in Play Console

**Monetize → Products → Subscriptions → Create:**

| Product ID | Name | Base plan | Price |
|---|---|---|---|
| `com.andrewnguyen.bowpress.monthly` | BowPress Pro — Monthly | Auto-renewing, monthly, 1-month free trial | $4.99 USD |
| `com.andrewnguyen.bowpress.annual` | BowPress Pro — Annual | Auto-renewing, yearly, 1-month free trial | $49.99 USD |

Product IDs MUST match `feature-subscription/PlayBillingManager.kt:46-47` exactly or the paywall will show an empty product list.

## Assets needed

- **App icon:** 512×512 PNG, no transparency
- **Feature graphic:** 1024×500 PNG/JPEG
- **Phone screenshots:** at least 2, ideally 4–8, 1080×1920 (or any 16:9 / 9:16 between 320–3840px)
- **Tablet screenshots (optional):** 7" and 10" if you target tablets

Phone screenshots to capture (recommended set):
1. Equipment list — shows bow + arrow inventory
2. BowDetail — sections + steppers + Specific Limbs picker
3. Session active — target plot with pen magnifier in action
4. Analytics dashboard — Score timeline + Impact map
5. Sight Marks list — calibration card + measured marks
6. Session detail — per-arrow chips + heatmap
