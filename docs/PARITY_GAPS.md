# KFINAL ↔ RFINAL — Full Parity Gap Master List
_Generated 2026-08-20 via 15-agent code sweep_  
_Severity: 🔴 MISSING (absent from Kotlin) · 🟡 WRONG (present but differs) · ⚪ MINOR (cosmetic/spacing)_

---

## CROSS-CUTTING (every screen affected)

| # | Severity | Issue |
|---|----------|-------|
| CC-1 | 🟡 | **Icons**: RN uses Ionicons outline style throughout. Kotlin uses Material Icons filled — heavier visual weight everywhere |
| CC-2 | 🔴 | **Delete confirmations**: RN shows `Alert.alert()` before every destructive action. Kotlin deletes immediately — no confirmation dialog on any screen |
| CC-3 | 🔴 | **TopBanner after save**: RN shows a success banner after every form save. Kotlin navigates back immediately with no banner |
| CC-4 | 🔴 | **Fade-in animations**: RN wraps form content in `Animated.View` with `useFormFadeIn()` — opacity+slide on load. Kotlin has no entry animation |
| CC-5 | 🟡 | **Currency display**: RN `formatCurrency({decimals:0})` → whole numbers. Kotlin `compactCurrency()` abbreviates (e.g. "4.5K") — different representation for large amounts |
| CC-6 | 🟡 | **Color tokens**: RN uses `theme.colors.onSurface` / `onSurfaceVariant`. Kotlin uses `onBackground` / `onBackground.copy(alpha=...)` — not the same semantic token |
| CC-7 | 🟡 | **PageScaffold background**: RN uses flat `theme.colors.background`. Kotlin uses a custom gradient — visible on every screen |
| CC-8 | 🟡 | **HeroSurface**: Kotlin completely redesigned with different API and hardcoded colors vs RN's theme-adaptive gradient component |
| CC-9 | 🟡 | **ShimmerLoadingState**: RN renders simple placeholder rectangles. Kotlin renders complex icon+text skeleton — different structure |

---

## ONBOARDING & AUTH

### OnboardingScreen
| # | Severity | Issue |
|---|----------|-------|
| ON-1 | 🔴 | **App logo in WelcomeStep**: RN shows 80×80 bordered box with `icon.png` (48×48). Kotlin starts directly with the welcome text — no logo |
| ON-2 | 🔴 | **Step-transition animation**: RN animates card fade+slide (opacity 0→1, translateY 8→0) on every step change. Kotlin swaps content instantly |
| ON-3 | 🟡 | **"Allowed" icon in PermissionStep/BackgroundReceiverStep**: RN shows `Ionicons checkmark-circle` (green). Kotlin shows `Icons.Filled.Person` — completely wrong icon |
| ON-4 | 🟡 | **Selected goal checkmark**: RN shows `Ionicons checkmark-circle` trailing. Kotlin shows `Icons.Filled.Person` — wrong icon |
| ON-5 | 🟡 | **PillarCards per permission step**: RN shows 2 distinct cards per step (step 4: timer/reminder; step 5: M-Pesa import/on-device). Kotlin shows 1 generic "Private & secure" card for both — 3 of 4 cards missing |
| ON-6 | 🟡 | **Name field leading icon**: RN TextInput has `left={<TextInput.Icon icon="person-outline" />}`. Kotlin OutlinedTextField has no leading icon |
| ON-7 | 🟡 | **"Balanced" goal icon**: RN maps `balanced` → `options-outline` (sliders). Kotlin maps to `Icons.Filled.AutoAwesome` (sparkles) |
| ON-8 | ⚪ | **Progress dots gap**: RN `gap: spacing.sm`. Kotlin `padding(horizontal = 3.dp)` — slightly tighter |

### AuthScreen
| # | Severity | Issue |
|---|----------|-------|
| AU-1 | 🔴 | **Loading/splash state**: RN shows logo (88×88) + spinner while `isLoading`. Kotlin always shows the form immediately |
| AU-2 | 🔴 | **Leading icon on text fields**: Both RN fields have `left={<TextInput.Icon icon="person-outline" />}`. Kotlin has no leading icon |
| AU-3 | 🟡 | **Logo badge**: RN is 40×40 rounded-rect with border, containing real `icon.png`. Kotlin is 40×40 circular no-border with `Icons.Filled.Person` — wrong shape, no border, placeholder icon |

### AppLockScreen
| # | Severity | Issue |
|---|----------|-------|
| AL-1 | 🔴 | **"Forgot PIN?" dialog**: RN `Alert.alert()` with "Turn off screen lock" action that actually clears the PIN. Kotlin falls back to `Toast("Contact support...")` — doesn't clear the PIN |
| AL-2 | 🟡 | **PIN length hardcoded to 6**: RN reads `settings.pinCode.length` dynamically (default 4). Kotlin hardcodes 6 dots and 6-digit limit everywhere |
| AL-3 | 🟡 | **Keypad bottom row**: RN `[null, '0', 'backspace']` — empty spacer, 0, backspace. Kotlin `["bio", "0", "del"]` — adds biometric key that doesn't exist in RN source |
| AL-4 | 🟡 | **Subtitle & "Forgot PIN?" color**: RN `onSurfaceVariant`. Kotlin `onBackground.copy(alpha=0.55f)` |
| AL-5 | 🟡 | **Headline text style**: RN `variant="headlineSmall"` (typography system). Kotlin raw `fontSize=22.sp, fontWeight=Bold` — bypasses typography scale |
| AL-6 | 🟡 | **Fingerprint icon**: RN `finger-print-outline` (outline). Kotlin `Icons.Filled.Fingerprint` (filled) |

---

## HOME

