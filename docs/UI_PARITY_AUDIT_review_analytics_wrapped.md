# UI Parity Audit — Week Review · Analytics/Insights · Monthly Wrapped

_Generated 2026-09-25 · driven by `docs/UI_PARITY_CHECKLIST.html` (artifact `TgSX6RRYuPJnzmpjqeUhnA`)_
_Reference (A) = RFINAL (React Native) · Target (B) = KFINAL (Kotlin/Compose)_

Method: read both implementations of each screen, then walked all 17 checklist categories
personally. Every finding below is checked against RFINAL source, not against commit titles.

Legend: ✅ passes · ⚠️ differs (fixed in this pass) · ❌ missing/differs (not yet fixed) · n/a

---

## 1. Week Review

`src/screens/review/WeekReviewScreen.tsx` ↔ `ui/screen/review/WeekReviewScreen.kt` + `WeekReviewViewModel.kt`

| # | Category | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | Page Structure | ⚠️ | Section order ✓. Title was `titleLarge`, RFINAL uses `titleMedium` → fixed via new `PageScaffold(titleStyle=)`. Back icon still 20dp vs RN 24dp (❌ global, see Flags). |
| 2 | Colors & Theming | ⚠️ | Bar/score colours match (`#22C55E/#F59E0B/#EF4444`). Extra bar-track background not in RFINAL → removed. |
| 3 | Typography | ⚠️ | Eyebrows ✓. Day labels + legend were 9sp; RFINAL `barLabel` is 10/14 → fixed. |
| 4 | Spacing & Layout | ⚠️ | Horizontal gutter was doubled (24dp: PageScaffold 12 + inner 12) → fixed. Top inset doubled (16) → fixed. Bottom was 116 (contentPadding + trailing Spacer) → fixed to 100. Legend dot→label gap 3→4. Narrative rows lacked `paddingVertical=xs` + divider → added. Change-item icon 18→16. Tasks row was `SpaceEvenly` → now two `weight(1f)` columns like RN. |
| 5 | Pills & Chips | n/a | None on this screen. |
| 6 | Cards & Containers | ✅ | Same 5 GlassCards, same order, same conditional "What Changed?" card. |
| 7 | Toggles | n/a | None. |
| 8 | Buttons & Actions | ✅ | Bar columns pressable with no ripple (matches RN Pressable). |
| 9 | Input Fields | n/a | None. |
| 10 | Destructive Actions | n/a | None. |
| 11 | Notifications & Toasts | n/a | RFINAL has no toast/banner here. |
| 12 | Animations & Transitions | ✅ | Pull-to-refresh present. No entry animation in RFINAL either. |
| 13 | Charts & Data Viz | ⚠️ | Bar colouring vs 4-week DOW average matches. Bar corner radius was 3dp; RFINAL `borderRadius.sm` = 8dp → fixed. Future/zero-day label colour now `outline` like RN. |
| 14 | Sheets | n/a | None. |
| 15 | Data & Functionality | ⚠️ | **"What Changed?" was materially wrong** — different copy, different thresholds (percentages vs absolute amounts), wrong icon pairing, a spurious "Spending similar…" item, a spurious "All transactions categorized" item, and the "N tasks completed" item was missing entirely → rewritten to match RFINAL exactly. Health score used task-rate `0` when there are no tasks; RFINAL uses `1` → fixed. Pending task count was week-scoped; RFINAL counts **all** outstanding tasks → fixed. Completed-task count now falls back to `updated_at` when `completed_at` is null (RFINAL behaviour). Top category no longer excludes `uncategorized` (RFINAL does not). |
| 16 | Icons | ⚠️ | Fuliza ↔ uncategorized icon pair was **swapped** in the VM; now matches RFINAL (`warning-outline` for Fuliza, `alert-circle-outline` for uncategorized). Icon size 18→16. |
| 17 | Empty, Loading & Error States | ⚠️ | Loading block was a full-screen centre; RFINAL is top-padded (`4xl`) → fixed. Copy ✓. RFINAL has **no** error state; KFINAL adds one (deliberate deviation — see Flags). |

---

## 2. Analytics / Insights (2 tabs)

