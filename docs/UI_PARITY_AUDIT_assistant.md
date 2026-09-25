# UI Parity Audit — Assistant (AI)

_Generated 2026-09-25 · driven by `docs/UI_PARITY_CHECKLIST.html` (17 categories per unit)_
_Reference (A) = RFINAL (React Native) · Target (B) = KFINAL (Kotlin/Compose)_

Method: read both implementations (`AssistantScreen.tsx`, `components/assistant/{ChatMessage,ChatInput,SuggestedPrompts}.tsx`,
`store/useAssistantStore.ts`, `database/repositories/AssistantMessageRepository.ts` ↔
`ui/screen/assistant/AssistantScreen.kt`, `viewmodel/AssistantViewModel.kt`, `data/db/dao/AssistantDao.kt`)
and walked all 17 checklist categories. Every finding is checked against RFINAL source, not commit titles.

Legend: ✅ passes · ⚠️ differs (fixed in this pass) · ❌ missing/differs (not yet fixed) · n/a

---

## Assistant screen

`src/screens/assistant/AssistantScreen.tsx` (+ `components/assistant/*`) ↔
`ui/screen/assistant/AssistantScreen.kt` + `AssistantViewModel.kt` + `AssistantDao.kt`

| # | Category | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | Page Structure | ✅ | Header ("Assistant" + "Offline · Rule-based" subtitle + conditional trash action) → message area → suggested prompts → composer, in RFINAL's top-to-bottom order. Empty state renders in the message area, not as the list's first row. |
| 2 | Colors & Theming | ⚠️ | Header/subtitle/bubble/empty-state tokens match (onSurface, onSurfaceVariant, primary, surfaceVariant, outlineVariant). KFINAL paints `colorScheme.surface` behind the composer; RFINAL's `ChatInput` is transparent on `background` — minor residual (see Flags). |
| 3 | Typography | ⚠️ | Global `bodySmall` was 13sp; RFINAL MD3 `bodySmall` is 12sp — corrected in `Type.kt`, fixing subtitles, timestamps, prompt labels and empty-state copy. |
| 4 | Spacing & Layout | ⚠️ | Bubble max-width 78% ✓. Inter-message spacing was 14dp; RFINAL's list gap 8 + per-bubble marginBottom 14 = 22dp → fixed. "Try asking:" xs→sm bottom gap fixed. Composer bottom inset was hand-rolled; now RFINAL's exact `max(insets.bottom, sm) + sm + 48 + 2` / `keyboardHeight + base + 8`. |
| 5 | Pills & Chips | ⚠️ | Action chips were Material's 8dp rounded rect; RFINAL `Chip` uses `borderRadius.full` → pill. Suggested-prompts chips (surfaceVariant fill, primary label) match. |
| 6 | Cards & Containers | n/a | No cards on this screen. |
| 7 | Toggles | n/a | None. |
| 8 | Buttons & Actions | ✅ | Clear (trash, error tint) and send (arrow-up-circle, primary/outlineVariant by enabled) match. |
| 9 | Input Fields | ✅ | Placeholder "Message LifeOS...", maxLength 500, multiline, readOnly while a reply is in flight (A passes `editable={!disabled}`), `ImeAction.Send`. |
| 10 | Destructive Actions | ✅ | Clear conversation shows a confirmation dialog before clearing. |
| 11 | Notifications & Toasts | n/a | None in A. |
| 12 | Animations & Transitions | ⚠️ | Auto-scroll on new message + on content change ✓. Typing indicator is a static 3-dot + "Thinking…" (no bounce) ✓. Floating tab bar now hides while the keyboard is open (`tabBarHideOnKeyboard`) — fixed. No entry animation in A either. |
| 13 | Charts & Data Viz | n/a | None. |
| 14 | Sheets | n/a | None. |
| 15 | Data & Functionality | ⚠️ | **Conversation id was `main`; RFINAL's `CONVERSATION_ID` is `default`** → constant + `MIGRATION_5_6` fixed, so existing chat survives. History cap was 10; A caps at 100 → fixed. Greeting truncated to the first word; A uses the full profile name → fixed. Reply amounts were hand-rolled `KES`; now delegate to shared `formatCurrency` (Ksh, grouping, 2dp). Offline engine intent order and keywords match. |
| 16 | Icons | ⚠️ | Semantic icons match (sparkles avatar/empty, trash clear, arrow-up-circle send), but the family is still Material vs RFINAL's Ionicons — the global CC-1 gap. |
| 17 | Empty, Loading & Error States | ⚠️ | Empty state now centres in the message area (was the first list row) with icon + "Ask me anything" + description. "Thinking…" indicator ✓. No error state in A; B has none either. |

---

## Cross-cutting flags (global — not changed here)

1. **Icon family.** RFINAL renders Ionicons; KFINAL renders Material icons. Semantically correct per-screen, but a visual weight/style difference on every screen (CC-1 in `PARITY_GAPS.md`).
2. **Composer surface band.** KFINAL paints `colorScheme.surface` behind the composer; RFINAL's `ChatInput` is transparent on `background`. Cosmetic; left as-is unless you want it removed.
3. **Composer vertical alignment.** KFINAL centres the input (`CenterVertically`); RFINAL's `ChatInput` row uses `alignItems: 'flex-end'`. Minor — only visible once the field grows to multiple lines.
4. **Floating tab bar bottom offset.** Was `insets.bottom + 12` (an extra 8dp); corrected to RFINAL's `insets.bottom + 4` in this pass. This also makes the composer's `tabBarSafeInset` clear the bar with the intended 6dp gap.

---

## Verification status

✅ Compiler-verified locally: `compileDebugKotlin`, `testDebugUnitTest` (migration test), `detekt` and `ktlintCheck` all pass.