### HomeScreen
| # | Severity | Issue |
|---|----------|-------|
| HO-1 | 🔴 | **AgendaCard section entirely absent**: RN shows today's events/tasks as an agenda list below the header. Kotlin has no AgendaCard |
| HO-2 | 🔴 | **QuickActions section entirely absent**: RN shows quick-action buttons (Add Transaction, Add Task, etc.). Kotlin has none |
| HO-3 | 🔴 | **Username in greeting**: RN shows "Good morning, {name}". Kotlin shows generic greeting — no username |
| HO-4 | 🔴 | **Profile button navigation**: RN navigates to PersonalInformationScreen. Kotlin is a no-op |

---

## CALENDAR

### CalendarScreen
| # | Severity | Issue |
|---|----------|-------|
| CA-1 | 🔴 | **Day-of-week grid offset formula wrong**: Month grid always displays days in wrong columns — calendar is visually broken |
| CA-2 | 🔴 | **Task dots on day cells missing**: RN shows colored dots under days that have tasks. Kotlin has no dots |
| CA-3 | 🔴 | **Swipe month navigation missing**: RN supports horizontal swipe to change month. Kotlin only supports chevron taps |
| CA-4 | 🔴 | **Today button missing**: RN shows a "Today" button to jump back to current date. Kotlin has none |
| CA-5 | 🟡 | **Task completion style**: RN completed tasks show strikethrough + 0.5 opacity. Kotlin has no strikethrough/opacity change |
| CA-6 | 🟡 | **Tasks appear in Events tab**: Kotlin shows tasks under the Events tab — they should only appear in the Tasks tab |

---

## FINANCE

### FinanceScreen
| # | Severity | Issue |
|---|----------|-------|
| FI-1 | 🔴 | **SMS permission banner missing**: RN shows a banner when SMS permission is not granted. Kotlin has no such banner |
| FI-2 | 🔴 | **Date-group Card containers missing**: RN wraps each date group of transactions in a `Card`. Kotlin renders them without card containers — flat list |
| FI-3 | 🔴 | **Post-import result banner missing**: After SMS import, RN shows "Imported N transactions". Kotlin has no post-import banner |
| FI-4 | 🟡 | **Budget banner fallback logic wrong**: RN's fallback budget calculation differs from Kotlin's implementation |

### TransactionListItem (inside FinanceScreen)
| # | Severity | Issue |
|---|----------|-------|
| TL-1 | 🟡 | **Category icon hardcoded**: Kotlin hardcodes `Icons.Filled.Share` for all transactions — wrong icon |
| TL-2 | 🟡 | **Expense amounts not in error color**: RN shows expense amounts in `error` color (red). Kotlin shows them in default color |
| TL-3 | 🟡 | **Transfers shown as negative**: RN shows transfers as a positive outgoing amount. Kotlin prepends a minus sign |

### TransactionDetailScreen
| # | Severity | Issue |
|---|----------|-------|
| TD-1 | 🟡 | **Hero card icon hardcoded**: Kotlin hardcodes `Icons.Filled.Share` as the transaction type icon — copy-paste error |

### ReviewQueueScreen
| # | Severity | Issue |
|---|----------|-------|
| RQ-1 | 🟡 | **OutcomeChip "batch_pending" label**: Chip shows "Batch" but RN shows "Pending" for `batch_pending` outcome |
| RQ-2 | 🟡 | **Amount typography too large**: RN uses `bodyMedium` for amount. Kotlin uses `titleMedium` — one size too large |

### ExportScreen
| # | Severity | Issue |
|---|----------|-------|
| EX-1 | 🔴 | **Custom date range picker missing**: RN shows start/end date fields when "Custom" is selected. Kotlin has no custom range UI |
| EX-2 | 🔴 | **Item counts in export cards missing**: RN shows "N transactions" / "N events" etc. inside each export type card. Kotlin cards have no counts |
| EX-3 | 🔴 | **History clear button missing**: RN has a "Clear history" button in the export history section. Kotlin has none |

### CategorizeScreen
| # | Severity | Issue |
|---|----------|-------|
| CZ-1 | 🔴 | **TopBanner auto-dismiss**: RN auto-dismisses after 1500 ms. Kotlin `TopBanner` has no `autoDismissMs` passed — banner stays until manually dismissed |
| CZ-2 | 🔴 | **Chevron-down icon on "Pick a category…" button**: RN has `Ionicons chevron-down` trailing. Kotlin has no icon |
| CZ-3 | 🟡 | **Category picker modal vs bottom sheet**: RN uses `Modal` with slide animation, 70% height, rounded top, FlashList of text buttons. Kotlin uses `ModalBottomSheet` — different presentation |
| CZ-4 | 🟡 | **Empty state icon**: RN `checkmark-done-circle-outline`. Kotlin `Icons.Filled.CheckCircle` (filled) |
| CZ-5 | 🟡 | **Loading state layout**: RN top-padded center (`paddingVertical: 3xl`). Kotlin full-screen center (`Box fillMaxSize`) |

### FeeAnalyticsScreen
| # | Severity | Issue |
|---|----------|-------|
| FA-1 | 🟡 | **Empty state icon**: RN `checkmark-circle-outline`. Kotlin `Icons.Filled.CheckCircle` (filled) |
| FA-2 | ⚪ | **Divider margin between category bars**: RN `marginVertical: 2`. Kotlin `Divider()` with no margin — slightly tighter |

### MerchantDetailScreen
| # | Severity | Issue |
|---|----------|-------|
| MD-1 | 🔴 | **"Avg per day" rounding missing**: RN `formatCurrency(avgPerDay, {decimals:0})` → whole number. Kotlin passes no decimals option — shows "KES 123.67" instead of "KES 124" |
| MD-2 | 🟡 | **Time format**: RN 12-hour with AM/PM (`"02:30 PM"`). Kotlin 24-hour (`"14:30"`) |
| MD-3 | 🟡 | **Date zero-padding**: RN zero-pads day (`"Jan 05, 2024"`). Kotlin no padding (`"Jan 5, 2024"`) |
| MD-4 | 🟡 | **Category text capitalization**: RN forces rest to lowercase after first char. Kotlin only uppercases first char — stored-uppercase categories stay all-caps |
| MD-5 | 🟡 | **Empty state icon**: RN `receipt-outline`. Kotlin `Icons.Filled.Receipt` (filled) |