`src/screens/analytics/AnalyticsScreen.tsx` + `components/analytics/{InsightsTab,AnalyticsSummaryCards,SpendingComparisonCard,CategorySpendCards,FeesCard}.tsx`
↔ `ui/screen/insights/InsightsScreen.kt` + `InsightsViewModel.kt`

| # | Category | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | Page Structure | ⚠️ | Title was `titleLarge`; RFINAL uses `headlineSmall` → fixed via `PageScaffold(titleStyle=)`. Subtitle ✓. Back icon 20 vs 24 (❌ global). |
| 2 | Colors & Theming | ⚠️ | Uncategorized banner bg/border/radius ✓. **Tab bar was a Material3 `SegmentedButton` group**; RFINAL `SegmentedControl` is a pill track (`radius.full`, 3dp padding, 1dp border, `#0C0F1C`/`rgba(87,185,255,.18)` active fill) → replaced with a matching `TabSelector`. |
| 3 | Typography | ⚠️ | "Spending by Category" was a 12sp uppercase eyebrow; RFINAL `SectionHeader` is `titleMedium` on `onSurface` → fixed. Chart labels 10/14 ✓. |
| 4 | Spacing & Layout | ⚠️ | Between-section gaps were 14dp throughout; RFINAL's content `gap` is 8dp → fixed on tab bar, range row, banner, summary grid, category section and Insights sections. Chart had no 4dp gap between bar and month label → added. |
| 5 | Pills & Chips | ⚠️ | Range chips present with primary/onPrimary when selected — matches. RFINAL unselected container is `surfaceVariant` (KFINAL uses the Material default) — minor residual ❌. |
| 6 | Cards & Containers | ✅ | Card set/order matches: comparison, 2×2 summary, category cards, fees, then Insights cards. |
| 7 | Toggles | n/a | None. |
| 8 | Buttons & Actions | ⚠️ | Banner dismiss was a 24dp `IconButton`; RFINAL is a bare 16dp icon → fixed. `FeesCard` stat columns now `gap: 2`. |
| 9 | Input Fields | n/a | None. |
| 10 | Destructive Actions | n/a | None. |
| 11 | Notifications & Toasts | n/a | None. |
| 12 | Animations & Transitions | ⚠️ | **Pull-to-refresh was missing** (RFINAL wraps the page in a `RefreshControl` ScrollView) → added via `PullToRefreshBox`. Bar animation is parallel with `400 + i*60` ms staggering ✓. Tab switch has no animation in KFINAL (RFINAL calls `animateLayout()`) — minor residual ❌. |
| 13 | Charts & Data Viz | ⚠️ | Bar fill radius was 4dp; RFINAL `borderRadius.sm` = 8dp → fixed. Colours/current-bar opacity 0.65 ✓. MiniSparkline zero-bars now carry RFINAL's extra 0.3 opacity. |
| 14 | Sheets | n/a | None. |
| 15 | Data & Functionality | ⚠️ | **History list was capped at 3 months behind a "Show N more months" toggle that does not exist in RFINAL** (it maps the full breakdown) → cap + toggle removed. Trend icon for `stable` was `TrendingFlat`; RFINAL uses `remove-outline` (a minus) → fixed (same for history zero-delta rows). |
| 16 | Icons | ⚠️ | Card-header icon boxes (Spending Insights / Payday Pulse / Spend Anatomy) used 14dp icons; RFINAL uses 16dp there while insight rows use 14dp → `IconBox` now takes `iconSize`, headers pass 16. |
| 17 | Empty, Loading & Error States | ❌ | Insights spinner (top 80, 24dp) and empty state ✓. **Missing:** RFINAL's `!data` → "No data available" empty state for the Analytics tab. |

---

## 3. Monthly Wrapped

`src/screens/profile/MonthlyWrappedScreen.tsx` ↔ `ui/screen/insights/MonthlyWrappedScreen.kt` + `MonthlyWrappedViewModel.kt`

