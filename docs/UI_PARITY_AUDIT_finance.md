# UI Parity Audit — Finance (all pages + floating windows)

_Generated 2026-09-25 · driven by `docs/UI_PARITY_CHECKLIST.html` (17 categories per unit)_
_Reference (A) = RFINAL (React Native) · Target (B) = KFINAL (Kotlin/Compose)_

Method: every unit was read on both sides by a dedicated audit pass, then the high-impact claims
were re-verified by hand against the source. **Two agent claims were rejected on verification**
(see "Rejected findings") — agent output was treated as a lead, not as truth.

Scope: FinanceScreen, TransactionListItem, TransactionDetailScreen, TransactionFormScreen,
CategorizeScreen, FeeAnalyticsScreen, MerchantDetailScreen, ReviewQueueScreen, ImportSmsSheet,
ImportCsvSheet, FulizaLimitModal, CounterpartyCard, FulizaSimulatorCard, ExportScreen, CsvImportScreen.

---

## Fixed in this pass

| # | File | Fix |
|---|------|-----|
| F1 | `ui/theme/Color.kt` | **Global theme drift.** The file's own header claims "values from src/theme/paperTheme.ts exactly", but nearly every M3 role had drifted: `surface` `#08090E`→`#0C0E16`, `surfaceVariant` `#161618`→`#141825`, `onSurfaceVariant` `#A1A1AA`→`#9499B0`, `outline` `#2E2E33`→`#2A2E42`, `outlineVariant` `#222226`→`#1E2235`, `secondary` purple `#8B5CF6`→grey `#9BA3B8`, `primaryContainer`, `tertiaryContainer`, `errorContainer`, `onError`, `inverseSurface`, `inversePrimary`, all `secondary/tertiary` roles, plus `ElevationColors` levels 1-5. Dark **and** light schemes re-aligned to `paperTheme.ts`. This one file colours every surface, divider, secondary label, sheet background and segmented control in the app. |
| F2 | `util/FormatUtils.kt` | **Currency symbol.** A formats via `Intl.NumberFormat('en-KE',{style:'currency',currency:'KES',currencyDisplay:'symbol'})`, whose CLDR symbol is `Ksh` — verified with node/ICU, and independently found by four separate audit passes. B emitted a literal `KES `. Changed `formatCurrency` + `compactCurrency`. |
| F3 | `data/db/dao/TransactionDao.kt` | **Search ignored `description`.** A: `(merchant LIKE ? OR description LIKE ? OR mpesa_code LIKE ?)`. B omitted `description` in both `getFilteredPaged` and `getFiltered`. |
| F4 | `ui/screen/finance/TransactionListItem.kt` | **List rows had no card container.** A wraps each row in a bordered card (`surfaceVariant` fill, 1dp `outlineVariant` border, radius 12, 12dp side margins, 8dp bottom gap) — B rendered a bare full-width row. Restored the chrome, corrected the ripple to `rgba(255,255,255,0.08)`, and matched A's CSS `text-transform: capitalize` (per-word, e.g. "personal care" → "Personal Care"). |
| F5 | `ui/components/AppDropdown.kt` **(new)** | **Shared picker port.** A has no anchored Material dropdown anywhere — `Dropdown.tsx` is a boxed trigger (1dp `outlineVariant` border, 20dp radius, label line, selected option's icon in its own colour, chevron) that opens a `SwipeableSheet` (0.55 scrim, 32dp top radius, 40×4 grabber, `titleLarge` + Close header, scrollable list, primary check on the selected row, hairline separators). Material3's `ExposedDropdownMenuBox` matches none of that. Added `AppDropdownField` + `AppPickerSheet` to reproduce it. |
| F6 | `FinanceScreen.kt`, `TransactionFormScreen.kt`, `CategorizeScreen.kt`, `ExportScreen.kt` | Swapped onto F5: the Finance **Period** selector, the TransactionForm **Category** + **Status** pickers, the Categorize category chooser, and the Export **Date window**. Categorize's old sheet was the worst case — a fixed-70%-height sheet with a **non-scrolling** `Column` of plain `Text` rows that clipped past ~16 categories; the dead `CategoryPickerSheet` composable was deleted. Picker sheets are hoisted out of the `LazyColumn` so scrolling can't dispose them while open. Also dropped the two `Haptics.light()` calls on category/status select (A fires no haptic there), and Export now clears the custom range when the window moves off "custom", as A does. |
| F7 | `TransactionDetailScreen.kt` | **Two user-reported layout bugs in the inline edit panel.** (a) The card was capped at `heightIn(max = 520.dp)` with an internal `verticalScroll`, so tapping **Edit** crammed the panel into a fixed-height scroll box instead of the card growing the way RFINAL's does. (b) The Cancel/Save row used a weighted 1:3 split, squeezing "Cancel" below its intrinsic width so the label broke and rendered **vertically**. Fixed: the card is now content-sized up to **90% of the real available height** (`BoxWithConstraints` + `heightIn`), so it grows naturally and only scrolls as a last resort; Cancel sizes to its content (`maxLines = 1`, `softWrap = false`) with Save taking the remainder, at the reference's 16dp roundness and grey `onSurfaceVariant` label instead of primary. |
| F8 | `ui/components/AppSegmentedControl.kt` **(new)** + 4 screens | **Custom pills app-wide.** RFINAL never uses Material's segmented buttons — `components/common/SegmentedControl.tsx` is one bordered pill track (radius full, 3dp inset, 1dp border) where the active segment gets a primary-tinted fill *and* border with a bold primary label. Ported it as `AppSegmentedControl` and replaced every Material `SingleChoiceSegmentedButtonRow` in the app: **TransactionDetailScreen** (type), **TransactionFormScreen** (type), **CalendarScreen** (Tasks/Events tabs), **InsightsScreen** (Analytics/Insights tabs — its private duplicate `TabSelector` and its colour constants were deleted). No Material segmented buttons remain. Note: `ui/components/SegmentedControl.kt` already existed but is a port of the *other* React control (`components/settings/SegmentedControl.tsx`, used by Settings) — both React controls now have ports. The two type selectors also gained back the no-haptic behaviour RFINAL has. |

| F9 | `ui/components/AppChip.kt` **(new)** + 3 screens | **Chip selectors are no longer Material.** RFINAL builds every chip from Paper's `Chip` with per-call-site colours; three variants recur — **Solid** (selected = accent fill + accent border + onPrimary label), **Soft** (selected = accent @18% fill + @40% border + bold accent label, used by the task/event type chips) and **Tinted** (accent @9% fill + @31% border, used by the Learning "Continue/Start" chip). Ported as `AppChip` with those three styles plus accent/selectedContent/unselectedContent overrides. Converted the finance-scoped sites: **InsightsScreen** (date range — unselected label is `onSurface`, not muted), **ExportScreen** (format, now with RFINAL's grid/document/article icons) and **CsvImportScreen** (column mapping, using Paper's `secondaryContainer` selected palette). Unused `FilterChip` imports removed. |
| F10 | `FinanceScreen.kt` | **Golden-rule removals** (B had these, RFINAL does not): list **shimmer skeleton**, **append/load-more spinner**, **pull-to-refresh**, the floating **TopBanner** error/import toast, and the **in-place `TransactionDetailDialog`** — tapping a row now navigates to `Route.TRANSACTION_DETAIL` as RFINAL does. Also found and fixed a **double-drawn card**: FinanceScreen already wrapped each row in the same bordered container that `TransactionListItem` now draws itself (F4), so the wrapper was removed and the component owns the chrome (as it does in RFINAL). |

### Chip call sites still to convert

`AppChip` now exists, so the rest are mechanical swaps in their own unit passes:
`LearningScreen` ×2 (category filter, session chip — the session chip is the **Tinted** style), `TaskFormScreen` (priority — **Soft** style), `SearchScreen` (filters), and `ImportSmsScreen`'s detection count chip (currently an `AssistChip`).

### Remaining picker call site

- `TransactionDetailScreen.kt:~293-338` — Category + Status still `ExposedDropdownMenuBox`. **Deliberately not swapped yet:** this panel (`InlineEditPanel`) renders *inside* a `Dialog`, and nesting a `ModalBottomSheet` (itself dialog-hosted) inside another dialog is a fragile pattern I can't runtime-test here. The correct fix is the one already in the backlog — A pushes a real `TransactionDetail` route instead of showing an in-place dialog — after which these pickers can be swapped safely. Doing the swap first would risk a broken overlay with no way to verify it.

---

## Verified — still open (ranked by impact)

### Data correctness (highest impact)

1. **Charges / Fee Analytics use two different formulas.** A sums `amount` over fee *categories* (`UPPER(category) IN ('AIRTIME','FULIZA','WITHDRAWAL','SUBSCRIPTION','FEE')`); B sums the `fee` *column* across all categories. Both the Finance "Charges" insight card and the whole FeeAnalyticsScreen show unrelated numbers. Also: tx cap `LIMIT 20` (A) vs `50` (B); A's range is `date >= startOfMonth` open-ended, B is bounded.
2. **Budget spend includes transfers + Fuliza in B.** A: `transaction_type = 'expense' AND status='completed'`. B: `IN ('expense','transfer','fuliza')`. Inflates the Budget insight card *and* the budget-alert percentage on Finance.
3. **Period filter end-boundary.** A uses `endOfDay`/`endOfWeek`/`endOfMonth` per period; B caps all three at end-of-today — so A includes later rows in the week/month and B excludes them.
4. **Review-queue membership + persistence.** A's predicate shows `imported_review_approved` entries (B hides them); A caps the audit log at 200, B at 500; A persists approve as `outcome='imported_review_approved'` + `sync_state='pending'`, B writes `'dismissed'` + `status='completed'`. Different rows, different writes.
5. **MerchantDetail aggregates use different populations.** A: total = all transactions, `avgAmount = total / all count`, `avgPerDay = total / activeDays`. B: outflow-only for all three. Numbers visibly differ.
6. **Categorize list order + missing side effects.** A sorts by latest date desc and, after a bulk assign, also persists `merchant_categories`, checks budget thresholds and bumps the data version; B sorts by count desc and drops all three side effects.
7. **Transaction type mapping.** B's `getMonthTotals` reads `'receive'` where A reads `'income'` — this is the **expected** schema divergence (your memory documents KFINAL = `receive`), so *not* a gap; but KFINAL is internally inconsistent (`getMonthlyTotalsRange` accepts both). Worth normalising for safety.
8. **Search debounce scope.** A debounces only the search field (300 ms); B debounces *every* filter change, adding latency to period switches.

### Structure / interaction

9. **Transaction detail is a floating dialog in B, a pushed screen in A.** B's `TransactionDetailDialog` (opened from the Finance list) has no A counterpart; A navigates to a `transparentModal`+`fade` route. This also breaks the back-stack semantics and means the detail window has no A-equivalent entry point.
10. **Period selector + Category/Status pickers are anchored M3 dropdowns in B, bottom sheets in A.** A's `Dropdown` is a boxed field (20dp radius, in-field category icon, `chevron-down`) opening a `SwipeableSheet` (title + Close, 0.55 scrim, 40×4 grabber, spring slide-up, drag-to-dismiss at 80px/0.3 velocity, 32dp top radius). B uses `ExposedDropdownMenuBox`. 5 of the 17 categories are affected by this one component choice.
11. **Segmented controls.** A uses a custom pill track (`radius.full`, 3dp inset, 1dp border, primary-tinted active segment); B uses stock M3 `SegmentedButton` (previously purple `secondaryContainer` — F1 improves this but the track/border/tint structure is still missing).
12. **Categorize category picker.** Content-sized RN Modal (max 70%, 55% scrim, scrolling list of Paper text buttons) vs fixed-70% M3 `ModalBottomSheet` with a **non-scrolling** `Column` of 16 plain `Text` rows (~640dp, clipped on short screens). A also exposes 20 categories vs B's 16 — A-only: `fuel, loans, insurance, fuliza, transfer, withdrawal`; B-only: `rent, investment`.
13. **`Card.Content` 16dp never ported.** A's `GlassCard` + `Card.Content` = 12 + 16 = **28dp** inner padding; B's `GlassCard` = **12dp**. Affects Categorize and MerchantDetail cards.
14. **TransactionForm success feedback is the wrong widget.** B uses a bottom `SnackbarHost` (M3 `Short` = 4000 ms) and `showSnackbar` **suspends**, blocking back-navigation ~4 s; A shows a top tone-coloured pill at 3000 ms and goes back after 400 ms. All 8 other KFINAL form screens already use the correct `TopBanner(Success) + delay(400) + popBackStack()` pattern — this screen is the outlier.
15. **TransactionForm edit-load flash.** A holds the form at `opacity: 0` until the record loads; B calls `rememberFormFadeIn()` with the default `ready = true`, so blank/default fields are visible then pop in.
16. **SMS import sheet.** B adds an "Allow SMS Access" permission gate A doesn't have (A handles permission on the Finance screen); B merges M-Pesa into the bank list where A splits it; B dismisses immediately for `mpesa_only` where A keeps the sheet open while the import runs.
17. **ImportCsv "Choose File"** carries a `FileDownload` icon in B that A does not have.
18. **FulizaLimitModal** — closest to parity of all units. A `parseInt` → Int, B `toDoubleOrNull` → Double; A has no explicit focus request (relies on `autoFocus`), B uses a `FocusRequester`; A dismisses the keyboard on cancel, B doesn't. Both use a `KSh ` affix/prefix (note: A itself writes `KSh` here, inconsistent with its own `formatCurrency`).

### States / extras

19. **B adds surfaces A doesn't have** on FinanceScreen: list shimmer skeleton, append spinner, pull-to-refresh, and a top error toast. A has only the refresh button.
20. **FinanceScreen SMS banner placement + tone.** A renders it inline in the scroll flow *after search*, with `primary`/`surfaceVariant` backgrounds and 3000/5000/6000 ms auto-dismiss per outcome. B renders a fixed top bar before the header plus a success-toned pill, flat 3000 ms; B's error pill uses `autoDismissMs = 0` **and** `onDismiss = null`, so it can stick permanently.
21. **FinanceScreen day totals dropped.** A computes a signed per-day total (completed outflows/inflows, whole numbers) in the date header; B passes `total = null` so it never renders.
22. **FinanceScreen "Transactions" header.** Flush to the screen edge (component adds no horizontal gutter), and the count never renders (`SectionHeader` only draws its action when `onAction != null`, which B passes as null). Also uppercased 12sp vs A's `titleMedium` 16sp.
23. **Review-queue outcome chips.** Review colour `#F5CB5C` vs A's `#FBBF24`; unknown outcomes print the raw string in A but are forced to `"Pending"` in B; A's Paper Chip is a bordered pill vs B's borderless `Surface(12dp)`.
24. **Category alias table is missing app-wide.** A normalises via `CATEGORY_ALIASES` (`food & restaurants→food`, `sent→transfer`, `received→income`, `deposit→savings`, `buy_goods→shopping`, `paybill→utilities`, `other→miscellaneous`); B has no equivalent anywhere, so aliased categories get the wrong icon/colour.
25. **CounterpartyCard is a different component on each side** — A: an editable counterparty-name override (hash + DB override + save/clear); B: a read-only counterparty stats card. **Both are dead code** (no call sites found on either side), so this is a naming collision rather than a live gap.
26. **`TransactionListItem.kt`'s header comment is wrong** — it says "arrow up/down/swap based on type" but the code is category-based (matching A).
27. **Dark/light mode**: A was screenshot-audited dark-first; the light `outline`/`outlineVariant` values B had deliberately changed (contrast rationale) were reverted to A by F1 — revert if you prefer B's contrast.

---

## Rejected findings (agent claims that failed verification)

- **"FinanceScreen header is 24sp, B is 20sp."** The agent assumed Paper's *default* `headlineSmall` (24/400). RFINAL overrides it in `paperTheme.ts` to **20/26/700**, which matches KFINAL's `Type.kt`. Not a gap.
- **"Income uses `income` in A vs `receive` in B — B is wrong."** This is the documented, intended schema divergence between the two apps. B is correct; only B's internal inconsistency is worth tidying.

---

## ExportScreen + CsvImportScreen (audited separately, by hand)

**Good news first:** the three `EX-*` items in `docs/PARITY_GAPS.md` are **already fixed** in KFINAL and that doc is stale — `EX-1` custom start/end fields exist (`ExportScreen.kt:170-193`), `EX-2` item counts render inside each preview tile (`:310-316`), `EX-3` a Clear-history action exists (`:392-394`).

### ExportScreen

| # | Category | Finding |
|---|----------|---------|
| 5/8 | Chips/Buttons | A's format selector is 3 `Chip`s with icons — selected = `primary` fill + `onPrimary` label + `borderRadius.full`, `flex:1`. B uses bare M3 `FilterChip` with **no icons** and no full-radius. |
| 9 | Input Fields | **A opens a native date picker** from "From"/"To" fields (`TouchableRipple`, `MMM d, yyyy`, arrow between, `maximumDate` today). B uses free-text `OutlinedTextField`s with a `YYYY-MM-DD` placeholder — no picker, no validation. This violates checklist 9.3 outright. |
| 11/15 | Validation | A blocks export with alerts for empty/<6-char passphrase and for zero selected domains. **B has no client-side validation at all.** |
| 3/17 | Preview header | A shows a loading spinner plus "Total items: {n}" and a hint line ("Tap a card to include or exclude that data type." / "CSV exports transactions only."). B renders only the `EXPORT PREVIEW` label. |
| 2/6 | Preview tiles | A: radius `lg` (20), bg `${color}30` active / `${color}15` inactive, 1px border always, 32dp **circle** icon box, count text in `onSurface`. B: `shapes.large` (24), bg α0.32 active / `surfaceVariant` inactive, 2dp border when active, 30dp radius-12 icon box, count tinted with the domain colour. |
| 6/15 | History list | A renders **every** export; B caps at `take(10)`. |
| 16 | History icons | A varies the icon per format (`grid-outline`/`document-text-outline`/`document-outline`) and colours JSON `#34D399`; B uses one `Description` icon for all and JSON `#22C55E`. |
| 4 | History dot | A's green status dot is a separate trailing element; B overlays it on the icon box (`Alignment.TopEnd`). |
| 14 | Date-window picker | A uses an outlined trigger + `SwipeableSheet` option list; B uses `ExposedDropdownMenuBox`. Same picker/sheet gap as the finance screens — fixed by the shared picker. |

**Deliberate deviation (not a gap):** A "exports" PDF by sharing plain text named `.txt`; B builds a real A4 `PdfDocument`. B is ahead — leave it.

### CsvImportScreen

| # | Category | Finding |
|---|----------|---------|
| 3 | Formatting | A's preview date is `formatDate(row.date)` → "04 Feb 2026"; B prints `row.date.take(10)` → "2026-02-04". |
| 6/2 | Invalid rows | A renders `Card mode="elevated"` with `errorContainer` bg **and** an `error` border; B is a borderless `Box` with `errorContainer` bg only. |
| 4 | Spacing | A gives each mapping field `marginBottom: 14` and its label `marginBottom: 8`; B uses 4dp vertical padding + a 4dp spacer. A's "Preview" heading has both top and bottom margin; B only top. |
| 8 | Import button | When there are no valid rows A colours the button `onSurfaceVariant`; B colours it `primary`. |
| 11 | Completion feedback | A shows a blocking `Alert.alert('Import complete', 'N transactions imported.')` then navigates; B shows an `InlineBanner` success and auto-pops. Different pattern (and A also alerts on "No valid rows", where B merely disables the button). |

Both screens otherwise match on structure, copy, and the bulk of their layout.

---

## Verification status

⚠️ **Not compiler-verified.** `gradlew :app:compileDebugKotlin` exits 1 with an empty log in this sandbox (the documented loopback restriction). All edited files pass a string/comment-aware brace + paren + bracket balance check. Run CI to confirm before merging.

---

## Reconciliation — fixes made after F10 (this table was missing; the doc previously read as finished)

| # | File | Fix |
|---|------|-----|
| F11 | `TransactionViewModel.kt` | Period filter end boundary: `today`/`week`/`month` now close at end-of-day / **Sunday** / **last day** instead of all three capping at end-of-today. |
| F12 | `TransactionDao.kt`, `FeeAnalyticsViewModel.kt` | **Charges / FeeAnalytics aggregation.** React sums `amount` by fee *category* (`AIRTIME/FULIZA/WITHDRAWAL/SUBSCRIPTION/FEE`, `deleted_at IS NULL`, no status filter, tx list `LIMIT 20`) — verified at `FeeAnalyticsScreen.tsx:96-113`. `getFeeTotal` + `getFeeTransactions` repointed; added `getChargesByCategory`. `getFeeByCategory` deliberately **left on the fee column** because a second consumer (Insights' fee summary) has an unverified React counterpart — marked `Do not repoint`. |
| F13 | `TransactionDao.kt`, `BudgetViewModel.kt`, `BudgetAlertService.kt` | **Budget spend is expense-only.** Added `getExpenseCategoryTotals` (`transaction_type = 'expense' AND status = 'completed'`); transfers + Fuliza no longer inflate a budget's used %. Shared `getCategoryTotals` left alone (9 consumers; transfer/fuliza correct elsewhere). |
| F14 | `MerchantDetailViewModel.kt` | Headline total + average now cover **all** transactions; only active-days/peak stay outflow-only — matching React's `avgPerDay` quirk. Also dropped a phantom `""` day key. |
| F15 | `CategorizeViewModel.kt`, `TransactionDao.kt` | Merchant groups sorted by **most-recent activity** (was volume); `updateCategoryForMerchant` guarded with the uncategorized predicate so it can't overwrite real categories; the missing **budget-threshold check** now runs after a bulk assign. |
| F16 | `ReviewQueueViewModel.kt` | Audit-log cap **500 → 200**. |
| F17 | `TransactionDetailScreen.kt` + 4 screens | **All five picker call sites now use `AppDropdownField` + `AppPickerSheet`** (Finance Period, TransactionForm Category/Status, Categorize, Export Date window, TransactionDetail Category/Status). TransactionDetail was the last; it landed **without** first converting that screen off `Dialog`, so it is the one change needing a device check (nested bottom-sheet-in-dialog), with a one-line revert. |
| F18 | `FinanceScreen.kt`, `ExportScreen.kt` | Transactions header rebuilt as React's row (`titleMedium` label + always-rendered `bodyMedium` count, 12dp gutter, 8dp vertical margins) — `SectionHeader` uppercased it and never drew the count because `onAction` was null. Export history cap `take(10)` removed (React renders all). |

## FulizaSimulatorCard — audit gap closed

**Dead code on both sides** (no call sites in either app; only its own definition). Audited for completeness, and it is **not** a 1:1 port: KFINAL wraps it in **`GlassCard`** where RFINAL uses a plain bordered card (`surfaceVariant` fill, `outlineVariant` border, radius 12); KFINAL uses Material3 **`Slider`** where RFINAL uses a custom thin PanResponder slider (6dp track, 20dp round thumb, `step 50`); plus small gaps in the 2dp header text gap, the days-chip padding, the progress bar's `minWidth: 6`, the fee-row divider height and the fee-row `overflow: hidden`. **Unverified:** `formatKes` output format and `estimatedPayoffDate` format, both of which come from RFINAL's `utils/fulizaProjection.ts` (not read). No user impact while the component stays unreachable — recommend deleting it from both, or leaving as-is.

## Still open in Finance

- **TransactionDetail** still renders as a `Dialog`, not a real screen (F17's caveat).
- **ReviewQueue**: membership predicate deferred (KFINAL writes `imported_batch` / `parse_failed:*`, which are in neither React's 4-value set nor KFINAL's substring test — a product decision); approve still records `'dismissed'` instead of `'imported_review_approved'` (needs a `DbWriter` change in the `sms` module).
- **Categorize**: React's learned merchant→category table is replaced by KFINAL's ML classifier — an architecture difference, not ported.
- **FinanceScreen**: per-day signed totals still not rendered (needs a ViewModel-level per-day query, not a Paging-3 `peek` window sum); SMS banner placement/tone/durations.
- **Export**: free-text date fields instead of a native date picker; no client-side validation (passphrase length / zero domains); preview-tile styling; history icons + status-dot placement.
- **CsvImport**: preview date renders ISO instead of `04 Feb 2026`; invalid rows lose the error border; import-button colour when empty; completion feedback uses a banner where React uses an alert.
- **`CounterpartyCard`**: a different component on each side (dead code on both) — flagged, not resolved.