---

## BILLS, LOANS & BUDGETS

### BillsScreen
| # | Severity | Issue |
|---|----------|-------|
| BI-1 | 🟡 | **Paid/unpaid icon bug**: Both paid and unpaid states use `Icons.Filled.CheckCircle` — copy-paste error; unpaid should use a different icon |
| BI-2 | 🟡 | **Cycle label "one_time"**: Renders as `"One_time"` (underscore). RN maps it to `"One-time"` (hyphen) via label map |

### LoanFormScreen
| # | Severity | Issue |
|---|----------|-------|
| LF-1 | 🔴 | **"Total repaid" field missing**: RN has a read-only total-repaid field. Kotlin form lacks this field entirely |
| LF-2 | 🔴 | **"Last repayment date" field missing**: RN has this field. Kotlin form lacks it entirely |

---

## EVENTS & TASKS

### EventFormScreen
| # | Severity | Issue |
|---|----------|-------|
| EF-1 | 🔴 | **Reminder enable toggle missing**: RN has a toggle to enable/disable reminders. Kotlin has none |
| EF-2 | 🔴 | **Delete confirmation missing**: RN shows `Alert.alert` before deleting an event. Kotlin deletes immediately |
| EF-3 | 🟡 | **Modal pattern**: RN uses bottom sheets for date/time/recurrence pickers. Kotlin uses AlertDialogs — different presentation |

---

## RECURRING

### RecurringScreen
| # | Severity | Issue |
|---|----------|-------|
| RC-1 | 🔴 | **Delete confirmation dialog**: RN shows `Alert.alert('Delete rule', 'Remove {title}?')`. Kotlin deletes immediately |
| RC-2 | 🔴 | **animateLayout on delete**: RN animates item removal. Kotlin has no animation |
| RC-3 | 🟡 | **Delete icon**: RN `trash-outline`. Kotlin `Icons.Filled.Delete` (filled) |
| RC-4 | 🟡 | **Add button icon**: RN `add` (outline). Kotlin `Icons.Filled.Add` |
| RC-5 | 🟡 | **Empty state icon**: RN `repeat-outline`. Kotlin `Icons.Filled.Repeat` (filled) |
| RC-6 | 🟡 | **Empty state subtitle**: RN `bodySmall`. Kotlin `bodyMedium` — one size too large |
| RC-7 | 🟡 | **Delete button alignment**: RN left-aligned. Kotlin `Arrangement.End` — right-aligned |
| RC-8 | 🟡 | **Switch component**: RN custom `LifeOSSwitch`. Kotlin standard Material3 `Switch` |

### RecurringFormScreen
| # | Severity | Issue |
|---|----------|-------|
| RF-1 | 🔴 | **Form fade-in animation**: RN fades in content once data loads. Kotlin has no fade |
| RF-2 | 🔴 | **TopBanner success on save**: RN shows "Rule updated"/"Rule added" banner before navigating. Kotlin navigates immediately |
| RF-3 | 🔴 | **Delete confirmation dialog**: RN shows `Alert.alert('Delete rule', 'Are you sure?')`. Kotlin deletes immediately |
| RF-4 | 🔴 | **Haptic feedback on save**: RN calls `haptic('light')`. Kotlin has none |
| RF-5 | 🔴 | **Validation alerts**: RN shows `Alert.alert('Missing title')` and `Alert.alert('Missing date')`. Kotlin only renders inline error text |
| RF-6 | 🔴 | **Cadence label mapping**: RN has `CADENCE_LABELS` mapping `mon_fri → "Mon–Fri"`. Kotlin uses `.replaceFirstChar` only → `"Mon_fri"` (underscore, wrong) |
| RF-7 | 🟡 | **Type selector widget**: RN `Dropdown`. Kotlin `FilterChip` row — different control |
| RF-8 | 🟡 | **Status toggle button**: RN changes button background (green when active, surfaceVariant when paused). Kotlin `OutlinedButton` — no color change |
| RF-9 | 🟡 | **Delete icon**: RN `trash-outline`. Kotlin `Icons.Filled.Delete` |
| RF-10 | 🟡 | **Date field widget**: RN custom `DateField`. Kotlin read-only `OutlinedTextField` + clickable `Box` → `DatePickerDialog` |
| RF-11 | 🟡 | **Saving indicator**: RN shows `"Saving…"` text. Kotlin shows `CircularProgressIndicator` inside button |

---

## ANALYTICS

### AnalyticsScreen (bar chart)
| # | Severity | Issue |
|---|----------|-------|
| AN-1 | 🟡 | **Bar chart animation**: RN animates all bars simultaneously. Kotlin animates them sequentially — different feel |
| AN-2 | 🟡 | **Currency in charts**: RN uses full `formatCurrency`. Kotlin uses `compactCurrency` (abbreviates) |

### WeekReviewScreen
| # | Severity | Issue |
|---|----------|-------|
| WR-1 | 🟡 | **Greeting typography**: RN `titleMedium`. Kotlin `headlineSmall` — one size too large |
| WR-2 | 🟡 | **Section heading typography**: Kotlin section headings are one size too large vs RN |

---

## SEARCH

### SearchScreen
| # | Severity | Issue |
|---|----------|-------|
| SE-1 | 🔴 | **Bills result section missing**: RN shows matching bills. Kotlin has no bills section |
| SE-2 | 🔴 | **Goals result section missing**: RN shows matching goals. Kotlin has no goals section |
| SE-3 | 🔴 | **Income result section missing**: RN shows matching income entries. Kotlin has no income section |
| SE-4 | 🔴 | **Loans result section missing**: RN shows matching loans. Kotlin has no loans section |
| SE-5 | 🔴 | **Recent searches missing**: RN persists and shows recent search queries. Kotlin has none |
| SE-6 | 🔴 | **Query highlighting missing**: RN highlights matched text in results. Kotlin shows plain text |