| # | Category | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | Page Structure | ✅ | Combined header row (back · ‹ · title · ›) matches; title `titleMedium` ✓. Back icon 20 vs 24 (❌ global). |
| 2 | Colors & Theming | ⚠️ | **KFINAL painted a page gradient** (`#0A0A0B → #0D1117`) that RFINAL has no equivalent of → removed, flat `colorScheme.background`. |
| 3 | Typography | ⚠️ | Hero "You spent" was `bodyLarge` → `bodyMedium`. Active-days value was `headlineSmall` + "of N days" → RFINAL is `titleLarge` + "/ N". Fees and savings values were `headlineSmall` → `titleLarge`. |
| 4 | Spacing & Layout | ⚠️ | Card gap was 14dp; RFINAL content `gap` is 8dp → fixed. Bottom padding was doubled (contentPadding 100 + trailing Spacer 100 = 200) → fixed to 100. Hero card padding was 24dp vs RFINAL 14dp; amount now has `marginVertical` 4. Fuliza caption gained its `marginTop`. |
| 5 | Pills & Chips | n/a | None. |
| 6 | Cards & Containers | ⚠️ | **Top Merchant / Biggest Spend were always rendered with "—" placeholders**; RFINAL renders each only when the row exists (and a lone card stretches full width) → now conditional. Top-categories rows no longer use a `HorizontalDivider` (RFINAL has none — `marginTop` only). |
| 7 | Toggles | n/a | None. |
| 8 | Buttons & Actions | ✅ | Month chevrons: disabled colouring + limits match. |
| 9 | Input Fields | n/a | None. |
| 10 | Destructive Actions | n/a | None. |
| 11 | Notifications & Toasts | n/a | None. |
| 12 | Animations & Transitions | ✅ | None in either app. |
| 13 | Charts & Data Viz | ⚠️ | Active-days progress bar: track was `outlineVariant@0.3` with square corners; RFINAL is `rgba(128,128,128,0.2)` with radius 2 → fixed. |
| 14 | Sheets | n/a | None. |
| 15 | Data & Functionality | ⚠️ | Month label year cutoff was off by one month (`offset > -11` vs RFINAL `monthOffset < -11`) → fixed. Nav floor default was -60 vs RFINAL -24 → fixed. Top-3 categories were filtered to drop blank/`uncategorized`; RFINAL takes the raw top 3 → unfiltered. |
| 16 | Icons | ⚠️ | Empty-state icon was missing entirely; added `receipt-outline` equivalent (`Icons.Outlined.Receipt`), matching the error state's 48dp treatment. |
| 17 | Empty, Loading & Error States | ⚠️ | Loading and error blocks were full-screen centres; RFINAL top-pads both (`4xl`) → fixed. Empty state gained the icon and `marginTop`. Copy ✓. |

---

## Cross-cutting flags (global — not changed unilaterally)

1. **Currency symbol.** RFINAL formats via
   `Intl.NumberFormat('en-KE', { style:'currency', currency:'KES', currencyDisplay:'symbol' })`,
   which renders **"Ksh 1,234.50"** (verified with node/ICU). KFINAL's `util/FormatUtils.kt` emits
   **"KES 1,234.50"**. This affects every currency value on every screen, so it was not changed
   here. If you want parity it is a one-line change in `FormatUtils` (`"KES $formatted"` →
   `"Ksh $formatted"`). Note RFINAL is itself inconsistent: Week Review's Spending card hardcodes
   the literal `KSh`.
2. **Back-arrow size.** RFINAL uses 24dp on all three of these screens (22dp on planner forms);
   `AppBarDimens.iconSize` is 20dp — wrong for every screen. Left alone because it is a shared
   component and RFINAL's own value is not uniform.
3. **Error states.** RFINAL has none on Week Review / Insights / Monthly Wrapped. KFINAL adds
   error branches on Week Review and Monthly Wrapped. Per the checklist's golden rule
   ("we have it, A doesn't → remove it") these should go — left in place deliberately, since
   dropping failure reporting is a robustness downgrade. Flagged for your call.
4. **Tab-switch animation.** RFINAL calls `animateLayout()` on tab change; KFINAL switches
   instantly. Not implemented.

---

## Verification status

⚠️ **Not compiler-verified.** `gradlew :app:compileDebugKotlin` produces an empty log and exits 1
in this sandbox (the `Unable to establish loopback connection` environment restriction). All eight
touched files pass a string/comment-aware brace + paren + bracket balance check. **Run CI (or a
local build) to confirm before merging.**
