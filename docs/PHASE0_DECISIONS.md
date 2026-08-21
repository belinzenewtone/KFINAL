# Phase 0 — Decisions & Baseline

Status: COMPLETE · Date: 2026-08-21
Scope: canonical repo, component winners (RFINAL vs KOTLIN), single-writer DB decision.

---

## D1 — Canonical repo

**KFINAL is canonical.** RFINAL and Downloads\KOTLIN become reference-only.
All parser code must live in-repo by end of Phase 1 (no build-time staging).

## D2 — Component winners (RFINAL vs KOTLIN diff)

| # | Component | Winner | Reason |
|---|-----------|--------|--------|
| 1 | Pipeline/router | **KOTLIN** | ImportFilter modes, unified `SmsParseOutcome` (+FulizaBalanceUpdate), ML-demote step, cheap pre-filter |
| 2 | M-Pesa core parser | **RFINAL** | 12 detection rules vs 10 (M-Shwari, Lipa Mdogo, GlobalPay), deeper counterparty cleaning, date-shape buckets |
| 3 | Airtel Money parser | **RFINAL** | Extracts real tx date (KOTLIN stamps receivedAtMs) |
| 4 | Generic bank parser | **KOTLIN** | Card-debit detection, larger service-notice corpus, real date extraction, inline merchant category |
| 5 | Institution detection | Tie | Same 26-institution tables; KOTLIN adds IMBANK-NEWS sender |
| 6 | Normalizer/sender trust | **RFINAL** | Repairs 5 concatenation artefact classes vs 2 |
| 7 | Confidence scoring/routing | **KOTLIN** | Standalone scorer w/ sender-trust factor + explicit IMPORT/DEFER/QUARANTINE mapping, 15 tests |
| 8 | Cross-parser voting | Tie | Identical algorithm |
| 9 | Dedup engine | **RFINAL** | 5 tiers incl. cross-sender M-Pesa-ref dedup + airtime exemption |
| 10 | Fuliza lifecycle | **KOTLIN** | Typed charge-notice event → draw/repayment persistence (fix its fee-tier table from RFINAL values) |
| 11 | Merchant/category + ML | **KOTLIN** | On-device CART classifier learned from user corrections; RFINAL keyword-only |
| 12 | Rule bundles (OTA) | **KOTLIN** | Only implementation: JSON→rule compiler, 6-hourly fetch, hot-swap |
| 13 | Ingestion workers | **RFINAL** | Queue-first durability, row claims, boot drain, self-healing sweep. (Port KOTLIN's streaming historical scanner into it.) |
| 14 | Parser-table persistence | n/a | Decided separately in D3 |
| 15 | Tests | **KOTLIN** | 342 tests / 231 fixtures vs 115 / 198 |

### Port into KFINAL (from KOTLIN)
1. `SmsConfidenceScorer` + `ConfidenceBasedImportFilter`
2. Rule-bundle stack (`ParserRuleBundle`, `BundleCompiler`, `RuleBundleFetcher`, `activeRules()` hook)
3. Streaming historical scanner (cursor→Flow→chunked parallel ingest) — keep RFINAL durable queue underneath
4. Typed `FulizaChargeNotice` + Fuliza loan-state store (RFINAL captures fields but persists nothing)
5. Bank service-notice corpus + card-debit detection + 33 extra bank fixtures
6. `ImportAuditLogger` telemetry pattern
7. Optional/later: on-device CART classifier stack

### Keep from RFINAL
M-Pesa core + config, Airtel parser, normalizer/sender-trust, dedup engine,
durable queue workers/receivers, extra detection rules, `refineAppCategory()`.

## D3 — Database: Room becomes single owner

**Current state (verified):**
- One shared file: `filesDir/SQLite/lifeos.db`, WAL, opened by BOTH Room
  (`di/DatabaseModule.kt:32`) and DbWriter (`SmsService.kt:270`).
- Room v2 owns 18 entities incl. `transactions` (all dedupe columns/indices declared).
- DbWriter creates `import_audit` + `sms_ingest_queue` AND mutates Room-owned
  `transactions`: ALTERs 4 columns, creates a partial unique index
  `idx_tx_inst_extref` that collides with Room's `idx_tx_inst_ext_ref`,
  plus duplicates of Room's hash indices.
- `ImportAuditEntity.kt` / `SmsIngestQueueEntity.kt` exist but are ORPHANED
  (not registered in `LifeOsDatabase`; `SmsDao` explicitly excludes them).
- Blocker found: both tables use `DEFAULT (datetime('now'))` expression
  defaults, which Room cannot declare — hence the original dual-writer design.

**Decision:** Room owns all tables. DbWriter is deleted at end of Phase 3.

Migration scope (executed Phases 1–3):
1. Room v2→v3 migration REBUILDS `import_audit` + `sms_ingest_queue`
   (copy data, drop expression defaults — app supplies timestamps),
   registers the two existing entities, adds DAOs.
2. Remove every DbWriter statement touching `transactions` (ALTERs +
   duplicate/partial indices); rely on Room-declared indices only.
3. Re-point `SmsService`/workers from DbWriter to DAOs; keep the public
   SmsService API stable so call sites don't churn.
4. Delete DbWriter.kt and the shared-file path juggling in
   `LifeOsApplication`/`DatabaseModule`.

Risks logged: partial-index mismatch (above), WAL cross-connection visibility
during transition, fresh-install ordering (currently hacked via forced Room
warm-up in `LifeOsApplication.onCreate` — removed once single-writer).

## Exit criteria for Phase 0

- [x] Canonical repo chosen (D1)
- [x] Per-component winner recorded (D2)
- [x] Single-writer strategy decided with concrete migration scope (D3)
- [x] RFINAL tree confirmed present at `Music\RFINAL` (main + test sources)

Next: **Phase 1** — vendor parser sources into `sms/src/main/java`, delete the
staging Copy task, verify standalone build.