---

## ASSISTANT

### AssistantScreen
| # | Severity | Issue |
|---|----------|-------|
| AS-1 | 🔴 | **Action buttons in message bubbles**: RN renders interactive action chips inside assistant replies. Kotlin renders plain text only |
| AS-2 | 🔴 | **Scroll on content resize**: RN scrolls on `onContentSizeChange`. Kotlin only scrolls on new message count — misses incremental reply growth |
| AS-3 | 🟡 | **Suggested prompts visibility**: RN shows prompts when `messages.length <= 1`. Kotlin shows only when `messages.isEmpty()` — prompts disappear too early |
| AS-4 | 🟡 | **TypingIndicator**: RN shows 3 dots with staggered opacity + "Thinking…" text. Kotlin shows 3 animated bounce dots, no "Thinking…" label |
| AS-5 | 🟡 | **Input area**: RN custom `ChatInput` component. Kotlin inline `OutlinedTextField` + circular `IconButton` — styling differs |

---

## LEARNING

### LearningScreen
| # | Severity | Issue |
|---|----------|-------|
| LE-1 | 🔴 | **Real DB integration missing**: RN reads from SQLite (`learning_sessions` table). Kotlin uses 11 hardcoded static entries |
| LE-2 | 🔴 | **Log Session Modal missing**: RN has full modal with Topic/Duration/Notes fields + validation. Kotlin FAB is a no-op |
| LE-3 | 🔴 | **`markCompleted` on tap missing**: RN tapping a card calls `UPDATE learning_sessions SET is_completed = 1`. Kotlin cards are not tappable |
| LE-4 | 🔴 | **Empty state missing**: RN shows icon + "No sessions here" when filter returns nothing. Kotlin shows blank |
| LE-5 | 🔴 | **In-progress session UI missing**: RN shows `ProgressBar` and "Continue"/"Start" chip on non-completed cards. Kotlin has no progress bar or chip |
| LE-6 | 🟡 | **Category filter UI**: RN horizontally-scrolling `Chip` row. Kotlin `ExposedDropdownMenuBox` — different interaction model |
| LE-7 | 🟡 | **Session card base**: RN `Card mode="elevated"`. Kotlin `GlassCard` — different background/elevation |
| LE-8 | 🟡 | **Completed checkmark always shown**: Kotlin renders `Icons.Filled.CheckCircle` on every card. Should only show when `is_completed = 1` |
| LE-9 | 🟡 | **Progress bar color**: RN dynamic (green ≥0.8, yellow ≥0.4, red otherwise). Kotlin always green |
| LE-10 | 🟡 | **Monthly hours**: RN calculates from DB (`SUM(duration_minutes)/60`). Kotlin hardcodes `2.5f` |

---

## PROFILE & PERSONAL INFO

### PersonalInformationScreen
| # | Severity | Issue |
|---|----------|-------|
| PI-1 | 🔴 | **`autoFocus` on edit field**: RN keyboard opens automatically. Kotlin requires manual tap |
| PI-2 | 🟡 | **TopBanner visibility timing**: RN banner shows over full screen unconditionally. Kotlin wraps it inside `if (editing != null)` — banner may never appear after sheet closes |
| PI-3 | 🟡 | **Title text alignment**: RN `textAlign: center`. Kotlin left-aligned |
| PI-4 | 🟡 | **Row divider**: RN bottom-only `borderBottomWidth: 1`. Kotlin full `Modifier.border(...)` — adds unintended left/right/top borders |

---

## SETTINGS

### SettingsScreen
| # | Severity | Issue |
|---|----------|-------|
| ST-1 | 🟡 | **All row icons wrong family**: Every `SettingsRow` uses Ionicons outline (RN) vs Material filled (Kotlin) — 14 rows affected (shield, notifications, sparkles, card, radio, medkit, list, gift, info, trash, refresh, download, etc.) |
| ST-2 | 🔴 | **Permission banner loading state**: RN shows "Requesting…" text and hides chevron while requesting. Kotlin has no requesting state |
| ST-3 | 🔴 | **Permission denied feedback**: RN shows "Permissions denied — grant them in device Settings". Kotlin is silent on denial |
| ST-4 | 🔴 | **Permission focus re-check**: RN `useFocusEffect` re-checks SMS permission every time screen comes into focus. Kotlin only checks once at composition |
| ST-5 | 🟡 | **Permission banner icon**: RN `alert-circle-outline`. Kotlin `Icons.Filled.Warning` (solid triangle) |
| ST-6 | 🔴 | **Battery optimization flow on background receiver toggle**: RN checks `isIgnoringBatteryOptimizations()` and prompts user. Kotlin has no battery optimization check |
| ST-7 | 🔴 | **Background receiver error state**: RN shows "Could not update background receiver" on failure. Kotlin has no error handling |
| ST-8 | 🟡 | **About Version row tap**: RN shows `Alert.alert('About', 'LifeOS v1.0.0')`. Kotlin is a no-op |
| ST-9 | 🔴 | **App Updates: loading states**: RN buttons have `loading` + `disabled` during async check/download. Kotlin buttons have no loading state |
| ST-10 | 🔴 | **App Updates: real update check**: RN calls `Updates.checkForUpdateAsync()`. Kotlin always shows dev-build message regardless |
| ST-11 | 🔴 | **App Updates: "Restart now" dialog**: RN downloads then shows Alert with "Later" / "Restart now". Kotlin shows "No update available" |
| ST-12 | 🔴 | **App Updates: `updateAvailable` guard**: RN blocks Download if no update available. Kotlin always allows click |
| ST-13 | 🟡 | **Notifications subtitle field**: RN reads `settings.dailyDigestMorningSummary`. Kotlin reads `settings.notifDailyDigest` — may map to wrong preference key |

