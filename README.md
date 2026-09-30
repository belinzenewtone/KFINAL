# LifeOS — Kotlin/Compose

Personal life-management app (finance with on-device M-Pesa/bank SMS
parsing, tasks, events, budgets, goals). Kotlin Multiplatform with Compose
Multiplatform UI, SQLDelight persistence, Koin DI, and WorkManager background
ingestion on Android. Fully on-device — no network permission required for the
finance pipeline.

## Modules

| Module | Purpose |
|---|---|
| `:app` | Multiplatform application. Source sets: `commonMain` (shared data layer/entities/SQLDelight schema), `androidMain` (UI, ViewModels, services — the shipping app), `iosMain` and `desktopMain` (entry points, still shells) |
| `:sms` | Self-contained Android SMS parser library (M-Pesa, Airtel Money, ~26 Kenyan institutions, durable ingest queue, quarantine/review pipeline) |

## Architecture notes

- **Single-writer database** — SQLDelight owns the schema (`.sq` files under
  `app/src/commonMain/sqldelight`), including `import_audit` / `sms_ingest_queue`.
  The parser's `DbWriter` executes on the app database's Android driver
  connection via `SmsParserDatabase.attach`; no second SQLiteOpenHelper exists.
  `docs/PHASE0_DECISIONS.md` (D3) is the historical record and still describes
  the earlier Room design.
- **Dependency injection** — Koin (`di/AppModule.kt`); register new ViewModels
  there. Hilt is not used.
- **Dates** — `kotlinx-datetime` everywhere; no `java.time`.
- **Parser quality gates** — 177 unit tests including a 182-message fixture
  corpus, cross-parser voting, confidence routing (DIRECT/REVIEW/QUARANTINE),
  and OTA rule-bundle hot-swap (`BundleCompiler`).
- **Static analysis** — detekt (`maxIssues: 0`) + ktlint gate every build;
  config in `config/detekt/detekt.yml`, formatter-rule exceptions in
  `.editorconfig` files.

## Commands

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # minified release (unsigned without keystore.properties)
./gradlew :sms:testDebugUnitTest :app:testDebugUnitTest   # full test suite
./gradlew :app:detekt :sms:detekt :app:ktlintCheck :sms:ktlintCheck   # static analysis
```

## Release process

1. Update `versionCode` / `versionName` in `app/build.gradle.kts`.
2. Run the checklist in `docs/RELEASE_CHECKLIST.md`.
3. Tag `vX.Y.Z` and push — `.github/workflows/release.yml` builds the signed
   APK, publishes SHA-256 checksums, and creates the GitHub Release.
   Signing requires the `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` repository secrets.

## Documentation map

- `docs/PHASE0_DECISIONS.md` — canonical-repo decision, parser component
  winners (RFINAL vs KOTLIN diff), single-writer DB migration record.
- `docs/PARITY_GAPS.md` — open parity backlog vs the legacy RN app.
- `docs/archive_MIGRATION_PLAN.html` — archived RN→Compose migration plan.
- `docs/RELEASE_CHECKLIST.md` — pre-release go/no-go checklist.