### SmsImportHealthScreen
| # | Severity | Issue |
|---|----------|-------|
| SH-1 | 🔴 | **Pull-to-refresh missing**: RN `ScrollView` with `RefreshControl`. Kotlin `LazyColumn` with no pull-to-refresh |
| SH-2 | 🔴 | **Lifetime counters loading state**: RN shows spinner while `loading && !stats`. Kotlin shows 0s immediately |
| SH-3 | 🔴 | **Import log loading state**: RN shows spinner while `loading && auditEntries.length === 0`. Kotlin shows empty state immediately |
| SH-4 | 🔴 | **Battery optimization button icon**: RN has `battery-half-outline` leading icon. Kotlin button has no icon |
| SH-5 | 🔴 | **DB integrity button icon**: RN has `warning-outline` leading icon. Kotlin has none |
| SH-6 | 🔴 | **Ingest queue button icon**: RN has `layers-outline` leading icon. Kotlin has none |
| SH-7 | 🔴 | **DB repair loading/disabled state**: RN `disabled={repairing}` + `loading={repairing}`. Kotlin always enabled, no feedback |
| SH-8 | 🔴 | **Reconcile button icon**: RN `sync-outline` leading icon. Kotlin has none |
| SH-9 | 🔴 | **Retry Queue button icon**: RN `refresh-outline` leading icon. Kotlin has none |
| SH-10 | 🔴 | **Action buttons cross-disable**: RN `disabled={reconciling || retrying}` on both. Kotlin no cross-disable |
| SH-11 | 🟡 | **Activity section icon rendering**: RN renders real Ionicons glyphs. Kotlin maps Ionicons names to Material Unicode codepoints — different glyphs, different font |
| SH-12 | 🔴 | **"Clear import log" confirmation**: RN shows `Alert.alert` before clearing. Kotlin clears immediately |
| SH-13 | 🔴 | **Import Log header clock icon**: RN shows `time-outline` icon before "Import Log". Kotlin text only |
| SH-14 | 🟡 | **Audit entry limit**: RN limits display to first 10 entries. Kotlin shows all entries — no cap |
| SH-15 | 🔴 | **Path text selectability**: RN `selectable` prop on DB path text (long-press to copy). Kotlin not selectable |
| SH-16 | 🟡 | **`parse_failed:` label prefix**: RN replaces with `"fail:"` prefix → `"fail:reason"`. Kotlin removes prefix → `"reason"` — different display |
| SH-17 | 🔴 | **Live receiver status clock**: RN updates status every 30s via `setInterval` `nowTick`. Kotlin status is static until manual refresh |
| SH-18 | 🔴 | **Data reactivity**: RN `useLiveQuery` re-loads on DB changes. Kotlin loads on entry + manual refresh only |

---

## SUMMARY COUNTS

| Area | 🔴 MISSING | 🟡 WRONG | ⚪ MINOR |
|------|-----------|---------|---------|
| Cross-cutting | 4 | 5 | 0 |
| Onboarding/Auth/AppLock | 7 | 10 | 2 |
| Home | 4 | 0 | 0 |
| Calendar | 4 | 2 | 0 |
| Finance (all sub-screens) | 8 | 12 | 2 |
| Bills/Loans | 2 | 2 | 0 |
| Events/Tasks | 2 | 1 | 0 |
| Recurring | 6 | 8 | 0 |
| Analytics/WeekReview | 0 | 4 | 0 |
| Search | 6 | 0 | 0 |
| Assistant | 2 | 3 | 2 |
| Learning | 5 | 5 | 1 |
| Profile/PersonalInfo | 1 | 3 | 0 |
| Settings | 15 | 5 | 0 |
| SmsImportHealth | 13 | 3 | 0 |
| **TOTAL** | **79** | **63** | **7** |

**149 total gaps across the full app.**

---

## EXECUTION PHASES — 100% COVERAGE

Every gap ID below is assigned to exactly one phase. No ID is left out.

---

### Phase 1 — Bug fixes & wrong logic (fastest wins, most visible)
_Things that are present but broken — copy-paste errors, wrong icons causing data confusion, wrong formulas._

| ID | Fix |
|----|-----|
| BI-1 | Bills: unpaid rows use same `CheckCircle` icon as paid — replace with unchecked icon |
| BI-2 | Bills: cycle label `"One_time"` → `"One-time"` (underscore → hyphen in label map) |
| RQ-1 | ReviewQueue: OutcomeChip `"batch_pending"` shows "Batch" → change to "Pending" |
| RQ-2 | ReviewQueue: amount text style `titleMedium` → `bodyMedium` |
| TL-1 | TransactionListItem: category icon hardcoded `Icons.Filled.Share` → derive from category |
| TL-2 | TransactionListItem: expense amounts not shown in `error` color → add color logic |
| TL-3 | TransactionListItem: transfers shown as negative → display as positive outgoing |
| TD-1 | TransactionDetailScreen: hero icon hardcoded `Icons.Filled.Share` → derive from type |
| WR-1 | WeekReviewScreen: greeting style `headlineSmall` → `titleMedium` |
| WR-2 | WeekReviewScreen: section headings one size too large → correct to match RN |
| AN-1 | AnalyticsScreen: bars animate sequentially → animate all simultaneously |
| CA-1 | CalendarScreen: day-of-week grid offset formula wrong → fix start-of-month column |
| CA-5 | CalendarScreen: completed tasks missing strikethrough + 0.5 opacity |
| CA-6 | CalendarScreen: tasks appear in Events tab → filter correctly to Tasks tab only |
| RF-6 | RecurringFormScreen: cadence `"mon_fri"` displays as `"Mon_fri"` → add `CADENCE_LABELS` map |
| SH-14 | SmsImportHealth: audit entries show all → cap display at 10 (match RN `slice(0, 10)`) |
| SH-16 | SmsImportHealth: `parse_failed:reason` label → prefix with `"fail:"` to match RN |
| AS-3 | AssistantScreen: suggested prompts hide on first message → keep showing until `messages.length > 1` |
| LE-8 | LearningScreen: checkmark shown on every card → only show when `is_completed = 1` |
| ON-3 | Onboarding: "Allowed" icon is `Icons.Filled.Person` → replace with `Icons.Filled.CheckCircle` |
| ON-4 | Onboarding: selected goal icon is `Icons.Filled.Person` → replace with `Icons.Filled.CheckCircle` |
| AL-2 | AppLockScreen: PIN length hardcoded to 6 → read `settings.pinCode.length` dynamically |
| PI-3 | PersonalInfo: title text left-aligned → `textAlign = TextAlign.Center` |
| PI-4 | PersonalInfo: full border on rows → bottom-only border (match RN `borderBottomWidth: 1`) |
| ST-8 | Settings: About Version row is no-op → show `AlertDialog('About', 'LifeOS v1.0.0')` |
| ST-13 | Settings: notifications subtitle reads wrong preference key → align field name |

---

### Phase 2 — Missing sections & fields (high functional impact)
_Entire UI sections or form fields that exist in RN but are completely absent in Kotlin._

| ID | Fix |
|----|-----|
| HO-1 | HomeScreen: add AgendaCard section (today's events + tasks below header) |
| HO-2 | HomeScreen: add QuickActions section (Add Transaction, Add Task shortcuts) |
| HO-3 | HomeScreen: show username in greeting ("Good morning, {name}") |
| HO-4 | HomeScreen: profile button → navigate to PersonalInformationScreen |
| SE-1 | SearchScreen: add Bills result section |
| SE-2 | SearchScreen: add Goals result section |
| SE-3 | SearchScreen: add Income result section |
| SE-4 | SearchScreen: add Loans result section |
| SE-5 | SearchScreen: add persisted recent searches |
| SE-6 | SearchScreen: highlight matched query text in results |
| CA-2 | CalendarScreen: show colored task dots under days that have tasks |
| CA-3 | CalendarScreen: add horizontal swipe gesture to change month |
| CA-4 | CalendarScreen: add "Today" button to jump to current date |
| FI-1 | FinanceScreen: show SMS permission banner when permission not granted |
| FI-2 | FinanceScreen: wrap each date-group of transactions in a `Card` container |
| FI-3 | FinanceScreen: show post-import result banner ("Imported N transactions") |
| FI-4 | FinanceScreen: fix budget banner fallback calculation to match RN logic |
| EX-1 | ExportScreen: add custom date range picker (start + end date fields) when "Custom" selected |
| EX-2 | ExportScreen: show item counts inside each export type card |
| EX-3 | ExportScreen: add "Clear history" button to export history section |
| LF-1 | LoanFormScreen: add "Total repaid" read-only field |
| LF-2 | LoanFormScreen: add "Last repayment date" field |
| EF-1 | EventFormScreen: add reminder enable/disable toggle |
| EF-2 | EventFormScreen: add delete confirmation dialog before deleting event |
| CZ-2 | CategorizeScreen: add `chevron-down` trailing icon on "Pick a category…" button |
| PI-1 | PersonalInfoScreen: add `FocusRequester` so edit field auto-focuses when modal opens |
| AS-1 | AssistantScreen: render interactive action chips inside assistant message bubbles |
| AS-2 | AssistantScreen: scroll to bottom on `onContentSizeChange` (not just on message count change) |
| AU-1 | AuthScreen: add loading/splash state (logo + spinner) while `isLoading` is true |
| AU-2 | AuthScreen: add leading `person-outline` icon to both text fields |
| ON-1 | OnboardingScreen: add 80×80 logo box with `icon.png` in WelcomeStep |
| ON-2 | OnboardingScreen: add step-transition fade+slide animation on step change |
| ON-5 | OnboardingScreen: add correct 2 PillarCards per permission step (step 4: timer/reminder; step 5: M-Pesa/on-device) |
| ON-6 | OnboardingScreen: add leading `person-outline` icon to name field in ProfileSetupStep |
| SH-1 | SmsImportHealth: add pull-to-refresh (`RefreshControl`) to the scroll container |
| SH-4 | SmsImportHealth: add `battery-half-outline` leading icon to battery optimization button |
| SH-5 | SmsImportHealth: add `warning-outline` leading icon to DB integrity button |
| SH-6 | SmsImportHealth: add `layers-outline` leading icon to ingest queue button |
| SH-8 | SmsImportHealth: add `sync-outline` leading icon to Reconcile button |
| SH-9 | SmsImportHealth: add `refresh-outline` leading icon to Retry Queue button |
| SH-13 | SmsImportHealth: add `time-outline` icon before "Import Log" section header |
| LE-2 | LearningScreen: implement Log Session modal (Topic, Duration, Notes fields + validation + save) |
| LE-3 | LearningScreen: make session cards tappable → call `markCompleted(id)` on tap |
| LE-4 | LearningScreen: add empty state view (icon + "No sessions here" + contextual text) |
| LE-5 | LearningScreen: add `ProgressBar` + "Continue"/"Start" chip on non-completed cards |

---

### Phase 3 — UX behavior: confirmations, banners, loading states, animations
_Interactions that should happen but don't — dialogs before destructive actions, feedback after saves, loading indicators._

| ID | Fix |
|----|-----|
| CC-2 | Add `AlertDialog` confirmation before every delete action across all screens (bills, goals, loans, budgets, events, tasks, recurring rules, transactions) |
| CC-3 | Show `TopBanner` success message after every form save before navigating back (all form screens) |
| CC-4 | Add fade-in entry animation to all form/detail screens (opacity 0→1, translateY 8→0 on load) |
| RC-1 | RecurringScreen: show delete confirmation dialog before deleting rule |
| RC-2 | RecurringScreen: animate list item removal on delete |
| RF-1 | RecurringFormScreen: add content fade-in animation when form data loads |
| RF-2 | RecurringFormScreen: show "Rule updated"/"Rule added" banner before navigating back |
| RF-3 | RecurringFormScreen: show delete confirmation dialog before deleting rule |
| RF-4 | RecurringFormScreen: add haptic feedback (`HapticFeedback.perform(LONG_PRESS)`) on save |
| RF-5 | RecurringFormScreen: show `AlertDialog` for missing title / missing date validation (before inline error) |
| EF-3 | EventFormScreen: replace AlertDialog date/time/recurrence pickers with bottom sheets to match RN |
| CZ-1 | CategorizeScreen: auto-dismiss TopBanner after 1500 ms |
| AL-1 | AppLockScreen: "Forgot PIN?" → show `AlertDialog` that offers to clear PIN (call `updateSettings(pinCode='', screenLockEnabled=false)`) |
| ST-2 | SettingsScreen: show "Requesting…" loading state + hide chevron while permission request in-flight |
| ST-3 | SettingsScreen: show "Permissions denied — grant them in device Settings" message on denial |
| ST-4 | SettingsScreen: use `DisposableEffect(lifecycleOwner)` to re-check SMS permission on every resume |
| ST-6 | SettingsScreen: on background receiver toggle → check battery optimization → prompt `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` |
| ST-7 | SettingsScreen: show error message "Could not update background receiver" if toggle throws |
| ST-9 | SettingsScreen: add loading spinner + disable both update buttons during async check/download |
| ST-10 | SettingsScreen: call real update check API; show "Update available" or "Already on latest" result |
| ST-11 | SettingsScreen: after download, show `AlertDialog` with "Later" and "Restart now" options |
| ST-12 | SettingsScreen: disable Download button when no update is available |
| SH-2 | SmsImportHealth: show `CircularProgressIndicator` while `isLoading && stats == null` (counters section) |
| SH-3 | SmsImportHealth: show `CircularProgressIndicator` while `isLoading && auditEntries.isEmpty()` (log section) |
| SH-7 | SmsImportHealth: disable + show loading state on DB repair button while repair is running |
| SH-10 | SmsImportHealth: cross-disable both action buttons when either reconcile or retry is in-flight |
| SH-12 | SmsImportHealth: show `AlertDialog` confirmation before clearing import log |
| SH-15 | SmsImportHealth: make JS DB path + Native DB path text selectable (`Modifier.selectable` / `SelectionContainer`) |
| SH-17 | SmsImportHealth: add a 30-second ticker that re-evaluates receiver status without manual refresh |
| SH-18 | SmsImportHealth: subscribe to DB change signal and auto-reload data when transactions change |
| PI-2 | PersonalInfo: move `TopBanner` outside the `if (editing != null)` guard so it shows after sheet closes |
| AS-4 | AssistantScreen: add "Thinking…" text label to TypingIndicator alongside the dots |

---

### Phase 4 — Format & display corrections
_Data displayed in the wrong format — numbers, dates, times, text casing, color tokens._

| ID | Fix |
|----|-----|
| CC-5 | Replace `compactCurrency()` with `formatCurrency(decimals=0)` in analytics charts and summary cards |
| CC-6 | Replace `onBackground` / `onBackground.copy(alpha)` color usages with `onSurface` / `onSurfaceVariant` tokens |
| AN-2 | AnalyticsScreen: chart currency → `formatCurrency(decimals=0)` (covered by CC-5, confirm chart specifically) |
| MD-1 | MerchantDetailScreen: "Avg per day" → `formatCurrency(avgPerDay, decimals=0)` (whole number) |
| MD-2 | MerchantDetailScreen: transaction time format `HH:mm` (24h) → `hh:mm a` (12h, en-KE) |
| MD-3 | MerchantDetailScreen: date format `MMM d, yyyy` → `MMM dd, yyyy` (zero-pad day) |
| MD-4 | MerchantDetailScreen: category text → force rest of string lowercase after first char |
| RC-6 | RecurringScreen: empty state subtitle `bodyMedium` → `bodySmall` |
| RF-11 | RecurringFormScreen: saving indicator button text `"Saving…"` instead of `CircularProgressIndicator` inside button |
| SH-11 | SmsImportHealth: fix activity section icon rendering — map Ionicons names to correct vector assets instead of Material Unicode codepoints |
| SH-16 | (already in Phase 1) |

---

### Phase 5 — Icon family & visual component fixes
_All icon swaps (Ionicons outline → matching Material outlined equivalents), component visual redesigns._

| ID | Fix |
|----|-----|
| CC-1 | Swap all `Icons.Filled.*` throughout the app to their `Icons.Outlined.*` equivalents to match Ionicons outline weight |
| CC-7 | PageScaffold: remove custom gradient background → use flat `MaterialTheme.colorScheme.background` |
| CC-8 | HeroSurface: align API and visual output to match RN's theme-adaptive gradient component |
| CC-9 | ShimmerLoadingState: replace complex icon+text skeleton with simple rectangle placeholders matching RN |
| RC-3 | RecurringScreen: delete icon `Icons.Filled.Delete` → `Icons.Outlined.Delete` |
| RC-4 | RecurringScreen: add icon `Icons.Filled.Add` → `Icons.Outlined.Add` |
| RC-5 | RecurringScreen: empty state icon `Icons.Filled.Repeat` → `Icons.Outlined.Repeat` |
| RC-7 | RecurringScreen: delete button `Arrangement.End` → `Arrangement.Start` (left-aligned) |
| RC-8 | RecurringScreen: standard Material3 `Switch` → custom `LifeOSSwitch` (or match visual style) |
| RF-7 | RecurringFormScreen: type selector `FilterChip` row → `Dropdown` component (match RN) |
| RF-8 | RecurringFormScreen: status toggle `OutlinedButton` → color-changing button (green active, surfaceVariant paused) |
| RF-9 | RecurringFormScreen: delete icon `Icons.Filled.Delete` → `Icons.Outlined.Delete` |
| RF-10 | RecurringFormScreen: date `OutlinedTextField` overlay → custom `DateField` widget (match RN presentation) |
| FA-1 | FeeAnalyticsScreen: empty state icon `Icons.Filled.CheckCircle` → `Icons.Outlined.CheckCircle` |
| FA-2 | FeeAnalyticsScreen: `Divider()` → add `marginVertical: 2dp` spacing around divider |
| MD-5 | MerchantDetailScreen: empty state icon `Icons.Filled.Receipt` → `Icons.Outlined.Receipt` |
| CZ-3 | CategorizeScreen: `ModalBottomSheet` → slide-up `Modal` at 70% height with rounded top corners (match RN) |
| CZ-4 | CategorizeScreen: empty state icon `Icons.Filled.CheckCircle` → `Icons.Outlined.CheckCircle` |
| CZ-5 | CategorizeScreen: loading state `Box(fillMaxSize)` center → top-padded center (`paddingVertical = 3xl`) |
| ST-1 | SettingsScreen: all 14 `SettingsRow` icons → swap to matching Ionicons outline equivalents |
| ST-5 | SettingsScreen: permission banner icon `Icons.Filled.Warning` → `Icons.Outlined.Warning` (circle outline) |
| AL-3 | AppLockScreen: keypad bottom row `["bio", "0", "del"]` → `[null, "0", "del"]` (remove bio key, add empty spacer) |
| AL-4 | AppLockScreen: subtitle + "Forgot PIN?" color → `onSurfaceVariant` (not `onBackground.copy(alpha)`) |
| AL-5 | AppLockScreen: headline raw `fontSize=22.sp` → `MaterialTheme.typography.headlineSmall` |
| AL-6 | AppLockScreen: fingerprint icon `Icons.Filled.Fingerprint` → `Icons.Outlined.FingerprintOutlined` |
| AU-3 | AuthScreen: logo badge → 40×40 `RoundedCornerShape(borderRadius.lg)` + `1dp` border + real app `icon.png` (not `Icons.Filled.Person`) |
| ON-3 | (already fixed in Phase 1) |
| ON-7 | OnboardingScreen: "Balanced" goal icon `Icons.Filled.AutoAwesome` → `Icons.Outlined.Tune` (options/sliders) |
| LE-6 | LearningScreen: category filter `ExposedDropdownMenuBox` → horizontal `Chip` scroll row |
| LE-7 | LearningScreen: session card `GlassCard` → `Card(elevation=...)` (elevated surface) |
| LE-9 | LearningScreen: progress bar always green → dynamic color (green ≥80%, yellow ≥40%, red <40%) |
| AS-5 | AssistantScreen: inline `OutlinedTextField` input → `ChatInput` component (pill shape, match RN style) |
| ON-8 | OnboardingScreen: progress dots spacing `padding(horizontal=3.dp)` → `gap: spacing.sm` equivalent |

---

### Phase 6 — Deep feature parity & DB integration
_Features requiring new DB tables, ViewModels, or significant new screens._

| ID | Fix |
|----|-----|
| LE-1 | LearningScreen: replace hardcoded list → Room DAO + `learning_sessions` table (create table, DAO, ViewModel) |
| LE-10 | LearningScreen: monthly hours → `SUM(duration_minutes)/60.0` from DB (after LE-1) |
| ON-2 | OnboardingScreen: add step-transition `Animatable` fade+slide between steps |
| ON-5 | (already in Phase 2) |
| AU-1 | (already in Phase 2) |

---

### Checklist totals per phase

| Phase | Gap IDs covered | Count |
|-------|----------------|-------|
| 1 — Bug fixes & wrong logic | BI-1/2, RQ-1/2, TL-1/2/3, TD-1, WR-1/2, AN-1, CA-1/5/6, RF-6, SH-14/16, AS-3, LE-8, ON-3/4, AL-2, PI-3/4, ST-8/13 | 26 |
| 2 — Missing sections & fields | HO-1/2/3/4, SE-1–6, CA-2/3/4, FI-1/2/3/4, EX-1/2/3, LF-1/2, EF-1/2, CZ-2, PI-1, AS-1/2, AU-1/2, ON-1/5/6, SH-1/4/5/6/8/9/13, LE-2/3/4/5 | 46 |
| 3 — Confirmations, banners, loading | CC-2/3/4, RC-1/2, RF-1/2/3/4/5, EF-3, CZ-1, AL-1, ST-2/3/4/6/7/9/10/11/12, SH-2/3/7/10/12/15/17/18, PI-2, AS-4 | 34 |
| 4 — Format & display | CC-5/6, AN-2, MD-1/2/3/4, RC-6, RF-11, SH-11 | 10 |
| 5 — Icons & visual components | CC-1/7/8/9, RC-3/4/5/7/8, RF-7/8/9/10, FA-1/2, MD-5, CZ-3/4/5, ST-1/5, AL-3/4/5/6, AU-3, ON-7/8, LE-6/7/9, AS-5 | 33 |
| 6 — Deep DB & features | LE-1/10, ON-2 | 3 |
| **Total** | | **152** |

> _Note: 3 IDs appear in 2 phases (ON-3/ON-4 fixed in Phase 1, referenced in Phase 5; SH-16 fixed in Phase 1, also mentioned in Phase 4; ON-5/AU-1 placed in Phase 2, noted in Phase 6) — the fix happens in the earlier phase. Net unique IDs = **149**. Zero gaps unassigned._  
